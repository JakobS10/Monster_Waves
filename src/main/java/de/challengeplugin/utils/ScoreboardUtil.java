package de.challengeplugin.utils;

import de.challengeplugin.managers.StatisticsManager;
import de.challengeplugin.models.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import java.util.*;

/**
 * Utility-Klasse für Scoreboard-Anzeigen
 * Zeigt Challenge-Statistiken im Scoreboard
 */
public class ScoreboardUtil {

    /**
     * Erstellt Scoreboard für Challenge-Übersicht
     * @param player Der Spieler
     * @param challenge Die aktuelle Challenge
     */
    public static void showChallengeScoreboard(Player player, Challenge challenge) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective(
                "challenge",
                "dummy",
                "§6§lChallenge"
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerChallengeData data = challenge.getPlayerData().get(player.getUniqueId());
        if (data == null) return;

        // Zeile 10: Leerzeile
        objective.getScore("§7").setScore(10);

        // Zeile 9: Aktuelle Wave
        objective.getScore("§eWave: §6" + (data.getCurrentWaveIndex() + 1) + "§e/§63").setScore(9);

        // Zeile 8: Leerzeile
        objective.getScore("§8").setScore(8);

        // Zeile 7: Tode
        objective.getScore("§cTode: §f" + data.getTotalDeaths()).setScore(7);

        // Zeile 6: Schaden
        objective.getScore("§cSchaden: §f" + String.format("%.1f", data.getTotalDamageTaken())).setScore(6);

        // Zeile 5: Leerzeile
        objective.getScore("§9").setScore(5);

        // Zeile 4: Zeit
        long elapsedTicks = System.currentTimeMillis() / 50 - data.getCombatStartTick();
        objective.getScore("§eZeit: §f" + formatTicks(elapsedTicks)).setScore(4);

        player.setScoreboard(scoreboard);
    }

    /**
     * Erstellt Ranglisten-Scoreboard
     * @param player Der Spieler
     * @param challenge Die Challenge
     * @param criteria Sortier-Kriterium
     */
    public static void showRankingScoreboard(Player player, Challenge challenge,
                                             StatisticsManager.SortCriteria criteria) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();

        String title = criteria == StatisticsManager.SortCriteria.FASTEST ? "§6§lSchnellste" :
                criteria == StatisticsManager.SortCriteria.LEAST_DEATHS ? "§a§lWenigste Tode" :
                        "§c§lWenigster Schaden";

        Objective objective = scoreboard.registerNewObjective(
                "ranking",
                "dummy",
                title
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Sortiere Spieler
        List<PlayerChallengeData> winners = new ArrayList<>(challenge.getPlayerData().values());
        winners.removeIf(d -> !d.isHasCompleted());

        switch (criteria) {
            case FASTEST:
                winners.sort(Comparator.comparingLong(d ->
                        d.getCombatEndTick() - d.getCombatStartTick()));
                break;
            case LEAST_DEATHS:
                winners.sort(Comparator.comparingInt(PlayerChallengeData::getTotalDeaths));
                break;
            case LEAST_DAMAGE:
                winners.sort(Comparator.comparingDouble(PlayerChallengeData::getTotalDamageTaken));
                break;
        }

        // Zeige Top 5
        int score = 10;
        int rank = 1;
        for (PlayerChallengeData data : winners) {
            if (rank > 5) break;

            Player p = Bukkit.getPlayer(data.getPlayerId());
            String name = p != null ? p.getName() : "???";

            String value = "";
            switch (criteria) {
                case FASTEST:
                    value = formatTicks(data.getCombatEndTick() - data.getCombatStartTick());
                    break;
                case LEAST_DEATHS:
                    value = String.valueOf(data.getTotalDeaths());
                    break;
                case LEAST_DAMAGE:
                    value = String.format("%.1f", data.getTotalDamageTaken());
                    break;
            }

            objective.getScore("§e#" + rank + " §7" + name + ": §f" + value).setScore(score);
            score--;
            rank++;
        }

        player.setScoreboard(scoreboard);
    }

    /**
     * Entfernt Scoreboard vom Spieler
     */
    public static void clearScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    /**
     * Formatiert Ticks zu Zeit-String
     */
    private static String formatTicks(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}