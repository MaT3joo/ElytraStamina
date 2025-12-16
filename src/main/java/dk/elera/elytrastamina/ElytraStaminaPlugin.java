package dk.elera.elytrastamina;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ElytraStaminaPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, Double> stamina = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Integer> noStaminaAttempts = new HashMap<>();
    private final Map<UUID, Integer> penaltiesCount = new HashMap<>();
    private double maxStamina;
    private double glideDrainPerTick;
    private double rocketCost;
    private double regenPerTick;
    private double staminaResetThreshold;
    private double penaltyVelocity;
    private int maxNoStaminaAttempts;
    private BukkitRunnable staminaTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        maxStamina = config.getDouble("max-stamina", 60.0);
        glideDrainPerTick = config.getDouble("glide-drain-per-second", 1.0) / 20.0;
        rocketCost = config.getDouble("rocket-cost", 10.0) / 2;
        regenPerTick = config.getDouble("regen-per-second", 0.3) / 20.0;
        staminaResetThreshold = config.getDouble("stamina-reset-threshold", 2.0);
        maxNoStaminaAttempts = config.getInt("max-no-stamina-attempts", 3);
				penaltyVelocity = config.getDouble("penalty-velocity", -3.0);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("elytrastamina").setExecutor(this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        stamina.putIfAbsent(event.getPlayer().getUniqueId(), maxStamina);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) bar.removeAll();
        noStaminaAttempts.remove(uuid);
        penaltiesCount.remove(uuid);
    }

    @EventHandler
    public void onRocketUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.isGliding() && event.getItem() != null && event.getItem().getType().toString().contains("FIREWORK_ROCKET") && event.getAction() == Action.RIGHT_CLICK_AIR) {
            UUID uuid = player.getUniqueId();
            stamina.putIfAbsent(uuid, maxStamina);
            double current = stamina.get(uuid);
            current -= rocketCost;
            stamina.put(uuid, Math.max(0, current));
        }
    }

    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        stamina.putIfAbsent(uuid, maxStamina);
        double current = stamina.get(uuid);

    		// Blokada startu lotu przy zerowej staminie
    		if (current <= 0 && event.isGliding()) {
    		    event.setCancelled(true);
    		}
				

				if (current < staminaResetThreshold) {
					int attempts = noStaminaAttempts.getOrDefault(uuid, 0) + 1;
      		noStaminaAttempts.put(uuid, attempts);

					if (attempts >= maxNoStaminaAttempts) {
							player.setVelocity(new org.bukkit.util.Vector(0, penaltyVelocity, 0));
							penaltiesCount.put(uuid, penaltiesCount.getOrDefault(uuid, 0) + 1);
							noStaminaAttempts.put(uuid, 0);
					}
				}

        startStaminaTask();
    }

    private void startStaminaTask() {
		    if (staminaTask != null) return;
		    staminaTask = new BukkitRunnable() {
		        @Override
		        public void run() {
		            boolean anyNotFull = false;
						
		            for (Player player : Bukkit.getOnlinePlayers()) {
		                UUID uuid = player.getUniqueId();
		                stamina.putIfAbsent(uuid, maxStamina);
		                double current = stamina.get(uuid);
		                boolean isFlying = player.isGliding();
		                boolean onGround = player.isOnGround();
										boolean touchedGround = true;
								
		                if (isFlying) {
												touchedGround = false;
		                    current -= glideDrainPerTick;
		                    if (current <= 0) {
		                        current = 0;
		                        player.setGliding(false);
		                    }
		                } else if (onGround) {
													touchedGround = true;
										}
									
		                if (!isFlying && touchedGround && current < maxStamina) {
		                    current += regenPerTick;
		                }
									
		                current = Math.max(0, Math.min(maxStamina, current));
		                stamina.put(uuid, current);
		                
		                // Resetuj licznik prób gdy stamina osiągnie staminaResetThreshold
		                if (current >= staminaResetThreshold) {
		                    noStaminaAttempts.put(uuid, 0);
		                }
						
		                if (current < maxStamina) anyNotFull = true;
						
		                updateBossBar(player, current);
		            }
							
		            if (!anyNotFull) {
		                this.cancel();
		                staminaTask = null;
		            }
		        }
		    };
		    staminaTask.runTaskTimer(this, 0L, 1L);
		}


    private void updateBossBar(Player player, double value) {
        UUID uuid = player.getUniqueId();
        BossBar bar = bossBars.computeIfAbsent(uuid, u -> {
            BossBar b = Bukkit.createBossBar("Elytra Stamina", BarColor.PURPLE, BarStyle.SOLID);
            b.addPlayer(player);
            return b;
        });
        double progress = value / maxStamina;
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        bar.setVisible(progress < 1.0);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("elytrastamina")) return false;
        
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /elytrastamina <reload|player> [amount]");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("elytrastamina.reload")) {
                sender.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            reloadConfig();
            maxStamina = getConfig().getDouble("max-stamina", 60.0);
            glideDrainPerTick = getConfig().getDouble("glide-drain-per-second", 1.0) / 20.0;
            rocketCost = getConfig().getDouble("rocket-cost", 10.0) / 2;
            regenPerTick = getConfig().getDouble("regen-per-second", 0.3) / 20.0;
            staminaResetThreshold = getConfig().getDouble("stamina-reset-threshold", 2.0);
            penaltyVelocity = getConfig().getDouble("penalty-velocity", -3.0);
            maxNoStaminaAttempts = getConfig().getInt("max-no-stamina-attempts", 3);
            sender.sendMessage("§aConfiguration reloaded!");
            return true;
        }
        
        if (!sender.hasPermission("elytrastamina.set")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /elytrastamina <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            UUID uuid = target.getUniqueId();
            amount = Math.max(0, Math.min(maxStamina, amount));
            stamina.put(uuid, amount);
            sender.sendMessage("§aSet stamina of " + target.getName() + " to " + amount);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: " + args[1]);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return Collections.emptyList();
    }
}