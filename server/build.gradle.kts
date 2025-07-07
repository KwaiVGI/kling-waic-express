plugins {
    kotlin("jvm") version "2.0.21"
    id("org.springframework.boot") version "3.4.5"
}

group = "com.klingai.express"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.5")
    implementation("redis.clients:jedis:6.0.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.9.10")
    implementation("org.apache.commons:commons-lang3:3.10")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

apply(plugin = "org.springframework.boot")
