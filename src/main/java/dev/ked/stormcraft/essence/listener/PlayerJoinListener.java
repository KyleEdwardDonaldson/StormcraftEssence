package dev.ked.stormcraft.essence.listener;

import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles player join events, including first-join compass distribution
 */
public class PlayerJoinListener implements Listener {

    private final StormcraftEssencePlugin plugin;

    public PlayerJoinListener(StormcraftEssencePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if this is the player's first join
        if (!player.hasPlayedBefore()) {
            giveStormCompass(player);
        }
    }

    /**
     * Give a Storm Compass to a new player
     */
    private void giveStormCompass(Player player) {
        // Check if compass distribution is enabled in config
        if (!plugin.getConfig().getBoolean("compass.giveOnFirstJoin", true)) {
            return;
        }

        // Create compass item
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();

        if (meta != null) {
            // Get compass name from config or use default
            String compassName = plugin.getConfig().getString("compass.compassName", "Storm Compass");
            meta.displayName(Component.text(compassName)
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));

            // Get lore from config or use default
            List<String> loreStrings = plugin.getConfig().getStringList("compass.compassLore");
            if (loreStrings.isEmpty()) {
                loreStrings = List.of(
                    "Right-click to use abilities",
                    "Sneak + Right-click to cycle abilities"
                );
            }

            List<Component> lore = new ArrayList<>();
            for (String line : loreStrings) {
                lore.add(Component.text(line)
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);

            compass.setItemMeta(meta);
        }

        // Try to add to inventory, drop if full
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(compass);
        } else {
            player.getWorld().dropItem(player.getLocation(), compass);
        }

        // Welcome message
        String welcomeMessage = plugin.getConfig().getString("compass.welcomeMessage",
                "You've been given a Storm Compass! Use it to harness storm abilities.");
        player.sendMessage(Component.text(welcomeMessage)
                .color(NamedTextColor.GOLD));
    }
}
