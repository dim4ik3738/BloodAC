package me.dim4ik.commands;

import me.dim4ik.BloodAC;
import me.dim4ik.suspectMenu.SuspectMenu;
import me.dim4ik.utils.PredictHologram;
import me.dim4ik.utils.ProbActionBar;
import me.dim4ik.utils.lang.Lang;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BloodACCommand implements CommandExecutor, Listener {
    public static List<UUID> hologramViewers = new ArrayList<>();
    public static Map<UUID, String> monitor = new ConcurrentHashMap<>();

    private static final Set<String> playerOnlySubCommands = Set.of(
            "prob", "monitor", "suspectsMenu", "predictHologram"
    );

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 0) {
            commandSender.sendMessage(Lang.prefixed("command.usage_main"));
            return true;
        }

        String sub = strings[0];

        if (playerOnlySubCommands.contains(sub.toLowerCase()) && !(commandSender instanceof Player)) {
            commandSender.sendMessage(Lang.prefixed("command.player_only"));
            return true;
        }

        UUID senderUuid = (commandSender instanceof Player) ? ((Player) commandSender).getUniqueId() : null;
        Player senderPlayer = (commandSender instanceof Player) ? (Player) commandSender : null;

        if (sub.equalsIgnoreCase("prob")) {
            if (!commandSender.hasPermission("BloodAC.prob")) {
                commandSender.sendMessage(Lang.prefixed("command.no_permission"));
                return true;
            }

            String usage = Lang.prefixed("command.prob_usage");

            if (strings.length < 2 || strings.length > 3) {
                commandSender.sendMessage(usage);
                return true;
            }

            String target = strings.length == 3 ? strings[1].toLowerCase() : "all";
            String state = strings[strings.length - 1];

            boolean on = state.equalsIgnoreCase("on");
            if (!on && !state.equalsIgnoreCase("off")) {
                commandSender.sendMessage(usage);
                return true;
            }

            if (target.equals("all")) {
                if (on) ProbActionBar.enableAll(senderUuid);
                else ProbActionBar.disableAll(senderUuid);
            } else if (target.equals("aim")) {
                if (on) ProbActionBar.enableAim(senderUuid);
                else ProbActionBar.disableAim(senderUuid);
            } else if (target.equals("attack")) {
                if (on) ProbActionBar.enableAttack(senderUuid);
                else ProbActionBar.disableAttack(senderUuid);
            } else {
                commandSender.sendMessage(usage);
                return true;
            }

            String name = target.equals("all") ? "prob" : "prob " + target;
            commandSender.sendMessage(Lang.prefixed("command.toggle", "name", name, "state", Lang.get(on ? "state.enabled" : "state.disabled")));
            return true;
        }

        if (sub.equalsIgnoreCase("canPunish")) {
            if (commandSender.hasPermission("BloodAC.canPunish")) {
                if (strings.length < 2 || !(strings[1].equalsIgnoreCase("on") || strings[1].equalsIgnoreCase("off"))) {
                    commandSender.sendMessage(Lang.prefixed("command.canpunish_usage"));
                    return true;
                }
                if (strings[1].equalsIgnoreCase("on")) {
                    commandSender.sendMessage(Lang.prefixed("command.toggle", "name", "canPunish", "state", Lang.get("state.enabled")));
                    BloodAC.getInstance().configManager.set("canPunish", true);
                } else {
                    commandSender.sendMessage(Lang.prefixed("command.toggle", "name", "canPunish", "state", Lang.get("state.disabled")));
                    BloodAC.getInstance().configManager.set("canPunish", false);
                }
            } else {
                commandSender.sendMessage(Lang.prefixed("command.no_permission"));
            }
            return true;
        }

        if (sub.equalsIgnoreCase("reload")) {
            if (commandSender.hasPermission("BloodAC.reload")) {
                BloodAC.getInstance().configManager.reload();
                commandSender.sendMessage(Lang.prefixed("command.reloaded"));
            } else {
                commandSender.sendMessage(Lang.prefixed("command.no_permission"));
            }
            return true;
        }

        if (sub.equalsIgnoreCase("monitor")) {
            if (commandSender.hasPermission("BloodAC.monitor")) {
                if (strings.length == 2 && strings[1].equalsIgnoreCase("on")) {
                    monitor.put(senderUuid, "");
                    commandSender.sendMessage(Lang.prefixed("command.monitor_on_all"));
                } else if (strings.length == 2 && strings[1].equalsIgnoreCase("off")) {
                    monitor.remove(senderUuid);
                    commandSender.sendMessage(Lang.prefixed("command.monitor_off"));
                } else if (strings.length >= 3 && strings[2].equalsIgnoreCase("off")) {
                    monitor.remove(senderUuid);
                    commandSender.sendMessage(Lang.prefixed("command.monitor_off"));
                } else if (strings.length >= 3 && strings[2].equalsIgnoreCase("on")) {
                    String targetNick = strings[1];
                    Player targetPlayer = Bukkit.getPlayer(targetNick);
                    if (targetPlayer != null) {
                        monitor.put(senderUuid, targetNick);
                        commandSender.sendMessage(Lang.prefixed("command.monitor_on_player", "player", targetNick));
                    } else {
                        commandSender.sendMessage(Lang.prefixed("command.player_not_found"));
                    }
                } else {
                    commandSender.sendMessage(Lang.prefixed("command.monitor_usage"));
                }
            } else {
                commandSender.sendMessage(Lang.prefixed("command.no_permission"));
                return true;
            }
            return true;
        }

        if (sub.equalsIgnoreCase("acPunish")) {
            if (commandSender.hasPermission("BloodAC.acPunish")) {
                if (strings.length < 2) {
                    commandSender.sendMessage(Lang.prefixed("command.acpunish_usage"));
                    return true;
                }
                String playerName = strings[1];
                Player player = Bukkit.getPlayer(playerName);
                if (player != null) {
                    BloodAC.getInstance().punishAnimation.punish(player);
                } else {
                    commandSender.sendMessage(Lang.prefixed("command.player_not_found"));
                }
            } else {
                commandSender.sendMessage(Lang.prefixed("command.no_permission"));
                return true;
            }
            return true;
        }

        if (sub.equalsIgnoreCase("suspectsMenu")) {
            if (commandSender.hasPermission("BloodAC.suspectsMenu")) {
                if (strings.length >= 2) {
                    Player target = Bukkit.getPlayer(strings[1]);
                    if (target != null) {
                        SuspectMenu.openDetail(senderPlayer, target.getUniqueId());
                    } else {
                        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(strings[1]);
                        if (offlineTarget.hasPlayedBefore()) {
                            SuspectMenu.openDetail(senderPlayer, offlineTarget.getUniqueId());
                        } else {
                            commandSender.sendMessage(Lang.prefixed("command.player_not_found"));
                        }
                    }
                } else {
                    SuspectMenu.openMain(senderPlayer);
                }
            } else {
                commandSender.sendMessage(Lang.prefixed("command.no_permission"));
            }
            return true;
        }

        if (sub.equalsIgnoreCase("predictHologram")) {
            if (commandSender.hasPermission("BloodAC.predictHologram")) {
                if (strings.length < 2 || !(strings[1].equalsIgnoreCase("on") || strings[1].equalsIgnoreCase("off"))) {
                    commandSender.sendMessage(Lang.prefixed("command.hologram_usage"));
                    return true;
                }
                if (strings[1].equalsIgnoreCase("on")) {
                    hologramViewers.add(senderUuid);
                    PredictHologram.onViewerEnabled(senderPlayer);
                    commandSender.sendMessage(Lang.prefixed("command.hologram_toggle", "state", Lang.get("state.enabled")));
                } else {
                    hologramViewers.remove(senderUuid);
                    PredictHologram.onViewerDisabled(senderPlayer);
                    commandSender.sendMessage(Lang.prefixed("command.hologram_toggle", "state", Lang.get("state.disabled")));
                }
            } else {
                commandSender.sendMessage(Lang.prefixed("command.no_permission"));
            }
            return true;
        }

        commandSender.sendMessage(Lang.prefixed("command.unknown", "sub", sub));
        return true;
    }
}