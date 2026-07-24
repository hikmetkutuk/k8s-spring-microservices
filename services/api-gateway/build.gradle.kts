plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    // Spring Cloud Gateway reactive (WebFlux) çalışır; libs:common'ın servlet/MVC
    // tabanlı bağımlılıkları (web/security/validation) reactive stack ile çakışacağı
    // için dışlanır. jjwt ve DTO'lar (ApiResponse/SecurityConstants) plain POJO
    // oldukları için sorunsuz kullanılabilir.
    implementation(project(":libs:common")) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-web")
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-security")
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-validation")
    }
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${property("springdocVersion")}")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}
