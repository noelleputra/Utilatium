package dev.noellx.inventarium.models;

import dev.noellx.inventarium.managers.InventariumKeys;
import dev.noellx.inventarium.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Representasi satu button dari config.yml. Standalone — tidak bergantung plugin item
 * manapun (MMOItems dll). Item dibangun native pakai Material biasa + di-tag PDC supaya
 * sistem lock bisa memverifikasi identitas item secara pasti.
 *
 * "slot" pakai indeks PlayerInventory Bukkit:
 *   0-8   = hotbar
 *   9-35  = inventory utama
 *   36-39 = armor (36 boots, 37 leggings, 38 chestplate, 39 helmet)
 *   40    = offhand
 */
public class ButtonConfig {

    private final String id;
    private final int slot;
    private final Material material;
    private final String displayName;    // nullable
    private final List<String> lore;     // nullable
    private final String command;        // dijalankan as-player, tanpa slash
    private final boolean executeOnAirClick;
    private final boolean executeOnBlockClick;
    private final boolean protect;

    public ButtonConfig(String id, int slot, Material material,
                         String displayName, List<String> lore, String command,
                         boolean executeOnAirClick, boolean executeOnBlockClick, boolean protect) {
        this.id = id;
        this.slot = slot;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.command = command;
        this.executeOnAirClick = executeOnAirClick;
        this.executeOnBlockClick = executeOnBlockClick;
        this.protect = protect;
    }

    public String getId() { return id; }
    public int getSlot() { return slot; }
    public String getCommand() { return command; }
    public boolean isExecuteOnAirClick() { return executeOnAirClick; }
    public boolean isExecuteOnBlockClick() { return executeOnBlockClick; }
    public boolean isProtect() { return protect; }

    /**
     * Build ItemStack resmi buat button ini, ditag dengan PersistentDataContainer
     * (key "button_id" = id button ini) supaya identitasnya bisa diverifikasi pasti,
     * bukan cuma dibandingkan lewat isSimilar().
     */
    public ItemStack buildItem() {
        ItemStack item = new ItemStack(material != null ? material : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) meta.setDisplayName(Utils.colorize(displayName));
            if (lore != null) meta.setLore(lore.stream().map(Utils::colorize).toList());
            meta.getPersistentDataContainer().set(InventariumKeys.buttonId(), PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return item;
    }
}
