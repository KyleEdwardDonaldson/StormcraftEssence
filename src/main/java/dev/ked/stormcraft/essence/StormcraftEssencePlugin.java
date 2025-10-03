package dev.ked.stormcraft.essence;

import dev.ked.stormcraft.StormcraftPlugin;
import dev.ked.stormcraft.essence.ability.AbilityManager;
import dev.ked.stormcraft.essence.ability.ActiveAbilityManager;
import dev.ked.stormcraft.essence.command.EssenceCommand;
import dev.ked.stormcraft.essence.command.EssenceConvertCommand;
import dev.ked.stormcraft.essence.config.EssenceConfig;
import dev.ked.stormcraft.essence.crafting.CompassCraftingListener;
import dev.ked.stormcraft.essence.infusion.InfusionPedestalManager;
import dev.ked.stormcraft.essence.infusion.InfusedArmorListener;
import dev.ked.stormcraft.essence.listener.PedestalInteractListener;
import dev.ked.stormcraft.essence.integration.PlaceholderAPIIntegration;
import dev.ked.stormcraft.essence.listener.CompassInteractListener;
import dev.ked.stormcraft.essence.listener.EssenceAwardListener;
import dev.ked.stormcraft.essence.listener.PassiveAbilityListener;
import dev.ked.stormcraft.essence.listener.PlayerJoinListener;
import dev.ked.stormcraft.essence.listener.StormriderListener;
import dev.ked.stormcraft.essence.persistence.PlayerDataManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Stormcraft-Essence addon plugin.
 * Provides storm-fused abilities based on accumulated storm exposure essence.
 */
public class StormcraftEssencePlugin extends JavaPlugin {
    private StormcraftPlugin stormcraft;
    private Economy economy;
    private EssenceConfig config;
    private PlayerDataManager playerDataManager;
    private AbilityManager abilityManager;
    private ActiveAbilityManager activeAbilityManager;
    private InfusionPedestalManager infusionPedestalManager;

    @Override
    public void onEnable() {
        // Check for Stormcraft dependency
        if (!getServer().getPluginManager().isPluginEnabled("Stormcraft")) {
            getLogger().severe("Stormcraft plugin not found! Disabling Stormcraft-Essence...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        stormcraft = (StormcraftPlugin) getServer().getPluginManager().getPlugin("Stormcraft");

        // Setup Vault economy
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling Stormcraft-Essence...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load config
        config = new EssenceConfig(this);
        config.loadConfig();

        // Initialize managers
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.loadAllPlayerData();

        abilityManager = new AbilityManager(this, config, playerDataManager, economy);
        abilityManager.startDrainTask();

        activeAbilityManager = new ActiveAbilityManager(this, config, playerDataManager, economy);

        infusionPedestalManager = new InfusionPedestalManager(this, config);
        infusionPedestalManager.startUpdateTask();

        // Register listeners
        getServer().getPluginManager().registerEvents(new EssenceAwardListener(this, playerDataManager, economy), this);
        getServer().getPluginManager().registerEvents(new PassiveAbilityListener(abilityManager, playerDataManager, config), this);
        getServer().getPluginManager().registerEvents(new CompassInteractListener(activeAbilityManager, playerDataManager), this);
        getServer().getPluginManager().registerEvents(new StormriderListener(activeAbilityManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new CompassCraftingListener(this, economy, config.getCompassEssenceCost()), this);
        getServer().getPluginManager().registerEvents(new PedestalInteractListener(infusionPedestalManager), this);
        getServer().getPluginManager().registerEvents(new InfusedArmorListener(this), this);

        // Register commands
        getCommand("essence").setExecutor(new EssenceCommand(this, playerDataManager, abilityManager, config));
        getCommand("essenceconvert").setExecutor(new EssenceConvertCommand(this, economy, config.getEssenceConversionRate()));

        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIIntegration(this).register();
            getLogger().info("PlaceholderAPI expansion registered!");
        }

        getLogger().info("Stormcraft-Essence enabled!");
    }

    @Override
    public void onDisable() {
        if (abilityManager != null) {
            abilityManager.stopDrainTask();
        }

        if (activeAbilityManager != null) {
            activeAbilityManager.shutdown();
        }

        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }

        getLogger().info("Stormcraft-Essence disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    public StormcraftPlugin getStormcraft() {
        return stormcraft;
    }

    public Economy getEconomy() {
        return economy;
    }

    public EssenceConfig getEssenceConfig() {
        return config;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public InfusionPedestalManager getInfusionPedestalManager() {
        return infusionPedestalManager;
    }
}
