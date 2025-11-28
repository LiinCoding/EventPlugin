package com.liincoding.eventplugin.commands;

import com.liincoding.eventplugin.events.EventManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EventEndCommand implements CommandExecutor {

    private final EventManager eventManager;

    public EventEndCommand(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage("§cYou do not have permission to end events.");
            return true;
        }

        if (!eventManager.isEventRunning()) {
            sender.sendMessage("§cNo event is currently running.");
            return true;
        }

        eventManager.endEvent();
        sender.sendMessage("§aEvent has been ended.");

        return true;
    }
}
