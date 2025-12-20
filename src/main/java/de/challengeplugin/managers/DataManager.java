package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import de.challengeplugin.models.PlayerChallengeData;
import de.challengeplugin.models.Wave;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Verwaltet persistente Daten (Timer, Challenge-State)
 * NEU: Speichert laufende Challenges bei Server-Crash/Neustart!
 */
public class DataManager {

    private final ChallengePlugin plugin;
    private final File dataFile;
    private FileConfiguration config;

    // Timer-Daten
    private long timerTicks = 0;
    private boolean timerRunning = false;

    // ActionBar-Einstellungen pro Spieler
    private final Set<UUID> actionBarEnabled = new HashSet<>();

    // Challenge-Daten (optional für Persistence)
    private Challenge lastChallenge = null;

    public DataManager(ChallengePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    /**
     * Lädt Daten aus YAML-Datei
     * NEU: Lädt auch gespeicherte Challenge!
     */
    public void loadData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("Keine gespeicherten Daten gefunden, starte mit Standardwerten");
            return;
        }

        config = YamlConfiguration.loadConfiguration(dataFile);

        // Timer-Daten laden
        timerTicks = config.getLong("timer.ticks", 0);
        timerRunning = config.getBoolean("timer.running", false);

        // ActionBar-Einstellungen laden
        List<String> actionBarList = config.getStringList("actionbar.enabled");
        for (String uuidStr : actionBarList) {
            try {
                actionBarEnabled.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ungültige UUID: " + uuidStr);
            }
        }

        plugin.getLogger().info("Daten geladen - Timer: " + formatTime(timerTicks));

        // NEU: Lade gespeicherte Challenge
        if (config.contains("challenge")) {
            try {
                Challenge challenge = loadChallenge();
                if (challenge != null) {
                    plugin.getLogger().info("§e§l=================================");
                    plugin.getLogger().info("§e§lGespeicherte Challenge gefunden!");
                    plugin.getLogger().info("§e§l=================================");

                    // Stelle Challenge im ChallengeManager wieder her
                    plugin.getChallengeManager().restoreChallenge(challenge);

                    plugin.getLogger().info("§aChallenge erfolgreich wiederhergestellt!");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Fehler beim Laden der Challenge:");
                e.printStackTrace();
            }
        }
    }

    /**
     * Speichert Daten in YAML-Datei
     * NEU: Speichert auch aktive Challenge!
     */
    public void saveData() {
        config = new YamlConfiguration();

        // Timer-Daten speichern
        config.set("timer.ticks", timerTicks);
        config.set("timer.running", timerRunning);

        // ActionBar-Einstellungen speichern
        List<String> actionBarList = new ArrayList<>();
        for (UUID uuid : actionBarEnabled) {
            actionBarList.add(uuid.toString());
        }
        config.set("actionbar.enabled", actionBarList);

        // NEU: Speichere aktive Challenge
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge != null) {
            saveChallenge(challenge);
            plugin.getLogger().info("[DataManager] Aktive Challenge gespeichert");
        } else {
            config.set("challenge", null);
        }

        // Speichere in Datei
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Daten!");
            e.printStackTrace();
        }
    }

    /**
     * NEU: Speichert Challenge in Config
     */
    private void saveChallenge(Challenge challenge) {
        ConfigurationSection sec = config.createSection("challenge");

        // Basis-Daten
        sec.set("id", challenge.getChallengeId().toString());
        sec.set("startTime", challenge.getStartTime());
        sec.set("farmDurationTicks", challenge.getFarmDurationTicks());
        sec.set("bossPlayerId", challenge.getBossPlayerId().toString());
        sec.set("netherEnabled", challenge.isNetherEnabled());
        sec.set("endEnabled", challenge.isEndEnabled());
        sec.set("bossParticipates", challenge.isBossParticipates());
        sec.set("teamMode", challenge.getTeamMode().name());
        sec.set("currentPhase", challenge.getCurrentPhase().name());
        sec.set("phaseStartTick", challenge.getPhaseStartTick());

        // Participants
        List<String> participantList = new ArrayList<>();
        for (UUID id : challenge.getParticipants()) {
            participantList.add(id.toString());
        }
        sec.set("participants", participantList);

        // Teams
        ConfigurationSection teamsSection = sec.createSection("teams");
        for (Map.Entry<UUID, List<UUID>> entry : challenge.getTeams().entrySet()) {
            List<String> memberList = new ArrayList<>();
            for (UUID memberId : entry.getValue()) {
                memberList.add(memberId.toString());
            }
            teamsSection.set(entry.getKey().toString(), memberList);
        }

        // Player-Team-Mapping
        ConfigurationSection playerToTeamSection = sec.createSection("playerToTeam");
        for (Map.Entry<UUID, UUID> entry : challenge.getPlayerToTeam().entrySet()) {
            playerToTeamSection.set(entry.getKey().toString(), entry.getValue().toString());
        }

        // PlayerData
        ConfigurationSection playerDataSection = sec.createSection("playerData");
        for (Map.Entry<UUID, PlayerChallengeData> entry : challenge.getPlayerData().entrySet()) {
            ConfigurationSection playerSec = playerDataSection.createSection(entry.getKey().toString());
            PlayerChallengeData data = entry.getValue();

            playerSec.set("currentWaveIndex", data.getCurrentWaveIndex());
            playerSec.set("isAlive", data.isAlive());
            playerSec.set("hasForfeited", data.isHasForfeited());
            playerSec.set("hasCompleted", data.isHasCompleted());
            playerSec.set("totalDeaths", data.getTotalDeaths());
            playerSec.set("totalDamageTaken", data.getTotalDamageTaken());
            playerSec.set("combatStartTick", data.getCombatStartTick());
            playerSec.set("combatEndTick", data.getCombatEndTick());

            // Inventar-Backup
            if (data.getInventoryBackup() != null) {
                playerSec.set("inventoryBackup", Arrays.asList(data.getInventoryBackup()));
            }
            if (data.getArmorBackup() != null) {
                playerSec.set("armorBackup", Arrays.asList(data.getArmorBackup()));
            }
            if (data.getOffHandBackup() != null) {
                playerSec.set("offHandBackup", Arrays.asList(data.getOffHandBackup()));
            }

            // Wave-Stats
            ConfigurationSection waveStatsSection = playerSec.createSection("waveStats");
            for (Map.Entry<Integer, PlayerChallengeData.WaveStats> statsEntry : data.getWaveStats().entrySet()) {
                ConfigurationSection statsSec = waveStatsSection.createSection(statsEntry.getKey().toString());
                PlayerChallengeData.WaveStats stats = statsEntry.getValue();
                statsSec.set("startTick", stats.startTick);
                statsSec.set("endTick", stats.endTick);
                statsSec.set("deaths", stats.deaths);
                statsSec.set("damageTaken", stats.damageTaken);
                statsSec.set("mobsKilled", stats.mobsKilled);
            }
        }

        // Team-Waves
        ConfigurationSection teamWavesSection = sec.createSection("teamWaves");
        for (Map.Entry<UUID, List<Wave>> entry : challenge.getTeamWaves().entrySet()) {
            ConfigurationSection teamWaveSection = teamWavesSection.createSection(entry.getKey().toString());
            int waveIndex = 0;
            for (Wave wave : entry.getValue()) {
                ConfigurationSection waveSec = teamWaveSection.createSection(String.valueOf(waveIndex));
                waveSec.set("waveNumber", wave.getWaveNumber());

                List<String> mobTypes = new ArrayList<>();
                for (EntityType type : wave.getMobs()) {
                    mobTypes.add(type.name());
                }
                waveSec.set("mobs", mobTypes);
                waveIndex++;
            }
        }

        // NEU: Backpack-Items speichern!
        ConfigurationSection backpacksSection = sec.createSection("teamBackpacks");
        for (Map.Entry<UUID, Inventory> entry : plugin.getChallengeManager()
                .getBackpackManager().getTeamBackpacks().entrySet()) {
            UUID teamId = entry.getKey();
            Inventory backpack = entry.getValue();

            // Speichere alle Items aus dem Backpack
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack item : backpack.getContents()) {
                if (item != null) {
                    items.add(item);
                }
            }

            if (!items.isEmpty()) {
                backpacksSection.set(teamId.toString(), items);
                plugin.getLogger().info("[DataManager] Backpack für Team gespeichert: " + items.size() + " Items");
            }
        }
    }

    /**
     * NEU: Lädt Challenge aus Config
     */
    private Challenge loadChallenge() {
        if (!config.contains("challenge")) return null;

        ConfigurationSection sec = config.getConfigurationSection("challenge");
        if (sec == null) return null;

        try {
            Challenge challenge = new Challenge();

            // Basis-Daten
            challenge.setChallengeId(UUID.fromString(sec.getString("id")));
            challenge.setStartTime(sec.getLong("startTime"));
            challenge.setFarmDurationTicks(sec.getLong("farmDurationTicks"));
            challenge.setBossPlayerId(UUID.fromString(sec.getString("bossPlayerId")));
            challenge.setNetherEnabled(sec.getBoolean("netherEnabled"));
            challenge.setEndEnabled(sec.getBoolean("endEnabled"));
            challenge.setBossParticipates(sec.getBoolean("bossParticipates"));
            challenge.setTeamMode(Challenge.TeamMode.valueOf(sec.getString("teamMode")));
            challenge.setCurrentPhase(Challenge.ChallengePhase.valueOf(sec.getString("currentPhase")));
            challenge.setPhaseStartTick(sec.getLong("phaseStartTick"));

            // Participants
            for (String uuidStr : sec.getStringList("participants")) {
                challenge.getParticipants().add(UUID.fromString(uuidStr));
            }

            // Teams
            ConfigurationSection teamsSection = sec.getConfigurationSection("teams");
            if (teamsSection != null) {
                for (String teamIdStr : teamsSection.getKeys(false)) {
                    UUID teamId = UUID.fromString(teamIdStr);
                    List<UUID> members = new ArrayList<>();
                    for (String memberStr : teamsSection.getStringList(teamIdStr)) {
                        members.add(UUID.fromString(memberStr));
                    }
                    challenge.getTeams().put(teamId, members);
                }
            }

            // Player-Team-Mapping
            ConfigurationSection playerToTeamSection = sec.getConfigurationSection("playerToTeam");
            if (playerToTeamSection != null) {
                for (String playerIdStr : playerToTeamSection.getKeys(false)) {
                    UUID playerId = UUID.fromString(playerIdStr);
                    UUID teamId = UUID.fromString(playerToTeamSection.getString(playerIdStr));
                    challenge.getPlayerToTeam().put(playerId, teamId);
                }
            }

            // PlayerData
            ConfigurationSection playerDataSection = sec.getConfigurationSection("playerData");
            if (playerDataSection != null) {
                for (String playerIdStr : playerDataSection.getKeys(false)) {
                    UUID playerId = UUID.fromString(playerIdStr);
                    ConfigurationSection playerSec = playerDataSection.getConfigurationSection(playerIdStr);

                    PlayerChallengeData data = new PlayerChallengeData(playerId);
                    data.setCurrentWaveIndex(playerSec.getInt("currentWaveIndex"));
                    data.setAlive(playerSec.getBoolean("isAlive"));
                    data.setHasForfeited(playerSec.getBoolean("hasForfeited"));
                    data.setHasCompleted(playerSec.getBoolean("hasCompleted"));
                    data.setTotalDeaths(playerSec.getInt("totalDeaths"));
                    data.setTotalDamageTaken(playerSec.getDouble("totalDamageTaken"));
                    data.setCombatStartTick(playerSec.getLong("combatStartTick"));
                    data.setCombatEndTick(playerSec.getLong("combatEndTick"));

                    // Inventar-Backup
                    if (playerSec.contains("inventoryBackup")) {
                        @SuppressWarnings("unchecked")
                        List<ItemStack> invList = (List<ItemStack>) playerSec.getList("inventoryBackup");
                        if (invList != null) {
                            data.setInventoryBackup(invList.toArray(new ItemStack[0]));
                        }
                    }
                    if (playerSec.contains("armorBackup")) {
                        @SuppressWarnings("unchecked")
                        List<ItemStack> armorList = (List<ItemStack>) playerSec.getList("armorBackup");
                        if (armorList != null) {
                            data.setArmorBackup(armorList.toArray(new ItemStack[0]));
                        }
                    }
                    if (playerSec.contains("offHandBackup")) {
                        @SuppressWarnings("unchecked")
                        List<ItemStack> offHandList = (List<ItemStack>) playerSec.getList("offHandBackup");
                        if (offHandList != null) {
                            data.setOffHandBackup(offHandList.toArray(new ItemStack[0]));
                        }
                    }

                    // Wave-Stats
                    ConfigurationSection waveStatsSection = playerSec.getConfigurationSection("waveStats");
                    if (waveStatsSection != null) {
                        for (String waveIndexStr : waveStatsSection.getKeys(false)) {
                            int waveIndex = Integer.parseInt(waveIndexStr);
                            ConfigurationSection statsSec = waveStatsSection.getConfigurationSection(waveIndexStr);

                            PlayerChallengeData.WaveStats stats = new PlayerChallengeData.WaveStats();
                            stats.startTick = statsSec.getLong("startTick");
                            stats.endTick = statsSec.getLong("endTick");
                            stats.deaths = statsSec.getInt("deaths");
                            stats.damageTaken = statsSec.getDouble("damageTaken");
                            stats.mobsKilled = statsSec.getInt("mobsKilled");

                            data.getWaveStats().put(waveIndex, stats);
                        }
                    }

                    challenge.getPlayerData().put(playerId, data);
                }
            }

            // Team-Waves
            ConfigurationSection teamWavesSection = sec.getConfigurationSection("teamWaves");
            if (teamWavesSection != null) {
                for (String teamIdStr : teamWavesSection.getKeys(false)) {
                    UUID teamId = UUID.fromString(teamIdStr);
                    ConfigurationSection teamWaveSection = teamWavesSection.getConfigurationSection(teamIdStr);

                    List<Wave> waves = new ArrayList<>();
                    for (String waveIndexStr : teamWaveSection.getKeys(false)) {
                        ConfigurationSection waveSec = teamWaveSection.getConfigurationSection(waveIndexStr);

                        Wave wave = new Wave();
                        wave.setWaveNumber(waveSec.getInt("waveNumber"));
                        wave.setTargetTeamId(teamId);

                        for (String mobTypeStr : waveSec.getStringList("mobs")) {
                            wave.addMob(EntityType.valueOf(mobTypeStr));
                        }

                        waves.add(wave);
                    }

                    challenge.getTeamWaves().put(teamId, waves);
                }
            }

            return challenge;
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Parsen der Challenge:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * NEU: Lädt Backpack-Items und gibt sie zurück
     * Wird von ChallengeManager beim Restore aufgerufen
     */
    public Map<UUID, List<ItemStack>> loadBackpackItems() {
        Map<UUID, List<ItemStack>> backpackItems = new HashMap<>();

        if (!config.contains("challenge.teamBackpacks")) {
            return backpackItems;
        }

        ConfigurationSection backpacksSection = config.getConfigurationSection("challenge.teamBackpacks");
        if (backpacksSection == null) return backpackItems;

        for (String teamIdStr : backpacksSection.getKeys(false)) {
            try {
                UUID teamId = UUID.fromString(teamIdStr);

                @SuppressWarnings("unchecked")
                List<ItemStack> items = (List<ItemStack>) backpacksSection.getList(teamIdStr);

                if (items != null && !items.isEmpty()) {
                    backpackItems.put(teamId, items);
                    plugin.getLogger().info("[DataManager] Backpack-Items geladen für Team: " + items.size() + " Items");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Laden von Backpack-Items: " + e.getMessage());
            }
        }

        return backpackItems;
    }

    // === TIMER-METHODEN ===

    /**
     * Startet den Timer
     */
    public void startTimer() {
        this.timerRunning = true;
        plugin.getLogger().info("Timer gestartet");
    }

    /**
     * Pausiert den Timer
     */
    public void pauseTimer() {
        this.timerRunning = false;
        plugin.getLogger().info("Timer pausiert bei: " + formatTime(timerTicks));
    }

    /**
     * Setzt Timer zurück
     */
    public void resetTimer() {
        this.timerTicks = 0;
        this.timerRunning = false;
        plugin.getLogger().info("Timer zurückgesetzt");
    }

    /**
     * Erhöht Timer (wird jede 0.5 Sekunden aufgerufen)
     */
    public void incrementTimer() {
        if (timerRunning) {
            timerTicks += 10; // 10 Ticks = 0.5 Sekunden
        }
    }

    /**
     * Formatiert Ticks zu Zeit-String (DD:HH:MM:SS)
     */
    public static String formatTime(long ticks) {
        long totalSeconds = ticks / 20;

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }

    // === ACTIONBAR-METHODEN ===

    /**
     * Togglet ActionBar für Spieler
     */
    public void toggleActionBar(UUID playerId) {
        if (actionBarEnabled.contains(playerId)) {
            actionBarEnabled.remove(playerId);
        } else {
            actionBarEnabled.add(playerId);
        }
    }

    /**
     * Prüft ob Spieler ActionBar aktiviert hat
     */
    public boolean hasActionBarEnabled(UUID playerId) {
        return actionBarEnabled.contains(playerId);
    }

    // === GETTER/SETTER ===

    public long getTimerTicks() {
        return timerTicks;
    }

    public void setTimerTicks(long ticks) {
        this.timerTicks = ticks;
    }

    public boolean isTimerRunning() {
        return timerRunning;
    }

    public Challenge getLastChallenge() {
        return lastChallenge;
    }

    public void setLastChallenge(Challenge challenge) {
        this.lastChallenge = challenge;
    }
}