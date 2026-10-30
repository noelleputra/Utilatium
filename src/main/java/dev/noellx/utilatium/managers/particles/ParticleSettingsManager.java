package dev.noellx.utilatium.managers.particles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParticleSettingsManager {
    public enum Level {
        LOW(0.25D),
        MEDIUM(0.50D),
        HIGH(0.75D),
        EXTREME(1.00D);

        private final double multiplier;

        Level(double multiplier) {
            this.multiplier = multiplier;
        }

        public double multiplier() {
            return multiplier;
        }
    }

    private static final Map<UUID, Level> SETTINGS = new ConcurrentHashMap<>();
    private static final Pattern SETTING_PATTERN = Pattern.compile(
            "\\{\"uuid\":\"([0-9a-fA-F-]+)\",\"level\":\"([A-Z]+)\"\\}");
    private static JavaPlugin plugin;
    private static Path settingsFile;

    private ParticleSettingsManager() {
    }

    public static void initialize(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        settingsFile = plugin.getDataFolder().toPath().resolve("particle-settings.jsonl");
        SETTINGS.clear();

        if (!Files.exists(settingsFile)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(settingsFile, StandardCharsets.UTF_8)) {
                Matcher matcher = SETTING_PATTERN.matcher(line.trim());
                if (!matcher.matches()) {
                    plugin.getLogger().warning("Skipping invalid particle setting line.");
                    continue;
                }
                try {
                    SETTINGS.put(UUID.fromString(matcher.group(1)), Level.valueOf(matcher.group(2)));
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Invalid particle setting in " + settingsFile.getFileName() + ".");
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Could not read " + settingsFile.getFileName() + ".", exception);
        }
    }

    public static Level get(UUID playerId) {
        return SETTINGS.getOrDefault(playerId, Level.HIGH);
    }

    public static void set(UUID playerId, Level level) {
        SETTINGS.put(playerId, level);
        save();
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(settingsFile.getParent());
            Path temporaryFile = settingsFile.resolveSibling(settingsFile.getFileName() + ".tmp");
            Map<UUID, Level> snapshot = new LinkedHashMap<>(SETTINGS);
            StringBuilder contents = new StringBuilder();
            for (Map.Entry<UUID, Level> entry : snapshot.entrySet()) {
                contents.append("{\"uuid\":\"")
                        .append(entry.getKey())
                        .append("\",\"level\":\"")
                        .append(entry.getValue().name())
                        .append("\"}")
                        .append(System.lineSeparator());
            }
            Files.writeString(temporaryFile, contents.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, settingsFile,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, settingsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Could not save " + settingsFile.getFileName() + ".", exception);
        }
    }
}
