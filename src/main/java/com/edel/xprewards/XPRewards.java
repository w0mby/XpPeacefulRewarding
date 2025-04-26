package com.edel.xprewards;

import org.bukkit.plugin.java.JavaPlugin;

public class XPRewards extends JavaPlugin {

    private ExploreXP exploreXP; // Declare ExploreXP so we can call saveExploredChunks()

    @Override
    public void onEnable() {
        exploreXP = new ExploreXP(this);

        getServer().getPluginManager().registerEvents(new BreakXP(), this);
        getServer().getPluginManager().registerEvents(new HarvestXP(this), this);
        getServer().getPluginManager().registerEvents(exploreXP, this);
        getServer().getPluginManager().registerEvents(new ChatWordGame(this), this);
        getServer().getPluginManager().registerEvents(new GunpowderFeature(this), this);

        getLogger().info("XPRewards plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save explored chunks when the plugin is disabled
        if (exploreXP != null) {
            exploreXP.saveExploredChunks();
        }

        getLogger().info("XPRewards plugin has been disabled!");
    }
}
