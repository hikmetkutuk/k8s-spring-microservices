plugins {
    id("java")
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.diffplug.spotless") version "8.8.0"
    id("com.google.cloud.tools.jib") version "3.5.4" apply false
}

allprojects {
    group = property("group") as String
    version = property("version") as String

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(property("javaVersion").toString().toInt()))
        }
    }

    // Mockito'nun inline mock maker'ı JDK'ya kendi kendine agent olarak attach ediyor;
    // bu, gelecekteki JDK sürümlerinde kaldırılacak (JEP 451). Mockito'nun önerdiği gibi
    // agent'ı build'de explicit tanımlıyoruz (bkz. https://github.com/mockito/mockito/issues/3111).
    val mockitoAgent = configurations.create("mockitoAgent")

    tasks.withType<Test> {
        useJUnitPlatform()
        doFirst {
            if (!mockitoAgent.isEmpty) {
                jvmArgs("-javaagent:${mockitoAgent.singleFile}")
            }
        }
    }

    dependencies {
        mockitoAgent("org.mockito:mockito-core:${property("mockitoVersion")}") {
            isTransitive = false
        }
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()
            target("src/**/*.java")
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    // Sadece çalıştırılabilir servisler (Spring Boot plugin'i uygulanmış modüller) image
    // build eder; libs:common bir kütüphane olduğu için Jib'e dahil edilmez.
    plugins.withId("org.springframework.boot") {
        apply(plugin = "com.google.cloud.tools.jib")

        configure<com.google.cloud.tools.jib.gradle.JibExtension> {
            from {
                // Digest ile sabitlenmiş, multi-arch (amd64/arm64) manifest list — "latest" kullanılmıyor.
                image = property("jibBaseImage") as String
                platforms {
                    platform {
                        architecture = "amd64"
                        os = "linux"
                    }
                    platform {
                        architecture = "arm64"
                        os = "linux"
                    }
                }
            }
            to {
                // Tag doğrudan image string'ine gömülü: Jib, to.image tag'sizse otomatik olarak
                // ekstra bir ":latest" da push eder — CLAUDE.md'nin yasakladığı davranış budur.
                image = "${findProperty("imageRegistry") ?: "localhost:5000"}/${project.name}:${project.version}"
                auth {
                    username = System.getenv("REGISTRY_USERNAME")
                    password = System.getenv("REGISTRY_PASSWORD")
                }
            }
            // Local Kind registry düz HTTP üzerinden çalışır (bkz. infra/kind/create-cluster.sh).
            // CI/production'da gerçek bir HTTPS registry'ye push edilirken bu -PjibAllowInsecureRegistries=false
            // ile kapatılmalı (varsayılan değer sadece local dev'i hedefler, bkz. gradle.properties).
            setAllowInsecureRegistries((findProperty("jibAllowInsecureRegistries") as String?)?.toBoolean() ?: false)
            container {
                // Reproducible build: sabit epoch zaman damgası, aynı kaynak → bit-bit aynı image.
                creationTime.set("EPOCH")
            }
        }
    }
}

tasks.register<Copy>("installGitHooks") {
    description = "githooks/ altındaki hook script'lerini .git/hooks içine kurar ve çalıştırılabilir yapar."
    group = "git hooks"

    from("githooks")
    into(".git/hooks")
    filePermissions {
        unix("rwxr-xr-x")
    }
}

