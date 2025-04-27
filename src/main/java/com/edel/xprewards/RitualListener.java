
package com.edel.xprewards;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class RitualListener implements Listener {

    private final PassDayChallengeManager challengeManager;

    private final HashMap<UUID, Long> ritualCooldowns = new HashMap<>();
    private static final long RITUAL_COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

    private final List<Material> requiredItems = new ArrayList<>();

    public RitualListener(PassDayChallengeManager challengeManager) {
        this.challengeManager = challengeManager;

        requiredItems.add(Material.CLOCK);
        requiredItems.add(Material.GLOWSTONE_DUST);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (!isValidRitualBlock(block)) {
            return;
        }

        if (isPlayerOnCooldown(player)) {
            handlePlayerCooldown(player);
            return;
        }

        if (challengeManager.isChallengeActive()) {
            player.sendMessage(ChatColor.RED + "A challenge is already active! Complete it first.");
            return;
        }

        if (!hasRequiredItems(player)) {
            informPlayerAboutRequiredItems(player);
            return;
        }

        handleRitual(player, block);
    }

    private void handleRitual(Player player, Block block) {
        consumeRitualItems(player);
        playRitualEffects(block.getLocation());

        boolean success = challengeManager.startRandomChallenge(player);

        if (success) {
            setPlayerCooldown(player);
            player.sendMessage(ChatColor.GREEN + "You have successfully performed the ritual! A challenge has begun.");
        } else {
            player.sendMessage(ChatColor.RED + "The ritual failed to start a challenge.");
        }
    }

    private void informPlayerAboutRequiredItems(Player player) {
        player.sendMessage(ChatColor.RED + "You need the following items to perform the ritual:");
        for (Material material : requiredItems) {
            player.sendMessage(ChatColor.YELLOW + "- " + formatItemName(material));
        }
    }

    private void handlePlayerCooldown(Player player) {
        long remainingSeconds = getRemainingCooldownSeconds(player);
        player.sendMessage(ChatColor.RED + "You must wait " + remainingSeconds + " more seconds before performing another ritual.");
    }

    private boolean isValidRitualBlock(Block block) {
        return block.getType() == Material.ENCHANTING_TABLE || block.getType() == Material.LODESTONE || block.getType() == Material.RESPAWN_ANCHOR;
    }

    private boolean hasRequiredItems(Player player) {
        for (Material material : requiredItems) {
            if (!player.getInventory().contains(material)) {
                return false;
            }
        }
        return true;
    }

    private void consumeRitualItems(Player player) {
        for (Material material : requiredItems) {
            if(material == Material.CLOCK) continue;
            ItemStack item = player.getInventory().getItem(player.getInventory().first(material));
            int amount = item.getAmount();
            if (amount > 1) {
                item.setAmount(amount - 1);
            } else {
                player.getInventory().remove(item);
            }
        }
    }

    private void playRitualEffects(Location location) {
        Location effectLocation = location.clone().add(0.5, 1.0, 0.5); // Center above the block

        effectLocation.getWorld().spawnParticle(Particle.PORTAL, effectLocation, 100, 0.5, 0.5, 0.5, 0.5);
        effectLocation.getWorld().spawnParticle(Particle.WITCH, effectLocation, 50, 0.5, 0.5, 0.5, 0.1);

        effectLocation.getWorld().playSound(effectLocation, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        effectLocation.getWorld().playSound(effectLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                effectLocation.getWorld().spawnParticle(Particle.EXPLOSION, effectLocation, 1, 0, 0, 0, 0);
                effectLocation.getWorld().playSound(effectLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
            }
        }.runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("XPRewards")), 20L);
    }

    private boolean isPlayerOnCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        if (!ritualCooldowns.containsKey(playerId)) {
            return false;
        }
        long lastRitualTime = ritualCooldowns.get(playerId);
        return (System.currentTimeMillis() - lastRitualTime) < RITUAL_COOLDOWN_MS;
    }

    private void setPlayerCooldown(Player player) {
        ritualCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private long getRemainingCooldownSeconds(Player player) {
        UUID playerId = player.getUniqueId();
        long lastRitualTime = ritualCooldowns.get(playerId);
        long elapsedMs = System.currentTimeMillis() - lastRitualTime;
        return (RITUAL_COOLDOWN_MS - elapsedMs) / 1000;
    }

    private String formatItemName(Material material) {
        String name = material.toString();
        String[] words = name.split("_");
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formattedName.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return formattedName.toString().trim();
    }
}
