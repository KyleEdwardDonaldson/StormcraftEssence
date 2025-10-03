package dev.ked.stormcraft.essence.crafting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Handles custom compass crafting using Storm Essence items.
 */
public class CompassCraftingListener implements Listener {
    private final Plugin plugin;
    private final Economy economy;
    private final int essenceRequired;

    public CompassCraftingListener(Plugin plugin, Economy economy, int essenceRequired) {
        this.plugin = plugin;
        this.economy = economy;
        this.essenceRequired = essenceRequired;
    }

    @EventHandler
    public void onCraft(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getInventory().getType() != InventoryType.CRAFTING &&
            event.getInventory().getType() != InventoryType.WORKBENCH) {
            return;
        }

        if (!(event.getInventory() instanceof CraftingInventory craftingInventory)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() != Material.COMPASS) {
            return;
        }

        // Check if the result slot was clicked
        if (event.getSlot() != 0) {
            return;
        }

        // Check crafting ingredients
        ItemStack[] matrix = craftingInventory.getMatrix();
        double totalEssence = 0.0;
        int essenceItemCount = 0;

        for (ItemStack item : matrix) {
            if (item != null && EssenceItem.isEssenceItem(plugin, item)) {
                totalEssence += EssenceItem.getEssenceValue(plugin, item) * item.getAmount();
                essenceItemCount++;
            } else if (item != null && item.getType() != Material.AIR) {
                // Non-essence items in crafting grid - this is vanilla crafting
                return;
            }
        }

        // If no essence items, allow vanilla crafting
        if (essenceItemCount == 0) {
            return;
        }

        // Check if enough essence
        if (totalEssence < essenceRequired) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Insufficient essence! Need " + essenceRequired + ", have " +
                String.format("%.1f", totalEssence), NamedTextColor.RED));
            return;
        }

        // Allow crafting - the essence items will be consumed
        player.sendMessage(Component.text("Crafted Storm Compass using " +
            String.format("%.1f", totalEssence) + " essence!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
    }
}
