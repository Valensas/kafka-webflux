import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jmailen.kotlinter") version "5.0.1"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("maven-publish")
    id("java-library")
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.1.10"
}

group = "com.valensas.data"

java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    repositories {
        if (System.getenv("CI_API_V4_URL") != null) {
            maven {
                name = "Gitlab"
                url = uri("${System.getenv("CI_API_V4_URL")}/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
                credentials(HttpHeaderCredentials::class.java) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
                authentication {
                    create("header", HttpHeaderAuthentication::class)
                }
            }
        }
        mavenLocal()
    }

    publications {
        create<MavenPublication>("artifact") {
            from(components["java"])
        }
    }
}
