package me.dim4ik.suspectMenu;

import me.dim4ik.checks.AimNeuro;
import me.dim4ik.checks.AttackNeuro;
import me.dim4ik.utils.ColorUtil;
import me.dim4ik.utils.SuspectRules;
import me.dim4ik.utils.lang.Lang;
import me.dim4ik.utils.math.MathUtil;
import me.dim4ik.utils.packet.listeners.HitMissTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SuspectMenu {
    public static String titleMain() {
        return Lang.get("menu.title_main");
    }

    public static String titleDetail() {
        return Lang.get("menu.title_detail");
    }

    public static String titlePredicts() {
        return Lang.get("menu.title_predicts");
    }

    private static final int PAGE_SIZE = 45;
    public static final int SLOT_PREV = 48;
    private static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT = 50;

    public static final int PREDICTS_PAGE_SIZE = 45;
    public static final int PREDICTS_SLOT_PREV = 48;
    public static final int PREDICTS_SLOT_BACK = 49;
    public static final int PREDICTS_SLOT_NEXT = 50;

    public static final Map<UUID, Map<Integer, UUID>> slotUuidMap = new ConcurrentHashMap<>();

    public static final Map<UUID, Integer> openMainViewers = new ConcurrentHashMap<>();
    public static final Map<UUID, UUID> openDetailViewers = new ConcurrentHashMap<>();

    public static final Map<UUID, PredictsViewState> openPredictsViewers = new ConcurrentHashMap<>();

    public static class PredictsViewState {
        public final UUID target;
        public final String type;
        public int page;

        public PredictsViewState(UUID target, String type, int page) {
            this.target = target;
            this.type = type;
            this.page = page;
        }
    }

    public static void openMain(Player viewer, int page) {
        List<UUID> suspects = getSuspects();
        int totalPages = totalPages(suspects.size(), PAGE_SIZE);
        page = clampPage(page, totalPages);

        openMainViewers.put(viewer.getUniqueId(), page);
        openDetailViewers.remove(viewer.getUniqueId());
        openPredictsViewers.remove(viewer.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54, titleMain());
        populateSuspectPage(inv, viewer, suspects, page, totalPages);

        viewer.openInventory(inv);
    }

    public static void openMain(Player viewer) {
        openMain(viewer, 0);
    }

    public static void refreshMain(Player viewer, int page) {
        if (!viewer.getOpenInventory().getTitle().equals(titleMain())) return;

        Inventory inv = viewer.getOpenInventory().getTopInventory();
        List<UUID> suspects = getSuspects();
        int totalPages = totalPages(suspects.size(), PAGE_SIZE);
        page = clampPage(page, totalPages);

        clearSlots(inv);
        populateSuspectPage(inv, viewer, suspects, page, totalPages);
    }

    private static void populateSuspectPage(Inventory inv, Player viewer, List<UUID> suspects, int page, int totalPages) {
        openMainViewers.put(viewer.getUniqueId(), page);

        Map<Integer, UUID> slotMap = new HashMap<>();
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, suspects.size());
        for (int i = from; i < to; i++) {
            int slot = i - from;
            inv.setItem(slot, buildSuspectHead(suspects.get(i)));
            slotMap.put(slot, suspects.get(i));
        }
        slotUuidMap.put(viewer.getUniqueId(), slotMap);

        fillGlass(inv, 45);
        buildPageNav(inv, SLOT_PREV, SLOT_NEXT, page, totalPages);
        inv.setItem(SLOT_INFO, buildPageInfo(suspects.size(), page + 1, totalPages));
    }

    private static List<UUID> getSuspects() {
        Set<UUID> allSuspects = new HashSet<>();

        allSuspects.addAll(AttackNeuro.recentAttackPredictions.keySet());
        allSuspects.addAll(AimNeuro.recentAimPredictions.keySet());

        List<UUID> suspects = new ArrayList<>();
        for (UUID uuid : allSuspects) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName() != null ? op.getName() : "";
            if (!isOnline(uuid, name)) continue;
            if (SuspectRules.isSuspect(uuid)) {
                suspects.add(uuid);
            }
        }
        suspects.sort(Comparator.comparingDouble(SuspectMenu::averageScore).reversed());
        return suspects;
    }

    public static void openDetail(Player viewer, UUID target) {
        openDetailViewers.put(viewer.getUniqueId(), target);
        openPredictsViewers.remove(viewer.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 18, titleDetail() + nameOf(target));
        populateDetail(inv, target);

        viewer.openInventory(inv);
    }

    public static void refreshDetail(Player viewer, UUID target) {
        if (!viewer.getOpenInventory().getTitle().startsWith(titleDetail())) return;

        Inventory inv = viewer.getOpenInventory().getTopInventory();
        populateDetail(inv, target);
    }

    private static void populateDetail(Inventory inv, UUID target) {
        inv.setItem(4, buildSuspectHead(target));
        inv.setItem(11, buildAimAllButton(target));
        inv.setItem(15, buildAttackAllButton(target));
        inv.setItem(17, buildBack(Lang.get("menu.back_to_list")));
    }

    private static ItemStack buildAttackAllButton(UUID uuid) {
        float last10 = attackAvg(uuid, 10);
        float allTime = MathUtil.average(AttackNeuro.recentAttackPredictions.get(uuid));

        List<String> lore = new ArrayList<>();
        lore.add("");
        String label = "Attack";
        lore.add(Lang.get("menu.last10", "label", label, "value", formatScore(last10)));
        lore.add(Lang.get("menu.last1000", "label", label, "value", formatScore(allTime)));
        lore.add("");
        lore.add(Lang.get("menu.open_predicts"));

        return buildItem(Material.PAPER, ChatColor.RED, label, lore);
    }

    private static ItemStack buildAimAllButton(UUID uuid) {
        float last10 = aimAvg(uuid, 10);
        float allTime = MathUtil.average(AimNeuro.recentAimPredictions.get(uuid));

        List<String> lore = new ArrayList<>();
        lore.add("");
        String label = "Aim";
        lore.add(Lang.get("menu.last10", "label", label, "value", formatScore(last10)));
        lore.add(Lang.get("menu.last1000", "label", label, "value", formatScore(allTime)));
        lore.add("");
        lore.add(Lang.get("menu.open_predicts"));

        return buildItem(Material.PAPER, ChatColor.RED, label, lore);
    }

    public static void openPredicts(Player viewer, UUID target, String type, int page) {
        List<Float> values = getPredictValues(target, type);
        Collections.reverse(values);
        int totalPages = totalPages(values.size(), PREDICTS_PAGE_SIZE);
        page = clampPage(page, totalPages);

        openPredictsViewers.put(viewer.getUniqueId(), new PredictsViewState(target, type, page));

        String typeLabel = type.equals("attack") ? "Attack" : "Aim";
        Inventory inv = Bukkit.createInventory(null, 54, titlePredicts() + typeLabel + " > " + nameOf(target));
        populatePredictsPage(inv, values, page, totalPages);

        viewer.openInventory(inv);
    }

    public static void refreshPredicts(Player viewer, PredictsViewState state) {
        if (!viewer.getOpenInventory().getTitle().startsWith(titlePredicts())) return;

        Inventory inv = viewer.getOpenInventory().getTopInventory();

        List<Float> values = getPredictValues(state.target, state.type);
        Collections.reverse(values);
        int totalPages = totalPages(values.size(), PREDICTS_PAGE_SIZE);
        state.page = clampPage(state.page, totalPages);

        populatePredictsPage(inv, values, state.page, totalPages);
    }

    private static void populatePredictsPage(Inventory inv, List<Float> values, int page, int totalPages) {
        fillGlass(inv, 45);

        int from = page * PREDICTS_PAGE_SIZE;
        int to = Math.min(from + PREDICTS_PAGE_SIZE, values.size());
        for (int i = from; i < to; i++) {
            inv.setItem(i - from, buildPredPane(values.get(i)));
        }

        buildPageNav(inv, PREDICTS_SLOT_PREV, PREDICTS_SLOT_NEXT, page, totalPages);
        inv.setItem(PREDICTS_SLOT_BACK, buildBack(Lang.get("menu.back_to_player")));
    }

    private static List<Float> getPredictValues(UUID target, String type) {
        ArrayDeque<Float> deque = type.equals("attack") ? AttackNeuro.recentAttackPredictions.get(target) : AimNeuro.recentAimPredictions.get(target);
        return deque != null ? new ArrayList<>(deque) : new ArrayList<>();
    }

    public static void refresh(UUID uuid) {
        refreshDetailViewersOf(uuid);
        refreshMainViewers();
        refreshPredictsViewersOf(uuid, null);
    }

    public static void refreshAimData(UUID uuid) {
        refreshMainViewers();
        refreshDetailViewersOf(uuid);
        refreshPredictsViewersOf(uuid, "aim");
    }

    public static void refreshAttackData(UUID uuid) {
        refreshMainViewers();
        refreshDetailViewersOf(uuid);
        refreshPredictsViewersOf(uuid, "attack");
    }

    public static void refreshHitMiss(UUID uuid) {
        refreshMainViewers();
        refreshDetailViewersOf(uuid);
    }

    private static void refreshDetailViewersOf(UUID target) {
        for (Map.Entry<UUID, UUID> entry : openDetailViewers.entrySet()) {
            if (entry.getValue().equals(target)) {
                Player viewer = Bukkit.getPlayer(entry.getKey());
                if (viewer != null) refreshDetail(viewer, target);
            }
        }
    }

    private static void refreshMainViewers() {
        for (Map.Entry<UUID, Integer> entry : openMainViewers.entrySet()) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null) refreshMain(viewer, entry.getValue());
        }
    }

    private static void refreshPredictsViewersOf(UUID target, String typeFilter) {
        for (Map.Entry<UUID, PredictsViewState> entry : openPredictsViewers.entrySet()) {
            PredictsViewState state = entry.getValue();
            if (!state.target.equals(target)) continue;
            if (typeFilter != null && !state.type.equals(typeFilter)) continue;

            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null) refreshPredicts(viewer, state);
        }
    }

    private static ItemStack buildSuspectHead(UUID uuid) {
        String name = nameOf(uuid);

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        float atkAvg = attackAvg(uuid, 10);
        float aimAvg = aimAvg(uuid, 10);
        String color = ColorUtil.colorForPrediction(Math.max(atkAvg, aimAvg));

        meta.setDisplayName(color + name);
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(Lang.get("menu.value", "label", "Aim", "value", formatScore(aimAvg)));
        lore.add(Lang.get("menu.value", "label", "Attack", "value", formatScore(atkAvg)));
        lore.add("");

        float missAll = HitMissTracker.getHitRateAll(uuid);
        int hits = HitMissTracker.totalHits.getOrDefault(uuid, 0);
        int misses = HitMissTracker.totalMisses.getOrDefault(uuid, 0);
        lore.add(Lang.get("menu.hit_rate", "color", ColorUtil.missColor(missAll), "rate", MathUtil.num(MathUtil.globalRound(missAll)), "hits", hits, "misses", misses));

        if (AimNeuro.recentAimPredictions.containsKey(uuid)) {
            lore.add("");
            lore.add(Lang.get("menu.recent_aim"));
            lore.add(formatLastChecks(AimNeuro.recentAimPredictions.get(uuid), 10));
        }
        if (AttackNeuro.recentAttackPredictions.containsKey(uuid)) {
            lore.add("");
            lore.add(Lang.get("menu.recent_attack"));
            lore.add(formatLastChecks(AttackNeuro.recentAttackPredictions.get(uuid), 10));
        }

        lore.add("");

        lore.add(Lang.get("menu.click_right"));
        lore.add(Lang.get("menu.click_left"));

        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private static String formatLastChecks(ArrayDeque<Float> deque, int count) {
        List<Float> values = new ArrayList<>(deque);
        int from = Math.max(0, values.size() - count);
        List<Float> last = new ArrayList<>(values.subList(from, values.size()));
        Collections.reverse(last);

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < last.size(); i++) {
            float val = last.get(i);
            line.append(ColorUtil.riskColor(val)).append(MathUtil.num(MathUtil.globalRound(val))).append("%");
            if (i != last.size() - 1) {
                line.append(" ");
            }
        }
        return line.toString();
    }

    private static ItemStack buildPredPane(float val) {
        ChatColor col;
        if (val > 90f) col = ChatColor.RED;
        else if (val > 75f) col = ChatColor.GOLD;
        else if (val > 25f) col = ChatColor.YELLOW;
        else if (val > 10f) col = ChatColor.DARK_GREEN;
        else col = ChatColor.GREEN;

        return buildItem(glassPaneMaterial(col), col, MathUtil.num(MathUtil.globalRound(val)) + "%", null);
    }

    private static ItemStack buildPageBtn(String label, String subtitle) {
        return buildItem(Material.ARROW, ChatColor.GREEN, label, Collections.singletonList(ChatColor.GRAY + subtitle));
    }

    private static ItemStack buildPageInfo(int total, int current, int totalPages) {
        return buildItem(Material.BOOK, ChatColor.WHITE, Lang.get("menu.page_info", "page", current, "total", totalPages), Collections.singletonList(Lang.get("menu.total_suspects", "total", total)));
    }

    private static ItemStack buildBack(String subtitle) {
        return buildItem(Material.ARROW, ChatColor.RED, Lang.get("menu.back"), Collections.singletonList(ChatColor.GRAY + subtitle));
    }

    private static ItemStack buildGlass() {
        return buildItem(glassPaneMaterial(ChatColor.GRAY), ChatColor.RESET, "", null);
    }

    private static ItemStack buildItem(Material mat, ChatColor color, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void fillGlass(Inventory inv, int from) {
        ItemStack glass = buildGlass();
        for (int i = from; i < inv.getSize(); i++){
            inv.setItem(i, glass);
        }
    }

    private static void clearSlots(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, null);
        }
    }

    private static void buildPageNav(Inventory inv, int prevSlot, int nextSlot, int page, int totalPages) {
        if (page > 0) {
            inv.setItem(prevSlot, buildPageBtn(Lang.get("menu.prev"), Lang.get("menu.page_sub", "page", page, "total", totalPages)));
        }
        if (page < totalPages - 1) {
            inv.setItem(nextSlot, buildPageBtn(Lang.get("menu.next"), Lang.get("menu.page_sub", "page", page + 2, "total", totalPages)));
        }
    }

    private static int totalPages(int itemCount, int pageSize) {
        return Math.max(1, (int) Math.ceil(itemCount / (double) pageSize));
    }

    private static int clampPage(int page, int totalPages) {
        if (page < 0) return 0;
        if (page >= totalPages) return totalPages - 1;
        return page;
    }

    private static Material glassPaneMaterial(ChatColor color) {
        if (color == ChatColor.YELLOW) return Material.YELLOW_STAINED_GLASS_PANE;
        if (color == ChatColor.RED) return Material.RED_STAINED_GLASS_PANE;
        if (color == ChatColor.GOLD) return Material.ORANGE_STAINED_GLASS_PANE;
        if (color == ChatColor.DARK_GREEN) return Material.GREEN_STAINED_GLASS_PANE;
        if (color == ChatColor.GREEN) return Material.LIME_STAINED_GLASS_PANE;
        if (color == ChatColor.GRAY) return Material.GRAY_STAINED_GLASS_PANE;
        return null;
    }

    private static boolean isOnline(UUID uuid, String nameHint) {
        if (Bukkit.getPlayer(uuid) != null) return true;

        String name = nameHint;
        if (name == null || name.isEmpty()) {
            name = Bukkit.getOfflinePlayer(uuid).getName();
        }
        if (name == null || name.isEmpty()) return false;

        return Bukkit.getPlayerExact(name) != null;
    }

    private static String nameOf(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : Lang.get("menu.unknown_player");
    }

    private static float aimAvg(UUID uuid, int n) {
        return MathUtil.averageLast(AimNeuro.recentAimPredictions.get(uuid), n);
    }

    private static float attackAvg(UUID uuid, int n) {
        return MathUtil.averageLast(AttackNeuro.recentAttackPredictions.get(uuid), n);
    }

    private static float averageScore(UUID uuid) {
        float atk = attackAvg(uuid, 7);
        float aim = aimAvg(uuid, 7);
        return MathUtil.clamp((atk + aim) / 2f, 0, 100);
    }

    private static String formatScore(float val) {
        return ColorUtil.riskColor(val) + MathUtil.num(MathUtil.globalRound(val)) + "%";
    }
}