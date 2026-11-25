package me.dim4ik.utils;

import org.bukkit.entity.Player;
import java.util.UUID;

public class PlayerData {
    public Player player;
    public UUID uuid;

    public PlayerData(Player player) {
        this.player = player;
        uuid = player.getUniqueId();
    }

    public boolean isSprinting;
    public boolean isOnGround;
    public int groundTicks;

    public volatile int tick;
}
