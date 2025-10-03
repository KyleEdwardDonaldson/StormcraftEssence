package dev.ked.stormcraft.essence.listener;

import dev.ked.stormcraft.api.events.StormcraftEssenceAwardEvent;
import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import dev.ked.stormcraft.essence.model.PlayerEssenceData;
import dev.ked.stormcraft.essence.persistence.PlayerDataManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for essence award events from Stormcraft and handles:
 * 1. Vault economy deposits
 * 2. Player essence tracking for SEL progression
 */
public class EssenceAwardListener implements Listener {
    private final StormcraftEssencePlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final Economy economy;

    public EssenceAwardListener(StormcraftEssencePlugin plugin, PlayerDataManager playerDataManager, Economy economy) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.economy = economy;
    }

    /**
     * Handles essence deposits to Vault and tracks SEL progression.
     * Priority is set to LOWEST to run first, allowing other plugins to modify the event.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEssenceAward(StormcraftEssenceAwardEvent event) {
        Player player = event.getPlayer();
        double essence = event.getEssenceAmount();

        // Award essence via Vault economy
        if (economy != null) {
            try {
                economy.depositPlayer(player, essence);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to deposit essence for " + player.getName() + ": " + e.getMessage());
                return; // Don't track essence if deposit failed
            }
        }

        // Track essence for SEL progression
        PlayerEssenceData data = playerDataManager.getPlayerData(player);

        // Block essence gain if player has active passives
        if (data.hasActivePassives()) {
            return; // Cannot accrue essence while passives are draining it
        }

        // Add to lifetime total
        data.addStormEssence(essence);
    }

    /**
     * Save player data on logout
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataManager.unloadPlayerData(event.getPlayer().getUniqueId());
    }
}
