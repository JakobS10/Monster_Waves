package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Verwaltet persistente Daten (Timer, Challenge-State)
 * Speichert in YAML-Datei
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
    }

    /**
     * Speichert Daten in YAML-Datei
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

        // Speichere in Datei
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Daten!");
            e.printStackTrace();
        }
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