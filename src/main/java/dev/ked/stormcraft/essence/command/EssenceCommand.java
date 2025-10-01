package dev.ked.stormcraft.essence.command;

import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import dev.ked.stormcraft.essence.ability.AbilityManager;
import dev.ked.stormcraft.essence.config.EssenceConfig;
import dev.ked.stormcraft.essence.model.PassiveAbility;
import dev.ked.stormcraft.essence.model.PlayerEssenceData;
import dev.ked.stormcraft.essence.persistence.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for /essence
 */
public class EssenceCommand implements CommandExecutor, TabCompleter {
    private final StormcraftEssencePlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final AbilityManager abilityManager;
    private final EssenceConfig config;
    private final MiniMessage miniMessage;

    public EssenceCommand(StormcraftEssencePlugin plugin, PlayerDataManager playerDataManager,
                         AbilityManager abilityManager, EssenceConfig config) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.abilityManager = abilityManager;
        this.config = config;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            showStatus(player);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "status" -> showStatus(player);
            case "toggle" -> {
                if (args.length < 2) {
                    player.sendMessage(parseMessage(config.getMessage("prefix"))
                            .append(Component.text("Usage: /essence toggle <resistance|reflexes|stormborn>")));
                    return true;
                }
                toggleAbility(player, args[1]);
            }
            case "info" -> {
                if (args.length < 2) {
                    showAllAbilityInfo(player);
                } else {
                    showAbilityInfo(player, args[1]);
                }
            }
            case "help" -> showHelp(player);
            default -> player.sendMessage(parseMessage(config.getMessage("prefix"))
                    .append(Component.text("Unknown subcommand. Use /essence help for usage.")));
        }

        return true;
    }

    private void showStatus(Player player) {
        PlayerEssenceData data = playerDataManager.getPlayerData(player);
        double balance = plugin.getEconomy().getBalance(player);
        double drainRate = abilityManager.calculateDrainRate(data);

        String passivesList = data.hasActivePassives()
                ? data.getActivePassives().stream()
                    .map(PassiveAbility::getDisplayName)
                    .collect(Collectors.joining(", "))
                : "None";

        player.sendMessage(parseMessage(config.getMessage("status_header")));
        player.sendMessage(parseMessage(config.getMessage("status_sel")
                .replace("{level}", String.valueOf(data.getStormExposureLevel()))));
        player.sendMessage(parseMessage(config.getMessage("status_total")
                .replace("{total}", String.format("%.2f", data.getTotalStormEssence()))));
        player.sendMessage(parseMessage(config.getMessage("status_current")
                .replace("{balance}", String.format("%.2f", balance))));
        player.sendMessage(parseMessage(config.getMessage("status_passives")
                .replace("{passives}", passivesList)));
        player.sendMessage(parseMessage(config.getMessage("status_drain")
                .replace("{rate}", String.format("%.2f", drainRate))));
    }

    private void toggleAbility(Player player, String abilityName) {
        PassiveAbility ability = parseAbilityName(abilityName);
        if (ability == null) {
            player.sendMessage(parseMessage(config.getMessage("prefix"))
                    .append(Component.text("Unknown ability: " + abilityName)));
            return;
        }

        abilityManager.togglePassive(player, ability);
    }

    private void showAbilityInfo(Player player, String abilityName) {
        PassiveAbility ability = parseAbilityName(abilityName);
        if (ability == null) {
            player.sendMessage(parseMessage(config.getMessage("prefix"))
                    .append(Component.text("Unknown ability: " + abilityName)));
            return;
        }

        PlayerEssenceData data = playerDataManager.getPlayerData(player);
        int sel = data.getStormExposureLevel();
        int required = config.getUnlockLevel(ability);
        boolean unlocked = sel >= required;

        player.sendMessage(Component.text("━━━ " + ability.getDisplayName() + " ━━━").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        player.sendMessage(Component.text(ability.getDescription()).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        player.sendMessage(Component.text("Required SEL: " + required + " | Your SEL: " + sel)
                .color(unlocked ? net.kyori.adventure.text.format.NamedTextColor.GREEN : net.kyori.adventure.text.format.NamedTextColor.RED));

        if (unlocked && ability.isPassive()) {
            String effectInfo = switch (ability) {
                case STORM_RESISTANCE -> String.format("%.1f%% damage reduction",
                        abilityManager.getStormResistanceReduction(data));
                case LIGHTNING_REFLEXES -> String.format("Speed %d during storms",
                        abilityManager.getLightningReflexesSpeed(data) + 1);
                case STORMBORN -> String.format("%.1f HP/s regen when exposed",
                        abilityManager.getStormbornRegen(data));
                default -> ""; // Active abilities don't have passive effect info
            };
            if (!effectInfo.isEmpty()) {
                player.sendMessage(Component.text("Current Effect: " + effectInfo).color(net.kyori.adventure.text.format.NamedTextColor.AQUA));
            }
        } else if (unlocked && ability.isActive()) {
            player.sendMessage(Component.text("Right-click compass to use this ability").color(net.kyori.adventure.text.format.NamedTextColor.AQUA));
        }
    }

    private void showAllAbilityInfo(Player player) {
        player.sendMessage(Component.text("━━━ Storm-Fused Abilities ━━━").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        for (PassiveAbility ability : PassiveAbility.values()) {
            int required = config.getUnlockLevel(ability);
            player.sendMessage(Component.text("• " + ability.getDisplayName() + " (SEL " + required + "): " + ability.getDescription())
                    .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("━━━ Storm Essence Commands ━━━").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        player.sendMessage(Component.text("/essence - Show your status").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/essence toggle <ability> - Toggle an ability on/off").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/essence info [ability] - View ability information").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/essence help - Show this help").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
    }

    private PassiveAbility parseAbilityName(String name) {
        return switch (name.toLowerCase()) {
            case "resistance", "storm_resistance" -> PassiveAbility.STORM_RESISTANCE;
            case "reflexes", "lightning_reflexes", "speed" -> PassiveAbility.LIGHTNING_REFLEXES;
            case "stormborn", "regen" -> PassiveAbility.STORMBORN;
            default -> null;
        };
    }

    private Component parseMessage(String message) {
        return miniMessage.deserialize(message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("status", "toggle", "info", "help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("toggle") || args[0].equalsIgnoreCase("info"))) {
            return Arrays.asList("resistance", "reflexes", "stormborn").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
