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

package dev.bluetree242.discordsrvutils.commands.game;

import dev.bluetree242.discordsrvutils.DiscordSRVUtils;
import dev.bluetree242.discordsrvutils.VersionInfo;
import dev.bluetree242.discordsrvutils.config.ConfigUpgrade;
import dev.bluetree242.discordsrvutils.events.LevelupEvent;
import dev.bluetree242.discordsrvutils.exceptions.ConfigurationLoadException;
import dev.bluetree242.discordsrvutils.exceptions.InvalidResponseException;
import dev.bluetree242.discordsrvutils.placeholder.PlaceholdObject;
import dev.bluetree242.discordsrvutils.placeholder.PlaceholdObjectList;
import dev.bluetree242.discordsrvutils.platform.PlatformPlayer;
import dev.bluetree242.discordsrvutils.platform.command.CommandUser;
import dev.bluetree242.discordsrvutils.platform.command.ConsoleCommandUser;
import dev.bluetree242.discordsrvutils.platform.command.PlatformCommand;
import dev.bluetree242.discordsrvutils.systems.leveling.PlayerStats;
import dev.bluetree242.discordsrvutils.updatechecker.UpdateChecker;
import dev.bluetree242.discordsrvutils.utils.Utils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.event.ClickEvent;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.format.NamedTextColor;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DiscordSRVUtilsCommand implements PlatformCommand {
    private final DiscordSRVUtils core;
    private boolean migrated = false;

    @Override
    public void onRunAsync(String[] args, CommandUser sender, String label) throws Throwable {
        if (args.length == 0) {
            sender.sendMessage("<reset><yellow>Running DiscordSRVUtils v<reset><green>" + core.getPlatform().getDescription().getVersion());
            sender.sendMessage("<reset><yellow>Build " + (VersionInfo.BUILD_NUMBER.equalsIgnoreCase("NONE") ? "<reset><green>None/Unknown" : "<reset><green>#" + VersionInfo.BUILD_NUMBER));
            sender.sendMessage("<reset><aqua>Status: " + (core.isReady() ? "<reset><green>Running and functioning" : "<reset><red>Not running"));
            return;
        }
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("configupgrade")) {
                if (!sender.hasPermission("discordsrvutils.configupgrade")) {
                    sender.sendMessage("<red>You do not have permission to upgrade configurations.");
                    return;
                }
                try {
                    ConfigUpgrade.Result result = ConfigUpgrade.upgrade(core.getPlatform().getDataFolder().toPath());
                    if (result.messages() == 0) {
                        sender.sendMessage("<green>No legacy Minecraft message formatting needs upgrading.");
                        return;
                    }
                    sender.sendMessage("<green>Converted " + result.messages() + " message(s). Backup: <yellow>"
                            + Utils.escapeMiniMessage(result.backup().getFileName().toString()));
                    try {
                        core.reloadConfigs();
                        sender.sendMessage("<green>Configuration reloaded successfully.");
                    } catch (Exception ex) {
                        sender.sendMessage("<red>File upgraded, but reload failed. Check console; the backup is available.");
                        core.getLogger().log(java.util.logging.Level.SEVERE, "Configuration reload after upgrade failed", ex);
                    }
                } catch (Exception ex) {
                    sender.sendMessage("<red>Config upgrade failed: " + Utils.escapeMiniMessage(ex.getMessage()));
                    core.getLogger().log(java.util.logging.Level.SEVERE, "Configuration upgrade failed", ex);
                }
                return;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("discordsrvutils.reload")) {
                    sender.sendMessage("<reset><yellow>Reloading Configuration");
                    try {
                        core.reloadConfigs();
                        sender.sendMessage("<reset><green>Configuration successfully reloaded");
                    } catch (ConfigurationLoadException e) {
                        sender.sendMessage("<reset><red>Could not reload the " + e.getConfigName() + ". Please check server console");
                        e.printStackTrace();
                    }
                    return;
                }
            } else if (args[0].equalsIgnoreCase("debug")) {
                if (sender.hasPermission("discordsrvutils.debug")) {
                    sender.sendMessage("<reset><green>Preparing Debug Report... Please wait");
                    try {
                        sender.sendMessage("<reset><green>Your Debug report is available at: <reset><yellow>" + core.getServer().getDebugger().run());
                    } catch (Exception e) {
                        sender.sendMessage("<reset><red>ERROR: " + Utils.escapeMiniMessage(e.getMessage()));
                    }
                    return;
                }
            } else if (args[0].equalsIgnoreCase("updatecheck")) {
                if (sender.hasPermission("discordsrvutils.updatecheck")) {
                    try {
                        UpdateChecker.UpdateCheckResult result = core.getUpdateChecker().updateCheck(false);
                        if (result == null) sender.sendMessage("<reset><red>Looks like update checking is disabled.");
                        else {
                            if (result.getMessage() == null) {
                                if (result.getVersionsBehind() <= 0) {
                                    sender.sendMessage("<reset><green>You are up to date.");
                                } else {
                                    sender.sendMessage(Component.text("You are ").color(NamedTextColor.YELLOW)
                                            .append(Component.text(result.getVersionsBehind()).color(NamedTextColor.RED))
                                            .appendSpace().append(Component.text("versions behind. Update is available at "))
                                            .append(Component.text(result.getDownloadUrl()).color(NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(result.getDownloadUrl())))
                                    );
                                }
                            } else sender.sendMessage(result.getMessage());
                        }
                    } catch (InvalidResponseException e) {
                        sender.sendMessage("<reset><dark_red>Failed to check for updates: <reset><red>" + Utils.escapeMiniMessage(e.getFriendlyMessage()));
                    }
                    return;
                }
            } else if (args[0].equalsIgnoreCase("resetlevel")) {
                if (sender.hasPermission("discordsrvutils.resetlevel")) {
                    String name = args.length >= 2 ? args[1] : null;
                    if (name == null) {
                        sender.sendMessage("<reset><red>Please provide player name or all for all players.");
                        return;
                    }
                    if (name.equalsIgnoreCase("all")) {
                        core.getLevelingManager().resetLeveling();
                        core.getLevelingManager().cachedUUIDS.invalidateAll();
                        core.getLevelingManager().getLevelingRewardsManager().setRewardCache(Utils.OBJECT_MAPPER.createObjectNode());
                        core.getLevelingManager().getLevelingRewardsManager().saveRewardCache();
                        sender.sendMessage("<reset><yellow>Everyone's level has been reset");
                    } else {
                        PlayerStats stats = core.getLevelingManager().getPlayerStats(name);
                        if (stats == null) {
                            sender.sendMessage("<reset><red>Player not found");
                        } else {
                            stats.setLevel(0);
                            stats.setXP(0);
                            core.getLevelingManager().getLevelingRewardsManager().getRewardCache().remove(stats.getUuid().toString());
                            core.getLevelingManager().getLevelingRewardsManager().saveRewardCache();
                            sender.sendMessage("<reset><yellow>Player's level has been reset.");
                        }
                    }
                    return;
                }
            } else if (args[0].equalsIgnoreCase("migrateLeveling")) {
                if (sender instanceof ConsoleCommandUser) { // Only console
                    if (migrated) {
                        sender.sendMessage("<reset><red>Already migrated.");
                        return;
                    }
                    migrated = true;
                    sender.sendMessage("<reset><red>Migrating leveling to new mee6 leveling, please wait....");
                    core.getLevelingManager().convertToMee6();
                    sender.sendMessage("<reset><yellow>Successfully migrated, If you used leveling roles before, please reconfigure according to the new leveling system, keep in mind that leveling roles was upgraded to leveling-<bold>rewards<reset><yellow>.json (https://wiki.discordsrvutils.xyz/leveling-conversion)");
                    return;
                }
            } else if (args[0].equalsIgnoreCase("addxp") && core.getLevelingConfig().enabled()) {
                if (sender.hasPermission("discordsrvutils.addxp")) {
                    String name = args.length >= 2 ? args[1] : null;
                    if (name == null) {
                        sender.sendMessage("<reset><red>Please provide player name.");
                        return;
                    }
                    String amountString = args.length >= 3 ? args[2] : null;
                    if (amountString == null || !Utils.isInt(amountString)) {
                        sender.sendMessage("<reset><red>Please provide a valid xp amount.");
                        return;
                    }
                    PlayerStats stats = core.getLevelingManager().getPlayerStats(name);
                    if (stats == null) {
                        sender.sendMessage("<reset><red>Player not found");
                    } else {
                        int xp = Integer.parseInt(amountString);
                        stats.setXP(xp + stats.getXp(), new LevelupEvent(stats, stats.getUuid()));
                        sender.sendMessage("<reset><green>Added <reset><gold>" + xp + " <reset><green>xp to <reset><gold>" + name + "<reset><green>. Now they are level <reset><gold>" + stats.getLevel() + " <reset><green>and have <reset><gold>" + stats.getXp() + "<reset><green> xp.");
                        core.getLevelingManager().getLevelingRewardsManager().rewardIfOnline(stats);
                        return;
                    }
                }
            } else if ((args[0].equalsIgnoreCase("sendAfk") || args[0].equalsIgnoreCase("sendUnAfk")) && sender instanceof ConsoleCommandUser) {
                String name = args.length >= 2 ? args[1] : null;
                if (name == null) {
                    sender.sendMessage("<reset><red>Please provide player name.");
                    return;
                }
                PlatformPlayer player = core.getServer().getPlayer(name);
                if (player == null) {
                    sender.sendMessage("<reset><red>Player not found.");
                    return;
                }
                if (!player.shouldSendAfk()) return;
                if (core.getMainConfig().afk_message_enabled()) {
                    PlaceholdObjectList holders = new PlaceholdObjectList(core);
                    holders.add(new PlaceholdObject(core, player, "player"));
                    TextChannel channel = core.getJdaManager().getChannel(core.getMainConfig().afk_channel());
                    if (channel == null) {
                        core.severe("No Channel was found with ID " + core.getMainConfig().afk_channel() + ". Afk/NoLonger message was not sent for " + player.getName());
                        return;
                    }
                    Message msg = core.getMessageManager().getMessage(args[0].equalsIgnoreCase("sendAfk") ? core.getMainConfig().afk_message() : core.getMainConfig().no_longer_afk_message(), holders, player).build();
                    core.queueMsg(msg, channel).queue();
                }
            }
        }
        sender.sendMessage("<reset><red>SubCommand not found");
    }

    @Override
    public List<String> onTabComplete(String[] args, CommandUser sender, String label) {
        List<String> values = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("discordsrvutils.configupgrade"))
                values.add("configupgrade");
            if (sender.hasPermission("discordsrvutils.reload"))
                values.add("reload");
            if (sender.hasPermission("discordsrvutils.debug"))
                values.add("debug");
            if (sender.hasPermission("discordsrvutils.updatecheck"))
                values.add("updatecheck");
            if (sender.hasPermission("discordsrvutils.removeslash"))
                values.add("removeslash");
            if (sender.hasPermission("discordsrvutils.resetlevel"))
                values.add("resetlevel");
            if (sender.hasPermission("discordsrvutils.addxp") && core.getLevelingConfig().enabled())
                values.add("addxp");
        } else if ((args.length == 2 && args[0].equalsIgnoreCase("resetlevel") && sender.hasPermission("discordsrvutils.resetlevel"))
                || (args.length == 2 && args[0].equalsIgnoreCase("addxp") && sender.hasPermission("discordsrvutils.addxp") && core.getLevelingConfig().enabled())) {
            List<String> result = core.getPlatform().getServer().getOnlinePlayers().stream().map(PlatformPlayer::getName).collect(Collectors.toList());
            if (args[0].equalsIgnoreCase("resetlevel")) result.add("all");
            return result;
        }

        List<String> result = new ArrayList<>();
        for (String a : values) {
            if (a.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                result.add(a);
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }
}
