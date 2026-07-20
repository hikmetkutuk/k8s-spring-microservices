plugins {
    id("java")
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.diffplug.spotless") version "8.8.0"
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

    tasks.withType<Test> {
        useJUnitPlatform()
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

