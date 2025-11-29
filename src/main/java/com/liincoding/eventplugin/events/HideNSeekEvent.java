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
    Map < UUID,
    EventManager.PlayerData > playersMap = manager.getEventPlayers();
    if (playersMap.isEmpty()) return;

    List < Player > players = new ArrayList < >();
    for (UUID uuid: playersMap.keySet()) {
      Player player = Bukkit.getPlayer(uuid);
      if (player != null) players.add(player);
    }

    if (players.isEmpty()) return;

    // Pick 1 seeker randomly
    Player seeker = players.get(new Random().nextInt(players.size()));
    seeker.sendMessage("§cYou are the Seeker!");
    seeker.setGameMode(GameMode.SURVIVAL);

    // Rest are hiders
    for (Player p: players) {
      if (!p.equals(seeker)) {
        p.sendMessage("§aYou are a Hider!");
        p.setGameMode(GameMode.ADVENTURE);
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