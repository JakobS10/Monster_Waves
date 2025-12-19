package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * Haupt-Manager für die Challenge-Logik
 * NEU: Integriert TeamColorManager und TeamBackpackManager
 */
public class ChallengeManager {

    private final ChallengePlugin plugin;
    private Challenge activeChallenge = null;

    // Sub-Manager
    private final ArenaManager arenaManager;
    private final WaveManager waveManager;
    private final BossSetupManager bossSetupManager;
    private final StatisticsManager statisticsManager;
    private final SpectatorManager spectatorManager;
    private final TeamColorManager teamColorManager;     // NEU!
    private final TeamBackpackManager backpackManager;  // NEU!

    public ChallengeManager(ChallengePlugin plugin) {
        this.plugin = plugin;
        this.arenaManager = new ArenaManager(plugin);
        this.waveManager = new WaveManager(plugin);
        this.bossSetupManager = new BossSetupManager(plugin);
        this.statisticsManager = new StatisticsManager(plugin);
        this.spectatorManager = new SpectatorManager(plugin);
        this.teamColorManager = new TeamColorManager(plugin);   // NEU!
        this.backpackManager = new TeamBackpackManager(plugin); // NEU!
    }

    /**
     * Startet eine neue Challenge
     */
    public void startChallenge(int farmTimeSeconds, Player bossPlayer, Player initiator) {
        if (activeChallenge != null) {
            initiator.sendMessage("§cEs läuft bereits eine Challenge!");
            return;
        }

        long farmTicks = farmTimeSeconds * 20L;
        activeChallenge = new Challenge(
                UUID.randomUUID(),
                farmTicks,
                bossPlayer.getUniqueId()
        );

        // Sammle alle Online-Spieler als Teilnehmer
        for (Player p : Bukkit.getOnlinePlayers()) {
            activeChallenge.addParticipant(p.getUniqueId());
            PlayerChallengeData data = new PlayerChallengeData(p.getUniqueId());
            activeChallenge.getPlayerData().put(p.getUniqueId(), data);
        }

        startSetupPhase(bossPlayer);
    }

    /**
     * Setup-Phase: Boss bereitet Waves vor
     */
    private void startSetupPhase(Player bossPlayer) {
        activeChallenge.setCurrentPhase(Challenge.ChallengePhase.SETUP);
        bossSetupManager.startCompleteSetup(bossPlayer);
    }

    /**
     * Farmphase: Spieler sammeln Ressourcen
     */
    private void startFarmingPhase() {
        activeChallenge.setCurrentPhase(Challenge.ChallengePhase.FARMING);

        // NEU: Setup Team-Colors NACH Team-Erstellung
        teamColorManager.setupTeamColors(activeChallenge);

        // NEU: Erstelle Backpacks für alle Teams
        backpackManager.createBackpacks(activeChallenge);

        // Setze Timer auf 0 und starte ihn
        DataManager dm = plugin.getDataManager();
        dm.setTimerTicks(0);
        dm.startTimer();
        activeChallenge.setPhaseStartTick(0);

        // Resette alle Spieler-Inventare
        for (UUID playerId : activeChallenge.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.getInventory().clear();
                player.setGameMode(GameMode.SURVIVAL);
                player.sendMessage("§a§l=== CHALLENGE GESTARTET ===");
                player.sendMessage("§7Farmzeit: §e" + (activeChallenge.getFarmDurationTicks() / 20) + " Sekunden");
                player.sendMessage("§7Sammle Ressourcen für den Kampf!");
                player.sendMessage("");
                player.sendMessage("§6Nutze §e/backpack §6oder §e/bp §6um deinen Team-Backpack zu öffnen!");
                player.sendMessage("§6Ach ja!");
                player.sendMessage("§6Eins noch:");
                player.sendMessage("");
                player.sendMessage("§4§l67");
                player.sendMessage("");
            }
        }

        startFarmingTimer();
    }

    /**
     * Überwacht die Farmzeit
     */
    private void startFarmingTimer() {
        Bukkit.getScheduler().runTaskTimer(plugin, (task) -> {
            DataManager dm = plugin.getDataManager();
            long currentTicks = dm.getTimerTicks();
            long targetTicks = activeChallenge.getFarmDurationTicks();

            if (currentTicks >= targetTicks) {
                task.cancel();
                startCombatPhase();
            }
        }, 20L, 20L);
    }

    /**
     * Combat-Phase: Teams kämpfen in Arenen
     */
    private void startCombatPhase() {
        if (activeChallenge.getTeams().isEmpty()) {
            Bukkit.broadcastMessage("§c§l=== FEHLER ===");
            Bukkit.broadcastMessage("§cKeine Teams für die Challenge!");
            activeChallenge = null;
            plugin.getDataManager().pauseTimer();
            return;
        }

        activeChallenge.setCurrentPhase(Challenge.ChallengePhase.COMBAT);
        activeChallenge.setPhaseStartTick(plugin.getDataManager().getTimerTicks());

        // Erstelle Arenen für alle Teams
        arenaManager.createArenasForTeams(activeChallenge);

        // Teleportiere Teams in Arenen
        for (Map.Entry<UUID, List<UUID>> entry : activeChallenge.getTeams().entrySet()) {
            UUID teamId = entry.getKey();
            List<UUID> teamMembers = entry.getValue();

            ArenaInstance arena = arenaManager.getArenaForTeam(teamId);
            if (arena == null) continue;

            Location spawnPoint = arena.getSpawnPoint();
            for (UUID playerId : teamMembers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    // Speichere Inventar
                    PlayerChallengeData data = activeChallenge.getPlayerData().get(playerId);
                    data.backupInventory(player);
                    data.setCombatStartTick(plugin.getDataManager().getTimerTicks());

                    // Teleportiere
                    player.teleport(spawnPoint);
                    player.setGameMode(GameMode.SURVIVAL);

                    player.sendMessage("§c§l=== KAMPFPHASE BEGINNT ===");
                    player.sendMessage("§7Dein Team: §e" + getTeamMemberNames(teamMembers));
                    player.sendMessage("");
                    player.sendMessage("§e§lVorbereitung...");
                    player.sendMessage("§7Die erste Wave startet gleich!");
                }
            }

            // Starte erste Wave für dieses Team
            waveManager.startWaveForTeam(teamId, 0);
        }
    }

    /**
     * Wird aufgerufen wenn ein Spieler alle Waves abgeschlossen hat
     * NEU: Adventure Mode statt Spectator
     */
    public void onPlayerCompleted(UUID playerId) {
        PlayerChallengeData data = activeChallenge.getPlayerData().get(playerId);
        data.setHasCompleted(true);
        data.setCombatEndTick(plugin.getDataManager().getTimerTicks());

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage("§a§l=== DU HAST ALLE WAVES GESCHAFFT! ===");

            // NEU: Adventure Mode mit Fly statt Spectator
            spectatorManager.enableSpectatorMode(player);
        }

        checkChallengeCompletion();
    }

    /**
     * Wird aufgerufen wenn ein Spieler aufgibt
     * NEU: Adventure Mode statt Spectator
     */
    public void onPlayerForfeited(UUID playerId) {
        PlayerChallengeData data = activeChallenge.getPlayerData().get(playerId);
        data.setHasForfeited(true);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage("§c§lDu hast aufgegeben!");

            // NEU: Adventure Mode mit Fly statt Spectator
            spectatorManager.enableSpectatorMode(player);
        }

        checkChallengeCompletion();
    }

    /**
     * Prüft ob Challenge beendet ist
     */
    private void checkChallengeCompletion() {
        boolean allDone = activeChallenge.getPlayerData().values().stream()
                .allMatch(data -> data.isHasCompleted() || data.isHasForfeited());

        if (allDone) {
            endChallenge();
        }
    }

    /**
     * Beendet Challenge und zeigt Auswertung
     */
    private void endChallenge() {
        activeChallenge.setCurrentPhase(Challenge.ChallengePhase.EVALUATION);
        plugin.getDataManager().pauseTimer();
        statisticsManager.showEvaluation(activeChallenge);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cleanup();
        }, 1200L);
    }

    /**
     * Räumt Challenge-Daten auf
     */
    private void cleanup() {
        // Teleportiere alle zurück
        for (UUID playerId : activeChallenge.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.teleport(player.getWorld().getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);

                PlayerChallengeData data = activeChallenge.getPlayerData().get(playerId);
                data.restoreInventory(player);
            }
        }

        // NEU: Cleanup Spectator-System
        spectatorManager.cleanup();

        // Cleanup Team-Colors und Backpacks
        teamColorManager.cleanup();
        backpackManager.cleanup();

        waveManager.cleanupAll();
        arenaManager.clearArenas();
        activeChallenge = null;
    }

    /**
     * Bricht aktive Challenge ab
     */
    public void cancelChallenge() {
        if (activeChallenge == null) return;

        Bukkit.getLogger().info("[ChallengeManager] Challenge wird abgebrochen...");

        plugin.getDataManager().pauseTimer();
        waveManager.cleanupAll();

        // NEU: Cleanup Spectators, Team-Colors und Backpacks
        spectatorManager.cleanup();
        teamColorManager.cleanup();
        backpackManager.cleanup();

        for (UUID playerId : activeChallenge.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);

                if (activeChallenge.getPlayerData().containsKey(playerId)) {
                    PlayerChallengeData data = activeChallenge.getPlayerData().get(playerId);
                    data.restoreInventory(player);
                }

                player.teleport(player.getWorld().getSpawnLocation());
                player.setFlying(false);
                player.setAllowFlight(false);
                player.setInvulnerable(false);
                player.setCollidable(true);
                player.setInvisible(false); // NEU!

                for (Player other : Bukkit.getOnlinePlayers()) {
                    other.showPlayer(plugin, player);
                }

                if (activeChallenge.getCurrentPhase() == Challenge.ChallengePhase.SETUP) {
                    player.getInventory().clear();
                }
            }
        }

        if (arenaManager != null) {
            arenaManager.clearArenas();
        }

        if (waveManager != null) {
            for (UUID playerId : activeChallenge.getParticipants()) {
                waveManager.cleanup(playerId);
            }
        }

        if (activeChallenge.getBossPlayerId() != null) {
            bossSetupManager.removeTeamBuilder(activeChallenge.getBossPlayerId());

            Player boss = Bukkit.getPlayer(activeChallenge.getBossPlayerId());
            if (boss != null) {
                boss.closeInventory();
                boss.setGameMode(GameMode.SURVIVAL);
                boss.getInventory().clear();
            }
        }

        activeChallenge = null;
        Bukkit.getLogger().info("[ChallengeManager] Challenge abgebrochen und aufgeräumt");
    }

    // Hilfsmethode
    private String getTeamMemberNames(List<UUID> members) {
        List<String> names = new ArrayList<>();
        for (UUID id : members) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) names.add(p.getName());
        }
        return String.join(" & ", names);
    }

    // PUBLIC Wrapper
    public void startFarmingPhasePublic() {
        startFarmingPhase();
    }

    // Getter
    public Challenge getActiveChallenge() {
        return activeChallenge;
    }

    public boolean isChallengeActive() {
        return activeChallenge != null;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    public BossSetupManager getBossSetupManager() {
        return bossSetupManager;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    // NEU: Getter für neue Manager
    public TeamColorManager getTeamColorManager() {
        return teamColorManager;
    }

    public TeamBackpackManager getBackpackManager() {
        return backpackManager;
    }

    // Dummy für TimerManager
    public long getChallengeDurationTicks() {
        return activeChallenge != null ? activeChallenge.getFarmDurationTicks() : 0;
    }

    public void completed() {
        if (activeChallenge != null) {
            endChallenge();
        }
    }
}