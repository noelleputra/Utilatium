package dev.noellx.utilatium.managers.inventarium;

import dev.noellx.utilatium.utils.Utils;
import java.io.File;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads {@code messages.yml} and hands out colorized, placeholder-substituted
 * strings so every player-facing message in the plugin lives in one config file.
 */
public final class MessagesManager {

    private static MessagesManager instance;

    private JavaPlugin plugin;
    private YamlConfiguration messages;
    private String prefix = "";

    private MessagesManager() {
    }

    public static MessagesManager getInstance() {
        if (instance == null) {
            instance = new MessagesManager();
        }
        return instance;
    }

    /**
     * Loads (or reloads) messages.yml from the plugin's data folder,
     * copying the bundled default first if it doesn't exist yet.
     */
    public void load(JavaPlugin javaPlugin) {
        this.plugin = javaPlugin;

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(file);

        // Keep newly-added default keys present when updating an older messages.yml
        var defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
        }

        prefix = Utils.colorize(messages.getString("prefix", ""));
        plugin.getLogger().info("Loaded messages.yml");
    }

    /**
     * Returns the raw (un-prefixed) colorized message at the given path,
     * with no placeholders replaced.
     */
    public String raw(String path) {
        if (messages == null) {
            return path;
        }
        String value = messages.getString(path);
        if (value == null) {
            plugin.getLogger().warning("Missing message key '" + path + "' in messages.yml");
            return path;
        }
        return Utils.colorize(value);
    }

    /** Returns the colorized message at the given path with the plugin prefix prepended. */
    public String get(String path) {
        return prefix + raw(path);
    }

    public String get(String path, Map<String, String> placeholders) {
        return prefix + format(raw(path), placeholders);
    }

    public String rawFormatted(String path, Map<String, String> placeholders) {
        return format(raw(path), placeholders);
    }

    private String format(String value, Map<String, String> placeholders) {
        if (placeholders == null) {
            return value;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return value;
    }
}
