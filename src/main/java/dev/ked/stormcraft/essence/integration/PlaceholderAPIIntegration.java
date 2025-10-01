package dev.ked.stormcraft.essence.integration;

import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import dev.ked.stormcraft.essence.model.PassiveAbility;
import dev.ked.stormcraft.essence.model.PlayerEssenceData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * PlaceholderAPI expansion for Stormcraft-Essence.
 * Provides placeholders for SEL display in chat prefixes and other plugins.
 */
public class PlaceholderAPIIntegration extends PlaceholderExpansion {
    private final StormcraftEssencePlugin plugin;

    public PlaceholderAPIIntegration(StormcraftEssencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "stormessence";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ked";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Keep expansion loaded even if plugin reloads
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        PlayerEssenceData data = plugin.getPlayerDataManager().getPlayerData(player);

        // %stormessence_sel% - Storm Exposure Level (for chat prefix)
        if (identifier.equals("sel")) {
            return String.valueOf(data.getStormExposureLevel());
        }

        // %stormessence_sel_formatted% - Formatted SEL with brackets
        if (identifier.equals("sel_formatted")) {
            int sel = data.getStormExposureLevel();
            return sel > 0 ? "[SEL " + sel + "]" : "";
        }

        // %stormessence_total% - Total storm essence earned
        if (identifier.equals("total")) {
            return String.format("%.2f", data.getTotalStormEssence());
        }

        // %stormessence_balance% - Current essence balance (from Vault)
        if (identifier.equals("balance")) {
            return String.format("%.2f", plugin.getEconomy().getBalance(player));
        }

        // %stormessence_drain_rate% - Current essence drain rate per second
        if (identifier.equals("drain_rate")) {
            return String.format("%.2f", plugin.getAbilityManager().calculateDrainRate(data));
        }

        // %stormessence_active_count% - Number of active passives
        if (identifier.equals("active_count")) {
            return String.valueOf(data.getActivePassives().size());
        }

        // %stormessence_active_list% - Comma-separated list of active abilities
        if (identifier.equals("active_list")) {
            return data.hasActivePassives()
                    ? data.getActivePassives().stream()
                        .map(PassiveAbility::getDisplayName)
                        .collect(Collectors.joining(", "))
                    : "None";
        }

        // %stormessence_has_resistance% - true/false if Storm Resistance is active
        if (identifier.equals("has_resistance")) {
            return String.valueOf(data.getActivePassives().contains(PassiveAbility.STORM_RESISTANCE));
        }

        // %stormessence_has_reflexes% - true/false if Lightning Reflexes is active
        if (identifier.equals("has_reflexes")) {
            return String.valueOf(data.getActivePassives().contains(PassiveAbility.LIGHTNING_REFLEXES));
        }

        // %stormessence_has_stormborn% - true/false if Stormborn is active
        if (identifier.equals("has_stormborn")) {
            return String.valueOf(data.getActivePassives().contains(PassiveAbility.STORMBORN));
        }

        return null; // Unknown placeholder
    }
}
