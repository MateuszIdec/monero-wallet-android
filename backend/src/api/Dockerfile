
FROM golang:1.22 AS builder
ENV TZ="Europe/Warsaw"

WORKDIR /app

COPY . .
RUN go mod download
RUN CGO_ENABLED=0 GOOS=linux go build -o /api

FROM alpine:3.19

WORKDIR /app

COPY --from=builder /api /

EXPOSE 3000
ENTRYPOINT ["/api"]