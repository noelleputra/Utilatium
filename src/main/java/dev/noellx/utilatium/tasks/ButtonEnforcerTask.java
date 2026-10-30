package dev.noellx.utilatium.tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import dev.noellx.utilatium.managers.inventarium.ButtonLockManager;

public class ButtonEnforcerTask extends BukkitRunnable {

    private final ButtonLockManager lockManager = ButtonLockManager.getInstance();

    public void start(Plugin plugin) {
        runTaskTimer(plugin, 1L, 2L); // every 2 tick (~100ms)
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            lockManager.enforce(player);
        }
    }
}
