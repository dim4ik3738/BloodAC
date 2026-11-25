package me.dim4ik.utils.buffer;

import me.dim4ik.BloodAC;
import me.dim4ik.utils.SoundUtil;
import me.dim4ik.utils.lang.Lang;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public final class BufferActions {
    public enum Type {FLAG, PUNISH, ALERT, COMMAND, SOUND, INVALID}

    public static final class Action {
        public final Type type;
        public final String arg;
        public final String raw;

        Action(Type type, String arg, String raw) {
            this.type = type;
            this.arg = arg;
            this.raw = raw;
        }
    }

    private static volatile Map<String, NavigableMap<Integer, List<Action>>> byCheck = new HashMap<>();

    private BufferActions() {
    }

    public static void load(YamlConfiguration cfg) {
        Map<String, NavigableMap<Integer, List<Action>>> result = new HashMap<>();
        ConfigurationSection checks = cfg.getConfigurationSection("Checks");
        if (checks != null) {
            for (String name : checks.getKeys(false)) {
                ConfigurationSection check = checks.getConfigurationSection(name);
                if (check == null) continue;
                ConfigurationSection actions = check.getConfigurationSection("bufferActions");
                if (actions == null) continue;
                result.put("Checks." + name, parse(actions));
            }
        }
        byCheck = result;
    }

    private static NavigableMap<Integer, List<Action>> parse(ConfigurationSection section) {
        NavigableMap<Integer, List<Action>> map = new TreeMap<>();
        for (String key : section.getKeys(false)) {
            int value;
            try {
                value = Integer.parseInt(key.trim());
            } catch (NumberFormatException e) {
                map.computeIfAbsent(-1, k -> new ArrayList<>()).add(new Action(Type.INVALID, null, key));
                continue;
            }
            if (value <= 0) continue;

            List<Action> list = map.computeIfAbsent(value, k -> new ArrayList<>());
            Object raw = section.get(key);
            if (raw instanceof List) {
                for (Object o : (List<?>) raw) list.add(parseAction(o));
            } else if (raw != null && !(raw instanceof ConfigurationSection)) {
                list.add(parseAction(raw));
            }
        }
        return map;
    }

    private static Action parseAction(Object o) {
        String s;
        if (o instanceof Map && ((Map<?, ?>) o).size() == 1) {
            Map.Entry<?, ?> e = ((Map<?, ?>) o).entrySet().iterator().next();
            s = e.getKey() + ": " + e.getValue();
        } else {
            s = String.valueOf(o);
        }
        s = s.trim();
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.equals("flag")) return new Action(Type.FLAG, null, s);
        if (lower.equals("punish")) return new Action(Type.PUNISH, null, s);

        int idx = s.indexOf(':');
        if (idx > 0) {
            String t = s.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String arg = s.substring(idx + 1).trim();
            if (!arg.isEmpty()) {
                if (t.equals("alert") && arg.toLowerCase(Locale.ROOT).startsWith("lang:")) {
                    String key = arg.substring(5).trim();
                    if (!key.isEmpty()) return new Action(Type.ALERT, key, s);
                }
                if (t.equals("sound") && SoundUtil.isValid(arg)) return new Action(Type.SOUND, arg, s);
                if (t.equals("command")) return new Action(Type.COMMAND, arg.startsWith("/") ? arg.substring(1) : arg, s);
            }
        }
        return new Action(Type.INVALID, null, s);
    }

    public static NavigableMap<Integer, List<Action>> get(String path) {
        NavigableMap<Integer, List<Action>> map = byCheck.get(path);
        return map != null ? map : new TreeMap<>();
    }

    public static float max(String path) {
        NavigableMap<Integer, List<Action>> map = get(path);
        return map.isEmpty() || map.lastKey() <= 0 ? 0f : (float) map.lastKey();
    }

    public static void validate() {
        for (Map.Entry<String, NavigableMap<Integer, List<Action>>> check : byCheck.entrySet()) {
            for (List<Action> list : check.getValue().values()) {
                for (Action a : list) {
                    if (a.type == Type.INVALID) {
                        BloodAC.getInstance().getLogger().warning(Lang.get("log.unknown_buffer_action", "path", check.getKey() + ".bufferActions", "action", a.raw));
                    }
                }
            }
        }
    }
}
