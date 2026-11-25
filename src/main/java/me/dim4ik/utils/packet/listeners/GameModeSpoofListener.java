package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import me.dim4ik.BloodAC;
import org.bukkit.entity.Player;
import java.util.EnumSet;

public class GameModeSpoofListener extends PacketListenerAbstract {
    public GameModeSpoofListener() {
        PacketListener.addListener(this);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!BloodAC.getInstance().configManager.getBoolean("Hiders.gameMode")) return;
        if (event.getPacketType() != PacketType.Play.Server.PLAYER_INFO_UPDATE) return;

        Player receiver = event.getPlayer();
        if (receiver == null) return;
        WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(event);

        EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = packet.getActions();
        if (!actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER) && !actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE)) return;

        for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry : packet.getEntries()) {
            if (entry.getProfileId().equals(receiver.getUniqueId())) continue;
            entry.setGameMode(GameMode.CREATIVE);
        }
    }
}