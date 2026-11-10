/*
 * LICENSE
 * DiscordSRVUtils
 * -------------
 * Copyright (C) 2020 - 2024 BlueTree242
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

dependencies {
    implementation(project(":bukkit")) {
        isTransitive = false
    }
    implementation(project(":core"))
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")
    repositories {
        mavenCentral()
        maven("https://mvn-repo.arim.space/affero-gpl3/")
        maven("https://mvn-repo.arim.space/lesser-gpl3/")
        maven("https://nexus.scarsz.me/content/groups/public/")
    }
    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.40")
        annotationProcessor("org.projectlombok:lombok:1.18.40")
    }
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }
}

allprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
        options.compilerArgs.addAll(listOf("-Xlint:deprecation"))
    }
}

val buildNumber = project.properties["buildNumber"]?.toString() ?: "NONE"
val commit = System.getenv("GIT_COMMIT") ?: System.getProperty("GIT_COMMIT") ?: System.getenv("GITHUB_SHA") ?: "UNKNOWN"

println("Build number is $buildNumber")
println("Commit hash is $commit")

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    tasks.withType<Copy> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    if (name == "core") {
        val prepareJavaSources = tasks.register<Sync>("prepareJavaSources") {
            from("src/main/java")
            into(layout.buildDirectory.dir("generated/sources/versionInfo/java"))
            val tokens = mapOf(
                "version" to project.version.toString(),
                "build" to buildNumber,
                "commit" to commit,
                "buildDate" to System.currentTimeMillis().toString()
            )
            inputs.properties(tokens)
            filesMatching("**/VersionInfo.java") {
                filter<ReplaceTokens>("tokens" to tokens)
            }
        }
        sourceSets.main {
            java.setSrcDirs(listOf(prepareJavaSources))
        }
    } else if (name == "bukkit") {
        tasks.processResources {
            inputs.property("version", project.version)
            expand("version" to project.version)
        }
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

val prepareTestServer = tasks.register("prepareTestServer") {
    group = "Run Paper"
    description = "Creates local-only server defaults without replacing existing settings."
    doLast {
        val properties = layout.projectDirectory.file("run/server.properties").asFile
        properties.parentFile.mkdirs()
        if (properties.createNewFile()) {
            properties.writeText("server-ip=127.0.0.1\nserver-port=25565\nmotd=DiscordSRVUtils test server\nview-distance=6\nsimulation-distance=4\n")
        }
    }
}

// Uses the root shaded JAR, containing both the Paper adapter and core dependencies.
tasks.runServer {
    dependsOn(prepareTestServer)
    minecraftVersion("26.3")
    runDirectory.set(layout.projectDirectory.dir("run"))
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
    jvmArgs("-Xms1G", "-Xmx2G")
    downloadPlugins {
        url("https://nexus.scarsz.me/content/groups/public/com/discordsrv/discordsrv/${project.properties["discordsrv_version"]}/discordsrv-${project.properties["discordsrv_version"]}.jar")
    }
}
