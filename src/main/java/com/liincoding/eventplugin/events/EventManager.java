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
  private String currentMapName;
  private final Random rand = new Random();
  private EventType currentEventType = null;
  private String currentEventWorldName;
  private String templateMapName;

  // Stores original player locations and inventories
  private final Map < UUID,
  PlayerData > eventPlayers = new HashMap < >();

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

    String templateMap = mapName; // the original world template
    currentEventWorldName = templateMap + "_event_" + System.currentTimeMillis(); // unique name

    // Clone the template world via Multiverse
    Bukkit.dispatchCommand(
    Bukkit.getConsoleSender(), "mv clone " + templateMap + " " + currentEventWorldName);

    // Wait for world to load
    new org.bukkit.scheduler.BukkitRunnable() {@Override
      public void run() {
        World eventWorld = Bukkit.getWorld(currentEventWorldName);
        if (eventWorld != null) {
          setupEventWorld(eventWorld, eventName); // sets borders, initializes event, boss bar, etc.
          cancel();
        }
      }
    }.runTaskTimer(plugin, 0L, 1L); // check every tick

    templateMapName = templateMap;

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

  private Location getSpawnLocation(World world, String eventName, String mapName) {
    FileConfiguration config = plugin.getConfig();
    double x = config.getDouble("events." + eventName + ".spawn." + mapName + ".x");
    double y = config.getDouble("events." + eventName + ".spawn." + mapName + ".y");
    double z = config.getDouble("events." + eventName + ".spawn." + mapName + ".z");
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

    if (currentEventWorldName != null) {
      World eventWorld = getEventWorld(currentEventWorldName);
      deleteWorld(eventWorld);
      currentEventWorldName = null;
      templateMapName = null;
      currentEventType = null;
    }

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

  private void setupEventWorld(World eventWorld, String eventName) {

    if (eventName.equalsIgnoreCase("hideNseek")) {
      int baseBorder = 30;
      int extraPer5Players = 10;
      int borderSize = baseBorder + ((eventPlayers.size() / 5) * extraPer5Players);
      eventWorld.getWorldBorder().setCenter(getSpawnLocation(eventWorld, eventName, templateMapName));
      eventWorld.getWorldBorder().setSize(borderSize * 2);

      currentEventType = new HideNSeekEvent(plugin);
    } else {
      currentEventType = null;
    }

    eventRunning = true;
    currentEventName = eventName;
  }

  public void deleteWorld(World world) {
    if (world == null) return;

    // Teleport any players out
    for (Player player: world.getPlayers()) {
      player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
    }

    // Unload the world
    Bukkit.unloadWorld(world, false);

    // Delete world folder
    try {
      File worldFolder = world.getWorldFolder();
      if (worldFolder.exists()) {
        FileUtils.deleteDirectory(worldFolder);
      }
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  public boolean isInEvent(UUID uuid) {
    return eventPlayers.containsKey(uuid);
  }

  public void addPlayer(Player player) {
    eventPlayers.put(player.getUniqueId(), new PlayerData(player));
    player.setGameMode(GameMode.ADVENTURE);

    // Teleport to event world spawn if event already started
    if (eventRunning) {
      World world = getEventWorld(currentEventWorldName);
      if (world != null) {
        Location spawn = getSpawnLocation(world, currentEventName, templateMapName);
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
    // Check if the player is in the event
    if (!isInEvent(player.getUniqueId())) {
        player.sendMessage("§cYou are not currently in an event!");
        return;
    }

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