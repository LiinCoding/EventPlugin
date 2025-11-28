package com.liincoding.eventplugin;

import com.liincoding.eventplugin.events.EventManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EventPlugin extends JavaPlugin {

    private static EventPlugin instance;
    private EventManager eventManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        eventManager = new EventManager(this);

        getCommand("event").setExecutor(new commands.EventCommand(eventManager));
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
