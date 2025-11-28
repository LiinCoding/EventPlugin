package com.liincoding.eventplugin;

import com.liincoding.eventplugin.events.EventManager;
import com.liincoding.eventplugin.commands.EventCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class EventPlugin extends JavaPlugin {

    private static EventPlugin instance;
    private EventManager eventManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

    e   ventManager = new EventManager(this);

        getCommand("event").setExecutor(new EventCommand(eventManager));
    g   etCommand("eventend").setExecutor(new EventEndCommand(eventManager)); // new command
    }


    public static EventPlugin getInstance() {
        return instance;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public void onDisable() {
        // cleanup
    }
}
