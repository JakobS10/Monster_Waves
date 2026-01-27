package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import java.util.*;

/**
 * NEU: Verwaltet Team-Farben in der Tab-Liste
 * Zeigt Teams farbig an: Team 1 = Hellblau, Team 2 = Grün, Team 3 = Magenta, etc.
 */
public class TeamColorManager {

    private final ChallengePlugin plugin;

    // Farben für Teams (in Reihenfolge)
    private static final ChatColor[] TEAM_COLORS = {
            ChatColor.AQUA,        // Team 1: Hellblau
            ChatColor.GREEN,       // Team 2: Grün
            ChatColor.LIGHT_PURPLE,// Team 3: Magenta
            ChatColor.YELLOW,      // Team 4: Gelb
            ChatColor.RED,         // Team 5: Rot
            ChatColor.BLUE,        // Team 6: Blau
            ChatColor.GOLD,        // Team 7: Orange
            ChatColor.DARK_GREEN,  // Team 8: Dunkelgrün
            ChatColor.DARK_PURPLE, // Team 9: Lila
            ChatColor.WHITE        // Team 10+: Weiß
    };

    // Scoreboard für Team-Coloring
    private Scoreboard teamScoreboard;
    private final Map<UUID, Team> challengeTeams = new HashMap<>();

    public TeamColorManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Erstellt Team-Coloring für aktive Challenge
     */
    public void setupTeamColors(Challenge challenge) {
        // FIX: Cleanup VORHER aufrufen, dann neu erstellen
        cleanup();

        // Erstelle neues Scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        teamScoreboard = manager.getNewScoreboard();

        // Erstelle Scoreboard-Teams für jedes Challenge-Team
        int teamNumber = 1;
        for (Map.Entry<UUID, List<UUID>> entry : challenge.getTeams().entrySet()) {
            addTeamColorInternal(challenge, entry.getKey(), teamNumber);
            teamNumber++;
        }

        plugin.getLogger().info("[TeamColorManager] Team-Colors aktiviert für " + challenge.getTeams().size() + " Teams");
    }

    /**
     * NEU: Fügt Team-Color für ein einzelnes Team hinzu (für Late-Joining)
     */
    public void addTeamColor(Challenge challenge, UUID teamId) {
        if (teamScoreboard == null) {
            // Wenn noch kein Scoreboard existiert, erstelle es
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            teamScoreboard = manager.getNewScoreboard();
        }

        // Berechne Team-Nummer
        int teamNumber = 1;
        for (UUID id : challenge.getTeams().keySet()) {
            if (id.equals(teamId)) {
                break;
            }
            teamNumber++;
        }

        addTeamColorInternal(challenge, teamId, teamNumber);

        // Setze Scoreboard für alle Spieler dieses Teams
        for (UUID memberId : challenge.getTeamMembers(teamId)) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                player.setScoreboard(teamScoreboard);
            }
        }

        plugin.getLogger().info("[TeamColorManager] Team-Color hinzugefügt für Team " + teamNumber);
    }

    /**
     * Interne Methode zum Hinzufügen eines Team-Colors
     */
    private void addTeamColorInternal(Challenge challenge, UUID teamId, int teamNumber) {
        List<UUID> members = challenge.getTeamMembers(teamId);

        // Wähle Farbe basierend auf Team-Nummer
        ChatColor color = getTeamColor(teamNumber - 1);

        // Erstelle Scoreboard-Team
        String teamName = "team_" + teamNumber;
        Team scoreboardTeam = teamScoreboard.registerNewTeam(teamName);
        scoreboardTeam.setColor(color);
        scoreboardTeam.setPrefix(color + "[Team " + teamNumber + "] ");
        scoreboardTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        scoreboardTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);

        // Füge alle Spieler hinzu
        for (UUID playerId : members) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                scoreboardTeam.addEntry(player.getName());
            }
        }

        // Speichere für Cleanup
        challengeTeams.put(teamId, scoreboardTeam);

        // Setze Scoreboard für alle Teilnehmer (damit neue Team-Farbe sichtbar ist)
        for (UUID participantId : challenge.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                participant.setScoreboard(teamScoreboard);
            }
        }

        plugin.getLogger().info("[TeamColorManager] Team " + teamNumber + " (" + color.name() + ") mit " + members.size() + " Spielern erstellt");
    }

    /**
     * Gibt Farbe für Team-Index zurück
     */
    private ChatColor getTeamColor(int index) {
        if (index < 0 || index >= TEAM_COLORS.length) {
            return TEAM_COLORS[TEAM_COLORS.length - 1]; // Fallback: Weiß
        }
        return TEAM_COLORS[index];
    }

    /**
     * Entfernt alle Team-Colors
     */
    public void cleanup() {
        if (teamScoreboard == null) return;

        // Entferne alle Spieler von Teams
        for (Team team : challengeTeams.values()) {
            for (String entry : new HashSet<>(team.getEntries())) {
                team.removeEntry(entry);
            }
            team.unregister();
        }

        challengeTeams.clear();

        // Resette Scoreboards für alle Online-Spieler
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboard().equals(teamScoreboard)) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }

        teamScoreboard = null;
        plugin.getLogger().info("[TeamColorManager] Team-Colors entfernt");
    }

    /**
     * Fügt Spieler zu seinem Team hinzu (für Rejoins)
     */
    public void addPlayerToTeam(Challenge challenge, Player player) {
        if (teamScoreboard == null) return;

        UUID playerId = player.getUniqueId();
        UUID teamId = challenge.getTeamOfPlayer(playerId);

        if (teamId == null) return;

        Team scoreboardTeam = challengeTeams.get(teamId);
        if (scoreboardTeam != null) {
            scoreboardTeam.addEntry(player.getName());
            player.setScoreboard(teamScoreboard);
        }
    }
}