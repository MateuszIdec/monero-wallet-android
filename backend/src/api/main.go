package main

import (
	"api/db/models"
	"api/server"
	"api/wallet/monero"
	"context"
	"fmt"
	"log"
	"os"
	"strconv"

	"github.com/jackc/pgx/v5/pgxpool"
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

	dbUser := getEnv("POSTGRES_USER", "")
	if dbUser == "" {
		log.Fatal("[ERROR] DB user has to be specified")
	}
	dbPassword := getEnv("POSTGRES_PASSWORD", "")
	if dbPassword == "" {
		log.Fatal("[ERROR] DB password has to be specified")
	}
	dbName := getEnv("POSTGRES_DB", "")
	if dbName == "" {
		log.Fatal("[ERROR] DB name has to be specified")
	}
	dbHost := getEnv("POSTGRES_HOST", "")
	if dbHost == "" {
		log.Fatal("[ERROR] DB name has to be specified")
	}
	dbPortString := getEnv("POSTGRES_PORT", "")
	if dbPortString == "" {
		log.Fatal("[ERROR] DB port has to be specified")
	}
	dbPort, err := strconv.ParseInt(dbPortString, 10, 0)
	if err != nil {
		log.Fatal("[ERROR] DB port has to be a number")
	}

	db, err := pgxpool.New(context.Background(), postgresConnString(dbUser, dbPassword, dbHost, int(dbPort), dbName))
	if err != nil {
		log.Fatal("[ERROR] Failed to connect with postgresql: " + err.Error())
	}
	defer db.Close()

	queries := models.New(db)

	m := monero.New(monerodURL, moneroRpcURL)
	s := server.New(queries, m, server.DemoWallet{Token: demoToken, File: demoWallet, Password: demoWalletPassword})

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

func postgresConnString(user string, pass string, host string, port int, dbName string) string {
	return fmt.Sprintf("postgres://%s:%s@%s:%d/%s", user, pass, host, port, dbName)
}
