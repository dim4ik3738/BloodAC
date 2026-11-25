package me.dim4ik.utils;

import me.dim4ik.BloodAC;
import me.dim4ik.utils.buffer.BufferActions;
import me.dim4ik.utils.math.MathUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProbActionBar {
    private static final Set<UUID> aimEnabled = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> attackEnabled = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, State> states = new ConcurrentHashMap<>();

    public static void enableAll(UUID uuid) {
        aimEnabled.add(uuid);
        attackEnabled.add(uuid);
    }

    public static void disableAll(UUID uuid) {
        aimEnabled.remove(uuid);
        attackEnabled.remove(uuid);
    }

    public static void enableAim(UUID uuid) {
        attackEnabled.remove(uuid);
        aimEnabled.add(uuid);
    }

    public static void disableAim(UUID uuid) {
        aimEnabled.remove(uuid);
    }

    public static void enableAttack(UUID uuid) {
        aimEnabled.remove(uuid);
        attackEnabled.add(uuid);
    }

    public static void disableAttack(UUID uuid) {
        attackEnabled.remove(uuid);
    }

    public static void updateAim(Player player, float prediction, float bufferValue, float delta) {
        UUID uuid = player.getUniqueId();
        if (!aimEnabled.contains(uuid)) {
            return;
        }
        states.computeIfAbsent(uuid, id -> new State()).aim.set(prediction, bufferValue, delta);
        render(player);
    }

    public static void updateAttack(Player player, float prediction, float bufferValue, float delta) {
        UUID uuid = player.getUniqueId();
        if (!attackEnabled.contains(uuid)) {
            return;
        }
        states.computeIfAbsent(uuid, id -> new State()).attack.set(prediction, bufferValue, delta);
        render(player);
    }

    private static void render(Player player) {
        UUID uuid = player.getUniqueId();
        State state = states.computeIfAbsent(uuid, id -> new State());

        boolean showAim = aimEnabled.contains(uuid);
        boolean showAttack = attackEnabled.contains(uuid);

        StringBuilder text = new StringBuilder();
        if (showAim) {
            float aimMax = BufferActions.max("Checks.Aim");
            text.append("§7").append("aim").append(" ").append(format(state.aim, aimMax));
        }
        if (showAim && showAttack) {
            text.append("   ");
        }
        if (showAttack) {
            float attackMax = BufferActions.max("Checks.Attack");
            text.append("§7").append("attack").append(" ").append(format(state.attack, attackMax));
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text.toString()));
    }

    private static String format(Channel c, float maxBuffer) {
        String bufferColor = ColorUtil.colorForBuffer(c.buffer, maxBuffer);
        String deltaColor = c.delta > 0 ? "§c" : c.delta < 0 ? "§a" : "§7";

        return ColorUtil.colorForPrediction(c.prediction) + MathUtil.num(MathUtil.globalRound(c.prediction)) + "%"
                + " §7| " + bufferColor + MathUtil.num(MathUtil.globalRound(c.buffer))
                + " §7| " + deltaColor + (c.delta > 0 ? "+" : "") + MathUtil.num(MathUtil.globalRound(c.delta));
    }

    private static final class State {
        final Channel aim = new Channel();
        final Channel attack = new Channel();
    }

    private static final class Channel {
        volatile float prediction;
        volatile float buffer;
        volatile float delta;

        void set(float prediction, float buffer, float delta) {
            this.prediction = prediction;
            this.buffer = buffer;
            this.delta = delta;
        }
    }
}