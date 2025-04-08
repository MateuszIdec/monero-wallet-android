package server

import (
	"context"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"net/http"

	"github.com/tyler-smith/go-bip39"
)

func (s *Server) authMiddleware(next http.Handler) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		token := r.Header.Get("Authorization")
		filename, password, err := s.accountMoneroWallet(token)
		if err != nil {
			response(w, http.StatusUnauthorized, errorResponse{Message: "Unauthorized"})
			return
		}

		r.Header.Set("Wallet-File", filename)
		r.Header.Set("Wallet-Password", password)
		next.ServeHTTP(w, r)
	}
}

func newAccountData() (mnemonic string, entropy string, hash []byte, err error) {
	entropyBytes, err := bip39.NewEntropy(256)
	if err != nil {
		return "", "", nil, err
	}
	mnemonic, err = bip39.NewMnemonic(entropyBytes)
	if err != nil {
		return "", "", nil, err
	}
	h := sha256.Sum256(entropyBytes)
	hash = h[:]

	return mnemonic, base64.StdEncoding.EncodeToString(entropyBytes), hash, nil
}

func entropyFromMnemonic(mnemonic string) (string, error) {
	entropyBytes, err := bip39.EntropyFromMnemonic(mnemonic)
	if err != nil {
		return "", err
	}

	return base64.StdEncoding.EncodeToString(entropyBytes), nil
}

func hash(data []byte) []byte {
	h := sha256.Sum256(data)
	hh := h[:]
	return hh
}

func (s *Server) accountMoneroWallet(entropy string) (file string, password string, err error) {
	if entropy == s.demo.Token {
		return s.demo.File, s.demo.Password, nil
	}
	entropyBytes, err := base64.StdEncoding.DecodeString(entropy)
	if err != nil {
		return "", "", err
	}
	h := hash(entropyBytes)
	filename, err := s.q.MoneroWallet(context.Background(), h)
	if err != nil {
		return "", "", err
	}

	if !filename.Valid {
		return "", "", errors.New("wallet file is not set")
	}

	return filename.String, entropy, err
}
