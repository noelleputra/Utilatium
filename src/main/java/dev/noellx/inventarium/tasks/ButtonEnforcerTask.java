package dev.noellx.inventarium.tasks;

import dev.noellx.inventarium.managers.ButtonLockManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Layer 3 — jaring pengaman terakhir. Menangkap kasus yang TIDAK bisa dicegah lewat
 * packet/event sama sekali: plugin lain manggil player.getInventory().setItem(slot, ...)
 * langsung dari kode Java-nya. Berlaku untuk semua slot button (bukan cuma satu).
 */
public class ButtonEnforcerTask extends BukkitRunnable {

    private final ButtonLockManager lockManager = ButtonLockManager.getInstance();

    public void start(Plugin plugin) {
        runTaskTimer(plugin, 1L, 2L); // tiap 2 tick (~100ms)
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            lockManager.enforce(player);
        }
    }
}
