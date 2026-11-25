package me.dim4ik.checks;

import me.dim4ik.BloodAC;
import me.dim4ik.checks.check.Check;
import me.dim4ik.checks.check.CheckHandler;
import me.dim4ik.commands.BloodACCommand;
import me.dim4ik.suspectMenu.SuspectMenu;
import me.dim4ik.utils.*;
import me.dim4ik.utils.buffer.Buffer;
import me.dim4ik.utils.math.MathUtil;
import me.dim4ik.utils.packet.listeners.AttackListener;
import me.dim4ik.utils.packet.listeners.MovePacketsListener;
import me.dim4ik.utils.packet.listeners.SprintListener;
import me.dim4ik.utils.api.PredictClient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@CheckHandler(name = "Attack", type = "ML")
public class AttackNeuro extends Check {
    public static Map<UUID, ArrayDeque<float[]>> playerHistory = new ConcurrentHashMap<>();
    public static final Map<UUID, ArrayDeque<Float>> recentAttackPredictions = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> tickCounter = new ConcurrentHashMap<>();

    static final int windowSize = 250;
    static final int predictStep = 50;
    static final int inputSize = 6;

    private final Map<UUID, Boolean> attackThisTick = new ConcurrentHashMap<>();

    @Override
    public void onSwing(Player player) {
        attackThisTick.put(player.getUniqueId(), true);
    }

    @Override
    public void onMoveRotate(Player player, Vector velocity, float yaw, float pitch) {
        UUID uuid = player.getUniqueId();

        PlayerData playerData = BloodAC.getInstance().playersData.get(uuid);

        if (playerData == null || !AttackListener.hasAttackedRecently(uuid)) {
            clear(player);
            return;
        }

        boolean attacked = false;

        if (attackThisTick.containsKey(uuid) && attackThisTick.get(uuid)) {
            attacked = true;
            attackThisTick.put(uuid, false);
        }

        UUID victimUUID = AttackListener.lastVictim.get(uuid);
        if (victimUUID == null) return;

        Player target = Bukkit.getPlayer(victimUUID);
        if (target == null) return;

        UUID targetUUID = target.getUniqueId();

        if (SprintListener.lastStartSprintTick.get(uuid) == null) return;
        if (SprintListener.lastStopSprintTick.get(uuid) == null) return;
        if (AttackListener.lastAttackTick.get(uuid) == null) return;
        if (MovePacketsListener.playerPos.get(uuid) == null) return;
        if (MovePacketsListener.playerPos.get(targetUUID) == null) return;

        float distance = (float) MovePacketsListener.playerPos.get(uuid).distance(MovePacketsListener.playerPos.get(targetUUID));

        if (distance < 6) {
            int ticks = tickCounter.merge(uuid, 1, Integer::sum);

            float[] sample = new float[]{
                    playerData.isSprinting ? 1.0F : 0.0F,
                    attacked ? 1.0F : 0.0F,
                    (float)velocity.getY(),
                    (float)(System.currentTimeMillis() - AttackListener.lastAttackTime.get(uuid)),
                    (float)(System.currentTimeMillis() - SprintListener.lastStopSprint.get(uuid)),
                    (float)(System.currentTimeMillis() - SprintListener.lastStartSprint.get(uuid))
            };

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

                            float prediction = PredictClient.predictAttack(inputData, player.getName(), isFull);

                            if (prediction == -1) {
                                return;
                            }

                            ArrayDeque<Float> recent = recentAttackPredictions.computeIfAbsent(uuid, k -> new ArrayDeque<>());
                            recent.addLast(prediction);
                            if (recent.size() > 1000) {
                                recent.pollFirst();
                            }

                            PredictHologram.updateAttack(player, recent);
                            SuspectMenu.refreshAttackData(uuid);

                            Buffer.Result r = Buffer.updateAttack(uuid, prediction);

                            for (Player player2 : Bukkit.getOnlinePlayers()) {
                                if (player2.hasPermission("BloodAC.alerts")) {
                                    String verboseTarget = BloodACCommand.monitor.get(player2.getUniqueId());
                                    if (verboseTarget != null && (verboseTarget.isEmpty() || verboseTarget.equalsIgnoreCase(player.getName()))) {
                                        player2.sendMessage(BloodAC.acPrefix + me.dim4ik.utils.lang.Lang.get("alert.monitor", "player", player.getName(), "check", "attack", "type", getType(), "value", ColorUtil.colorForPrediction(prediction) + MathUtil.num(MathUtil.globalRound(prediction)), "buffer_warning", getBufferWarning(r.buffer), "ping", getPlayerPing(player)));
                                    }
                                }
                            }

                            runBufferActions(player, r);
                            ProbActionBar.updateAttack(player, prediction, r.buffer, r.delta);
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