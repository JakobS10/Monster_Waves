package de.challengeplugin.managers;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.*;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Verwaltet Arena-Instanzen und Schematic-Loading
 * Erstellt für jeden Spieler eine eigene Arena-Kopie
 */
public class ArenaManager {

    private final ChallengePlugin plugin;
    private final Map<UUID, ArenaInstance> arenaInstances = new HashMap<>();

    // Arena-Konfiguration
    private static final int ARENA_SPACING = 1000;  // Abstand zwischen Arenen
    private static final int ARENA_SIZE_X = 50;
    private static final int ARENA_SIZE_Z = 50;
    private static final int ARENA_SIZE_Y = 25;

    private Clipboard arenaSchematic = null;

    public ArenaManager(ChallengePlugin plugin) {
        this.plugin = plugin;
        loadSchematic();
    }

    /**
     * Lädt die Arena-Schematic aus dem Plugin-Ordner
     * Erwartet: plugins/AchievementChallenge/arena.schem
     */
    private void loadSchematic() {
        File schematicFile = new File(plugin.getDataFolder(), "arena.schem");

        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Arena-Schematic nicht gefunden: " + schematicFile.getPath());
            plugin.getLogger().warning("Erstelle Fallback-Arena...");
            return;
        }

        try {
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                arenaSchematic = reader.read();
                plugin.getLogger().info("Arena-Schematic geladen: " + schematicFile.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Laden der Schematic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Erstellt Arenen für alle Challenge-Teilnehmer
     */
    public void createArenas(Challenge challenge) {
        World world = Bukkit.getWorlds().get(0); // Overworld
        int arenaIndex = 0;

        for (UUID playerId : challenge.getParticipants()) {
            // Berechne Arena-Position (Grid-Layout)
            int x = arenaIndex * ARENA_SPACING;
            int z = 0;
            Location centerLoc = new Location(world, x, 100, z);

            // Erstelle Arena-Instanz
            ArenaInstance arena = createArenaInstance(playerId, centerLoc);
            arenaInstances.put(playerId, arena);
            challenge.getPlayerArenaMapping().put(playerId, arena.getInstanceId());

            arenaIndex++;
        }

        plugin.getLogger().info("Erstellt " + arenaIndex + " Arenen für die Challenge");
    }

    /**
     * Erstellt eine einzelne Arena-Instanz
     */
    private ArenaInstance createArenaInstance(UUID playerId, Location centerLocation) {
        ArenaInstance arena = new ArenaInstance(
                UUID.randomUUID(),
                playerId,
                centerLocation,
                ARENA_SIZE_X,
                ARENA_SIZE_Z,
                ARENA_SIZE_Y
        );

        // Platziere Schematic oder erstelle Fallback
        if (arenaSchematic != null) {
            pasteSchematic(centerLocation);
        } else {
            buildFallbackArena(centerLocation);
        }

        // Erstelle WorldBorder für diese Arena
        createArenaBorder(arena);

        return arena;
    }

    /**
     * Fügt Schematic an Position ein
     */
    private void pasteSchematic(Location location) {
        try {
            WorldEdit we = WorldEdit.getInstance();
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());

            EditSession editSession = we.newEditSession(world);
            Operation operation = new ClipboardHolder(arenaSchematic)
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(operation);
            editSession.close();

        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Einfügen der Schematic: " + e.getMessage());
        }
    }

    /**
     * Erstellt eine einfache Fallback-Arena wenn keine Schematic vorhanden
     */
    private void buildFallbackArena(Location center) {
        World world = center.getWorld();
        int radius = 25;

        // Boden aus Stone
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location loc = center.clone().add(x, -1, z);
                loc.getBlock().setType(Material.STONE);
            }
        }

        // Barrier-Wände (werden später durch WorldBorder ergänzt)
        for (int y = 0; y < 20; y++) {
            for (int i = -radius; i <= radius; i++) {
                // Nord/Süd-Wände
                new Location(world, center.getX() + i, center.getY() + y, center.getZ() - radius).getBlock().setType(Material.BARRIER);
                new Location(world, center.getX() + i, center.getY() + y, center.getZ() + radius).getBlock().setType(Material.BARRIER);

                // Ost/West-Wände
                new Location(world, center.getX() - radius, center.getY() + y, center.getZ() + i).getBlock().setType(Material.BARRIER);
                new Location(world, center.getX() + radius, center.getY() + y, center.getZ() + i).getBlock().setType(Material.BARRIER);
            }
        }

        // Dach aus Barrier
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                new Location(world, center.getX() + x, center.getY() + 20, center.getZ() + z).getBlock().setType(Material.BARRIER);
            }
        }
    }

    /**
     * Erstellt WorldBorder für eine Arena
     */
    private void createArenaBorder(ArenaInstance arena) {
        // Hinweis: Bukkit WorldBorder ist global pro Welt
        // Für per-Player-Borders müssten wir ein Movement-Cancel-System nutzen
        // Siehe ArenaProtectionListener
    }

    /**
     * Gibt Arena für Spieler zurück
     */
    public ArenaInstance getArenaForPlayer(UUID playerId) {
        return arenaInstances.get(playerId);
    }

    /**
     * Prüft ob Location in einer Arena ist
     */
    public boolean isInArena(Location location) {
        return arenaInstances.values().stream()
                .anyMatch(arena -> isInArenaBounds(location, arena));
    }

    /**
     * Prüft ob Location in Arena-Grenzen ist
     */
    private boolean isInArenaBounds(Location loc, ArenaInstance arena) {
        Location center = arena.getCenterLocation();
        double halfX = arena.getSizeX() / 2.0;
        double halfZ = arena.getSizeZ() / 2.0;

        return Math.abs(loc.getX() - center.getX()) <= halfX
                && Math.abs(loc.getZ() - center.getZ()) <= halfZ
                && loc.getY() >= center.getY()
                && loc.getY() <= center.getY() + arena.getSizeY();
    }

    /**
     * Entfernt alle Arenen
     */
    public void clearArenas() {
        // Optional: Setze Arena-Bereiche auf Luft zurück
        // Oder lasse sie stehen für manuelles Cleanup
        arenaInstances.clear();
    }
}