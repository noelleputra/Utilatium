package dev.noellx.inventarium.listeners;

import dev.noellx.inventarium.managers.ButtonLockManager;
import dev.noellx.inventarium.managers.ConfigManager;
import dev.noellx.inventarium.models.ButtonConfig;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import io.papermc.paper.event.player.PlayerSwapWithEquipmentSlotEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Cancels all player inventory paths that can move/take/replace a protected button. */
public final class ButtonBukkitListener implements Listener {

    private final JavaPlugin plugin;
    private final ButtonLockManager lock = ButtonLockManager.getInstance();
    private final ConfigManager config = ConfigManager.getInstance();
    private final ButtonInteractListener actions;

    public ButtonBukkitListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.actions = new ButtonInteractListener(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !lock.hasAnyLock(player)) return;
        if (lock.isMutating(player)) return;

        ButtonConfig button = null;
        ItemStack clicked = event.getCurrentItem();

        if (event.getClickedInventory() == player.getInventory()) {
            int slot = event.getSlot();
            if (lock.isLockedSlot(player, slot)) button = config.getButton(slot);
        }

        // Number-key swap targets the hotbar slot, not the clicked slot.
        if (button == null && event.getClick() == ClickType.NUMBER_KEY) {
            int slot = event.getHotbarButton();
            if (slot >= 0 && slot <= 8 && lock.isLockedSlot(player, slot)) {
                button = config.getButton(slot);
            }
        }

        // Ctrl+F / offhand and other swaps while holding a protected button.
        if (button == null && event.getClick() == ClickType.SWAP_OFFHAND) {
            int held = player.getInventory().getHeldItemSlot();
            if (lock.isLockedSlot(player, held)) button = config.getButton(held);
        }

        // If the protected item is the thing on the cursor/current slot, never allow it
        // to participate in an inventory operation.
        if (button == null) {
            int identifiedSlot = lock.findLockedSlotByItem(player, clicked);
            if (identifiedSlot >= 0) button = config.getButton(identifiedSlot);
        }
        if (button == null) {
            int identifiedSlot = lock.findLockedSlotByItem(player, event.getCursor());
            if (identifiedSlot >= 0) button = config.getButton(identifiedSlot);
        }

        if (button != null) {
            event.setCancelled(true);
            actions.triggerCommand(player, button);
            repairNextTick(player);
            return;
        }

        // Prevent indirect movement into any reserved slot.
        if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            repairNextTick(player);
            return;
        }

        if (touchesReservedHotbar(event, player)) {
            event.setCancelled(true);
            repairNextTick(player);
        }
    }

    private boolean touchesReservedHotbar(InventoryClickEvent event, Player player) {
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int slot = event.getHotbarButton();
            return slot >= 0 && slot <= 8 && lock.isLockedSlot(player, slot);
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !lock.hasAnyLock(player)) return;
        if (lock.isMutating(player)) return;

        if (lock.findLockedSlotByItem(player, event.getOldCursor()) >= 0) {
            event.setCancelled(true);
            ButtonConfig button = config.getButton(lock.findLockedSlotByItem(player, event.getOldCursor()));
            if (button != null) actions.triggerCommand(player, button);
            repairNextTick(player);
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) continue;
            int slot = event.getView().convertSlot(raw);
            if (lock.isLockedSlot(player, slot)) {
                event.setCancelled(true);
                ButtonConfig button = config.getButton(slot);
                if (button != null) actions.triggerCommand(player, button);
                repairNextTick(player);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!lock.hasAnyLock(player)) return;

        int slot = lock.findLockedSlotByItem(player, event.getItemDrop().getItemStack());
        if (slot < 0) slot = player.getInventory().getHeldItemSlot();
        if (!lock.isLockedSlot(player, slot)) return;

        event.setCancelled(true);
        ButtonConfig button = config.getButton(slot);
        if (button != null) actions.triggerCommand(player, button);
        repairNextTick(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        int held = player.getInventory().getHeldItemSlot();
        if (!lock.isLockedSlot(player, held) && !lock.isLockedSlot(player, 40)) return;

        event.setCancelled(true);
        ButtonConfig button = lock.isLockedSlot(player, held) ? config.getButton(held) : config.getButton(40);
        if (button != null) actions.triggerCommand(player, button);
        repairNextTick(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPickItem(PlayerPickItemEvent event) {
        Player player = event.getPlayer();
        int target = event.getTargetSlot();
        int source = event.getSourceSlot();
        int locked = lock.isLockedSlot(player, target) ? target : (lock.isLockedSlot(player, source) ? source : -1);
        if (locked < 0) return;

        event.setCancelled(true);
        ButtonConfig button = config.getButton(locked);
        if (button != null) actions.triggerCommand(player, button);
        repairNextTick(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwapEquipment(PlayerSwapWithEquipmentSlotEvent event) {
        Player player = event.getPlayer();
        int held = player.getInventory().getHeldItemSlot();
        if (!lock.isLockedSlot(player, held) && !lock.isLockedSlot(player, 40)) return;

        event.setCancelled(true);
        ButtonConfig button = lock.isLockedSlot(player, held) ? config.getButton(held) : config.getButton(40);
        if (button != null) actions.triggerCommand(player, button);
        repairNextTick(player);
    }

    private void repairNextTick(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) lock.enforce(player);
        });
    }
}
