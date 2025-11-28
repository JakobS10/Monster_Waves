package de.challengeplugin;

import de.challengeplugin.commands.*;
import de.challengeplugin.listeners.*;
import de.challengeplugin.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Haupt-Plugin-Klasse für das Challenge-Plugin
 * Unabhängiges Plugin ohne externe Abhängigkeiten (außer WorldEdit)
 */
public class ChallengePlugin extends JavaPlugin {

    // Manager-Instanzen
    private DataManager dataManager;
    private ChallengeManager challengeManager;
    private TimerManager timerManager;

    @Override
    public void onEnable() {
        // Plugin-Ordner erstellen falls nicht vorhanden
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialisiere Manager
        this.dataManager = new DataManager(this);
        this.timerManager = new TimerManager(this);
        this.challengeManager = new ChallengeManager(this);

        // Lade gespeicherte Daten
        dataManager.loadData();

        // Registriere Commands
        registerCommands();

        // Registriere Listener
        registerListeners();

        // Starte Timer-Systeme
        startTimerSystems();

        getLogger().info("§a=================================");
        getLogger().info("§aChallenge-Plugin wurde geladen!");
        getLogger().info("§aVersion: " + getDescription().getVersion());
        getLogger().info("§a=================================");
    }

    @Override
    public void onDisable() {
        // Speichere Daten
        dataManager.saveData();

        // Cleanup Challenge falls aktiv
        if (challengeManager.isChallengeActive()) {
            getLogger().warning("Challenge war noch aktiv beim Shutdown!");
            // TODO: Challenge-State speichern für Reload
        }

        getLogger().info("Challenge-Plugin wurde deaktiviert!");
    }

    /**
     * Registriert alle Commands
     */
    private void registerCommands() {
        // Challenge-Commands
        getCommand("challenge").setExecutor(new ChallengeCommand(this));
        getCommand("aufgeben").setExecutor(new ForfeitCommand(this));
        getCommand("spectate").setExecutor(new SpectateCommand(this));

        // Timer-Commands (für Admin/Testing)
        getCommand("timer").setExecutor(new TimerCommand(this));
        getCommand("timerstart").setExecutor(new TimerStartCommand(this));
        getCommand("timerpause").setExecutor(new TimerPauseCommand(this));
        getCommand("timerresume").setExecutor(new TimerResumeCommand(this));
        getCommand("timerreset").setExecutor(new TimerResetCommand(this));
        getCommand("timertoggle").setExecutor(new TimerToggleCommand(this));
    }

    /**
     * Registriert alle Listener
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChallengeListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new SetupGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new TimerResetListener(this), this);
    }

    /**
     * Startet Timer-bezogene Systeme
     */
    private void startTimerSystems() {
        // Starte ActionBar-Update (alle 10 Ticks = 0.5 Sekunden)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (dataManager.isTimerRunning()) {
                // Erhöhe Timer
                dataManager.incrementTimer();

                // Update ActionBars
                timerManager.updateActionBars();
            }
        }, 10L, 10L);
    }

    // === GETTER FÜR MANAGER ===

    public DataManager getDataManager() {
        return dataManager;
    }

    public ChallengeManager getChallengeManager() {
        return challengeManager;
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }
}