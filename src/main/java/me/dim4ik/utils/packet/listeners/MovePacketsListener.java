package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import me.dim4ik.BloodAC;
import me.dim4ik.checks.check.Check;
import me.dim4ik.utils.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MovePacketsListener extends PacketListenerAbstract {
    public MovePacketsListener() {
        PacketListener.addListener(this);
    }

    private final Map<UUID, Float> lastYaw = new ConcurrentHashMap<>();
    private final Map<UUID, Float> lastPitch = new ConcurrentHashMap<>();

    Map<UUID, Float> prevX = new ConcurrentHashMap<>();
    Map<UUID, Float> prevY = new ConcurrentHashMap<>();
    Map<UUID, Float> prevZ = new ConcurrentHashMap<>();

    public static Map<UUID, Vector> playerVelocity = new ConcurrentHashMap<>();
    public static Map<UUID, Vector> playerPos = new ConcurrentHashMap<>();

    Map<UUID, Float> playerYawSpeed = new ConcurrentHashMap<>();
    Map<UUID, Float> playerPitchSpeed = new ConcurrentHashMap<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        PlayerData playerData = BloodAC.getInstance().playersData.get(uuid);

        if (playerData == null) return;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            boolean isLook = event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
            boolean isPosition = event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;

            WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);

            playerData.tick++;

            if (isLook) {
                float yaw = packet.getLocation().getYaw();
                float pitch = packet.getLocation().getPitch();

                float yawSpeed = playerYawSpeed.getOrDefault(uuid, Float.MIN_VALUE);
                float pitchSpeed = playerPitchSpeed.getOrDefault(uuid, Float.MIN_VALUE);

                if (lastYaw.containsKey(uuid)) {
                    yawSpeed = yaw - lastYaw.get(uuid);
                    pitchSpeed = pitch - lastPitch.get(uuid);
                    for (Check check : BloodAC.getInstance().checksManager.enabledChecks) {
                        check.onRotate(player, yawSpeed, pitchSpeed, yaw, pitch);
                    }
                }
                lastYaw.put(uuid, yaw);
                lastPitch.put(uuid, pitch);
                playerYawSpeed.put(uuid, yawSpeed);
                playerPitchSpeed.put(uuid, pitchSpeed);
            }

            if (isPosition) {
                updatePositionAndVelocity(uuid, packet.getLocation().getX(), packet.getLocation().getY(), packet.getLocation().getZ());

                playerData.isOnGround = packet.isOnGround();

                if (playerData.isOnGround) {
                    playerData.groundTicks++;
                } else {
                    playerData.groundTicks = 0;
                }
            }

            for (Check check : BloodAC.getInstance().checksManager.enabledChecks) {
                check.onMoveRotate(player, playerVelocity.getOrDefault(uuid, new Vector(0, 0, 0)), lastYaw.getOrDefault(uuid, 0F), lastPitch.getOrDefault(uuid, 0F));
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            playerData.groundTicks = 999;
            playerData.tick++;

            WrapperPlayClientVehicleMove packet = new WrapperPlayClientVehicleMove(event);

            updatePositionAndVelocity(uuid, packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ());

            for (Check check : BloodAC.getInstance().checksManager.enabledChecks) {
                check.onMoveRotate(player, playerVelocity.getOrDefault(uuid, new Vector(0, 0, 0)), lastYaw.getOrDefault(uuid, 0F), lastPitch.getOrDefault(uuid, 0F));
            }
        }
    }

    private void updatePositionAndVelocity(UUID uuid, double x, double y, double z) {
        if (prevX.containsKey(uuid)) {
            float speedX = (float) (x - prevX.get(uuid));
            float speedY = (float) (y - prevY.get(uuid));
            float speedZ = (float) (z - prevZ.get(uuid));

            Vector velocity = new Vector(speedX, speedY, speedZ);
            playerVelocity.put(uuid, velocity);
            playerPos.put(uuid, new Vector(x, y, z));
        }
        prevX.put(uuid, (float) x);
        prevY.put(uuid, (float) y);
        prevZ.put(uuid, (float) z);
    }
}