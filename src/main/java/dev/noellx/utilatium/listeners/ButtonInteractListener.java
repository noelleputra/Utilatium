package dev.noellx.utilatium.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import dev.noellx.utilatium.gui.ButtonConfig;
import dev.noellx.utilatium.managers.ConfigManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Converts hand interaction with a button into the configured player command. */
public final class ButtonInteractListener implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager config = ConfigManager.getInstance();
    private final Map<UUID, Long> lastExecuted = new HashMap<>();

    public ButtonInteractListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        int slot = player.getInventory().getHeldItemSlot();
        ButtonConfig button = config.getButton(slot);
        if (button == null) return;

        boolean air = event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR;
        boolean block = event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        boolean trigger = button.isProtect() || (air && button.isExecuteOnAirClick()) || (block && button.isExecuteOnBlockClick());
        if (!trigger) return;

        event.setCancelled(true);
        triggerCommand(player, button);
    }

    public void triggerCommand(Player player, ButtonConfig button) {
        String configuredCommand = button.getCommand();
        if (configuredCommand == null || configuredCommand.isBlank()) return;

        boolean runAsConsole = configuredCommand.startsWith("/");
        if (runAsConsole) {
            configuredCommand = configuredCommand.substring(1);
        }

        String command = replacePlaceholders(configuredCommand, player);
        if (command.isBlank()) return;
        final boolean resolvedConsole = runAsConsole;
        final String resolvedCommand = command;

        long now = System.currentTimeMillis();
        Long last = lastExecuted.put(player.getUniqueId(), now);
        if (last != null && now - last < 100L) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (resolvedConsole) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolvedCommand);
            } else {
                player.performCommand(resolvedCommand);
            }
        });
    }

    private String replacePlaceholders(String command, Player player) {
        return command
                .replace("%player_name%", player.getName())
                .replace("%player_uuid%", player.getUniqueId().toString())
                .replace("%player_display_name%", player.getDisplayName())
                .replace("%player_world%", player.getWorld().getName())
                .replace("%player_x%", String.valueOf(player.getLocation().getBlockX()))
                .replace("%player_y%", String.valueOf(player.getLocation().getBlockY()))
                .replace("%player_z%", String.valueOf(player.getLocation().getBlockZ()));
    }
}
