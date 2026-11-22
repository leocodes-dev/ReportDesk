plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.reportdesk"
version = "1.0.0"

repositories {
    mavenCentral()
    // Spigot API snapshots (compile-only: the server provides it at runtime).
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    // Spigot 1.20.1 API: the lowest common denominator across the Spigot, Paper and Folia
    // cores. The descriptors keep api-version '1.20', so servers from 1.20 upwards accept
    // the jar; compiling against the oldest 1.20 API guarantees no newer-only call slips in.
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")

    // Adventure (MiniMessage, Component, legacy/plain serializers) and Gson ship inside the
    // shaded jar, relocated under com.reportdesk.libs: Spigot exposes neither to plugins,
    // while Paper/Folia expose their own copies, so relocation keeps the two from mixing.
    // They stay on the compile classpath as well, so the MiniMessage-based message engine
    // is testable without a server.
    implementation("net.kyori:adventure-text-minimessage:4.23.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.23.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.23.0")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

base {
    archivesName.set("ReportDesk")
}

java {
    // Emit Java 17 bytecode so the jar runs on the default JDK of 1.20 servers
    // (Java 17) as well as Java 21+ servers.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
    options.compilerArgs.add("-Xlint:deprecation")
    options.compilerArgs.add("-Xlint:unchecked")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}

// The shaded jar is the distribution artifact: it contains the Adventure + Gson
// libraries relocated under com.reportdesk.libs so the plugin works on Spigot
// (no server-provided Adventure/Gson) without clashing with the Paper/Folia copies.
tasks.shadowJar {
    archiveClassifier.set("")
    relocate("net.kyori", "com.reportdesk.libs.kyori")
    relocate("com.google.gson", "com.reportdesk.libs.gson")
    mergeServiceFiles()
    manifest {
        attributes(
            "Implementation-Title" to "ReportDesk",
            "Implementation-Version" to project.version,
        )
    }
}

// The plain jar is not the artifact; the shaded one replaces it.
tasks.jar {
    enabled = false
}
