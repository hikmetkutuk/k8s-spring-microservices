plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
    dependencies {
        // Spring Cloud BOM'u resilience4j-spring6'yı kendi (daha eski) sürümüne sabitliyor;
        // resilience4j-spring-boot3:2.3.0'ın beklediği sürümle eşleşmezse
        // RxJava3FallbackDecorator gibi sınıflar bulunamayıp NoClassDefFoundError atıyor.
        dependency("io.github.resilience4j:resilience4j-spring6:${property("resilience4jVersion")}")
    }
}

dependencies {
    implementation(project(":libs:common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("springdocVersion")}")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    // Spring Boot 4'te Kafka autoconfiguration'ı ayrı bir starter'a taşınmış (Flyway ile
    // aynı desen) — sadece spring-kafka yeterli değil, KafkaTemplate/KafkaOperations bean'i
    // wiring edilmiyor.
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${property("resilience4jVersion")}")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
    testImplementation("org.testcontainers:postgresql:${property("testcontainersVersion")}")
    testImplementation("org.testcontainers:kafka:${property("testcontainersVersion")}")
}
