package me.dim4ik.checks.check;

import me.dim4ik.BloodAC;
import me.dim4ik.checks.AimNeuro;
import me.dim4ik.checks.AttackNeuro;

import java.util.*;

public class ChecksManager {
    public List<Check> checks;
    public List<Check> enabledChecks;

    public ChecksManager() {
        checks = Arrays.asList(
                new AimNeuro(),
                new AttackNeuro()
        );

        reload();
    }

    public void reload() {
        enabledChecks = new ArrayList<>();

        for (Check check : checks) {
            if (check.isCheckEnabled()) {
                enabledChecks.add(check);
                BloodAC.getInstance().getServer().getPluginManager().registerEvents(check, BloodAC.getInstance());
            }
        }
    }
}
