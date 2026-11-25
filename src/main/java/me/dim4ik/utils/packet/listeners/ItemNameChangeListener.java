package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import me.dim4ik.BloodAC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemNameChangeListener extends PacketListenerAbstract {
    private static final Map<UUID, Set<Integer>> itemEntityIds = new ConcurrentHashMap<>();

    public ItemNameChangeListener() {
        PacketListener.addListener(this);
    }

    public static void remove(UUID uuid) {
        itemEntityIds.remove(uuid);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getUser() == null) return;
        UUID receiver = event.getUser().getUUID();
        if (receiver == null) return;

        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(event);
            if (spawn.getEntityType() == EntityTypes.ITEM) {
                itemEntityIds.computeIfAbsent(receiver, k -> ConcurrentHashMap.newKeySet()).add(spawn.getEntityId());
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(event);
            Set<Integer> ids = itemEntityIds.get(receiver);
            if (ids == null) return;
            for (int id : destroy.getEntityIds()) {
                ids.remove(id);
            }
            return;
        }

        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;

        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        Set<Integer> ids = itemEntityIds.get(receiver);
        if (ids == null || !ids.contains(packet.getEntityId())) return;

        if (!BloodAC.getInstance().configManager.getBoolean("Hiders.spoofItemsName")) return;

        List<EntityData<?>> entityDataList = packet.getEntityMetadata();

        for (int i = 0; i < entityDataList.size(); i++) {
            EntityData<?> data = entityDataList.get(i);
            if (data.getType() != EntityDataTypes.ITEMSTACK) continue;

            ItemStack original = (ItemStack) data.getValue();
            if (original.isEmpty()) continue;

            ItemStack fake = original.copy();
            String name = BloodAC.getInstance().configManager.getString("Hiders.itemName");
            fake.setComponent(ComponentTypes.CUSTOM_NAME, Component.text(name, NamedTextColor.RED));

            EntityData<ItemStack> newData = new EntityData<>(data.getIndex(), EntityDataTypes.ITEMSTACK, fake);
            entityDataList.set(i, newData);
        }

        packet.setEntityMetadata(entityDataList);
    }
}