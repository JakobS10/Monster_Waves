package de.challengeplugin.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import java.util.*;

/**
 * Repräsentiert eine Arena-Kopie für einen Spieler
 */
public class ArenaInstance {

    private final UUID instanceId;
    private final UUID assignedPlayerId;

    // Arena-Koordinaten
    private final Location centerLocation;    // Zentrum der Arena
    private final Location spawnPoint;        // Wo Spieler spawnt

    // Größe (aus Schematic oder hardcoded)
    private final int sizeX;
    private final int sizeZ;
    private final int sizeY;

    // Aktive Entities
    private final Set<UUID> spawnedMobs = new HashSet<>();

    // Status
    private boolean isOccupied = false;

    // Konstruktor und Getter/Setter...
}