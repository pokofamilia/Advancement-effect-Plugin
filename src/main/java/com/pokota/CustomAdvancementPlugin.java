package com.pokota;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CustomAdvancementPlugin extends JavaPlugin implements Listener {

    private final Map<String, Long> recentAdvancements = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // --- Tabキー（プレイヤーリスト）に進捗数を表示する準備 ---
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = board.getObjective("adv_count");
        if (objective == null) {
            // "adv_count" という内部名のスコアボードを作成し、表示名を「進捗数」にする
            objective = board.registerNewObjective("adv_count", "dummy", Component.text("進捗数", NamedTextColor.AQUA));
        }
        // 表示場所をPLAYER_LIST（Tabキーの画面）に設定
        objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);

        getLogger().info("CustomAdvancementPlugin が有効になりました！");
    }

    // --- プレイヤーがサーバーに入った時に、Tabキーの数字を最新にする ---
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int completed = countCompletedAdvancements(player);
        updateScoreboard(player, completed);
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Advancement adv = event.getAdvancement();
        String key = adv.getKey().toString();

        if (key.startsWith("minecraft:recipes/")) return;
        if (adv.getDisplay() == null) return;

        long currentTime = System.currentTimeMillis();
        if (recentAdvancements.containsKey(key) && (currentTime - recentAdvancements.get(key)) < 500) {
            return;
        }
        recentAdvancements.put(key, currentTime);

        Player player = event.getPlayer();

        // 達成数を再計算
        int totalAdvancements = 118;
        int completed = countCompletedAdvancements(player);

        // --- Tabキーの数字を更新 ---
        updateScoreboard(player, completed);

        // --- タイトルとアクションバーの表示 ---
        Component mainTitle = Component.text(player.getName(), NamedTextColor.YELLOW)
                .append(Component.text(" が進捗を達成！", NamedTextColor.WHITE));
        
        Component subTitle = adv.getDisplay().title().color(NamedTextColor.GREEN);

        Title title = Title.title(mainTitle, subTitle, 
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500)));

        Component actionBarText = Component.text("進捗状況: " + completed + " / " + totalAdvancements, NamedTextColor.AQUA);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
            p.sendActionBar(actionBarText);
        }
    }

    // --- Tabキーのスコアボードを更新する専用の処理 ---
    private void updateScoreboard(Player player, int score) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = board.getObjective("adv_count");
        if (objective != null) {
            objective.getScore(player.getName()).setScore(score);
        }
    }

    private int countCompletedAdvancements(Player player) {
        int count = 0;
        var iterator = Bukkit.getServer().advancementIterator();
        while (iterator.hasNext()) {
            Advancement adv = iterator.next();
            if (adv.getKey().toString().startsWith("minecraft:recipes/")) continue;
            if (adv.getDisplay() == null) continue;

            if (player.getAdvancementProgress(adv).isDone()) {
                count++;
            }
        }
        return count;
    }
}