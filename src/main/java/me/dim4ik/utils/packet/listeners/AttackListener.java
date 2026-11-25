package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import me.dim4ik.BloodAC;
import me.dim4ik.checks.check.Check;
import me.dim4ik.utils.PlayerData;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AttackListener extends PacketListenerAbstract {
    public static Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    public static Map<UUID, Integer> lastAttackTick = new ConcurrentHashMap<>();
    public static Map<UUID, UUID> lastVictim = new ConcurrentHashMap<>();

    public static final int MAX_TICKS = 40;

    public static void remove(UUID uuid) {
        lastAttackTime.remove(uuid);
        lastAttackTick.remove(uuid);
        lastVictim.remove(uuid);
    }

    public AttackListener() {
        PacketListener.addListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);

            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

            UUID uuid = player.getUniqueId();
            int targetId = packet.getEntityId();

            Player target = null;
            for (PlayerData playerData : BloodAC.getInstance().playersData.values()) {
                if (playerData.player != null && playerData.player.getEntityId() == targetId) {
                    target = playerData.player;
                    break;
                }
            }

            HealthListener.onAttack(player);
            HitMissTracker.onAttack(player);

            if (target == null) {
                lastAttackTime.remove(uuid);
                lastAttackTick.remove(uuid);
                lastVictim.remove(uuid);
                return;
            }

            lastAttackTime.put(uuid, System.currentTimeMillis());
            PlayerData data = BloodAC.getInstance().playersData.get(uuid);
            if (data != null) lastAttackTick.put(uuid, data.tick);
            lastVictim.put(uuid, target.getUniqueId());
        }

        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            for (Check check : BloodAC.getInstance().checksManager.enabledChecks) {
                check.onSwing(player);
            }
        }
    }

    public static boolean hasAttackedRecently(UUID uuid) {
        Long lastTime = lastAttackTime.get(uuid);
        if (lastTime == null) return false;
        return (System.currentTimeMillis() - lastTime) <= 5000;
    }
}
