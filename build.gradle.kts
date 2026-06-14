plugins {
    kotlin("jvm") version "2.1.10"
}

group = "io.github.mtbarr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT")
    compileOnly(kotlin("stdlib"))

    compileOnly("org.openjdk.nashorn:nashorn-core:15.7")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")

    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.mongodb:mongodb-driver-sync:5.2.1")
    compileOnly("io.github.cdimascio:dotenv-java:3.0.0")
    compileOnly("redis.clients:jedis:5.2.0")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
}

kotlin {
    jvmToolchain(21)
}
