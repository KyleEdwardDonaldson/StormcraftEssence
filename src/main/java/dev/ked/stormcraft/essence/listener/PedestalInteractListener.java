package dev.ked.stormcraft.essence.listener;

import dev.ked.stormcraft.essence.infusion.InfusionPedestalManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles player interactions with infusion pedestals.
 */
public class PedestalInteractListener implements Listener {
    private final InfusionPedestalManager pedestalManager;

    public PedestalInteractListener(InfusionPedestalManager pedestalManager) {
        this.pedestalManager = pedestalManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null || block.getType() != Material.ENCHANTING_TABLE) {
            return;
        }

        // Check if this is a pedestal
        if (pedestalManager.getPedestalAt(block.getLocation()) == null) {
            return;
        }

        event.setCancelled(true);

        // Right-click to add item
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() == Material.AIR) {
                player.sendMessage(Component.text("Hold an item to infuse it!", NamedTextColor.YELLOW));
                return;
            }

            pedestalManager.addItemToPedestal(block.getLocation(), player, item);
        }

        // Sneak + Left-click to retrieve items
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.isSneaking()) {
            pedestalManager.retrieveItems(block.getLocation(), player);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.ENCHANTING_TABLE) {
            // Check if this is a pedestal
            if (pedestalManager.getPedestalAt(block.getLocation()) != null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("Use Sneak + Left-click to retrieve items first!", NamedTextColor.YELLOW));
            }
        }
    }
}
