plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.6.0"
}

group = "org.intellij.sdk"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3.3")
    }
    compileOnly("com.jetbrains.intellij.java:java-execution:251.25410.159")
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        sinceBuild.set("241")
    }
}
