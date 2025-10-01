package dev.ked.stormcraft.essence.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stores a player's Storm Exposure Level (SEL) and active passive abilities.
 */
public class PlayerEssenceData {
    private final UUID playerId;
    private double totalStormEssence; // Lifetime essence earned from storms (never decreases)
    private Set<PassiveAbility> activePassives;
    private PassiveAbility selectedActiveAbility; // Currently selected compass ability

    public PlayerEssenceData(UUID playerId) {
        this.playerId = playerId;
        this.totalStormEssence = 0.0;
        this.activePassives = new HashSet<>();
        this.selectedActiveAbility = null;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * @return Storm Exposure Level (SEL) - integer level based on total storm essence
     */
    public int getStormExposureLevel() {
        // Formula: SEL = floor(sqrt(totalStormEssence / 10))
        // This creates a diminishing returns progression
        // Example: 100 essence = level 3, 1000 = level 10, 10000 = level 31
        return (int) Math.floor(Math.sqrt(totalStormEssence / 10.0));
    }

    /**
     * @return Total lifetime essence earned from storm exposure
     */
    public double getTotalStormEssence() {
        return totalStormEssence;
    }

    /**
     * Adds essence to the lifetime total (from storm exposure)
     */
    public void addStormEssence(double amount) {
        this.totalStormEssence += amount;
    }

    /**
     * @return Set of currently active passive abilities
     */
    public Set<PassiveAbility> getActivePassives() {
        return activePassives;
    }

    /**
     * Toggles a passive ability on/off
     */
    public void togglePassive(PassiveAbility ability) {
        if (activePassives.contains(ability)) {
            activePassives.remove(ability);
        } else {
            activePassives.add(ability);
        }
    }

    /**
     * Enables a passive ability
     */
    public void enablePassive(PassiveAbility ability) {
        activePassives.add(ability);
    }

    /**
     * Disables a passive ability
     */
    public void disablePassive(PassiveAbility ability) {
        activePassives.remove(ability);
    }

    /**
     * @return Whether any passive abilities are currently active
     */
    public boolean hasActivePassives() {
        return !activePassives.isEmpty();
    }

    /**
     * @return Currently selected active ability for compass
     */
    public PassiveAbility getSelectedActiveAbility() {
        return selectedActiveAbility;
    }

    /**
     * Sets the selected active ability for compass
     */
    public void setSelectedActiveAbility(PassiveAbility ability) {
        this.selectedActiveAbility = ability;
    }
}
