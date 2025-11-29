package com.liincoding.eventplugin.listeners;

import com.liincoding.eventplugin.events.EventManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class EventPlayerQuitListener implements Listener {

  private final EventManager eventManager;

  public EventPlayerQuitListener(EventManager eventManager) {
    this.eventManager = eventManager;
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (eventManager.isInEvent(player.getUniqueId())) {
      eventManager.leaveEvent(player);
      System.out.println(player.getName() + " disconnected and was restored from the event.");
    }
  }
}