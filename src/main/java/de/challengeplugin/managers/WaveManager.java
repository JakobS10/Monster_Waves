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
 * ERWEITERT:
 * - Spectators sehen Bossbars der Spieler die sie beobachten
 * - Bossbars werden nach Wave-Complete gecleaned
 * - Mobs spawnen progressiv mit Verzögerung
 * - Spawn-Points werden verteilt
 */
public class WaveManager {

    private final ChallengePlugin plugin;
    private final Map<UUID, BossBar> playerBossbars = new HashMap<>();
    private final Map<UUID, Set<UUID>> teamMobs = new HashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<UUID, UUID> mobToTeam = new HashMap<>();

    // NEU: Tracking welche Spectators welche Bossbars sehen
    private final Map<UUID, Set<UUID>> spectatorBossbars = new HashMap<>(); // Spectator -> Set<BossBar Owner>

    // NEU: Spawn-Tasks für progressive Spawns
    private final Map<UUID, BukkitTask> spawnTasks = new HashMap<>();

    public WaveManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Startet eine Wave für ein ganzes Team
     */
    public void startWaveForTeam(UUID teamId, int waveIndex) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;

        List<Wave> waves = challenge.getTeamWaves().get(teamId);
        if (waves == null || waveIndex >= waves.size()) {
            plugin.getLogger().warning("Keine Wave-Daten für Team " + teamId);
            return;
        }

        Wave wave = waves.get(waveIndex);
        startWaveCountdownForTeam(teamId, wave, 30);
    }

    /**
     * Countdown für Team
     */
    private void startWaveCountdownForTeam(UUID teamId, Wave wave, int seconds) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
        List<UUID> teamMembers = challenge.getTeamMembers(teamId);

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
     * Spawnt Mobs PROGRESSIV mit verteilten Spawn-Points
     */
    private void spawnWaveMobsForTeam(UUID teamId, Wave wave) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
        List<UUID> teamMembers = challenge.getTeamMembers(teamId);

        ArenaInstance arena = plugin.getChallengeManager().getArenaManager().getArenaForTeam(teamId);
        if (arena == null) return;

        Set<UUID> mobIds = new HashSet<>();
        teamMobs.put(teamId, mobIds);

        // Wähle zufälliges Team-Mitglied
        Player randomMember = null;
        for (UUID memberId : teamMembers) {
            randomMember = Bukkit.getPlayer(memberId);
            if (randomMember != null) break;
        }

        if (randomMember == null) return;

        final Player targetPlayer = randomMember;

        // Update Bossbar für alle
        for (UUID memberId : teamMembers) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                updateBossbarForPlayer(player, wave, wave.getTotalMobCount());
                player.sendMessage("§c§lWave " + wave.getWaveNumber() + " gestartet!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);

                PlayerChallengeData data = challenge.getPlayerData().get(memberId);
                if (data != null) {
                    data.getWaveStats().put(wave.getWaveNumber() - 1, new PlayerChallengeData.WaveStats());
                    Objects.requireNonNull(data.getWaveStats().get(wave.getWaveNumber() - 1)).startTick =
                            plugin.getDataManager().getTimerTicks();
                }
            }
        }

        // Spawne Mobs PROGRESSIV (alle 2 Sekunden 5 Mobs)
        List<EntityType> mobsToSpawn = new ArrayList<>(wave.getMobs());
        final int[] spawnedCount = {0};

        BukkitRunnable spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                int batchSize = Math.min(5, mobsToSpawn.size() - spawnedCount[0]);

                if (batchSize <= 0) {
                    spawnTasks.remove(teamId);
                    cancel();
                    return;
                }

                for (int i = 0; i < batchSize; i++) {
                    EntityType mobType = mobsToSpawn.get(spawnedCount[0] + i);

                    // Verteile Spawn-Points
                    Location spawnLoc = getRandomSpawnPoint(arena);

                    Entity entity = targetPlayer.getWorld().spawnEntity(spawnLoc, mobType);
                    if (entity instanceof Mob mob) {

                        mob.setTarget(targetPlayer);
                        mob.setRemoveWhenFarAway(false);
                        mob.setCanPickupItems(false);
                        mob.setLootTable(null);

                        UUID mobId = mob.getUniqueId();
                        mobIds.add(mobId);
                        arena.getSpawnedMobs().add(mobId);
                        mobToTeam.put(mobId, teamId);
                    }
                }

                spawnedCount[0] += batchSize;

                // Update Bossbar
                for (UUID memberId : teamMembers) {
                    Player player = Bukkit.getPlayer(memberId);
                    if (player != null) {
                        updateBossbarForPlayer(player, wave, mobIds.size());
                    }
                }
            }
        };

        BukkitTask task = spawnTask.runTaskTimer(plugin, 0L, 40L); // Alle 2 Sekunden
        spawnTasks.put(teamId, task);

        startMobWatcher(teamId);
    }

    /**
     * Gibt zufälligen Spawn-Point innerhalb der Arena zurück
     */
    private Location getRandomSpawnPoint(ArenaInstance arena) {
        Location center = arena.getCenterLocation();
        Random random = new Random();

        // Spawn in Radius von 10 Blöcken um Center
        double offsetX = (random.nextDouble() - 0.5) * 20; // -10 bis +10
        double offsetZ = (random.nextDouble() - 0.5) * 20;

        return center.clone().add(offsetX, -8, offsetZ);
    }

    /**
     * Überwacht Mobs und prüft ob sie noch existieren
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

                Set<UUID> deadMobs = new HashSet<>();
                for (UUID mobId : mobs) {
                    Entity entity = Bukkit.getEntity(mobId);
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        deadMobs.add(mobId);
                    }
                }

                for (UUID deadMob : deadMobs) {
                    handleMobDeath(teamId, deadMob);
                }
            }
        };
        watcher.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Mob stirbt (wird vom ChallengeListener aufgerufen)
     */
    public void onMobDeath(UUID mobId) {
        UUID teamId = mobToTeam.get(mobId);
        if (teamId == null) return;

        handleMobDeath(teamId, mobId);
    }

    /**
     * Zentrale Methode für Mob-Tod-Handling
     */
    private void handleMobDeath(UUID teamId, UUID mobId) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;

        Set<UUID> mobs = teamMobs.get(teamId);
        if (mobs == null || !mobs.remove(mobId)) return;

        mobToTeam.remove(mobId);

        List<UUID> teamMembers = challenge.getTeamMembers(teamId);
        for (UUID memberId : teamMembers) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                PlayerChallengeData data = challenge.getPlayerData().get(memberId);
                if (data != null) {
                    int currentWave = data.getCurrentWaveIndex();
                    if (data.getWaveStats().containsKey(currentWave)) {
                        Objects.requireNonNull(data.getWaveStats().get(currentWave)).mobsKilled++;
                    }

                    List<Wave> waves = challenge.getTeamWaves().get(teamId);
                    if (waves != null && data.getCurrentWaveIndex() < waves.size()) {
                        Wave currentWaveData = waves.get(data.getCurrentWaveIndex());
                        updateBossbarForPlayer(player, currentWaveData, mobs.size());
                    }
                }
            }
        }

        if (mobs.isEmpty()) {
            onWaveCompletedForTeam(teamId);
        }
    }

    /**
     * Wave für Team abgeschlossen
     * NEU: Bossbars werden gecleaned + Spectator-Bossbars entfernt!
     */
    private void onWaveCompletedForTeam(UUID teamId) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
        List<UUID> teamMembers = challenge.getTeamMembers(teamId);

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
                    Objects.requireNonNull(data.getWaveStats().get(waveIndex)).endTick = plugin.getDataManager().getTimerTicks();
                    data.setCurrentWaveIndex(waveIndex + 1);
                }
                player.sendMessage("§a§l✓ Wave " + (waveIndex + 1) + " abgeschlossen!");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                // NEU: Entferne alte Bossbar (auch von Spectators!)
                BossBar oldBar = playerBossbars.remove(memberId);
                if (oldBar != null) {
                    oldBar.removeAll();
                }

                // NEU: Entferne Spectator-Tracking für diese Bossbar
                removeSpectatorBossbar(memberId);
            }
        }

        List<Wave> teamWaves = challenge.getTeamWaves().get(teamId);
        int totalWaves = teamWaves != null ? teamWaves.size() : 3;

        if (waveIndex + 1 < totalWaves) {
            startWaveForTeam(teamId, waveIndex + 1);
        } else {
            // Team hat alle Waves geschafft!
            for (UUID memberId : teamMembers) {
                plugin.getChallengeManager().onPlayerCompleted(memberId);

                // NEU: Final-Bossbar cleanup (auch von Spectators!)
                BossBar bar = playerBossbars.remove(memberId);
                if (bar != null) {
                    bar.removeAll();
                }

                // NEU: Final Spectator-Cleanup
                removeSpectatorBossbar(memberId);
            }
        }
    }

    /**
     * Update Bossbar für einzelnen Spieler
     * NEU: Auch Spectators sehen diese Bossbar!
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

        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;
        UUID teamId = challenge.getTeamOfPlayer(player.getUniqueId());
        List<Wave> teamWaves = challenge.getTeamWaves().get(teamId);
        int totalWaves = teamWaves != null ? teamWaves.size() : 3;

        bar.setTitle("§cWave " + wave.getWaveNumber() + "§7/§c" + totalWaves + " §7- §e" + remainingMobs + "§7/§e" + totalMobs + " Mobs");
        bar.setProgress(Math.max(0.01, progress));
        bar.setColor(BarColor.RED);

        // NEU: Update auch für Spectators die diesen Spieler beobachten
        updateSpectatorBossbars(player.getUniqueId(), bar);
    }

    /**
     * NEU: Zeigt Spectator die Bossbar eines Spielers an
     */
    public void showBossbarToSpectator(Player spectator, UUID targetPlayerId) {
        BossBar targetBar = playerBossbars.get(targetPlayerId);
        if (targetBar != null) {
            targetBar.addPlayer(spectator);

            // Tracking
            spectatorBossbars.computeIfAbsent(spectator.getUniqueId(), k -> new HashSet<>()).add(targetPlayerId);

            spectator.sendMessage("§7Du siehst jetzt die Wave-Info von §e" + Bukkit.getPlayer(targetPlayerId).getName());
        }
    }

    /**
     * NEU: Entfernt alle Bossbars von einem Spectator
     */
    public void hideAllBossbarsFromSpectator(Player spectator) {
        Set<UUID> watching = spectatorBossbars.remove(spectator.getUniqueId());
        if (watching != null) {
            for (UUID targetId : watching) {
                BossBar bar = playerBossbars.get(targetId);
                if (bar != null) {
                    bar.removePlayer(spectator);
                }
            }
        }
    }

    /**
     * NEU: Update Spectator-Bossbars wenn sich was ändert
     */
    private void updateSpectatorBossbars(UUID playerId, BossBar bar) {
        // Finde alle Spectators die diesen Spieler beobachten
        for (Map.Entry<UUID, Set<UUID>> entry : spectatorBossbars.entrySet()) {
            if (entry.getValue().contains(playerId)) {
                Player spectator = Bukkit.getPlayer(entry.getKey());
                if (spectator != null && spectator.isOnline()) {
                    // Bossbar ist bereits zu diesem Spectator hinzugefügt
                    // Automatisches Update durch BossBar-API
                }
            }
        }
    }

    /**
     * NEU: Entfernt Spectator-Tracking für eine Bossbar
     */
    private void removeSpectatorBossbar(UUID playerId) {
        for (Set<UUID> watching : spectatorBossbars.values()) {
            watching.remove(playerId);
        }
    }

    /**
     * Stellt den Zustand der Waves für alle Teams wieder her.
     */
    public void restoreWavesForTeams(Challenge challenge) {
        for (UUID teamId : challenge.getTeams().keySet()) {
            // Finde die aktuelle Wave-Nummer für dieses Team
            int currentWaveIndex = -1;
            for (UUID memberId : challenge.getTeamMembers(teamId)) {
                PlayerChallengeData data = challenge.getPlayerData().get(memberId);
                if (data != null && !data.isHasCompleted()) {
                    currentWaveIndex = data.getCurrentWaveIndex();
                    break;
                }
            }

            if (currentWaveIndex != -1) {
                List<Wave> teamWaves = challenge.getTeamWaves().get(teamId);
                if (teamWaves != null && currentWaveIndex < teamWaves.size()) {
                    plugin.getLogger().info("Stelle Wave " + (currentWaveIndex + 1) + " für Team " + teamId + " wieder her.");
                    startWaveForTeam(teamId, currentWaveIndex);
                }
            }
        }
    }

    /**
     * Räumt Daten für ein Team auf
     */
    public void cleanupTeam(UUID teamId) {
        BukkitTask task = countdownTasks.remove(teamId);
        if (task != null) {
            task.cancel();
        }

        // Stoppe Spawn-Tasks
        BukkitTask spawnTask = spawnTasks.remove(teamId);
        if (spawnTask != null) {
            spawnTask.cancel();
        }

        Set<UUID> mobs = teamMobs.remove(teamId);
        if (mobs != null) {
            for (UUID mobId : mobs) {
                mobToTeam.remove(mobId);
                Entity entity = Bukkit.getEntity(mobId);
                if (entity != null) {
                    entity.remove();
                }
            }
        }

        plugin.getLogger().info("[WaveManager] Team-Cleanup durchgeführt für: " + teamId);
    }

    public void cleanup(UUID playerId) {
        BossBar bar = playerBossbars.remove(playerId);
        if (bar != null) {
            bar.removeAll();
        }

        // NEU: Cleanup Spectator-Tracking
        removeSpectatorBossbar(playerId);
    }

    /**
     * Räumt ALLES auf
     * NEU: Auch Spectator-Bossbars!
     */
    public void cleanupAll() {
        // Cleanup alle Bossbars (auch von Spectators!)
        for (BossBar bar : new ArrayList<>(playerBossbars.values())) {
            bar.removeAll();
        }
        playerBossbars.clear();

        for (BukkitTask task : new ArrayList<>(countdownTasks.values())) {
            task.cancel();
        }
        countdownTasks.clear();

        // Cleanup Spawn-Tasks
        for (BukkitTask task : new ArrayList<>(spawnTasks.values())) {
            task.cancel();
        }
        spawnTasks.clear();

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

        // NEU: Cleanup Spectator-Tracking
        spectatorBossbars.clear();

        plugin.getLogger().info("[WaveManager] Vollständiges Cleanup durchgeführt (inkl. Spectator-Bossbars)");
    }
}