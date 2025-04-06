package server

import (
	"api/wallet/monero"
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"sync"
)

type Server struct {
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

func New(monero *monero.Monero, demoWallet DemoWallet) Server {
	return Server{monero: monero, demo: demoWallet}
}

func (s *Server) Start(port uint) error {
	portString := ":" + strconv.Itoa(int(port))
	auth := s.authMiddleware()
	mux := http.NewServeMux()
	handler := enableCORS(mux)

	mux.HandleFunc("/status", s.handleStatus)
	mux.Handle("/wallet/{crypto}/balance", auth(http.HandlerFunc(s.handleWalletBalance)))

	log.Printf("[INFO] Starting server on port %d", port)
	err := http.ListenAndServe(portString, handler)
	if err != nil {
		return err
	}
	return nil
}

func (s *Server) handleStatus(w http.ResponseWriter, _ *http.Request) {
	status, err := s.monero.Status()
	if err != nil {
		log.Println(err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	jsonData, err := json.Marshal(status)
	if err != nil {
		log.Println(err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	response(w, http.StatusOK, jsonData)
}

type balanceResponse struct {
	Confirmed   float64 `json:"confirmed"`
	Unconfirmed float64 `json:"unconfirmed"`
}

func (s *Server) handleWalletBalance(w http.ResponseWriter, r *http.Request) {
	walletFile := w.Header().Get("Wallet-File")
	if walletFile == "" {
		response(w, http.StatusBadRequest, errorResponse{Message: "wallet file has to be specified", Error: "NO_WALLET_FILE"})
		return
	}
	walletPassword := w.Header().Get("Wallet-Password")

	c := r.PathValue("crypto")
	if c != "XMR" {
		response(w, http.StatusBadRequest, errorResponse{Message: "Specified cryptocurrency is not available", Error: "INCORRECT_CRYPTO"})
		return
	}

	err := s.claimWallet(walletFile, walletPassword)
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to open wallet", Error: "FAILED_TO_OPEN_WALLET"})
		return
	}
	defer s.releaseWallet()

	confirmed, unconfirmed, err := s.monero.TotalBalance()
	if err != nil {
		response(w, http.StatusBadRequest, errorResponse{Message: "Failed to get wallet balance", Error: "FAILED_TO_GET_WALLET_BALANCE"})
		return
	}

	response(w, http.StatusOK, balanceResponse{Confirmed: confirmed, Unconfirmed: unconfirmed})
}

func (s *Server) claimWallet(filename, password string) error {
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
	s.mutex.Unlock()
}

func (s *Server) authMiddleware() func(next http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			token := r.Header.Get("Authorization")

			if token != "" && token == s.demo.Token {
				w.Header().Set("Wallet-File", s.demo.File)
				w.Header().Set("Wallet-Password", s.demo.Password)
				next.ServeHTTP(w, r)
				return
			}

			// Google OAuth validation

			response(w, http.StatusUnauthorized, errorResponse{Message: "Unauthorized"})
			return
		})
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
	}
}
