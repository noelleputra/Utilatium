package dev.noellx.utilatium;

import org.bukkit.plugin.java.JavaPlugin;

import dev.noellx.utilatium.commands.ParticleCommand;
import dev.noellx.utilatium.listeners.ButtonBukkitListener;
import dev.noellx.utilatium.listeners.ButtonInteractListener;
import dev.noellx.utilatium.listeners.PlayerListener;
import dev.noellx.utilatium.managers.ConfigManager;
import dev.noellx.utilatium.managers.PluginManager;
import dev.noellx.utilatium.managers.inventarium.InventariumKeys;
import dev.noellx.utilatium.managers.inventarium.MessagesManager;
import dev.noellx.utilatium.listeners.particles.ParticlePacketListener;
import dev.noellx.utilatium.tasks.ButtonEnforcerTask;

public final class Utilatium extends JavaPlugin {

    @Override
    public void onEnable() {
        InventariumKeys.init(this);
        ConfigManager.getInstance().load(this);
        MessagesManager.getInstance().load(this);
        PluginManager.getInstance().initialize();

        ParticlePacketListener.initialize(this);
        ParticlePacketListener.register();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ButtonBukkitListener(this), this);
        getServer().getPluginManager().registerEvents(new ButtonInteractListener(this), this);

        ParticleCommand particleCommand = new ParticleCommand();
        var particlePluginCommand = getCommand("particle");
        if (particlePluginCommand != null) {
            particlePluginCommand.setExecutor(particleCommand);
            particlePluginCommand.setTabCompleter(particleCommand);
        } else {
            getLogger().warning("Command 'particle' is missing from plugin.yml — /particle will not work.");
        }

        new ButtonEnforcerTask().start(this);
        getLogger().info(getDescription().getName() + "Utilatium Noellx has been enabled! Inventarium by Noellx, Nullifidev <3");
    }

    @Override
    public void onDisable() {
        ParticlePacketListener.unregister();
        getLogger().info(getDescription().getName() + "Utilatium Noellx has been disabled!");
    }
}
