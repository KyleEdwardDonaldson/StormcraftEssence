package dev.ked.stormcraft.essence.listener;

import dev.ked.stormcraft.StormcraftPlugin;
import dev.ked.stormcraft.essence.ability.ActiveAbilityManager;
import dev.ked.stormcraft.schedule.StormManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles Stormrider flight mechanics:
 * - Flight enabled in storms
 * - Gliding enabled outside storms
 * - No fall damage while Stormrider is active
 */
public class StormriderListener implements Listener {
    private final ActiveAbilityManager abilityManager;
    private final StormcraftPlugin stormcraftPlugin;

    public StormriderListener(ActiveAbilityManager abilityManager) {
        this.abilityManager = abilityManager;
        this.stormcraftPlugin = (StormcraftPlugin) Bukkit.getPluginManager().getPlugin("Stormcraft");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Skip if not in survival/adventure mode
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        // Check if Stormrider is active
        if (!abilityManager.isStormriderActive(player.getUniqueId())) {
            return;
        }

        if (stormcraftPlugin == null) return;

        StormManager stormManager = stormcraftPlugin.getStormManager();
        boolean inStorm = stormManager.isLocationInAnyStorm(player.getLocation());

        if (inStorm) {
            // In storm: Enable full flight
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
        } else {
            // Outside storm: Enable gliding only (no creative flight)
            if (player.isFlying()) {
                player.setFlying(false);
            }

            // Enable elytra-like gliding
            if (!player.isOnGround() && player.getFallDistance() > 0) {
                player.setGliding(true);
            }

            // Disable flight ability when on ground
            if (player.isOnGround()) {
                player.setAllowFlight(false);
                player.setGliding(false);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Cancel fall damage if Stormrider is active
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (abilityManager.isStormriderActive(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}
