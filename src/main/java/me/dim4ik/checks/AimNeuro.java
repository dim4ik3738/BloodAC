package me.dim4ik.checks;

import me.dim4ik.BloodAC;
import me.dim4ik.checks.check.Check;
import me.dim4ik.checks.check.CheckHandler;
import me.dim4ik.commands.BloodACCommand;
import me.dim4ik.suspectMenu.SuspectMenu;
import me.dim4ik.utils.*;
import me.dim4ik.utils.buffer.Buffer;
import me.dim4ik.utils.math.DeltaAngles;
import me.dim4ik.utils.math.MathUtil;
import me.dim4ik.utils.packet.listeners.AttackListener;
import me.dim4ik.utils.packet.listeners.MovePacketsListener;
import me.dim4ik.utils.api.PredictClient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@CheckHandler(name = "Aim", type = "ML")
public class AimNeuro extends Check {
    public static Map<UUID, ArrayDeque<float[]>> playerHistory = new ConcurrentHashMap<>();
    public static final Map<UUID, ArrayDeque<Float>> recentAimPredictions = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> tickCounter = new ConcurrentHashMap<>();

    static final int windowSize = 100;
    static final int predictStep = 10;
    static final int inputSize = 13;

    public void onRotate(Player player, float yawSpeed, float pitchSpeed, float yaw, float pitch) {
        UUID uuid = player.getUniqueId();

        if (AttackListener.lastAttackTime.get(uuid) == null || !AttackListener.hasAttackedRecently(uuid)) {
            clear(player);
            return;
        }

        UUID victimUUID = AttackListener.lastVictim.get(uuid);
        if (victimUUID == null) return;

        Player target = Bukkit.getPlayer(victimUUID);
        if (target == null) return;

        UUID targetUUID = target.getUniqueId();

        if (MovePacketsListener.playerVelocity.get(uuid) == null) return;
        if (MovePacketsListener.playerVelocity.get(targetUUID) == null) return;
        if (MovePacketsListener.playerPos.get(uuid) == null) return;
        if (MovePacketsListener.playerPos.get(targetUUID) == null) return;

        float distance = (float) MovePacketsListener.playerPos.get(uuid).distance(MovePacketsListener.playerPos.get(targetUUID));

        if (distance < (player.isGliding() ? 20 : 6)) {
            int ticks = tickCounter.merge(uuid, 1, Integer::sum);

            float[] angles = DeltaAngles.calculate(player, target);
            float yawAngle = angles[0];
            float pitchAngle = angles[1];

            Vector forward = MathUtil.fromAngles(yaw, pitch);
            Vector right = forward.crossProduct(new Vector(0, 1, 0)).normalize();
            Vector up = new Vector(0, 1, 0);

            Vector targetVelocity = MovePacketsListener.playerVelocity.get(targetUUID);
            float targetForward = (float) targetVelocity.dot(forward);
            float targetStrafe = (float) targetVelocity.dot(right);
            float targetVertical = (float) targetVelocity.dot(up);

            Vector selfVelocity = MovePacketsListener.playerVelocity.get(uuid);
            float selfForward = (float) selfVelocity.dot(forward);
            float selfStrafe = (float) selfVelocity.dot(right);
            float selfVertical = (float) selfVelocity.dot(up);

            float cosYaw = (float) Math.cos(Math.toRadians(yawAngle));
            float sinYaw = (float) Math.sin(Math.toRadians(yawAngle));
            float cosPitch = (float) Math.cos(Math.toRadians(pitchAngle));
            float sinPitch = (float) Math.sin(Math.toRadians(pitchAngle));

            float[] sample = {cosYaw, sinYaw, cosPitch, sinPitch, yawSpeed, pitchSpeed, targetForward, targetStrafe, targetVertical, selfForward, selfStrafe, selfVertical, distance};

            playerHistory.putIfAbsent(uuid, new ArrayDeque<>());
            ArrayDeque<float[]> history = playerHistory.get(uuid);

            synchronized (history) {
                history.addLast(sample);

                if (history.size() >= windowSize) {
                    float[] inputData = new float[windowSize * inputSize];
                    int i = 0;
                    for (float[] vec : history) {
                        if (i >= windowSize) break;
                        System.arraycopy(vec, 0, inputData, i * inputSize, inputSize);
                        i++;
                    }

                    for (int s = 0; s < predictStep; s++) {
                        if (!history.isEmpty()) history.pollFirst();
                    }

                    CompletableFuture.runAsync(() -> {
                        try {
                            boolean isFull = false;
                            if (ticks == windowSize) {
                                isFull = true;
                                tickCounter.put(uuid, 0);
                            }

                            float prediction = PredictClient.predictAim(inputData, player.getName(), isFull);

                            if (prediction == -1) {
                                return;
                            }

                            ArrayDeque<Float> recent = recentAimPredictions.computeIfAbsent(uuid, k -> new ArrayDeque<>());
                            recent.addLast(prediction);
                            if (recent.size() > 1000) {
                                recent.pollFirst();
                            }

                            PredictHologram.updateAim(player, recent);
                            SuspectMenu.refreshAimData(uuid);

                            Buffer.Result r = Buffer.updateAim(uuid, prediction);

                            for (Player player2 : Bukkit.getOnlinePlayers()) {
                                if (player2.hasPermission("BloodAC.alerts")) {
                                    String verboseTarget = BloodACCommand.monitor.get(player2.getUniqueId());
                                    if (verboseTarget != null && (verboseTarget.isEmpty() || verboseTarget.equalsIgnoreCase(player.getName()))) {
                                        player2.sendMessage(BloodAC.acPrefix + me.dim4ik.utils.lang.Lang.get("alert.monitor", "player", player.getName(), "check", "aim", "type", getType(), "value", ColorUtil.colorForPrediction(prediction) + MathUtil.num(MathUtil.globalRound(prediction)), "buffer_warning", getBufferWarning(r.buffer), "ping", getPlayerPing(player)));
                                    }
                                }
                            }

                            runBufferActions(player, r);
                            ProbActionBar.updateAim(player, prediction, r.buffer, r.delta);
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    });
                }
            }
        }
    }

    public static void clear(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerHistory.containsKey(uuid)) {
            playerHistory.remove(uuid);
            tickCounter.remove(uuid);
        }
    }
}