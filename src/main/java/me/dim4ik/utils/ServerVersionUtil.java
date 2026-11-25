package me.dim4ik.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class ServerVersionUtil {
    private static final ServerVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion();

    public static boolean isNewerThanOrEquals(ServerVersion version) {
        return serverVersion.isNewerThanOrEquals(version);
    }

    public static boolean isOlderThan(ServerVersion version) {
        return serverVersion.isOlderThan(version);
    }

    public static int getHealthMetaIndex() {
        if (isNewerThanOrEquals(ServerVersion.V_1_17)) return 9;
        return 8;
    }
}
