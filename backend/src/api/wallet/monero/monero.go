package monero

import (
	"api/wallet"
	"encoding/json"
	"errors"
	"log"
)

type Monero struct {
	monerodUrl string
	rpcUrl     string
}

func New(monerodUrl string, rpcUrl string) *Monero {
	w := &Monero{monerodUrl: monerodUrl + "/json_rpc", rpcUrl: rpcUrl + "/json_rpc"}
	return w
}

type networkInfo struct {
	Offline             bool   `json:"offline"`
	OutgoingConnections uint64 `json:"outgoing_connections_count"`
	Height              uint64 `json:"height"`
	TargetHeight        uint64 `json:"target_height"`
}

func (m *Monero) networkInfo() (*networkInfo, error) {
	body, err := wallet.RPC(m.monerodUrl, "0", "get_info", nil)
	if err != nil {
		return nil, err
	}

	var result networkInfo
	err = json.Unmarshal(body, &result)
	if err != nil {
		return nil, err
	}

	return &result, nil
}

type daemonHeight struct {
	Height uint64 `json:"height"`
}

func (m *Monero) deamonInfo() (uint64, error) {
	body, err := wallet.RPC(m.rpcUrl, "0", "get_height", nil)
	if err != nil {
		return 0, err
	}

	var result daemonHeight
	err = json.Unmarshal(body, &result)
	if err != nil {
		return 0, err
	}

	return result.Height, nil
}

type addressData struct {
	Address         string `json:"address"`
	AddressIndex    uint64 `json:"addressIndex"`
	TotalBalance    uint64 `json:"balance"`
	UnlockedBalance uint64 `json:"unlockedBalance"`
}

type getBalanceResponse struct {
	TotalBalance    uint64        `json:"balance"`
	UnlockedBalance uint64        `json:"unlockedBalance"`
	PerSubaddress   []addressData `json:"perSubaddress"`
}

func (m *Monero) getBalance(addressIndexes []uint64) (*getBalanceResponse, error) {
	body, err := wallet.RPC(m.rpcUrl, "0", "get_balance", map[string]any{"account_index": 0, "address_indices": addressIndexes})
	if err != nil {
		return nil, err
	}

	var result getBalanceResponse
	err = json.Unmarshal(body, &result)
	if err != nil {
		return nil, err
	}

	return &result, nil
}

type getAddressResponse struct {
	Addresses []addressData `json:"addresses"`
}

func (m *Monero) addressIndex(address string) (uint64, error) {
	body, err := wallet.RPC(m.rpcUrl, "0", "get_address", map[string]any{"account_index": 0})
	if err != nil {
		return 0, err
	}

	var result getAddressResponse
	err = json.Unmarshal(body, &result)
	if err != nil {
		return 0, err
	}

	for _, a := range result.Addresses {
		if address == a.Address {
			return a.AddressIndex, nil
		}
	}
	return 0, errors.New("address not found")
}

func (m *Monero) Status() (*wallet.Status, error) {
	info, err := m.networkInfo()
	if err != nil {
		return nil, err
	}
	height, err := m.deamonInfo()
	if err != nil {
		return nil, err
	}

	return &wallet.Status{Name: "monero", Connected: !info.Offline, Connections: info.OutgoingConnections,
		VerificationProgress: float64(info.Height), WalletHeight: height}, nil
}

func (m *Monero) Balance(address string) (confirmed, unconfirmed float64, err error) {
	result, err := m.addressIndex(address)
	log.Printf("Index for address %s is %d", address, result)
	if err != nil {
		return 0, 0, err
	}

	addresses, err := m.getBalance([]uint64{result})
	if err != nil {
		return 0, 0, err
	}

	for _, a := range addresses.PerSubaddress {
		if address == a.Address {
			confirmed := atomicToXmr(a.UnlockedBalance)
			unconfirmed := atomicToXmr(a.TotalBalance - a.UnlockedBalance)

			return confirmed, unconfirmed, nil
		}
	}

	return 0, 0, errors.New("address not found")
}

type totalBalanceResponse struct {
	TotalBalance    uint64 `json:"balance"`
	UnlockedBalance uint64 `json:"unlockedBalance"`
}

func (m *Monero) TotalBalance() (confirmed, unconfirmed float64, err error) {
	result, err := wallet.RPC(m.rpcUrl, "0", "get_balance", map[string]any{"account_index": 0})
	if err != nil {
		return 0, 0, err
	}

	var balance totalBalanceResponse
	err = json.Unmarshal(result, &balance)
	if err != nil {
		return 0, 0, err
	}

	return atomicToXmr(balance.UnlockedBalance),
		atomicToXmr(balance.TotalBalance - balance.UnlockedBalance), nil
}

type address struct {
	Address string `json:"address"`
}

func (m *Monero) NewAddress() (string, error) {
	body, err := wallet.RPC(m.rpcUrl, "0", "create_address", map[string]any{"account_index": 0})
	if err != nil {
		return "", err
	}

	var result address
	err = json.Unmarshal(body, &result)
	if err != nil {
		return "", err
	}

	return result.Address, nil
}

type Addresses struct {
	Addresses []address `json:"addresses"`
}

func (m *Monero) Addresses() (*Addresses, error) {
	body, err := wallet.RPC(m.rpcUrl, "0", "get_address", map[string]any{"account_index": 0})
	if err != nil {
		return nil, err
	}

	var result Addresses
	err = json.Unmarshal(body, &result)
	if err != nil {
		return nil, err
	}

	return &result, nil
}

func (m *Monero) CreateWallet(filename string) error {
	_, err := wallet.RPC(m.rpcUrl, "0", "create_wallet", map[string]any{"filename": filename, "language": "English"})
	if err != nil {
		return err
	}
	return nil
}

func (m *Monero) OpenWallet(filename, password string) error {
	_, err := wallet.RPC(m.rpcUrl, "0", "open_wallet", map[string]any{"filename": filename, "password": password})
	if err != nil {
		return err
	}
	return nil
}

func (m *Monero) CloseWallet() error {
	_, err := wallet.RPC(m.rpcUrl, "0", "close_wallet", nil)
	if err != nil {
		return err
	}
	return nil
}

func (m *Monero) Transfer(address string, amount float64) error {
	atomicAmount := xmrToAtomic(amount)
	_, err := wallet.RPC(m.rpcUrl, "0", "transfer", map[string]any{"destinations": []any{map[string]any{"address": address, "amount": atomicAmount}}, "priority": 0})
	if err != nil {
		return err
	}

	return nil
}

type Transaction struct {
	Address       string `json:"address"`
	Amount        uint64 `json:"amount"`
	Type          string `json:"type"`
	Fee           uint64 `json:"fee"`
	Confirmations uint64 `json:"confirmations"`
}

type transactionResponse struct {
	Address       string  `json:"address"`
	Amount        float64 `json:"amount"`
	Type          string  `json:"type"`
	Fee           float64 `json:"fee"`
	Confirmations uint64  `json:"confirmations"`
}

type TransactionsResponse struct {
	In      []transactionResponse `json:"in"`
	Out     []transactionResponse `json:"out"`
	Pending []transactionResponse `json:"pending"`
}

func transactionToResponse(t *Transaction) transactionResponse {
	return transactionResponse{Address: t.Address, Amount: atomicToXmr(t.Amount), Type: t.Type, Fee: atomicToXmr(t.Fee), Confirmations: t.Confirmations}
}

type transactionsData struct {
	In      []Transaction `json:"in"`
	Out     []Transaction `json:"out"`
	Pending []Transaction `json:"pending"`
}

func (m *Monero) Transactions() (TransactionsResponse, error) {
	r, err := wallet.RPC(m.rpcUrl, "0", "get_transfers", map[string]any{"in": true, "out": true, "pending": true})
	if err != nil {
		return TransactionsResponse{}, err
	}
	var data transactionsData
	err = json.Unmarshal(r, &data)
	if err != nil {
		return TransactionsResponse{}, err
	}

	var result TransactionsResponse
	for x := 0; x < len(data.In); x++ {
		result.In = append(result.In, transactionToResponse(&data.In[x]))
	}
	for x := 0; x < len(data.Out); x++ {
		result.Out = append(result.Out, transactionToResponse(&data.Out[x]))
	}
	for x := 0; x < len(data.Pending); x++ {
		result.Pending = append(result.Pending, transactionToResponse(&data.Pending[x]))
	}

	return result, nil
}

const atomicToXMR float64 = 1e12

func xmrToAtomic(xmr float64) uint64 {
	return uint64(xmr * atomicToXMR)
}
func atomicToXmr(atomic uint64) float64 {
	return float64(atomic) / atomicToXMR
}
