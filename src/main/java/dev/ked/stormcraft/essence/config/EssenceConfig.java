package dev.ked.stormcraft.essence.config;

import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import dev.ked.stormcraft.essence.model.PassiveAbility;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages configuration for Stormcraft-Essence.
 */
public class EssenceConfig {
    private final StormcraftEssencePlugin plugin;
    private FileConfiguration config;

    // Ability unlock levels
    private final Map<PassiveAbility, Integer> unlockLevels = new HashMap<>();

    // Active ability settings
    private final Map<PassiveAbility, Integer> essenceCosts = new HashMap<>();
    private final Map<PassiveAbility, Integer> cooldownSeconds = new HashMap<>();

    // Drain settings
    private double baseDrainPerSecond;
    private boolean selScalingEnabled;
    private double drainPerTenLevels;
    private double multiPassiveMultiplier;
    private int drainIntervalTicks;

    // Crafting settings
    private int compassEssenceCost;
    private double essenceConversionRate;

    // Infusion settings
    private int baseTicksPerTier;
    private int minSELForInfusion;

    public EssenceConfig(StormcraftEssencePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load passive ability unlock levels
        unlockLevels.put(PassiveAbility.STORM_RESISTANCE, config.getInt("abilities.storm_resistance.unlock_level", 10));
        unlockLevels.put(PassiveAbility.LIGHTNING_REFLEXES, config.getInt("abilities.lightning_reflexes.unlock_level", 15));
        unlockLevels.put(PassiveAbility.STORMBORN, config.getInt("abilities.stormborn.unlock_level", 20));
        unlockLevels.put(PassiveAbility.STORMRIDER, config.getInt("abilities.stormrider.unlock_level", 100));

        // Load active ability unlock levels
        unlockLevels.put(PassiveAbility.STORM_SENSE, config.getInt("abilities.storm_sense.unlock_level", 10));
        unlockLevels.put(PassiveAbility.EYE_OF_THE_STORM, config.getInt("abilities.eye_of_the_storm.unlock_level", 25));
        unlockLevels.put(PassiveAbility.STORMCALLER, config.getInt("abilities.stormcaller.unlock_level", 40));
        unlockLevels.put(PassiveAbility.STORMCLEAR, config.getInt("abilities.stormclear.unlock_level", 50));

        // Load active ability essence costs
        essenceCosts.put(PassiveAbility.STORM_SENSE, config.getInt("abilities.storm_sense.essence_cost", 1));
        essenceCosts.put(PassiveAbility.EYE_OF_THE_STORM, config.getInt("abilities.eye_of_the_storm.essence_cost", 500));
        essenceCosts.put(PassiveAbility.STORMCALLER, config.getInt("abilities.stormcaller.essence_cost", 100));
        essenceCosts.put(PassiveAbility.STORMCLEAR, config.getInt("abilities.stormclear.essence_cost", 2000));

        // Load active ability cooldowns
        cooldownSeconds.put(PassiveAbility.STORM_SENSE, config.getInt("abilities.storm_sense.cooldown_seconds", 60));
        cooldownSeconds.put(PassiveAbility.EYE_OF_THE_STORM, config.getInt("abilities.eye_of_the_storm.cooldown_seconds", 1200));
        cooldownSeconds.put(PassiveAbility.STORMCALLER, config.getInt("abilities.stormcaller.cooldown_seconds", 30));
        cooldownSeconds.put(PassiveAbility.STORMCLEAR, config.getInt("abilities.stormclear.cooldown_seconds", 3600));

        // Load drain settings
        baseDrainPerSecond = config.getDouble("drain.base_drain_per_second", 1.0);
        selScalingEnabled = config.getBoolean("drain.sel_scaling.enabled", true);
        drainPerTenLevels = config.getDouble("drain.sel_scaling.per_10_levels", 0.1);
        multiPassiveMultiplier = config.getDouble("drain.multi_passive_multiplier", 1.5);
        drainIntervalTicks = config.getInt("drain.drain_interval_ticks", 20);

        // Load crafting settings
        compassEssenceCost = config.getInt("crafting.compass_essence_cost", 100);
        essenceConversionRate = config.getDouble("crafting.essence_conversion_rate", 10.0);

        // Load infusion settings
        baseTicksPerTier = config.getInt("infusion.base_ticks_per_tier", 6000);
        minSELForInfusion = config.getInt("infusion.min_sel_for_infusion", 10);
    }

    public int getUnlockLevel(PassiveAbility ability) {
        return unlockLevels.getOrDefault(ability, 999);
    }

    public double getBaseDrainPerSecond() {
        return baseDrainPerSecond;
    }

    public boolean isSelScalingEnabled() {
        return selScalingEnabled;
    }

    public double getDrainPerTenLevels() {
        return drainPerTenLevels;
    }

    public double getMultiPassiveMultiplier() {
        return multiPassiveMultiplier;
    }

    public int getDrainIntervalTicks() {
        return drainIntervalTicks;
    }

    // Storm Resistance ability stats
    public double getStormResistanceBaseReduction() {
        return config.getDouble("abilities.storm_resistance.base_reduction_percent", 5.0);
    }

    public double getStormResistancePerLevelBonus() {
        return config.getDouble("abilities.storm_resistance.per_level_bonus", 0.5);
    }

    public double getStormResistanceMaxReduction() {
        return config.getDouble("abilities.storm_resistance.max_reduction_percent", 50.0);
    }

    // Lightning Reflexes ability stats
    public int getLightningReflexesBaseSpeed() {
        return config.getInt("abilities.lightning_reflexes.base_speed_amplifier", 0);
    }

    public int getLightningReflexesPer10LevelsBonus() {
        return config.getInt("abilities.lightning_reflexes.per_10_levels_bonus", 1);
    }

    public int getLightningReflexesMaxSpeed() {
        return config.getInt("abilities.lightning_reflexes.max_speed_amplifier", 3);
    }

    // Stormborn ability stats
    public double getStormbornBaseRegen() {
        return config.getDouble("abilities.stormborn.base_regen_amount", 0.5);
    }

    public double getStormbornPer5LevelsBonus() {
        return config.getDouble("abilities.stormborn.per_5_levels_bonus", 0.1);
    }

    public double getStormbornMaxRegen() {
        return config.getDouble("abilities.stormborn.max_regen_amount", 2.0);
    }

    // Messages
    public String getMessage(String key) {
        return config.getString("messages." + key, "");
    }

    // Crafting getters
    public int getCompassEssenceCost() {
        return compassEssenceCost;
    }

    public double getEssenceConversionRate() {
        return essenceConversionRate;
    }

    // Infusion getters
    public int getBaseTicksPerTier() {
        return baseTicksPerTier;
    }

    public int getMinSELForInfusion() {
        return minSELForInfusion;
    }

    // Active ability getters
    public int getEssenceCost(PassiveAbility ability) {
        return essenceCosts.getOrDefault(ability, 0);
    }

    public int getCooldownSeconds(PassiveAbility ability) {
        return cooldownSeconds.getOrDefault(ability, 0);
    }

    // Storm Sense settings
    public int getStormSenseParticleDuration() {
        return config.getInt("abilities.storm_sense.particle_duration_ticks", 600);
    }

    public int getStormSenseParticleInterval() {
        return config.getInt("abilities.storm_sense.particle_interval_ticks", 5);
    }

    // Eye of the Storm settings
    public int getEyeOfStormDuration() {
        return config.getInt("abilities.eye_of_the_storm.duration_seconds", 600);
    }

    public double getEyeOfStormRadius() {
        return config.getDouble("abilities.eye_of_the_storm.bubble_radius", 10.0);
    }

    // Stormcaller settings
    public double getStormcallerDamage() {
        return config.getDouble("abilities.stormcaller.damage", 5.0);
    }

    public double getStormcallerRange() {
        return config.getDouble("abilities.stormcaller.range", 100.0);
    }

    // Stormclear settings
    public double getStormclearPushDistance() {
        return config.getDouble("abilities.stormclear.push_distance", 1000.0);
    }

    public int getStormclearSpeedAmplifier() {
        return config.getInt("abilities.stormclear.speed_amplifier", 3);
    }

    public int getStormclearSpeedDuration() {
        return config.getInt("abilities.stormclear.speed_duration_seconds", 300);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
