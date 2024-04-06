plugins {
    id("java")
    kotlin("jvm") version(libs.versions.kotlin.get())
    alias(libs.plugins.crowdin)
    alias(libs.plugins.pv.entrypoints)
}

dependencies {
    compileOnly(libs.pv)
    annotationProcessor(libs.lombok)
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
