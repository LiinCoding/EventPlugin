package com.liincoding.eventplugin;

import com.liincoding.eventplugin.listeners.EventCommandBlockListener;
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

        eventManager = new EventManager(this);

        getCommand("event").setExecutor(new EventCommand(eventManager));

        getServer().getPluginManager().registerEvents(
            new EventCommandBlockListener(eventManager),
            this
        );

    }


    public static EventPlugin getInstance() {
        return instance;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public boolean isInEvent(UUID uuid) {
        return eventPlayers.containsKey(uuid);
    }

    @Override
    public void onDisable() {
        // cleanup
    }
}
