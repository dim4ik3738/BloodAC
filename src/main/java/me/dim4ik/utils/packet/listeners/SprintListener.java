package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import me.dim4ik.BloodAC;
import me.dim4ik.utils.PlayerData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class SprintListener extends PacketListenerAbstract {
    public static Map<UUID, Long> lastStopSprint = new ConcurrentHashMap<>();
    public static Map<UUID, Long> lastStartSprint = new ConcurrentHashMap<>();
    public static Map<UUID, Integer> lastStopSprintTick = new ConcurrentHashMap<>();
    public static Map<UUID, Integer> lastStartSprintTick = new ConcurrentHashMap<>();

    public static void remove(UUID uuid) {
        lastStopSprint.remove(uuid);
        lastStartSprint.remove(uuid);
        lastStopSprintTick.remove(uuid);
        lastStartSprintTick.remove(uuid);
    }

    public SprintListener() {
        PacketListener.addListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.ENTITY_ACTION) return;

        Player player = (Player) event.getPlayer();
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        PlayerData playerData = BloodAC.getInstance().playersData.get(uuid);
        if (playerData == null) return;

        WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
        WrapperPlayClientEntityAction.Action action = packet.getAction();

        if (action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
            lastStopSprint.put(uuid, System.currentTimeMillis());
            lastStopSprintTick.put(uuid, playerData.tick);
        }

        if (action == WrapperPlayClientEntityAction.Action.START_SPRINTING) {
            lastStartSprint.put(uuid, System.currentTimeMillis());
            lastStartSprintTick.put(uuid, playerData.tick);
        }

        if (action == WrapperPlayClientEntityAction.Action.START_SPRINTING || action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
            playerData.isSprinting = action == WrapperPlayClientEntityAction.Action.START_SPRINTING;
        }
    }
}