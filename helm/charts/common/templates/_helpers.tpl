{{/*
K8s Service adları, Release adından bağımsız olarak sabit (sadece chart adı) tutulur;
böylece servisler birbirine "http://catalog-service:8083" gibi öngörülebilir, release-name'e
bağlı olmayan DNS adlarıyla erişebilir.
*/}}
{{- define "common.fullname" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "common.labels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "common.selectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
{{- end -}}

{{/*
Prometheus'un annotation-tabanlı pod discovery ile Actuator metriklerini bulması için.
Servisin service.port'unu kullanır — tüm servisler /actuator/prometheus'u aynı port'ta açar.
*/}}
{{- define "common.podAnnotations" -}}
prometheus.io/scrape: "true"
prometheus.io/path: "/actuator/prometheus"
prometheus.io/port: {{ .Values.service.port | quote }}
{{- end -}}

{{/*
Graceful shutdown — container seviyesi preStop hook'u.

Pod silindiğinde iki şey EŞZAMANLI olur: kubelet container'a SIGTERM gönderir ve
endpoint controller pod'u Service endpoint'lerinden düşürür. Bu yarış nedeniyle
uygulama kapanmaya başladıktan sonra kube-proxy hâlâ bu pod'a trafik yönlendirebilir
ve istemci "connection refused" alır. preStop'taki bekleme, SIGTERM'i geciktirerek
endpoint propagasyonunun önce tamamlanmasını sağlar.

Base image eclipse-temurin:*-alpine olduğu için busybox `sh`/`sleep` mevcut.
K8s 1.30+ yerleşik `sleep` action'ı da var ama exec formu tüm sürümlerde çalışır.
*/}}
{{- define "common.preStopHook" -}}
lifecycle:
  preStop:
    exec:
      command: ["sh", "-c", "sleep {{ .Values.gracefulShutdown.preStopSleepSeconds }}"]
{{- end -}}

{{/*
Graceful shutdown — pod seviyesi kapanma bütçesi.

Bu süre preStop hook'u BİTTİKTEN sonra değil, pod silinmeye işaretlendiği anda başlar
ve preStop + SIGTERM sonrası uygulama kapanışının tamamını kapsar. Aşılırsa kubelet
SIGKILL gönderir ve devam eden istekler/Kafka batch'i yarıda kesilir. Bu yüzden
preStopSleepSeconds + (spring.lifecycle.timeout-per-shutdown-phase × faz sayısı)
toplamından güvenli bir marjla büyük olmalı.
*/}}
{{- define "common.terminationGracePeriod" -}}
terminationGracePeriodSeconds: {{ .Values.gracefulShutdown.terminationGracePeriodSeconds }}
{{- end -}}
