package dev.noellx.utilatium.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import dev.noellx.utilatium.gui.ButtonConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigManager {

    private static ConfigManager instance;

    private final Map<Integer, ButtonConfig> buttonsBySlot = new LinkedHashMap<>();

    private ConfigManager() {}

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        buttonsBySlot.clear();

        ConfigurationSection buttonsSection = plugin.getConfig().getConfigurationSection("buttons");
        if (buttonsSection == null) {
            plugin.getLogger().warning("No section 'buttons' in config.yml — skipped button load.");
            return;
        }

        for (String id : buttonsSection.getKeys(false)) {
            ConfigurationSection sec = buttonsSection.getConfigurationSection(id);
            if (sec == null) continue;

            if (!sec.isInt("slot")) {
                plugin.getLogger().warning("Button '" + id + "' do not have 'slot' valid, skip.");
                continue;
            }
            int slot = sec.getInt("slot");
            if (slot < 0 || slot > 40) {
                plugin.getLogger().warning("Button '" + id + "' slot=" + slot + " out range 0-40, skip.");
                continue;
            }
            if (buttonsBySlot.containsKey(slot)) {
                plugin.getLogger().warning("Button '" + id + "' using slot " + slot
                        + " used by another button, skipped.");
                continue;
            }

            Material material = Material.matchMaterial(sec.getString("material", "PAPER"));
            String name = sec.getString("name", null);
            var lore = sec.getStringList("lore");
            String command = sec.getString("command", "");
            String legacyConsoleCommand = sec.getString("console-command", "");
            boolean executeOnAir = sec.getBoolean("execute-on-air-click", false);
            boolean executeOnBlock = sec.getBoolean("execute-on-block-click", false);
            boolean protect = sec.getBoolean("protect", true);

            if (!legacyConsoleCommand.isBlank()) {
                if (command == null || command.isBlank()) {
                    command = "/" + legacyConsoleCommand;
                } else {
                    plugin.getLogger().warning("Button '" + id + "' has both 'command' and deprecated 'console-command'; 'command' takes precedence.");
                }
            }

            ButtonConfig button = new ButtonConfig(id, slot, material,
                    name, lore.isEmpty() ? null : lore, command,
                    executeOnAir, executeOnBlock, protect);
            buttonsBySlot.put(slot, button);
        }

        plugin.getLogger().info("Loaded " + buttonsBySlot.size() + " button(s) from config.yml");
    }

    public Map<Integer, ButtonConfig> getButtonsBySlot() {
        return buttonsBySlot;
    }

    public ButtonConfig getButton(int slot) {
        return buttonsBySlot.get(slot);
    }

    public boolean isButtonSlot(int slot) {
        return buttonsBySlot.containsKey(slot);
    }
}
