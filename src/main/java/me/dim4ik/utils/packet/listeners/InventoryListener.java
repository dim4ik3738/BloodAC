package me.dim4ik.utils.packet.listeners;

import me.dim4ik.suspectMenu.SuspectMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.UUID;

public class InventoryListener implements Listener {
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        String title = event.getView().getTitle();

        if (title.equals(SuspectMenu.titleMain())) {
            if (!SuspectMenu.openMainViewers.containsKey(uuid)) {
                SuspectMenu.openMainViewers.remove(uuid);
            }
        } else if (title.startsWith(SuspectMenu.titleDetail())) {
            SuspectMenu.openDetailViewers.remove(uuid);
        }
    }
}
