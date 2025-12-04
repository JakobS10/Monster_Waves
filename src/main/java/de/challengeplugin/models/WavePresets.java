package de.challengeplugin.models;

import org.bukkit.entity.EntityType;
import java.util.*;

/**
 * Vordefinierte Wave-Presets mit verschiedenen Schwierigkeitsgraden
 */
public class WavePresets {

    public enum Difficulty {
        EASY("§a§lEinfach", "§7Für Anfänger"),
        MEDIUM("§e§lMittel", "§7Ausgewogene Herausforderung"),
        HARD("§c§lSchwer", "§7Für erfahrene Spieler"),
        EXTREME("§4§lExtrem", "§7Nur für Profis"),
        CUSTOM("§6§lCustom", "§7Eigene Waves erstellen");

        private final String displayName;
        private final String description;

        Difficulty(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Gibt vordefinierte Waves für Schwierigkeit zurück
     * @param difficulty Schwierigkeitsgrad
     * @return Liste von 3 Waves, jede Wave ist eine Liste von EntityTypes
     */
    public static List<List<EntityType>> getPreset(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return getEasyWaves();
            case MEDIUM:
                return getMediumWaves();
            case HARD:
                return getHardWaves();
            case EXTREME:
                return getExtremeWaves();
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Gibt Beschreibung der Waves zurück
     */
    public static List<String> getWaveDescription(Difficulty difficulty) {
        List<String> descriptions = new ArrayList<>();

        switch (difficulty) {
            case EASY:
                descriptions.add("§7Wave 1: §e10 Zombies");
                descriptions.add("§7Wave 2: §e8 Skeletons");
                descriptions.add("§7Wave 3: §e5 Zombies + 5 Skeletons");
                break;
            case MEDIUM:
                descriptions.add("§7Wave 1: §e15 Zombies + 5 Spiders");
                descriptions.add("§7Wave 2: §e10 Skeletons + 3 Creepers");
                descriptions.add("§7Wave 3: §eMixed (15 Mobs)");
                break;
            case HARD:
                descriptions.add("§7Wave 1: §e20 Zombies + 10 Spiders");
                descriptions.add("§7Wave 2: §e15 Skeletons + 5 Creepers + 3 Witches");
                descriptions.add("§7Wave 3: §eMixed + Endermen (30 Mobs)");
                break;
            case EXTREME:
                descriptions.add("§7Wave 1: §e30 Zombies + 15 Spiders + 5 Witches");
                descriptions.add("§7Wave 2: §e20 Skeletons + 10 Creepers + 5 Endermen + Ravager");
                descriptions.add("§7Wave 3: §c§lWITHER §7+ 30 Support-Mobs");
                break;
            case CUSTOM:
                descriptions.add("§7Definiere deine eigenen Waves");
                descriptions.add("§7mit Spawn Eggs im Inventar");
                break;
        }

        return descriptions;
    }

    // === PRESET-DEFINITIONEN ===

    private static List<List<EntityType>> getEasyWaves() {
        List<List<EntityType>> waves = new ArrayList<>();

        // Wave 1: 10 Zombies
        List<EntityType> wave1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            wave1.add(EntityType.ZOMBIE);
        }
        waves.add(wave1);

        // Wave 2: 8 Skeletons
        List<EntityType> wave2 = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            wave2.add(EntityType.SKELETON);
        }
        waves.add(wave2);

        // Wave 3: 5 Zombies + 5 Skeletons
        List<EntityType> wave3 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            wave3.add(EntityType.ZOMBIE);
            wave3.add(EntityType.SKELETON);
        }
        waves.add(wave3);

        return waves;
    }

    private static List<List<EntityType>> getMediumWaves() {
        List<List<EntityType>> waves = new ArrayList<>();

        // Wave 1: 15 Zombies + 5 Spiders
        List<EntityType> wave1 = new ArrayList<>();
        for (int i = 0; i < 15; i++) wave1.add(EntityType.ZOMBIE);
        for (int i = 0; i < 5; i++) wave1.add(EntityType.SPIDER);
        waves.add(wave1);

        // Wave 2: 10 Skeletons + 3 Creepers
        List<EntityType> wave2 = new ArrayList<>();
        for (int i = 0; i < 10; i++) wave2.add(EntityType.SKELETON);
        for (int i = 0; i < 3; i++) wave2.add(EntityType.CREEPER);
        waves.add(wave2);

        // Wave 3: Mixed
        List<EntityType> wave3 = new ArrayList<>();
        for (int i = 0; i < 5; i++) wave3.add(EntityType.ZOMBIE);
        for (int i = 0; i < 5; i++) wave3.add(EntityType.SKELETON);
        for (int i = 0; i < 3; i++) wave3.add(EntityType.SPIDER);
        for (int i = 0; i < 2; i++) wave3.add(EntityType.CREEPER);
        waves.add(wave3);

        return waves;
    }

    private static List<List<EntityType>> getHardWaves() {
        List<List<EntityType>> waves = new ArrayList<>();

        // Wave 1: 20 Zombies + 10 Spiders
        List<EntityType> wave1 = new ArrayList<>();
        for (int i = 0; i < 20; i++) wave1.add(EntityType.ZOMBIE);
        for (int i = 0; i < 10; i++) wave1.add(EntityType.SPIDER);
        waves.add(wave1);

        // Wave 2: 15 Skeletons + 5 Creepers + 3 Witches
        List<EntityType> wave2 = new ArrayList<>();
        for (int i = 0; i < 15; i++) wave2.add(EntityType.SKELETON);
        for (int i = 0; i < 5; i++) wave2.add(EntityType.CREEPER);
        for (int i = 0; i < 3; i++) wave2.add(EntityType.WITCH);
        waves.add(wave2);

        // Wave 3: Heavy Mixed
        List<EntityType> wave3 = new ArrayList<>();
        for (int i = 0; i < 10; i++) wave3.add(EntityType.ZOMBIE);
        for (int i = 0; i < 10; i++) wave3.add(EntityType.SKELETON);
        for (int i = 0; i < 5; i++) wave3.add(EntityType.SPIDER);
        for (int i = 0; i < 3; i++) wave3.add(EntityType.CREEPER);
        for (int i = 0; i < 2; i++) wave3.add(EntityType.ENDERMAN);
        waves.add(wave3);

        return waves;
    }

    private static List<List<EntityType>> getExtremeWaves() {
        List<List<EntityType>> waves = new ArrayList<>();

        // Wave 1: 30 Zombies + 15 Spiders + 5 Witches
        List<EntityType> wave1 = new ArrayList<>();
        for (int i = 0; i < 30; i++) wave1.add(EntityType.ZOMBIE);
        for (int i = 0; i < 15; i++) wave1.add(EntityType.SPIDER);
        for (int i = 0; i < 5; i++) wave1.add(EntityType.WITCH);
        waves.add(wave1);

        // Wave 2: 20 Skeletons + 10 Creepers + 5 Endermen + 1 Ravager
        List<EntityType> wave2 = new ArrayList<>();
        for (int i = 0; i < 20; i++) wave2.add(EntityType.SKELETON);
        for (int i = 0; i < 10; i++) wave2.add(EntityType.CREEPER);
        for (int i = 0; i < 5; i++) wave2.add(EntityType.ENDERMAN);
        wave2.add(EntityType.RAVAGER);
        waves.add(wave2);

        // Wave 3: Boss-Wave mit Wither
        List<EntityType> wave3 = new ArrayList<>();
        wave3.add(EntityType.WITHER);
        for (int i = 0; i < 15; i++) wave3.add(EntityType.ZOMBIE);
        for (int i = 0; i < 10; i++) wave3.add(EntityType.SKELETON);
        for (int i = 0; i < 5; i++) wave3.add(EntityType.BLAZE);
        waves.add(wave3);

        return waves;
    }
}