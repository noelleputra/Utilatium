package dev.noellx.utilatium.commands;

import dev.noellx.utilatium.managers.inventarium.MessagesManager;
import dev.noellx.utilatium.managers.particles.ParticleSettingsManager;
import dev.noellx.utilatium.managers.particles.ParticleSettingsManager.Level;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class ParticleCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessagesManager messages = MessagesManager.getInstance();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("particle.player-only"));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(messages.get("particle.usage"));
            return true;
        }
        try {
            Level level = Level.valueOf(args[0].toUpperCase(Locale.ROOT));
            ParticleSettingsManager.set(player.getUniqueId(), level);
            player.sendMessage(messages.get("particle.success", Map.of("level", level.name())));
        } catch (IllegalArgumentException exception) {
            player.sendMessage(messages.get("particle.invalid-level"));
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return java.util.Collections.emptyList();
        }
        String prefix = args[0].toUpperCase(Locale.ROOT);
        return Arrays.stream(Level.values())
                .map(Enum::name)
                .filter(value -> value.startsWith(prefix))
                .collect(Collectors.toList());
    }
}
