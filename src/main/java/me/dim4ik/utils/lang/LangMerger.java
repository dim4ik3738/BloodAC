package me.dim4ik.utils.lang;

import java.util.List;

public final class LangMerger {
    private LangMerger() {
    }

    public static void insert(List<String> lines, String path, String value) {
        String[] parts = path.split("\\.");
        int from = 0;
        int to = lines.size();
        int indent = 0;
        int depth = 0;

        while (depth < parts.length - 1) {
            int header = findChild(lines, from, to, indent, parts[depth]);
            if (header < 0) break;
            int headerIndent = indentOf(lines.get(header));
            int end = blockEnd(lines, header);
            from = header + 1;
            to = end;
            indent = childIndent(lines, from, to, headerIndent);
            depth++;
        }

        int pos;
        boolean newTopSection = false;
        if (depth == 0) {
            pos = lines.size();
            while (pos > 0 && lines.get(pos - 1).trim().isEmpty()) pos--;
            newTopSection = parts.length > 1 && pos > 0;
        } else {
            pos = to;
        }

        java.util.ArrayList<String> add = new java.util.ArrayList<>();
        if (newTopSection) add.add("");
        for (int k = depth; k < parts.length; k++) {
            String pad = " ".repeat(indent + 2 * (k - depth));
            if (k == parts.length - 1) add.add(pad + parts[k] + ": " + quote(value));
            else add.add(pad + parts[k] + ":");
        }
        lines.addAll(pos, add);
    }

    /**
     * Returns dotted paths of all leaf keys (keys that hold a value, not a section),
     * read straight from the file text, so it does not depend on how the YAML parser types keys.
     */
    public static List<String> leafPaths(List<String> lines) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> indents = new java.util.ArrayList<>();
        java.util.ArrayList<String> names = new java.util.ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!isContent(line)) continue;
            String key = keyOf(line);
            if (key == null) continue;
            int indent = indentOf(line);

            while (!indents.isEmpty() && indents.get(indents.size() - 1) >= indent) {
                indents.remove(indents.size() - 1);
                names.remove(names.size() - 1);
            }

            boolean section = false;
            String value = line.trim().substring(line.trim().indexOf(':') + 1).trim();
            if (value.isEmpty() || value.startsWith("#")) {
                for (int j = i + 1; j < lines.size(); j++) {
                    if (!isContent(lines.get(j))) continue;
                    section = indentOf(lines.get(j)) > indent && !lines.get(j).trim().startsWith("-");
                    break;
                }
            }

            if (section) {
                indents.add(indent);
                names.add(key);
            } else {
                result.add(names.isEmpty() ? key : String.join(".", names) + "." + key);
            }
        }
        return result;
    }

    /**
     * Removes the key at the given dotted path (together with the comments right above it and
     * any parent sections that become empty). Returns false when the key was not found.
     */
    public static boolean remove(List<String> lines, String path) {
        String[] parts = path.split("\\.");
        int[] headers = new int[parts.length];
        int from = 0;
        int to = lines.size();
        int indent = 0;

        for (int d = 0; d < parts.length; d++) {
            int header = findChild(lines, from, to, indent, parts[d]);
            if (header < 0) return false;
            headers[d] = header;
            if (d < parts.length - 1) {
                int headerIndent = indentOf(lines.get(header));
                int end = blockEnd(lines, header);
                from = header + 1;
                to = end;
                indent = childIndent(lines, from, to, headerIndent);
            }
        }

        removeBlock(lines, headers[parts.length - 1]);
        for (int d = parts.length - 2; d >= 0; d--) {
            int header = headers[d];
            if (blockEnd(lines, header) > header + 1) break;
            removeBlock(lines, header);
        }
        return true;
    }

    private static void removeBlock(List<String> lines, int header) {
        int end = blockEnd(lines, header);
        int start = header;
        int indent = indentOf(lines.get(header));
        while (start > 0) {
            String prev = lines.get(start - 1);
            if (prev.trim().startsWith("#") && indentOf(prev) == indent) start--;
            else break;
        }
        lines.subList(start, end).clear();
        if (start > 0 && start < lines.size() && lines.get(start - 1).trim().isEmpty() && lines.get(start).trim().isEmpty()) {
            lines.remove(start);
        }
    }

    public static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    private static boolean isContent(String line) {
        String t = line.trim();
        return !t.isEmpty() && !t.startsWith("#");
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        return i;
    }

    private static String keyOf(String line) {
        String t = line.trim();
        if (t.startsWith("-")) return null;
        int idx = t.indexOf(':');
        if (idx <= 0) return null;
        String name = t.substring(0, idx).trim();
        if (name.length() >= 2 && ((name.startsWith("\"") && name.endsWith("\"")) || (name.startsWith("'") && name.endsWith("'")))) {
            name = name.substring(1, name.length() - 1);
        }
        return name;
    }

    private static int findChild(List<String> lines, int from, int to, int indent, String key) {
        for (int i = from; i < to; i++) {
            String line = lines.get(i);
            if (!isContent(line) || indentOf(line) != indent) continue;
            if (key.equals(keyOf(line))) return i;
        }
        return -1;
    }

    private static int blockEnd(List<String> lines, int header) {
        int headerIndent = indentOf(lines.get(header));
        int last = header;
        for (int i = header + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!isContent(line)) continue;
            if (indentOf(line) > headerIndent) last = i;
            else break;
        }
        return last + 1;
    }

    private static int childIndent(List<String> lines, int from, int to, int headerIndent) {
        for (int i = from; i < to; i++) {
            if (isContent(lines.get(i))) return indentOf(lines.get(i));
        }
        return headerIndent + 2;
    }
}
