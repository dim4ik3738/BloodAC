package me.dim4ik.utils;

import org.bukkit.Keyed;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundUtil {
    private SoundUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null) return null;
        String name = raw.trim();
        if (name.length() >= 2 && ((name.startsWith("\"") && name.endsWith("\"")) || (name.startsWith("'") && name.endsWith("'")))) {
            name = name.substring(1, name.length() - 1).trim();
        }
        if (name.matches("[A-Z0-9_]+")) {
            try {
                Object sound = Sound.class.getMethod("valueOf", String.class).invoke(null, name);
                return ((Keyed) sound).getKey().toString();
            } catch (Exception ignored) {
            }
        }
        return name.toLowerCase();
    }

    public static boolean isValid(String arg) {
        if (arg == null) return false;
        String[] parts = arg.trim().split("[\\s,]+");
        if (parts.length == 0 || parts.length > 3 || normalize(parts[0]).isEmpty()) return false;
        try {
            for (int i = 1; i < parts.length; i++) Float.parseFloat(parts[i]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static void play(Player player, String arg) {
        String[] parts = arg.trim().split("[\\s,]+");
        String name = normalize(parts[0]);
        if (name == null || name.isEmpty()) return;
        float volume = 1f;
        float pitch = 1f;
        try {
            if (parts.length > 1) volume = Float.parseFloat(parts[1]);
            if (parts.length > 2) pitch = Float.parseFloat(parts[2]);
        } catch (NumberFormatException ignored) {
        }
        player.playSound(player.getLocation(), name, volume, pitch);
    }
}
