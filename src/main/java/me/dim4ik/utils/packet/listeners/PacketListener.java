package me.dim4ik.utils.packet.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import java.util.ArrayList;
import java.util.List;

public class PacketListener {
    private static final List<PacketListenerAbstract> packetListeners = new ArrayList<>();

    public static void addListener(PacketListenerAbstract listenerAbstract) {
        PacketEvents.getAPI().getEventManager().registerListener(listenerAbstract);
        packetListeners.add(listenerAbstract);
    }

    public static void unregisterAll() {
        for (PacketListenerAbstract listener : packetListeners) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        }
        packetListeners.clear();
    }
}
