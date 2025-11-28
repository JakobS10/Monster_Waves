package de.challengeplugin.models;

import org.bukkit.entity.EntityType;
import java.util.*;

/**
 * Definiert eine Wave mit Mob-Zusammenstellung
 */
public class Wave {

    private final int waveNumber;              // 1, 2 oder 3
    private final List<EntityType> mobs;       // Mob-Typen aus Spawn Eggs
    private final UUID targetPlayerId;         // Für wen ist diese Wave

    // Konstruktor
    public Wave(int waveNumber, UUID targetPlayerId) {
        this.waveNumber = waveNumber;
        this.targetPlayerId = targetPlayerId;
        this.mobs = new ArrayList<>();
    }

    public void addMob(EntityType type) {
        this.mobs.add(type);
    }

    public int getTotalMobCount() {
        return mobs.size();
    }

    // Getter...
}