package com.liincoding.eventplugin.events;

import com.liincoding.eventplugin.EventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventManager {

    private final EventPlugin plugin;

    private boolean eventRunning = false;
    private String currentEventName;
    private BossBar bossBar;

    // Stores original player locations and inventories
    private final Map<UUID, PlayerData> eventPlayers = new HashMap<>();

    public EventManager(EventPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEventRunning() {
        return eventRunning;
    }

    public String getCurrentEventName() {
        return currentEventName;
    }

    // Called when event starts
    public void startEvent(String eventName) {
        if (eventRunning) return;

        eventRunning = true;
        currentEventName = eventName;

        // Example boss bar
        bossBar = Bukkit.createBossBar("Event " + eventName + " starting!", BarColor.GREEN, BarStyle.SOLID);
        // TODO: Add players to boss bar and schedule countdown
    }

    // Called when event ends (or is cancelled)
    public void endEvent() {
        if (!eventRunning) return;

        // Restore all players
        for (UUID uuid : eventPlayers.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                PlayerData data = eventPlayers.get(uuid);
                player.teleport(data.getLocation());
                player.getInventory().setContents(data.getInventory());
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        // Clear boss bar
        if (bossBar != null) {
            bossBar.removeAll();
        }

        // Reset state
        eventPlayers.clear();
        eventRunning = false;
        currentEventName = null;

        plugin.getLogger().info("Event has ended.");
    }

    // Store player data when they join
    public void addPlayer(Player player) {
        eventPlayers.put(player.getUniqueId(), new PlayerData(player));
        player.setGameMode(GameMode.ADVENTURE);
        // TODO: teleport to event location
    }

    public void joinEvent(Player player) {
        addPlayer(player); // already stores inventory/location and sets gamemode
    }

    public void leaveEvent(Player player) {
        PlayerData data = eventPlayers.remove(player.getUniqueId());
        if (data != null) {
            player.teleport(data.getLocation());
            player.getInventory().setContents(data.getInventory());
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    // Simple container for inventory and location
    private static class PlayerData {
        private final org.bukkit.Location location;
        private final org.bukkit.inventory.ItemStack[] inventory;

        public PlayerData(Player player) {
            this.location = player.getLocation();
            this.inventory = player.getInventory().getContents();
        }

        public org.bukkit.Location getLocation() {
            return location;
        }

        public org.bukkit.inventory.ItemStack[] getInventory() {
            return inventory;
        }
    }
}
