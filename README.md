![GitHub repo size](https://img.shields.io/github/repo-size/hikmetkutuk/k8s-spring-microservices?color=inactive&logo=github&style=for-the-badge)
![Java](https://img.shields.io/static/v1?&logo=openjdk&label=Java&message=25&color=ED8B00&style=for-the-badge)
![Spring Boot](https://img.shields.io/static/v1?&logo=springboot&label=Spring+Boot&message=4.1.0&color=6DB33F&style=for-the-badge)
![Spring Cloud Gateway](https://img.shields.io/static/v1?&logo=spring&label=Spring+Cloud+Gateway&message=2025.1.2&color=6DB33F&style=for-the-badge)
![Gradle](https://img.shields.io/static/v1?&logo=gradle&label=Gradle&message=9.6.1&color=02303A&style=for-the-badge)
![PostgreSQL](https://img.shields.io/static/v1?&logo=postgresql&label=PostgreSQL&message=16&color=4169E1&style=for-the-badge)
![Redis](https://img.shields.io/static/v1?&logo=redis&label=Redis&message=7&color=DC382D&style=for-the-badge)
![Apache Kafka](https://img.shields.io/static/v1?&logo=apachekafka&label=Kafka&message=3.8.0&color=231F20&style=for-the-badge)
![Resilience4j](https://img.shields.io/static/v1?&label=Resilience4j&message=2.3.0&color=5F259F&style=for-the-badge)
![OpenAPI](https://img.shields.io/static/v1?&logo=swagger&label=springdoc-openapi&message=3.0.3&color=85EA2D&style=for-the-badge)
![Jib](https://img.shields.io/static/v1?&logo=docker&label=Jib&message=3.5.4&color=2496ED&style=for-the-badge)
![Kind](https://img.shields.io/static/v1?&logo=kubernetes&label=Kind&message=0.32.0&color=326CE5&style=for-the-badge)
![Helm](https://img.shields.io/static/v1?&logo=helm&label=Helm&message=4.2.3&color=0F1689&style=for-the-badge)
![Skaffold](https://img.shields.io/static/v1?&logo=googlecloud&label=Skaffold&message=2.24.0&color=4285F4&style=for-the-badge)
![Prometheus](https://img.shields.io/static/v1?&logo=prometheus&label=Prometheus&message=29.19.0&color=E6522C&style=for-the-badge)
![Grafana](https://img.shields.io/static/v1?&logo=grafana&label=Grafana&message=10.5.15&color=F46800&style=for-the-badge)

## Kurulum

### 1. JDK 25

Repo, Gradle toolchain üzerinden Java 25 gerektirir. Kurulu değilse Gradle
(`org.gradle.toolchains.foojay-resolver-convention`) otomatik indirir; elle
kurulum gerekmez.

### 2. Git hook'ları

Commit öncesi otomatik kod formatlama (Spotless) için hook'u kurun:

```
./gradlew installGitHooks
```

Repo'yu her klonladığınızda bir kere çalıştırmanız yeterli.

### 3. `google-java-format` / JDK uyumsuzluğu (sadece bazı makinelerde)

Spotless'ın kullandığı `google-java-format`, bazı JDK dağıtımlarının (örn.
Homebrew'in getirdiği güncel sürümler) iç `javac` API'leriyle uyumsuz
olabilir. Böyle bir hata alırsanız, kendi makinenizdeki JDK 25 kurulum yoluna
işaret eden bir satırı **kişisel** `~/.gradle/gradle.properties`
dosyanıza ekleyin (bu dosya repo'nun bir parçası değildir):

```properties
org.gradle.java.home=/path/to/your/jdk-25
```

### 4. JWT anahtarları (auth-service)

`auth-service`, RS256 ile imzalama yapar; private/public key çifti repo'ya
dahil değildir (`.gitignore`'da `*.pem`). Üretmek için:

```
services/auth-service/scripts/generate-jwt-keys.sh
```

Diğer servislerin (örn. `user-service`) token doğrulaması için public key'e
ihtiyacı var. Local geliştirmede kopyalamak için ilgili servisin
`scripts/copy-jwt-public-key.sh` script'ini çalıştırın. Production'da bu
paylaşım Kubernetes Secret üzerinden yapılır.

## Build

```
./gradlew build
```

## Skaffold ile local Kind dev loop

Kod değişikliğinde otomatik build (Jib) + push + Helm upgrade + log/port-forward için
[Skaffold](https://skaffold.dev/) kullanılıyor (`skaffold.yaml`, kök dizinde). Önkoşul:
`infra/kind/create-cluster.sh` ve `infra/kind/deploy-infra.sh` ile Kind cluster'ı ve
altyapı (Postgres/Redis/Kafka) ayakta olmalı (bkz. `infra/README.md`).

Her servis chart'ının yanındaki `values-secrets.yaml` (DB kullanıcı adı/şifresi,
auth-service için JWT RS256 anahtar çifti) `.gitignore`'lu olduğu için repo'da yok —
`skaffold dev`/`skaffold run` çalıştırmadan önce tek seferlik üretin:

```
infra/kind/generate-skaffold-secrets.sh
```

Sonrasında:

```
skaffold dev --port-forward   # sürekli izleme: kod değişikliğinde otomatik rebuild+redeploy
skaffold run                  # tek seferlik build+deploy
```

Tek bir servisi build etmek için:

```
./gradlew :services:<servis-adı>:build
```
