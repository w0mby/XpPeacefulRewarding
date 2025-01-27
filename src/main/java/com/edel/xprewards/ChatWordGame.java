package com.edel.xprewards;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.io.File;
import java.io.IOException;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatWordGame implements Listener {
    private static final int MIN_INTERVAL = 5 * 60 * 20; // 5 minutes in ticks
    private static final int MAX_INTERVAL = 20 * 60 * 20;
    private static final int TIME_LIMIT = 20 * 10;
    private static final int MIN_XP_REWARD = 10;
    private static final int MAX_XP_REWARD = 15;
    public static final String WORDS = "words";

    private final JavaPlugin plugin;
    private List<String> words;
    private String currentWord;
    private boolean gameActive;
    private final Random random = new Random();


    public ChatWordGame(JavaPlugin plugin) {
        this.plugin = plugin;
        loadWords();
        scheduleNextGame();
    }

    private void loadWords() {
        File wordsFile = getWordsFile();
        FileConfiguration config = YamlConfiguration.loadConfiguration(wordsFile);
        this.words = config.getStringList(WORDS);
    }

    private @NotNull File getWordsFile() {
        File wordsFile = new File(plugin.getDataFolder(), "words.yml");
        if (!wordsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                wordsFile.createNewFile();
                FileConfiguration config = YamlConfiguration.loadConfiguration(wordsFile);
                config.set(WORDS, List.of("apple", "banana", "cherry", "date", "elderberry"));
                config.save(wordsFile);
            } catch (IOException ex) {
                plugin.getLogger().severe("Error during word file creation: " + ex);

            }
        }
        return wordsFile;
    }

    private void scheduleNextGame() {
        int interval = new Random().nextInt(MAX_INTERVAL - MIN_INTERVAL) + MIN_INTERVAL;
        plugin.getLogger().info("a chat game will start in " + interval / 20 + " seconds.");
        new BukkitRunnable() {
            @Override
            public void run() {
                startGame();
            }
        }.runTaskLater(plugin, interval);
    }

    private void startGame() {
        currentWord = this.words.get(new Random().nextInt(this.words.size()));
        gameActive = true;
        Bukkit.broadcastMessage(ChatColor.GREEN + "NEW CHATWORDGAME: To gain XP type the word: " + ChatColor.YELLOW + currentWord);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameActive) {
                    return;
                }
                Bukkit.broadcastMessage(ChatColor.RED + "Time's up! No one typed the word correctly.");
                gameActive = false;
                scheduleNextGame();
            }
        }.runTaskLater(plugin, TIME_LIMIT);
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        if (!gameActive) {
            return;
        }
        Player player = event.getPlayer();
        String message = getEventMessage(event);

        if (message.equalsIgnoreCase(currentWord)) {
            int xpReward = random.nextInt((MAX_XP_REWARD - MIN_XP_REWARD) + 1) + MIN_XP_REWARD;
            player.giveExp(xpReward);
            Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " typed the word correctly and wins " + xpReward + " XP!");
            gameActive = false;
            scheduleNextGame();
        }
    }

    private static @NotNull String getEventMessage(AsyncChatEvent event) {
        Component messageComponent = event.message();
        return PlainTextComponentSerializer.plainText().serialize(messageComponent);
    }
}
