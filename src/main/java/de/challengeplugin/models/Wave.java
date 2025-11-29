package de.challengeplugin.models;

import org.bukkit.entity.EntityType;
import java.util.*;

/**
 * Definiert eine Wave mit Mob-Zusammenstellung
 */
public class Wave {

    private int waveNumber;
    private List<EntityType> mobs;
    private UUID targetTeamId; // GEÄNDERT: von targetPlayerId zu targetTeamId

    // Konstruktor
    public Wave() {
        this.mobs = new ArrayList<>();
    }

    public Wave(int waveNumber, UUID targetTeamId) {
        this.waveNumber = waveNumber;
        this.targetTeamId = targetTeamId;
        this.mobs = new ArrayList<>();
    }

    public void addMob(EntityType type) {
        this.mobs.add(type);
    }

    public int getTotalMobCount() {
        return mobs.size();
    }

    // === GETTER UND SETTER ===

    public int getWaveNumber() {
        return waveNumber;
    }

    public void setWaveNumber(int waveNumber) {
        this.waveNumber = waveNumber;
    }

    public List<EntityType> getMobs() {
        return mobs;
    }

    public void setMobs(List<EntityType> mobs) {
        this.mobs = mobs;
    }

    public UUID getTargetTeamId() {
        return targetTeamId;
    }

    public void setTargetTeamId(UUID targetTeamId) {
        this.targetTeamId = targetTeamId;
    }
}