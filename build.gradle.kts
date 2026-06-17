import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jmailen.kotlinter") version "5.5.0"
    id("com.github.ben-manes.versions") version "0.54.0"
    id("maven-publish")
    id("java-library")
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.spring") version "2.4.0"
    id("net.thebugmc.gradle.sonatype-central-portal-publisher") version "1.2.4"
}

group = "com.valensas.data"

java.sourceCompatibility = JavaVersion.VERSION_21

extra["kotlin.version"] = "2.4.0"

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-webflux")
    api("org.springframework.kafka:spring-kafka")
    api("io.projectreactor.kafka:reactor-kafka")
    api("com.google.protobuf:protobuf-java:4.29.3")
    api("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.15")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create("library", MavenPublication::class.java) {
            artifactId = "kafka-webflux"
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}

signing {
    val keyId = System.getenv("SIGNING_KEYID")
    val secretKey = System.getenv("SIGNING_SECRETKEY")
    val passphrase = System.getenv("SIGNING_PASSPHRASE")

    useInMemoryPgpKeys(keyId, secretKey, passphrase)
}

centralPortal {
    name = "kafka-webflux"
    username = System.getenv("SONATYPE_USERNAME")
    password = System.getenv("SONATYPE_PASSWORD")
    pom {
        name = "Kafka Webflux"
        description = "A reactive Kafka library for Spring Boot WebFlux."
        url = "https://valensas.com/"
        scm {
            url = "https://github.com/Valensas/kafka-webflux"
        }

        licenses {
            license {
                name.set("MIT License")
                url.set("https://mit-license.org")
            }
        }

        developers {
            developer {
                id.set("0")
                name.set("Valensas")
                email.set("info@valensas.com")
            }
        }
    }
}
