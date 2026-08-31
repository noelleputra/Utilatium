package dev.noellx.inventarium.managers;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class InventariumKeys {

    private static NamespacedKey buttonIdKey;

    private InventariumKeys() {}

    public static void init(Plugin plugin) {
        buttonIdKey = new NamespacedKey(plugin, "button_id");
    }

    public static String getButtonId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(buttonId(), PersistentDataType.STRING);
    }

    public static NamespacedKey buttonId() {
        if (buttonIdKey == null) {
            throw new IllegalStateException("InventariumKeys.init(plugin) belum dipanggil.");
        }
        return buttonIdKey;
    }
}
