package me.dim4ik.utils;

import me.dim4ik.BloodAC;
import me.dim4ik.utils.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PunishAnimation {
    private void finish(Player player, boolean withEndSound) {
        if (withEndSound) playSound(player, "punishSounds.end");
        if (player.isOnline() && BloodAC.getInstance().configManager.getBoolean("killPlayer")) {
            player.setHealth(0);
        }
        String punishCommand = BloodAC.getInstance().configManager.getString("punishCommand");
        String finalCommand = punishCommand.replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
    }

    private void playSound(Player player, String path) {
        ConfigManager config = BloodAC.getInstance().configManager;
        if (!config.getBoolean(path + ".enabled")) return;
        String name = SoundUtil.normalize(config.getString(path + ".name"));
        if (name == null || name.isEmpty()) return;
        player.getWorld().playSound(player.getLocation(), name, (float) config.getDouble(path + ".volume"), (float) config.getDouble(path + ".pitch"));
    }

    public void punish(Player player) {
        ConfigManager config = BloodAC.getInstance().configManager;
        playSound(player, "punishSounds.start");
        if (!config.getBoolean("punishAnimation")) {
            finish(player, false);
            return;
        }
        int duration = Math.max(1, config.getInt("punishLevitationTicks"));
        Color color = parseColor(config.getString("punishParticleColor"));

        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, 2, false, false));

        new BukkitRunnable() {
            double angle = 0;
            int ticks = 0;

            @Override
            public void run() {
                Location loc = player.getLocation().add(0, 1, 0);

                for (int i = 0; i < 3; i++) {
                    double a = angle + (2 * Math.PI / 3) * i;
                    double x = Math.cos(a) * 1.2;
                    double z = Math.sin(a) * 1.2;
                    double y = (ticks % 20) * 0.1;

                    Location particleLoc = loc.clone().add(x, y, z);

                    player.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(color, 1.5f));
                }

                angle += Math.PI / 10;
                ticks++;

                if (ticks >= duration) {
                    this.cancel();
                    finish(player, true);
                }
            }
        }.runTaskTimer(BloodAC.getInstance(), 0L, 1L);
    }

    private Color parseColor(String value) {
        try {
            String[] parts = value.split(",");
            return Color.fromRGB(clamp(Integer.parseInt(parts[0].trim())), clamp(Integer.parseInt(parts[1].trim())), clamp(Integer.parseInt(parts[2].trim())));
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}