plugins {
    kotlin("jvm") version "2.4.0"
    application
}

group = "kz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "kz.MainKt"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
