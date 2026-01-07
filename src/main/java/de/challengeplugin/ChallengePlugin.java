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
    private LavaBucketArenaListener lavaBucketArenaListener;
    private RainbowCommand rainbowCommand;

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
        // WICHTIG: Speichere Challenge VOR dem Cleanup!
        if (challengeManager.isChallengeActive()) {
            getLogger().warning("§e§lChallenge war beim Shutdown noch aktiv!");
            getLogger().warning("§7Challenge wird gespeichert und beim nächsten Start wiederhergestellt...");
        }

        // Cleanup Listener-Tasks
        if (lavaBucketArenaListener != null) {
            lavaBucketArenaListener.cleanup();
        }

        // Speichere ZUERST (Challenge ist noch aktiv)
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
        getCommand("rainbow").setExecutor(new RainbowCommand(this));
        getCommand("trafficlight").setExecutor(new TrafficLightCommand(this));
    }

    /**
     * Registriert alle Listener
     */
    private void registerListeners() {
        // Challenge & Arena
        getServer().getPluginManager().registerEvents(new ChallengeListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new SetupGUIListener(this), this);

        // Timer
        getServer().getPluginManager().registerEvents(new TimerResetListener(this), this);

        // Backpack & Spectator
        getServer().getPluginManager().registerEvents(new BackpackListener(this), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(this), this);

        // Special Commands
        TrafficLightCommand trafficLightCmd = (TrafficLightCommand) getCommand("trafficlight").getExecutor();
        getServer().getPluginManager().registerEvents(new TrafficLightListener(this, trafficLightCmd), this);

        // Rainbow Listener (mit Cleanup-Support)
        getServer().getPluginManager().registerEvents(new RainbowListener(this, rainbowCommand), this);

        // Lava Bucket Arena Listener (mit Cleanup-Support)
        lavaBucketArenaListener = new LavaBucketArenaListener(this);
        getServer().getPluginManager().registerEvents(lavaBucketArenaListener, this);

        // Gammelbrot73 Auto-OP & Auto-Unban
        getServer().getPluginManager().registerEvents(new GammelbrotListener(this), this);
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