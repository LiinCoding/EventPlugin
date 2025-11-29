package com.liincoding.eventplugin.events;

import com.liincoding.eventplugin.EventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class HideNSeekEvent implements EventManager.EventType {

    private final EventPlugin plugin;

    public HideNSeekEvent(EventPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onStart(EventManager manager) {
        // Get all players who joined
        List<UUID> playerUUIDs = new ArrayList<>(manager.getEventPlayers().keySet());
        if (playerUUIDs.isEmpty()) return;

        // Shuffle for randomness
        Collections.shuffle(playerUUIDs);

        // Select 1 seeker
        UUID seekerUUID = playerUUIDs.get(0);
        Player seeker = Bukkit.getPlayer(seekerUUID);
        if (seeker != null) {
            seeker.sendMessage(ChatColor.RED + "You are the Seeker!");
            // Optionally: give seeker special items/effects
        }

        // Assign remaining as hiders
        for (int i = 1; i < playerUUIDs.size(); i++) {
            UUID hiderUUID = playerUUIDs.get(i);
            Player hider = Bukkit.getPlayer(hiderUUID);
            if (hider != null) {
                hider.sendMessage(ChatColor.GREEN + "You are a Hider!");
                // Optionally: hide hiders from seeker initially or give special effects
            }
        }
    }

    @Override
    public void onJoin(EventManager manager, Player player) {
        // Optional: handle player-specific logic when they join mid-countdown
    }

    @Override
    public void onEnd(EventManager manager) {
        // Optional: cleanup roles/effects when event ends
    }
}
