package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import me.dim4ik.suspectMenu.SuspectMenu;
import me.dim4ik.utils.PredictHologram;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HitMissTracker extends PacketListenerAbstract implements Listener {
    public static final Map<UUID, Boolean> wasAttack = new ConcurrentHashMap<>();

    public static final Map<UUID, Integer> totalHits = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> totalMisses = new ConcurrentHashMap<>();

    public static final Map<UUID, Boolean> breakingBlock = new ConcurrentHashMap<>();
    public static final Map<UUID, Boolean> setNextSwingBreakingBlock = new ConcurrentHashMap<>();
    public static final Map<UUID, Boolean> cancelNextSwing = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> lastSwingTime = new ConcurrentHashMap<>();

    public HitMissTracker() {
        PacketListener.addListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
            Player player = event.getPlayer();
            if (player == null) return;
            UUID uuid = player.getUniqueId();

            if (wrapper.getAction() == DiggingAction.START_DIGGING) {
                Vector3i pos = wrapper.getBlockPosition();
                Block bukkitBlock = player.getWorld().getBlockAt(pos.x, pos.y, pos.z);
                if (!isInstantBreak(bukkitBlock.getType()) && player.getGameMode() != GameMode.CREATIVE) {
                    setNextSwingBreakingBlock.put(uuid, true);
                }
            }

            if (wrapper.getAction() == DiggingAction.CANCELLED_DIGGING) {
                breakingBlock.put(uuid, false);
                setNextSwingBreakingBlock.put(uuid, false);
            }

            if (wrapper.getAction() == DiggingAction.FINISHED_DIGGING) {
                breakingBlock.put(uuid, false);
                setNextSwingBreakingBlock.put(uuid, false);
                lastSwingTime.put(uuid, System.currentTimeMillis());
            }

            if (wrapper.getAction() == DiggingAction.DROP_ITEM || wrapper.getAction() == DiggingAction.DROP_ITEM_STACK) {
                cancelNextSwing.put(uuid, true);
            }
        }
    }

    public boolean isInstantBreak(Material material) {
        return material.getHardness() == 0.0f;
    }

    public static void onAttack(Player player) {
        wasAttack.put(player.getUniqueId(), true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        cancelNextSwing.put(event.getPlayer().getUniqueId(), true);
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (breakingBlock.getOrDefault(uuid, false)){
            return;
        }

        if (cancelNextSwing.getOrDefault(uuid, false)) {
            cancelNextSwing.put(uuid, false);
            return;
        }

        if (lastSwingTime.containsKey(uuid)) {
            long time = System.currentTimeMillis() - lastSwingTime.get(uuid);

            if (time < 100) {
                lastSwingTime.put(uuid, System.currentTimeMillis());
                return;
            }
        }

        lastSwingTime.put(uuid, System.currentTimeMillis());

        boolean playerNearby = !player.getWorld().getNearbyEntities(player.getLocation(), 6, 6, 6, e -> e instanceof Player && !e.equals(player)).isEmpty();
        boolean attackRecently = AttackListener.hasAttackedRecently(uuid);

        if (!attackRecently) {
            wasAttack.put(uuid, false);
            return;
        }

        if (!playerNearby) {
            wasAttack.put(uuid, false);
            return;
        }

        boolean hit = wasAttack.getOrDefault(uuid, false);
        wasAttack.put(uuid, false);

        if (hit) {
            totalHits.merge(uuid, 1, Integer::sum);
        } else {
            totalMisses.merge(uuid, 1, Integer::sum);
        }

        PredictHologram.updateMisHit(player, totalHits.getOrDefault(uuid, 0), totalMisses.getOrDefault(uuid, 0), getHitRateAll(uuid));
        SuspectMenu.refreshHitMiss(uuid);

        if (setNextSwingBreakingBlock.getOrDefault(uuid, false)) {
            breakingBlock.put(uuid, true);
        }
    }

    public static float getHitRateAll(UUID uuid) {
        float hits = totalHits.getOrDefault(uuid, 0);
        float misses = totalMisses.getOrDefault(uuid, 0);
        float total = hits + misses;
        return total > 0 ? (hits / total) * 100f : 0f;
    }

    public static void clearFlags(UUID uuid) {
        wasAttack.remove(uuid);
        lastSwingTime.remove(uuid);
        setNextSwingBreakingBlock.remove(uuid);
        breakingBlock.remove(uuid);
    }
}