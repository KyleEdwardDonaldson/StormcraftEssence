package dev.ked.stormcraft.essence.listener;

import dev.ked.stormcraft.api.events.StormcraftEssenceAwardEvent;
import dev.ked.stormcraft.essence.model.PlayerEssenceData;
import dev.ked.stormcraft.essence.persistence.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for essence award events to track Storm Exposure Level (SEL).
 */
public class EssenceAwardListener implements Listener {
    private final PlayerDataManager playerDataManager;

    public EssenceAwardListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    /**
     * Tracks essence earned from storm exposure for SEL progression.
     * Note: Only counts essence from storms, not from trading/other sources.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEssenceAward(StormcraftEssenceAwardEvent event) {
        PlayerEssenceData data = playerDataManager.getPlayerData(event.getPlayer());

        // Block essence gain if player has active passives
        if (data.hasActivePassives()) {
            return; // Cannot accrue essence while passives are draining it
        }

        // Add to lifetime total
        data.addStormEssence(event.getEssenceAmount());
    }

    /**
     * Save player data on logout
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataManager.unloadPlayerData(event.getPlayer().getUniqueId());
    }
}
