package dev.ked.stormcraft.essence.model;

/**
 * Enum of available passive and active storm-fused abilities.
 */
public enum PassiveAbility {
    /**
     * Reduces storm damage taken by a percentage based on SEL (SEL 5)
     */
    STORM_RESISTANCE("Storm Resistance", "Reduces storm damage taken", true),

    /**
     * Grants speed boost during storms based on SEL (SEL 15)
     */
    LIGHTNING_REFLEXES("Lightning Reflexes", "Increased movement speed during storms", true),

    /**
     * Grants health regeneration when exposed to storms based on SEL (SEL 30)
     */
    STORMBORN("Stormborn", "Regenerate health when exposed to storms", true),

    /**
     * Active: Right-click compass to reveal nearby storms with particle trails (SEL 10)
     */
    STORM_SENSE("Storm Sense", "Right-click compass to see path to nearest storm edge", false),

    /**
     * Active: Right-click compass to create 10-minute immunity bubble (SEL 25)
     */
    EYE_OF_THE_STORM("Eye of the Storm", "Right-click compass for temporary storm immunity", false),

    /**
     * Active: Right-click compass to summon lightning strike (SEL 40)
     */
    STORMCALLER("Stormcaller", "Right-click compass to summon lightning", false),

    /**
     * Active: Right-click compass to push storms away (SEL 50)
     */
    STORMCLEAR("Stormclear", "Right-click compass to push storms away", false),

    /**
     * Active: Right-click compass to toggle flight in storms (SEL 100)
     */
    STORMRIDER("Stormrider", "Right-click compass to toggle storm flight mode", false);

    private final String displayName;
    private final String description;
    private final boolean isPassive;

    PassiveAbility(String displayName, String description, boolean isPassive) {
        this.displayName = displayName;
        this.description = description;
        this.isPassive = isPassive;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPassive() {
        return isPassive;
    }

    public boolean isActive() {
        return !isPassive;
    }
}
