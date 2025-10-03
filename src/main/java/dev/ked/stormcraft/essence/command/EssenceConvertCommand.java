package dev.ked.stormcraft.essence.command;

import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import dev.ked.stormcraft.essence.crafting.EssenceItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to convert Vault economy essence into physical essence items.
 */
public class EssenceConvertCommand implements CommandExecutor {
    private final StormcraftEssencePlugin plugin;
    private final Economy economy;
    private final double conversionRate; // How much vault essence = 1 item essence

    public EssenceConvertCommand(StormcraftEssencePlugin plugin, Economy economy, double conversionRate) {
        this.plugin = plugin;
        this.economy = economy;
        this.conversionRate = conversionRate;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /essence convert <amount>", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Converts vault essence into physical essence items", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Rate: " + conversionRate + " vault essence = 1 physical essence", NamedTextColor.GRAY));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount!", NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be positive!", NamedTextColor.RED));
            return true;
        }

        // Calculate vault cost
        double vaultCost = amount * conversionRate;

        if (!economy.has(player, vaultCost)) {
            player.sendMessage(Component.text("Insufficient essence! Need " + String.format("%.1f", vaultCost) +
                ", have " + String.format("%.1f", economy.getBalance(player)), NamedTextColor.RED));
            return true;
        }

        // Withdraw essence
        economy.withdrawPlayer(player, vaultCost);

        // Give physical essence item
        player.getInventory().addItem(EssenceItem.create(plugin, amount));

        player.sendMessage(Component.text("Converted " + String.format("%.1f", vaultCost) +
            " vault essence into " + String.format("%.1f", amount) + " physical essence!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

        return true;
    }
}
