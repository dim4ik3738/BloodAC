package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import me.dim4ik.BloodAC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.List;

public class ItemCountChange extends PacketListenerAbstract {

    public ItemCountChange() {
        PacketListener.addListener(this);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_EQUIPMENT) return;

        boolean count = BloodAC.getInstance().configManager.getBoolean("Hiders.itemsCount");
        boolean name = BloodAC.getInstance().configManager.getBoolean("Hiders.spoofHeldItemName");
        if (!count && !name) return;

        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
        List<Equipment> equipmentList = packet.getEquipment();

        if (equipmentList.isEmpty()) return;

        String text = BloodAC.getInstance().configManager.getString("Hiders.heldItemName");
        boolean changed = false;

        for (Equipment equipment : equipmentList) {
            ItemStack peItem = equipment.getItem();

            if (peItem.isEmpty()) continue;

            ItemStack modifiedItem = peItem.copy();
            if (count) modifiedItem.setAmount(64);
            if (name && text != null && (equipment.getSlot() == EquipmentSlot.MAIN_HAND || equipment.getSlot() == EquipmentSlot.OFF_HAND)) {
                modifiedItem.setComponent(ComponentTypes.CUSTOM_NAME, Component.text(text, NamedTextColor.RED));
            }
            equipment.setItem(modifiedItem);
            changed = true;
        }

        if (changed) packet.setEquipment(equipmentList);
    }
}