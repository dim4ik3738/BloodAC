package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.dim4ik.BloodAC;
import me.dim4ik.utils.ServerVersionUtil;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HealthListener extends PacketListenerAbstract {
    private static final float[] values = {52F, 42F, 1337F, 67F, 228F, 69F};
    private static final Map<UUID, Float> fakeHealthMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> indexMap = new ConcurrentHashMap<>();

    public HealthListener() {
        PacketListener.addListener(this);
    }

    public static void remove(UUID uuid) {
        fakeHealthMap.remove(uuid);
        indexMap.remove(uuid);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!BloodAC.getInstance().configManager.getBoolean("Hiders.hp")) return;
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;

        Player receiver = event.getPlayer();
        if (receiver == null) return;
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);

        if (receiver.getEntityId() == packet.getEntityId()) return;

        List<EntityData<?>> dataValues = packet.getEntityMetadata();
        if (dataValues.isEmpty()) return;

        fakeHealthMap.putIfAbsent(receiver.getUniqueId(), values[0]);
        Float fakeHealth = fakeHealthMap.get(receiver.getUniqueId());

        boolean changed = false;
        for (EntityData data : dataValues) {
            if (data.getIndex() == ServerVersionUtil.getHealthMetaIndex() && data.getValue() instanceof Float realHealth) {
                if (realHealth == 0) continue;
                data.setValue(fakeHealth);
                changed = true;
            }
        }

        if (changed) {
            packet.setEntityMetadata(dataValues);
        }
    }

    public static void onAttack(Player player) {
        UUID uuid = player.getUniqueId();
        int index = indexMap.getOrDefault(uuid, 0);
        fakeHealthMap.put(uuid, values[index % values.length]);
        indexMap.put(uuid, index + 1);
    }
}