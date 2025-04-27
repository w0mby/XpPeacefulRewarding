package com.edel.xprewards;

import org.bukkit.plugin.java.JavaPlugin;

public class XPRewards extends JavaPlugin {

    private ExploreXP exploreXP;
    private PassDayChallengeManager challengeManager;

    @Override
    public void onEnable() {
        exploreXP = new ExploreXP(this);

        getServer().getPluginManager().registerEvents(new BreakXP(), this);
        getServer().getPluginManager().registerEvents(new HarvestXP(this), this);
        getServer().getPluginManager().registerEvents(exploreXP, this);
        getServer().getPluginManager().registerEvents(new ChatWordGame(this), this);
        getServer().getPluginManager().registerEvents(new GunpowderFeature(this), this);

        challengeManager = new PassDayChallengeManager(this);
        getServer().getPluginManager().registerEvents(new RitualListener(challengeManager), this);

        getLogger().info("XPRewards plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (exploreXP != null) {
            exploreXP.saveExploredChunks();
        }

        getLogger().info("XPRewards plugin has been disabled!");
    }
}
