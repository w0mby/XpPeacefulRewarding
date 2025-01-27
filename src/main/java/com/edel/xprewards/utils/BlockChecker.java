package com.edel.xprewards.utils;

import java.util.Set;

import org.bukkit.Material;

public class BlockChecker {
    private static final Set<Material> STONE_TYPES = Set.of(
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
            Material.GRANITE, Material.DIORITE, Material.ANDESITE,
            Material.TUFF, Material.CALCITE, Material.DRIPSTONE_BLOCK,
            Material.BASALT, Material.BLACKSTONE, Material.NETHERRACK
    );

    private static final Set<Material> MINERAL_BLOCKS = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE, Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS
    );

    private static final Set<Material> SHOVEL_BLOCKS = Set.of(
            Material.DIRT, Material.COARSE_DIRT, Material.GRASS_BLOCK,
            Material.PODZOL, Material.SAND, Material.RED_SAND, Material.GRAVEL,
            Material.MYCELIUM, Material.CLAY, Material.ROOTED_DIRT, Material.MUD,
            Material.SNOW, Material.SNOW_BLOCK, Material.SOUL_SAND, Material.SOUL_SOIL
    );

    public static boolean isStone(Material material) {
        return STONE_TYPES.contains(material);
    }

    public static boolean isMineral(Material material) {
        return MINERAL_BLOCKS.contains(material);
    }

    public static boolean isTree(Material material) {
        return material.toString().endsWith("_LOG");
    }

    public static boolean isShovelBlock(Material material) {
        return SHOVEL_BLOCKS.contains(material);
    }
}
