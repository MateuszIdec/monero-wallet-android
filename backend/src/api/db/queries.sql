-- name: CreateAccount :exec
INSERT INTO account (hash) VALUES ($1);

-- name: MoneroWallet :one
SELECT monero_wallet FROM account WHERE hash = $1;

-- name: SetMoneroWallet :exec
UPDATE account
SET monero_wallet = $2
WHERE hash = $1;