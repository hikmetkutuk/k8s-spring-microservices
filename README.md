![GitHub repo size](https://img.shields.io/github/repo-size/hikmetkutuk/k8s-spring-microservices?color=inactive&logo=github&style=for-the-badge)
![Java](https://img.shields.io/static/v1?&logo=openjdk&label=Java&message=25&color=ED8B00&style=for-the-badge)
![Spring Boot](https://img.shields.io/static/v1?&logo=springboot&label=Spring+Boot&message=4.1.0&color=6DB33F&style=for-the-badge)
![Gradle](https://img.shields.io/static/v1?&logo=gradle&label=Gradle&message=9.6.1&color=02303A&style=for-the-badge)
![PostgreSQL](https://img.shields.io/static/v1?&logo=postgresql&label=PostgreSQL&message=16&color=4169E1&style=for-the-badge)

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

Tek bir servisi build etmek için:

```
./gradlew :services:<servis-adı>:build
```
