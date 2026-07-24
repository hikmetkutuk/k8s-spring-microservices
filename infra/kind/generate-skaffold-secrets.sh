#!/bin/sh
# skaffold.yaml, her servis chart'ının yanında (gitignore'lu: helm/**/values-secrets*.yaml)
# bir values-secrets.yaml dosyası bekliyor — bu script onları sıfırdan üretir.
# Sadece local Kind geliştirme ortamı içindir; production'da Secret'lar Vault/K8s Secret ile yönetilir.

set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
DEV_PASSWORD="localdevpw"

write_secrets_file() {
  service="$1"
  db_username="$2"
  cat > "$ROOT_DIR/helm/charts/$service/values-secrets.yaml" << EOF
# Yerel geliştirme sırları — skaffold.yaml tarafından kullanılır, gitignore'lu.
# Üretmek için: infra/kind/generate-skaffold-secrets.sh
secrets:
  dbUsername: $db_username
  dbPassword: $DEV_PASSWORD
EOF
}

for pair in "user-service:user_service" "catalog-service:catalog_service" \
  "task-service:task_service" "notification-service:notification_service"; do
  service="${pair%%:*}"
  db_username="${pair##*:}"
  write_secrets_file "$service" "$db_username"
  echo "yazıldı: helm/charts/$service/values-secrets.yaml"
done

# auth-service ayrıca RS256 anahtar çifti gerektiriyor (jwtKeys.privateKey/publicKey).
TMP_DIR="$(mktemp -d)"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMP_DIR/private_key.pem"
openssl rsa -pubout -in "$TMP_DIR/private_key.pem" -out "$TMP_DIR/public_key.pem"

{
  echo "# Yerel geliştirme sırları — skaffold.yaml tarafından kullanılır, gitignore'lu."
  echo "# Üretmek için: infra/kind/generate-skaffold-secrets.sh"
  echo "secrets:"
  echo "  dbUsername: auth_service"
  echo "  dbPassword: $DEV_PASSWORD"
  echo ""
  echo "jwtKeys:"
  echo "  privateKey: |"
  sed 's/^/    /' "$TMP_DIR/private_key.pem"
  echo "  publicKey: |"
  sed 's/^/    /' "$TMP_DIR/public_key.pem"
} > "$ROOT_DIR/helm/charts/auth-service/values-secrets.yaml"
echo "yazıldı: helm/charts/auth-service/values-secrets.yaml"

rm -rf "$TMP_DIR"

echo "Tamamlandı. Artık 'skaffold dev' / 'skaffold run' çalıştırılabilir."
