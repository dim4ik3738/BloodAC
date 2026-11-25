package me.dim4ik.utils;

import me.dim4ik.utils.math.MathUtil;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {
    public static String colorForPing(int ping) {
        if (ping < 75) return "§a";
        if (ping < 150) return "§e";
        return "§c";
    }

    public static String colorForBuffer(float buffer, float maxBuffer) {
        if (buffer < maxBuffer / 2f) return "§a";
        if (buffer < maxBuffer * 2f / 3f) return "§e";
        return "§c";
    }

    public static String colorForPrediction(float p) {
        p = MathUtil.globalRound(p);
        if (p < 25) return "§a";
        if (p < 75) return "§e";
        return "§c";
    }

    public static String colorForPredictionReverse(float p) {
        p = MathUtil.globalRound(p);
        if (p < 25) return "§c";
        if (p < 75) return "§e";
        return "§a";
    }

    public static ChatColor missColor(float score) {
        score = MathUtil.globalRound(score);
        if (score < 85F) return ChatColor.GREEN;
        if (score < 95F) return ChatColor.YELLOW;
        return ChatColor.RED;
    }

    public static ChatColor riskColor(float score) {
        score = MathUtil.globalRound(score);
        if (score < 25f) return ChatColor.GREEN;
        if (score < 75f) return ChatColor.YELLOW;
        return ChatColor.RED;
    }

    public static String translateHexColors(String message) {
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(message);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder();
            replacement.append(ChatColor.COLOR_CHAR).append('x');
            for (char c : hex.toCharArray()) {
                replacement.append(ChatColor.COLOR_CHAR).append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }
}
