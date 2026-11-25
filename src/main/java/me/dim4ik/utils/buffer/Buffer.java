package me.dim4ik.utils.buffer;

import me.dim4ik.BloodAC;
import me.dim4ik.utils.buffer.BufferActions.Action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Buffer {
    public static final class Result {
        public final float buffer;
        public final float reached;
        public final float delta;
        public final List<Action> actions;

        private Result(float buffer, float reached, float delta, List<Action> actions) {
            this.buffer = buffer;
            this.reached = reached;
            this.delta = delta;
            this.actions = actions;
        }
    }

    public static final Map<UUID, Float> aimBuffer = new ConcurrentHashMap<>();
    public static final Map<UUID, Float> attackBuffer = new ConcurrentHashMap<>();

    public static Result updateAim(UUID uuid, float prediction) {
        return update(aimBuffer, "Checks.Aim", uuid, prediction);
    }

    public static Result updateAttack(UUID uuid, float prediction) {
        return update(attackBuffer, "Checks.Attack", uuid, prediction);
    }

    private static Result update(Map<UUID, Float> map, String path, UUID uuid, float prediction) {
        float decrease = (float) BloodAC.getInstance().configManager.getDouble(path + ".bufferDecrease");
        float increase = (float) BloodAC.getInstance().configManager.getDouble(path + ".bufferIncrease");
        NavigableMap<Integer, List<Action>> actions = BufferActions.get(path);

        float delta = computeDelta(prediction, decrease, increase);

        float[] prev = new float[1];
        float[] next = new float[1];
        float stored = map.compute(uuid, (key, current) -> {
            float base = current == null ? 0f : current;
            prev[0] = base;
            next[0] = Math.max(0f, base + delta);
            boolean reachedLast = !actions.isEmpty() && base <= actions.lastKey() && next[0] > actions.lastKey();
            return reachedLast ? 0f : next[0];
        });

        List<Action> fired = new ArrayList<>();
        if (next[0] > prev[0]) {
            for (Map.Entry<Integer, List<Action>> e : actions.entrySet()) {
                int value = e.getKey();
                if (value > 0 && prev[0] <= value && next[0] > value) fired.addAll(e.getValue());
            }
        }
        return new Result(stored, next[0], delta, fired.isEmpty() ? Collections.emptyList() : fired);
    }

    public static float getAim(UUID uuid) {
        return aimBuffer.getOrDefault(uuid, 0f);
    }

    public static float getAttack(UUID uuid) {
        return attackBuffer.getOrDefault(uuid, 0f);
    }

    public static void resetAim(UUID uuid) {
        aimBuffer.remove(uuid);
    }

    public static void resetAttack(UUID uuid) {
        attackBuffer.remove(uuid);
    }

    public static void resetAll(UUID uuid) {
        aimBuffer.remove(uuid);
        attackBuffer.remove(uuid);
    }

    public static float computeDelta(float prediction, float decrease, float increase) {
        if (prediction <= 0f) {
            return -decrease;
        }
        if (prediction < 10f) {
            return -decrease * (1f - prediction / 10f);
        }
        if (prediction <= 75f) {
            return 0f;
        }
        if (prediction >= 100f) {
            return increase;
        }
        return increase * (prediction - 75f) / 25f;
    }
}
