package dev.bluetree242.discordsrvutils.config;

import dev.bluetree242.discordsrvutils.utils.Utils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Updates only Minecraft message scalars, preserving surrounding YAML and comments. */
public final class ConfigUpgrade {
    private ConfigUpgrade() {}

    public record Result(int messages, Path backup) {}
    private record Edit(int start, int end, String value) {}

    public static synchronized Result upgrade(Path dataFolder) throws IOException {
        Path file = dataFolder.resolve("leveling.yml");
        if (!Files.exists(file)) return new Result(0, null);
        String original = Files.readString(file);
        List<Edit> edits = new ArrayList<>();
        Node root = new Yaml().compose(new StringReader(original));
        if (!(root instanceof MappingNode mapping)) throw new IOException("leveling.yml must contain a YAML mapping");
        for (NodeTuple entry : mapping.getValue()) {
            if (!(entry.getKeyNode() instanceof ScalarNode key)
                    || !key.getValue().equals("minecraft-levelup-message")) continue;
            if (!(entry.getValueNode() instanceof SequenceNode messages)) {
                throw new IOException("minecraft-levelup-message must be a list");
            }
            for (Node node : messages.getValue()) {
                if (!(node instanceof ScalarNode scalar) || !scalar.getTag().equals(Tag.STR)) {
                    throw new IOException("Minecraft messages must be strings");
                }
                String converted = Utils.migrateLegacyMessage(scalar.getValue());
                if (converted.equals(scalar.getValue())) continue;
                if (scalar.getAnchor() != null || scalar.getStartMark().getIndex() < messages.getStartMark().getIndex()) {
                    throw new IOException("Expand YAML anchors in minecraft-levelup-message before upgrading");
                }
                int start = original.offsetByCodePoints(0, scalar.getStartMark().getIndex());
                int end = original.offsetByCodePoints(0, scalar.getEndMark().getIndex());
                String replacement = quote(converted);
                // Block scalars include their terminating newline in the source range.
                if (original.substring(start, end).endsWith("\n")) replacement += original.contains("\r\n") ? "\r\n" : "\n";
                edits.add(new Edit(start, end, replacement));
            }
        }
        if (edits.isEmpty()) return new Result(0, null);
        StringBuilder updated = new StringBuilder(original);
        edits.sort(Comparator.comparingInt(Edit::start).reversed());
        for (Edit edit : edits) updated.replace(edit.start(), edit.end(), edit.value());
        // Validate the resulting YAML before creating a backup or replacing the file.
        new Yaml().compose(new StringReader(updated.toString()));
        Path backup = Files.createTempFile(dataFolder, "leveling.yml.", ".bak");
        Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        Path temporary = Files.createTempFile(dataFolder, "leveling-upgrade-", ".tmp");
        try {
            Files.writeString(temporary, updated);
            if (!Files.readString(file).equals(original)) throw new IOException("leveling.yml changed during upgrade; retry the command");
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return new Result(edits.size(), backup);
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> result.append("\\\\");
                case '"' -> result.append("\\\"");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) result.append(String.format("\\u%04x", (int) c));
                    else result.append(c);
                }
            }
        }
        return result.append('"').toString();
    }
}
