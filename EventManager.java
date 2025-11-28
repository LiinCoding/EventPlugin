package com.yourname.eventplugin.events;

import com.yourname.eventplugin.EventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EventManager {

    private final EventPlugin plugin;

    private Event currentEvent;
    private Map<UUID, PlayerData> participants = new HashMap<>();

    public EventManager(EventPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEventRunning() {
        return currentEvent != null;
    }

    public boolean startEvent(String eventName) {
        if (isEventRunning()) return false;

        // Load event metadata from config
        if (!plugin.getConfig().contains("events." + eventName)) return false;

        currentEvent = new Event(eventName, plugin.getConfig().getConfigurationSection("events." + eventName));

        startCountdown(30); // 30 seconds countdown
        return true;
    }

    private void startCountdown(int seconds) {
        BossBar bossBar = Bukkit.createBossBar("Event " + currentEvent.getName() + " starting", BarColor.GREEN, BarStyle.SOLID);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        new BukkitRunnable() {
            int timer = seconds;

            @Override
            public void run() {
                if (timer <= 0) {
                    bossBar.removeAll();
                    this.cancel();
                    // Event officially starts
                    currentEvent.setRunning(true);
                    Bukkit.broadcastMessage("Event " + currentEvent.getName() + " has started!");
                    return;
                }
                bossBar.setProgress(timer / (double) seconds);
                bossBar.setTitle("Event " + currentEvent.getName() + " starting in " + timer + "s");
                timer--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // 20L = 1 second
    }

    public boolean joinEvent(Player player) {
        if (!isEventRunning()) return false;
        if (participants.containsKey(player.getUniqueId())) return false;

        PlayerData data = new PlayerData(player);
        participants.put(player.getUniqueId(), data);

        // Teleport and set adventure mode
        currentEvent.teleportToEvent(player);
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);

        player.sendMessage("You joined the event!");
        return true;
    }

    public boolean leaveEvent(Player player) {
        if (!participants.containsKey(player.getUniqueId())) return false;

        PlayerData data = participants.remove(player.getUniqueId());
        data.restore(player);

        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.sendMessage("You left the event!");

        return true;
    }

    public Map<UUID, PlayerData> getParticipants() {
        return participants;
    }

    public Event getCurrentEvent() {
        return currentEvent;
    }
}
