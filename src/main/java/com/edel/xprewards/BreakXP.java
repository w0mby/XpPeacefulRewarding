package com.edel.xprewards;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.edel.xprewards.utils.BlockChecker;
import com.edel.xprewards.utils.ToolChecker;

public class BreakXP implements Listener {

    public static final int MINERAL_XP_REWARD = 7;
    public static final int STONE_XP_REWARD = 3;
    public static final int DIRT_XP_REWARD = 2;
    public static final int LOG_XP_REWARD = 3;
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material tool = player.getInventory().getItemInMainHand().getType();

        if (ToolChecker.isAxe(tool) && BlockChecker.isTree(block.getType())) {
            player.giveExp(LOG_XP_REWARD);
            return;
        }

        if (ToolChecker.isPickaxe(tool)) {
            if (BlockChecker.isMineral(block.getType())) {
                player.giveExp(MINERAL_XP_REWARD);
            }
            if (BlockChecker.isStone(block.getType())) {
                player.giveExp(STONE_XP_REWARD);
            }
            return;
        }

        if (ToolChecker.isShovel(tool) && BlockChecker.isShovelBlock(block.getType())) {
            player.giveExp(DIRT_XP_REWARD);
        }
    }
}
