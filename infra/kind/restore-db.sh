#!/bin/sh
# PostgreSQL backup'tan geri yükleme script'i.
#
# Kullanım:
#   ./infra/kind/restore-db.sh              # En son backup'ı listeler ve onay bekler
#   ./infra/kind/restore-db.sh <backup-adı> # Belirli bir backup'ı restore eder
#
# Gereksinimler:
#   - Kind cluster'ı çalışıyor olmalı
#   - infra chart'ı deploy edilmiş olmalı (helm/infra)
#   - En az bir backup alınmış olmalı
#
# UYARI: Bu işlem MEVCUT VERİYİ SİLER ve backup'tan geri yükler.
#         Production'da kullanmadan önce mutlaka test edin.

set -e

NAMESPACE="${NAMESPACE:-microservices}"
RESTORE_POD="postgres-restore-$(date -u +%Y%m%d%H%M%S)"

# ---- Backup listesini göster ----
list_backups() {
  kubectl run "$RESTORE_POD-list" --rm -i --restart=Never --image=busybox:1.36 \
    -n "$NAMESPACE" --overrides='
{
  "spec": {
    "containers": [{
      "name": "list",
      "image": "busybox:1.36",
      "command": ["sh", "-c", "echo === Mevcut Backuplar === && ls -lhS /backups/"],
      "volumeMounts": [{"name": "backups", "mountPath": "/backups"}]
    }],
    "volumes": [{
      "name": "backups",
      "persistentVolumeClaim": {"claimName": "postgres-backups"}
    }]
  }
}'
}

echo "============================================"
echo "  PostgreSQL VERİTABANI GERİ YÜKLEME"
echo "============================================"
echo ""

list_backups
echo ""

# ---- Hangi backup'ın restore edileceğini belirle ----
if [ $# -gt 0 ]; then
  BACKUP_FILE="$1"
  echo "Belirtilen backup kullanılacak: $BACKUP_FILE"
else
  # En son backup'ı bul
  BACKUP_FILE=$(kubectl run "$RESTORE_POD-find" --rm -i --restart=Never --image=busybox:1.36 \
    -n "$NAMESPACE" --overrides='
{
  "spec": {
    "containers": [{
      "name": "find",
      "image": "busybox:1.36",
      "command": ["sh", "-c", "ls -t /backups/backup-*.sql.gz 2>/dev/null | head -1"],
      "volumeMounts": [{"name": "backups", "mountPath": "/backups"}]
    }],
    "volumes": [{
      "name": "backups",
      "persistentVolumeClaim": {"claimName": "postgres-backups"}
    }]
  }
}')

  if [ -z "$BACKUP_FILE" ]; then
    echo "HATA: Hiç backup bulunamadı. Önce backup alın: ./infra/kind/backup-now.sh"
    exit 1
  fi
  BACKUP_FILE=$(basename "$BACKUP_FILE")
  echo "En son backup kullanılacak: $BACKUP_FILE"
fi

# ---- Onay ----
echo ""
echo "UYARI: BU İŞLEM MEVCUT POSTGRESQL VERİSİNİ SİLİP YERİNE '$BACKUP_FILE' DOSYASINI YÜKLEYECEK."
printf "Devam etmek için 'restore' yazın: "
read -r CONFIRM
if [ "$CONFIRM" != "restore" ]; then
  echo "İptal edildi."
  exit 0
fi

# ---- Restore öncesi mevcut durumu yedekle (güvenlik) ----
echo ""
echo "Restore öncesi mevcut verinin yedeği alınıyor..."
PRE_RESTORE_BACKUP="/backups/pre-restore-$(date -u +%Y%m%d-%H%M%S).sql.gz"
kubectl run "$RESTORE_POD-presave" --rm -i --restart=Never --image=postgres:16-alpine \
  -n "$NAMESPACE" --overrides='
{
  "spec": {
    "containers": [{
      "name": "presave",
      "image": "postgres:16-alpine",
      "env": [{
        "name": "PGPASSWORD",
        "valueFrom": {"secretKeyRef": {"name": "postgres", "key": "POSTGRES_PASSWORD"}}
      }],
      "command": ["sh", "-c", "pg_dumpall -h postgres -U postgres | gzip > '"$PRE_RESTORE_BACKUP"' && echo Pre-restore backup oluşturuldu: '"$PRE_RESTORE_BACKUP"'"],
      "volumeMounts": [{"name": "backups", "mountPath": "/backups"}]
    }],
    "volumes": [{
      "name": "backups",
      "persistentVolumeClaim": {"claimName": "postgres-backups"}
    }]
  }
}'
echo "Güvenlik backup'ı alındı: $PRE_RESTORE_BACKUP"

# ---- Mevcut veritabanlarını temizle ve restore et ----
echo ""
echo "Backup restore ediliyor: $BACKUP_FILE"

kubectl run "$RESTORE_POD" --rm -i --restart=Never --image=postgres:16-alpine \
  -n "$NAMESPACE" --overrides='
{
  "spec": {
    "containers": [{
      "name": "restore",
      "image": "postgres:16-alpine",
      "env": [{
        "name": "PGPASSWORD",
        "valueFrom": {"secretKeyRef": {"name": "postgres", "key": "POSTGRES_PASSWORD"}}
      }],
      "command": ["sh", "-c", "set -e\necho \"Mevcut bağlantıları kesip veritabanlarını temizliyoruz...\"\npsql -h postgres -U postgres -d postgres -c \"DO \\$\\$ DECLARE r RECORD; BEGIN FOR r IN (SELECT datname FROM pg_database WHERE datname NOT IN (''postgres'', ''template0'', ''template1'')) LOOP PERFORM pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = r.datname; EXECUTE ''DROP DATABASE IF EXISTS \\\"'' || r.datname || ''\\\"''; END LOOP; END \\$\\$;\"\necho \"Backup restore ediliyor...\"\ngunzip -c /backups/'"$BACKUP_FILE"' | psql -h postgres -U postgres -d postgres\necho \"Restore tamamlandı.\""],
      "volumeMounts": [{"name": "backups", "mountPath": "/backups"}]
    }],
    "volumes": [{
      "name": "backups",
      "persistentVolumeClaim": {"claimName": "postgres-backups"}
    }]
  }
}'

echo ""
echo "============================================"
echo "  RESTORE TAMAMLANDI"
echo "============================================"
echo "Restore edilen backup: $BACKUP_FILE"
echo "Restore öncesi güvenlik backup'ı: $PRE_RESTORE_BACKUP"
echo ""
echo "Servislerin yeniden bağlanması için restart edilmesi gerekebilir:"
echo "  kubectl rollout restart deployment -n microservices"
