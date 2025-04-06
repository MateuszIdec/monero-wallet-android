package main

import (
	"api/server"
	"api/wallet/monero"
	"log"
	"os"
	"strconv"
)

func main() {
	monerodURL := getEnv("MONEROD_URL", "http://127.0.0.1:18089")
	moneroRpcURL := getEnv("MONERO_RPC_URL", "http://127.0.0.1:3010")
	apiPort, err := strconv.Atoi(getEnv("API_PORT", "3002"))
	if err != nil {
		log.Fatalln("[ERROR] API_PORT has to be a number")
	}

	demoToken := getEnv("DEMO_TOKEN", "")
	demoWallet := getEnv("DEMO_WALLET_FILE", "")
	demoWalletPassword := getEnv("DEMO_WALLET_PASSWORD", "")

	m := monero.New(monerodURL, moneroRpcURL)
	s := server.New(m, server.DemoWallet{Token: demoToken, File: demoWallet, Password: demoWalletPassword})

	err = s.Start(uint(apiPort))
	if err != nil {
		log.Fatalln(err)
	}
}

func getEnv(key, defaultValue string) string {
	value := os.Getenv(key)

	if value == "" {
		return defaultValue
	}
	return value
}
