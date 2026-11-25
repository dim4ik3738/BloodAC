package me.dim4ik.suspectMenu;

import me.dim4ik.utils.lang.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SuspectMenuListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.equals(SuspectMenu.titleMain())) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            int slot = event.getRawSlot();

            if (slot == SuspectMenu.SLOT_PREV && clicked.getType() == Material.ARROW) {
                int page = SuspectMenu.openMainViewers.getOrDefault(player.getUniqueId(), 0);
                if (page > 0) SuspectMenu.openMain(player, page - 1);
                return;
            }

            if (slot == SuspectMenu.SLOT_NEXT && clicked.getType() == Material.ARROW) {
                int page = SuspectMenu.openMainViewers.getOrDefault(player.getUniqueId(), 0);
                SuspectMenu.openMain(player, page + 1);
                return;
            }

            if (slot < 45 && clicked.getType() == Material.PLAYER_HEAD) {
                java.util.Map<Integer, UUID> slotMap = SuspectMenu.slotUuidMap.get(player.getUniqueId());
                if (slotMap == null) return;
                UUID uuid = slotMap.get(slot);
                if (uuid == null) return;

                if (event.getClick().isRightClick()) {
                    SuspectMenu.openDetail(player, uuid);
                } else if (event.getClick().isLeftClick()) {
                    org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(uuid);
                    if (target == null) {
                        player.sendMessage(Lang.get("menu.offline"));
                        return;
                    }
                    player.closeInventory();
                    player.teleport(target.getLocation());
                    player.sendMessage(Lang.get("menu.teleporting", "player", target.getName()));
                }
            }
            return;
        }

        if (title.startsWith(SuspectMenu.titleDetail())) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            int slot = event.getRawSlot();

            if (slot == 17) {
                int page = SuspectMenu.openMainViewers.getOrDefault(player.getUniqueId(), 0);
                SuspectMenu.openMain(player, page);
                return;
            }

            if (slot == 11) {
                UUID target = SuspectMenu.openDetailViewers.get(player.getUniqueId());
                if (target == null) return;
                SuspectMenu.openPredicts(player, target, "aim", 0);
                return;
            }

            if (slot == 15) {
                UUID target = SuspectMenu.openDetailViewers.get(player.getUniqueId());
                if (target == null) return;
                SuspectMenu.openPredicts(player, target, "attack", 0);
                return;
            }

            return;
        }

        if (title.startsWith(SuspectMenu.titlePredicts())) {
            event.setCancelled(true);

            int slot = event.getRawSlot();

            SuspectMenu.PredictsViewState state = SuspectMenu.openPredictsViewers.get(player.getUniqueId());
            if (state == null) return;

            if (slot == SuspectMenu.PREDICTS_SLOT_PREV) {
                SuspectMenu.openPredicts(player, state.target, state.type, state.page - 1);
                return;
            }

            if (slot == SuspectMenu.PREDICTS_SLOT_NEXT) {
                SuspectMenu.openPredicts(player, state.target, state.type, state.page + 1);
                return;
            }

            if (slot == SuspectMenu.PREDICTS_SLOT_BACK) {
                SuspectMenu.openDetail(player, state.target);
            }
        }
    }
}