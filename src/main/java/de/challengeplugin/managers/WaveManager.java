package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.*;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

/**
 * Verwaltet Wave-Ablauf für Teams
 * FIX: Mob-Tod wird jetzt unabhängig vom Killer getrackt
 */
public class WaveManager {

    private final ChallengePlugin plugin;

    // Aktive Bossbars pro Spieler
    private final Map<UUID, BossBar> playerBossbars = new HashMap<>();

    // Aktive Mobs pro Team
    private final Map<UUID, Set<UUID>> teamMobs = new HashMap<>();

    // Countdown-Tasks
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();

    // NEU: Tracking welches Mob zu welchem Team gehört
    private final Map<UUID, UUID> mobToTeam = new HashMap<>();

    public WaveManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Startet eine Wave für ein ganzes Team
     */
    public void startWaveForTeam(UUID teamId, int waveIndex) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;

        // Hole Wave-Daten für dieses Team
        List<Wave> waves = challenge.getTeamWaves().get(teamId);
        if (waves == null || waveIndex >= waves.size()) {
            plugin.getLogger().warning("Keine Wave-Daten für Team " + teamId);
            return;
        }

        Wave wave = waves.get(waveIndex);

        // Starte mit 30 Sekunden Countdown
        startWaveCountdownForTeam(teamId, wave, 30);
    }

    /**
     * Countdown für Team
     */
    private void startWaveCountdownForTeam(UUID teamId, Wave wave, int seconds) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        List<UUID> teamMembers = challenge.getTeamMembers(teamId);

        // Erstelle Bossbar für alle Team-Mitglieder
        BossBar bossbar = Bukkit.createBossBar(
                "§eWave " + wave.getWaveNumber() + " startet in...",
                BarColor.YELLOW,
                BarStyle.SOLID
        );

        for (UUID memberId : teamMembers) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                bossbar.addPlayer(player);
                playerBossbars.put(memberId, bossbar);

                player.sendMessage("§e§l=== Wave " + wave.getWaveNumber() + " ===");
                player.sendMessage("§7Vorbereitung: §e" + seconds + " Sekunden");
            }
        }

        // Countdown-Task
        BukkitRunnable runnable = new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    countdownTasks.remove(teamId);
                    spawnWaveMobsForTeam(teamId, wave);
                    cancel();
                    return;
                }

                bossbar.setProgress((double) remaining / seconds);
                bossbar.setTitle("§eWave " + wave.getWaveNumber() + " startet in §6" + remaining + "s");

                // Sound für alle Team-Mitglieder
                if (remaining == 10 || remaining == 5 || remaining <= 3) {
                    for (UUID memberId : teamMembers) {
                        Player player = Bukkit.getPlayer(memberId);
                        if (player != null) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                        }
                    }
                }

                remaining--;
            }
        };
        BukkitTask task = runnable.runTaskTimer(plugin, 0L, 20L);
        countdownTasks.put(teamId, task);
    }

    /**
     * Spawnt Mobs für Team
     */
    private void spawnWaveMobsForTeam(UUID teamId, Wave wave) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        List<UUID> teamMembers = challenge.getTeamMembers(teamId);

        // Hole Arena
        ArenaInstance arena = plugin.getChallengeManager().getArenaManager().getArenaForTeam(teamId);
        if (arena == null) return;

        // Erstelle Set für Mob-Tracking (PRO TEAM!)
        Set<UUID> mobIds = new HashSet<>();
        teamMobs.put(teamId, mobIds);

        // Spawne alle Mobs
        Location spawnLoc = arena.getSpawnPoint().clone().add(0, 2, 0);

        // Wähle zufälliges Team-Mitglied als initiales Aggro-Target
        Player randomMember = null;
        for (UUID memberId : teamMembers) {
            randomMember = Bukkit.getPlayer(memberId);
            if (randomMember != null) break;
        }

        for (EntityType mobType : wave.getMobs()) {
            Entity entity = randomMember.getWorld().spawnEntity(spawnLoc, mobType);
            if (entity instanceof Mob) {
                Mob mob = (Mob) entity;

                // Setze Aggro auf zufälliges Team-Mitglied
                if (randomMember != null) {
                    mob.setTarget(randomMember);
                }
                mob.setRemoveWhenFarAway(false);
                mob.setCanPickupItems(false);
                mob.setLootTable(null);

                UUID mobId = mob.getUniqueId();
                mobIds.add(mobId);
                arena.getSpawnedMobs().add(mobId);

                // NEU: Speichere Mob->Team Mapping
                mobToTeam.put(mobId, teamId);
            }
        }

        // Update Bossbar für alle Team-Mitglieder
        for (UUID memberId : teamMembers) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                updateBossbarForPlayer(player, wave, mobIds.size());
                player.sendMessage("§c§lWave " + wave.getWaveNumber() + " gestartet!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);

                // Starte Wave-Stats
                PlayerChallengeData data = challenge.getPlayerData().get(memberId);
                if (data != null) {
                    data.getWaveStats().put(wave.getWaveNumber() - 1, new PlayerChallengeData.WaveStats());
                    data.getWaveStats().get(wave.getWaveNumber() - 1).startTick =
                            plugin.getDataManager().getTimerTicks();
                }
            }
        }

        // NEU: Starte Mob-Überwachung (prüft ob Mobs noch existieren)
        startMobWatcher(teamId);
    }

    /**
     * NEU: Überwacht Mobs und prüft ob sie noch existieren
     * (Verhindert dass gestorbene Mobs die Wave blockieren)
     */
    private void startMobWatcher(UUID teamId) {
        BukkitRunnable watcher = new BukkitRunnable() {
            @Override
            public void run() {
                Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
                if (challenge == null) {
                    cancel();
                    return;
                }

                Set<UUID> mobs = teamMobs.get(teamId);
                if (mobs == null || mobs.isEmpty()) {
                    cancel();
                    return;
                }

                // Prüfe alle Mobs
                Set<UUID> deadMobs = new HashSet<>();
                for (UUID mobId : mobs) {
                    Entity entity = Bukkit.getEntity(mobId);
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        deadMobs.add(mobId);
                    }
                }

                // Entferne tote Mobs
                for (UUID deadMob : deadMobs) {
                    handleMobDeath(teamId, deadMob);
                }
            }
        };
        watcher.runTaskTimer(plugin, 20L, 20L); // Alle 1 Sekunde prüfen
    }

    /**
     * FIX: Mob stirbt (Team-basiert, UNABHÄNGIG vom Killer!)
     */
    public void onMobDeath(UUID killerId, UUID mobId) {
        // Finde Team über Mob-Mapping
        UUID teamId = mobToTeam.get(mobId);
        if (teamId == null) return;

        handleMobDeath(teamId, mobId);
    }

    /**
     * NEU: Zentrale Methode für Mob-Tod-Handling
     */
    private void handleMobDeath(UUID teamId, UUID mobId) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;

        Set<UUID> mobs = teamMobs.get(teamId);
        if (mobs == null || !mobs.remove(mobId)) return; // Bereits entfernt

        // Entferne aus Mapping
        mobToTeam.remove(mobId);

        // Update für alle Team-Mitglieder
        List<UUID> teamMembers = challenge.getTeamMembers(teamId);
        for (UUID memberId : teamMembers) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                PlayerChallengeData data = challenge.getPlayerData().get(memberId);
                if (data != null) {
                    int currentWave = data.getCurrentWaveIndex();
                    if (data.getWaveStats().containsKey(currentWave)) {
                        data.getWaveStats().get(currentWave).mobsKilled++;
                    }
                }

                // Update Bossbar
                List<Wave> waves = challenge.getTeamWaves().get(teamId);
                if (waves != null && data.getCurrentWaveIndex() < waves.size()) {
                    Wave currentWave = waves.get(data.getCurrentWaveIndex());
                    updateBossbarForPlayer(player, currentWave, mobs.size());
                }
            }
        }

        // Prüfe ob Wave abgeschlossen
        if (mobs.isEmpty()) {
            onWaveCompletedForTeam(teamId);
        }
    }

    /**
     * Wave für Team abgeschlossen
     */
    private void onWaveCompletedForTeam(UUID teamId) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        List<UUID> teamMembers = challenge.getTeamMembers(teamId);

        // Hole aktuellen Wave-Index
        int waveIndex = 0;
        for (UUID memberId : teamMembers) {
            PlayerChallengeData data = challenge.getPlayerData().get(memberId);
            if (data != null) {
                waveIndex = data.getCurrentWaveIndex();
                break;
            }
        }

        // Update für alle Team-Mitglieder
        for (UUID memberId : teamMembers) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                PlayerChallengeData data = challenge.getPlayerData().get(memberId);
                if (data != null) {
                    data.getWaveStats().get(waveIndex).endTick = plugin.getDataManager().getTimerTicks();
                    data.setCurrentWaveIndex(waveIndex + 1);
                }
                player.sendMessage("§a§l✓ Wave " + (waveIndex + 1) + " abgeschlossen!");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        // NEU: Hole tatsächliche Wave-Anzahl aus den Team-Waves
        List<Wave> teamWaves = challenge.getTeamWaves().get(teamId);
        int totalWaves = teamWaves != null ? teamWaves.size() : 3;

        // Nächste Wave oder fertig?
        if (waveIndex + 1 < totalWaves) {
            startWaveForTeam(teamId, waveIndex + 1);
        } else {
            // Team hat alle Waves geschafft!
            for (UUID memberId : teamMembers) {
                plugin.getChallengeManager().onPlayerCompleted(memberId);
                BossBar bar = playerBossbars.remove(memberId);
                if (bar != null) {
                    bar.removeAll();
                }
            }
        }
    }

    /**
     * Update Bossbar für einzelnen Spieler
     */
    private void updateBossbarForPlayer(Player player, Wave wave, int remainingMobs) {
        BossBar bar = playerBossbars.get(player.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar(
                    "Wave",
                    BarColor.RED,
                    BarStyle.SOLID
            );
            bar.addPlayer(player);
            playerBossbars.put(player.getUniqueId(), bar);
        }

        int totalMobs = wave.getTotalMobCount();
        double progress = (double) remainingMobs / totalMobs;

        // NEU: Zeige totale Wave-Anzahl dynamisch
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        UUID teamId = challenge.getTeamOfPlayer(player.getUniqueId());
        List<Wave> teamWaves = challenge.getTeamWaves().get(teamId);
        int totalWaves = teamWaves != null ? teamWaves.size() : 3;

        bar.setTitle("§cWave " + wave.getWaveNumber() + "§7/§c" + totalWaves + " §7- §e" + remainingMobs + "§7/§e" + totalMobs + " Mobs");
        bar.setProgress(Math.max(0.01, progress));
        bar.setColor(BarColor.RED);
    }

    /**
     * Räumt Daten für ein Team auf
     */
    public void cleanupTeam(UUID teamId) {
        // Stoppe Team-Countdown
        BukkitTask task = countdownTasks.remove(teamId);
        if (task != null) {
            task.cancel();
        }

        // Entferne Team-Mobs
        Set<UUID> mobs = teamMobs.remove(teamId);
        if (mobs != null) {
            for (UUID mobId : mobs) {
                mobToTeam.remove(mobId); // Cleanup Mapping
                Entity entity = Bukkit.getEntity(mobId);
                if (entity != null) {
                    entity.remove();
                }
            }
        }

        Bukkit.getLogger().info("[WaveManager] Team-Cleanup durchgeführt für: " + teamId);
    }

    /**
     * Räumt für einzelnen Spieler auf (Legacy-Support)
     */
    public void cleanup(UUID playerId) {
        // Entferne Bossbar
        BossBar bar = playerBossbars.remove(playerId);
        if (bar != null) {
            bar.removeAll();
        }
    }

    /**
     * Räumt ALLES auf (für Challenge-Ende)
     */
    public void cleanupAll() {
        // Entferne alle Bossbars
        for (BossBar bar : new ArrayList<>(playerBossbars.values())) {
            bar.removeAll();
        }
        playerBossbars.clear();

        // Stoppe alle Tasks
        for (BukkitTask task : new ArrayList<>(countdownTasks.values())) {
            task.cancel();
        }
        countdownTasks.clear();

        // Entferne alle Mobs
        for (Set<UUID> mobSet : new ArrayList<>(teamMobs.values())) {
            for (UUID mobId : mobSet) {
                mobToTeam.remove(mobId);
                Entity entity = Bukkit.getEntity(mobId);
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        teamMobs.clear();
        mobToTeam.clear();

        Bukkit.getLogger().info("[WaveManager] Vollständiges Cleanup durchgeführt");
    }
}