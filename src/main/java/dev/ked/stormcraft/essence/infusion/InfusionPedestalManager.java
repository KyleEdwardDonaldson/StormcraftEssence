package dev.ked.stormcraft.essence.infusion;

import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import dev.ked.stormcraft.essence.config.EssenceConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages infusion pedestals placed in the world.
 */
public class InfusionPedestalManager {
    private final StormcraftEssencePlugin plugin;
    private final EssenceConfig config;
    private final Map<String, InfusionPedestal> pedestals;
    private static final String PEDESTAL_KEY = "stormcraft_pedestal";
    private static final String INFUSION_TIER_KEY = "stormcraft_infusion_tier";

    public InfusionPedestalManager(StormcraftEssencePlugin plugin, EssenceConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.pedestals = new ConcurrentHashMap<>();
    }

    /**
     * Places an infusion pedestal at the target location.
     */
    public boolean placePedestal(Player player, Location targetLocation) {
        // Check if block is air
        Block block = targetLocation.getBlock();
        if (block.getType() != Material.AIR) {
            player.sendMessage(Component.text("Cannot place pedestal here - block is not empty!", NamedTextColor.RED));
            return false;
        }

        // Check if pedestal already exists nearby
        String key = locationToKey(targetLocation);
        if (pedestals.containsKey(key)) {
            player.sendMessage(Component.text("A pedestal already exists here!", NamedTextColor.RED));
            return false;
        }

        // Place the pedestal block (use enchantment table for visual effect)
        block.setType(Material.ENCHANTING_TABLE);

        // Create armor stand to hold items visually
        Location armorStandLoc = targetLocation.clone().add(0.5, 1.0, 0.5);
        ArmorStand armorStand = (ArmorStand) targetLocation.getWorld().spawnEntity(armorStandLoc, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setCustomNameVisible(true);
        armorStand.customName(Component.text("⚡ Infusion Pedestal ⚡", NamedTextColor.AQUA, TextDecoration.BOLD));
        armorStand.getPersistentDataContainer().set(
            new NamespacedKey(plugin, PEDESTAL_KEY),
            PersistentDataType.STRING,
            player.getUniqueId().toString()
        );

        // Create pedestal data
        int sel = plugin.getPlayerDataManager().getPlayerData(player).getStormExposureLevel();
        InfusionPedestal pedestal = new InfusionPedestal(targetLocation, player.getUniqueId(), sel);
        pedestals.put(key, pedestal);

        // Visual effects
        targetLocation.getWorld().spawnParticle(Particle.ENCHANT, targetLocation.clone().add(0.5, 1, 0.5), 50, 0.5, 0.5, 0.5, 0.1);
        targetLocation.getWorld().playSound(targetLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);

        player.sendMessage(Component.text("Infusion pedestal placed! Right-click with items to infuse them.", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Adds an item to a pedestal for infusion.
     */
    public boolean addItemToPedestal(Location location, Player player, ItemStack item) {
        String key = locationToKey(location);
        InfusionPedestal pedestal = pedestals.get(key);

        if (pedestal == null) {
            return false;
        }

        if (!pedestal.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("This is not your pedestal!", NamedTextColor.RED));
            return false;
        }

        if (!isInfusable(item)) {
            player.sendMessage(Component.text("This item cannot be infused!", NamedTextColor.RED));
            return false;
        }

        // Add item to pedestal
        pedestal.addItem(item);

        // Update armor stand display
        updateArmorStandDisplay(location, pedestal);

        // Remove item from player inventory
        item.setAmount(0);

        player.sendMessage(Component.text("Item added to pedestal for infusion!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);

        return true;
    }

    /**
     * Retrieves infused items from a pedestal.
     */
    public boolean retrieveItems(Location location, Player player) {
        String key = locationToKey(location);
        InfusionPedestal pedestal = pedestals.get(key);

        if (pedestal == null) {
            player.sendMessage(Component.text("No pedestal found here!", NamedTextColor.RED));
            return false;
        }

        if (!pedestal.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("This is not your pedestal!", NamedTextColor.RED));
            return false;
        }

        int tier = pedestal.getInfusionTier();
        if (tier == 0) {
            player.sendMessage(Component.text("Items have not reached minimum infusion yet!", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Time to Tier 1: " + formatTime(pedestal.getTimeToNextTier()), NamedTextColor.GRAY));
            return false;
        }

        // Apply infusion to all items
        List<ItemStack> infusedItems = new ArrayList<>();
        for (ItemStack item : pedestal.getStoredItems()) {
            ItemStack infused = item.clone();
            applyInfusion(infused, tier, player.getUniqueId());
            infusedItems.add(infused);
        }

        // Give items back to player
        for (ItemStack infused : infusedItems) {
            player.getInventory().addItem(infused);
        }

        // Remove pedestal
        removePedestal(location);

        player.sendMessage(Component.text("Retrieved " + infusedItems.size() + " infused items (Tier " + tier + ")!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
        location.getWorld().spawnParticle(Particle.ENCHANT, location.clone().add(0.5, 1, 0.5), 100, 0.5, 1, 0.5, 0.2);

        return true;
    }

    /**
     * Removes a pedestal from the world.
     */
    public void removePedestal(Location location) {
        String key = locationToKey(location);
        pedestals.remove(key);

        // Remove block
        Block block = location.getBlock();
        if (block.getType() == Material.ENCHANTING_TABLE) {
            block.setType(Material.AIR);
        }

        // Remove armor stand
        for (Entity entity : location.getWorld().getNearbyEntities(location.clone().add(0.5, 1, 0.5), 1, 1, 1)) {
            if (entity instanceof ArmorStand armorStand) {
                if (armorStand.getPersistentDataContainer().has(new NamespacedKey(plugin, PEDESTAL_KEY))) {
                    armorStand.remove();
                }
            }
        }
    }

    /**
     * Updates the visual display on the armor stand.
     */
    private void updateArmorStandDisplay(Location location, InfusionPedestal pedestal) {
        for (Entity entity : location.getWorld().getNearbyEntities(location.clone().add(0.5, 1, 0.5), 1, 1, 1)) {
            if (entity instanceof ArmorStand armorStand) {
                if (armorStand.getPersistentDataContainer().has(new NamespacedKey(plugin, PEDESTAL_KEY))) {
                    // Display first item on the armor stand's head
                    if (!pedestal.getStoredItems().isEmpty()) {
                        armorStand.getEquipment().setHelmet(pedestal.getStoredItems().get(0));
                    }

                    // Update name with progress
                    int tier = pedestal.getInfusionTier();
                    double progress = pedestal.getInfusionProgress() * 100;
                    armorStand.customName(Component.text("⚡ Infusion Pedestal ⚡", NamedTextColor.AQUA, TextDecoration.BOLD)
                        .append(Component.newline())
                        .append(Component.text("Progress: " + String.format("%.1f", progress) + "%", NamedTextColor.YELLOW))
                        .append(Component.newline())
                        .append(Component.text("Tier: " + tier, getTierColor(tier))));
                }
            }
        }
    }

    /**
     * Starts the update task to refresh pedestal displays.
     */
    public void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<String, InfusionPedestal> entry : pedestals.entrySet()) {
                    updateArmorStandDisplay(entry.getValue().getLocation(), entry.getValue());
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 10); // Update every 10 seconds
    }

    private void applyInfusion(ItemStack item, int tier, UUID ownerUUID) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey tierKey = new NamespacedKey(plugin, INFUSION_TIER_KEY);
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, tier);

        String tierName = getTierName(tier);
        NamedTextColor tierColor = getTierColor(tier);

        List<Component> lore = new ArrayList<>(meta.lore() != null ? meta.lore() : List.of());
        lore.add(Component.empty());
        lore.add(Component.text("⚡ Storm-Infused " + tierName, tierColor)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Protection: " + InfusedArmorListener.getProtectionPercent(tier) + "%", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
    }

    private boolean isInfusable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        Material type = item.getType();
        return type.name().endsWith("_HELMET") || type.name().endsWith("_CHESTPLATE") ||
               type.name().endsWith("_LEGGINGS") || type.name().endsWith("_BOOTS") ||
               type == Material.COMPASS || type == Material.SHIELD || type == Material.ELYTRA;
    }

    private String getTierName(int tier) {
        return switch (tier) {
            case 1 -> "Tier I";
            case 2 -> "Tier II";
            case 3 -> "Tier III";
            case 4 -> "Tier IV";
            case 5 -> "Tier V";
            default -> "Unknown";
        };
    }

    private NamedTextColor getTierColor(int tier) {
        return switch (tier) {
            case 1 -> NamedTextColor.GREEN;
            case 2 -> NamedTextColor.BLUE;
            case 3 -> NamedTextColor.LIGHT_PURPLE;
            case 4 -> NamedTextColor.GOLD;
            case 5 -> NamedTextColor.RED;
            default -> NamedTextColor.GRAY;
        };
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "Complete!";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return secs + "s";
        }
    }

    private String locationToKey(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    public InfusionPedestal getPedestalAt(Location location) {
        return pedestals.get(locationToKey(location));
    }
}
