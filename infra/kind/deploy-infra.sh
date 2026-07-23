#!/bin/sh
# helm/infra chart'ını (Postgres/Redis/Kafka) kind cluster'ın "microservices" namespace'ine
# deploy eder. Parolalar script içinde YOK — ortam değişkeni olarak sağlanmalı.
# Örnek:
#   export POSTGRES_PASSWORD=... AUTH_SERVICE_DB_PASSWORD=... USER_SERVICE_DB_PASSWORD=... \
#          CATALOG_SERVICE_DB_PASSWORD=... TASK_SERVICE_DB_PASSWORD=... \
#          NOTIFICATION_SERVICE_DB_PASSWORD=...
#   ./infra/kind/deploy-infra.sh

set -e

: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD ortam değişkeni gereklidir}"
: "${AUTH_SERVICE_DB_PASSWORD:?AUTH_SERVICE_DB_PASSWORD ortam değişkeni gereklidir}"
: "${USER_SERVICE_DB_PASSWORD:?USER_SERVICE_DB_PASSWORD ortam değişkeni gereklidir}"
: "${CATALOG_SERVICE_DB_PASSWORD:?CATALOG_SERVICE_DB_PASSWORD ortam değişkeni gereklidir}"
: "${TASK_SERVICE_DB_PASSWORD:?TASK_SERVICE_DB_PASSWORD ortam değişkeni gereklidir}"
: "${NOTIFICATION_SERVICE_DB_PASSWORD:?NOTIFICATION_SERVICE_DB_PASSWORD ortam değişkeni gereklidir}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NAMESPACE="microservices"

# Parolalar --set yerine stdin'den bir values dosyası olarak geçiriliyor: --set, virgül
# içeren değerleri ek atama olarak yorumlar ve parola virgül içerirse kurulum bozulurdu.
# JSON, YAML'ın geçerli bir alt kümesi olduğu için python3'ün json.dumps'ı tüm özel
# karakterleri (virgül, tırnak, backslash) güvenle kaçırır.
VALUES_JSON=$(python3 - <<'PYEOF'
import json
import os

print(
    json.dumps(
        {
            "secrets": {
                "postgresPassword": os.environ["POSTGRES_PASSWORD"],
                "dbPasswords": {
                    "auth_service": os.environ["AUTH_SERVICE_DB_PASSWORD"],
                    "user_service": os.environ["USER_SERVICE_DB_PASSWORD"],
                    "catalog_service": os.environ["CATALOG_SERVICE_DB_PASSWORD"],
                    "task_service": os.environ["TASK_SERVICE_DB_PASSWORD"],
                    "notification_service": os.environ["NOTIFICATION_SERVICE_DB_PASSWORD"],
                },
            }
        }
    )
)
PYEOF
)

echo "$VALUES_JSON" | helm upgrade --install infra "$SCRIPT_DIR/../../helm/infra" -n "$NAMESPACE" -f -

# Sadece bu chart'ın deploy ettiği pod'ları bekle — namespace'te başka (örn. henüz kararlı
# olmayan uygulama) pod'ları varsa bu wait'i gereksiz yere timeout'a düşürmesin.
kubectl wait --for=condition=Ready pod \
  -l 'app.kubernetes.io/name in (postgres,redis,kafka)' \
  -n "$NAMESPACE" --timeout=180s

echo "Altyapı hazır: postgres, redis, kafka (namespace=$NAMESPACE)"
