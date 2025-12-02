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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard. * ;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import java.util.function.Function;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.Node;

public class HideNSeekEvent implements EventManager.EventType,
Listener {

  private final EventPlugin plugin;
  private Player seeker;
  private final List < Player > hiders = new ArrayList < >();
  private final Random random = new Random();
  private World eventWorld;
  private Location eventSpawn;
  private Scoreboard scoreboard;
  private Objective objective;

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

    // Teleport seeker to event spawn
    if (eventSpawn != null) {
      seeker.teleport(eventSpawn);
    }

    seeker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1, false, false));
    seeker.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, 250, false, false, false));

    seeker.setWalkSpeed(0f);
    seeker.setFlySpeed(0f);

    // Delay 10 seconds → give equipment + unfreeze
    new BukkitRunnable() {@Override
      public void run() {
        if (!seeker.isOnline()) return;

        // Restore movement
        seeker.setWalkSpeed(0.2f);
        seeker.setFlySpeed(0.1f);

        // Now give the seeker equipment
        equipSeeker(seeker);
      }
    }.runTaskLater(plugin, 200L);

    // Rest are hiders
    for (Player p: players) {
      if (!p.equals(seeker)) {
        p.sendMessage("§aYou are a Hider!");
        p.setGameMode(GameMode.ADVENTURE);

        // Set hider health to 2 hearts
        p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(4.0);
        p.setHealth(4.0); // Also set current health
        p.setFoodLevel(3); // drops hunger to 3 (≤ 6 prevents sprinting)
        p.setSaturation(0); // ensures they can't immediately regen food

        // Apply tiny scale via console command
         AttributeInstance scaleAttr = p.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(0.08); // VERY small
        }
        hiders.add(p);
      }
    }
    updateScoreboard();
  }

  @Override
  public void onJoin(EventManager manager, Player player) {
    // optional
  }

  @Override
  public void onEnd(EventManager manager) {
    hiders.clear();
    seeker = null;
    eventWorld = null;
    eventSpawn = null;
    if (scoreboard != null) {
      for (Player p: Bukkit.getOnlinePlayers()) {
        if (p.getScoreboard() == scoreboard) {
          p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
      }
    }

    for (UUID uuid: manager.getEventPlayers().keySet()) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null) removeSpectatorPermission(p);
    }

    scoreboard = null;
    objective = null;
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
          giveSpectatorPermission(player);

          new BukkitRunnable() {@Override
            public void run() {
              hiders.remove(player);
              updateScoreboard();
            }
          }.runTaskLater(plugin, 2L);
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
    removeSpectatorPermission(player);
    updateScoreboard();
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

  private void updateScoreboard() {
    if (scoreboard == null) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();

        objective = scoreboard.registerNewObjective("hns", "dummy", "§c§lHide & Seek");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    // Clear old entries and teams
    for (String entry : scoreboard.getEntries()) {
        scoreboard.resetScores(entry);
        Team oldTeam = scoreboard.getTeam(entry);
        if (oldTeam != null) oldTeam.unregister();
    }

    int scoreValue = 100;

    // Helper method to generate a unique invisible entry
    Function<Integer, String> invisibleEntry = (i) -> ChatColor.values()[i % ChatColor.values().length].toString();

    int index = 0;

    // Seeker
    if (seeker != null) {
        String entry = invisibleEntry.apply(index++);
        Score score = objective.getScore(entry);
        score.setScore(scoreValue--);

        Team team = scoreboard.registerNewTeam("seekerTeam");
        team.addEntry(entry);
        team.setPrefix("§eSeeker: §c" + seeker.getName());
    }

    // Hiders header
    String headerEntry = invisibleEntry.apply(index++);
    Score headerScore = objective.getScore(headerEntry);
    headerScore.setScore(scoreValue--);
    Team headerTeam = scoreboard.registerNewTeam("headerTeam");
    headerTeam.addEntry(headerEntry);
    headerTeam.setPrefix("§aHiders Alive:");

    // Hiders
    for (Player hider : hiders) {
        String entry = invisibleEntry.apply(index++);
        Score score = objective.getScore(entry);
        score.setScore(scoreValue--);

        Team team = scoreboard.registerNewTeam("hiderTeam" + hider.getUniqueId());
        team.addEntry(entry);
        team.setPrefix("§7• " + hider.getName());
    }

    // Apply scoreboard
    for (Player p : Bukkit.getOnlinePlayers()) {
        if (hiders.contains(p) || p.equals(seeker)) p.setScoreboard(scoreboard);
    }
}

  private void giveSpectatorPermission(Player player) {
    LuckPerms api = LuckPermsProvider.get();
    User user = api.getUserManager().getUser(player.getUniqueId());
    if (user == null) return;

    Node node = PermissionNode.builder("nclaim.fly.bypass").value(true).build();
    user.data().add(node);
    api.getUserManager().saveUser(user);
  }

  private void removeSpectatorPermission(Player player) {
    LuckPerms api = LuckPermsProvider.get();
    User user = api.getUserManager().getUser(player.getUniqueId());
    if (user == null) return;

    Node node = PermissionNode.builder("nclaim.fly.bypass").value(true).build();
    user.data().remove(node);
    api.getUserManager().saveUser(user);
  }

}