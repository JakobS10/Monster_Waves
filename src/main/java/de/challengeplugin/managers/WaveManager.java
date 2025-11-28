package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.*;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

/**
 * Verwaltet Wave-Ablauf für einzelne Spieler
 * Spawnt Mobs, trackt Kills, zeigt Bossbar an
 */
public class WaveManager {

    private final ChallengePlugin plugin;

    // Aktive Bossbars pro Spieler
    private final Map<UUID, BossBar> playerBossbars = new HashMap<>();

    // Aktive Mobs pro Spieler
    private final Map<UUID, Set<UUID>> playerMobs = new HashMap<>();

    // Countdown-Tasks
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();

    public WaveManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Startet eine Wave für einen Spieler
     * @param player Der Spieler
     * @param waveIndex Wave-Index (0, 1 oder 2)
     */
    public void startWave(Player player, int waveIndex) {
        UUID playerId = player.getUniqueId();
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge == null) return;

        // Hole Wave-Daten
        List<Wave> waves = challenge.getPlayerWaves().get(playerId);
        if (waves == null || waveIndex >= waves.size()) {
            plugin.getLogger().warning("Keine Wave-Daten für " + player.getName());
            return;
        }

        Wave wave = waves.get(waveIndex);

        // Wenn nicht erste Wave, starte Countdown
        if (waveIndex > 0) {
            startWaveCountdown(player, wave, 30); // 30 Sekunden Countdown
        } else {
            // Erste Wave sofort starten
            spawnWaveMobs(player, wave);
        }
    }

    /**
     * Startet Countdown zwischen Waves
     */
    private void startWaveCountdown(Player player, Wave wave, int seconds) {
        UUID playerId = player.getUniqueId();

        // Erstelle Bossbar für Countdown
        BossBar bossbar = createOrGetBossbar(player);
        bossbar.setTitle("§eWave " + wave.getWaveNumber() + " startet in...");
        bossbar.setColor(BarColor.YELLOW);
        bossbar.setProgress(1.0);

        player.sendMessage("§e§l=== Wave " + wave.getWaveNumber() + " ===");
        player.sendMessage("§7Vorbereitung: §e" + seconds + " Sekunden");

        // Countdown-Task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    // Countdown beendet, starte Wave
                    countdownTasks.remove(playerId);
                    spawnWaveMobs(player, wave);
                    return;
                }

                // Update Bossbar
                bossbar.setProgress((double) remaining / seconds);
                bossbar.setTitle("§eWave " + wave.getWaveNumber() + " startet in §6" + remaining + "s");

                // Sound bei 10, 5, 3, 2, 1
                if (remaining <= 10 && remaining <= 5 || remaining <= 3) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }

                remaining--;
            }
        }, 0L, 20L); // Jede Sekunde

        countdownTasks.put(playerId, task);
    }

    /**
     * Spawnt alle Mobs einer Wave
     */
    private void spawnWaveMobs(Player player, Wave wave) {
        UUID playerId = player.getUniqueId();
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        PlayerChallengeData data = challenge.getPlayerData().get(playerId);

        // Hole Arena
        ArenaManager arenaManager = plugin.getChallengeManager().getArenaManager();
        ArenaInstance arena = arenaManager.getArenaForPlayer(playerId);

        if (arena == null) {
            plugin.getLogger().warning("Keine Arena für " + player.getName());
            return;
        }

        // Erstelle Set für Mob-Tracking
        Set<UUID> mobIds = new HashSet<>();
        playerMobs.put(playerId, mobIds);

        // Spawne alle Mobs
        Location spawnLoc = arena.getSpawnPoint().clone().add(0, 2, 0);

        for (EntityType mobType : wave.getMobs()) {
            // Spawne Mob
            Entity entity = player.getWorld().spawnEntity(spawnLoc, mobType);

            if (entity instanceof Mob) {
                Mob mob = (Mob) entity;

                // Setze Aggro auf Spieler
                mob.setTarget(player);
                mob.setRemoveWhenFarAway(false);
                mob.setCanPickupItems(false);

                // Verhindere Drops (Loot-Tabelle leeren)
                mob.setLootTable(null);

                // Speichere Mob-ID
                mobIds.add(mob.getUniqueId());
                arena.getSpawnedMobs().add(mob.getUniqueId());
            }
        }

        // Update Bossbar zu Mob-Count
        updateBossbar(player, wave, mobIds.size());

        // Starte Wave-Stats-Tracking
        data.getWaveStats().put(wave.getWaveNumber() - 1, new PlayerChallengeData.WaveStats());
        data.getWaveStats().get(wave.getWaveNumber() - 1).startTick =
                plugin.getDataManager().getTimerTicks();

        player.sendMessage("§c§lWave " + wave.getWaveNumber() + " gestartet!");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
    }

    /**
     * Wird aufgerufen wenn ein Mob stirbt
     */
    public void onMobDeath(UUID playerId, UUID mobId) {
        Set<UUID> mobs = playerMobs.get(playerId);
        if (mobs == null) return;

        mobs.remove(mobId);

        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        PlayerChallengeData data = challenge.getPlayerData().get(playerId);

        // Update Stats
        int currentWave = data.getCurrentWaveIndex();
        if (data.getWaveStats().containsKey(currentWave)) {
            data.getWaveStats().get(currentWave).mobsKilled++;
        }

        // Update Bossbar
        List<Wave> waves = challenge.getPlayerWaves().get(playerId);
        Wave currentWaveObj = waves.get(currentWave);
        updateBossbar(player, currentWaveObj, mobs.size());

        // Prüfe ob Wave abgeschlossen
        if (mobs.isEmpty()) {
            onWaveCompleted(player, currentWave);
        }
    }

    /**
     * Wird aufgerufen wenn eine Wave abgeschlossen ist
     */
    private void onWaveCompleted(Player player, int waveIndex) {
        UUID playerId = player.getUniqueId();
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        PlayerChallengeData data = challenge.getPlayerData().get(playerId);

        // Speichere Wave-End-Zeit
        data.getWaveStats().get(waveIndex).endTick = plugin.getDataManager().getTimerTicks();

        player.sendMessage("§a§l✓ Wave " + (waveIndex + 1) + " abgeschlossen!");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Nächste Wave oder fertig?
        data.setCurrentWaveIndex(waveIndex + 1);

        if (waveIndex + 1 < 3) {
            // Nächste Wave starten
            startWave(player, waveIndex + 1);
        } else {
            // Alle Waves geschafft!
            plugin.getChallengeManager().onPlayerCompleted(playerId);

            // Entferne Bossbar
            BossBar bar = playerBossbars.remove(playerId);
            if (bar != null) {
                bar.removeAll();
            }
        }
    }

    /**
     * Erstellt oder holt Bossbar für Spieler
     */
    private BossBar createOrGetBossbar(Player player) {
        UUID playerId = player.getUniqueId();

        if (playerBossbars.containsKey(playerId)) {
            return playerBossbars.get(playerId);
        }

        BossBar bar = Bukkit.createBossBar(
                "Wave",
                BarColor.RED,
                BarStyle.SOLID
        );
        bar.addPlayer(player);
        playerBossbars.put(playerId, bar);

        return bar;
    }

    /**
     * Aktualisiert Bossbar mit Mob-Count
     */
    private void updateBossbar(Player player, Wave wave, int remainingMobs) {
        BossBar bar = createOrGetBossbar(player);

        int totalMobs = wave.getTotalMobCount();
        double progress = (double) remainingMobs / totalMobs;

        bar.setTitle("§cWave " + wave.getWaveNumber() + " §7- §e" + remainingMobs + "§7/§e" + totalMobs + " Mobs");
        bar.setProgress(Math.max(0.01, progress)); // Minimum 1% für Sichtbarkeit
        bar.setColor(BarColor.RED);
    }

    /**
     * Räumt Daten für einen Spieler auf
     */
    public void cleanup(UUID playerId) {
        // Stoppe Countdown-Task
        BukkitTask task = countdownTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        // Entferne Bossbar
        BossBar bar = playerBossbars.remove(playerId);
        if (bar != null) {
            bar.removeAll();
        }

        // Entferne verbleibende Mobs
        Set<UUID> mobs = playerMobs.remove(playerId);
        if (mobs != null) {
            for (UUID mobId : mobs) {
                Entity entity = Bukkit.getEntity(mobId);
                if (entity != null) {
                    entity.remove();
                }
            }
        }
    }
}