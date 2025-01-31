plugins {
    java
    `java-library`
    `maven-publish`
    //id("org.springframework.boot") version "3.4.2"
}

group = "io.github.meiiraru"
version = "0.0.1"

//dependencies versions
val lwjglVersion = "3.3.6"
val jomlVersion = "1.10.8"
val log4jVersion = "2.24.3"
val gsonVersion = "2.12.0"
val jTransformsVersion = "3.1"

//lwjgl natives
val lwjglNatives = Pair(
        System.getProperty("os.name")!!,
        System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                "linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            else if (arch.startsWith("ppc"))
                "linux-ppc64le"
            else if (arch.startsWith("riscv"))
                "linux-riscv64"
            else
                "linux"

        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            "macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"

        arrayOf("Windows").any { name.startsWith(it) } ->
            if (arch.contains("64"))
                "windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            else
                "windows-x86"

        else ->
            throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    //lwjgl
    api(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    api("org.lwjgl", "lwjgl")
    api("org.lwjgl", "lwjgl-assimp")
    api("org.lwjgl", "lwjgl-egl")
    api("org.lwjgl", "lwjgl-glfw")
    api("org.lwjgl", "lwjgl-openal")
    api("org.lwjgl", "lwjgl-opengl")
    api("org.lwjgl", "lwjgl-openxr")
    api("org.lwjgl", "lwjgl-stb")

    val lwjglNatives = "natives-$lwjglNatives"
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-openxr", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)

    //extra libraries
    api("org.joml", "joml", jomlVersion)
    api("org.apache.logging.log4j", "log4j-api", log4jVersion)
    api("org.apache.logging.log4j", "log4j-core", log4jVersion)
    api("org.apache.logging.log4j", "log4j-iostreams", log4jVersion)
    api("com.google.code.gson", "gson", gsonVersion)
    api("com.github.wendykierp", "JTransforms", jTransformsVersion)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
            from(components["java"])
        }
    }
}

tasks.jar {
    //from(configurations.runtimeClasspath.get().map(::zipTree))
    //duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest.attributes["Main-Class"] = "cinnamon.Cinnamon"
    from("LICENSE.md")
    archiveClassifier.set("")
}

//tasks.bootJar {
//    from("LICENSE.md")
//    archiveClassifier.set("")
//}