package com.edel.xprewards;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;

import java.util.Random;

public class GunpowderFeature implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public GunpowderFeature(JavaPlugin plugin) {
        this.plugin = plugin;
        addCraftingRecipe();
        addFurnaceRecipe();
    }

    private void addCraftingRecipe() {
        ItemStack gunpowderMix = new ItemStack(Material.GUNPOWDER);
        var meta = gunpowderMix.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§6Gunpowder Mix"));
            gunpowderMix.setItemMeta(meta);
        }

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "gunpowder_mix"), gunpowderMix);
        recipe.shape(" C ", " F ", " S ");
        recipe.setIngredient('C', Material.CHARCOAL);
        recipe.setIngredient('F', Material.FLINT);
        recipe.setIngredient('S', Material.SAND);

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info("GunPowder Recipe added.");
    }

    private void addFurnaceRecipe() {
        ItemStack gunpowder = new ItemStack(Material.GUNPOWDER, 3);
        FurnaceRecipe furnaceRecipe = new FurnaceRecipe(new NamespacedKey(plugin, "smelt_gunpowder_mix"), gunpowder, Material.GUNPOWDER, 0.1f, 200);
        Bukkit.addRecipe(furnaceRecipe);
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        if (source.getType() == Material.GUNPOWDER && source.hasItemMeta() &&
                Component.text("§6Gunpowder Mix").equals(source.getItemMeta().displayName())) {

            if (random.nextInt(100) < 10) {
                var furnaceLocation = event.getBlock().getLocation();
                var nearbyPlayers = furnaceLocation.getWorld()
                        .getNearbyEntities(furnaceLocation, 5, 5, 5, entity -> entity instanceof Player);

                furnaceLocation.getWorld().createExplosion(furnaceLocation, 2.0f, false, false);

                for (var entity : nearbyPlayers) {
                    if (entity instanceof Player player) {
                        player.sendMessage("§cThe furnace exploded while smelting Gunpowder Mix! Brace yourself as you're blasted away!");
                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                        Vector randomDirection = getRandomDirection().multiply(4);
                        randomDirection.setY(Math.abs(randomDirection.getY()) + 1.5);
                        player.setVelocity(randomDirection);
                    }
                }

                furnaceLocation.getWorld().spawnParticle(Particle.EXPLOSION, furnaceLocation, 1);
                event.setCancelled(true);
            }
        }
    }

    private Vector getRandomDirection() {
        double x = -1 + (2 * random.nextDouble());
        double y = 0.5 + random.nextDouble();
        double z = -1 + (2 * random.nextDouble());
        return new Vector(x, y, z).normalize();
    }
}
