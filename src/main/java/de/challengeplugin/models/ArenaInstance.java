package de.challengeplugin.models;

import org.bukkit.Location;
import java.util.*;

/**
 * Repräsentiert eine Arena-Kopie für einen Spieler
 */
public class ArenaInstance {

    private UUID instanceId;
    private UUID assignedPlayerId;

    // Arena-Koordinaten
    private Location centerLocation;
    private Location spawnPoint;

    // Größe
    private int sizeX;
    private int sizeZ;
    private int sizeY;

    // Aktive Entities
    private Set<UUID> spawnedMobs = new HashSet<>();

    // Status
    private boolean isOccupied = false;

    // Konstruktor
    public ArenaInstance() {
    }

    public ArenaInstance(UUID instanceId, UUID assignedPlayerId, Location centerLocation,
                         int sizeX, int sizeZ, int sizeY) {
        this.instanceId = instanceId;
        this.assignedPlayerId = assignedPlayerId;
        this.centerLocation = centerLocation;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.sizeY = sizeY;

        // Spawn-Point ist Zentrum + 1 Block hoch
        this.spawnPoint = centerLocation.clone().add(0, 1, 0);
    }

    // === GETTER UND SETTER ===

    public UUID getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(UUID instanceId) {
        this.instanceId = instanceId;
    }

    public UUID getAssignedPlayerId() {
        return assignedPlayerId;
    }

    public void setAssignedPlayerId(UUID assignedPlayerId) {
        this.assignedPlayerId = assignedPlayerId;
    }

    public Location getCenterLocation() {
        return centerLocation;
    }

    public void setCenterLocation(Location centerLocation) {
        this.centerLocation = centerLocation;
    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public int getSizeX() {
        return sizeX;
    }

    public void setSizeX(int sizeX) {
        this.sizeX = sizeX;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public void setSizeZ(int sizeZ) {
        this.sizeZ = sizeZ;
    }

    public int getSizeY() {
        return sizeY;
    }

    public void setSizeY(int sizeY) {
        this.sizeY = sizeY;
    }

    public Set<UUID> getSpawnedMobs() {
        return spawnedMobs;
    }

    public void setSpawnedMobs(Set<UUID> spawnedMobs) {
        this.spawnedMobs = spawnedMobs;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }
}