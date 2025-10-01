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

    // Drain settings
    private double baseDrainPerSecond;
    private boolean selScalingEnabled;
    private double drainPerTenLevels;
    private double multiPassiveMultiplier;
    private int drainIntervalTicks;

    public EssenceConfig(StormcraftEssencePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load ability unlock levels
        unlockLevels.put(PassiveAbility.STORM_RESISTANCE, config.getInt("abilities.storm_resistance.unlock_level", 10));
        unlockLevels.put(PassiveAbility.LIGHTNING_REFLEXES, config.getInt("abilities.lightning_reflexes.unlock_level", 15));
        unlockLevels.put(PassiveAbility.STORMBORN, config.getInt("abilities.stormborn.unlock_level", 20));

        // Load drain settings
        baseDrainPerSecond = config.getDouble("drain.base_drain_per_second", 1.0);
        selScalingEnabled = config.getBoolean("drain.sel_scaling.enabled", true);
        drainPerTenLevels = config.getDouble("drain.sel_scaling.per_10_levels", 0.1);
        multiPassiveMultiplier = config.getDouble("drain.multi_passive_multiplier", 1.5);
        drainIntervalTicks = config.getInt("drain.drain_interval_ticks", 20);
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

    public FileConfiguration getConfig() {
        return config;
    }
}
