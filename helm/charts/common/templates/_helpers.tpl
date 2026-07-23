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
