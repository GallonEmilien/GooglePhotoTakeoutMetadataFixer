plugins {
    kotlin("jvm") version "2.0.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "fr.gallonemilien"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.9.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("org.apache.commons:commons-imaging:1.0-alpha3")
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    implementation("org.mp4parser:isoparser:1.9.41")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation(kotlin("test"))
}

javafx {
    version = "21"
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("fr.gallonemilien.googlephotometadatafixer.LauncherKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.register<Exec>("jpackage") {
    dependsOn("shadowJar")
    group = "distribution"
    description = "Create a portable application package"

    val shadowJarTask = tasks.named("shadowJar").get() as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
    val inputDir = shadowJarTask.destinationDirectory.get().asFile
    val inputJarName = shadowJarTask.archiveFileName.get()
    val outputDir = layout.buildDirectory.dir("jpackage").get().asFile

    doFirst {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
    }

    val jpackage = javaToolchains.launcherFor(java.toolchain).get()
        .metadata.installationPath.file("bin/jpackage").asFile.absolutePath

    commandLine(
        jpackage,
        "--type", "app-image",
        "--dest", outputDir,
        "--input", inputDir,
        "--name", "GooglePhotoMetadataFixer",
        "--main-jar", inputJarName,
        "--main-class", "fr.gallonemilien.googlephotometadatafixer.LauncherKt",
        "--win-console",
        "--java-options", "-Dfile.encoding=UTF-8"
    )
}

tasks.register<Zip>("packagePortable") {
    dependsOn("jpackage")
    group = "distribution"
    description = "Create a portable ZIP package"

    from(layout.buildDirectory.dir("jpackage/GooglePhotoMetadataFixer"))
    archiveFileName.set("GooglePhotoMetadataFixer-${version}-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}