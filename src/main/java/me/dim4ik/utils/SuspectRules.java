package me.dim4ik.utils;

import me.dim4ik.BloodAC;
import me.dim4ik.checks.AimNeuro;
import me.dim4ik.checks.AttackNeuro;
import me.dim4ik.utils.config.ConfigManager;
import me.dim4ik.utils.math.MathUtil;

import java.util.ArrayDeque;
import java.util.UUID;

public final class SuspectRules {
    private SuspectRules() {
    }

    public static boolean isSuspect(UUID uuid) {
        ConfigManager config = BloodAC.getInstance().configManager;
        return matches(config, "Aim", AimNeuro.recentAimPredictions.get(uuid))
                || matches(config, "Attack", AttackNeuro.recentAttackPredictions.get(uuid));
    }

    private static boolean matches(ConfigManager config, String check, ArrayDeque<Float> predictions) {
        if (predictions == null || predictions.isEmpty()) return false;
        String path = "Suspects." + check;
        int last = Math.max(1, Math.min(1000, config.getInt(path + ".predictions")));
        return MathUtil.averageLast(predictions, last) > config.getDouble(path + ".threshold");
    }
}
