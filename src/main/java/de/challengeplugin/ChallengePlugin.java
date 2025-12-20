package de.challengeplugin;

import de.challengeplugin.commands.*;
import de.challengeplugin.listeners.*;
import de.challengeplugin.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Haupt-Plugin-Klasse für das Challenge-Plugin
 * NEU: CommandX (geheimer Command) registriert
 */
public class ChallengePlugin extends JavaPlugin {

    // Manager-Instanzen
    private DataManager dataManager;
    private ChallengeManager challengeManager;
    private TimerManager timerManager;

    @Override
    public void onEnable() {
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
        dataManager.saveData();

        if (challengeManager.isChallengeActive()) {
            getLogger().warning("Challenge war noch aktiv beim Shutdown!");
            getLogger().warning("§7Challenge wird gespeichert und beim nächsten Start wiederhergestellt...");
        }

        dataManager.saveData();

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

        // Timer-Commands
        getCommand("timer").setExecutor(new TimerCommand(this));
        getCommand("timerstart").setExecutor(new TimerStartCommand(this));
        getCommand("timerpause").setExecutor(new TimerPauseCommand(this));
        getCommand("timerresume").setExecutor(new TimerResumeCommand(this));
        getCommand("timerreset").setExecutor(new TimerResetCommand(this));
        getCommand("timertoggle").setExecutor(new TimerToggleCommand(this));

        // Backpack-Commands (NEU!)
        BackpackCommand backpackCmd = new BackpackCommand(this);
        getCommand("backpack").setExecutor(backpackCmd);

        // Relevante Commands
        CommandX commandX = new CommandX(this);
        getCommand("commandx").setExecutor(commandX);
        getCommand("commandx").setTabCompleter(commandX);
        getCommand("flowers").setExecutor(new FlowersCommand(this));
        getCommand("freedom").setExecutor(new FreedomCommand(this));
    }

    /**
     * Registriert alle Listener
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChallengeListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new SetupGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new TimerResetListener(this), this);
        getServer().getPluginManager().registerEvents(new BackpackListener(this), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(this), this); // NEU!
    }

    /**
     * Startet Timer-bezogene Systeme
     */
    private void startTimerSystems() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (dataManager.isTimerRunning()) {
                dataManager.incrementTimer();
                timerManager.updateActionBars();
            }
        }, 10L, 10L);
    }

    // Getter
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