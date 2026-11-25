package me.dim4ik.utils.math;

import me.dim4ik.BloodAC;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MathUtil {
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float wrapDegrees(float angle) {
        angle = angle % 360.0F;
        if (angle >= 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    public static Vector fromAngles(float yaw, float pitch) {
        return new Vector(-Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)), -Math.sin(Math.toRadians(pitch)), Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))).normalize();
    }

    public static float averageLast(ArrayDeque<Float> deque, int last) {
        if (deque == null || deque.isEmpty()) return 0f;
        List<Float> list = new ArrayList<>(deque);
        int from = Math.max(0, list.size() - last);
        return average(list.subList(from, list.size()));
    }

    public static float average(List<Float> list) {
        if (list == null || list.isEmpty()) return 0f;
        float sum = 0;
        for (float f : list) sum += f;
        return sum / list.size();
    }

    public static float average(ArrayDeque<Float> deque) {
        if (deque == null || deque.isEmpty()) return 0f;
        return average(new ArrayList<>(deque));
    }

    public static float globalRound(float val) {
        int places = (int) BloodAC.getInstance().configManager.getDouble("decimalPlaces");
        return Float.parseFloat(String.format(Locale.ROOT, "%." + Math.max(places, 0) + "f", val));
    }

    public static String num(float v) {
        if (v == (int) v) {
            return String.valueOf((int) v);
        }
        return String.valueOf(v);
    }
}
