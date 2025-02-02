package com.edel.xprewards;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Particle;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

public class ExploreXP implements Listener {

    private static final double TREASURE_CHANCE = 0.03;
    private static final double MONSTER_SPAWN_CHANCE = 0.1;
    public static final double PEACEFUL_ENTITY_SPAWN_CHANCE = 0.02;
    private static final int BASE_XP_REWARDS = 5;
    private static final double STREAK_XP_MULTIPLIER = 1.5;
    private final HashMap<UUID, HashSet<String>> playerExploredChunks = new HashMap<>();
    private final HashMap<UUID, Integer> playerExplorationStreaks = new HashMap<>();
    private final HashMap<UUID, String> playerLastChunk = new HashMap<>();

    private File dataFile;
    private FileConfiguration dataConfig;
    private final Random random = new Random();
    public ExploreXP(JavaPlugin plugin) {
        dataFile = new File(plugin.getDataFolder(), "explored_chunks.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadExploredChunks();
    }

    private void initializePlayerData(UUID playerId) {
        playerExploredChunks.putIfAbsent(playerId, new HashSet<>());
        playerExplorationStreaks.putIfAbsent(playerId, 0);
        playerLastChunk.putIfAbsent(playerId, "");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk currentChunk = player.getLocation().getChunk();
        UUID playerId = player.getUniqueId();

        initializePlayerData(playerId);

        String chunkKey = generateChunkKey(currentChunk);
        String lastChunkKey = playerLastChunk.get(playerId);

        if (stillInTheSameChunk(chunkKey, lastChunkKey)) {
            return;
        }

        if (isNewChunk(playerId, chunkKey)) {
            handleNewChunkDiscovery(player, playerId, chunkKey);
        } else if (lastChunkKey != null) {
            handleKnownChunk(player);
        }
        playerLastChunk.put(playerId, chunkKey);
    }

    private String generateChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ";" + chunk.getX() + ";" + chunk.getZ();
    }

    private static boolean stillInTheSameChunk(String chunkKey, String lastChunkKey) {
        return chunkKey.equals(lastChunkKey);
    }

    private void handleKnownChunk(Player player) {
        resetExplorationStreak(player.getUniqueId());
        if (random.nextDouble() < PEACEFUL_ENTITY_SPAWN_CHANCE) {
            spawnPeacefulEntity(player);
        }
    }

    private boolean isNewChunk(UUID playerId, String chunkKey) {
        return !playerExploredChunks.get(playerId).contains(chunkKey);
    }

    private void handleNewChunkDiscovery(Player player, UUID playerId, String chunkKey) {
        playerExploredChunks.get(playerId).add(chunkKey);
        int currentStreak = updateExplorationStreak(playerId);
        rewardPlayerWithXP(player, currentStreak);

        if (random.nextDouble() < TREASURE_CHANCE) {
            giveTreasure(player);
        }

        if (random.nextDouble() < MONSTER_SPAWN_CHANCE) {
            spawnRandomMonster(player);
        }
    }

    private int updateExplorationStreak(UUID playerId) {
        int currentStreak = playerExplorationStreaks.get(playerId) + 1;
        playerExplorationStreaks.put(playerId, currentStreak);
        return currentStreak;
    }

    private void rewardPlayerWithXP(Player player, int currentStreak) {
        int xpReward = (int) (BASE_XP_REWARDS + (currentStreak * STREAK_XP_MULTIPLIER));
        player.giveExp(xpReward);
        player.sendMessage("You discovered a new area! (+" + xpReward + " XP, Streak: " + currentStreak + ")");
    }

    private void resetExplorationStreak(UUID playerId) {
        playerExplorationStreaks.put(playerId, 0);
    }

    private void giveTreasure(Player player) {
        ItemStack selectedTreasure = selectRandomTreasure();
        spawnTreasureChest(player.getLocation(), selectedTreasure);
        createVisualEffect(player);
        playSoundEffect(player);
        broadcastTreasureMessage(player, selectedTreasure);
    }

    private ItemStack selectRandomTreasure() {
        // Define treasures with their respective weights
        Treasure[] treasures = {
                new Treasure(new ItemStack(Material.DIAMOND, random.nextInt(2) + 1), 5), // 5% chance
                new Treasure(new ItemStack(Material.IRON_INGOT, random.nextInt(5) + 1), 20), // 20% chance
                new Treasure(new ItemStack(Material.GOLD_INGOT, random.nextInt(3) + 1), 15), // 15% chance
                new Treasure(new ItemStack(Material.EMERALD, random.nextInt(2) + 1), 10), // 10% chance
                new Treasure(new ItemStack(Material.EXPERIENCE_BOTTLE, random.nextInt(10) + 1), 15), // 25% chance
                new Treasure(new ItemStack(Material.REDSTONE, random.nextInt(8) + 1), 25) // 25% chance
        };

        // Calculate total weight
        int totalWeight = 0;
        for (Treasure treasure : treasures) {
            totalWeight += treasure.weight;
        }

        // Select a random treasure based on weight
        int randomIndex = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (Treasure treasure : treasures) {
            currentWeight += treasure.weight;
            if (randomIndex < currentWeight) {
                return treasure.itemStack;
            }
        }

        // Fallback (should not reach here)
        return treasures[0].itemStack;
    }

    private void spawnTreasureChest(Location location, ItemStack treasure) {
        location.getBlock().setType(Material.CHEST);
        Chest chest = (Chest) location.getBlock().getState();
        chest.getInventory().addItem(treasure);
    }

    private void spawnRandomMonster(Player player) {
        EntityType[] monsters = {EntityType.ZOMBIE,EntityType.WITCH, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER};
        EntityType selectedMonster = monsters[random.nextInt(monsters.length)];
        Location location = player.getLocation();
        player.getWorld().spawnEntity(location, selectedMonster);
        player.sendMessage("Watch out! A " + selectedMonster.name().toLowerCase().replace('_', ' ') + " has appeared!");
    }

    private void spawnPeacefulEntity(Player player) {
        if (player.getVehicle() != null && player.getVehicle().getType() == EntityType.BOAT) {
            return;
        }
        EntityType[] peacefulEntities = {EntityType.SHEEP, EntityType.COW, EntityType.CHICKEN, EntityType.PIG, EntityType.HORSE, EntityType.PARROT};
        EntityType selectedEntity = peacefulEntities[random.nextInt(peacefulEntities.length)];
        Location location = player.getLocation();
        player.getWorld().spawnEntity(location, selectedEntity);
        player.sendMessage("A friendly " + selectedEntity.name().toLowerCase().replace('_', ' ') + " has appeared!");
    }

    private void createVisualEffect(Player player) {
        Location location = player.getLocation();
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location.add(0.5, 1, 0.5), 30, 1, 1, 1, 0.1);
    }

    private void playSoundEffect(Player player) {
        Location location = player.getLocation();
        player.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
    }

    private void broadcastTreasureMessage(Player player, ItemStack treasure) {
        String message = "Congratulations! " + player.getName() + " found a hidden treasure: "
                + treasure.getAmount() + " "
                + treasure.getType().toString().replace('_', ' ') + "!";
        player.getServer().broadcastMessage(message);
    }


    public void saveExploredChunks() {
        for (UUID playerId : playerExploredChunks.keySet()) {
            HashSet<String> chunks = playerExploredChunks.get(playerId);
            dataConfig.set(playerId.toString(), chunks.toArray(new String[0]));
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadExploredChunks() {
        for (String key : dataConfig.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            HashSet<String> chunks = new HashSet<>(dataConfig.getStringList(key));
            playerExploredChunks.put(playerId, chunks);
        }
    }
}
