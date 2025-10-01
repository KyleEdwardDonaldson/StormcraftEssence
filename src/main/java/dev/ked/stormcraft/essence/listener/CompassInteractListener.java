package dev.ked.stormcraft.essence.listener;

import dev.ked.stormcraft.essence.ability.ActiveAbilityManager;
import dev.ked.stormcraft.essence.model.PassiveAbility;
import dev.ked.stormcraft.essence.model.PlayerEssenceData;
import dev.ked.stormcraft.essence.persistence.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for compass right-clicks to trigger active abilities.
 */
public class CompassInteractListener implements Listener {
    private final ActiveAbilityManager abilityManager;
    private final PlayerDataManager playerDataManager;

    public CompassInteractListener(ActiveAbilityManager abilityManager, PlayerDataManager playerDataManager) {
        this.abilityManager = abilityManager;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player right-clicked with compass
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        // Cancel vanilla compass behavior
        event.setCancelled(true);

        PlayerEssenceData data = playerDataManager.getPlayerData(player);

        // SHIFT + RIGHT-CLICK = Cycle through abilities
        if (player.isSneaking()) {
            cycleAbility(player, data);
            return;
        }

        // RIGHT-CLICK = Use selected ability
        PassiveAbility selected = data.getSelectedActiveAbility();

        if (selected == null || !data.getActivePassives().contains(selected)) {
            // Auto-select first available ability
            selected = getFirstUnlockedActiveAbility(data);
            if (selected != null) {
                data.setSelectedActiveAbility(selected);
                playerDataManager.savePlayerData(player.getUniqueId());
            }
        }

        if (selected != null && data.getActivePassives().contains(selected)) {
            abilityManager.useAbility(player, selected);
        } else {
            player.sendMessage(Component.text("You have no active compass abilities enabled!")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text("\nUse /essence to enable abilities", NamedTextColor.YELLOW)));
        }
    }

    /**
     * Cycles to the next unlocked active ability
     */
    private void cycleAbility(Player player, PlayerEssenceData data) {
        // Get all active abilities player has enabled
        PassiveAbility[] allActiveAbilities = {
            PassiveAbility.STORM_SENSE,      // SEL 10
            PassiveAbility.EYE_OF_THE_STORM, // SEL 25
            PassiveAbility.STORMCALLER,      // SEL 40
            PassiveAbility.STORMCLEAR,       // SEL 50
            PassiveAbility.STORMRIDER        // SEL 100
        };

        java.util.List<PassiveAbility> unlockedActive = new java.util.ArrayList<>();
        for (PassiveAbility ability : allActiveAbilities) {
            if (ability.isActive() && data.getActivePassives().contains(ability)) {
                unlockedActive.add(ability);
            }
        }

        if (unlockedActive.isEmpty()) {
            player.sendMessage(Component.text("No active abilities enabled!").color(NamedTextColor.RED));
            return;
        }

        // Find current index
        PassiveAbility current = data.getSelectedActiveAbility();
        int currentIndex = unlockedActive.indexOf(current);

        // Cycle to next (or first if none selected)
        int nextIndex = (currentIndex + 1) % unlockedActive.size();
        PassiveAbility next = unlockedActive.get(nextIndex);

        data.setSelectedActiveAbility(next);
        playerDataManager.savePlayerData(player.getUniqueId());

        player.sendMessage(Component.text("⚡ Selected: " + next.getDisplayName())
                .color(NamedTextColor.GOLD));
    }

    /**
     * Gets the first unlocked active ability
     */
    private PassiveAbility getFirstUnlockedActiveAbility(PlayerEssenceData data) {
        PassiveAbility[] allActiveAbilities = {
            PassiveAbility.STORM_SENSE,
            PassiveAbility.EYE_OF_THE_STORM,
            PassiveAbility.STORMCALLER,
            PassiveAbility.STORMCLEAR,
            PassiveAbility.STORMRIDER
        };

        for (PassiveAbility ability : allActiveAbilities) {
            if (ability.isActive() && data.getActivePassives().contains(ability)) {
                return ability;
            }
        }

        return null;
    }
}
