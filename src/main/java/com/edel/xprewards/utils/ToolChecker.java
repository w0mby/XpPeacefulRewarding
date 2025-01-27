package com.edel.xprewards.utils;

import java.util.Set;

import org.bukkit.Material;

public class ToolChecker {
    private static final Set<Material> PICKAXES = Set.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    );

    private static final Set<Material> AXES = Set.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    );

    private static final Set<Material> SHOVELS = Set.of(
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
    );

    public static boolean isAxe(Material material) {
        return AXES.contains(material);
    }

    public static boolean isPickaxe(Material material) {
        return PICKAXES.contains(material);
    }

    public static boolean isShovel(Material material) {
        return SHOVELS.contains(material);
    }
}
