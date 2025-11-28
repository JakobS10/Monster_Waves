package de.challengeplugin.models;

import org.bukkit.inventory.ItemStack;
import java.util.*;

/**
 * Speichert alle Challenge-Daten eines einzelnen Spielers
 */
public class PlayerChallengeData {

    private final UUID playerId;

    // Inventar-Backups
    private ItemStack[] inventoryBackup;
    private ItemStack[] armorBackup;
    private ItemStack[] offHandBackup;

    // Combat-Status
    private int currentWaveIndex = 0;      // 0-2 (3 Waves)
    private boolean isAlive = true;
    private boolean hasForfeited = false;
    private boolean hasCompleted = false;

    // Statistiken
    private int totalDeaths = 0;
    private double totalDamageTaken = 0.0;
    private long combatStartTick = 0;      // Wann Kampf begann
    private long combatEndTick = 0;        // Wann Kampf endete

    // Wave-Tracking
    private final Map<Integer, WaveStats> waveStats = new HashMap<>();

    // Spectator-Modus
    private boolean isSpectating = false;

    public static class WaveStats {
        public long startTick;
        public long endTick;
        public int deaths;
        public double damageTaken;
        public int mobsKilled;
    }

    // Konstruktor und Getter/Setter...
}