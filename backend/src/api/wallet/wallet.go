package wallet

import (
	"bytes"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net/http"
)

type Status struct {
	Name                 string
	Connected            bool
	Connections          int
	VerificationProgress float64
}

type errorRes struct {
	Error *errorResData `json:"error"`
}
type errorResData struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
}

type resultRes struct {
	Result json.RawMessage `json:"result"`
}

func RPC(rpcUrl string, id string, method string, params map[string]any) ([]byte, error) {
	var p string
	l := len(params)

	if l == 0 {
		p = "{}"
	} else {
		r, err := json.Marshal(params)
		if err != nil {
			return nil, errors.New("failed to marshal params: " + err.Error())
		}
		p = string(r)
	}

	payloadString := `{"jsonrpc": "2.0", "id":"` + id + `" ,"method":"` + method + `", "params":` + p + `}`
	payload := []byte(payloadString)
	log.Println(payloadString)
	res, err := http.Post(rpcUrl, "application/json", bytes.NewReader(payload))
	if err != nil {
		return nil, err
	}

	body, err := io.ReadAll(res.Body)
	if err != nil {
		return nil, errors.New("failed to read body: " + err.Error())
	}

	log.Println("Response")
	log.Println(res.StatusCode)
	log.Println(string(body))

	var walletError errorRes
	err = json.Unmarshal(body, &walletError)
	if err != nil {
		return nil, errors.New("failed to unmarshal json: " + err.Error())
	}

	if walletError.Error != nil {
		return nil, errors.New("wallet received error with message: " + walletError.Error.Message)
	}

	var result resultRes
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, errors.New("failed to parse result data: " + err.Error())
	}

	if result.Result == nil {
		return nil, errors.New("walletCall: result data is nil")
	}

	return result.Result, nil
}