package dev.noellx.inventarium.listeners;

import dev.noellx.inventarium.managers.ButtonLockManager;
import dev.noellx.inventarium.managers.ConfigManager;
import dev.noellx.inventarium.models.ButtonConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public final class PlayerListener implements Listener {

    private final JavaPlugin plugin;
    private final ButtonLockManager lock = ButtonLockManager.getInstance();

    public PlayerListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> installButtons(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Remove all Inventarium menu items from the server's death drop list.
        event.getDrops().removeIf(lock::isProtectedButtonItem);

        // Prevent keepInventory and other inventory state from carrying the menu through death.
        lock.beginMutation(player);
        try {
            for (int slot = 0; slot <= 40; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (lock.isProtectedButtonItem(item)) {
                    player.getInventory().setItem(slot, null);
                }
            }
        } finally {
            lock.endMutation(player);
        }
        lock.unlockAll(player);

        // Another plugin may create a drop after PlayerDeathEvent. Clean only our own PDC items.
        Bukkit.getScheduler().runTask(plugin, () -> removeOwnDrops(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> installButtons(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lock.clearOnQuit(event.getPlayer());
    }

    private void installButtons(Player player) {
        if (!player.isOnline()) return;

        Set<Integer> reserved = new HashSet<>();
        for (ButtonConfig button : ConfigManager.getInstance().getButtonsBySlot().values()) {
            if (button.isProtect()) reserved.add(button.getSlot());
        }

        for (ButtonConfig button : ConfigManager.getInstance().getButtonsBySlot().values()) {
            if (!button.isProtect()) continue;
            lock.install(player, button, reserved);
        }

        lock.enforce(player);
    }

    private void removeOwnDrops(Player player) {
        for (Item item : player.getWorld().getEntitiesByClass(Item.class)) {
            if (item.getLocation().distanceSquared(player.getLocation()) > 16 * 16) continue;
            if (lock.isProtectedButtonItem(item.getItemStack())) item.remove();
        }
    }
}
