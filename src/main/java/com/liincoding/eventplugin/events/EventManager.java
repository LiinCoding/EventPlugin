package com.liincoding.eventplugin.events;

import com.liincoding.eventplugin.EventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventManager {

  private final EventPlugin plugin;

  private boolean eventRunning = false;
  private String currentEventName;
  private BossBar bossBar;

  // Stores original player locations and inventories
  private final Map < UUID,
  PlayerData > eventPlayers = new HashMap < >();

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

    // Check if the event is configured
    World eventWorld = getEventWorld(eventName);
    if (eventWorld == null) {
        plugin.getLogger().warning("Cannot start event '" + eventName + "': world not found in config or not loaded!");
        Bukkit.broadcastMessage("§cCannot start event '" + eventName + "': world not found!");
        return; // abort event start
    }

    eventRunning = true;
    currentEventName = eventName;

    bossBar = Bukkit.createBossBar("Event " + eventName + " starting in 30s", BarColor.GREEN, BarStyle.SOLID);

    new org.bukkit.scheduler.BukkitRunnable() {
      int seconds = 30;

      @Override
      public void run() {
        if (seconds <= 0) {
          bossBar.removeAll();
          Bukkit.broadcastMessage("§aEvent " + eventName + " has started!");

          // Teleport all players who joined
          for (UUID uuid: eventPlayers.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
              // Example: teleport to event world spawn
              org.bukkit.World world = getEventWorld(eventName);
              if (world != null) {
                org.bukkit.Location spawn = world.getSpawnLocation();
                player.teleport(spawn);
                player.setGameMode(GameMode.ADVENTURE);
              } else {
                player.sendMessage("§cEvent world not found!");
              }
            }
          }

          cancel();
          return;
        }

        bossBar.setTitle("Event " + eventName + " starting in " + seconds + "s");
        bossBar.setProgress(seconds / 30.0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
        seconds--;
      }
    }.runTaskTimer(plugin, 0L, 20L); // 20 ticks = 1 second
  }

  // Called when event ends (or is cancelled)
  public void endEvent() {
    if (!eventRunning) return;

    // Restore all players
    for (UUID uuid: eventPlayers.keySet()) {
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

  public void addPlayer(Player player) {
    eventPlayers.put(player.getUniqueId(), new PlayerData(player));
    player.setGameMode(GameMode.ADVENTURE);

    // Teleport to event world spawn if event already started
    if (eventRunning) {
      World world = getEventWorld(currentEventName);
      if (world != null) {
        double x = plugin.getConfig().getDouble("events." + currentEventName + ".spawn.x");
        double y = plugin.getConfig().getDouble("events." + currentEventName + ".spawn.y");
        double z = plugin.getConfig().getDouble("events." + currentEventName + ".spawn.z");
        player.teleport(new org.bukkit.Location(world, x, y, z));
      } else {
        player.sendMessage("§cEvent world not found!");
      }

      // Add to boss bar so player sees countdown
      if (bossBar != null) bossBar.addPlayer(player);
    }
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

  private World getEventWorld(String eventName) {
    FileConfiguration config = plugin.getConfig();
    if (!config.isConfigurationSection("events." + eventName)) return null;

    String worldName = config.getString("events." + eventName + ".world");
    if (worldName == null) return null;

    return Bukkit.getWorld(worldName);
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