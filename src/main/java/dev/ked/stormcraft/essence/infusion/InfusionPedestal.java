package dev.ked.stormcraft.essence.infusion;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an infusion pedestal placed in the world.
 */
public class InfusionPedestal {
    private final Location location;
    private final UUID ownerUUID;
    private final int ownerSEL;
    private final long placedTime;
    private final List<ItemStack> storedItems;

    public InfusionPedestal(Location location, UUID ownerUUID, int ownerSEL) {
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.ownerSEL = ownerSEL;
        this.placedTime = System.currentTimeMillis();
        this.storedItems = new ArrayList<>();
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public int getOwnerSEL() {
        return ownerSEL;
    }

    public long getPlacedTime() {
        return placedTime;
    }

    public List<ItemStack> getStoredItems() {
        return storedItems;
    }

    public void addItem(ItemStack item) {
        storedItems.add(item.clone());
    }

    public void removeItem(ItemStack item) {
        storedItems.remove(item);
    }

    public void clearItems() {
        storedItems.clear();
    }

    /**
     * Gets the infusion progress based on time elapsed.
     * 70% at 1 day (24 hours)
     * 100% at 2 days (48 hours)
     *
     * Only SEL 100 players can reach 100% infusion.
     * Lower SEL players cap at lower percentages.
     *
     * @return Infusion percentage (0.0 to 1.0)
     */
    public double getInfusionProgress() {
        long elapsed = System.currentTimeMillis() - placedTime;
        long oneDayMs = 24 * 60 * 60 * 1000L; // 24 hours in milliseconds

        double progress;
        if (elapsed < oneDayMs) {
            // First day: 0% to 70%
            progress = (elapsed / (double) oneDayMs) * 0.70;
        } else {
            // Second day: 70% to 100%
            long secondDayElapsed = elapsed - oneDayMs;
            double secondDayProgress = (secondDayElapsed / (double) oneDayMs) * 0.30;
            progress = 0.70 + Math.min(secondDayProgress, 0.30);
        }

        // Cap at max tier for SEL
        double maxProgress = getMaxInfusionForSEL();
        return Math.min(progress, maxProgress);
    }

    /**
     * Gets the maximum infusion percentage based on owner's SEL.
     * SEL 100 = 100% (tier 5)
     * SEL 75 = 80% (tier 4)
     * SEL 50 = 60% (tier 3)
     * SEL 25 = 40% (tier 2)
     * SEL 10 = 20% (tier 1)
     */
    private double getMaxInfusionForSEL() {
        if (ownerSEL >= 100) return 1.00; // 100% - Tier 5
        if (ownerSEL >= 75) return 0.80;  // 80% - Tier 4
        if (ownerSEL >= 50) return 0.60;  // 60% - Tier 3
        if (ownerSEL >= 25) return 0.40;  // 40% - Tier 2
        if (ownerSEL >= 10) return 0.20;  // 20% - Tier 1
        return 0.0; // No infusion possible
    }

    /**
     * Gets the infusion tier (1-5) based on current progress.
     */
    public int getInfusionTier() {
        double progress = getInfusionProgress();
        if (progress >= 0.95) return 5; // 95%+ = Tier 5
        if (progress >= 0.75) return 4; // 75%+ = Tier 4
        if (progress >= 0.55) return 3; // 55%+ = Tier 3
        if (progress >= 0.35) return 2; // 35%+ = Tier 2
        if (progress >= 0.15) return 1; // 15%+ = Tier 1
        return 0; // Not enough progress yet
    }

    /**
     * Gets time remaining until next tier (in seconds).
     */
    public long getTimeToNextTier() {
        double currentProgress = getInfusionProgress();
        double maxProgress = getMaxInfusionForSEL();

        if (currentProgress >= maxProgress) {
            return 0; // Already at max
        }

        int currentTier = getInfusionTier();
        double nextTierProgress = switch (currentTier) {
            case 0 -> 0.15;
            case 1 -> 0.35;
            case 2 -> 0.55;
            case 3 -> 0.75;
            case 4 -> 0.95;
            default -> 1.0;
        };

        nextTierProgress = Math.min(nextTierProgress, maxProgress);

        if (currentProgress >= nextTierProgress) {
            return 0;
        }

        double progressNeeded = nextTierProgress - currentProgress;
        long oneDayMs = 24 * 60 * 60 * 1000L;

        // Calculate time needed based on which phase we're in
        long timeNeeded;
        if (currentProgress < 0.70) {
            // First day phase (0% to 70%)
            timeNeeded = (long) ((progressNeeded / 0.70) * oneDayMs);
        } else {
            // Second day phase (70% to 100%)
            timeNeeded = (long) ((progressNeeded / 0.30) * oneDayMs);
        }

        return timeNeeded / 1000; // Convert to seconds
    }
}
