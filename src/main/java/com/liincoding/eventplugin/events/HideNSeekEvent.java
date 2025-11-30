package com.liincoding.eventplugin.events;

import com.liincoding.eventplugin.events.PlayerData;
import com.liincoding.eventplugin.EventPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Random;

public class HideNSeekEvent implements EventManager.EventType,
Listener {

  private final EventPlugin plugin;
  private Player seeker;
  private final List < Player > hiders = new ArrayList < >();
  private final Random random = new Random();
  private World eventWorld;
  private Location eventSpawn;

  public HideNSeekEvent(EventPlugin plugin) {
    this.plugin = plugin;
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void onStart(EventManager manager) {
    Map < UUID,
    PlayerData > playersMap = manager.getEventPlayers();
    if (playersMap.isEmpty()) return;

    List < Player > players = new ArrayList < >();
    for (UUID uuid: playersMap.keySet()) {
      Player player = Bukkit.getPlayer(uuid);
      if (player != null) players.add(player);
    }

    if (players.isEmpty()) return;

    eventWorld = Bukkit.getWorld(manager.getCurrentEventWorldName());
    eventSpawn = manager.getEventSpawnLocation();

    // Pick 1 seeker randomly
    seeker = players.get(random.nextInt(players.size()));
    seeker.sendMessage("§cYou are the Seeker!");
    seeker.setGameMode(GameMode.SURVIVAL);

    // Equip seeker with weapons and rockets
    equipSeeker(seeker); // <--- call here

    // Rest are hiders
    for (Player p: players) {
      if (!p.equals(seeker)) {
        p.sendMessage("§aYou are a Hider!");
        p.setGameMode(GameMode.ADVENTURE);

        // Set hider health to 2 hearts
        p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(4.0);
        p.setHealth(4.0); // Also set current health
        
        // Apply tiny scale via console command
        Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(), "attribute " + p.getUniqueId() + " minecraft:scale base set 0.08");

        hiders.add(p);
      }
    }

    forceHidersSneak();

  }

  @Override
  public void onJoin(EventManager manager, Player player) {
    // optional
  }

  @Override
  public void onEnd(EventManager manager) {
    // Reset hiders' health and scale
    for (Player hider: hiders) {
      if (hider.isOnline()) {
        hider.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0); // restore default

        // Restore scale using command (no setScale method)
        Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(), "attribute " + hider.getUniqueId() + " minecraft:scale base set 1.0");
      }
    }

    hiders.clear();
    seeker = null;
    eventWorld = null;
    eventSpawn = null;
  }

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player damager) {
      if (hiders.contains(damager)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onHiderDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();

    if (hiders.contains(player) && eventWorld != null) {
      // Prevent drops and XP
      event.getDrops().clear();
      event.setDroppedExp(0);

      new BukkitRunnable() {@Override
        public void run() {
          if (!player.isOnline()) return;

          // Force respawn
          player.spigot().respawn();

          // Teleport to event world spawn
          if (eventSpawn != null) {
            player.teleport(eventSpawn);
          }

          // Set to spectator
          player.setGameMode(GameMode.SPECTATOR);
        }
      }.runTaskLater(plugin, 1L);
    }
  }

  public void leaveEvent(EventManager manager, Player player) {

    if (hiders.contains(player)) {
      if (player.isOnline()) {
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0); // restore default

        // restore scale
        Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(), "attribute " + player.getUniqueId() + " minecraft:scale base set 1.0");
      }
      hiders.remove(player);
    }

    // Restore saved data
    PlayerData data = manager.getEventPlayers().remove(player.getUniqueId());
    if (data != null) {
      data.restore(player);
    }
  }

  private void equipSeeker(Player seeker) {
    if (seeker == null) return;

    PlayerInventory inv = seeker.getInventory();
    inv.clear(); // optional: clear inventory first

    // Give two crossbows with Quick Charge III
    for (int i = 0; i < 2; i++) {
      ItemStack crossbow = new ItemStack(Material.CROSSBOW);
      CrossbowMeta meta = (CrossbowMeta) crossbow.getItemMeta();
      meta.addEnchant(Enchantment.QUICK_CHARGE, 3, true);
      crossbow.setItemMeta(meta);
      inv.addItem(crossbow);
    }

    // Firework rocket stack
    ItemStack fireworkStack = new ItemStack(Material.FIREWORK_ROCKET, 64);
    FireworkMeta fwMeta = (FireworkMeta) fireworkStack.getItemMeta();

    FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(Color.RED).withFade(Color.RED).build();

    fwMeta.addEffect(effect);
    fwMeta.setPower(3); // Flight duration
    fireworkStack.setItemMeta(fwMeta);

    // Give to offhand
    inv.setItemInOffHand(fireworkStack);
  }

  // Call this after assigning hiders in onStart
  private void forceHidersSneak() {
    for (Player hider: hiders) {
      if (hider.isOnline()) {
        hider.setSneaking(true); // force sneaking
      }
    }
  }

  // Event handler to prevent them from stopping sneaking
  @EventHandler
  public void onHiderMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (hiders.contains(player) && !player.isSneaking()) {
      player.setSneaking(true); // reapply sneaking
    }
  }
}