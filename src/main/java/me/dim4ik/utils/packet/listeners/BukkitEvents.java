package me.dim4ik.utils.packet.listeners;

import me.dim4ik.BloodAC;
import me.dim4ik.commands.BloodACCommand;
import me.dim4ik.suspectMenu.SuspectMenu;
import me.dim4ik.utils.PlayerData;
import me.dim4ik.utils.PredictHologram;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;

public class BukkitEvents implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        BloodAC.getInstance().playersData.put(uuid, new PlayerData(player));

        PredictHologram.showExistingTo(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PredictHologram.remove(uuid);
        BloodAC.getInstance().fastPing.removePlayer(uuid);
        BloodAC.getInstance().playersData.remove(uuid);

        SuspectMenu.openMainViewers.remove(uuid);
        SuspectMenu.openDetailViewers.remove(uuid);
        SuspectMenu.slotUuidMap.remove(uuid);

        Bukkit.getScheduler().runTaskLater(BloodAC.getInstance(), () -> SuspectMenu.refresh(uuid), 1L);

        HitMissTracker.clearFlags(uuid);
        AttackListener.remove(uuid);
        SprintListener.remove(uuid);
        ItemNameChangeListener.remove(uuid);
        HealthListener.remove(uuid);
    }
}
