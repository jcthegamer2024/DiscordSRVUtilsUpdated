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

import java.net.URI

/*
 * LICENSE
 * DiscordSRVUtils
 * -------------
 * Copyright (C) 2020 - 2023 BlueTree242
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

plugins {
    id("java")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://jitpack.io")
}

// Plugin integrations may bring older server APIs; compile against Paper only.
configurations.configureEach {
    exclude(group = "org.spigotmc", module = "spigot-api")
    exclude(group = "org.bukkit", module = "bukkit")
}

// Download at task execution time so `clean build` cannot remove the API after resolution.
val downloadCmiApi = tasks.register("downloadCmiApi") {
    val apiJar = layout.buildDirectory.file("download/CMI-API.jar")
    outputs.file(apiJar)
    doLast {
        val file = apiJar.get().asFile
        file.parentFile.mkdirs()
        URI("https://github.com/Zrips/CMI-API/releases/download/8.7.8.2/CMIAPI8.7.8.2.jar")
            .toURL().openStream().use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
    }
}

dependencies {
    // Paper API and plugin integrations
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("com.gitlab.ruany:LiteBansAPI:0.3.4")
    compileOnly("space.arim.libertybans:bans-api:0.8.0")
    compileOnly("com.github.LeonMangler:SuperVanish:6.2.17")
    compileOnly("com.github.DevLeoko:AdvancedBan:v2.3.0")
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly(files(downloadCmiApi))
    compileOnly("net.lapismc:AFKPlus:3.3.15")
    compileOnly("com.discordsrv:discordsrv:${rootProject.properties["discordsrv_version"]}")
    compileOnly("net.essentialsx:EssentialsX:2.19.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.1")

    // JOOQ
    implementation("org.jooq:jooq:3.16.23")

    // Core plugin
    implementation(project(":core"))
}
