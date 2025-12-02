package com.liincoding.eventplugin.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.GameMode;

public class PlayerData {

    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final Location location;
    private final double maxHealth;
    private final double health;
    private final int food;
    private final float saturation;
    private final int xp;
    private final int level;
    private final PotionEffect[] effects;
    private final GameMode gameMode;
    private final int fireTicks;

    public PlayerData(Player player) {
        this.inventory = player.getInventory().getContents();
        this.armor = player.getInventory().getArmorContents();
        this.location = player.getLocation();
        this.maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        this.health = player.getHealth();
        this.food = player.getFoodLevel();
        this.xp = player.getTotalExperience();
        this.level = player.getLevel();
        this.saturation = player.getSaturation();
        this.effects = player.getActivePotionEffects().toArray(new PotionEffect[0]);
        this.gameMode = player.getGameMode();
        this.fireTicks = player.getFireTicks();
    }

    public void restore(Player player) {
        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor);
        player.teleport(location);
          player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
        player.setHealth(health);
        player.setFoodLevel(food);
        player.setSaturation(saturation);
        player.setTotalExperience(xp);
        player.setLevel(level);
        player.setGameMode(gameMode);
        player.setFireTicks(fireTicks);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
    }
}
