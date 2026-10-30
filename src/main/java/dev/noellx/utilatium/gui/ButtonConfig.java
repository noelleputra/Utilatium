package dev.noellx.utilatium.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import dev.noellx.utilatium.managers.inventarium.InventariumKeys;
import dev.noellx.utilatium.utils.Utils;

import java.util.List;

public class ButtonConfig {

    private final String id;
    private final int slot;
    private final Material material;
    private final String displayName;
    private final List<String> lore;     
    private final String command;
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
