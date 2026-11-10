package dev.bluetree242.discordsrvutils.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ConfigUpgradeTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private Path folder() { return temporary.getRoot().toPath(); }
    private Path file() { return folder().resolve("leveling.yml"); }

    @Test public void preservesCommentsUnrelatedSettingsAndBackup() throws Exception {
        String original = "# Header ??\r\nminecraft-levelup-message:\r\n  - '&eHello [player.name]' # inline\r\n"
                + "  - '<green>Already modern</green>'\r\ndiscord-message: '&cDo not change'\r\n";
        Files.writeString(file(), original);
        ConfigUpgrade.Result result = ConfigUpgrade.upgrade(folder());
        assertEquals(1, result.messages());
        assertEquals(original, Files.readString(result.backup()));
        String updated = Files.readString(file());
        assertTrue(updated.contains("# Header ??\r\n"));
        assertTrue(updated.contains(" # inline\r\n"));
        assertTrue(updated.contains("discord-message: '&cDo not change'\r\n"));
        assertTrue(updated.contains("'<green>Already modern</green>'"));
        Map<?, ?> parsed = new Yaml().load(updated);
        assertTrue(((List<?>) parsed.get("minecraft-levelup-message")).get(0).toString().contains("<yellow>"));
        assertTrue(updated.contains("[player.name]"));
        long count;
        try (var files = Files.list(folder())) { count = files.count(); }
        assertEquals(0, ConfigUpgrade.upgrade(folder()).messages());
        assertEquals(updated, Files.readString(file()));
        try (var files = Files.list(folder())) { assertEquals(count, files.count()); }
    }

    @Test public void handlesFlowListsQuotesAndMultilineScalars() throws Exception {
        for (String yaml : new String[]{
                "minecraft-levelup-message: ['&cSay hi', '&aIt''s done']\nother: true\n",
                "minecraft-levelup-message:\n  - |\n    &eFirst\n    second\nother: true\n"}) {
            Files.writeString(file(), yaml);
            assertTrue(ConfigUpgrade.upgrade(folder()).messages() > 0);
            Map<?, ?> parsed = new Yaml().load(Files.readString(file()));
            assertEquals(Boolean.TRUE, parsed.get("other"));
            assertFalse(parsed.get("minecraft-levelup-message").toString().contains("&e"));
        }
    }

    @Test public void malformedMessageListDoesNotModifyFile() throws Exception {
        String original = "minecraft-levelup-message: wrong\n";
        Files.writeString(file(), original);
        assertThrows(java.io.IOException.class, () -> ConfigUpgrade.upgrade(folder()));
        assertEquals(original, Files.readString(file()));
        try (var files = Files.list(folder())) { assertEquals(1, files.count()); }
    }

    @Test public void rejectsSharedAnchorsWithoutRewritingOtherSettings() throws Exception {
        String original = "discord-message: &shared '&cKeep'\nminecraft-levelup-message: [*shared]\n";
        Files.writeString(file(), original);
        assertThrows(java.io.IOException.class, () -> ConfigUpgrade.upgrade(folder()));
        assertEquals(original, Files.readString(file()));
    }

    @Test public void missingFileIsNoOp() throws Exception {
        assertEquals(0, ConfigUpgrade.upgrade(folder()).messages());
        try (var files = Files.list(folder())) { assertEquals(0, files.count()); }
    }
}
