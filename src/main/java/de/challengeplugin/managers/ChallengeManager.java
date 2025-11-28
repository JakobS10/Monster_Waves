package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * Haupt-Manager für die Challenge-Logik
 * Koordiniert alle anderen Manager und überwacht den Challenge-Ablauf
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

    public ChallengeManager(ChallengePlugin plugin) {
        this.plugin = plugin;
        this.arenaManager = new ArenaManager(plugin);
        this.waveManager = new WaveManager(plugin);
        this.bossSetupManager = new BossSetupManager(plugin);
        this.statisticsManager = new StatisticsManager(plugin);
        this.spectatorManager = new SpectatorManager(plugin);
    }

    /**
     * Startet eine neue Challenge
     * @param farmTimeSeconds Farmzeit in Sekunden
     * @param bossPlayer Boss-Spieler
     * @param initiator Wer den Command ausgeführt hat
     */
    public void startChallenge(int farmTimeSeconds, Player bossPlayer, Player initiator) {
        // Prüfe ob bereits eine Challenge läuft
        if (activeChallenge != null) {
            initiator.sendMessage("§cEs läuft bereits eine Challenge!");
            return;
        }

        // Erstelle neue Challenge-Instanz
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

        // Starte Setup-Phase
        startSetupPhase(bossPlayer);
    }

    /**
     * Setup-Phase: Boss bereitet Waves vor
     */
    private void startSetupPhase(Player bossPlayer) {
        activeChallenge.setCurrentPhase(Challenge.ChallengePhase.SETUP);

        // Frage Boss nach Einstellungen
        askBossSettings(bossPlayer);
    }

    /**
     * Fragt Boss-Spieler nach Challenge-Einstellungen
     */
    private void askBossSettings(Player bossPlayer) {
        // GUI für Nether/End-Aktivierung
        bossSetupManager.openDimensionSettingsGUI(bossPlayer, (netherEnabled, endEnabled) -> {
            activeChallenge.setNetherEnabled(netherEnabled);
            activeChallenge.setEndEnabled(endEnabled);

            // Frage ob Boss mitspielt
            bossSetupManager.openParticipationGUI(bossPlayer, (participates) -> {
                activeChallenge.setBossParticipates(participates);

                // Starte Wave-Setup
                startWaveSetup(bossPlayer);
            });
        });
    }

    /**
     * Boss definiert Waves für alle Spieler
     */
    private void startWaveSetup(Player bossPlayer) {
        // Liste aller Spieler (ohne Boss wenn er nicht mitmacht)
        List<UUID> playersToSetup = new ArrayList<>(activeChallenge.getParticipants());
        if (!activeChallenge.isBossParticipates()) {
            playersToSetup.remove(bossPlayer.getUniqueId());
        }

        // Starte interaktiven Setup-Prozess
        bossSetupManager.startWaveSetup(bossPlayer, playersToSetup, () -> {
            // Setup abgeschlossen -> Starte Farmphase
            startFarmingPhase();
        });
    }

    /**
     * Farmphase: Spieler sammeln Ressourcen
     */
    private void startFarmingPhase() {
        activeChallenge.setCurrentPhase(Challenge.ChallengePhase.FARMING);

        // Setze Timer auf 0 und starte ihn
        DataManager dm = plugin.getDataManager();
        dm.setTimerTicks(0);
        dm.startTimer();

        // Speichere Start-Tick
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
            }
        }

        // Starte Farmzeit-Überwachung
        startFarmingTimer();
    }

    /**
     * Überwacht die Farmzeit und startet Combat-Phase wenn abgelaufen
     */
    private void startFarmingTimer() {
        Bukkit.getScheduler().runTaskTimer(plugin, (task) -> {
            DataManager dm = plugin.getDataManager();
            long currentTicks = dm.getTimerTicks();
            long targetTicks = activeChallenge.getFarmDurationTicks();

            // Prüfe ob Farmzeit abgelaufen
            if (currentTicks >= targetTicks) {
                task.cancel();
                startCombatPhase();
            }
        }, 20L, 20L); // Alle 1 Sekunde prüfen
    }

    /**
     * Combat-Phase: Spieler kämpfen in Arenen
     */
    private void startCombatPhase() {
        activeChallenge.setCurrentPhase(Challenge.ChallengePhase.COMBAT);
        activeChallenge.setPhaseStartTick(plugin.getDataManager().getTimerTicks());

        // Erstelle Arenen für alle Spieler
        arenaManager.createArenas(activeChallenge);

        // Teleportiere Spieler in Arenen und starte Waves
        for (UUID playerId : activeChallenge.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Speichere Inventar
                PlayerChallengeData data = activeChallenge.getPlayerData().get(playerId);
                data.backupInventory(player);

                // Teleportiere in Arena
                ArenaInstance arena = arenaManager.getArenaForPlayer(playerId);
                player.teleport(arena.getSpawnPoint());
                player.setGameMode(GameMode.SURVIVAL);

                player.sendMessage("§c§l=== KAMPFPHASE BEGINNT ===");

                // Starte erste Wave
                waveManager.startWave(player, 0);
            }
        }
    }

    /**
     * Wird aufgerufen wenn ein Spieler alle Waves abgeschlossen hat
     */
    public void onPlayerCompleted(UUID playerId) {
        PlayerChallengeData data = activeChallenge.getPlayerData().get(playerId);
        data.setHasCompleted(true);
        data.setCombatEndTick(plugin.getDataManager().getTimerTicks());

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage("§a§l=== DU HAST ALLE WAVES GESCHAFFT! ===");

            // Gib Spectator-Compass
            spectatorManager.giveSpectatorCompass(player);
            player.setGameMode(GameMode.SPECTATOR);
        }

        // Prüfe ob alle fertig sind
        checkChallengeCompletion();
    }

    /**
     * Wird aufgerufen wenn ein Spieler aufgibt
     */
    public void onPlayerForfeited(UUID playerId) {
        PlayerChallengeData data = activeChallenge.getPlayerData().get(playerId);
        data.setHasForfeited(true);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage("§c§lDu hast aufgegeben!");
            player.setGameMode(GameMode.SPECTATOR);
            spectatorManager.giveSpectatorCompass(player);
        }

        checkChallengeCompletion();
    }

    /**
     * Prüft ob Challenge beendet ist (alle durch oder aufgegeben)
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

        // Stoppe Timer
        plugin.getDataManager().pauseTimer();

        // Zeige Auswertung
        statisticsManager.showEvaluation(activeChallenge);

        // Cleanup nach 60 Sekunden
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cleanup();
        }, 1200L); // 60 Sekunden
    }

    /**
     * Räumt Challenge-Daten auf
     */
    private void cleanup() {
        // Teleportiere alle zurück zum Spawn
        for (UUID playerId : activeChallenge.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.teleport(player.getWorld().getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);

                // Restore Inventar
                PlayerChallengeData data = activeChallenge.getPlayerData().get(playerId);
                data.restoreInventory(player);
            }
        }

        // Entferne Arenen
        arenaManager.clearArenas();

        // Resette Challenge
        activeChallenge = null;
    }

    // Getter
    public Challenge getActiveChallenge() {
        return activeChallenge;
    }

    public boolean isChallengeActive() {
        return activeChallenge != null;
    }
}