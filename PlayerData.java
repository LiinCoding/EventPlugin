package com.yourname.eventplugin.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class PlayerData {

    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final Location location;
    private final double health;
    private final int food;
    private final int xp;
    private final int level;
    private final PotionEffect[] effects;

    public PlayerData(Player player) {
        this.inventory = player.getInventory().getContents();
        this.armor = player.getInventory().getArmorContents();
        this.location = player.getLocation();
        this.health = player.getHealth();
        this.food = player.getFoodLevel();
        this.xp = player.getTotalExperience();
        this.level = player.getLevel();
        this.effects = player.getActivePotionEffects().toArray(new PotionEffect[0]);
    }

    public void restore(Player player) {
        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor);
        player.teleport(location);
        player.setHealth(health);
        player.setFoodLevel(food);
        player.setTotalExperience(xp);
        player.setLevel(level);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
    }
}
