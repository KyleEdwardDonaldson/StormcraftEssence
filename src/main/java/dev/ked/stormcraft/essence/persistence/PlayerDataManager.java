package dev.ked.stormcraft.essence.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.ked.stormcraft.essence.StormcraftEssencePlugin;
import dev.ked.stormcraft.essence.model.PassiveAbility;
import dev.ked.stormcraft.essence.model.PlayerEssenceData;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Manages persistence of player essence data to JSON files.
 */
public class PlayerDataManager {
    private final StormcraftEssencePlugin plugin;
    private final File dataFolder;
    private final Gson gson;
    private final Map<UUID, PlayerEssenceData> playerDataCache;

    public PlayerDataManager(StormcraftEssencePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.playerDataCache = new HashMap<>();

        // Create data folder if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Gets or creates player essence data
     */
    public PlayerEssenceData getPlayerData(UUID playerId) {
        return playerDataCache.computeIfAbsent(playerId, id -> {
            PlayerEssenceData data = loadPlayerData(id);
            return data != null ? data : new PlayerEssenceData(id);
        });
    }

    /**
     * Gets or creates player essence data by Player object
     */
    public PlayerEssenceData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    /**
     * Loads all player data files into cache
     */
    public void loadAllPlayerData() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        int loaded = 0;
        for (File file : files) {
            try {
                String fileName = file.getName().replace(".json", "");
                UUID playerId = UUID.fromString(fileName);
                PlayerEssenceData data = loadPlayerData(playerId);
                if (data != null) {
                    playerDataCache.put(playerId, data);
                    loaded++;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid player data file: " + file.getName());
            }
        }

        plugin.getLogger().info("Loaded " + loaded + " player essence data files");
    }

    /**
     * Loads player data from file
     */
    private PlayerEssenceData loadPlayerData(UUID playerId) {
        File file = new File(dataFolder, playerId.toString() + ".json");
        if (!file.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);

            PlayerEssenceData playerData = new PlayerEssenceData(playerId);

            // Load total storm essence
            if (data.containsKey("totalStormEssence")) {
                playerData.addStormEssence(((Number) data.get("totalStormEssence")).doubleValue());
            }

            // Load active passives
            if (data.containsKey("activePassives")) {
                List<String> passives = (List<String>) data.get("activePassives");
                for (String passiveName : passives) {
                    try {
                        PassiveAbility ability = PassiveAbility.valueOf(passiveName);
                        playerData.enablePassive(ability);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Unknown passive ability: " + passiveName);
                    }
                }
            }

            return playerData;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load player data for " + playerId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves player data to file
     */
    public void savePlayerData(UUID playerId) {
        PlayerEssenceData playerData = playerDataCache.get(playerId);
        if (playerData == null) {
            return;
        }

        File file = new File(dataFolder, playerId.toString() + ".json");

        Map<String, Object> data = new HashMap<>();
        data.put("totalStormEssence", playerData.getTotalStormEssence());

        List<String> activePassives = new ArrayList<>();
        for (PassiveAbility ability : playerData.getActivePassives()) {
            activePassives.add(ability.name());
        }
        data.put("activePassives", activePassives);

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + playerId + ": " + e.getMessage());
        }
    }

    /**
     * Saves all cached player data
     */
    public void saveAllPlayerData() {
        for (UUID playerId : playerDataCache.keySet()) {
            savePlayerData(playerId);
        }
        plugin.getLogger().info("Saved " + playerDataCache.size() + " player essence data files");
    }

    /**
     * Removes player from cache (call on logout)
     */
    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        playerDataCache.remove(playerId);
    }
}
