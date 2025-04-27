
package com.edel.xprewards;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PassDayChallengeManager implements Listener {

    private final JavaPlugin plugin;
    private boolean challengeActive = false;
    private BukkitTask challengeTimer;
    private Challenge currentChallenge;
    private final Random random = new Random();

    private int mineStoneTarget = 5;
    private int harvestCropsTarget = 10;
    private int travelDistanceTarget = 100;
    private long challengeDurationTicks = 60 * 20; // 60 seconds
    private int xpReward = 50;

    private final ConcurrentHashMap<UUID, Integer> playerChallengeProgress = new ConcurrentHashMap<>();

    private enum ChallengeType { MINE_STONE, HARVEST_CROPS, TRAVEL_DISTANCE }

    private class Challenge {
        ChallengeType type;
        int targetAmount;
        long startTime;
        long endTime;
        Listener challengeListener;

        Challenge(ChallengeType type, int targetAmount, long durationTicks) {
            this.type = type;
            this.targetAmount = targetAmount;
            this.startTime = plugin.getServer().getWorlds().getFirst().getFullTime();
            this.endTime = this.startTime + durationTicks;
        }
    }

    public PassDayChallengeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        loadConfiguration();
    }

    // Load values from config.yml if available
    private void loadConfiguration() {
        if (plugin.getConfig().contains("challenges.mineStoneTaget")) {
            mineStoneTarget = plugin.getConfig().getInt("challenges.mineStoneTarget", 5);
        }
        if (plugin.getConfig().contains("challenges.harvestCropsTarget")) {
            harvestCropsTarget = plugin.getConfig().getInt("challenges.harvestCropsTarget", 10);
        }
        if (plugin.getConfig().contains("challenges.travelDistanceTarget")) {
            travelDistanceTarget = plugin.getConfig().getInt("challenges.travelDistanceTarget", 100);
        }
        if (plugin.getConfig().contains("challenges.durationSeconds")) {
            challengeDurationTicks = plugin.getConfig().getInt("challenges.durationSeconds", 60) * 20L;
        }
        if (plugin.getConfig().contains("challenges.xpReward")) {
            xpReward = plugin.getConfig().getInt("challenges.xpReward", 50);
        }
    }

    public boolean startRandomChallenge(Player triggerPlayer) {
        if (challengeActive) {
            return false;
        }

        challengeActive = true;
        playerChallengeProgress.clear();
        ChallengeType selectedType = ChallengeType.values()[random.nextInt(ChallengeType.values().length)];

        int targetAmount;
        long durationTicks;

        targetAmount = switch (selectedType) {
            case MINE_STONE -> mineStoneTarget;
            case HARVEST_CROPS -> harvestCropsTarget;
            case TRAVEL_DISTANCE -> travelDistanceTarget;
        };

        durationTicks = challengeDurationTicks;

        currentChallenge = new Challenge(selectedType, targetAmount, durationTicks);
        registerChallengeListener(selectedType);
        announceChallenge(triggerPlayer, selectedType, targetAmount, durationTicks);
        startChallengeTimer(durationTicks);

        return true;
    }

    private void announceChallenge(Player triggerPlayer, ChallengeType type, int targetAmount, long durationTicks) {
        String message = ChatColor.translateAlternateColorCodes('&', "&b⚡ A Pass the Day Challenge has started! ⚡");
        Bukkit.broadcastMessage(message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        String challengeGoal = switch (type) {
            case MINE_STONE -> "&eBe the first to mine &6" + targetAmount + " &eStone or Cobblestone!";
            case HARVEST_CROPS -> "&eBe the first to harvest &6" + targetAmount + " &ecrops!";
            case TRAVEL_DISTANCE -> "&eBe the first to travel &6" + targetAmount + " &eblocks!";
        };
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', challengeGoal));
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&fTime Limit: &a" + (durationTicks / 20) + " seconds &f| Reward: &a" + xpReward + " XP"));
    }

    private void registerChallengeListener(ChallengeType type) {
        if (currentChallenge != null && currentChallenge.challengeListener != null) {
            HandlerList.unregisterAll(currentChallenge.challengeListener);
            currentChallenge.challengeListener = null;
        }

        switch (type) {
        case MINE_STONE:
            currentChallenge.challengeListener = new BlockBreakChallengeListener(this, Material.STONE, Material.COBBLESTONE);
            break;
        case HARVEST_CROPS:
            currentChallenge.challengeListener = new HarvestCropsChallengeListener(this);
            break;
        case TRAVEL_DISTANCE:
            currentChallenge.challengeListener = new TravelDistanceChallengeListener(this);
            break;
        default:
            plugin.getLogger().warning("Unsupported challenge type: " + type);
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(currentChallenge.challengeListener, plugin);
    }

    private void startChallengeTimer(long durationTicks) {
        if (challengeTimer != null && !challengeTimer.isCancelled()) {
            challengeTimer.cancel();
        }
        challengeTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (challengeActive) {
                    endChallenge(null);
                }
            }
        }.runTaskLater(plugin, durationTicks);
    }

    public void completeChallenge(Player player) {
        if (!challengeActive) {
            return;
        }

        endChallenge(player);
    }

    private void endChallenge(Player winner) {
        if (!challengeActive) {
            return;
        }

        reInitializeChallenge();

        if (winner != null) {
            applyRewards(winner);
        } else {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&c⚡ The Pass the Day Challenge has ended! No one completed it in time. ⚡"));
        }
    }

    private void reInitializeChallenge() {
        challengeActive = false;
        playerChallengeProgress.clear();

        if (challengeTimer != null) {
            challengeTimer.cancel();
            challengeTimer = null;
        }

        if (currentChallenge != null && currentChallenge.challengeListener != null) {
            HandlerList.unregisterAll(currentChallenge.challengeListener);
            currentChallenge.challengeListener = null;
        }
        currentChallenge = null;
    }

    private void applyRewards(Player winner) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setTime(13000);
            }
        }

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a⚡ " + winner.getName() + " has completed the challenge! The day has passed! ⚡"));

        winner.giveExp(xpReward);
        winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        winner.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a+&l" + xpReward + " XP &r&afor completing the Pass the Day Challenge!"));
    }

    public void updatePlayerProgress(Player player, int amount) {
        if (!challengeActive || currentChallenge == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        playerChallengeProgress.compute(playerId, (uuid, progress) -> (progress == null) ? amount : progress + amount);

        if (playerChallengeProgress.get(playerId) >= currentChallenge.targetAmount) {
            completeChallenge(player);
        } else {
            int currentProgress = playerChallengeProgress.get(playerId);
            if (currentProgress % Math.max(1, currentChallenge.targetAmount / 5) == 0) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&eChallenge progress: &6" + currentProgress + "/" + currentChallenge.targetAmount));
            }
        }
    }

    private static class BlockBreakChallengeListener implements Listener {
        private final PassDayChallengeManager manager;
        private final List<Material> targetBlocks;

        BlockBreakChallengeListener(PassDayChallengeManager manager, Material... targetBlocks) {
            this.manager = manager;
            this.targetBlocks = List.of(targetBlocks);
        }

        @EventHandler
        public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
            if (!event.isCancelled() && targetBlocks.contains(event.getBlock().getType())) {
                manager.updatePlayerProgress(event.getPlayer(), 1);
            }
        }
    }

    private record HarvestCropsChallengeListener(PassDayChallengeManager manager) implements Listener {

        @EventHandler
        public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
            org.bukkit.block.Block block = event.getBlock();
            Player player = event.getPlayer();

            if (isFullyGrownCrop(block)) {
                manager.updatePlayerProgress(player, 1);
            }
        }

        private boolean isFullyGrownCrop(org.bukkit.block.Block block) {
            Material type = block.getType();

            return switch (type) {
                case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART -> {
                    org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) block.getBlockData();
                    yield ageable.getAge() == ageable.getMaximumAge();
                }
                case MELON, PUMPKIN -> true;
                default -> false;
            };
        }
    }

    private static class TravelDistanceChallengeListener implements Listener {
        private final PassDayChallengeManager manager;
        private final HashMap<UUID, Location> lastLocations = new HashMap<>();

        TravelDistanceChallengeListener(PassDayChallengeManager manager) {
            this.manager = manager;
        }

        @EventHandler
        public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
            if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                    event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                    event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
                return;
            }

            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            Location currentLocation = player.getLocation();
            Location lastLocation = lastLocations.get(playerId);

            if (lastLocation != null && lastLocation.getWorld().equals(currentLocation.getWorld())) {
                double distanceTraveled = currentLocation.distance(lastLocation);

                if (distanceTraveled >= 1.0) {
                    manager.updatePlayerProgress(player, (int) distanceTraveled);
                    lastLocations.put(playerId, currentLocation);
                }
            } else {
                lastLocations.put(playerId, currentLocation);
            }
        }
    }

    public boolean isChallengeActive() {
        return challengeActive;
    }
}
