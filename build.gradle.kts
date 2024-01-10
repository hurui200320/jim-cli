plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "info.skyblond"
version = System.getenv("release_tag") ?: "dev"

repositories {
    mavenCentral()
}

dependencies {
    // kotlin logging and logback
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    // ktorm and sqlite
    implementation("org.jetbrains.exposed:exposed-core:0.44.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
    implementation("org.xerial:sqlite-jdbc:3.44.0.0")
    // cli things
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    // json for import and export
    implementation("com.google.code.gson:gson:2.10.1")
    // javalin for http service
    implementation("io.javalin:javalin:5.6.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
    executableDir = ""
    applicationName = "jim"
}
