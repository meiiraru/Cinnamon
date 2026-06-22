plugins {
    java
    `java-library`
    `maven-publish`
}

group = "com.github.meiiraru"
version = "0.4.4"
val mainClass = "cinnamon.Cinnamon"

//dependencies
val lwjglVersion = "3.4.1"
val jomlVersion = "1.10.9"
val gsonVersion = "2.14.0"

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
    api("org.joml:joml:$jomlVersion")
    api("com.google.code.gson:gson:$gsonVersion")
}

tasks.register<Jar>("sourcesJar") {
    description = "Generates a JAR containing the sources of this project"
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    dependsOn("updateVersionFile")
}

tasks.register<Jar>("javadocJar") {
    description = "Generates a JAR containing the javadocs of this project"
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks.register<Jar>("fatJar") {
    description = "Generates a JAR containing the compiled classes and all dependencies of this project"
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
    description = "Updates the version file with the current project version"
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

tasks.register<Exec>("packageApp") {
    description = "Packages the application into a platform-specific format using jpackage"
    dependsOn("fatJar")

    val isWindows = os.contains("windows")
    val appName = "${project.name}-${project.version}-$os"
    val outputDir = layout.buildDirectory.dir("dist/$appName").get().asFile
    val stagingDir = layout.buildDirectory.dir("staging").get().asFile

    val fatJarTask = tasks.named<Jar>("fatJar")
    val fatJarFileNameProvider = fatJarTask.flatMap { it.archiveFileName }
    val fatJarFileProvider = fatJarTask.flatMap { it.archiveFile }

    doFirst {
        outputDir.deleteRecursively()
        stagingDir.deleteRecursively()
        stagingDir.mkdirs()

        val fatJarFile = fatJarFileProvider.get().asFile
        fatJarFile.copyTo(File(stagingDir, fatJarFile.name), overwrite = true)
    }

    val compiler = javaToolchains.compilerFor(java.toolchain).get()
    val jdkHome = compiler.metadata.installationPath.asFile
    val jpackageBin = if (isWindows) {
        File(jdkHome, "bin/jpackage.exe").absolutePath
    } else {
        File(jdkHome, "bin/jpackage").absolutePath
    }

    val iconExtension = if (isWindows) "ico" else "png"
    val iconFile = file("src/main/resources/resources/vanilla/textures/icon.$iconExtension")

    workingDir = projectDir

    val jpackageArgs = listOf(
        jpackageBin,
        "--type", "app-image",
        "--name", appName,
        "--input", stagingDir.absolutePath,
        "--main-jar", fatJarFileNameProvider.get(),
        "--main-class", mainClass,
        "--dest", layout.buildDirectory.dir("dist").get().asFile.absolutePath,
        "--app-version", project.version.toString(),
        "--icon", iconFile.absolutePath
    )

    commandLine(jpackageArgs)

    doLast {
        stagingDir.deleteRecursively()
    }
}
