package com.liincoding.eventplugin.events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.GameMode;

public class Event {

    private final String name;
    private final ConfigurationSection config;
    private boolean running = false;

    public Event(String name, ConfigurationSection config) {
        this.name = name;
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void teleportToEvent(Player player) {
        String worldName = config.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage("Event world not found!");
            return;
        }

        double x = config.getDouble("spawn.x");
        double y = config.getDouble("spawn.y");
        double z = config.getDouble("spawn.z");

        Location spawn = new Location(world, x, y, z);
        player.teleport(spawn);
    }
}
