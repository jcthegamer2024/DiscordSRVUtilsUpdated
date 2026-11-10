package dev.bluetree242.discordsrvutils.config;

import dev.bluetree242.discordsrvutils.exceptions.ConfigurationLoadException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ConfManagerTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void allYamlSchemasPreserveOlderKeysAndMissingDefaults() throws Exception {
        for (Class<?> schema : new Class<?>[]{Config.class, SQLConfig.class, PunishmentsIntegrationConfig.class,
                TicketsConfig.class, LevelingConfig.class, SuggestionsConfig.class, StatusConfig.class}) {
            checkSchema(schema);
        }
    }

    private <T> void checkSchema(Class<T> schema) throws Exception {
        Path folder = temporary.newFolder(schema.getSimpleName()).toPath();
        Path file = folder.resolve("config.yml");
        String original = "# Old user comment ??\r\nlegacy-option:\r\n  custom: 'keep this'\r\n  values: [1, 2, 3]\r\n";
        Files.writeString(file, original);
        ConfManager<T> manager = ConfManager.create(folder, "config.yml", schema);
        manager.reloadConfig();
        assertNotNull(manager.getConfigData());
        assertEquals(original, Files.readString(file));
        // Exercise both public load paths used by older callers.
        manager.reloadConfigData();
        assertEquals(original, Files.readString(file));
    }

    @Test public void customValuesAndLegacyMessagesRemainActiveAndUnchanged() throws Exception {
        Path folder = temporary.getRoot().toPath();
        Path file = folder.resolve("leveling.yml");
        String original = "enabled: true\nminecraft-levelup-message: ['&eCustom [stats.level]']\n";
        Files.writeString(file, original);
        ConfManager<LevelingConfig> manager = ConfManager.create(folder, "leveling.yml", LevelingConfig.class);
        manager.reloadConfig();
        assertTrue(manager.getConfigData().enabled());
        assertEquals("&eCustom [stats.level]", manager.getConfigData().minecraft_levelup_message().get(0));
        assertEquals(original, Files.readString(file));
    }

    @Test public void invalidReloadDoesNotReplaceFileOrLastGoodValues() throws Exception {
        Path folder = temporary.getRoot().toPath();
        Path file = folder.resolve("leveling.yml");
        Files.writeString(file, "enabled: true\n");
        ConfManager<LevelingConfig> manager = ConfManager.create(folder, "leveling.yml", LevelingConfig.class);
        manager.reloadConfig();
        String invalid = "enabled: [broken\n";
        Files.writeString(file, invalid);
        assertThrows(ConfigurationLoadException.class, manager::reloadConfig);
        assertEquals(invalid, Files.readString(file));
        assertTrue(manager.getConfigData().enabled());
    }

    @Test public void absentFileGetsDefaultsOnce() throws Exception {
        Path folder = temporary.getRoot().toPath();
        ConfManager<LevelingConfig> manager = ConfManager.create(folder, "leveling.yml", LevelingConfig.class);
        manager.reloadConfig();
        Path file = folder.resolve("leveling.yml");
        assertTrue(Files.isRegularFile(file));
        String defaults = Files.readString(file);
        manager.reloadConfig();
        assertEquals(defaults, Files.readString(file));
    }
}
