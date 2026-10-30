package dev.noellx.utilatium.listeners.particles;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import dev.noellx.utilatium.managers.particles.ParticleSettingsManager;
import dev.noellx.utilatium.managers.particles.ParticleSettingsManager.Level;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParticlePacketListener {
    private static PacketListenerAbstract listener;

    private ParticlePacketListener() {
    }

    public static void initialize(JavaPlugin plugin) {
        ParticleSettingsManager.initialize(plugin);
    }

    public static void register() {
        listener = new PacketListenerAbstract() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                Player player = event.getPlayer();
                if (event.getPacketType() != PacketType.Play.Server.PARTICLE || player == null) {
                    return;
                }

                Level level = ParticleSettingsManager.get(player.getUniqueId());
                if (level == Level.EXTREME) {
                    return;
                }

                WrapperPlayServerParticle packet = new WrapperPlayServerParticle(event);
                if (packet.getParticleCount() == 0) {
                    if (ThreadLocalRandom.current().nextDouble() >= level.multiplier()) {
                        event.setCancelled(true);
                    }
                    return;
                }

                int reducedCount = (int) Math.ceil(packet.getParticleCount() * level.multiplier());
                if (reducedCount <= 0) {
                    event.setCancelled(true);
                } else if (reducedCount < packet.getParticleCount()) {
                    packet.setParticleCount(reducedCount);
                    event.markForReEncode(true);
                }
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(listener);
    }

    public static void unregister() {
        if (listener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            listener = null;
        }
    }
}
