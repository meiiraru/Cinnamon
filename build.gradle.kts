plugins {
    java
    `java-library`
    `maven-publish`
}

group = "com.github.meiiraru"
version = "0.3.10"
val mainClass = "cinnamon.Cinnamon"

//dependencies
val lwjglVersion = "3.3.6"
val jomlVersion = "1.10.8"
val gsonVersion = "2.13.2"

val lwjglModules = arrayOf(
    "lwjgl",
    "lwjgl-assimp",
    "lwjgl-glfw",
    "lwjgl-nfd",
    "lwjgl-openal",
    "lwjgl-opengl",
    "lwjgl-openxr",
    "lwjgl-stb"
)
val lwjglApiOnly = arrayOf(
    "lwjgl-egl"
)

val os = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        "FreeBSD" == name ->
            "freebsd"

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
    val lwjglNatives = "natives-$os"
    api(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    lwjglModules.forEach {
        api("org.lwjgl:$it")
        runtimeOnly("org.lwjgl:$it::$lwjglNatives")
    }
    //api only
    lwjglApiOnly.forEach {
        api("org.lwjgl:$it")
    }

    //extra libraries
    api("org.joml:joml:$jomlVersion") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
    api("com.google.code.gson:gson:$gsonVersion")

    //lua scripting
    api("org.luaj:luaj-jse:3.0.1")
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    dependsOn("updateVersionFile")
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set(os)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    manifest.attributes["Main-Class"] = mainClass

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.jar {
    archiveClassifier.set("")
    manifest.attributes["Main-Class"] = mainClass
    from("LICENSE.md")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}

tasks.register<DefaultTask>("updateVersionFile") {
    val projectVersion = project.provider { project.version.toString() }
    val versionFile = file("src/main/resources/resources/vanilla/version")
    outputs.file(versionFile)

    doLast {
        versionFile.writeText(projectVersion.get())
    }
}

tasks.processResources {
    dependsOn("updateVersionFile")
}