package me.dim4ik.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BloodACCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        Function<List<String>, List<String>> filter = list -> {
            String prefix = args[args.length - 1].toLowerCase();

            return list.stream()
                    .filter(s -> s.toLowerCase().startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        };

        if (args.length == 1) {
            List<String> cmds = new ArrayList<>();

            if (sender.hasPermission("BloodAC.canPunish")) cmds.add("canPunish");
            if (sender.hasPermission("BloodAC.predictHologram")) cmds.add("predictHologram");
            if (sender.hasPermission("BloodAC.acPunish")) cmds.add("acPunish");
            if (sender.hasPermission("BloodAC.prob")) cmds.add("prob");
            if (sender.hasPermission("BloodAC.monitor")) cmds.add("monitor");
            if (sender.hasPermission("BloodAC.suspectsMenu")) cmds.add("suspectsMenu");
            if (sender.hasPermission("BloodAC.reload")) cmds.add("reload");

            return filter.apply(cmds);
        }

        if (args.length == 2) {
            if ((args[0].equalsIgnoreCase("predictHologram") && sender.hasPermission("BloodAC.predictHologram")) ||
                    (args[0].equalsIgnoreCase("prob") && sender.hasPermission("BloodAC.prob")) ||
                    (args[0].equalsIgnoreCase("canPunish") && sender.hasPermission("BloodAC.canPunish"))) {
                return filter.apply(Arrays.asList("on", "off"));
            }
        }

        return null;
    }
}
