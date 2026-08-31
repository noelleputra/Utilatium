package dev.noellx.inventarium;

import dev.noellx.inventarium.listeners.ButtonBukkitListener;
import dev.noellx.inventarium.listeners.ButtonInteractListener;
import dev.noellx.inventarium.listeners.PlayerListener;
import dev.noellx.inventarium.managers.ConfigManager;
import dev.noellx.inventarium.managers.InventariumKeys;
import dev.noellx.inventarium.managers.PluginManager;
import dev.noellx.inventarium.tasks.ButtonEnforcerTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class Inventarium extends JavaPlugin {

    @Override
    public void onEnable() {
        InventariumKeys.init(this);
        ConfigManager.getInstance().load(this);
        PluginManager.getInstance().initialize();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ButtonBukkitListener(this), this);
        getServer().getPluginManager().registerEvents(new ButtonInteractListener(this), this);

        new ButtonEnforcerTask().start(this);
        getLogger().info(getDescription().getName() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(getDescription().getName() + " has been disabled!");
    }
}
