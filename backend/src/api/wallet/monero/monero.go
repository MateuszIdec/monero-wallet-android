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
	Offline             bool `json:"offline"`
	OutgoingConnections int  `json:"outgoing_connections_count"`
	Height              int  `json:"height"`
	TargetHeight        int  `json:"target_height"`
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

type addressData struct {
	Address         string `json:"address"`
	AddressIndex    uint64 `json:"address_index"`
	TotalBalance    uint64 `json:"balance"`
	UnlockedBalance uint64 `json:"unlocked_balance"`
}

type getBalanceResponse struct {
	TotalBalance    uint64        `json:"balance"`
	UnlockedBalance uint64        `json:"unlocked_balance"`
	PerSubaddress   []addressData `json:"per_subaddress"`
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
	return &wallet.Status{Name: "monerod_monero-rpc", Connected: !info.Offline, Connections: info.OutgoingConnections, VerificationProgress: float64(info.Height)}, nil
}

const atomicToXMR float64 = 1e12

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
			confirmed := float64(a.UnlockedBalance) / atomicToXMR
			unconfirmed := float64(a.TotalBalance-a.UnlockedBalance) / atomicToXMR

			return confirmed, unconfirmed, nil
		}
	}

	return 0, 0, errors.New("address not found")
}

type totalBalanceResponse struct {
	TotalBalance    uint64 `json:"balance"`
	UnlockedBalance uint64 `json:"unlocked_balance"`
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

	return float64(balance.UnlockedBalance) / atomicToXMR,
		float64(balance.TotalBalance-balance.UnlockedBalance) / atomicToXMR, nil
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

func (m *Monero) OpenWallet(filename string) error {
	_, err := wallet.RPC(m.rpcUrl, "0", "open_wallet", map[string]any{"filename": filename})
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

type transactionsResponse struct {
	In      []Transaction `json:"in"`
	Out     []Transaction `json:"out"`
	Pending []Transaction `json:"pending"`
}

func (m *Monero) Transactions() ([]Transaction, error) {
	r, err := wallet.RPC(m.rpcUrl, "0", "get_transfers", map[string]any{"in": true, "out": true, "pending": true})
	if err != nil {
		return nil, err
	}
	var data transactionsResponse
	err = json.Unmarshal(r, &data)
	if err != nil {
		return nil, err
	}

	var result []Transaction
	result = append(result, data.In...)
	result = append(result, data.Out...)

	return result, nil
}

func xmrToAtomic(xmr float64) uint64 {
	return uint64(xmr * 1e12)
}