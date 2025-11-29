package com.liincoding.eventplugin.listeners;

import com.liincoding.eventplugin.events.EventManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EventCommandBlockListener implements Listener {

    private final EventManager manager;

    private final Set<String> blockedCommands = new HashSet<>(Arrays.asList(
            "/spawn",
            "/tpa",
            "/tpahere",
            "/home",
            "/claim",
            "/rtp",
            "/wild"
    ));

    public EventCommandBlockListener(EventManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!manager.isEventRunning()) return;
        if (!manager.isInEvent(player.getUniqueId())) return;

        String msg = event.getMessage().toLowerCase();

        for (String cmd : blockedCommands) {
            if (msg.startsWith(cmd)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot use that command while in an event!");
                return;
            }
        }
    }
}
