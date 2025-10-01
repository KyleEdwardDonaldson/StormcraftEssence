package dev.ked.stormcraft.essence.ability;

import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import dev.ked.stormcraft.essence.config.EssenceConfig;
import dev.ked.stormcraft.essence.model.PassiveAbility;
import dev.ked.stormcraft.essence.model.PlayerEssenceData;
import dev.ked.stormcraft.essence.persistence.PlayerDataManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages passive ability activation and essence drain mechanics.
 */
public class AbilityManager {
    private final StormcraftEssencePlugin plugin;
    private final EssenceConfig config;
    private final PlayerDataManager playerDataManager;
    private final Economy economy;
    private BukkitTask drainTask;

    public AbilityManager(StormcraftEssencePlugin plugin, EssenceConfig config,
                         PlayerDataManager playerDataManager, Economy economy) {
        this.plugin = plugin;
        this.config = config;
        this.playerDataManager = playerDataManager;
        this.economy = economy;
    }

    /**
     * Starts the essence drain task that runs periodically
     */
    public void startDrainTask() {
        int interval = config.getDrainIntervalTicks();

        drainTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerEssenceData data = playerDataManager.getPlayerData(player);

                if (data.hasActivePassives()) {
                    drainEssence(player, data);
                }
            }
        }, interval, interval);
    }

    /**
     * Stops the essence drain task
     */
    public void stopDrainTask() {
        if (drainTask != null) {
            drainTask.cancel();
        }
    }

    /**
     * Calculates the essence drain rate per second for a player
     */
    public double calculateDrainRate(PlayerEssenceData data) {
        int activeCount = data.getActivePassives().size();
        if (activeCount == 0) {
            return 0.0;
        }

        double baseDrain = config.getBaseDrainPerSecond();

        // SEL scaling (higher level = more expensive to maintain)
        double selMultiplier = 1.0;
        if (config.isSelScalingEnabled()) {
            int sel = data.getStormExposureLevel();
            selMultiplier += (sel / 10.0) * config.getDrainPerTenLevels();
        }

        // Multiple passive multiplier (stacks multiplicatively)
        double passiveMultiplier = Math.pow(config.getMultiPassiveMultiplier(), activeCount - 1);

        return baseDrain * activeCount * selMultiplier * passiveMultiplier;
    }

    /**
     * Drains essence from a player based on their active passives
     */
    private void drainEssence(Player player, PlayerEssenceData data) {
        double drainPerSecond = calculateDrainRate(data);
        if (drainPerSecond <= 0) {
            return;
        }

        // Calculate drain for this interval
        int intervalTicks = config.getDrainIntervalTicks();
        double drainAmount = drainPerSecond * (intervalTicks / 20.0);

        // Check if player has enough essence
        double currentBalance = economy.getBalance(player);
        if (currentBalance < drainAmount) {
            // Insufficient essence - disable all passives
            disableAllPassives(player, data);
            player.sendMessage(config.getMessage("prefix") +
                    config.getMessage("insufficient_essence")
                            .replace("{ability}", "Passive abilities")
                            .replace("{rate}", String.format("%.2f", drainPerSecond)));
            return;
        }

        // Withdraw essence
        economy.withdrawPlayer(player, drainAmount);
    }

    /**
     * Disables all passive abilities for a player
     */
    private void disableAllPassives(Player player, PlayerEssenceData data) {
        for (PassiveAbility ability : PassiveAbility.values()) {
            if (data.getActivePassives().contains(ability)) {
                data.disablePassive(ability);
            }
        }
        playerDataManager.savePlayerData(player.getUniqueId());
    }

    /**
     * Checks if a player can enable a passive ability
     */
    public boolean canEnablePassive(Player player, PassiveAbility ability) {
        PlayerEssenceData data = playerDataManager.getPlayerData(player);
        int sel = data.getStormExposureLevel();
        int required = config.getUnlockLevel(ability);

        return sel >= required;
    }

    /**
     * Toggles a passive ability for a player
     */
    public boolean togglePassive(Player player, PassiveAbility ability) {
        PlayerEssenceData data = playerDataManager.getPlayerData(player);

        // Check unlock requirement
        if (!canEnablePassive(player, ability)) {
            int required = config.getUnlockLevel(ability);
            int current = data.getStormExposureLevel();
            player.sendMessage(config.getMessage("prefix") +
                    config.getMessage("passive_locked")
                            .replace("{ability}", ability.getDisplayName())
                            .replace("{required}", String.valueOf(required))
                            .replace("{current}", String.valueOf(current)));
            return false;
        }

        // Toggle the ability
        boolean wasActive = data.getActivePassives().contains(ability);
        data.togglePassive(ability);
        playerDataManager.savePlayerData(player.getUniqueId());

        // Calculate new drain rate
        double drainRate = calculateDrainRate(data);

        // Send message
        if (wasActive) {
            player.sendMessage(config.getMessage("prefix") +
                    config.getMessage("passive_disabled")
                            .replace("{ability}", ability.getDisplayName()));
        } else {
            player.sendMessage(config.getMessage("prefix") +
                    config.getMessage("passive_enabled")
                            .replace("{ability}", ability.getDisplayName())
                            .replace("{rate}", String.format("%.2f", drainRate)));
        }

        return true;
    }

    /**
     * Calculates storm damage reduction % for a player
     */
    public double getStormResistanceReduction(PlayerEssenceData data) {
        if (!data.getActivePassives().contains(PassiveAbility.STORM_RESISTANCE)) {
            return 0.0;
        }

        int sel = data.getStormExposureLevel();
        int unlockLevel = config.getUnlockLevel(PassiveAbility.STORM_RESISTANCE);

        double baseReduction = config.getStormResistanceBaseReduction();
        double perLevelBonus = config.getStormResistancePerLevelBonus();
        double maxReduction = config.getStormResistanceMaxReduction();

        int levelsAboveUnlock = Math.max(0, sel - unlockLevel);
        double reduction = baseReduction + (levelsAboveUnlock * perLevelBonus);

        return Math.min(reduction, maxReduction);
    }

    /**
     * Calculates speed amplifier for a player during storms
     */
    public int getLightningReflexesSpeed(PlayerEssenceData data) {
        if (!data.getActivePassives().contains(PassiveAbility.LIGHTNING_REFLEXES)) {
            return -1; // No speed boost
        }

        int sel = data.getStormExposureLevel();
        int unlockLevel = config.getUnlockLevel(PassiveAbility.LIGHTNING_REFLEXES);

        int baseSpeed = config.getLightningReflexesBaseSpeed();
        int per10LevelsBonus = config.getLightningReflexesPer10LevelsBonus();
        int maxSpeed = config.getLightningReflexesMaxSpeed();

        int levelsAboveUnlock = Math.max(0, sel - unlockLevel);
        int bonusLevels = (levelsAboveUnlock / 10) * per10LevelsBonus;
        int totalSpeed = baseSpeed + bonusLevels;

        return Math.min(totalSpeed, maxSpeed);
    }

    /**
     * Calculates health regen amount for a player when exposed to storms
     */
    public double getStormbornRegen(PlayerEssenceData data) {
        if (!data.getActivePassives().contains(PassiveAbility.STORMBORN)) {
            return 0.0;
        }

        int sel = data.getStormExposureLevel();
        int unlockLevel = config.getUnlockLevel(PassiveAbility.STORMBORN);

        double baseRegen = config.getStormbornBaseRegen();
        double per5LevelsBonus = config.getStormbornPer5LevelsBonus();
        double maxRegen = config.getStormbornMaxRegen();

        int levelsAboveUnlock = Math.max(0, sel - unlockLevel);
        int bonusInstances = levelsAboveUnlock / 5;
        double totalRegen = baseRegen + (bonusInstances * per5LevelsBonus);

        return Math.min(totalRegen, maxRegen);
    }
}
