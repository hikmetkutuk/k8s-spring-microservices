#!/bin/sh
# Kind cluster + local Docker registry'yi kurar (kind.sigs.k8s.io/docs/user/local-registry
# ile aynı desen). Jib, image'ları bu registry'ye (localhost:5001) push eder; kind-config.yaml
# node'ların bu registry'yi çözebilmesini sağlar.
# Not: 5000 değil 5001 kullanılıyor çünkü macOS'ta AirPlay Receiver 5000 portunu dinliyor.

set -e

REGISTRY_NAME="kind-registry"
REGISTRY_PORT="5001"
CLUSTER_NAME="k8s-spring-microservices"
NAMESPACE="microservices"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ "$(docker inspect -f '{{.State.Running}}' "$REGISTRY_NAME" 2>/dev/null || true)" = "true" ]; then
  RUNNING_PORT="$(docker inspect -f '{{ (index (index .NetworkSettings.Ports "5000/tcp") 0).HostPort }}' "$REGISTRY_NAME" 2>/dev/null || true)"
  if [ "$RUNNING_PORT" != "$REGISTRY_PORT" ]; then
    echo "HATA: $REGISTRY_NAME zaten çalışıyor ama port ${REGISTRY_PORT} değil ${RUNNING_PORT} yayınlıyor." >&2
    echo "Eski container'ı kaldırıp tekrar çalıştırın: docker rm -f $REGISTRY_NAME && $0" >&2
    exit 1
  fi
  echo "Local registry ($REGISTRY_NAME) zaten çalışıyor (port ${REGISTRY_PORT})."
else
  echo "Local registry ($REGISTRY_NAME) başlatılıyor..."
  docker run -d --restart=always -p "127.0.0.1:${REGISTRY_PORT}:5000" --name "$REGISTRY_NAME" registry:2
fi

if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  NODE_NAME="${CLUSTER_NAME}-control-plane"
  if ! docker exec "$NODE_NAME" grep -q "\"localhost:${REGISTRY_PORT}\"" /etc/containerd/config.toml 2>/dev/null; then
    echo "HATA: Kind cluster ($CLUSTER_NAME) zaten mevcut ama containerd mirror config'i localhost:${REGISTRY_PORT}'i içermiyor" >&2
    echo "(muhtemelen eski bir port ile oluşturulmuş). Cluster'ı silip tekrar çalıştırın:" >&2
    echo "  kind delete cluster --name $CLUSTER_NAME && $0" >&2
    exit 1
  fi
  echo "Kind cluster ($CLUSTER_NAME) zaten mevcut ve registry mirror'ı doğru."
else
  echo "Kind cluster ($CLUSTER_NAME) oluşturuluyor..."
  kind create cluster --name "$CLUSTER_NAME" --config "$SCRIPT_DIR/kind-config.yaml"
fi

if [ "$(docker inspect -f '{{json .NetworkSettings.Networks.kind}}' "$REGISTRY_NAME")" = "null" ]; then
  docker network connect "kind" "$REGISTRY_NAME"
fi

# KEP-1755: cluster'ın local registry'yi tanıması için kube-public'te ConfigMap.
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: local-registry-hosting
  namespace: kube-public
data:
  localRegistryHosting.v1: |
    host: "localhost:${REGISTRY_PORT}"
    help: "https://kind.sigs.k8s.io/docs/user/local-registry/"
EOF

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

echo "Hazır: cluster=$CLUSTER_NAME, namespace=$NAMESPACE, registry=localhost:${REGISTRY_PORT}"
