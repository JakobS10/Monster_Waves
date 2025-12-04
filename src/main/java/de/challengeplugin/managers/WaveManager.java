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
    /**
     * NEU: Startet eine Wave für ein ganzes Team
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

        // Wenn nicht erste Wave, starte Countdown
        if (waveIndex > 0) {
            startWaveCountdownForTeam(teamId, wave, 30);
        } else {
            spawnWaveMobsForTeam(teamId, wave);
        }
    }

    /**
     * NEU: Countdown für Team
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

     NEU: Spawnt Mobs für Team
     */
    private void spawnWaveMobsForTeam(UUID teamId, Wave wave) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        List<UUID> teamMembers = challenge.getTeamMembers(teamId);
// Hole Arena
        ArenaInstance arena = plugin.getChallengeManager().getArenaManager().getArenaForTeam(teamId);
        if (arena == null) return;
// Erstelle Set für Mob-Tracking (PRO TEAM, nicht pro Spieler!)
        Set<UUID> mobIds = new HashSet<>();
        playerMobs.put(teamId, mobIds); // Speichere unter Team-ID!
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

                mobIds.add(mob.getUniqueId());
                arena.getSpawnedMobs().add(mob.getUniqueId());
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
    }

    /**

     ANGEPASST: Mob stirbt (Team-basiert)
     */
    public void onMobDeath(UUID killerId, UUID mobId) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
// Finde Team des Killers
        UUID teamId = challenge.getTeamOfPlayer(killerId);
        if (teamId == null) return;
        Set<UUID> mobs = playerMobs.get(teamId); // Team-Mobs, nicht Spieler-Mobs!
        if (mobs == null) return;
        mobs.remove(mobId);
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

     NEU: Wave für Team abgeschlossen
     */
    private void onWaveCompletedForTeam(UUID teamId) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        List<UUID> teamMembers = challenge.getTeamMembers(teamId);
// Hole aktuellen Wave-Index (von erstem Team-Mitglied)
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
// Nächste Wave oder fertig?
        if (waveIndex + 1 < 3) {
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

     Hilfsmethode: Update Bossbar für einzelnen Spieler
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
        bar.setTitle("§cWave " + wave.getWaveNumber() + " §7- §e" + remainingMobs + "§7/§e" + totalMobs + " Mobs");
        bar.setProgress(Math.max(0.01, progress));
        bar.setColor(BarColor.RED);
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