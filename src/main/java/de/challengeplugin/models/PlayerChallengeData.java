package de.challengeplugin.models;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

/**
 * Speichert alle Challenge-Daten eines einzelnen Spielers
 */
public class PlayerChallengeData {

    private UUID playerId;

    // Inventar-Backups
    private ItemStack[] inventoryBackup;
    private ItemStack[] armorBackup;
    private ItemStack[] offHandBackup;

    // Combat-Status
    private int currentWaveIndex = 0;
    private boolean isAlive = true;
    private boolean hasForfeited = false;
    private boolean hasCompleted = false;

    // Statistiken
    private int totalDeaths = 0;
    private double totalDamageTaken = 0.0;
    private long combatStartTick = 0;
    private long combatEndTick = 0;

    // Wave-Tracking
    private Map<Integer, WaveStats> waveStats = new HashMap<>();

    // Spectator-Modus
    private boolean isSpectating = false;

    public static class WaveStats {
        public long startTick;
        public long endTick;
        public int deaths;
        public double damageTaken;
        public int mobsKilled;
    }

    // Konstruktor
    public PlayerChallengeData() {
    }

    public PlayerChallengeData(UUID playerId) {
        this.playerId = playerId;
    }

    // === INVENTAR-METHODEN ===

    /**
     * Sichert das Inventar eines Spielers
     */
    public void backupInventory(Player player) {
        this.inventoryBackup = player.getInventory().getContents().clone();
        this.armorBackup = player.getInventory().getArmorContents().clone();

        ItemStack offHand = player.getInventory().getItemInOffHand();
        this.offHandBackup = new ItemStack[]{offHand != null ? offHand.clone() : null};
    }

    /**
     * Stellt das Inventar eines Spielers wieder her
     */
    public void restoreInventory(Player player) {
        if (inventoryBackup != null) {
            player.getInventory().setContents(inventoryBackup);
        }
        if (armorBackup != null) {
            player.getInventory().setArmorContents(armorBackup);
        }
        if (offHandBackup != null && offHandBackup.length > 0) {
            player.getInventory().setItemInOffHand(offHandBackup[0]);
        }
    }

    // === GETTER UND SETTER ===

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public ItemStack[] getInventoryBackup() {
        return inventoryBackup;
    }

    public void setInventoryBackup(ItemStack[] inventoryBackup) {
        this.inventoryBackup = inventoryBackup;
    }

    public ItemStack[] getArmorBackup() {
        return armorBackup;
    }

    public void setArmorBackup(ItemStack[] armorBackup) {
        this.armorBackup = armorBackup;
    }

    public ItemStack[] getOffHandBackup() {
        return offHandBackup;
    }

    public void setOffHandBackup(ItemStack[] offHandBackup) {
        this.offHandBackup = offHandBackup;
    }

    public int getCurrentWaveIndex() {
        return currentWaveIndex;
    }

    public void setCurrentWaveIndex(int currentWaveIndex) {
        this.currentWaveIndex = currentWaveIndex;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public boolean isHasForfeited() {
        return hasForfeited;
    }

    public void setHasForfeited(boolean hasForfeited) {
        this.hasForfeited = hasForfeited;
    }

    public boolean isHasCompleted() {
        return hasCompleted;
    }

    public void setHasCompleted(boolean hasCompleted) {
        this.hasCompleted = hasCompleted;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public void setTotalDeaths(int totalDeaths) {
        this.totalDeaths = totalDeaths;
    }

    public double getTotalDamageTaken() {
        return totalDamageTaken;
    }

    public void setTotalDamageTaken(double totalDamageTaken) {
        this.totalDamageTaken = totalDamageTaken;
    }

    public long getCombatStartTick() {
        return combatStartTick;
    }

    public void setCombatStartTick(long combatStartTick) {
        this.combatStartTick = combatStartTick;
    }

    public long getCombatEndTick() {
        return combatEndTick;
    }

    public void setCombatEndTick(long combatEndTick) {
        this.combatEndTick = combatEndTick;
    }

    public Map<Integer, WaveStats> getWaveStats() {
        return waveStats;
    }

    public void setWaveStats(Map<Integer, WaveStats> waveStats) {
        this.waveStats = waveStats;
    }

    public boolean isSpectating() {
        return isSpectating;
    }

    public void setSpectating(boolean spectating) {
        isSpectating = spectating;
    }
}