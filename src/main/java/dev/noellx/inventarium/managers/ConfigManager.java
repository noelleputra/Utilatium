package dev.noellx.inventarium.managers;

import dev.noellx.inventarium.models.ButtonConfig;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

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
            plugin.getLogger().warning("Tidak ada section 'buttons' di config.yml — tidak ada button yang di-load.");
            return;
        }

        for (String id : buttonsSection.getKeys(false)) {
            ConfigurationSection sec = buttonsSection.getConfigurationSection(id);
            if (sec == null) continue;

            if (!sec.isInt("slot")) {
                plugin.getLogger().warning("Button '" + id + "' tidak punya 'slot' yang valid, dilewati.");
                continue;
            }
            int slot = sec.getInt("slot");
            if (slot < 0 || slot > 40) {
                plugin.getLogger().warning("Button '" + id + "' slot=" + slot + " di luar range 0-40, dilewati.");
                continue;
            }
            if (buttonsBySlot.containsKey(slot)) {
                plugin.getLogger().warning("Button '" + id + "' pakai slot " + slot
                        + " yang sudah dipakai button lain, dilewati.");
                continue;
            }

            Material material = Material.matchMaterial(sec.getString("material", "PAPER"));
            String name = sec.getString("name", null);
            var lore = sec.getStringList("lore");
            String command = sec.getString("command", "");
            boolean executeOnAir = sec.getBoolean("execute-on-air-click", false);
            boolean executeOnBlock = sec.getBoolean("execute-on-block-click", false);
            boolean protect = sec.getBoolean("protect", true);

            ButtonConfig button = new ButtonConfig(id, slot, material,
                    name, lore.isEmpty() ? null : lore, command, executeOnAir, executeOnBlock, protect);
            buttonsBySlot.put(slot, button);
        }

        plugin.getLogger().info("Loaded " + buttonsBySlot.size() + " button(s) dari config.yml");
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
