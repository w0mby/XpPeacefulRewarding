package com.edel.xprewards;

import org.bukkit.inventory.ItemStack;

public class Treasure {
    ItemStack itemStack;
    int weight;

    Treasure(ItemStack itemStack, int weight) {
        this.itemStack = itemStack;
        this.weight = weight;
    }
}
