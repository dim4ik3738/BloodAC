package me.dim4ik.utils;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class AttackUtil {
    // EntityLiving.isClimbing()
    static boolean isClimbing(Player p) {
        if (p.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }else {

            Block block = p.getLocation().getBlock();
            BlockData data = block.getBlockData();

            if (Tag.CLIMBABLE.isTagged(block.getType())) {
                return true;
            } else if (data instanceof TrapDoor && isClimbableTrapdoor(block, (TrapDoor) data)) {
                return true;
            } else {
                return false;
            }
        }
    }

    // EntityLiving.c(BlockPosition, IBlockData)
    static boolean isClimbableTrapdoor(Block block, TrapDoor trapdoor) {
        if (trapdoor.isOpen()) {
            Block below = block.getRelative(0, -1, 0);
            if (below.getType() == Material.LADDER && ((Ladder) below.getBlockData()).getFacing() == trapdoor.getFacing()) {
                return true;
            }
        }
        return false;
    }

    public static boolean canCrit(Player p) {
        boolean flag = p.getCooledAttackStrength(0.5F) > 0.9F;

        boolean flag2 = flag && p.getFallDistance() > 0.0F && !p.isOnGround() && !isClimbing(p)
                && !p.isInWater() && !p.hasPotionEffect(PotionEffectType.BLINDNESS)
                && !p.isInsideVehicle();

        flag2 = flag2 && !p.isSprinting();

        return flag2;
    }
}
