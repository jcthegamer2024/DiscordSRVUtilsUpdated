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

package dev.bluetree242.discordsrvutils.systems.tickets.listeners;

import dev.bluetree242.discordsrvutils.DiscordSRVUtils;
import dev.bluetree242.discordsrvutils.systems.tickets.Ticket;
import dev.bluetree242.discordsrvutils.systems.tickets.TicketClaimStore;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class TicketClaimListener extends ListenerAdapter {
    private final DiscordSRVUtils core;

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!"claim_ticket".equals(event.getComponentId()) || core.getMainConfig().bungee_mode()
                || !core.isReady() || event.getGuild() == null || event.getUser().isBot()
                || event.getGuild().getIdLong() != core.getDiscordSRV().getMainGuild().getIdLong()) return;
        event.deferReply(true).queue(hook -> core.getAsyncManager().executeAsync(() -> {
            try {
                Ticket ticket = core.getTicketManager().getTicketByMessageId(event.getMessageIdLong());
                if (ticket == null || ticket.isClosed() || ticket.getChannelID() != event.getChannel().getIdLong()) {
                    hook.editOriginal("This ticket is closed or this control message is outdated.").queue();
                    return;
                }
                Member member = event.getMember();
                boolean staff = member != null && (member.hasPermission(event.getTextChannel(), Permission.MANAGE_CHANNEL)
                        || core.getJdaManager().getAdminIds().contains(member.getIdLong())
                        || member.getRoles().stream().anyMatch(role -> (core.getJdaManager().getAdminIds().contains(role.getIdLong())
                                || ticket.getPanel().getAllowedRoles().contains(role.getIdLong()))));
                if (!staff) {
                    hook.editOriginal("Only ticket staff can claim a ticket.").queue();
                    return;
                }
                if (!TicketClaimStore.claim(core.getDatabaseManager().jooq(), ticket.getChannelID(),
                        event.getMessageIdLong(), event.getUser().getIdLong())) {
                    Ticket current = core.getTicketManager().getTicketByChannel(ticket.getChannelID());
                    String reply = current != null && current.getClaimedBy() != null
                            ? "This ticket is already claimed by <@" + current.getClaimedBy() + ">."
                            : "This ticket is no longer available to claim.";
                    hook.editOriginal(reply).queue();
                    return;
                }
                hook.editOriginal("You claimed this ticket.").queue();
                event.getMessage().editMessage(event.getMessage()).setActionRows(ActionRow.of(
                        core.getTicketManager().getOpenTicketButtons(event.getUser().getIdLong())))
                        .queue(ignored -> {}, error -> core.getLogger().warning("Ticket claimed, but button refresh failed: " + error.getMessage()));
                event.getChannel().sendMessage("Ticket claimed by " + event.getUser().getAsMention() + ".").queue();
            } catch (Exception ex) {
                core.getLogger().log(java.util.logging.Level.SEVERE, "Ticket claim failed", ex);
                hook.editOriginal("Could not claim the ticket. Please try again or contact an administrator.").queue();
            }
        }));
    }
}
