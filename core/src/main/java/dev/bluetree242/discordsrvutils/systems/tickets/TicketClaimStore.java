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

package dev.bluetree242.discordsrvutils.systems.tickets;

import dev.bluetree242.discordsrvutils.jooq.tables.TicketsTable;
import org.jooq.DSLContext;

public final class TicketClaimStore {
    private TicketClaimStore() {}

    /** Compare-and-set in the database: stale buttons and concurrent claims cannot win. */
    public static boolean claim(DSLContext database, long channel, long message, long staff) {
        var tickets = TicketsTable.TICKETS;
        return database.update(tickets).set(tickets.CLAIMEDBY, staff)
                .where(tickets.CHANNEL.eq(channel)).and(tickets.MESSAGEID.eq(message))
                .and(tickets.CLOSED.eq("false")).and(tickets.CLAIMEDBY.isNull())
                .execute() == 1;
    }
}
