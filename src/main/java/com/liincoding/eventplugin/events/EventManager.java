package com.liincoding.eventplugin.events;

import com.liincoding.eventplugin.events.PlayerData;
import com.liincoding.eventplugin.EventPlugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.World;
import org.bukkit.Location;

import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class EventManager {

  private final EventPlugin plugin;

  private boolean eventRunning = false;
  private boolean eventStarted = false;
  private String currentEventName;
  private BossBar bossBar;
  private static final int MIN_PLAYERS = 2;
  private File worldBackup = null;
  private String currentMapName;
  private final Random rand = new Random();
  private EventType currentEventType = null;

  // Stores original player locations and inventories
  private final Map < UUID, PlayerData > eventPlayers = new HashMap < >();

  public EventManager(EventPlugin plugin) {
    this.plugin = plugin;
  }

  public interface EventType {
    void onStart(EventManager manager);
    void onJoin(EventManager manager, Player player);
    void onEnd(EventManager manager);
  }

  public boolean isEventRunning() {
    return eventRunning;
  }

  public boolean hasEventStarted() {
    return eventStarted;
  }

  public String getCurrentEventName() {
    return currentEventName;
  }

  public Map < UUID,
  PlayerData > getEventPlayers() {
    return eventPlayers;
  }

  // Called when event starts
  public void startEvent(String eventName, String mapName) {
    if (eventRunning) return;

    // Get available maps from config
    List < String > maps = plugin.getConfig().getStringList("events." + eventName + ".maps");
    if (maps.isEmpty()) {
      plugin.getLogger().warning("Event '" + eventName + "' has no maps configured!");
      Bukkit.broadcastMessage("§cCannot start event '" + eventName + "': no maps configured!");
      return;
    }

    // Pick a map randomly if none specified
    if (mapName == null || mapName.isEmpty()) {
      int index = rand.nextInt(maps.size());
      mapName = maps.get(index);
      plugin.getLogger().info("Randomly selected map: " + mapName); // debug log
    } else if (!maps.contains(mapName)) {
      plugin.getLogger().warning("Event '" + eventName + "' map '" + mapName + "' does not exist!");
      Bukkit.broadcastMessage("§cCannot start event: map '" + mapName + "' not found!");
      return;
    }

    currentMapName = mapName;
    String worldName = mapName;

    // Try to create a backup
    if (!backupWorld(worldName)) {
      plugin.getLogger().warning("Event aborted: world backup failed for " + worldName);
      Bukkit.broadcastMessage("§cCannot start event '" + eventName + "': world backup failed!");
      return; // abort event
    }

    World eventWorld = Bukkit.getWorld(worldName);
    if (eventWorld == null) {
      plugin.getLogger().warning("Event world '" + worldName + "' is not loaded!");
      Bukkit.broadcastMessage("§cCannot start event '" + eventName + "': world not loaded!");
      return; // abort event
    }

    if (eventName.equalsIgnoreCase("hideNseek")) {
      int baseBorder = 30; // starting border
      int extraPer5Players = 10;
      int borderSize = baseBorder + ((eventPlayers.size() / 5) * extraPer5Players);
      eventWorld.getWorldBorder().setCenter(getSpawnLocation(eventName, mapName)); // center at spawn
      eventWorld.getWorldBorder().setSize(borderSize * 2); // Bukkit size = diameter
    }

    eventRunning = true;
    currentEventName = eventName;

    if (eventName.equalsIgnoreCase("hideNseek")) {
      currentEventType = new HideNSeekEvent(plugin); // pass plugin if needed
    } else {
      currentEventType = null; // generic event, no special logic
    }

    bossBar = Bukkit.createBossBar("Event " + eventName + " starting in 30s", BarColor.GREEN, BarStyle.SOLID);

    new org.bukkit.scheduler.BukkitRunnable() {
      int seconds = 30;

      @Override
      public void run() {
        if (seconds <= 0) {
          if (eventPlayers.size() < MIN_PLAYERS) {
            Bukkit.broadcastMessage("§cEvent " + eventName + " was cancelled due to insufficient players.");
            endEvent();
            cancel();
            return;
          }

          bossBar.removeAll();
          Bukkit.broadcastMessage("§aEvent " + eventName + " has started!");

          eventStarted = true;

          // Trigger event-specific logic
          if (currentEventType != null) {
            currentEventType.onStart(EventManager.this);
          }

          cancel();
          return;
        }

        bossBar.setTitle("Event " + eventName + " starting in " + seconds + "s");
        bossBar.setProgress(seconds / 30.0);

        for (Player player: Bukkit.getOnlinePlayers()) {
          bossBar.addPlayer(player);
        }
        seconds--;
      }
    }.runTaskTimer(plugin, 0L, 20L); // 20 ticks = 1 second
  }

  private Location getSpawnLocation(String eventName, String mapName) {
    FileConfiguration config = plugin.getConfig();
    double x = config.getDouble("events." + eventName + ".spawn." + mapName + ".x");
    double y = config.getDouble("events." + eventName + ".spawn." + mapName + ".y");
    double z = config.getDouble("events." + eventName + ".spawn." + mapName + ".z");
    World world = Bukkit.getWorld(mapName);
    if (world == null) return null;
    return new Location(world, x, y, z);
  }

  // Called when event ends (or is cancelled)
  public void endEvent() {
    eventStarted = false;

    if (!eventRunning) return;

    // Restore all players
    for (UUID uuid: eventPlayers.keySet()) {
      Player player = Bukkit.getPlayer(uuid);
      if (player != null && player.isOnline()) {
        PlayerData data = eventPlayers.get(uuid);
        data.restore(player); // restore everything
      }
    }

    // Restore world
    if (currentMapName != null) restoreWorld(currentMapName);

    // Clear boss bar (countdown or running event)
    if (bossBar != null) {
      bossBar.removeAll();
      bossBar = null; // set to null so future events create a new one
    }

    // Reset state
    eventPlayers.clear();
    eventRunning = false;
    currentEventName = null;

    plugin.getLogger().info("Event has ended.");
  }

  public boolean isInEvent(UUID uuid) {
    return eventPlayers.containsKey(uuid);
  }

  private boolean backupWorld(String worldName) {
    File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
    if (!worldFolder.exists()) {
      plugin.getLogger().warning("World " + worldName + " does not exist!");
      return false;
    }

    worldBackup = new File(Bukkit.getWorldContainer(), worldName + "_backup");
    try {
      if (worldBackup.exists()) FileUtils.deleteDirectory(worldBackup);
      FileUtils.copyDirectory(worldFolder, worldBackup);
      plugin.getLogger().info("Backup of world '" + worldName + "' created.");
      return true;
    } catch(IOException e) {
      e.printStackTrace();
      plugin.getLogger().warning("Failed to backup world '" + worldName + "'");
      return false;
    }
  }

  private void restoreWorld(String worldName) {
    if (worldBackup == null || !worldBackup.exists()) {
      plugin.getLogger().warning("No backup found for world '" + worldName + "'");
      return;
    }

    File worldFolder = new File(Bukkit.getWorldContainer(), worldName);

    try {
      // Step 1: Delete the current world folder if it exists
      if (worldFolder.exists()) {
        FileUtils.deleteDirectory(worldFolder);
      }

      // Step 2: Copy backup to the world folder
      FileUtils.copyDirectory(worldBackup, worldFolder);

      // Step 3: Only delete the backup AFTER successful copy
      FileUtils.deleteDirectory(worldBackup);
      worldBackup = null;

      plugin.getLogger().info("World '" + worldName + "' restored successfully from backup.");
    } catch(IOException e) {
      plugin.getLogger().severe("Failed to restore world '" + worldName + "'. Backup is still intact!");
      e.printStackTrace();
    }
  }

  public void addPlayer(Player player) {
    eventPlayers.put(player.getUniqueId(), new PlayerData(player));
    player.setGameMode(GameMode.ADVENTURE);

    // Teleport to event world spawn if event already started
    if (eventRunning) {
      World world = getEventWorld(currentMapName);
      if (world != null) {
        Location spawn = getSpawnLocation(currentEventName, currentMapName);
        if (spawn != null) player.teleport(spawn);
        else player.sendMessage("§cEvent spawn location not found!");
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
    // Handle HideNSeek-specific logic first
    if (currentEventType instanceof HideNSeekEvent hideEvent) {
      hideEvent.leaveEvent(this, player);
    }

    // Restore all player data
    PlayerData data = eventPlayers.remove(player.getUniqueId());
    if (data != null) {
      data.restore(player); // restores inventory, armor, location, health, food, XP, level, effects, gamemode, fire ticks
    }
  }

  private World getEventWorld(String mapName) {
    return Bukkit.getWorld(mapName);
  }
}