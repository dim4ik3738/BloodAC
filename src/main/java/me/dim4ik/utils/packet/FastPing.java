package me.dim4ik.utils.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import me.dim4ik.BloodAC;
import me.dim4ik.utils.PredictHologram;
import me.dim4ik.utils.packet.listeners.PacketListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FastPing extends PacketListenerAbstract {
    private final Map<UUID, Long> sentTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> answeredTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> ping = new ConcurrentHashMap<>();
    private final Map<UUID, LinkedList<Integer>> pingHistory = new ConcurrentHashMap<>();

    private static final boolean isNewServerVersion = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17);

    public FastPing() {
        PacketListener.addListener(this);

        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player player : BloodAC.getInstance().getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    Long sent = sentTime.get(uuid);

                    if (sent == null) {
                        sendPing(player);
                        continue;
                    }

                    Long answered = answeredTime.get(uuid);

                    if (!isBadClientVersion(player) && answered == null && now - sent >= 1000) {
                        PredictHologram.updatePing(player, 0, true);
                    }

                    if (answered == null && now - sent >= 10_000) {
                        sendPing(player);
                    }

                    if (answered != null) {
                        long elapsed = now - sent;
                        if (elapsed >= 1000) {
                            answeredTime.remove(uuid);
                            sendPing(player);
                        } else if (now - answered >= 1000) {
                            answeredTime.remove(uuid);
                            sendPing(player);
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(BloodAC.getInstance(), 0L, 1);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        if (isBadClientVersion(player)) {
            if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
                Bukkit.getScheduler().runTask(BloodAC.getInstance(), () -> {
                    int currentPing = player.getPing();
                    ping.put(player.getUniqueId(), currentPing);
                    PredictHologram.updatePing(player, currentPing, false);
                });
            }
        } else {
            if (event.getPacketType() == PacketType.Play.Client.PONG) {
                WrapperPlayClientPong packet = new WrapperPlayClientPong(event);

                if (packet.getId() != 1) return;

                Long sent = sentTime.get(uuid);
                if (sent != null) {
                    registerPong(player, sent);
                }
                return;
            }

            if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
                WrapperPlayClientWindowConfirmation packet = new WrapperPlayClientWindowConfirmation(event);

                if (packet.getActionId() != 1) return;

                Long sent = sentTime.get(uuid);
                if (sent != null) {
                    registerPong(player, sent);
                }
            }
        }
    }

    private void registerPong(Player player, long sent) {
        UUID uuid = player.getUniqueId();
        int currentPing = (int) (System.currentTimeMillis() - sent);
        ping.put(uuid, currentPing);

        PredictHologram.updatePing(player, currentPing, false);

        LinkedList<Integer> history = pingHistory.computeIfAbsent(uuid, k -> new LinkedList<>());
        synchronized (history) {
            history.add(currentPing);
            if (history.size() > 5) history.removeFirst();
        }
        answeredTime.put(uuid, System.currentTimeMillis());
    }

    private synchronized void sendPing(Player player) {
        UUID uuid = player.getUniqueId();
        sentTime.put(uuid, System.currentTimeMillis());

        if (isNewServerVersion) {
            if (!isBadClientVersion(player)) {
                WrapperPlayServerPing packet = new WrapperPlayServerPing(1);
                PacketUtil.send(player, packet);
            }
        } else {
            WrapperPlayServerWindowConfirmation packet = new WrapperPlayServerWindowConfirmation(0, (short) 1, false);
            PacketUtil.send(player, packet);
        }
    }

    public boolean isBadClientVersion(Player player) {
        ClientVersion clientVersion = PacketEvents.getAPI().getPlayerManager().getClientVersion(player);
        return isNewServerVersion && clientVersion.isOlderThan(ClientVersion.V_1_17);
    }

    public String formatedPing(UUID uuid) {
        int playerPing = ping.getOrDefault(uuid, -1);
        int[] throttle = isThrottled(uuid);
        if (throttle != null) {
            return "(" + throttle[0] + "-" + throttle[1] + ")";
        }
        return String.valueOf(playerPing);
    }

    public int[] isThrottled(UUID uuid) {
        LinkedList<Integer> history = pingHistory.get(uuid);
        if (history == null) return null;

        int min;
        int max;
        synchronized (history) {
            if (history.size() < 5) return null;
            min = history.stream().mapToInt(i -> i).min().orElse(0);
            max = history.stream().mapToInt(i -> i).max().orElse(0);
        }

        if (max - min > 50) return new int[]{min, max};
        return null;
    }

    public void removePlayer(UUID uuid) {
        sentTime.remove(uuid);
        answeredTime.remove(uuid);
        ping.remove(uuid);
        pingHistory.remove(uuid);
    }

    public int getPing(UUID uuid) {
        return ping.getOrDefault(uuid, -1);
    }
}