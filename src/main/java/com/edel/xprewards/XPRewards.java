package com.edel.xprewards;

import org.bukkit.plugin.java.JavaPlugin;

public class XPRewards extends JavaPlugin {

    private ExploreXP exploreXP;
    private PassDayChallengeManager challengeManager;
    private FrankensteinAssembly frankensteinAssembly;

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
        getLogger().info("Pass day Ritual feature has been enabled!");

        frankensteinAssembly = new FrankensteinAssembly(this);
        getServer().getPluginManager().registerEvents(frankensteinAssembly, this);
        getLogger().info("Frankenstein's Abomination Assembly feature has been enabled!");

        getLogger().info("XPRewards plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (exploreXP != null) {
            exploreXP.saveExploredChunks();
        }

        if (frankensteinAssembly != null) {
            frankensteinAssembly.saveCooldowns();
        }

        getLogger().info("XPRewards plugin has been disabled!");
    }
}
