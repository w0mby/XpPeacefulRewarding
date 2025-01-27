package com.edel.xprewards;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

import java.util.HashMap;
import java.util.UUID;

public class HarvestXP implements Listener {

    private static final int BONUS_THRESHOLD = 50; // Number of crops to harvest for a bonus
    private static final int BONUS_XP = 50; // Bonus XP amount
    public static final int XP_REWARD = 3;

    // Map to track the number of crops harvested by each player
    private final HashMap<UUID, Integer> playerHarvestCount = new HashMap<>();

    @EventHandler
    public void onHarvest(PlayerHarvestBlockEvent event) {
        Block block = event.getHarvestedBlock();
        if (!isFullyGrownCrop(block)) return;

        Player player = event.getPlayer();
        Location location = player.getLocation();

        player.giveExp(XP_REWARD);
        // Update the player's harvest count
        UUID playerId = player.getUniqueId();
        int newCount = playerHarvestCount.getOrDefault(playerId, 0) + 1;
        playerHarvestCount.put(playerId, newCount);

        // Check if the player has reached the bonus threshold
        if (newCount >= BONUS_THRESHOLD) {
            player.giveExp(BONUS_XP); // Give bonus XP

            // Create a visual effect around the chest
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location.add(0.5, 1, 0.5), 30, 1, 1, 1, 0.1);

            // Play a sound effect for the player
            player.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);


            player.sendMessage("Congratulations! You've harvested " + BONUS_THRESHOLD + " crops and earned a bonus of " + BONUS_XP + " XP!");
            playerHarvestCount.put(playerId, 0); // Reset the count
        }
    }

    // Helper method: Check if a crop is fully grown
    private boolean isFullyGrownCrop(Block block) {
        if (!(block.getBlockData() instanceof Ageable)) return false;

        Ageable crop = (Ageable) block.getBlockData();
        return crop.getAge() == crop.getMaximumAge();
    }
}
