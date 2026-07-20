#!/bin/sh
# auth-service'in ürettiği RS256 public key'ini local geliştirme için bu servise kopyalar.
# Production'da bu paylaşım Kubernetes Secret ile yapılır (bkz. todo.md madde 4.2), bu script sadece local çalıştırma içindir.

set -e

SOURCE="$(dirname "$0")/../../auth-service/src/main/resources/keys/public_key.pem"
DEST_DIR="$(dirname "$0")/../src/main/resources/keys"

if [ ! -f "$SOURCE" ]; then
  echo "auth-service public key bulunamadı: $SOURCE"
  echo "Önce services/auth-service/scripts/generate-jwt-keys.sh çalıştırılmalı."
  exit 1
fi

mkdir -p "$DEST_DIR"
cp "$SOURCE" "$DEST_DIR/public_key.pem"

echo "Public key kopyalandı: $DEST_DIR/public_key.pem"
