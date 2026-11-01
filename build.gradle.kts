import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
}

group = "com.reportdesk"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper 1.21.11 API. The produced jar keeps api-version 1.20 in paper-plugin.yml,
    // so servers from 1.20 upwards accept it; tested against 1.21.11.
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Gson is bundled with the Paper server, but declaring it explicitly keeps
    // compilation reproducible and lets the storage tests run without a server.
    compileOnly("com.google.code.gson:gson:2.11.0")

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

base {
    archivesName.set("ReportDesk")
}

java {
    // Emit Java 17 bytecode so the jar runs on the default JDK of 1.20 Paper
    // servers (Java 17) as well as Java 21+ servers.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// The Paper 1.21+ API jars declare a Java-21 target in their module metadata,
// so Gradle's default resolution refuses to pair them with a 17-target
// consumer. Bump the consumer side of that attribute to 21 on every
// consuming configuration; options.release = 17 (set on the compile tasks
// below) still keeps the shipped .class files at Java 17.
configurations.matching { it.name.endsWith("Classpath") }.all {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
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

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "ReportDesk",
            "Implementation-Version" to project.version,
        )
    }
}
