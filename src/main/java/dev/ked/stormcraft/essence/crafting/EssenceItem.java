package dev.ked.stormcraft.essence.crafting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Represents a physical Storm Essence item that can be crafted and used for infusion.
 */
public class EssenceItem {
    private static final String ESSENCE_KEY = "stormcraft_essence";

    /**
     * Creates a physical Storm Essence item.
     *
     * @param plugin The plugin instance
     * @param amount How much essence this item represents
     * @return ItemStack of storm essence
     */
    public static ItemStack create(Plugin plugin, double amount) {
        ItemStack item = new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta meta = item.getItemMeta();

        // Set display name
        meta.displayName(Component.text("Storm Essence", NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false));

        // Set lore
        meta.lore(List.of(
            Component.text("Crystallized storm energy", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Value: " + String.format("%.1f", amount) + " essence", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Use to craft compasses or", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("infuse items with storm power", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
        ));

        // Store essence amount in PDC
        NamespacedKey key = new NamespacedKey(plugin, ESSENCE_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, amount);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if an ItemStack is a Storm Essence item.
     */
    public static boolean isEssenceItem(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        NamespacedKey key = new NamespacedKey(plugin, ESSENCE_KEY);
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.DOUBLE);
    }

    /**
     * Gets the essence value of an item.
     *
     * @return Essence value, or 0 if not an essence item
     */
    public static double getEssenceValue(Plugin plugin, ItemStack item) {
        if (!isEssenceItem(plugin, item)) {
            return 0.0;
        }

        NamespacedKey key = new NamespacedKey(plugin, ESSENCE_KEY);
        Double value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        return value != null ? value : 0.0;
    }
}
