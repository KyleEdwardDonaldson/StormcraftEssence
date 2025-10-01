package dev.ked.stormcraft.essence.listener;

import dev.ked.stormcraft.api.events.StormcraftExposureCheckEvent;
import dev.ked.stormcraft.api.events.StormcraftStormTickEvent;
import dev.ked.stormcraft.essence.ability.AbilityManager;
import dev.ked.stormcraft.essence.config.EssenceConfig;
import dev.ked.stormcraft.essence.model.PassiveAbility;
import dev.ked.stormcraft.essence.model.PlayerEssenceData;
import dev.ked.stormcraft.essence.persistence.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applies passive ability effects during storms.
 */
public class PassiveAbilityListener implements Listener {
    private final AbilityManager abilityManager;
    private final PlayerDataManager playerDataManager;
    private final EssenceConfig config;

    public PassiveAbilityListener(AbilityManager abilityManager, PlayerDataManager playerDataManager, EssenceConfig config) {
        this.abilityManager = abilityManager;
        this.playerDataManager = playerDataManager;
        this.config = config;
    }

    /**
     * Apply Storm Resistance passive (reduces storm damage)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onExposureCheck(StormcraftExposureCheckEvent event) {
        Player player = event.getPlayer();
        PlayerEssenceData data = playerDataManager.getPlayerData(player);

        if (!data.getActivePassives().contains(PassiveAbility.STORM_RESISTANCE)) {
            return;
        }

        // Calculate damage reduction
        double reductionPercent = abilityManager.getStormResistanceReduction(data);
        if (reductionPercent > 0) {
            double currentDamage = event.getDamageAmount();
            double reducedDamage = currentDamage * (1.0 - (reductionPercent / 100.0));
            event.setDamageAmount(reducedDamage);
        }
    }

    /**
     * Apply Lightning Reflexes (speed during storm) and Stormborn (regen when exposed)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStormTick(StormcraftStormTickEvent event) {
        for (Player player : event.getExposedPlayers()) {
            PlayerEssenceData data = playerDataManager.getPlayerData(player);

            // Lightning Reflexes - speed boost during storm
            if (data.getActivePassives().contains(PassiveAbility.LIGHTNING_REFLEXES)) {
                int speedAmplifier = abilityManager.getLightningReflexesSpeed(data);
                if (speedAmplifier >= 0) {
                    int duration = config.getDrainIntervalTicks() + 10; // Slightly longer than tick interval
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, speedAmplifier, false, false, true));
                }
            }

            // Stormborn - health regen when exposed
            if (data.getActivePassives().contains(PassiveAbility.STORMBORN)) {
                double regenAmount = abilityManager.getStormbornRegen(data);
                if (regenAmount > 0 && player.getHealth() < player.getMaxHealth()) {
                    double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + regenAmount);
                    player.setHealth(newHealth);
                }
            }
        }
    }
}
