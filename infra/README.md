# Local Kubernetes Altyapısı

## 1. Kind cluster + local registry kurulumu

```
./infra/kind/create-cluster.sh
```

Bu script:
- `localhost:5001`'de bir Docker registry container'ı (`kind-registry`) başlatır — Jib'in
  image push ettiği hedef (bkz. `gradle.properties`'teki `imageRegistry`). 5000 değil 5001
  kullanılıyor çünkü macOS'ta AirPlay Receiver 5000 portunu dinliyor.
- Tek node'lu bir Kind cluster'ı (`k8s-spring-microservices`) oluşturur; node'lar
  `localhost:5001` referansını `kind-registry:5000`'e yönlendirecek şekilde containerd
  mirror config'i ile önceden yapılandırılmıştır (bkz. `kind-config.yaml`).
- `microservices` namespace'ini oluşturur.

Cluster zaten mevcutsa script bunu algılar ve yeniden oluşturmaz (idempotent).

## 2. Altyapı (Postgres/Redis/Kafka) deploy

```
export POSTGRES_PASSWORD=<local-only>
export AUTH_SERVICE_DB_PASSWORD=<local-only>
export USER_SERVICE_DB_PASSWORD=<local-only>
export CATALOG_SERVICE_DB_PASSWORD=<local-only>
export TASK_SERVICE_DB_PASSWORD=<local-only>
export NOTIFICATION_SERVICE_DB_PASSWORD=<local-only>
./infra/kind/deploy-infra.sh
```

`helm/infra` chart'ını kurar/günceller (`helm upgrade --install`, idempotent):
- **PostgreSQL** (tek instance, `postgres-0`): ilk açılışta 5 servisin veritabanı +
  kullanıcısını otomatik oluşturur (`helm/infra/templates/postgres-configmap-initdb.yaml`).
- **Redis**: cache/refresh-token store için.
- **Kafka** (KRaft, tek node, Zookeeper'sız): `apache/kafka:3.8.0`.

Doğrulama (manuel):

```
kubectl get pods -n microservices
kubectl exec -n microservices postgres-0 -- psql -U postgres -tAc \
  "SELECT datname FROM pg_database WHERE datname LIKE '%_service';"
kubectl exec -n microservices deploy/redis -- redis-cli ping
kubectl exec -n microservices kafka-0 -- /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

## 3. Uygulama servislerini deploy etme

Servis image'larını build edip local registry'ye push etmek için:

```
./gradlew jib
```

Ardından `helm/umbrella` chart'ı ile deploy — bkz. `helm/README.md`. Bu adım madde 18
(uçtan uca doğrulama) kapsamında ele alınacak.

## 4. Temizlik

```
kind delete cluster --name k8s-spring-microservices
docker rm -f kind-registry
```
