package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import me.dim4ik.BloodAC;
import org.bukkit.entity.Player;

public class EffectListener extends PacketListenerAbstract {
    public EffectListener() {
        super(PacketListenerPriority.HIGHEST);
        PacketListener.addListener(this);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!BloodAC.getInstance().configManager.getBoolean("Hiders.effects")) return;
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_EFFECT) return;

        Player receiver = event.getPlayer();
        if (receiver == null) return;
        WrapperPlayServerEntityEffect packet = new WrapperPlayServerEntityEffect(event);

        if (packet.getEntityId() == receiver.getEntityId()) return;

        ClientVersion clientVersion = event.getServerVersion().toClientVersion();
        int effectId = packet.getPotionType().getId(clientVersion);

        if (effectId != 14) {
            event.setCancelled(true);
        }
    }
}