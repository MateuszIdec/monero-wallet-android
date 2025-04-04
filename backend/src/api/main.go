package main

import (
	"api/wallet/monero"
	"log"
	"os"
)

func main() {
	monerodURL := getEnv("MONEROD_URL", "https://securemonero.ddns.net:18089")
	moneroRpcURL := getEnv("MONERO_RPC_URL", "http://127.0.0.1:18083")
	//token := getEnv("TOKEN", "default")

	m := monero.New(monerodURL, moneroRpcURL)
	s, err := m.Status()
	if err != nil {
		log.Fatalln(err)
	}
	log.Println(s)
}

func getEnv(key, defaultValue string) string {
	value := os.Getenv(key)

	if value == "" {
		return defaultValue
	}
	return value
}
