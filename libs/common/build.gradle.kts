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
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
}
