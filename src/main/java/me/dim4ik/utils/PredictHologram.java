package me.dim4ik.utils;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.dim4ik.BloodAC;
import me.dim4ik.utils.lang.Lang;
import me.dim4ik.checks.AimNeuro;
import me.dim4ik.checks.AttackNeuro;
import me.dim4ik.commands.BloodACCommand;
import me.dim4ik.utils.math.MathUtil;
import me.dim4ik.utils.packet.FastPing;
import me.dim4ik.utils.packet.PacketUtil;
import me.dim4ik.utils.packet.listeners.HitMissTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PredictHologram {
    private static final double viewDist = 20;

    public static final int AIM = 0;
    public static final int ATTACK = 1;
    public static final int PING = 2;

    public static final double[] yOffsets = {3.0, 2.75, 2.5};

    private static final Map<UUID, int[]> holograms = new ConcurrentHashMap<>();
    private static final Map<UUID, String[]> names = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> sentTo = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> pingValues = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> pingLag = new ConcurrentHashMap<>();
    private static final Map<UUID, float[]> misHitValues = new ConcurrentHashMap<>();

    private static final Map<Integer, UUID> playerEntityIdToOwner = new ConcurrentHashMap<>();

    public static void updateAim(Player target, Deque<Float> predictions) {
        ensureExists(target);
        String[] curNames = names.get(target.getUniqueId());
        if (curNames == null) return;
        curNames[AIM] = buildLine(Lang.get("hologram.aim"), predictions);
        updateMetadata(target, AIM);
    }

    public static void updateAttack(Player target, Deque<Float> predictions) {
        ensureExists(target);
        String[] curNames = names.get(target.getUniqueId());
        if (curNames == null) return;
        curNames[ATTACK] = buildLine(Lang.get("hologram.attack"), predictions);
        updateMetadata(target, ATTACK);
    }

    public static void updatePing(Player target, int ping, boolean lag) {
        ensureExists(target);
        UUID uuid = target.getUniqueId();
        String[] curNames = names.get(uuid);
        if (curNames == null) return;
        pingValues.put(uuid, ping);
        pingLag.put(uuid, lag);
        float[] misHit = misHitValues.getOrDefault(uuid, new float[]{0, 0, 0});
        curNames[PING] = buildPingAndHitsLine(target.getUniqueId(), ping, lag, (int) misHit[0], (int) misHit[1], misHit[2]);
        updateMetadata(target, PING);
    }

    public static void updateMisHit(Player target, int hits, int misses, float hitRate) {
        ensureExists(target);
        UUID uuid = target.getUniqueId();
        String[] curNames = names.get(uuid);
        if (curNames == null) return;
        misHitValues.put(uuid, new float[]{hits, misses, hitRate});
        int ping = pingValues.getOrDefault(uuid, 0);
        boolean lag = pingLag.getOrDefault(uuid, false);
        curNames[PING] = buildPingAndHitsLine(target.getUniqueId(), ping, lag, hits, misses, hitRate);
        updateMetadata(target, PING);
    }

    public static void remove(UUID uuid) {
        int[] ids = holograms.remove(uuid);
        Set<UUID> viewers = sentTo.remove(uuid);
        names.remove(uuid);
        pingValues.remove(uuid);
        pingLag.remove(uuid);
        misHitValues.remove(uuid);
        playerEntityIdToOwner.values().removeIf(v -> v.equals(uuid));
        for (Set<UUID> v : sentTo.values()) v.remove(uuid);
        if (ids == null) return;

        WrapperPlayServerDestroyEntities destroy = PacketUtil.buildDestroy(ids);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (viewers == null || viewers.contains(online.getUniqueId())) {
                PacketUtil.send(online, destroy);
            }
        }
    }

    public static void removeAll() {
        for (UUID uuid : new HashSet<>(holograms.keySet())) {
            remove(uuid);
        }
    }

    public static void onViewerEnabled(Player viewer) {
        showExistingTo(viewer);
    }

    public static void onViewerDisabled(Player viewer) {
        UUID viewerId = viewer.getUniqueId();
        for (Map.Entry<UUID, int[]> entry : holograms.entrySet()) {
            UUID targetUuid = entry.getKey();
            int[] ids = entry.getValue();
            Set<UUID> viewers = sentTo.get(targetUuid);
            if (viewers == null) continue;
            if (viewers.remove(viewerId)) {
                PacketUtil.send(viewer, PacketUtil.buildDestroy(ids));
            }
        }
    }

    public static void showExistingTo(Player viewer) {
        for (Map.Entry<UUID, int[]> entry : holograms.entrySet()) {
            UUID targetUuid = entry.getKey();
            int[] ids = entry.getValue();

            Player target = Bukkit.getPlayer(targetUuid);
            if (target == null) continue;
            if (!canSee(viewer, target)) continue;
            if (!isInRange(viewer, target.getLocation())) continue;

            String[] curNames = names.get(targetUuid);
            if (curNames == null) continue;

            Location[] locs = buildOffsetLocations(target.getLocation());

            PacketUtil.spawnTo(viewer, ids[AIM], locs[AIM], curNames[AIM]);
            PacketUtil.spawnTo(viewer, ids[ATTACK], locs[ATTACK], curNames[ATTACK]);
            PacketUtil.spawnTo(viewer, ids[PING], locs[PING], curNames[PING]);
            sentTo.computeIfAbsent(targetUuid, k -> ConcurrentHashMap.newKeySet()).add(viewer.getUniqueId());
        }
    }

    private static void ensureExists(Player target) {
        UUID uuid = target.getUniqueId();
        if (holograms.containsKey(uuid)) return;

        int aimId = PacketUtil.nextEntityId();
        int attackId = PacketUtil.nextEntityId();
        int pingId = PacketUtil.nextEntityId();
        holograms.put(uuid, new int[]{aimId, attackId, pingId});

        playerEntityIdToOwner.put(target.getEntityId(), uuid);

        Location base = target.getLocation();
        Location[] locs = buildOffsetLocations(base);

        Deque<Float> aimPreds = AimNeuro.recentAimPredictions.get(uuid);
        Deque<Float> attackPreds = AttackNeuro.recentAttackPredictions.get(uuid);

        float hitRate = HitMissTracker.getHitRateAll(uuid);
        int hits = HitMissTracker.totalHits.getOrDefault(uuid, 0);
        int misses = HitMissTracker.totalMisses.getOrDefault(uuid, 0);

        misHitValues.put(uuid, new float[]{hits, misses, hitRate});

        names.put(uuid, new String[]{
                aimPreds != null ? buildLine(Lang.get("hologram.aim"), aimPreds) : Lang.get("hologram.aim"),
                attackPreds != null ? buildLine(Lang.get("hologram.attack"), attackPreds) : Lang.get("hologram.attack"),
                buildPingAndHitsLine(uuid, 0, false, hits, misses, hitRate)
        });

        String[] curNames = names.get(uuid);
        Set<UUID> viewers = sentTo.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());

        for (Player viewer : target.getWorld().getPlayers()) {
            if (!canSee(viewer, target)) continue;
            if (!isInRange(viewer, base)) continue;
            PacketUtil.spawnTo(viewer, aimId, locs[AIM], curNames[AIM]);
            PacketUtil.spawnTo(viewer, attackId, locs[ATTACK], curNames[ATTACK]);
            PacketUtil.spawnTo(viewer, pingId, locs[PING], curNames[PING]);
            viewers.add(viewer.getUniqueId());
        }
    }

    private static void updateMetadata(Player target, int index) {
        UUID uuid = target.getUniqueId();
        int[] ids = holograms.get(uuid);
        String[] curNames = names.get(uuid);
        if (ids == null || curNames == null) return;

        WrapperPlayServerEntityMetadata meta = PacketUtil.buildArmorStandMetadata(ids[index], curNames[index]);
        WrapperPlayServerDestroyEntities destroy = PacketUtil.buildDestroy(ids);
        Set<UUID> viewers = sentTo.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());

        for (Player viewer : target.getWorld().getPlayers()) {
            UUID viewerId = viewer.getUniqueId();
            boolean allowed = canSee(viewer, target);
            boolean inRange = isInRange(viewer, target.getLocation());
            boolean sent = viewers.contains(viewerId);

            if (allowed && inRange) {
                if (!sent) {
                    Location[] locs = buildOffsetLocations(target.getLocation());
                    PacketUtil.spawnTo(viewer, ids[AIM], locs[AIM], curNames[AIM]);
                    PacketUtil.spawnTo(viewer, ids[ATTACK], locs[ATTACK], curNames[ATTACK]);
                    PacketUtil.spawnTo(viewer, ids[PING], locs[PING], curNames[PING]);
                    viewers.add(viewerId);
                } else {
                    PacketUtil.send(viewer, meta);
                }
            } else if (sent) {
                PacketUtil.send(viewer, destroy);
                viewers.remove(viewerId);
            }
        }
    }

    private static Location[] buildOffsetLocations(Location base) {
        return new Location[]{base.clone().add(0, yOffsets[AIM], 0), base.clone().add(0, yOffsets[ATTACK], 0), base.clone().add(0, yOffsets[PING], 0)};
    }

    private static boolean canSee(Player viewer, Player target) {
        if (target.getGameMode() == org.bukkit.GameMode.SPECTATOR) return false;
        if (!BloodAC.getInstance().configManager.getBoolean("Hologram.alwaysShow") && !SuspectRules.isSuspect(target.getUniqueId())) return false;
        return !viewer.equals(target) && viewer.hasPermission("BloodAC.viewHolograms") && BloodACCommand.hologramViewers.contains(viewer.getUniqueId());
    }

    private static boolean isInRange(Player viewer, Location targetLoc) {
        return viewer.getLocation().distanceSquared(targetLoc) <= viewDist * viewDist;
    }

    private static String buildLine(String prefix, Deque<Float> predictions) {
        StringBuilder sb = new StringBuilder(prefix);
        List<Float> values = new ArrayList<>(predictions);
        int from = Math.max(0, values.size() - 5);
        values = new ArrayList<>(values.subList(from, values.size()));
        Collections.reverse(values);
        for (int i = 0; i < values.size(); i++) {
            float v = MathUtil.globalRound(values.get(i));
            String color = ColorUtil.colorForPrediction(v);
            sb.append(color).append(MathUtil.num(v)).append("%");
            if (i < values.size() - 1) sb.append(" §7| ");
        }
        return sb.toString();
    }

    private static String buildPingAndHitsLine(UUID uuid, int ping, boolean lag, int hits, int misses, float hitRate) {
        FastPing fastPing = BloodAC.getInstance().fastPing;
        int[] throttle = fastPing.isThrottled(uuid);
        String pingColor = throttle != null ? "§c" : ColorUtil.colorForPing(ping);
        String pingText = pingColor + fastPing.formatedPing(uuid) + Lang.get("hologram.ms");
        return Lang.get("hologram.ping_line",
                "ping", lag ? Lang.get("hologram.lag") : pingText,
                "hits", hits,
                "misses", misses,
                "color", ColorUtil.missColor(hitRate),
                "rate", MathUtil.num(MathUtil.globalRound(hitRate)));
    }

    public static UUID getOwnerByPlayerEntityId(int entityId) {
        return playerEntityIdToOwner.get(entityId);
    }

    public static int[] getHologramIds(UUID uuid) {
        return holograms.get(uuid);
    }

    public static boolean isViewing(UUID targetUuid, UUID viewerUuid) {
        Set<UUID> viewers = sentTo.get(targetUuid);
        return viewers != null && viewers.contains(viewerUuid);
    }

    public static void removeViewer(UUID targetUuid, UUID viewerUuid) {
        Set<UUID> viewers = sentTo.get(targetUuid);
        if (viewers != null) viewers.remove(viewerUuid);
    }
}