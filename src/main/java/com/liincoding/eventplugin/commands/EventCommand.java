package com.yourname.eventplugin.commands;

import com.yourname.eventplugin.events.EventManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventCommand implements CommandExecutor {

    private final EventManager manager;

    public EventCommand(EventManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /event <start|join|leave> [eventName]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (!(sender.hasPermission("event.start"))) {
                    sender.sendMessage("You do not have permission!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Usage: /event start <eventName>");
                    return true;
                }
                String eventName = args[1];
                if (manager.startEvent(eventName)) {
                    Bukkit.broadcastMessage("Event " + eventName + " countdown started!");
                } else {
                    sender.sendMessage("Could not start event. Either it is running or event not found.");
                }
            }
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can join events!");
                    return true;
                }
                if (!player.hasPermission("event.join")) {
                    player.sendMessage("You do not have permission!");
                    return true;
                }
                if (!manager.joinEvent(player)) {
                    player.sendMessage("Could not join the event.");
                }
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can leave events!");
                    return true;
                }
                if (!player.hasPermission("event.leave")) {
                    player.sendMessage("You do not have permission!");
                    return true;
                }
                if (!manager.leaveEvent(player)) {
                    player.sendMessage("You are not in an event.");
                }
            }
            default -> sender.sendMessage("Unknown subcommand!");
        }

        return true;
    }
}
