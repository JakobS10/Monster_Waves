package de.challengeplugin.utils;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.*;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;

/**
 * Utility-Klasse zum Laden und Einfügen von WorldEdit-Schematics
 * Wrapper um WorldEdit-API
 */
public class SchematicLoader {

    private final Logger logger;

    public SchematicLoader(Logger logger) {
        this.logger = logger;
    }

    /**
     * Lädt eine Schematic-Datei
     * @param file Die Schematic-Datei (.schem oder .schematic)
     * @return Clipboard-Objekt oder null bei Fehler
     */
    public Clipboard loadSchematic(File file) {
        if (!file.exists()) {
            logger.warning("Schematic nicht gefunden: " + file.getAbsolutePath());
            return null;
        }

        try {
            // Erkenne Format automatisch
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) {
                logger.severe("Unbekanntes Schematic-Format: " + file.getName());
                return null;
            }

            // Lade Schematic
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                Clipboard clipboard = reader.read();
                logger.info("Schematic geladen: " + file.getName() +
                        " (Größe: " + clipboard.getDimensions() + ")");
                return clipboard;
            }

        } catch (Exception e) {
            logger.severe("Fehler beim Laden der Schematic: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fügt eine Schematic an einer Position ein
     * @param clipboard Die geladene Schematic
     * @param location Ziel-Position (Bukkit Location)
     * @param ignoreAir Soll Luft ignoriert werden?
     * @return true bei Erfolg
     */
    public boolean pasteSchematic(Clipboard clipboard, Location location, boolean ignoreAir) {
        if (clipboard == null) {
            logger.warning("Kann null-Clipboard nicht einfügen!");
            return false;
        }

        try {
            WorldEdit worldEdit = WorldEdit.getInstance();
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());

            // Erstelle EditSession
            try (EditSession editSession = worldEdit.newEditSession(world)) {

                // Erstelle Paste-Operation
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                        .ignoreAirBlocks(ignoreAir)
                        .build();

                // Führe Operation aus
                Operations.complete(operation);

                logger.info("Schematic eingefügt bei: " + location.getBlockX() +
                        ", " + location.getBlockY() + ", " + location.getBlockZ());
                return true;
            }

        } catch (Exception e) {
            logger.severe("Fehler beim Einfügen der Schematic: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Hilfsmethode: Lädt und fügt Schematic direkt ein
     * @param file Schematic-Datei
     * @param location Ziel-Position
     * @return true bei Erfolg
     */
    public boolean loadAndPaste(File file, Location location) {
        Clipboard clipboard = loadSchematic(file);
        if (clipboard == null) return false;

        return pasteSchematic(clipboard, location, false);
    }

    /**
     * Gibt Dimensionen einer Schematic zurück
     * @param clipboard Die Schematic
     * @return Dimensions als int-Array [x, y, z]
     */
    public int[] getDimensions(Clipboard clipboard) {
        if (clipboard == null) return new int[]{0, 0, 0};

        BlockVector3 dimensions = clipboard.getDimensions();
        return new int[]{dimensions.getBlockX(), dimensions.getBlockY(), dimensions.getBlockZ()};
    }
}