#!/bin/sh
# auth-service için RS256 imzalama anahtar çiftini üretir (sadece local geliştirme amaçlı).
# Production'da bu anahtarlar Kubernetes Secret / Vault üzerinden yönetilir, repo'ya asla girmez.

set -e

KEYS_DIR="$(dirname "$0")/../src/main/resources/keys"
mkdir -p "$KEYS_DIR"

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEYS_DIR/private_key.pem"
openssl rsa -pubout -in "$KEYS_DIR/private_key.pem" -out "$KEYS_DIR/public_key.pem"

echo "RSA anahtar çifti üretildi: $KEYS_DIR"
