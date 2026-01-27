package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Haupt-Manager für die Challenge-Logik
 * ERWEITERT: Mit vollständigen Restore-Methoden für Server-Neustarts
 * FIX: Neue teleportPlayerToTeamArena Methode für Late-Joining
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
    private final TeamColorManager teamColorManager;
    private final TeamBackpackManager backpackManager;

    public ChallengeManager(ChallengePlugin plugin) {
        this.plugin = plugin;
        this.arenaManager = new ArenaManager(plugin);
        this.waveManager = new WaveManager(plugin);
        this.bossSetupManager = new BossSetupManager(plugin);
        this.statisticsManager = new StatisticsManager(plugin);
        this.spectatorManager = new SpectatorManager(plugin);
        this.teamColorManager = new TeamColorManager(plugin);
        this.backpackManager = new TeamBackpackManager(plugin);
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

        // Setup Team-Colors NACH Team-Erstellung
        teamColorManager.setupTeamColors(activeChallenge);

        // Erstelle Backpacks für alle Teams
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
     * NEU: Teleportiert Spieler in die Arena seines Teams
     * Wird für Late-Joining während Combat-Phase genutzt
     */
    public void teleportPlayerToTeamArena(Player player, UUID teamId) {
        if (activeChallenge == null) {
            player.sendMessage("§cKeine aktive Challenge!");
            return;
        }

        // Hole Arena für Team
        ArenaInstance arena = arenaManager.getArenaForTeam(teamId);
        if (arena == null) {
            player.sendMessage("§cKeine Arena für dein Team gefunden!");
            plugin.getLogger().warning("[ChallengeManager] Keine Arena für Team " + teamId + " gefunden!");
            return;
        }

        Location spawnPoint = arena.getSpawnPoint();

        // Speichere Inventar
        PlayerChallengeData data = activeChallenge.getPlayerData().get(player.getUniqueId());
        if (data != null) {
            data.backupInventory(player);
            data.setCombatStartTick(plugin.getDataManager().getTimerTicks());
        }

        // Teleportiere
        player.teleport(spawnPoint);
        player.setGameMode(GameMode.SURVIVAL);

        player.sendMessage("§c§l=== KAMPFPHASE ===");
        player.sendMessage("§7Du wurdest in die Arena teleportiert!");
        player.sendMessage("");

        // Zeige aktuelle Wave-Info
        if (data != null) {
            List<Wave> teamWaves = activeChallenge.getTeamWaves().get(teamId);
            int currentWave = data.getCurrentWaveIndex();
            int totalWaves = teamWaves != null ? teamWaves.size() : 3;

            player.sendMessage("§7Aktuelle Wave: §e" + (currentWave + 1) + "§7/§e" + totalWaves);
            player.sendMessage("");
            player.sendMessage("§eViel Erfolg!");
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        plugin.getLogger().info("[ChallengeManager] Spieler " + player.getName() +
                " in Team-Arena teleportiert (Late-Join)");
    }

    /**
     * ========================================
     * RESTORE-METHODEN FÜR SERVER-NEUSTART
     * ========================================
     */

    /**
     * Stellt Challenge nach Server-Neustart wieder her
     * Hauptmethode die vom DataManager aufgerufen wird
     */
    public void restoreChallenge(Challenge challenge) {
        this.activeChallenge = challenge;

        plugin.getLogger().info("§6§l=================================");
        plugin.getLogger().info("§e§lChallenge wird wiederhergestellt...");
        plugin.getLogger().info("§e§lPhase: " + challenge.getCurrentPhase());
        plugin.getLogger().info("§6§l=================================");

        Bukkit.broadcastMessage("§6§l=================================");
        Bukkit.broadcastMessage("§e§lChallenge-Wiederherstellung");
        Bukkit.broadcastMessage("§7Der Server wurde neu gestartet");
        Bukkit.broadcastMessage("§6§l=================================");

        // Wiederherstellung basierend auf aktueller Phase
        switch (challenge.getCurrentPhase()) {
            case SETUP:
                handleSetupPhaseRestore();
                break;

            case FARMING:
                restoreFarmingPhase(challenge);
                break;

            case COMBAT:
                restoreCombatPhase(challenge);
                break;

            case EVALUATION:
                handleEvaluationPhaseRestore();
                break;
        }
    }

    /**
     * Setup-Phase kann nicht wiederhergestellt werden (zu komplex)
     */
    private void handleSetupPhaseRestore() {
        plugin.getLogger().warning("§cSetup-Phase kann nicht wiederhergestellt werden!");
        Bukkit.broadcastMessage("§cSetup-Phase kann nicht wiederhergestellt werden!");
        Bukkit.broadcastMessage("§7Die Challenge wurde abgebrochen.");
        Bukkit.broadcastMessage("§7Bitte starte eine neue Challenge mit /challenge start");

        cleanup();
    }

    /**
     * Evaluation-Phase: Challenge war bereits fertig
     */
    private void handleEvaluationPhaseRestore() {
        plugin.getLogger().info("§7Challenge war bereits in Auswertungs-Phase");
        Bukkit.broadcastMessage("§7Die Challenge war bereits beendet.");
        Bukkit.broadcastMessage("§7Auswertung wird übersprungen.");

        cleanup();
    }

    /**
     * Stellt Farming-Phase wieder her
     */
    private void restoreFarmingPhase(Challenge challenge) {
        plugin.getLogger().info("§aFarming-Phase wird wiederhergestellt!");
        Bukkit.broadcastMessage("§a§lFarming-Phase wird wiederhergestellt!");

        // Setup Team-Colors
        plugin.getLogger().info("[Restore] Setup Team-Colors...");
        teamColorManager.setupTeamColors(challenge);

        // Erstelle Backpacks
        plugin.getLogger().info("[Restore] Erstelle Backpacks...");
        backpackManager.createBackpacks(challenge);

        // NEU: Lade Backpack-Items aus data.yml und füge sie ein!
        Map<UUID, List<ItemStack>> backpackItems = plugin.getDataManager().loadBackpackItems();
        if (!backpackItems.isEmpty()) {
            backpackManager.restoreBackpackItems(backpackItems);
            plugin.getLogger().info("[Restore] Backpack-Items wiederhergestellt: " + backpackItems.size() + " Teams");
        }

        // Timer fortsetzen (läuft bereits vom DataManager)
        DataManager dm = plugin.getDataManager();
        if (!dm.isTimerRunning()) {
            dm.startTimer();
            plugin.getLogger().info("[Restore] Timer wurde wieder gestartet");
        }

        // Informiere Spieler und restore Inventare
        int onlineCount = 0;
        int restoredCount = 0;

        for (UUID playerId : challenge.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                onlineCount++;

                player.sendMessage("§a§l=== CHALLENGE WIEDERHERGESTELLT ===");
                player.sendMessage("§7Der Server wurde neu gestartet");
                player.sendMessage("§aFarmphase läuft weiter!");
                player.sendMessage("");
                player.sendMessage("§7Verbleibende Zeit: §e" + getRemainingFarmTime());
                player.sendMessage("§6Nutze §e/backpack §6oder §e/bp §6für deinen Team-Backpack!");

                // Restore Inventar
                PlayerChallengeData data = challenge.getPlayerData().get(playerId);
                if (data != null && data.getInventoryBackup() != null) {
                    data.restoreInventory(player);
                    restoredCount++;
                    plugin.getLogger().info("[Restore] Inventar wiederhergestellt für: " + player.getName());
                }

                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        plugin.getLogger().info("[Restore] Spieler online: " + onlineCount + "/" + challenge.getParticipants().size());
        plugin.getLogger().info("[Restore] Inventare wiederhergestellt: " + restoredCount);

        // Starte Farming-Timer neu
        startFarmingTimer();

        Bukkit.broadcastMessage("§a§lFarming-Phase erfolgreich wiederhergestellt!");
        plugin.getLogger().info("§aFarming-Phase erfolgreich wiederhergestellt!");
    }

    /**
     * Stellt Combat-Phase wieder her
     */
    private void restoreCombatPhase(Challenge challenge) {
        plugin.getLogger().info("§cKampf-Phase wird wiederhergestellt!");
        Bukkit.broadcastMessage("§c§lKampf-Phase wird wiederhergestellt!");

        // Setup Team-Colors
        plugin.getLogger().info("[Restore] Setup Team-Colors...");
        teamColorManager.setupTeamColors(challenge);

        // Erstelle Backpacks
        plugin.getLogger().info("[Restore] Erstelle Backpacks...");
        backpackManager.createBackpacks(challenge);

        // NEU: Lade Backpack-Items aus data.yml und füge sie ein!
        Map<UUID, List<ItemStack>> backpackItems = plugin.getDataManager().loadBackpackItems();
        if (!backpackItems.isEmpty()) {
            backpackManager.restoreBackpackItems(backpackItems);
            plugin.getLogger().info("[Restore] Backpack-Items wiederhergestellt: " + backpackItems.size() + " Teams");
        }

        // Erstelle Arenen NEU (Mobs sind nach Neustart weg)
        plugin.getLogger().info("[Restore] Erstelle Arenen neu...");
        arenaManager.createArenasForTeams(challenge);

        // Timer fortsetzen
        DataManager dm = plugin.getDataManager();
        if (!dm.isTimerRunning()) {
            dm.startTimer();
            plugin.getLogger().info("[Restore] Timer wurde wieder gestartet");
        }

        int teamsRestored = 0;
        int playersRestored = 0;
        int spectatorsRestored = 0;

        // Teleportiere Spieler zurück in Arenen und starte Waves
        for (Map.Entry<UUID, List<UUID>> entry : challenge.getTeams().entrySet()) {
            UUID teamId = entry.getKey();
            List<UUID> teamMembers = entry.getValue();

            ArenaInstance arena = arenaManager.getArenaForTeam(teamId);
            if (arena == null) {
                plugin.getLogger().warning("[Restore] Arena nicht gefunden für Team: " + teamId);
                continue;
            }

            Location spawnPoint = arena.getSpawnPoint();
            int teamCurrentWave = -1;
            boolean teamHasActivePlayers = false;

            // Teleportiere Team-Mitglieder
            for (UUID playerId : teamMembers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null) continue;

                PlayerChallengeData data = challenge.getPlayerData().get(playerId);
                if (data == null) {
                    plugin.getLogger().warning("[Restore] Keine PlayerData für: " + player.getName());
                    continue;
                }

                // Prüfe Status des Spielers
                if (data.isHasCompleted()) {
                    // Spieler hat Challenge abgeschlossen
                    spectatorManager.enableSpectatorMode(player);
                    player.sendMessage("§a§l=== CHALLENGE WIEDERHERGESTELLT ===");
                    player.sendMessage("§aDu hattest die Challenge bereits abgeschlossen!");
                    player.sendMessage("§7Du bist im Spectator-Modus");
                    spectatorsRestored++;

                } else if (data.isHasForfeited()) {
                    // Spieler hat aufgegeben
                    spectatorManager.enableSpectatorMode(player);
                    player.sendMessage("§c§l=== CHALLENGE WIEDERHERGESTELLT ===");
                    player.sendMessage("§cDu hattest aufgegeben");
                    player.sendMessage("§7Du bist im Spectator-Modus");
                    spectatorsRestored++;

                } else {
                    // Spieler kämpft noch
                    teamHasActivePlayers = true;

                    // Teleportiere in Arena
                    player.teleport(spawnPoint);
                    player.setGameMode(GameMode.SURVIVAL);

                    // Restore Inventar
                    if (data.getInventoryBackup() != null) {
                        data.restoreInventory(player);
                        plugin.getLogger().info("[Restore] Inventar wiederhergestellt für: " + player.getName());
                    }

                    // Bestimme aktuelle Wave (minimum aller Team-Mitglieder)
                    if (teamCurrentWave == -1 || data.getCurrentWaveIndex() < teamCurrentWave) {
                        teamCurrentWave = data.getCurrentWaveIndex();
                    }

                    // Informiere Spieler
                    player.sendMessage("§c§l=== KAMPF WIEDERHERGESTELLT ===");
                    player.sendMessage("§7Der Server wurde neu gestartet");
                    player.sendMessage("");
                    player.sendMessage("§7Dein Team: §e" + getTeamMemberNames(teamMembers));
                    player.sendMessage("§7Aktuelle Wave: §e" + (data.getCurrentWaveIndex() + 1));
                    player.sendMessage("§7Tode: §c" + data.getTotalDeaths());
                    player.sendMessage("");
                    player.sendMessage("§eWave wird neu gestartet...");
                    player.sendMessage("§6Nutze §e/backpack §6für deinen Team-Backpack!");

                    playersRestored++;
                }
            }

            // Starte Wave NEU für dieses Team (wenn noch Spieler aktiv)
            if (teamHasActivePlayers && teamCurrentWave >= 0) {
                plugin.getLogger().info("[Restore] Starte Wave " + (teamCurrentWave + 1) + " für Team " + (teamsRestored + 1));

                // Verzögerung von 5 Sekunden damit Spieler sich orientieren können
                final int waveIndex = teamCurrentWave;
                final UUID finalTeamId = teamId;

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    waveManager.startWaveForTeam(finalTeamId, waveIndex);
                }, 100L); // 5 Sekunden

                teamsRestored++;
            } else if (!teamHasActivePlayers) {
                plugin.getLogger().info("[Restore] Team hat keine aktiven Spieler mehr (alle fertig/aufgegeben)");
            }
        }

        // Log Statistiken
        plugin.getLogger().info("[Restore] Teams wiederhergestellt: " + teamsRestored + "/" + challenge.getTeams().size());
        plugin.getLogger().info("[Restore] Kämpfende Spieler: " + playersRestored);
        plugin.getLogger().info("[Restore] Spectators: " + spectatorsRestored);

        Bukkit.broadcastMessage("§a§lKampf-Phase erfolgreich wiederhergestellt!");
        Bukkit.broadcastMessage("§7Kämpfende Spieler: §e" + playersRestored);
        Bukkit.broadcastMessage("§7Spectators: §e" + spectatorsRestored);

        plugin.getLogger().info("§aKampf-Phase erfolgreich wiederhergestellt!");
    }

    /**
     * Berechnet verbleibende Farmzeit
     */
    private String getRemainingFarmTime() {
        if (activeChallenge == null) return "0:00";

        DataManager dm = plugin.getDataManager();
        long currentTicks = dm.getTimerTicks();
        long targetTicks = activeChallenge.getFarmDurationTicks();
        long remainingTicks = targetTicks - currentTicks;

        if (remainingTicks <= 0) return "0:00";

        long seconds = remainingTicks / 20;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%d:%02d", minutes, seconds);
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
            spectatorManager.enableSpectatorMode(player);
        }

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

        spectatorManager.cleanup();
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
                player.setInvisible(false);

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

    public TeamColorManager getTeamColorManager() {
        return teamColorManager;
    }

    public TeamBackpackManager getBackpackManager() {
        return backpackManager;
    }

    public long getChallengeDurationTicks() {
        return activeChallenge != null ? activeChallenge.getFarmDurationTicks() : 0;
    }

    public void completed() {
        if (activeChallenge != null) {
            endChallenge();
        }
    }
}