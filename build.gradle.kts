plugins {
    id("java")
    kotlin("jvm") version("1.8.22")
    id("su.plo.crowdin.plugin") version("1.0.2-SNAPSHOT")
    id("su.plo.voice.plugin.entrypoints") version("1.0.2-SNAPSHOT")
}

group = "su.plo"
version = "1.1.0"

dependencies {
    compileOnly("su.plo.voice.api:server:2.1.0-SNAPSHOT")

    annotationProcessor("org.projectlombok:lombok:1.18.24")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.plasmoverse.com/snapshots")
}

crowdin {
    projectId = "plasmo-voice-addons"
    sourceFileName = "server/priority.toml"
    resourceDir = "priority/languages"
    createList = true
}

tasks {
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    }
}
