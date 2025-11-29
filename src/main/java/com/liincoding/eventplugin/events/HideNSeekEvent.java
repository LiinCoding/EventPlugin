package com.liincoding.eventplugin.events;

import com.liincoding.eventplugin.EventPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Bukkit;

public class HideNSeekEvent implements EventManager.EventType {

    private final EventPlugin plugin;

    public HideNSeekEvent(EventPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onStart(EventManager manager) {
        // Example: assign hiders and seekers here
        // This will run AFTER the 30-second countdown
    }

    @Override
    public void onJoin(EventManager manager, Player player) {
        // Optional: logic for when a player joins after event started
    }

    @Override
    public void onEnd(EventManager manager) {
        // Optional: cleanup
    }
}
