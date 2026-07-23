plugins {
    id("java-library")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
    }
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-security")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")

    api("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // Distributed tracing (OpenTelemetry -> Tempo). Servlet/WebFlux'tan bağımsız,
    // tüm servislerde (api-gateway dahil) sorunsuz çalışır.
    // Spring Boot 4, autoconfigure'ı modüllere böldüğü için (bkz. spring-boot-flyway emsali)
    // sadece bridge/exporter jar'ları yeterli değil — tracing/OTLP bean'lerini asıl bu iki
    // modül wiring ediyor.
    api("org.springframework.boot:spring-boot-micrometer-tracing")
    api("org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry")
    api("io.micrometer:micrometer-tracing-bridge-otel")
    api("io.opentelemetry:opentelemetry-exporter-otlp")
}
