package me.dim4ik.utils.lang;

import me.dim4ik.BloodAC;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class Lang {
    public static final String DEFAULT = "en";
    private static final List<String> BUNDLED = List.of("en", "ru");
    private static final Pattern CODE = Pattern.compile("[a-z0-9_-]{1,32}");

    private static List<YamlConfiguration> layers = new ArrayList<>();
    private static String code = DEFAULT;

    private Lang() {
    }

    public static void load(String requested) {
        extractBundled();

        String c = normalize(requested);
        boolean unknown = !exists(c);
        if (unknown) c = DEFAULT;
        code = c;

        YamlConfiguration base = readJar(DEFAULT);

        List<String> removedLog = new ArrayList<>();
        if (base != null) {
            for (String lang : available()) {
                if (!langFile(lang).isFile()) continue;
                List<String> gone = removeObsoleteKeys(lang, base);
                if (gone.isEmpty()) continue;
                String sample = String.join(", ", gone.subList(0, Math.min(5, gone.size())));
                removedLog.add(lang + "|" + gone.size() + "|" + sample);
            }
        }

        YamlConfiguration custom = readFile(c);
        YamlConfiguration bundled = readJar(c);

        List<String> added = new ArrayList<>();
        if (custom != null && base != null) {
            added = fillMissingKeys(c, custom, bundled, base);
            if (!added.isEmpty()) custom = readFile(c);
        }

        List<YamlConfiguration> result = new ArrayList<>();
        if (custom != null) result.add(custom);
        if (bundled != null) result.add(bundled);
        if (base != null && !c.equals(DEFAULT)) result.add(base);
        layers = result;

        if (unknown) {
            BloodAC.getInstance().getLogger().warning(get("log.unknown_language", "language", String.valueOf(requested), "supported", String.join(", ", available())));
        }
        for (String entry : removedLog) {
            String[] parts = entry.split("\\|", 3);
            BloodAC.getInstance().getLogger().warning(get("log.keys_removed", "language", parts[0], "count", parts[1], "keys", parts[2]));
        }
        if (!added.isEmpty()) {
            String sample = String.join(", ", added.subList(0, Math.min(5, added.size())));
            BloodAC.getInstance().getLogger().warning(get("log.keys_added", "language", c, "count", added.size(), "keys", sample));
        }
    }

    public static String getCode() {
        return code;
    }

    public static List<String> available() {
        TreeSet<String> set = new TreeSet<>(BUNDLED);
        File[] files = langFolder().listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                name = name.substring(0, name.length() - 4);
                if (CODE.matcher(name).matches()) set.add(name);
            }
        }
        return new ArrayList<>(set);
    }

    private static String normalize(String requested) {
        if (requested == null) return DEFAULT;
        String c = requested.trim().toLowerCase(Locale.ROOT);
        if (c.length() >= 2 && ((c.startsWith("\"") && c.endsWith("\"")) || (c.startsWith("'") && c.endsWith("'")))) {
            c = c.substring(1, c.length() - 1).trim();
        }
        if (c.endsWith(".yml")) c = c.substring(0, c.length() - 4);
        return c.isEmpty() ? DEFAULT : c;
    }

    private static boolean exists(String c) {
        if (!CODE.matcher(c).matches()) return false;
        return langFile(c).isFile() || hasJar(c);
    }

    private static File langFolder() {
        return new File(BloodAC.getInstance().getDataFolder(), "lang");
    }

    private static File langFile(String c) {
        return new File(langFolder(), c + ".yml");
    }

    private static void extractBundled() {
        for (String name : BUNDLED) {
            if (langFile(name).exists()) continue;
            try {
                BloodAC.getInstance().saveResource("lang/" + name + ".yml", false);
            } catch (IllegalArgumentException e) {
                BloodAC.getInstance().getLogger().warning("Bundled language file lang/" + name + ".yml is missing in the jar");
            }
        }
    }

    private static boolean hasJar(String c) {
        try (InputStream is = BloodAC.getInstance().getResource("lang/" + c + ".yml")) {
            return is != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static YamlConfiguration readFile(String c) {
        File file = langFile(c);
        if (!file.isFile()) return null;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            BloodAC.getInstance().getLogger().severe("Cannot read lang/" + c + ".yml as UTF-8: " + e.getMessage());
            return null;
        }
    }

    private static YamlConfiguration readJar(String c) {
        try (InputStream is = BloodAC.getInstance().getResource("lang/" + c + ".yml")) {
            if (is == null) return null;
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> removeObsoleteKeys(String c, YamlConfiguration base) {
        File file = langFile(c);
        List<String> obsolete = new ArrayList<>();
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
            java.util.Set<String> known = new java.util.HashSet<>(base.getKeys(true));
            for (String key : LangMerger.leafPaths(lines)) {
                if (!known.contains(key)) obsolete.add(key);
            }
            if (obsolete.isEmpty()) return new ArrayList<>();

            List<String> done = new ArrayList<>();
            for (String key : obsolete) {
                if (LangMerger.remove(lines, key)) done.add(key);
            }
            if (done.isEmpty()) return done;
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            return done;
        } catch (IOException | RuntimeException e) {
            BloodAC.getInstance().getLogger().warning("Cannot remove " + obsolete.size() + " obsolete keys from lang/" + c + ".yml: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static List<String> fillMissingKeys(String c, YamlConfiguration custom, YamlConfiguration bundled, YamlConfiguration base) {
        Map<String, String> missing = new LinkedHashMap<>();
        for (String key : base.getKeys(true)) {
            if (!base.isString(key) || custom.contains(key) || blockedByValue(custom, key)) continue;
            String value = bundled != null && bundled.isString(key) ? bundled.getString(key) : base.getString(key);
            missing.put(key, value);
        }
        if (missing.isEmpty()) return new ArrayList<>();

        File file = langFile(c);
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
            for (Map.Entry<String, String> e : missing.entrySet()) {
                LangMerger.insert(lines, e.getKey(), e.getValue());
            }
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            return new ArrayList<>(missing.keySet());
        } catch (IOException | RuntimeException e) {
            BloodAC.getInstance().getLogger().warning("Cannot add " + missing.size() + " missing keys to lang/" + c + ".yml: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static boolean blockedByValue(YamlConfiguration custom, String key) {
        int idx = key.indexOf('.');
        while (idx > 0) {
            String parent = key.substring(0, idx);
            if (custom.contains(parent) && !custom.isConfigurationSection(parent)) return true;
            idx = key.indexOf('.', idx + 1);
        }
        return false;
    }

    public static boolean has(String key) {
        for (YamlConfiguration layer : layers) {
            if (layer.isString(key)) return true;
        }
        return false;
    }

    public static String get(String key, Object... pairs) {
        String s = null;
        for (YamlConfiguration layer : layers) {
            s = layer.getString(key);
            if (s != null) break;
        }
        if (s == null) return key;

        s = ChatColor.translateAlternateColorCodes('&', s);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            s = s.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return s;
    }

    public static String prefixed(String key, Object... pairs) {
        return BloodAC.acPrefix + get(key, pairs);
    }
}
