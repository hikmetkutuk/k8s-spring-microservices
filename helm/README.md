# Helm Chart'ları

```
helm/
├── charts/
│   ├── common/                # Library chart — paylaşılan template helper'lar (_helpers.tpl)
│   ├── auth-service/
│   ├── user-service/
│   ├── catalog-service/
│   ├── task-service/
│   ├── notification-service/
│   └── api-gateway/
└── umbrella/                  # Tüm servisleri tek `helm install` ile ayağa kaldıran meta-chart
```

Altyapı (Postgres/Redis/Kafka) bu maddenin kapsamında değil — madde 13'te Kind/Minikube
cluster'ına ayrı olarak Helm ile kurulacak. `config.dbHost`/`config.redisHost`/
`config.kafkaBootstrapServers` değerleri o kurulumun servis adlarına göre override edilecek.

## Hassas değerler (secrets)

Hiçbir chart'ın `values.yaml`'ında DB parolası veya JWT anahtarı gibi hassas veri
**yoktur** — `secrets.dbUsername`/`secrets.dbPassword` ve auth-service'in
`jwtKeys.privateKey`/`jwtKeys.publicKey` alanları varsayılan olarak boştur ve
`required` ile korunur; kurulum sırasında sağlanmazsa Helm hata verir.

Local kurulum örneği:

```
helm dependency update helm/umbrella

helm install platform helm/umbrella \
  --set auth-service.secrets.dbUsername=auth_service \
  --set auth-service.secrets.dbPassword=<local-only-password> \
  --set-file auth-service.jwtKeys.privateKey=services/auth-service/src/main/resources/keys/private_key.pem \
  --set-file auth-service.jwtKeys.publicKey=services/auth-service/src/main/resources/keys/public_key.pem \
  --set user-service.secrets.dbUsername=user_service \
  --set user-service.secrets.dbPassword=<local-only-password> \
  --set catalog-service.secrets.dbUsername=catalog_service \
  --set catalog-service.secrets.dbPassword=<local-only-password> \
  --set task-service.secrets.dbUsername=task_service \
  --set task-service.secrets.dbPassword=<local-only-password> \
  --set notification-service.secrets.dbUsername=notification_service \
  --set notification-service.secrets.dbPassword=<local-only-password>
```

Tekrar tekrar `--set` yazmamak için bu değerleri `helm/umbrella/values-secrets.local.yaml`
adında bir dosyaya yazıp `-f` ile geçebilirsiniz — bu dosya adı `.gitignore`'da zaten
hariç tutulmuştur (`helm/**/values-secrets*.yaml`), asla commit edilmemelidir.

Production'da bu değerler `--set`/dosya yerine bir Secret manager (Vault, External
Secrets Operator vb.) veya CI/CD pipeline secret injection ile sağlanmalıdır.

## JWT public key paylaşımı

`auth-service` chart'ı kendi `jwt-keys` Secret'ını oluşturur (private + public key).
Diğer 5 servis, aynı ad altındaki (`jwtKeys.publicKeySecretName`, varsayılan `jwt-keys`)
Secret'ın sadece `public_key.pem` alanını mount eder — bkz. todo.md madde 4.2'deki
production paylaşım planı.

## Doğrulama

```
helm dependency update helm/charts/<servis>
helm lint helm/charts/<servis> --set secrets.dbUsername=x --set secrets.dbPassword=y
helm template test helm/charts/<servis> --set secrets.dbUsername=x --set secrets.dbPassword=y
```

Umbrella için tüm alt chart secret'larını aynı anda vermeniz gerekir (bkz. yukarıdaki
kurulum örneği).
