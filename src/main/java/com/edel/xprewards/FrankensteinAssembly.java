package com.edel.xprewards;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Switch;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class FrankensteinAssembly implements Listener {

    private final XPRewards plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 10 * 60 * 1000;

    private final List<Material> requiredMaterials = new ArrayList<>();
    private final Set<Location> activeRitualLocations = new HashSet<>();
    private final Set<Location> runningRituals = new HashSet<>();

    private final Random random = new Random();

    private final List<PotionEffectType> negativeEffects = Arrays.asList(
        PotionEffectType.POISON,
        PotionEffectType.BLINDNESS,
        PotionEffectType.WEAKNESS,
        PotionEffectType.SLOWNESS,
        PotionEffectType.NAUSEA,
        PotionEffectType.HUNGER,
        PotionEffectType.DARKNESS,
        PotionEffectType.UNLUCK
    );

    public FrankensteinAssembly(XPRewards plugin) {
        this.plugin = plugin;

        requiredMaterials.add(Material.ROTTEN_FLESH);
        requiredMaterials.add(Material.BONE);
        requiredMaterials.add(Material.POTION);
        requiredMaterials.add(Material.EMERALD);
        requiredMaterials.add(Material.REDSTONE);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock.getType() == Material.LEVER) {
            Switch leverData = (Switch) clickedBlock.getBlockData();
            if (leverData.isPowered()) {
                handleLeverActivation(player, clickedBlock);
            } else {
                player.sendMessage(NamedTextColor.RED + "The lever must be ON to activate the ritual.");
            }
        }
        else if (clickedBlock.getType() == Material.LIGHTNING_ROD &&
                player.getInventory().getItemInMainHand().getType() == Material.FLINT_AND_STEEL) {
            handleLightningRodActivation(player, clickedBlock);
        }
    }

    private void handleLeverActivation(Player player, Block leverBlock) {
        plugin.getLogger().info("Lever activated! Prepare the assembly mechanism!");
        Location structureCenter = findStructureCenter(leverBlock.getLocation());
        if (!isValidStructure(structureCenter)) {
            plugin.getLogger().info("Invalid structure center found!");
            return;
        }
        if (isOnCooldown(player)) {
            plugin.getLogger().info("Cooldown is still active for this player!");
            long remainingTime = (cooldowns.get(player.getUniqueId()) + COOLDOWN_TIME - System.currentTimeMillis()) / 1000;
            player.sendMessage(NamedTextColor.RED + "You must wait " + remainingTime + " seconds before performing another assembly.");
            return;
        }
        plugin.getLogger().info("Starting preparation phase for the assembly mechanism!");
        if (areMaterialsPlaced(structureCenter)) {
            plugin.getLogger().info("Materials placed! Starting the preparation phase for the assembly mechanism!");
            startPreparationPhase(player, structureCenter);
            player.sendMessage(NamedTextColor.GREEN + "You've activated the assembly mechanism! Now use Flint and Steel on the lightning rod to initiate the final phase.");
        } else {
            plugin.getLogger().info("Materials not placed");
            player.sendMessage(NamedTextColor.RED + "The assembly requires specific materials to be placed on the structure.");
            informAboutRequiredMaterials(player);
        }
    }

    private void handleLightningRodActivation(Player player, Block rodBlock) {
        Location structureCenter = findStructureCenter(rodBlock.getLocation());
        if (!isValidStructure(structureCenter)) {
            return;
        }

        if (!activeRitualLocations.contains(structureCenter)) {
            player.sendMessage(NamedTextColor.RED + "You must first activate the lever to prepare the assembly!");
            return;
        }

        if (runningRituals.contains(structureCenter)) {
            player.sendMessage(NamedTextColor.RED + "The ritual is already in progress at this location!");
            return;
        }

        startRitual(player, structureCenter);
    }

    private boolean isLightningRodOnValidStructure(Block rodBlock) {
        Location center = findStructureCenter(rodBlock.getLocation());
        return isValidStructure(center);
    }

    private void startPreparationPhase(Player player, Location center) {
        activeRitualLocations.add(center);

        World world = center.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.WITCH, center.clone().add(0, 1, 0), 30, 1, 0.5, 1, 0.1);
            world.playSound(center, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.0f, 0.5f);

            for (Player p : world.getPlayers()) {
                if (p.getLocation().distance(center) <= 30) {
                    p.sendMessage(NamedTextColor.DARK_PURPLE + "A mysterious energy begins to gather...");
                }
            }
        }
        plugin.getLogger().info("Preparation phase completed. start a runnable...");
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeRitualLocations.contains(center) && world != null) {
                    for (Entity entity : world.getNearbyEntities(center, 10, 10, 10)) {
                        if (entity instanceof Player) {
                            entity.sendMessage(NamedTextColor.RED + "The energy is fading... you must complete the ritual soon!");
                        }
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (activeRitualLocations.contains(center)) {
                                // The ritual wasn't completed in time
                                activeRitualLocations.remove(center);
                                for (Entity entity : world.getNearbyEntities(center, 10, 10, 10)) {
                                    if (entity instanceof Player) {
                                        entity.sendMessage(NamedTextColor.RED + "The energies have dissipated. The ritual has failed.");
                                    }
                                }
                                world.spawnParticle(Particle.SMOKE, center.clone().add(0, 1, 0), 50, 1, 0.5, 1, 0.1);
                                world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.7f);
                            }
                        }
                    }.runTaskLater(plugin, 20 * 60); // 1 more minute (60 seconds)
                }
            }
        }.runTaskLater(plugin, 20 * 120);
    }

    @EventHandler
    public void onLightningStrike(org.bukkit.event.weather.LightningStrikeEvent event) {
        Location strikeLocation = event.getLightning().getLocation();
        World world = strikeLocation.getWorld();
        if (world == null) return;

        for (Block block : getNearbyBlocks(strikeLocation, 2)) {
            if (block.getType() == Material.LIGHTNING_ROD) {
                if (isLightningRodOnValidStructure(block)) {
                    Location structureCenter = block.getLocation();
                    if (areMaterialsPlaced(structureCenter)) {
                        if (runningRituals.contains(structureCenter)) {
                            return;
                        }
                        Player nearestPlayer = null;
                        double nearestDistance = Double.MAX_VALUE;

                        for (Entity entity : world.getNearbyEntities(structureCenter, 30, 30, 30)) {
                            if (entity instanceof Player) {
                                double distance = entity.getLocation().distance(structureCenter);
                                if (distance < nearestDistance) {
                                    nearestPlayer = (Player) entity;
                                    nearestDistance = distance;
                                }
                            }
                        }

                        if (nearestPlayer != null) {
                            startRitual(nearestPlayer, structureCenter);
                            nearestPlayer.sendMessage(NamedTextColor.GOLD + "A lightning strike has activated the assembly!");
                        }
                        break;
                    }
                }
            }
        }
    }

    private List<Block> getNearbyBlocks(Location location, int radius) {
        List<Block> blocks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    blocks.add(location.getBlock().getRelative(x, y, z));
                }
            }
        }
        return blocks;
    }

    private Location findStructureCenter(Location leverLocation) {
        Block leverBlock = leverLocation.getBlock();
        // Check for iron blocks in the 4 cardinal directions
        Block north = leverBlock.getRelative(0, -1, -1);
        Block south = leverBlock.getRelative(0, -1, 1);
        Block east = leverBlock.getRelative(1, -1, 0);
        Block west = leverBlock.getRelative(-1, -1, 0);

        Location centerLocation = null;

        // Determine the orientation based on where the iron blocks are found
        if (north.getType() == Material.IRON_BLOCK) {
            // Table extends southward
            centerLocation = leverLocation.clone().add(0, -1, -2);
        } else if (south.getType() == Material.IRON_BLOCK) {
            // Table extends northward
            centerLocation = leverLocation.clone().add(0, -1, 2);
        } else if (east.getType() == Material.IRON_BLOCK) {
            // Table extends westward
            centerLocation = leverLocation.clone().add(-2, -1, 0);
        } else if (west.getType() == Material.IRON_BLOCK) {
            // Table extends eastward
            centerLocation = leverLocation.clone().add(2, -1, 0);
        }
        plugin.getLogger().info("findStructureCenter-centerLocation:" + centerLocation);
        return centerLocation;
    }

    /**
     * Check if the structure at the center location is a valid Frankenstein assembly table
     * The table has a specific pattern of blocks:
     *  - Iron blocks in a line at the center
     *  - Lightning rod at the center for "capturing" lightning
     *  - Redstone components for "powering" the creation
     *  - slab brick at each side of the ironblock
     */
    private boolean isValidStructure(Location center) {
        if (center == null) return false;

        String orientation = determineOrientation(center);
        if (orientation == null) return false;
        return checkStructurePattern(center, orientation);
    }

    private String determineOrientation(Location center) {
        Block centerBlock = center.getBlock();

        boolean nsValid = centerBlock.getRelative(0, 0, -1).getType() == Material.IRON_BLOCK &&
                          centerBlock.getRelative(0, 0, 1).getType() == Material.IRON_BLOCK;

        boolean ewValid = centerBlock.getRelative(-1, 0, 0).getType() == Material.IRON_BLOCK &&
                          centerBlock.getRelative(1, 0, 0).getType() == Material.IRON_BLOCK;

        if (nsValid) return "NS";
        if (ewValid) return "EW";

        return null;
    }

    private boolean checkStructurePattern(Location center, String orientation) {
        World world = center.getWorld();
        if (world == null) return false;

        Block centerBlock = center.getBlock();

        // Check the structure pattern based on orientation
        if (orientation.equals("NS")) {
            return checkNorthSouthStructure(centerBlock);
        } else {
            return checkEastWestStructure(centerBlock);
        }
    }

    private boolean checkNorthSouthStructure(Block center) {
        // Check the base layer (Y = 0)
        // The middle row should be iron blocks
        for (int z = -2; z <= 2; z++) {
            if (center.getRelative(0, 0, z).getType() != Material.IRON_BLOCK) {
                return false;
            }
        }

        // Check for stone brick slabs on the sides
        for (int z = -2; z <= 2; z++) {
            if (center.getRelative(-1, 0, z).getType() != Material.STONE_BRICK_SLAB &&
                center.getRelative(1, 0, z).getType() != Material.STONE_BRICK_SLAB) {
                return false;
            }
        }

        // Check for lever at one end (z = -2, y = 1)
        if (center.getRelative(0, 0, -2).getRelative(0, 1, 0).getType() != Material.LEVER) {
            return false;
        }

        // Check for lightning rod at the other end (z = 2, y = 1)
        if (center.getRelative(0, 0, 2).getRelative(0, 1, 0).getType() != Material.LIGHTNING_ROD) {
            return false;
        }

        // Check for iron bars on the corners for restraints
        return center.getRelative(-1, 1, -2).getType() == Material.IRON_BARS &&
                center.getRelative(1, 1, -2).getType() == Material.IRON_BARS &&
                center.getRelative(-1, 1, 2).getType() == Material.IRON_BARS &&
                center.getRelative(1, 1, 2).getType() == Material.IRON_BARS;
    }

    private boolean checkEastWestStructure(Block center) {
        for (int x = -2; x <= 2; x++) {
            if (center.getRelative(x, 0, 0).getType() != Material.IRON_BLOCK) {
                return false;
            }
        }

        for (int x = -2; x <= 2; x++) {
            if (center.getRelative(x, 0, -1).getType() != Material.STONE_BRICK_SLAB &&
                center.getRelative(x, 0, 1).getType() != Material.STONE_BRICK_SLAB) {
                return false;
            }
        }

        // Check for lever at one end (x = -2, y = 1)
        if (center.getRelative(-2, 0, 0).getRelative(0, 1, 0).getType() != Material.LEVER) {
            return false;
        }

        // Check for lightning rod at the other end (x = 2, y = 1)
        if (center.getRelative(2, 0, 0).getRelative(0, 1, 0).getType() != Material.LIGHTNING_ROD) {
            return false;
        }

        return center.getRelative(-2, 1, -1).getType() == Material.IRON_BARS &&
                center.getRelative(-2, 1, 1).getType() == Material.IRON_BARS &&
                center.getRelative(2, 1, -1).getType() == Material.IRON_BARS &&
                center.getRelative(2, 1, 1).getType() == Material.IRON_BARS;
    }

    private boolean areMaterialsPlaced(Location center) {
        if (center == null) return false;

        var requiredCounts = getMaterialRequiredCounts();

        Map<Material, Integer> foundCounts = new HashMap<>();
        for (Material mat : requiredMaterials) {
            foundCounts.put(mat, 0);
        }

        var world = center.getWorld();
        if (world == null) return false;

        for (Entity entity : world.getNearbyEntities(center, 3, 2, 3)) {
            if (entity instanceof Item item) {
                ItemStack itemStack = item.getItemStack();

                if (requiredMaterials.contains(itemStack.getType())) {
                    var material = itemStack.getType();
                    int currentCount = foundCounts.getOrDefault(material, 0);
                    foundCounts.put(material, currentCount + itemStack.getAmount());
                }
            }
        }

        for (Material material : requiredMaterials) {
            int required = requiredCounts.getOrDefault(material, 1);
            int found = foundCounts.getOrDefault(material, 0);
            if (found < required) {
                plugin.getLogger().info("areMaterialsPlaced-foundCounts:" + foundCounts);
                return false;
            }
        }
        plugin.getLogger().info("Materials placed.");
        return true;
    }

    private Map<Material, Integer> getMaterialRequiredCounts() {
        Map<Material, Integer> requiredCounts = new HashMap<>();
        requiredCounts.put(Material.ROTTEN_FLESH, 5);
        requiredCounts.put(Material.BONE, 3);
        requiredCounts.put(Material.POTION, 1);
        requiredCounts.put(Material.EMERALD, 1);
        requiredCounts.put(Material.REDSTONE, 3);
        plugin.getLogger().info("areMaterialsPlaced-requiredCounts:" + requiredCounts);
        return requiredCounts;
    }

    private void informAboutRequiredMaterials(Player player) {
        player.sendMessage(NamedTextColor.YELLOW + "The assembly requires the following materials to be dropped on the structure:");
        player.sendMessage(NamedTextColor.GRAY + "- 5x " + formatItemName(Material.ROTTEN_FLESH) + " (Body parts)");
        player.sendMessage(NamedTextColor.GRAY + "- 3x " + formatItemName(Material.BONE) + " (Skeleton structure)");
        player.sendMessage(NamedTextColor.GRAY + "- 1x Water Bottle (Spark of life)");
        player.sendMessage(NamedTextColor.GRAY + "- 1x " + formatItemName(Material.EMERALD) + " (Villager essence)");
        player.sendMessage(NamedTextColor.GRAY + "- 3x " + formatItemName(Material.REDSTONE) + " (Power source)");
    }

    private void startRitual(Player player, Location center) {
        if (runningRituals.contains(center)) {
            plugin.getLogger().warning("Ritual already running at this location: " + center);
            return;
        }
        runningRituals.add(center);
        setCooldown(player);
        activeRitualLocations.add(center);
        consumeMaterials(center);
        broadcastRitualStart(player, center);
        playRitualStartEffects(center);
        scheduleRitualProgress(player, center);
    }

    private void broadcastRitualStart(Player player, Location center) {
        World world = center.getWorld();
        if (world != null) {
            world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
            // Screen shake effect for all nearby players (simulated by playing a damage sound)
            for (Player p : world.getPlayers()) {
                if (p.getLocation().distance(center) <= 50) { // Only affect nearby players
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.3f, 0.5f);
                    p.sendMessage(NamedTextColor.DARK_PURPLE + "The sky darkens as " + player.getName() +
                            NamedTextColor.LIGHT_PURPLE + " begins assembling a Frankenstein's Abomination!");
                }
            }
            boolean wasStorming = world.hasStorm();
            if (!wasStorming) {
                world.setStorm(true);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        world.setStorm(wasStorming);
                    }
                }.runTaskLater(plugin, 20 * 60 * 2); // 2 minutes later
            }
        }
    }

    private void playRitualStartEffects(Location center) {
        World world = center.getWorld();
        if (world != null) {
            spiralParticles(center, 3, 2, Particle.SOUL, 0.5);
            for (int i = 0; i < 10; i++) {
                final int height = i;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        world.spawnParticle(Particle.END_ROD, center.clone().add(0, height * 0.5, 0),
                                5, 0.1, 0.1, 0.1, 0.01);
                    }
                }.runTaskLater(plugin, i * 2L);
            }
            world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
            world.playSound(center, Sound.ENTITY_WITCH_AMBIENT, 1.0f, 0.6f);
            world.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.5f);

            for (int i = 0; i < 360; i += 20) {
                double angle = Math.toRadians(i);
                Location particleLoc = center.clone().add(Math.cos(angle) * 1.5, 0.1, Math.sin(angle) * 1.5);
                world.spawnParticle(Particle.SMOKE, particleLoc, 10, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    private void spiralParticles(Location center, int spirals, double height, Particle particle, double speed) {
        World world = center.getWorld();
        if (world == null) return;

        final double radius = 2.0;
        final int particlesPerSpiral = 40;

        for (int spiral = 0; spiral < spirals; spiral++) {
            final int spiralOffset = spiral;

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i = 0; i < particlesPerSpiral; i++) {
                        double angle = (Math.PI * 2 * i / particlesPerSpiral) + (spiralOffset * Math.PI / 2);
                        double x = center.getX() + (radius * Math.cos(angle));
                        double z = center.getZ() + (radius * Math.sin(angle));
                        double y = center.getY() + (height * i / particlesPerSpiral);

                        Location particleLoc = new Location(world, x, y, z);
                        world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, speed);
                    }
                }
            }.runTaskLater(plugin, spiral * 10L);
        }
    }

    private void scheduleRitualProgress(Player player, Location center) {
        new BukkitRunnable() {
            private int stage = 0;

            @Override
            public void run() {
                if (!activeRitualLocations.contains(center)) {
                    this.cancel();
                    return;
                }

                stage++;
                int maxStages = 5;
                if (stage <= maxStages) {
                    playRitualProgressEffects(center, stage, maxStages);
                } else {
                    completeRitual(player, center);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 30L, 30L);
    }

    private void playRitualProgressEffects(Location center, int stage, int maxStages) {
        World world = center.getWorld();
        if (world != null) {
            double progress = (double) stage / maxStages;
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1, 0),
                    (int)(30 * progress), 1.5, 1, 1.5, 0.05 * progress);

            if (progress > 0.3) {
                world.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 1.5, 0),
                        (int)(60 * progress), 1.2, 0.8, 1.2, 0.1 * progress);
            }

            if (progress > 0.5) {
                world.spawnParticle(Particle.WITCH, center.clone().add(0, 1.8, 0),
                        (int)(50 * progress), 1.0, 0.5, 1.0, 0.2 * progress);
            }

            if (stage == 1) {
                world.playSound(center, Sound.ENTITY_SKELETON_AMBIENT, 1.0f, 0.7f);
                world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.5f);
            } else if (stage == 2) {
                world.playSound(center, Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.7f);
                world.playSound(center, Sound.ENTITY_WITCH_CELEBRATE, 0.8f, 0.5f);
            } else if (stage == 3) {
                world.playSound(center, Sound.ENTITY_VILLAGER_DEATH, 1.0f, 0.6f);
                world.playSound(center, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.8f, 0.7f);
            } else if (stage == 4) {
                world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.7f);
                world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
            } else {
                world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.8f);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
                world.spawnParticle(Particle.FLASH, center.clone().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3, 0);
            }

            if (stage % 2 == 0) {
                createRisingRunes(center, progress);
            }

            if (progress > 0.7) {
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distance(center) <= 15) {
                        // Screen shake simulation
                        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_SMALL_FALL, 0.1f, 0.1f);
                    }
                }
            }

            if (stage == Math.ceil(maxStages / 2.0)) {
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distance(center) <= 50) {
                        p.sendMessage(NamedTextColor.DARK_RED + "The abomination's parts begin to twitch as energy crackles around the assembly platform...");
                    }
                }
            }
        }
    }

    private void createRisingRunes(Location center, double intensity) {
        World world = center.getWorld();
        if (world == null) return;

        // Create a circle of magical runes
        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            double radius = 1.0 + (intensity * 0.5);

            Location runeLoc = center.clone().add(
                    Math.cos(angle) * radius,
                    0.5,
                    Math.sin(angle) * radius
            );

            for (int j = 0; j < 5; j++) {
                final int height = j;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Particle enchantParticle = Particle.valueOf("ENCHANTMENT_TABLE");
                        world.spawnParticle(enchantParticle, runeLoc.clone().add(0, height * 0.3, 0), 3, 0.1, 0, 0.1, 0);
                    }
                }.runTaskLater(plugin, j * 2);
            }
        }
    }

    private void completeRitual(Player player, Location center) {
        World world = center.getWorld();
        if (world != null) {
            // 1. Darkening effect
            world.spawnParticle(Particle.SQUID_INK, center.clone().add(0, 1.5, 0), 200, 2, 2, 2, 0.1);
            world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.7f, 0.5f);
            // 2. Lightning strike after a short delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    world.strikeLightningEffect(center);
                    world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 0.8f);
                }
            }.runTaskLater(plugin, 10L);
            // 3. Explosion effect after lightning
            new BukkitRunnable() {
                @Override
                public void run() {
                    world.spawnParticle(Particle.EXPLOSION, center.clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
                    world.spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);

                    // Knockback effect for nearby players
                    for (Player p : world.getPlayers()) {
                        if (p.getLocation().distance(center) <= 10) {
                            //euclidian distance
                            Location playerLoc = p.getLocation();
                            double dx = playerLoc.getX() - center.getX();
                            double dz = playerLoc.getZ() - center.getZ();
                            double length = Math.sqrt(dx * dx + dz * dz);

                            if (length > 0) {
                                double knockbackStrength = 5.0 - (length / 10.0); // Stronger the closer you are
                                if (knockbackStrength > 0) {
                                    p.setVelocity(p.getVelocity().add(
                                            new org.bukkit.util.Vector(
                                                    dx / length * knockbackStrength,
                                                    0.2 * knockbackStrength,
                                                    dz / length * knockbackStrength)
                                    ));
                                }
                            }
                        }
                    }
                }
            }.runTaskLater(plugin, 15L);

            // 4. Consume materials and spawn abomination after the explosion
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnAbomination(center);

                    for (int i = 0; i < 3; i++) {
                        final int spiral = i;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                spiralParticles(center, 1, 3 + spiral, Particle.TOTEM_OF_UNDYING, 0.2);
                            }
                        }.runTaskLater(plugin, i * 5L);
                    }
                }
            }.runTaskLater(plugin, 25L);

            // 5. Broadcast completion and award XP
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : world.getPlayers()) {
                        if (p.getLocation().distance(center) <= 50) {
                            p.sendMessage(NamedTextColor.DARK_RED + "The abomination assembly is complete! " +
                                    NamedTextColor.RED + "A new undead being stirs to life!");
                        }
                    }

                    if (player != null && player.isOnline()) {
                        player.giveExp(150);
                        player.sendMessage(NamedTextColor.GREEN + "You've been awarded 150 XP for successfully creating an abomination!");

                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
                        world.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0),
                                50, 0.5, 0.5, 0.5, 0.2);
                    }
                    activeRitualLocations.remove(center);
                    runningRituals.remove(center);
                }
            }.runTaskLater(plugin, 40L);
        }
    }

    private void consumeMaterials(Location center) {
        if (center == null) return;

        World world = center.getWorld();
        if (world == null) return;

        var requiredCounts = getMaterialRequiredCounts();

        Map<Material, Integer> consumedCounts = new HashMap<>();
        for (Material mat : requiredMaterials) {
            consumedCounts.put(mat, 0);
        }

        for (Entity entity : world.getNearbyEntities(center, 3, 2, 3)) {
            if (entity instanceof Item item) {
                ItemStack itemStack = item.getItemStack();
                Material material = itemStack.getType();

                if (requiredMaterials.contains(material)) {
                    int required = requiredCounts.getOrDefault(material, 1);
                    int consumed = consumedCounts.getOrDefault(material, 0);

                    if (consumed < required) {
                        int needed = required - consumed;
                        int toConsume = Math.min(needed, itemStack.getAmount());

                        consumedCounts.put(material, consumed + toConsume);

                        if (toConsume >= itemStack.getAmount()) {
                            entity.remove();
                        } else {
                            itemStack.setAmount(itemStack.getAmount() - toConsume);
                            item.setItemStack(itemStack);
                        }

                        world.spawnParticle(Particle.EGG_CRACK, item.getLocation(), 20, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }
        }

        world.playSound(center, Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
    }

    private void spawnAbomination(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        Location spawnLoc = center.clone().add(0, 1.5, 0);
        ZombieVillager abomination = (ZombieVillager) world.spawnEntity(spawnLoc, EntityType.ZOMBIE_VILLAGER);

        abomination.setCustomName(NamedTextColor.DARK_RED + "Frankenstein's Abomination");
        abomination.setCustomNameVisible(true);

        abomination.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(120.0);
        abomination.setHealth(120.0);
        abomination.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(15.0);
        abomination.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35);

        abomination.setShouldBurnInDay(false);
        customizeAbominationAppearance(abomination);
        abomination.setPersistent(true);
        storeAbominationData(abomination);
        scheduleAbominationParticleEffects(abomination);
    }

    private void customizeAbominationAppearance(ZombieVillager abomination) {
        Villager.Profession[] professions = Villager.Profession.values();
        abomination.setVillagerProfession(professions[random.nextInt(professions.length)]);

        Villager.Type[] types = Villager.Type.values();
        abomination.setVillagerType(types[random.nextInt(types.length)]);

        if (random.nextInt(4) == 0) {
            abomination.setBaby();
            abomination.setCustomName(NamedTextColor.DARK_RED + "Junior Abomination");
        }
        applyRandomEquipment(abomination);
    }

    private void applyRandomEquipment(ZombieVillager abomination) {
        if (random.nextInt(2) == 0) {
            abomination.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            abomination.getEquipment().setHelmetDropChance(0.05f); // Low drop chance
        }

        if (random.nextInt(2) == 0) {
            abomination.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            abomination.getEquipment().setChestplateDropChance(0.05f);
        }

        if (random.nextInt(3) == 0) {
            abomination.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));
            abomination.getEquipment().setItemInMainHandDropChance(0.05f);
        }
    }

    private void storeAbominationData(ZombieVillager abomination) {
        PersistentDataContainer dataContainer = abomination.getPersistentDataContainer();

        NamespacedKey creationTimeKey = new NamespacedKey(plugin, "frankenstein_creation_time");
        NamespacedKey isAbominationKey = new NamespacedKey(plugin, "is_frankenstein_abomination");

        dataContainer.set(creationTimeKey, PersistentDataType.LONG, System.currentTimeMillis());
        dataContainer.set(isAbominationKey, PersistentDataType.BYTE, (byte) 1);
    }

    private void scheduleAbominationParticleEffects(ZombieVillager abomination) {
        new BukkitRunnable() {
            int ticksLived = 0;

            @Override
            public void run() {
                if (!abomination.isValid() || abomination.isDead()) {
                    this.cancel();
                    return;
                }
                ticksLived++;
                if (ticksLived % 20 == 0) {
                    Location loc = abomination.getLocation().add(0, 1, 0);
                    World world = abomination.getWorld();
                    if (random.nextInt(3) == 0) {
                        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 5, 0.3, 0.3, 0.3, 0);
                    }
                    world.spawnParticle(Particle.SMOKE, loc, 1, 0.2, 0.2, 0.2, 0);
                }
            }
        }.runTaskTimer(plugin, 20L, 5L); // Start after 1 second, run every 1/4 second
    }

    @EventHandler
    public void onAbominationDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        if (!(event.getEntity() instanceof ZombieVillager entity)) {
            return;
        }

        PersistentDataContainer dataContainer = entity.getPersistentDataContainer();
        NamespacedKey isAbominationKey = new NamespacedKey(plugin, "is_frankenstein_abomination");

        if (dataContainer.has(isAbominationKey, PersistentDataType.BYTE)) {
            World world = entity.getWorld();
            Location loc = entity.getLocation();

            world.spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
            world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_DEATH, 1.0f, 0.5f);
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.7f);

            world.spawnParticle(Particle.ELECTRIC_SPARK, loc.add(0, 1, 0), 30, 1, 1, 1, 0.5);

            addSpecialLoot(event);
        }
    }

    private void addSpecialLoot(org.bukkit.event.entity.EntityDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        drops.clear();
        event.setDroppedExp(250);

        drops.add(new ItemStack(Material.ROTTEN_FLESH, random.nextInt(5) + 3));
        drops.add(new ItemStack(Material.REDSTONE, random.nextInt(7) + 3));
        drops.add(new ItemStack(Material.EMERALD, random.nextInt(3) + 1));

        ItemStack essenceOfUndeath = createEssenceOfUndeath();
        drops.add(essenceOfUndeath);

        int rareDropRoll = random.nextInt(100);

        if (rareDropRoll < 40) {
            drops.add(createSpecialEnchantedBook());
        } else if (rareDropRoll < 60) {
            drops.add(createSpecialPotion());
        } else if (rareDropRoll < 75) {
            drops.add(createEnchantedEquipment());
        } else if (rareDropRoll < 90) {
            drops.add(createEnchantedWeapon());
        } else {
            drops.add(createSuperRareItem());
        }

        if (random.nextInt(4) == 0) {
            drops.add(new ItemStack(Material.GOLDEN_APPLE, 1));
        }

        if (random.nextInt(10) == 0) {
            drops.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
        }

        if (random.nextInt(3) == 0) {
            drops.add(new ItemStack(Material.EXPERIENCE_BOTTLE, random.nextInt(3) + 1));
        }
    }

    private static @NotNull ItemStack createEssenceOfUndeath() {
        ItemStack essenceOfUndeath = new ItemStack(Material.GHAST_TEAR);
        org.bukkit.inventory.meta.ItemMeta meta = essenceOfUndeath.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(NamedTextColor.DARK_PURPLE + "Essence of Undeath");
            String[] lore = { TextDecoration.ITALIC + "The concentrated life force of a defeated abomination"};
            List<String> loreList = new ArrayList<>(Arrays.asList(lore));
            meta.setLore(loreList);
            essenceOfUndeath.setItemMeta(meta);
        }
        return essenceOfUndeath;
    }

    private ItemStack createSpecialEnchantedBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();

        Enchantment[] possibleEnchants = {
            Enchantment.SHARPNESS,
            Enchantment.PROTECTION,
            Enchantment.UNBREAKING,
            Enchantment.RESPIRATION,
            Enchantment.AQUA_AFFINITY,
            Enchantment.THORNS,
            Enchantment.BANE_OF_ARTHROPODS,
            Enchantment.FIRE_ASPECT,
            Enchantment.LOOTING,
            Enchantment.SILK_TOUCH,
            Enchantment.LUCK_OF_THE_SEA,
            Enchantment.MENDING
        };

        int numEnchants = random.nextInt(3) + 1;
        for (int i = 0; i < numEnchants; i++) {
            org.bukkit.enchantments.Enchantment enchant = possibleEnchants[random.nextInt(possibleEnchants.length)];
            int level = Math.min(5, enchant.getMaxLevel() + random.nextInt(2));
            meta.addStoredEnchant(enchant, level, true);
        }

        List<String> lore = new ArrayList<>();
        lore.add(TextDecoration.ITALIC + "" + NamedTextColor.DARK_PURPLE + "Arcane knowledge from beyond the grave");
        meta.setLore(lore);

        meta.setDisplayName(NamedTextColor.DARK_PURPLE + "Tome of Forbidden Knowledge");

        book.setItemMeta(meta);
        return book;
    }

    private ItemStack createSpecialPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) potion.getItemMeta();

        int potionType = random.nextInt(5);
        switch (potionType) {
            case 0:
                meta.setBasePotionData(new org.bukkit.potion.PotionData(PotionType.STRENGTH, true, true));
                meta.setDisplayName(NamedTextColor.RED + "Elixir of Abominable Strength");
                meta.setLore(List.of(NamedTextColor.GRAY + "The raw power of the abomination flows through this flask"));
                break;
            case 1:
                meta.setBasePotionData(new org.bukkit.potion.PotionData(PotionType.REGENERATION, true, true));
                meta.setDisplayName(NamedTextColor.LIGHT_PURPLE + "Ichor of Undying Flesh");
                meta.setLore(List.of(NamedTextColor.GRAY + "The abomination's ability to regenerate its wounds"));
                break;
            case 2:
                meta.setBasePotionData(new PotionData(PotionType.NIGHT_VISION, false, true));
                meta.setDisplayName(NamedTextColor.BLUE + "Vision of the Dead");
                meta.setLore(List.of(NamedTextColor.GRAY + "See through the darkness as the undead do"));
                break;
            case 3:
                meta.setBasePotionData(new PotionData(PotionType.SLOW_FALLING, false, true));
                meta.setDisplayName(NamedTextColor.WHITE + "Ethereal Descent");
                meta.setLore(List.of(NamedTextColor.GRAY + "Float gently like a departing spirit"));
                break;
            case 4:
                meta.setBasePotionData(new PotionData(PotionType.LONG_FIRE_RESISTANCE, false, true));
                meta.setDisplayName(NamedTextColor.GOLD + "Frankenstein's Flame Ward");
                meta.setLore(List.of(NamedTextColor.GRAY + "The abomination's fear of fire, distilled into protection"));
                break;
        }

        potion.setItemMeta(meta);
        return potion;
    }

    private ItemStack createEnchantedEquipment() {
        Material[] armorTypes = {
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS
        };

        Material armorType = armorTypes[random.nextInt(armorTypes.length)];
        ItemStack armor = new ItemStack(armorType);

        int numEnchants = random.nextInt(2) + 2;

        Enchantment[] armorEnchants = {
            Enchantment.PROTECTION,
            Enchantment.FIRE_PROTECTION,
            Enchantment.PROJECTILE_PROTECTION,
            Enchantment.THORNS,
            Enchantment.UNBREAKING,
            Enchantment.MENDING
        };

        for (int i = 0; i < numEnchants; i++) {
            org.bukkit.enchantments.Enchantment enchant = armorEnchants[random.nextInt(armorEnchants.length)];
            int level = Math.min(enchant.getMaxLevel() + 1, 5);
            armor.addUnsafeEnchantment(enchant, level);
        }

        org.bukkit.inventory.meta.ItemMeta meta = armor.getItemMeta();
        if (meta != null) {
            String itemType = armorType.toString().split("_")[1].toLowerCase();
            meta.setDisplayName(NamedTextColor.AQUA + "Frankenstein's " + capitalize(itemType));
            meta.setLore(List.of(
                    NamedTextColor.GRAY + "This " + itemType + " seems to pulse with unnatural energy",
                    NamedTextColor.DARK_PURPLE + "The abomination's aura lingers within"
            ));
            armor.setItemMeta(meta);
        }

        return armor;
    }

    private ItemStack createEnchantedWeapon() {
        Material[] weaponTypes = {
            Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.IRON_AXE, Material.DIAMOND_AXE,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT
        };

        Material weaponType = weaponTypes[random.nextInt(weaponTypes.length)];
        ItemStack weapon = new ItemStack(weaponType);

        int numEnchants = random.nextInt(2) + 2;

        Enchantment[] weaponEnchants = {
            Enchantment.SHARPNESS,
            Enchantment.FIRE_ASPECT,
            Enchantment.KNOCKBACK,
            Enchantment.UNBREAKING,
            Enchantment.MENDING,
            Enchantment.LOOTING
        };

        if (weaponType == Material.BOW || weaponType == Material.CROSSBOW) {
            weaponEnchants = new Enchantment[]{
                Enchantment.FLAME,
                Enchantment.POWER,
                Enchantment.UNBREAKING,
                Enchantment.MENDING
            };
        } else if (weaponType == Material.TRIDENT) {
            weaponEnchants = new Enchantment[]{
                Enchantment.LOYALTY,
                Enchantment.IMPALING,
                Enchantment.RIPTIDE,
                Enchantment.UNBREAKING,
                Enchantment.MENDING
            };
        }

        for (int i = 0; i < numEnchants; i++) {
            Enchantment enchant = weaponEnchants[random.nextInt(weaponEnchants.length)];
            int level = Math.min(enchant.getMaxLevel() + 1, 5);
            weapon.addUnsafeEnchantment(enchant, level);
        }

        org.bukkit.inventory.meta.ItemMeta meta = weapon.getItemMeta();
        if (meta != null) {
            String[] namePrefixes = {"Abominable", "Monstrous", "Frankenstein's", "Undead", "Reanimated"};
            String[] nameSuffixes = {"Wrath", "Fury", "Rage", "Vengeance", "Power", "Terror"};
            String prefix = namePrefixes[random.nextInt(namePrefixes.length)];
            String suffix = nameSuffixes[random.nextInt(nameSuffixes.length)];

            meta.setDisplayName(NamedTextColor.RED + prefix + " " + suffix);
            meta.setLore(List.of(
                    NamedTextColor.GRAY + "This weapon channels the rage of the abomination",
                    NamedTextColor.DARK_RED + "\"It remembers the pain of its unnatural creation\""
            ));
            weapon.setItemMeta(meta);
        }

        return weapon;
    }

    private ItemStack createSuperRareItem() {
        int itemChoice = random.nextInt(5);

        switch (itemChoice) {
            case 0:
                ItemStack heart = new ItemStack(Material.HEART_OF_THE_SEA);
                org.bukkit.inventory.meta.ItemMeta heartMeta = heart.getItemMeta();
                heartMeta.setDisplayName(NamedTextColor.DARK_RED + "Frankenstein's Heart");
                heartMeta.setLore(List.of(
                        NamedTextColor.GRAY + "The still-beating heart of the abomination",
                        NamedTextColor.DARK_PURPLE + "It pulses with unnatural energy",
                        NamedTextColor.GREEN + "Said to have mysterious powers when used in rituals"
                ));
                heart.setItemMeta(heartMeta);
                return heart;

            case 1:
                Material[] netheriteTypes = {
                    Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                    Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                    Material.NETHERITE_SWORD, Material.NETHERITE_AXE
                };
                Material netheriteType = netheriteTypes[random.nextInt(netheriteTypes.length)];
                ItemStack netherite = new ItemStack(netheriteType);

                int numEnchants = random.nextInt(2) + 3;
                for (int i = 0; i < numEnchants; i++) {
                    org.bukkit.enchantments.Enchantment[] possibleEnchants;

                    if (netheriteType.toString().contains("HELMET") ||
                        netheriteType.toString().contains("CHESTPLATE") ||
                        netheriteType.toString().contains("LEGGINGS") ||
                        netheriteType.toString().contains("BOOTS")) {
                        possibleEnchants = new org.bukkit.enchantments.Enchantment[]{
                            Enchantment.PROTECTION,
                            Enchantment.FIRE_PROTECTION,
                            Enchantment.BLAST_PROTECTION,
                            Enchantment.THORNS,
                            Enchantment.UNBREAKING,
                            Enchantment.MENDING
                        };
                    } else {
                        possibleEnchants = new org.bukkit.enchantments.Enchantment[]{
                            Enchantment.SHARPNESS,
                            Enchantment.FIRE_ASPECT,
                            Enchantment.LOOTING,
                            Enchantment.UNBREAKING,
                            Enchantment.MENDING
                        };
                    }

                    Enchantment enchant = possibleEnchants[random.nextInt(possibleEnchants.length)];
                    int level = enchant.getMaxLevel() + 1;
                    netherite.addUnsafeEnchantment(enchant, level);
                }

                org.bukkit.inventory.meta.ItemMeta netherMeta = netherite.getItemMeta();
                netherMeta.setDisplayName(NamedTextColor.GOLD + "Abomination's " + capitalize(netheriteType.toString().split("_")[1].toLowerCase()));
                netherMeta.setLore(List.of(
                        NamedTextColor.GRAY + "A rare piece of netherite infused with the abomination's essence",
                        NamedTextColor.DARK_PURPLE + "Its power exceeds normal enchantment limitations"
                ));
                netherite.setItemMeta(netherMeta);
                return netherite;

            case 2:
                ItemStack lightningBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
                org.bukkit.inventory.meta.ItemMeta bottleMeta = lightningBottle.getItemMeta();
                bottleMeta.setDisplayName(NamedTextColor.YELLOW + "Lightning in a Bottle");
                bottleMeta.setLore(List.of(
                        NamedTextColor.GRAY + "The electrical essence that animated the abomination",
                        NamedTextColor.DARK_AQUA + "Throw it to summon a lightning strike",
                        NamedTextColor.RED + "Use with extreme caution"
                ));
                lightningBottle.setItemMeta(bottleMeta);
                return lightningBottle;

            case 3:
                ItemStack brain = new ItemStack(Material.FERMENTED_SPIDER_EYE);
                org.bukkit.inventory.meta.ItemMeta brainMeta = brain.getItemMeta();
                brainMeta.setDisplayName(NamedTextColor.LIGHT_PURPLE + "Abomination's Brain");
                brainMeta.setLore(List.of(
                        NamedTextColor.GRAY + "The fractured mind of the creature",
                        NamedTextColor.DARK_PURPLE + "Contains fragments of memories and knowledge",
                        NamedTextColor.AQUA + "Powerful brewing ingredient"
                ));
                brain.setItemMeta(brainMeta);
                return brain;

            case 4:
            default:
                ItemStack apple = new ItemStack(Material.APPLE);
                org.bukkit.inventory.meta.ItemMeta appleMeta = apple.getItemMeta();
                appleMeta.setDisplayName(NamedTextColor.GOLD + "Apples of Immortality");
                appleMeta.setLore(List.of(
                        NamedTextColor.GRAY + "The source of the abomination's resilience",
                        NamedTextColor.YELLOW + "Grants extraordinary regenerative powers"
                ));
                apple.setItemMeta(appleMeta);
                return apple;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private boolean isOnCooldown(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!cooldowns.containsKey(playerUUID)) {
            return false;
        }

        long lastRitualTime = cooldowns.get(playerUUID);
        return (System.currentTimeMillis() - lastRitualTime) < COOLDOWN_TIME;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private long getRemainingCooldownSeconds(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!cooldowns.containsKey(playerUUID)) {
            return 0;
        }

        long lastRitualTime = cooldowns.get(playerUUID);
        long elapsed = System.currentTimeMillis() - lastRitualTime;
        long remaining = COOLDOWN_TIME - elapsed;

        return Math.max(0, remaining / 1000);
    }

    private String formatCooldownTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return minutes + " min " + remainingSeconds + " sec";
        } else {
            return seconds + " seconds";
        }
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

    @EventHandler
    public void onAbominationHit(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        if (entity instanceof ZombieVillager zombieVillager && damager instanceof Player) {

            PersistentDataContainer dataContainer = zombieVillager.getPersistentDataContainer();
            NamespacedKey isAbominationKey = new NamespacedKey(plugin, "is_frankenstein_abomination");

            if (dataContainer.has(isAbominationKey, PersistentDataType.BYTE)) {
                Player player = (Player) damager;
                // chance to add a negative effect
                if (random.nextInt(100) < 60) {
                    PotionEffectType effectType = negativeEffects.get(random.nextInt(negativeEffects.size()));
                    int duration = 60 + random.nextInt(100); // 3-8 seconds
                    int amplifier = random.nextInt(2); // 0 or 1

                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                    player.sendMessage(NamedTextColor.DARK_PURPLE + "The Abomination retaliates with a curse!");

                    World world = player.getWorld();
                    world.spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                    world.playSound(player.getLocation(), Sound.ENTITY_WITCH_HURT, 0.8f, 0.5f);
                }
            }
        }
    }

    @EventHandler
    public void onAbominationCured(EntityTransformEvent event) {
        if (!(event.getEntity() instanceof ZombieVillager zombieVillager)) return;
        if (!(event.getTransformedEntity() instanceof Villager villager)) return;

        PersistentDataContainer data = zombieVillager.getPersistentDataContainer();
        NamespacedKey abominationKey = new NamespacedKey(plugin, "is_frankenstein_abomination");

        if (data.has(abominationKey, PersistentDataType.BYTE)) {
            villager.customName(Component.text(NamedTextColor.DARK_PURPLE + "The Abomination has been cured!"));
            villager.setCustomNameVisible(true);
        }
    }

    public void saveCooldowns() {
        //TODO: save cooldown to file
        cooldowns.clear();
    }
}
