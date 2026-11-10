package dev.bluetree242.discordsrvutils.systems.tickets;

import org.junit.Test;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TicketClaimStoreTest {
    private DSLContext database(Connection connection, boolean hsql) {
        return DSL.using(connection, hsql ? SQLDialect.HSQLDB : SQLDialect.H2,
                new Settings().withRenderQuotedNames(RenderQuotedNames.NEVER));
    }

    @Test public void migrationKeepsExistingTicketsAndClaimsSurviveReloadAndReopen() throws Exception {
        for (boolean hsql : new boolean[]{false, true}) {
            String url = (hsql ? "jdbc:hsqldb:mem:" : "jdbc:h2:mem:") + UUID.randomUUID();
            try (Connection connection = DriverManager.getConnection(url, "SA", "")) {
                DSLContext db = database(connection, hsql);
                db.execute("CREATE TABLE tickets (Channel BIGINT, MessageID BIGINT, Closed VARCHAR(5))");
                db.execute("INSERT INTO tickets VALUES (10, 20, 'false'), (11, 21, 'true')");
                try (var migration = getClass().getResourceAsStream("/flyway-migrations/V11__TICKET_CLAIMS.sql")) {
                    db.execute(new String(migration.readAllBytes(), StandardCharsets.UTF_8));
                }
                assertEquals(2, db.fetchCount(DSL.table("tickets")));
                assertFalse(TicketClaimStore.claim(db, 11, 21, 100)); // closed
                assertFalse(TicketClaimStore.claim(db, 10, 999, 100)); // stale message
                assertFalse(TicketClaimStore.claim(db, 999, 20, 100)); // missing ticket
                assertTrue(TicketClaimStore.claim(db, 10, 20, 100));
                try (Connection reloaded = DriverManager.getConnection(url, "SA", "")) {
                    DSLContext other = database(reloaded, hsql);
                    assertFalse(TicketClaimStore.claim(other, 10, 20, 101));
                    assertEquals(Long.valueOf(100), other.fetchOne("SELECT ClaimedBy FROM tickets WHERE Channel = 10").get(0, Long.class));
                    other.execute("UPDATE tickets SET Closed = 'true' WHERE Channel = 10");
                    other.execute("UPDATE tickets SET Closed = 'false', MessageID = 22 WHERE Channel = 10");
                    assertFalse(TicketClaimStore.claim(other, 10, 22, 101));
                }
            }
        }
    }

    @Test public void concurrentClicksHaveExactlyOneWinner() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(url, "SA", "")) {
            DSLContext db = database(connection, false);
            db.execute("CREATE TABLE tickets (Channel BIGINT, MessageID BIGINT, Closed VARCHAR(5), ClaimedBy BIGINT)");
            db.execute("INSERT INTO tickets VALUES (10, 20, 'false', NULL)");
            var pool = Executors.newFixedThreadPool(2);
            CountDownLatch start = new CountDownLatch(1);
            try {
                var first = pool.submit(() -> {
                    try (Connection c = DriverManager.getConnection(url, "SA", "")) {
                        start.await();
                        return TicketClaimStore.claim(database(c, false), 10, 20, 100);
                    }
                });
                var second = pool.submit(() -> {
                    try (Connection c = DriverManager.getConnection(url, "SA", "")) {
                        start.await();
                        return TicketClaimStore.claim(database(c, false), 10, 20, 101);
                    }
                });
                start.countDown();
                assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
                long claimant = db.fetchOne("SELECT ClaimedBy FROM tickets").get(0, Long.class);
                assertTrue(claimant == 100 || claimant == 101);
            } finally {
                pool.shutdownNow();
            }
        }
    }
}
