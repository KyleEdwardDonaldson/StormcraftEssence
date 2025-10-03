package dev.ked.stormcraft.essence.ability;

import dev.ked.stormcraft.StormcraftPlugin;
import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import dev.ked.stormcraft.essence.config.EssenceConfig;
import dev.ked.stormcraft.essence.model.PassiveAbility;
import dev.ked.stormcraft.essence.model.PlayerEssenceData;
import dev.ked.stormcraft.essence.persistence.PlayerDataManager;
import dev.ked.stormcraft.model.TravelingStorm;
import dev.ked.stormcraft.schedule.StormManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Manages active compass-based abilities.
 */
public class ActiveAbilityManager {
    private final StormcraftEssencePlugin plugin;
    private final EssenceConfig config;
    private final PlayerDataManager playerDataManager;
    private final Economy economy;
    private final StormcraftPlugin stormcraftPlugin;

    // Cooldown tracking (player UUID -> ability -> expiry time)
    private final Map<UUID, Map<PassiveAbility, Long>> cooldowns = new HashMap<>();

    // Active effects tracking
    private final Map<UUID, Long> eyeOfStormExpiry = new HashMap<>();
    private final Map<UUID, BukkitRunnable> stormSenseParticles = new HashMap<>();
    private final Map<UUID, Boolean> stormriderActive = new HashMap<>(); // Stormrider flight mode toggle

    public ActiveAbilityManager(StormcraftEssencePlugin plugin, EssenceConfig config,
                               PlayerDataManager playerDataManager, Economy economy) {
        this.plugin = plugin;
        this.config = config;
        this.playerDataManager = playerDataManager;
        this.economy = economy;
        this.stormcraftPlugin = (StormcraftPlugin) Bukkit.getPluginManager().getPlugin("Stormcraft");
    }

    /**
     * Attempts to use an active ability.
     * @return true if ability was successfully used
     */
    public boolean useAbility(Player player, PassiveAbility ability) {
        PlayerEssenceData data = playerDataManager.getPlayerData(player);

        // Check if ability is active (not passive)
        if (!ability.isActive()) {
            return false;
        }

        // Check if player has ability unlocked
        int required = config.getUnlockLevel(ability);
        if (data.getStormExposureLevel() < required) {
            player.sendMessage(Component.text("You need SEL " + required + " to use " + ability.getDisplayName())
                    .color(NamedTextColor.RED));
            return false;
        }

        // Check cooldown
        if (isOnCooldown(player, ability)) {
            long remaining = getCooldownRemaining(player, ability);
            player.sendMessage(Component.text("Ability on cooldown: " + formatTime(remaining))
                    .color(NamedTextColor.RED));
            return false;
        }

        // Execute ability
        return switch (ability) {
            case STORM_SENSE -> useStormSense(player);
            case EYE_OF_THE_STORM -> useEyeOfStorm(player);
            case STORMCALLER -> useStormcaller(player);
            case STORMCLEAR -> useStormclear(player);
            case STORMRIDER -> toggleStormrider(player);
            default -> false;
        };
    }

    /**
     * Storm Sense - Shows particle trail to nearest storm edge
     */
    private boolean useStormSense(Player player) {
        int cost = config.getEssenceCost(PassiveAbility.STORM_SENSE);
        if (!chargeEssence(player, cost)) {
            return false;
        }

        // Cancel existing particle task if any
        stopStormSense(player);

        // Start particle trail task
        BukkitRunnable particleTask = new BukkitRunnable() {
            int ticks = 0;
            final int duration = config.getStormSenseParticleDuration();

            @Override
            public void run() {
                if (ticks++ >= duration || !player.isOnline()) {
                    stopStormSense(player);
                    return;
                }

                showStormSenseParticles(player);
            }
        };

        int interval = config.getStormSenseParticleInterval();
        particleTask.runTaskTimer(plugin, 0L, interval);
        stormSenseParticles.put(player.getUniqueId(), particleTask);

        int cooldown = config.getCooldownSeconds(PassiveAbility.STORM_SENSE) * 1000;
        setCooldown(player, PassiveAbility.STORM_SENSE, cooldown);
        player.sendMessage(Component.text("⛈ Storm Sense activated - Follow the particles to safety!")
                .color(NamedTextColor.AQUA));
        return true;
    }

    /**
     * Shows particles leading to nearest storm edge
     */
    private void showStormSenseParticles(Player player) {
        if (stormcraftPlugin == null) return;

        StormManager stormManager = stormcraftPlugin.getStormManager();
        List<TravelingStorm> storms = stormManager.getActiveStorms();

        if (storms.isEmpty()) {
            player.sendMessage(Component.text("No storms nearby").color(NamedTextColor.GRAY));
            stopStormSense(player);
            return;
        }

        // Find closest storm
        TravelingStorm closest = findClosestStorm(player, storms);
        if (closest == null) return;

        Location playerLoc = player.getLocation().add(0, 1, 0); // Eye level
        Location stormLoc = closest.getCurrentLocation();
        double radius = closest.getDamageRadius();

        // Calculate escape direction (away from storm)
        Vector direction = playerLoc.toVector().subtract(stormLoc.toVector()).normalize();

        // Calculate distance to edge
        double distToCenter = playerLoc.distance(stormLoc);
        double distToEdge = Math.max(0, distToCenter - radius);

        // Show particles leading away from storm
        for (int i = 1; i <= 5; i++) {
            Location particleLoc = playerLoc.clone().add(direction.clone().multiply(i * 2));
            player.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 3, 0.2, 0.2, 0.2, 0.01);
        }
    }

    /**
     * Eye of the Storm - 10 minute immunity bubble (500 essence, 20 min cooldown)
     */
    private boolean useEyeOfStorm(Player player) {
        int cost = config.getEssenceCost(PassiveAbility.EYE_OF_THE_STORM);
        if (!chargeEssence(player, cost)) {
            return false;
        }

        long duration = config.getEyeOfStormDuration() * 1000; // Convert seconds to milliseconds
        eyeOfStormExpiry.put(player.getUniqueId(), System.currentTimeMillis() + duration);

        int cooldown = config.getCooldownSeconds(PassiveAbility.EYE_OF_THE_STORM) * 1000;
        setCooldown(player, PassiveAbility.EYE_OF_THE_STORM, cooldown);
        player.sendMessage(Component.text("☁ Eye of the Storm activated - Storm immunity for 10 minutes!")
                .color(NamedTextColor.GOLD));

        // Visual effect
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 50, 2, 2, 2, 0.1);

        return true;
    }

    /**
     * Checks if player has Eye of the Storm active
     */
    public boolean hasStormImmunity(UUID playerId) {
        Long expiry = eyeOfStormExpiry.get(playerId);
        if (expiry == null) return false;

        if (System.currentTimeMillis() > expiry) {
            eyeOfStormExpiry.remove(playerId);
            return false;
        }

        return true;
    }

    /**
     * Stormcaller - Summon lightning (100 essence, 30s cooldown)
     */
    private boolean useStormcaller(Player player) {
        int cost = config.getEssenceCost(PassiveAbility.STORMCALLER);
        if (!chargeEssence(player, cost)) {
            return false;
        }

        // Strike lightning where player is looking
        int range = (int) config.getStormcallerRange();
        Location target = player.getTargetBlock(null, range).getLocation();
        player.getWorld().strikeLightning(target);

        int cooldown = config.getCooldownSeconds(PassiveAbility.STORMCALLER) * 1000;
        setCooldown(player, PassiveAbility.STORMCALLER, cooldown);
        player.sendMessage(Component.text("⚡ Lightning strike called!")
                .color(NamedTextColor.YELLOW));

        return true;
    }

    /**
     * Stormclear - Push storms away and boost their speed (2000 essence, 60 min cooldown)
     */
    private boolean useStormclear(Player player) {
        int cost = config.getEssenceCost(PassiveAbility.STORMCLEAR);
        if (!chargeEssence(player, cost)) {
            return false;
        }

        if (stormcraftPlugin == null) {
            player.sendMessage(Component.text("Stormcraft plugin not found!").color(NamedTextColor.RED));
            return false;
        }

        StormManager stormManager = stormcraftPlugin.getStormManager();
        List<TravelingStorm> storms = stormManager.getActiveStorms();

        int pushedCount = 0;
        Location playerLoc = player.getLocation();
        double pushDistance = config.getStormclearPushDistance();

        for (TravelingStorm storm : storms) {
            Location stormLoc = storm.getCurrentLocation();
            if (!stormLoc.getWorld().equals(player.getWorld())) continue;

            double distance = playerLoc.distance(stormLoc);
            if (distance <= pushDistance) {
                // Push storm away - set new target opposite from player
                Vector pushDirection = stormLoc.toVector().subtract(playerLoc.toVector()).normalize();
                Location newTarget = stormLoc.clone().add(pushDirection.multiply(pushDistance * 3));
                storm.setTargetLocation(newTarget);

                // Boost storm speed temporarily
                double speedBoost = config.getStormclearSpeedAmplifier();
                int speedDuration = config.getStormclearSpeedDuration();
                storm.setTempSpeedBoost(speedBoost, speedDuration);

                pushedCount++;
            }
        }

        int cooldown = config.getCooldownSeconds(PassiveAbility.STORMCLEAR) * 1000;
        setCooldown(player, PassiveAbility.STORMCLEAR, cooldown);
        player.sendMessage(Component.text("⚡ STORMCLEAR! " + pushedCount + " storms pushed away!")
                .color(NamedTextColor.RED));

        // Massive visual effect
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, 5, 5, 5, 0.1);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 100, 10, 10, 10, 0.5);

        return true;
    }

    /**
     * Stormrider - Toggle flight mode (free, no cooldown - toggleable)
     */
    private boolean toggleStormrider(Player player) {
        UUID playerId = player.getUniqueId();
        boolean currentlyActive = stormriderActive.getOrDefault(playerId, false);

        if (currentlyActive) {
            // Disable Stormrider
            stormriderActive.put(playerId, false);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(Component.text("✈ Stormrider deactivated")
                    .color(NamedTextColor.GRAY));
        } else {
            // Enable Stormrider
            stormriderActive.put(playerId, true);
            player.sendMessage(Component.text("✈ Stormrider activated - Flight enabled in storms!")
                    .color(NamedTextColor.AQUA));
        }

        return true;
    }

    /**
     * Checks if player has Stormrider active
     */
    public boolean isStormriderActive(UUID playerId) {
        return stormriderActive.getOrDefault(playerId, false);
    }

    /**
     * Helper methods
     */

    private TravelingStorm findClosestStorm(Player player, List<TravelingStorm> storms) {
        TravelingStorm closest = null;
        double closestDist = Double.MAX_VALUE;

        for (TravelingStorm storm : storms) {
            Location stormLoc = storm.getCurrentLocation();
            if (!stormLoc.getWorld().equals(player.getWorld())) continue;

            double dist = player.getLocation().distance(stormLoc);
            if (dist < closestDist) {
                closestDist = dist;
                closest = storm;
            }
        }

        return closest;
    }

    private boolean chargeEssence(Player player, double amount) {
        if (economy.getBalance(player) < amount) {
            player.sendMessage(Component.text("Insufficient essence! Need " + amount + " essence")
                    .color(NamedTextColor.RED));
            return false;
        }

        economy.withdrawPlayer(player, amount);
        return true;
    }

    private void setCooldown(Player player, PassiveAbility ability, long cooldownMillis) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(ability, System.currentTimeMillis() + cooldownMillis);
    }

    private boolean isOnCooldown(Player player, PassiveAbility ability) {
        Map<PassiveAbility, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        Long expiry = playerCooldowns.get(ability);
        if (expiry == null) return false;

        if (System.currentTimeMillis() > expiry) {
            playerCooldowns.remove(ability);
            return false;
        }

        return true;
    }

    private long getCooldownRemaining(Player player, PassiveAbility ability) {
        Map<PassiveAbility, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;

        Long expiry = playerCooldowns.get(ability);
        if (expiry == null) return 0;

        return Math.max(0, expiry - System.currentTimeMillis());
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes + "m " + seconds + "s";
    }

    private void stopStormSense(Player player) {
        BukkitRunnable task = stormSenseParticles.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Cleanup when plugin disables
     */
    public void shutdown() {
        // Cancel all particle tasks
        for (BukkitRunnable task : stormSenseParticles.values()) {
            task.cancel();
        }
        stormSenseParticles.clear();
        cooldowns.clear();
        eyeOfStormExpiry.clear();
        stormriderActive.clear();
    }
}
