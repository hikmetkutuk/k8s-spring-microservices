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

## 3. PostgreSQL Backup ve Restore

`helm/infra` chart'ı otomatik günlük backup için bir **CronJob** içerir
(`backup-cronjob.yaml`). Backup'lar ayrı bir PVC'de (`postgres-backups`) saklanır.

### Backup stratejisi

| Özellik | Ayar |
|---|---|
| Schedule | Her gece 02:00 UTC (`postgres.backup.schedule`) |
| Retention | Son 7 gün (`postgres.backup.retentionDays`) |
| Yöntem | `pg_dumpall \| gzip` |
| Depolama | PVC (`postgres-backups`, `postgres.backup.storageSize`, varsayılan 1Gi) |
| Devre dışı bırakma | `postgres.backup.enabled: false` |

**Production notu:** Bu pg_dump tabanlı yaklaşım local/dev ortamlar için yeterlidir.
Production'da aşağıdakiler de değerlendirilmelidir:
- **Managed servis backup'ı** (RDS automated backup, Cloud SQL export)
- **PVC snapshot** (`VolumeSnapshot` — CSI driver gerektirir, Kind'da yok)
- **WAL archiving** (`pg_basebackup` + `wal-g`/`barman`/`pgBackRest` ile PITR)
- Backup'ların **cluster dışına kopyalanması** (S3, GCS, Azure Blob)

### Manuel backup tetikleme

```bash
./infra/kind/backup-now.sh
```

CronJob'tan bir Job oluşturup hemen çalıştırır, log'ları gösterir.

### Backup'ları listeleme

```bash
kubectl run backup-ls --rm -i --restart=Never --image=busybox:1.36 \
  -n microservices --overrides='
{
  "spec": {
    "containers": [{
      "name": "ls",
      "image": "busybox:1.36",
      "command": ["sh", "-c", "ls -lh /backups/"],
      "volumeMounts": [{"name": "backups", "mountPath": "/backups"}]
    }],
    "volumes": [{
      "name": "backups",
      "persistentVolumeClaim": {"claimName": "postgres-backups"}
    }]
  }
}'
```

### Restore

```bash
# En son backup'ı listeler, onay bekler ve restore eder:
./infra/kind/restore-db.sh

# Belirli bir backup'ı restore eder:
./infra/kind/restore-db.sh backup-20260101-020000.sql.gz
```

Restore işlemi:
1. Mevcut backup'ları listeler
2. Restore öncesi mevcut veriyi güvenlik amaçlı yedekler (`pre-restore-*.sql.gz`)
3. Tüm servis veritabanlarını drop eder
4. Seçilen backup'ı `psql` ile geri yükler

Restore sonrası servislerin yeniden bağlanması için:
```bash
kubectl rollout restart deployment -n microservices
```

### Backup PVC'sini büyütme

```bash
kubectl patch pvc postgres-backups -n microservices \
  -p '{"spec":{"resources":{"requests":{"storage":"5Gi"}}}}'
```

(Depolama sınıfı `allowVolumeExpansion: true` desteklemelidir.)

## 4. Uygulama servislerini deploy etme

Servis image'larını build edip local registry'ye push etmek için:

```
./gradlew jib
```

Ardından `helm/umbrella` chart'ı ile deploy — bkz. `helm/README.md`. Bu adım madde 18
(uçtan uca doğrulama) kapsamında ele alınacak.

## 5. Temizlik

```
kind delete cluster --name k8s-spring-microservices
docker rm -f kind-registry
```
