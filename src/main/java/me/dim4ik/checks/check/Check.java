package me.dim4ik.checks.check;

import lombok.Getter;
import me.dim4ik.BloodAC;
import me.dim4ik.utils.ColorUtil;
import me.dim4ik.utils.SoundUtil;
import me.dim4ik.utils.buffer.Buffer;
import me.dim4ik.utils.buffer.BufferActions;
import me.dim4ik.utils.lang.Lang;
import me.dim4ik.utils.math.MathUtil;
import me.dim4ik.utils.packet.FastPing;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.UUID;

@CheckHandler(name = "", type = "")
public class Check implements Listener {
    @Getter
    private final String name;
    @Getter
    private final String type;

    public Check() {
        var annotation = getClass().getAnnotation(CheckHandler.class);
        this.name = annotation.name();
        this.type = annotation.type();
    }

    public void flag(Player player) {
        BaseComponent[] message = TextComponent.fromLegacyText(
                BloodAC.acPrefix + Lang.get("alert.flag",
                        "player", player.getName(),
                        "check", name,
                        "type", type,
                        "ping", getPlayerPing(player))
        );

        BaseComponent[] hover = TextComponent.fromLegacyText(Lang.get("alert.hover"));

        for (BaseComponent component : message) {
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + player.getName()));
        }

        for (Player player2 : Bukkit.getOnlinePlayers()) {
            if (player2.hasPermission("BloodAC.alerts")) {
                player2.spigot().sendMessage(message);
            }
        }

    }

    public void punish(Player player) {
        if (BloodAC.getInstance().configManager.getBoolean("canPunish") && isCheckPunishEnabled()) {
            BloodAC.getInstance().punishAnimation.punish(player);
        }
    }

    public void runBufferActions(Player player, Buffer.Result result) {
        if (result.actions.isEmpty()) return;
        Bukkit.getScheduler().runTask(BloodAC.getInstance(), () -> {
            if (!player.isOnline()) return;
            for (BufferActions.Action action : result.actions) {
                if (action.type == BufferActions.Type.FLAG) {
                    flag(player);
                } else if (action.type == BufferActions.Type.PUNISH) {
                    punish(player);
                } else if (action.type == BufferActions.Type.ALERT) {
                    String text = Lang.get(action.arg,
                            "player", player.getName(),
                            "check", name,
                        "type", type,
                            "buffer", MathUtil.num(MathUtil.globalRound(result.reached)),
                            "buffer_warning", getBufferWarning(result.reached),
                            "ping", getPlayerPing(player));
                    for (Player staff : Bukkit.getOnlinePlayers()) {
                        if (staff.hasPermission("BloodAC.alerts")) staff.sendMessage(BloodAC.acPrefix + text);
                    }
                } else if (action.type == BufferActions.Type.SOUND) {
                    for (Player staff : Bukkit.getOnlinePlayers()) {
                        if (staff.hasPermission("BloodAC.alerts")) SoundUtil.play(staff, action.arg);
                    }
                } else if (action.type == BufferActions.Type.COMMAND) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fill(action.arg, player, result.reached));
                }
            }
        });
    }

    private String fill(String text, Player player, float buffer) {
        return text.replace("%player%", player.getName())
                .replace("{player}", player.getName())
                .replace("{check}", name)
                .replace("{type}", type)
                .replace("{buffer}", MathUtil.num(MathUtil.globalRound(buffer)));
    }

    public String getBufferWarning(float buffer) {
        float max = BufferActions.max("Checks." + name);
        if (max <= 0f || buffer <= max / 3f) return "";
        String color = ColorUtil.colorForBuffer(buffer, max);
        return Lang.get("alert.buffer_warning",
                "color", color,
                "buffer", me.dim4ik.utils.math.MathUtil.num(me.dim4ik.utils.math.MathUtil.globalRound(buffer)),
                "max", me.dim4ik.utils.math.MathUtil.num(me.dim4ik.utils.math.MathUtil.globalRound(max)));
    }

    public String getPlayerPing(Player player) {
        UUID uuid = player.getUniqueId();
        FastPing fastPing = BloodAC.getInstance().fastPing;
        int ping = fastPing.getPing(uuid);
        int[] throttle = fastPing.isThrottled(uuid);
        String pingColor = throttle != null ? "§c" : ColorUtil.colorForPing(ping);
        return Lang.get("alert.ping", "color", pingColor, "ping", fastPing.formatedPing(uuid));
    }

    public void onRotate(Player player, float yawSpeed, float pitchSpeed, float yaw, float pitch) {

    }

    public void onMoveRotate(Player player, Vector velocity, float yaw, float pitch) {

    }

    public void onSwing(Player player) {

    }

    public boolean isCheckEnabled() {
        return BloodAC.getInstance().configManager.getBoolean("Checks." + name + ".enabled");
    }

    public boolean isCheckPunishEnabled() {
        return BloodAC.getInstance().configManager.getBoolean("Checks." + name + ".canPunish");
    }
}