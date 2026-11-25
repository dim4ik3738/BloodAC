package me.dim4ik;

import lombok.Getter;
import me.dim4ik.checks.AttackNeuro;
import me.dim4ik.checks.check.ChecksManager;
import me.dim4ik.checks.AimNeuro;
import me.dim4ik.commands.BloodACCommand;
import me.dim4ik.commands.BloodACCommandTabCompleter;
import me.dim4ik.suspectMenu.SuspectMenuListener;
import me.dim4ik.utils.*;
import me.dim4ik.utils.buffer.BufferActions;
import me.dim4ik.utils.config.ConfigManager;
import me.dim4ik.utils.lang.Lang;
import me.dim4ik.utils.packet.FastPing;
import me.dim4ik.utils.packet.listeners.*;
import me.dim4ik.utils.api.PredictClient;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public final class BloodAC extends JavaPlugin implements Listener {
    @Getter
    private static BloodAC instance;

    public Map<UUID, PlayerData> playersData = new ConcurrentHashMap<>();

    static String bloodac = ColorUtil.translateHexColors("&#FF0000ʙ&#EF0000ʟ&#DF0000ᴏ&#D00000ᴏ&#C00000ᴅ&#B00000ᴀ&#A00000ᴄ");
    public static String acPrefix = bloodac + ChatColor.GRAY + " » ";

    public PunishAnimation punishAnimation;
    public ConfigManager configManager;
    public FastPing fastPing;
    public ChecksManager checksManager;
    public static String licenseKey;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        configManager = new ConfigManager();
        Lang.load(configManager.getString("language"));
        BufferActions.validate();
        licenseKey = configManager.getString("Key");

        if (licenseKey == null || licenseKey.isEmpty()) {
            BloodAC.getInstance().getLogger().severe(Lang.get("log.no_key"));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        String currentVersion = getDescription().getVersion();
        String latestVersion = PredictClient.getLatestVersion();

        if (latestVersion.isEmpty()) {
            BloodAC.getInstance().getLogger().warning(Lang.get("log.version_check_failed"));
        } else if (!latestVersion.equals(currentVersion)) {
            BloodAC.getInstance().getLogger().warning(Lang.get("log.outdated", "current", currentVersion, "latest", latestVersion));
        }

        fastPing = new FastPing();
        checksManager = new ChecksManager();
        punishAnimation = new PunishAnimation();

        getCommand("bloodac").setExecutor(new BloodACCommand());
        getCommand("bloodac").setTabCompleter(new BloodACCommandTabCompleter());

        new AttackListener();
        new ItemCountChange();
        new HealthListener();
        new EffectListener();
        new MovePacketsListener();
        new SprintListener();
        new GameModeSpoofListener();
        new MovePacketsForHologramListener();
        new ItemNameChangeListener();

        for (Player player : Bukkit.getOnlinePlayers()) {
            playersData.put(player.getUniqueId(), new PlayerData(player));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    if (AttackListener.lastAttackTime.get(uuid) == null || !AttackListener.hasAttackedRecently(uuid)) {
                        AimNeuro.clear(player);
                        AttackNeuro.clear(player);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 5L);

        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new SuspectMenuListener(), this);
        getServer().getPluginManager().registerEvents(new BukkitEvents(), this);
        getServer().getPluginManager().registerEvents(new HitMissTracker(), this);
    }

    @Override
    public void onDisable() {
        PredictHologram.removeAll();
        PacketListener.unregisterAll();
    }
}