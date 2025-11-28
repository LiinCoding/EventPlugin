package com.liincoding.eventplugin.commands;

import com.liincoding.eventplugin.events.EventManager;
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

        if (args.length == 0) {
            sender.sendMessage("Usage: /event <start|join|leave|end>");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "start":
                if (!sender.hasPermission("event.start")) {
                    sender.sendMessage("§cYou do not have permission.");
                    return true;
                }
                // TODO: call your manager.startEvent(eventName)
                sender.sendMessage("Event started (skeleton)");
                break;

            case "join":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can join events.");
                    return true;
                }
                if (!sender.hasPermission("event.join")) {
                    sender.sendMessage("§cYou do not have permission.");
                    return true;
                }
                manager.joinEvent(player); // make sure joinEvent() exists in EventManager
                sender.sendMessage("You joined the event.");
                break;

            case "leave":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can leave events.");
                    return true;
                }
                if (!sender.hasPermission("event.leave")) {
                    sender.sendMessage("§cYou do not have permission.");
                    return true;
                }
                manager.leaveEvent(player); // make sure leaveEvent() exists in EventManager
                sender.sendMessage("You left the event.");
                break;

            case "end":
                if (!sender.hasPermission("event.admin")) {
                    sender.sendMessage("§cYou do not have permission.");
                    return true;
                }
                if (!manager.isEventRunning()) {
                    sender.sendMessage("§cNo event is currently running.");
                    return true;
                }
                manager.endEvent();
                sender.sendMessage("§aEvent has been ended.");
                break;

            default:
                sender.sendMessage("Unknown subcommand. Usage: /event <start|join|leave|end>");
                break;
        }

        return true;
    }
}
