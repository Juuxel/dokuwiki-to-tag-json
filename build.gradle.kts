plugins {
    java
    id("com.github.johnrengelman.shadow") version "6.+"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("info.picocli:picocli:4.6.2")
}
