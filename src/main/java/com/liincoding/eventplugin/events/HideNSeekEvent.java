package com.liincoding.eventplugin.events;

import com.liincoding.eventplugin.events.PlayerData;
import com.liincoding.eventplugin.EventPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Random;

public class HideNSeekEvent implements EventManager.EventType,
Listener {

  private final EventPlugin plugin;
  private Player seeker;
  private final List < Player > hiders = new ArrayList < >();
  private final Random random = new Random();

  public HideNSeekEvent(EventPlugin plugin) {
    this.plugin = plugin;
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void onStart(EventManager manager) {
    Map<UUID, PlayerData> playersMap = manager.getEventPlayers();
    if (playersMap.isEmpty()) return;

    List < Player > players = new ArrayList < >();
    for (UUID uuid: playersMap.keySet()) {
      Player player = Bukkit.getPlayer(uuid);
      if (player != null) players.add(player);
    }

    if (players.isEmpty()) return;

    // Pick 1 seeker randomly
    seeker = players.get(random.nextInt(players.size()));
    seeker.sendMessage("§cYou are the Seeker!");
    seeker.setGameMode(GameMode.SURVIVAL);

    // Rest are hiders
    for (Player p: players) {
      if (!p.equals(seeker)) {
        p.sendMessage("§aYou are a Hider!");
        p.setGameMode(GameMode.ADVENTURE);

        // Set hider health to 2 hearts
        p.setHealth(4.0);

        // Set hider scale
        p.setScale(0.08);

        // Optionally, add to a list of hiders to use later (e.g., for damage prevention)
        hiders.add(p);
      }
    }
  }

  @Override
  public void onJoin(EventManager manager, Player player) {
    // Optional: handle player-specific logic when they join mid-countdown
  }

  @Override
  public void onEnd(EventManager manager) {
    // Reset hiders' health and scale
    for (Player hider: hiders) {
      if (hider.isOnline()) {
        hider.setHealth(hider.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
        hider.setScale(1.0);
      }
    }
    hiders.clear();
    seeker = null;
  }

  // Listener to prevent hiders from dealing damage
  @EventHandler
  public void onDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player damager) {
      if (hiders.contains(damager)) {
        event.setCancelled(true);
      }
    }
  }

  public void leaveEvent(EventManager manager, Player player) {
    // If the player is a hider, reset their special attributes
    if (hiders.contains(player)) {
      if (player.isOnline()) {
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());

        // Only if your server supports it (Paper 1.19+)
        try {
          player.setScale(1.0);
        } catch(NoSuchMethodError ignored) {
          // If not supported, just ignore scaling
        }
      }
      hiders.remove(player);
    }

    // Restore all saved player data
    PlayerData data = manager.getEventPlayers().remove(player.getUniqueId());
    if (data != null) {
      data.restore(player);
    }
  }
}