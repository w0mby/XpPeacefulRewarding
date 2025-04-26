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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class HarvestXP implements Listener {

    private static final int BONUS_THRESHOLD = 50;
    private static final int BONUS_XP = 50;
    public static final int XP_REWARD = 3;


    // Map to track the number of crops harvested by each player
    private final HashMap<UUID, Integer> playerHarvestCount = new HashMap<>();
    private final JavaPlugin plugin;

    public HarvestXP(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHarvest(PlayerHarvestBlockEvent event) {
        if (!isHarvestedBlockFullyGrownCrop(event)) return;

        Player player = event.getPlayer();
        player.giveExp(XP_REWARD);

        UUID playerId = player.getUniqueId();
        int newCount = playerHarvestCount.getOrDefault(playerId, 0) + 1;
        playerHarvestCount.put(playerId, newCount);

        if (newCount >= BONUS_THRESHOLD) {
            player.giveExp(BONUS_XP);
            showBonus(player);
            playerHarvestCount.put(playerId, 0);
        }
    }

    private void showBonus(Player player) {
        Location location = player.getLocation();
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location.add(0.5, 1, 0.5), 30, 1, 1, 1, 0.1);
        player.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        player.sendMessage("Congratulations! You've harvested " + BONUS_THRESHOLD + " crops and earned a bonus of " + BONUS_XP + " XP!");
    }

    private boolean isHarvestedBlockFullyGrownCrop(PlayerHarvestBlockEvent event) {
        Block block = event.getHarvestedBlock();
        if (!(block.getBlockData() instanceof Ageable crop)) return false;

        return crop.getAge() == crop.getMaximumAge();
    }
}
