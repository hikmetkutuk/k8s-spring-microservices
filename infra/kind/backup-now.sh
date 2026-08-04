#!/bin/sh
# Postgres backup'ını manuel tetikler — CronJob'tan bir Job oluşturur.
# Kullanım: ./infra/kind/backup-now.sh
#
# Gereksinimler:
#   - Kind cluster'ı çalışıyor olmalı
#   - infra chart'ı deploy edilmiş olmalı (helm/infra)
#   - kubectl current context microservices namespace'ini göstermeli

set -e

NAMESPACE="${NAMESPACE:-microservices}"
JOB_NAME="postgres-backup-manual-$(date -u +%Y%m%d%H%M%S)"

echo "Manuel backup Job'ı oluşturuluyor: $JOB_NAME"

kubectl create job "$JOB_NAME" \
  --from=cronjob/postgres-backup \
  -n "$NAMESPACE"

echo "Job durumu izleniyor..."
kubectl wait --for=condition=Complete "job/$JOB_NAME" -n "$NAMESPACE" --timeout=120s

echo "Job log'ları:"
kubectl logs "job/$JOB_NAME" -n "$NAMESPACE"

echo ""
echo "Backup tamamlandı. Mevcut backup'lar:"
kubectl exec -n "$NAMESPACE" statefulset/postgres -- ls -lh /backups/ 2>/dev/null || {
  echo "Backup PVC'sini listelemek için geçici bir pod oluşturuluyor..."
  kubectl run backup-ls --rm -i --restart=Never --image=busybox:1.36 \
    -n "$NAMESPACE" --overrides='
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
}
