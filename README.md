<div align="center">
  <img alt="Paper" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/paper_vector.svg">
  <a href="https://modrinth.com/plugin/discordsrvutils"><img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg"></a>
  <a href="https://discordsrvutils.xyz"><img alt="Website" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/website_vector.svg"></a>
</div>

# DiscordSRVUtilsUpdated

This is a modified fork of [DiscordSRVUtils](https://github.com/BlueDevelopersInc/DiscordSRVUtils),
originally developed by BlueTree242 and contributors. This fork's changes dated
10 November 2026 include Paper 26.3/Java 25 support, MiniMessage, safer configuration
migration, and staff ticket claiming. The code remains licensed under the
[GNU GPL version 3](LICENSE), retaining the original copyright notices.

The plugin's internal name remains `DiscordSRVUtils` so existing data directories
and integrations continue to work.

Elevate your Discord server with custom messages, synced punishments, tickets, and leveling!

![Modrinth Downloads](https://img.shields.io/modrinth/dt/kPCZgvM2?label=Modrinth%20downloads)
![Spigot Downloads](https://img.shields.io/spiget/downloads/85958?label=SpigotMC%20downloads&color=FF7F7F)
[![](https://bstats.org/signatures/bukkit/DiscordSRVUtils.svg)](https://bstats.org/plugin/bukkit/DiscordSRVUtils/9456 "DiscordSRVUtils' bstats")

## Requirements and building

Requires Paper 26.3 or newer, Java 25, and DiscordSRV.
Build with JDK 25 using `./gradlew build` (`.\gradlew.bat build` on Windows).
The plugin uses `paper-plugin.yml` and Paper lifecycle command registration for
`/discordsrvutils` and `/dsu`. DiscordSRV is required; other integrations are optional.
The shaded plugin JAR is written to `build/libs/`.

# API

### Version

![Latest release](https://repo.bluetree242.dev/api/badge/latest/maven-releases/dev/bluetree242/discordsrvutils/core?name=Latest%20Release&color=FF7F7F)
![Latest Development Build](https://repo.bluetree242.dev/api/badge/latest/maven-snapshots/dev/bluetree242/discordsrvutils/core?name=Latest%20Development%20Build&color=FF7F7F)

Replace `VERSION` with the API version you would like. The badges above displays the latest versions available.

## Maven

```xml

<repository>
    <id>bluetree242-repo</id>
    <url>https://repo.bluetree242.dev/maven-public</url>
</repository>
```

```xml

<dependency>
    <groupId>dev.bluetree242.discordsrvutils</groupId>
    <artifactId>core</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
```

## Gradle (Groovy DSL)

```gradle
repositories {
    // Any other repository...
    maven { url 'https://repo.bluetree242.dev/maven-public' }
}
```

```gradle
dependencies {
    // Any other dependency...
    compileOnly 'dev.bluetree242.discordsrvutils:core:VERSION'
}
```

## Gradle (Kotlin DSL)

```kts
repositories {
    // Any other repository...
    maven("https://repo.bluetree242.dev/maven-public")
}
```

```kts
dependencies {
    // Any other dependency...
    compileOnly("dev.bluetree242.discordsrvutils:core:VERSION")
}
```

You also need to add DiscordSRV's dependency and repository, which you can
find [here](https://docs.discordsrv.com/master/#developers)

# Contributing

To contribute you can fork this repo and make changes on the `develop` branch. And you can pull request to the `develop`
branch. Pull requests on `master` will be denied and closed.

# License

[License]:https://img.shields.io/github/license/BlueDevelopersInc/DiscordSRVUtils?color=e
[![License]](https://github.com/BlueDevelopersInc/DiscordSRVUtils/blob/master/LICENSE)
  







## Message formatting

Minecraft messages use MiniMessage, for example `<yellow>Hello <bold>world</bold></yellow>`.
Colors, gradients, hover text and click actions are rendered as Adventure components.
The `minecraft-levelup-message` setting in `leveling.yml` uses MiniMessage; existing
legacy messages are imported when read without rewriting your configuration.
Discord message templates continue to use Discord formatting.

Run `/discordsrvutils configupgrade` (or `/dsu configupgrade`) to save legacy
Minecraft leveling message formatting as MiniMessage in `leveling.yml`. Requires
`discordsrvutils.configupgrade` (operators by default). The command backs up the
original file, preserves comments and other settings, then reloads configuration.
Already converted messages are skipped; Discord templates are left unchanged.
Startup does not rewrite existing configuration files.

## Local Paper test server

Run `./gradlew runServer --console=plain` (Windows: `.\gradlew.bat runServer --console=plain`).
Gradle builds the shaded plugin, downloads Paper 26.3 and DiscordSRV, and runs
with Java 25 in `run/`. This directory survives `clean` and is ignored by Git.
On the first launch, read the Minecraft EULA linked in `run/eula.txt`; set
`eula=true` if you agree, then run the task again. Connect to `localhost:25565`.
Configure a test bot in `run/plugins/DiscordSRV/config.yml` to exercise Discord
features. Put optional integration plugin JARs in `run/plugins/`. Type `stop` in
the server console to shut it down.

Existing DiscordSRVUtils YAML files are read without rewriting them, including
unknown/older keys and comments. Missing options use defaults in memory; new
files are generated only when absent. Invalid YAML is reported without replacing
the file. The explicit `configupgrade` command still backs up and updates only
Minecraft message formatting. Preservation does not imply removed historical
options are supported by the current plugin.

## Claiming tickets

Open tickets have a **Claim Ticket** button alongside Close Ticket. Panel staff
roles, configured bot admins, and members with Manage Channels can claim them.
The bot announces the claimant and disables the claim button. Claims persist
through restarts and closing/reopening; only the first concurrent claim succeeds.
Existing open ticket messages receive the button when the bot becomes ready.
Customize the label with `ticket-claim-button` in `tickets.yml` (default: Claim Ticket).
Existing configuration files remain untouched and use the new default in memory.
