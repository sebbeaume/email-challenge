plugins {
    kotlin("jvm") version "1.9.23"
    id("org.springframework.boot") version "3.3.2"
    id("org.jetbrains.kotlin.plugin.spring") version "1.9.23"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.ubs.codingchallenge.mailtime"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.getByName<Jar>("bootJar") {
    from("README.md") {
        into("BOOT-INF/classes/static")
    }
}
tasks.getByName<Jar>("jar") {
    enabled = false
}
