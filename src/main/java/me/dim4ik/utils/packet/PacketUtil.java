package me.dim4ik.utils.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.EntityPositionData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import me.dim4ik.utils.ServerVersionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PacketUtil {
    private static final AtomicInteger entityIdCounter = new AtomicInteger(10000);

    public static int nextEntityId() {
        return entityIdCounter.getAndIncrement();
    }

    public static void send(Player receiver, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(receiver, packet);
    }

    public static WrapperPlayServerDestroyEntities buildDestroy(int[] ids) {
        return new WrapperPlayServerDestroyEntities(Arrays.copyOf(ids, ids.length));
    }

    public static void sendTeleport(Player receiver, int entityId, double x, double y, double z, float yaw, float pitch, boolean onGround) {
        if (ServerVersionUtil.isNewerThanOrEquals(ServerVersion.V_1_21_2)) {
            send(receiver, new WrapperPlayServerEntityPositionSync(entityId, new EntityPositionData(new Vector3d(x, y, z), Vector3d.zero(), yaw, pitch), onGround));
        } else {
            send(receiver, new WrapperPlayServerEntityTeleport(entityId, new Vector3d(x, y, z), yaw, pitch, onGround));
        }
    }

    public static void sendRelMoveAndLook(Player receiver, int entityId, double dx, double dy, double dz, float yaw, float pitch, boolean onGround) {
        send(receiver, new WrapperPlayServerEntityRelativeMoveAndRotation(entityId, dx, dy, dz, yaw, pitch, onGround));
    }

    public static void sendRelMove(Player receiver, int entityId, double dx, double dy, double dz, boolean onGround) {
        send(receiver, new WrapperPlayServerEntityRelativeMove(entityId, dx, dy, dz, onGround));
    }

    private static int getArmorStandFlagsIndex(ServerVersion version) {
        if (version.isNewerThanOrEquals(ServerVersion.V_1_17)) return 15;
        return 14;
    }

    public static WrapperPlayServerEntityMetadata buildArmorStandMetadata(int entityId, String customName) {
        Component nameComponent = LegacyComponentSerializer.legacySection().deserialize(customName);

        ServerVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion();
        int armorStandFlagsIndex = getArmorStandFlagsIndex(serverVersion);

        List<EntityData<?>> data = new ArrayList<>();
        data.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20));
        data.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(nameComponent)));
        data.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, true));
        data.add(new EntityData<>(armorStandFlagsIndex, EntityDataTypes.BYTE, (byte) 0x10));

        return new WrapperPlayServerEntityMetadata(entityId, data);
    }

    public static void spawnTo(Player viewer, int entityId, Location loc, String customName) {
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(entityId, Optional.of(UUID.randomUUID()), EntityTypes.ARMOR_STAND, new Vector3d(loc.getX(), loc.getY(), loc.getZ()), 0f, 0f, 0f, 0, Optional.empty());
        send(viewer, spawn);
        send(viewer, buildArmorStandMetadata(entityId, customName));
    }
}