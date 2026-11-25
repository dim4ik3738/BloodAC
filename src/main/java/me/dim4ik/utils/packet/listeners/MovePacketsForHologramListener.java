package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.EntityPositionData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import me.dim4ik.utils.packet.PacketUtil;
import me.dim4ik.utils.PredictHologram;
import org.bukkit.entity.Player;
import java.util.UUID;

public class MovePacketsForHologramListener extends PacketListenerAbstract {
    public MovePacketsForHologramListener() {
        PacketListener.addListener(this);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getUser() == null) return;
        UUID viewerUuid = event.getUser().getUUID();
        if (viewerUuid == null || event.getPlayer() == null) return;

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            handleRelativeMove(event, viewerUuid);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            handleRelativeMoveAndRotation(event, viewerUuid);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_POSITION_SYNC) {
            WrapperPlayServerEntityPositionSync packet = new WrapperPlayServerEntityPositionSync(event);
            EntityPositionData values = packet.getValues();
            handlePosSync(event, viewerUuid, packet.getId(), values.getPosition().getX(), values.getPosition().getY(), values.getPosition().getZ(), packet.isOnGround());
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(event);
            handlePosSync(event, viewerUuid, packet.getEntityId(), packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), packet.isOnGround());
        } else if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            handleDestroy(event, viewerUuid);
        }
    }

    private void handleDestroy(PacketSendEvent event, UUID viewerUuid) {
        Player viewer = (Player) event.getPlayer();
        if (viewer == null) return;

        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(event);

        for (int entityId : packet.getEntityIds()) {
            UUID targetUuid = PredictHologram.getOwnerByPlayerEntityId(entityId);
            if (targetUuid == null) continue;
            if (!PredictHologram.isViewing(targetUuid, viewerUuid)) continue;

            int[] ids = PredictHologram.getHologramIds(targetUuid);
            if (ids == null) continue;

            PacketUtil.send(viewer, PacketUtil.buildDestroy(ids));
            PredictHologram.removeViewer(targetUuid, viewerUuid);
        }
    }

    private void handlePosSync(PacketSendEvent event, UUID viewerUuid, int entityId, double baseX, double baseY, double baseZ, boolean onGround) {
        Player player = (Player) event.getPlayer();

        UUID targetUuid = PredictHologram.getOwnerByPlayerEntityId(entityId);
        if (targetUuid == null) return;
        if (!PredictHologram.isViewing(targetUuid, viewerUuid)) return;

        int[] ids = PredictHologram.getHologramIds(targetUuid);
        if (ids == null) return;

        for (int i = 0; i < ids.length; i++) {
            PacketUtil.sendTeleport(player, ids[i], baseX, baseY + PredictHologram.yOffsets[i], baseZ, 0, 0, onGround);
        }
    }

    private void handleRelativeMove(PacketSendEvent event, UUID viewerUuid) {
        Player player = (Player) event.getPlayer();
        WrapperPlayServerEntityRelativeMove packet = new WrapperPlayServerEntityRelativeMove(event);

        UUID targetUuid = PredictHologram.getOwnerByPlayerEntityId(packet.getEntityId());
        if (targetUuid == null) return;
        if (!PredictHologram.isViewing(targetUuid, viewerUuid)) return;

        int[] ids = PredictHologram.getHologramIds(targetUuid);
        if (ids == null) return;

        for (int hologramId : ids) {
            PacketUtil.sendRelMove(player, hologramId, packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ(), packet.isOnGround());
        }
    }

    private void handleRelativeMoveAndRotation(PacketSendEvent event, UUID viewerUuid) {
        Player player = (Player) event.getPlayer();
        WrapperPlayServerEntityRelativeMoveAndRotation packet = new WrapperPlayServerEntityRelativeMoveAndRotation(event);

        UUID targetUuid = PredictHologram.getOwnerByPlayerEntityId(packet.getEntityId());
        if (targetUuid == null) return;
        if (!PredictHologram.isViewing(targetUuid, viewerUuid)) return;

        int[] ids = PredictHologram.getHologramIds(targetUuid);
        if (ids == null) return;

        for (int hologramId : ids) {
            PacketUtil.sendRelMoveAndLook(player, hologramId, packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ(), 0, 0, packet.isOnGround());
        }
    }
}