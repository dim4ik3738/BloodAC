package me.dim4ik.utils.math;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DeltaAngles {
    public static float[] calculate(Player player, Player target) {
        Location eyeLoc = player.getEyeLocation();
        Location targetLoc = target.getEyeLocation();
        Vector dir = targetLoc.toVector().subtract(eyeLoc.toVector());

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(dir.getY(), Math.sqrt(dir.getX() * dir.getX() + dir.getZ() * dir.getZ())));

        float deltaYaw = MathUtil.wrapDegrees(targetYaw - player.getLocation().getYaw());
        float deltaPitch = targetPitch - player.getLocation().getPitch();

        return new float[]{deltaYaw, deltaPitch};
    }
}
