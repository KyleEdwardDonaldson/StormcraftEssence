package dev.ked.stormcraft.essence.infusion;

import dev.ked.stormcraft.api.events.StormcraftStormTickEvent;
import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles storm damage reduction for players wearing infused armor.
 */
public class InfusedArmorListener implements Listener {
    private final StormcraftEssencePlugin plugin;
    private static final String INFUSION_TIER_KEY = "stormcraft_infusion_tier";

    public InfusedArmorListener(StormcraftEssencePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only handle custom storm damage
        if (event.getCause() != EntityDamageEvent.DamageCause.CUSTOM) {
            return;
        }

        // Calculate total protection from all armor pieces
        int totalProtection = calculateTotalProtection(player);

        if (totalProtection > 0) {
            // Reduce damage by protection percentage
            double originalDamage = event.getDamage();
            double reduction = originalDamage * (totalProtection / 100.0);
            event.setDamage(Math.max(0, originalDamage - reduction));
        }
    }

    /**
     * Calculates total storm protection from all equipped infused armor.
     * Takes the average protection across all pieces.
     */
    private int calculateTotalProtection(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        int totalProtection = 0;
        int armorPieces = 0;

        for (ItemStack piece : armor) {
            if (piece != null) {
                int tier = getInfusionTier(piece);
                if (tier > 0) {
                    totalProtection += getProtectionPercent(tier);
                    armorPieces++;
                }
            }
        }

        // Average protection across equipped pieces
        if (armorPieces > 0) {
            return totalProtection / armorPieces;
        }

        return 0;
    }

    /**
     * Gets the infusion tier of an item.
     */
    private int getInfusionTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        NamespacedKey key = new NamespacedKey(plugin, INFUSION_TIER_KEY);
        Integer tier = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return tier != null ? tier : 0;
    }

    /**
     * Gets the storm protection percentage for a tier.
     */
    public static int getProtectionPercent(int tier) {
        return switch (tier) {
            case 1 -> 20;  // Weak
            case 2 -> 40;  // Moderate
            case 3 -> 60;  // Strong
            case 4 -> 80;  // Very Strong
            case 5 -> 95;  // Near Immunity
            default -> 0;
        };
    }
}
