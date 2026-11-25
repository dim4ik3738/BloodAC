package me.dim4ik.utils.config;

import me.dim4ik.BloodAC;
import me.dim4ik.suspectMenu.SuspectMenu;
import me.dim4ik.utils.lang.Lang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import me.dim4ik.utils.buffer.BufferActions;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
    private final String fileName = "config.yml";
    private List<String> lines;
    private Map<String, String> cache = new HashMap<>();

    public ConfigManager() {
        load();
    }

    public void load() {
        try {
            File file = getFile();
            if (!file.exists()) {
                BloodAC.getInstance().saveResource(fileName, false);
            }
            syncWithDefaults();
            refresh();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void reload() {
        try {
            syncWithDefaults();
            refresh();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        closeMenus();
        Lang.load(getString("language"));
        BufferActions.validate();
        BloodAC.getInstance().checksManager.reload();
        BloodAC.licenseKey = getString("Key");
    }

    private void closeMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            java.util.UUID id = player.getUniqueId();
            if (SuspectMenu.openMainViewers.containsKey(id)
                    || SuspectMenu.openDetailViewers.containsKey(id)
                    || SuspectMenu.openPredictsViewers.containsKey(id)) {
                player.closeInventory();
            }
        }
        SuspectMenu.openMainViewers.clear();
        SuspectMenu.openDetailViewers.clear();
        SuspectMenu.openPredictsViewers.clear();
    }

    private void syncWithDefaults() throws IOException {
        File file = getFile();
        List<String> userLines = Files.readAllLines(file.toPath());

        List<String> defaultLines = readDefaultLines();
        if (defaultLines == null) return;

        Files.write(file.toPath(), merge(defaultLines, userLines));
    }

    private List<String> readDefaultLines() throws IOException {
        try (InputStream is = BloodAC.getInstance().getResource(fileName)) {
            if (is == null) return null;
            List<String> defaultLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) defaultLines.add(line);
            }
            return defaultLines;
        }
    }

    private void refresh() throws IOException {
        lines = Files.readAllLines(getFile().toPath());
        cache = parseValues(lines);

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(getFile());
        } catch (IOException | InvalidConfigurationException e) {
            BloodAC.getInstance().getLogger().severe("config.yml has a YAML syntax error (" + e.getMessage()
                    + "). Default buffer actions are used until it is fixed. Hint: put alert/command texts in quotes");
            yaml = new YamlConfiguration();
            List<String> defaults = readDefaultLines();
            if (defaults != null) {
                try {
                    yaml.loadFromString(String.join("\n", defaults));
                } catch (InvalidConfigurationException ignored) {
                }
            }
        }
        BufferActions.load(yaml);
    }

    public void set(String path, Object value) {
        if (lines == null) return;

        String[] parts = path.split("\\.");
        int depth = 0;
        int targetIndent = (parts.length - 1) * 2;
        boolean inSection = parts.length == 1;
        int sectionDepth = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

            int indent = getIndent(line);

            if (parts.length > 1 && !inSection) {
                if (indent == (depth * 2)) {
                    String key = extractKey(line);
                    if (key != null && key.equals(parts[depth])) {
                        depth++;
                        if (depth == parts.length - 1) {
                            inSection = true;
                            sectionDepth = indent;
                        }
                    }
                } else if (indent < (depth * 2)) {
                    depth = indent / 2;
                }
                continue;
            }

            if (indent == targetIndent) {
                String key = extractKey(line);
                if (key != null && key.equals(parts[parts.length - 1])) {
                    lines.set(i, replaceValue(line, String.valueOf(value)));
                    cache.put(path, String.valueOf(value));
                    try {
                        save();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }

            if (parts.length > 1 && indent <= sectionDepth && !line.trim().startsWith(parts[depth - 1])) {
                break;
            }
        }
    }

    public void save() throws IOException {
        if (lines == null) return;
        Files.write(getFile().toPath(), lines);
    }

    public String getString(String path) {
        return cache.get(path);
    }

    public boolean getBoolean(String path) {
        String val = cache.get(path);
        return val != null && val.trim().equalsIgnoreCase("true");
    }

    public int getInt(String path) {
        return (int) getDouble(path);
    }

    public double getDouble(String path) {
        String val = cache.get(path);
        if (val == null) return 0.0;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static List<String> merge(List<String> defaultLines, List<String> userLines) {
        Map<String, String> userValues = parseValues(userLines);
        List<String> result = new ArrayList<>();
        Deque<String> sections = new ArrayDeque<>();
        Deque<Integer> indents = new ArrayDeque<>();
        int skipIndent = -1;

        for (int di = 0; di < defaultLines.size(); di++) {
            String line = defaultLines.get(di);
            if (skipIndent >= 0) {
                if (line.trim().isEmpty() || getIndent(line) <= skipIndent) skipIndent = -1;
                else continue;
            }
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                result.add(line);
                continue;
            }

            int indent = getIndent(line);
            String key = extractKey(line);
            if (key == null) {
                result.add(line);
                continue;
            }

            while (!indents.isEmpty() && indents.peekLast() >= indent) {
                indents.pollLast();
                sections.pollLast();
            }

            String fullKey = sections.isEmpty() ? key : String.join(".", sections) + "." + key;
            String value = extractValue(line);

            if (value != null && !value.isEmpty()) {
                String existing = userValues.get(fullKey);
                result.add(existing != null ? replaceValue(line, existing) : line);
                continue;
            }

            sections.addLast(key);
            indents.addLast(indent);

            if (key.equals("bufferActions")) {
                List<String> body = findUserBody(userLines, fullKey);
                if (body != null) {
                    result.add(line);
                    for (int j = di + 1; j < defaultLines.size(); j++) {
                        String l = defaultLines.get(j);
                        if (l.trim().startsWith("#") && getIndent(l) > indent) result.add(l);
                        else break;
                    }
                    result.addAll(body);
                    skipIndent = indent;
                    continue;
                }
            }
            result.add(line);
        }
        return result;
    }

    private static List<String> findUserBody(List<String> userLines, String targetKey) {
        Deque<String> sections = new ArrayDeque<>();
        Deque<Integer> indents = new ArrayDeque<>();

        for (int i = 0; i < userLines.size(); i++) {
            String line = userLines.get(i);
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

            int indent = getIndent(line);
            String key = extractKey(line);
            if (key == null) continue;

            while (!indents.isEmpty() && indents.peekLast() >= indent) {
                indents.pollLast();
                sections.pollLast();
            }

            String fullKey = sections.isEmpty() ? key : String.join(".", sections) + "." + key;
            String value = extractValue(line);
            boolean isSection = value == null || value.isEmpty();

            if (fullKey.equals(targetKey)) {
                if (!isSection) return null;
                int last = -1;
                for (int j = i + 1; j < userLines.size(); j++) {
                    String l = userLines.get(j);
                    if (l.trim().isEmpty() || l.trim().startsWith("#")) continue;
                    if (getIndent(l) > indent) last = j;
                    else break;
                }
                List<String> body = new ArrayList<>();
                int first = i + 1;
                while (first <= last && (userLines.get(first).trim().isEmpty() || userLines.get(first).trim().startsWith("#"))) first++;
                for (int j = first; j <= last; j++) body.add(userLines.get(j));
                return body.isEmpty() ? null : body;
            }

            if (isSection) {
                sections.addLast(key);
                indents.addLast(indent);
            }
        }
        return null;
    }

    private static Map<String, String> parseValues(List<String> fileLines) {
        Map<String, String> map = new HashMap<>();
        Deque<String> sections = new ArrayDeque<>();
        Deque<Integer> indents = new ArrayDeque<>();

        for (String line : fileLines) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

            int indent = getIndent(line);
            String key = extractKey(line);
            String value = extractValue(line);
            if (key == null) continue;

            while (!indents.isEmpty() && indents.peekLast() >= indent) {
                indents.pollLast();
                sections.pollLast();
            }

            if (value != null && !value.isEmpty()) {
                map.put(sections.isEmpty() ? key : String.join(".", sections) + "." + key, value);
            } else {
                sections.addLast(key);
                indents.addLast(indent);
            }
        }
        return map;
    }

    private static String extractKey(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("#") || trimmed.startsWith("-") || !trimmed.contains(":")) return null;
        return trimmed.split(":")[0].trim();
    }

    private static String extractValue(String line) {
        int idx = line.indexOf(':');
        if (idx < 0) return null;
        String val = line.substring(idx + 1).trim();
        int commentIdx = val.indexOf(" #");
        if (commentIdx > 0) val = val.substring(0, commentIdx).trim();
        return val.isEmpty() ? null : val;
    }

    private static String replaceValue(String line, String newValue) {
        int idx = line.indexOf(':');
        if (idx < 0) return line;
        String afterColon = line.substring(idx + 1);
        String inlineComment = "";
        int commentIdx = afterColon.indexOf(" #");
        if (commentIdx > 0) inlineComment = " " + afterColon.substring(commentIdx).trim();
        return line.substring(0, idx + 1) + " " + newValue + inlineComment;
    }

    private static int getIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else break;
        }
        return count;
    }

    private File getFile() {
        return new File(BloodAC.getInstance().getDataFolder(), fileName);
    }
}