package server

import (
	"api/db/models"
	"api/wallet/monero"
	"context"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"strconv"
	"sync"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
)

type Server struct {
	q                *models.Queries
	monero           *monero.Monero
	openedWalletFile string
	mutex            sync.Mutex
	demo             DemoWallet
}

type DemoWallet struct {
	Token    string
	File     string
	Password string
}

type errorResponse struct {
	Message string `json:"message"`
	Error   string `json:"error"`
}

func New(q *models.Queries, monero *monero.Monero, demoWallet DemoWallet) Server {
	return Server{q: q, monero: monero, demo: demoWallet}
}

func (s *Server) Start(port uint) error {
	portString := ":" + strconv.Itoa(int(port))

	mux := http.NewServeMux()
	handler := enableCORS(mux)

	mux.HandleFunc("/status", s.handleStatus)
	mux.HandleFunc("POST /account/new", s.handleNewAccount)
	mux.HandleFunc("POST /account/entropy-from-mnemonic", s.handleEntropyFromMnemonic)
	mux.Handle("/wallet/{crypto}/balance", s.authMiddleware(s.walletMiddleware(http.HandlerFunc(s.handleBalance))))
	mux.Handle("/wallet/{crypto}/addresses", s.authMiddleware(s.walletMiddleware(http.HandlerFunc(s.handleAddresses))))
	mux.Handle("POST /wallet/{crypto}/address", s.authMiddleware(s.walletMiddleware(http.HandlerFunc(s.handleNewAddress))))
	mux.Handle("/wallet/{crypto}/transactions", s.authMiddleware(s.walletMiddleware(http.HandlerFunc(s.handleTransactions))))
	mux.Handle("POST /wallet/{crypto}/transaction", s.authMiddleware(s.walletMiddleware(http.HandlerFunc(s.handleNewTransaction))))

	s.openDemoWallet()

	log.Printf("[INFO] Starting server on port %d", port)
	err := http.ListenAndServe(portString, handler)
	if err != nil {
		return err
	}
	return nil
}

type newAccountResponse struct {
	Mnemonic string `json:"mnemonic"`
	Entropy  string `json:"entropy"`
}

func (s *Server) handleNewAccount(w http.ResponseWriter, _ *http.Request) {
	mnemonic, entropy, hash, err := newAccountData()
	if err != nil {
		response(w, http.StatusInternalServerError, errorResponse{Message: "Failed to create new account: " + err.Error(), Error: "ACCOUNT_CREATION_ERROR"})
		return
	}

	err = s.q.CreateAccount(context.Background(), hash)
	if err != nil {
		response(w, http.StatusInternalServerError, errorResponse{Message: "Failed to create new account: " + err.Error(), Error: "ACCOUNT_CREATION_ERROR"})
		return
	}

	uuid := uuid.New().String()
	err = s.monero.CreateWallet(uuid, entropy)
	if err != nil {
		response(w, http.StatusInternalServerError, errorResponse{Message: "Failed create monero wallet for new account: " + err.Error(), Error: "ACCOUNT_CREATION_MONERO_WALLET_ERROR"})
		return
	}

	err = s.q.SetMoneroWallet(context.Background(), models.SetMoneroWalletParams{Hash: hash, MoneroWallet: pgtype.Text{String: uuid, Valid: true}})
	if err != nil {
		response(w, http.StatusInternalServerError, errorResponse{Message: "Failed to add monero wallet to new account: " + err.Error(), Error: "ACCOUNT_CREATION_MONERO_WALLET_DB_ERROR"})
		return
	}

	response(w, http.StatusOK, newAccountResponse{Mnemonic: mnemonic, Entropy: entropy})
}

type entropyFromMnemonicRequest struct {
	Mnemonic string `json:"mnemonic"`
}

type entropyFromMnemonicResponse struct {
	Entropy string `json:"entropy"`
}

func (s *Server) handleEntropyFromMnemonic(w http.ResponseWriter, r *http.Request) {
	bodyData, err := io.ReadAll(r.Body)
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to parse json data: " + err.Error(), Error: "INVALID_JSON"})
		return
	}

	var data entropyFromMnemonicRequest
	err = json.Unmarshal(bodyData, &data)
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to parse json data: " + err.Error(), Error: "INVALID_JSON"})
		return
	}

	entropy, err := entropyFromMnemonic(data.Mnemonic)
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to generate entropy from mnemonic: " + err.Error(), Error: "ENTROPY_FROM_MNEMONIC_ERROR"})
		return
	}

	response(w, http.StatusOK, entropyFromMnemonicResponse{Entropy: entropy})

}

func (s *Server) openDemoWallet() {
	if s.demo.File == "" {
		return
	}

	log.Println("[INFO] Opening demo wallet file after 10 seconds")
	time.Sleep(10 * time.Second)
	err := s.monero.OpenWallet(s.demo.File, s.demo.Password)
	if err != nil {
		log.Println("[ERROR] Failed to open demo wallet file: " + err.Error())
		return
	}

	s.openedWalletFile = s.demo.File
	log.Println("[INFO] Demo wallet opened successfully")
}

func (s *Server) handleStatus(w http.ResponseWriter, _ *http.Request) {
	status, err := s.monero.Status()
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to get status", Error: "STATUS_ERROR"})
		return
	}
	response(w, http.StatusOK, status)
}

func (s *Server) handleAddresses(w http.ResponseWriter, _ *http.Request) {
	addresses, err := s.monero.Addresses()
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to get wallet addresses: " + err.Error(), Error: "WALLET_GET_ADDRESSES_ERROR"})
		return
	}
	response(w, http.StatusOK, addresses)
}

func (s *Server) handleNewAddress(w http.ResponseWriter, _ *http.Request) {
	address, err := s.monero.NewAddress()
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to generate new address: " + err.Error(), Error: "WALLET_GENERATE_ADDRESS_ERROR"})
		return
	}
	response(w, http.StatusOK, address)
}

func (s *Server) handleTransactions(w http.ResponseWriter, _ *http.Request) {
	transactions, err := s.monero.Transactions()
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to get wallet transactions: " + err.Error(), Error: "WALLET_GET_TRANSACTION_ERROR"})
		return
	}
	response(w, http.StatusOK, transactions)
}

type transactionRequest struct {
	Address string  `json:"address"`
	Amount  float64 `json:"amount"`
}

func (s *Server) handleNewTransaction(w http.ResponseWriter, r *http.Request) {
	data, err := io.ReadAll(r.Body)
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to parse json data: " + err.Error(), Error: "INVALID_JSON"})
		return
	}

	var transactionData transactionRequest
	err = json.Unmarshal(data, &transactionData)
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to parse json data: " + err.Error(), Error: "INVALID_JSON"})
		return
	}

	if transactionData.Amount <= 0 {
		response(w, http.StatusBadRequest, errorResponse{Message: "Amount must be greater than 0", Error: "WALLET_NEW_TRANSACTION_INCORRECT_AMOUNT"})
		return
	}

	result, err := s.monero.Transfer(transactionData.Address, transactionData.Amount)
	if err != nil {
		response(w, http.StatusInternalServerError, errorResponse{Message: "Failed to create new transaction: " + err.Error(), Error: "WALLET_NEW_TRANSACTION_ERROR"})
		return
	}

	response(w, http.StatusOK, result)
}

type balanceResponse struct {
	Confirmed   float64 `json:"confirmed"`
	Unconfirmed float64 `json:"unconfirmed"`
}

func (s *Server) handleBalance(w http.ResponseWriter, r *http.Request) {
	confirmed, unconfirmed, err := s.monero.TotalBalance()
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to get wallet balance: " + err.Error(), Error: "WALLET_GET_BALANCE_ERROR"})
		return
	}

	response(w, http.StatusOK, balanceResponse{Confirmed: confirmed, Unconfirmed: unconfirmed})
}

func (s *Server) claimWallet(filename, password string) error {
	log.Println("claimWallet")
	s.mutex.Lock()
	if filename == s.openedWalletFile {
		return nil
	}
	err := s.monero.OpenWallet(filename, password)
	if err != nil {
		s.mutex.Unlock()
		return err
	}
	s.openedWalletFile = filename

	return nil
}

func (s *Server) releaseWallet() {
	log.Println("releaseWallet")
	s.mutex.Unlock()
}

func (s *Server) walletMiddleware(next http.Handler) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		c := r.PathValue("crypto")
		if c != "XMR" {
			response(w, http.StatusBadRequest, errorResponse{Message: "Specified cryptocurrency is not available", Error: "INCORRECT_CRYPTO"})
			return
		}

		walletFile := r.Header.Get("Wallet-File")
		if walletFile == "" {
			response(w, http.StatusBadRequest, errorResponse{Message: "Wallet file not specified", Error: "NO_WALLET_FILE"})
			return
		}
		walletPassword := r.Header.Get("Wallet-Password")

		err := s.claimWallet(walletFile, walletPassword)
		if err != nil {
			response(w, http.StatusBadRequest, errorResponse{Message: "Failed to open wallet", Error: "FAILED_TO_OPEN_WALLET"})
			return
		}

		defer s.releaseWallet()
		next.ServeHTTP(w, r)
	}
}

func enableCORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next.ServeHTTP(w, r)
	})
}

func response(w http.ResponseWriter, statusCode int, response interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(statusCode)
	if response == nil {
		return
	}
	err := json.NewEncoder(w).Encode(response)
	if err != nil {
		log.Println("[ERROR] Failed to encode to JSON")
		w.WriteHeader(http.StatusInternalServerError)
	}
}
