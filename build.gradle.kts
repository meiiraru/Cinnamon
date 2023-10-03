import org.lwjgl.Lwjgl
import org.lwjgl.Lwjgl.implementation
import org.lwjgl.sonatype

plugins {
    id("java")
    id("org.lwjgl.plugin") version "0.0.34"
}

group = "io.github.sheep_may"
version = "1.0"

repositories {
    mavenCentral()
    sonatype()
}

dependencies {
    implementation(Lwjgl.Preset.minimalOpenGL)
    implementation("org.joml:joml:1.10.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map(::zipTree))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from("LICENSE.md")

    manifest.attributes["Main-Class"] = "mayo.Main"
}