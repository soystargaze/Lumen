plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.10" // Soporte para Kotlin
    id("com.gradleup.shadow") version "8.3.0" // Sombreamiento del JAR
}

group = "com.erosmari"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.extendedclip.com/releases/")
    }
}

dependencies {
    // Kotlin Standard Library
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")

    // Dependencias del proyecto
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("cloud.commandframework:cloud-paper:1.8.4")
    implementation("com.zaxxer:HikariCP:5.0.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Usar Java 21
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21) // Asegura compatibilidad con Java 21
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("") // Elimina el sufijo "-all" del JAR generado
    relocate("cloud.commandframework", "com.erosmari.dicecraft.shaded.cloud") // Relocaliza paquetes para evitar conflictos
    minimize() // Optimiza el tamaño del JAR final
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
