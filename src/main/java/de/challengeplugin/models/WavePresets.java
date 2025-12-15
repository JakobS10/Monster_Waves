package de.challengeplugin.models;

import org.bukkit.entity.EntityType;
import java.util.*;

/**
 * ERWEITERT: Variable Wave-Anzahl (1, 3, 5, 10, 15)
 * Jede Schwierigkeit hat jetzt für jede Wave-Anzahl passende Presets
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
     * NEU: Gibt Waves für Schwierigkeit UND Wave-Anzahl zurück
     */
    public static List<List<EntityType>> getPreset(Difficulty difficulty, int waveCount) {
        switch (difficulty) {
            case EASY:
                return getEasyWaves(waveCount);
            case MEDIUM:
                return getMediumWaves(waveCount);
            case HARD:
                return getHardWaves(waveCount);
            case EXTREME:
                return getExtremeWaves(waveCount);
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Legacy: 3 Waves (Standard)
     */
    public static List<List<EntityType>> getPreset(Difficulty difficulty) {
        return getPreset(difficulty, 3);
    }

    // ==================== EASY ====================

    private static List<List<EntityType>> getEasyWaves(int count) {
        List<List<EntityType>> waves = new ArrayList<>();

        if (count == 1) {
            // 1 Wave: Leicht gemischt
            waves.add(createWave(
                    10, EntityType.ZOMBIE,
                    5, EntityType.SKELETON
            ));
        } else if (count == 3) {
            // 3 Waves: Klassisch
            waves.add(createWave(10, EntityType.ZOMBIE));
            waves.add(createWave(8, EntityType.SKELETON));
            waves.add(createWave(
                    5, EntityType.ZOMBIE,
                    5, EntityType.SKELETON
            ));
        } else if (count == 5) {
            // 5 Waves: Sanfte Steigerung
            waves.add(createWave(8, EntityType.ZOMBIE));
            waves.add(createWave(6, EntityType.SKELETON));
            waves.add(createWave(10, EntityType.ZOMBIE));
            waves.add(createWave(8, EntityType.SKELETON, 2, EntityType.SPIDER));
            waves.add(createWave(
                    7, EntityType.ZOMBIE,
                    7, EntityType.SKELETON
            ));
        } else if (count == 10) {
            // 10 Waves: Allmähliche Progression
            waves.add(createWave(6, EntityType.ZOMBIE));
            waves.add(createWave(5, EntityType.SKELETON));
            waves.add(createWave(8, EntityType.ZOMBIE));
            waves.add(createWave(6, EntityType.SKELETON));
            waves.add(createWave(5, EntityType.ZOMBIE, 3, EntityType.SPIDER));
            waves.add(createWave(10, EntityType.ZOMBIE));
            waves.add(createWave(8, EntityType.SKELETON, 2, EntityType.CREEPER));
            waves.add(createWave(6, EntityType.ZOMBIE, 6, EntityType.SKELETON));
            waves.add(createWave(8, EntityType.SKELETON, 4, EntityType.SPIDER));
            waves.add(createWave(
                    10, EntityType.ZOMBIE,
                    8, EntityType.SKELETON,
                    3, EntityType.CREEPER
            ));
        } else if (count == 15) {
            // 15 Waves: Lange aber machbar
            for (int i = 0; i < 5; i++) {
                waves.add(createWave(6 + i, EntityType.ZOMBIE));
            }
            for (int i = 0; i < 5; i++) {
                waves.add(createWave(5 + i, EntityType.SKELETON, i, EntityType.SPIDER));
            }
            for (int i = 0; i < 5; i++) {
                waves.add(createWave(
                        5 + i, EntityType.ZOMBIE,
                        5 + i, EntityType.SKELETON,
                        i, EntityType.CREEPER
                ));
            }
        }

        return waves;
    }

    // ==================== MEDIUM ====================

    private static List<List<EntityType>> getMediumWaves(int count) {
        List<List<EntityType>> waves = new ArrayList<>();

        if (count == 1) {
            waves.add(createWave(
                    15, EntityType.ZOMBIE,
                    10, EntityType.SKELETON,
                    5, EntityType.SPIDER,
                    2, EntityType.CREEPER
            ));
        } else if (count == 3) {
            waves.add(createWave(15, EntityType.ZOMBIE, 5, EntityType.SPIDER));
            waves.add(createWave(10, EntityType.SKELETON, 3, EntityType.CREEPER));
            waves.add(createWave(
                    8, EntityType.ZOMBIE,
                    8, EntityType.SKELETON,
                    3, EntityType.SPIDER,
                    2, EntityType.CREEPER
            ));
        } else if (count == 5) {
            waves.add(createWave(12, EntityType.ZOMBIE, 3, EntityType.SPIDER));
            waves.add(createWave(10, EntityType.SKELETON));
            waves.add(createWave(15, EntityType.ZOMBIE, 5, EntityType.SPIDER));
            waves.add(createWave(12, EntityType.SKELETON, 4, EntityType.CREEPER));
            waves.add(createWave(
                    10, EntityType.ZOMBIE,
                    10, EntityType.SKELETON,
                    5, EntityType.SPIDER,
                    3, EntityType.CREEPER
            ));
        } else if (count == 10) {
            for (int i = 0; i < 10; i++) {
                waves.add(createWave(
                        10 + i, EntityType.ZOMBIE,
                        8 + i, EntityType.SKELETON,
                        3 + (i / 2), EntityType.SPIDER,
                        i / 3, EntityType.CREEPER
                ));
            }
        } else if (count == 15) {
            for (int i = 0; i < 15; i++) {
                waves.add(createWave(
                        10 + i, EntityType.ZOMBIE,
                        8 + (i / 2), EntityType.SKELETON,
                        5 + (i / 3), EntityType.SPIDER,
                        i / 2, EntityType.CREEPER,
                        i / 5, EntityType.WITCH
                ));
            }
        }

        return waves;
    }

    // ==================== HARD ====================

    private static List<List<EntityType>> getHardWaves(int count) {
        List<List<EntityType>> waves = new ArrayList<>();

        if (count == 1) {
            waves.add(createWave(
                    25, EntityType.ZOMBIE,
                    20, EntityType.SKELETON,
                    10, EntityType.SPIDER,
                    5, EntityType.CREEPER,
                    3, EntityType.WITCH
            ));
        } else if (count == 3) {
            waves.add(createWave(20, EntityType.ZOMBIE, 10, EntityType.SPIDER));
            waves.add(createWave(15, EntityType.SKELETON, 5, EntityType.CREEPER, 3, EntityType.WITCH));
            waves.add(createWave(
                    15, EntityType.ZOMBIE,
                    15, EntityType.SKELETON,
                    8, EntityType.SPIDER,
                    5, EntityType.CREEPER,
                    2, EntityType.ENDERMAN
            ));
        } else if (count == 5) {
            waves.add(createWave(18, EntityType.ZOMBIE, 7, EntityType.SPIDER));
            waves.add(createWave(15, EntityType.SKELETON, 3, EntityType.CREEPER));
            waves.add(createWave(20, EntityType.ZOMBIE, 10, EntityType.SPIDER, 2, EntityType.WITCH));
            waves.add(createWave(18, EntityType.SKELETON, 8, EntityType.CREEPER, 3, EntityType.ENDERMAN));
            waves.add(createWave(
                    20, EntityType.ZOMBIE,
                    18, EntityType.SKELETON,
                    10, EntityType.SPIDER,
                    5, EntityType.CREEPER,
                    3, EntityType.WITCH,
                    2, EntityType.ENDERMAN
            ));
        } else if (count == 10) {
            for (int i = 0; i < 10; i++) {
                waves.add(createWave(
                        15 + (i * 2), EntityType.ZOMBIE,
                        12 + (i * 2), EntityType.SKELETON,
                        8 + i, EntityType.SPIDER,
                        3 + (i / 2), EntityType.CREEPER,
                        i / 2, EntityType.WITCH,
                        i / 3, EntityType.ENDERMAN
                ));
            }
        } else if (count == 15) {
            for (int i = 0; i < 15; i++) {
                waves.add(createWave(
                        15 + (i * 2), EntityType.ZOMBIE,
                        12 + (i * 2), EntityType.SKELETON,
                        8 + i, EntityType.SPIDER,
                        3 + i, EntityType.CREEPER,
                        i / 2, EntityType.WITCH,
                        i / 2, EntityType.ENDERMAN,
                        i / 5, EntityType.BLAZE
                ));
            }
        }

        return waves;
    }

    // ==================== EXTREME ====================

    private static List<List<EntityType>> getExtremeWaves(int count) {
        List<List<EntityType>> waves = new ArrayList<>();

        if (count == 1) {
            waves.add(createWave(
                    40, EntityType.ZOMBIE,
                    35, EntityType.SKELETON,
                    20, EntityType.SPIDER,
                    15, EntityType.CREEPER,
                    10, EntityType.WITCH,
                    5, EntityType.ENDERMAN,
                    3, EntityType.BLAZE,
                    1, EntityType.RAVAGER
            ));
        } else if (count == 3) {
            waves.add(createWave(30, EntityType.ZOMBIE, 15, EntityType.SPIDER, 5, EntityType.WITCH));
            waves.add(createWave(
                    20, EntityType.SKELETON,
                    10, EntityType.CREEPER,
                    5, EntityType.ENDERMAN,
                    1, EntityType.RAVAGER
            ));
            waves.add(createWave(
                    25, EntityType.ZOMBIE,
                    20, EntityType.SKELETON,
                    10, EntityType.SPIDER,
                    8, EntityType.CREEPER,
                    5, EntityType.WITCH,
                    3, EntityType.ENDERMAN,
                    3, EntityType.BLAZE,
                    1, EntityType.WITHER
            ));
        } else if (count == 5) {
            waves.add(createWave(25, EntityType.ZOMBIE, 12, EntityType.SPIDER, 3, EntityType.WITCH));
            waves.add(createWave(20, EntityType.SKELETON, 8, EntityType.CREEPER, 2, EntityType.ENDERMAN));
            waves.add(createWave(30, EntityType.ZOMBIE, 15, EntityType.SPIDER, 5, EntityType.WITCH, 1, EntityType.RAVAGER));
            waves.add(createWave(
                    25, EntityType.SKELETON,
                    15, EntityType.CREEPER,
                    8, EntityType.ENDERMAN,
                    5, EntityType.BLAZE
            ));
            waves.add(createWave(
                    30, EntityType.ZOMBIE,
                    25, EntityType.SKELETON,
                    15, EntityType.SPIDER,
                    10, EntityType.CREEPER,
                    8, EntityType.WITCH,
                    5, EntityType.ENDERMAN,
                    3, EntityType.BLAZE,
                    1, EntityType.WITHER
            ));
        } else if (count == 10) {
            for (int i = 0; i < 9; i++) {
                waves.add(createWave(
                        20 + (i * 3), EntityType.ZOMBIE,
                        18 + (i * 3), EntityType.SKELETON,
                        12 + (i * 2), EntityType.SPIDER,
                        8 + i, EntityType.CREEPER,
                        5 + (i / 2), EntityType.WITCH,
                        3 + (i / 2), EntityType.ENDERMAN,
                        i / 2, EntityType.BLAZE,
                        i / 4, EntityType.RAVAGER
                ));
            }
            // Boss-Wave
            waves.add(createWave(
                    40, EntityType.ZOMBIE,
                    35, EntityType.SKELETON,
                    20, EntityType.SPIDER,
                    15, EntityType.CREEPER,
                    10, EntityType.WITCH,
                    8, EntityType.ENDERMAN,
                    5, EntityType.BLAZE,
                    2, EntityType.RAVAGER,
                    1, EntityType.WITHER
            ));
        } else if (count == 15) {
            for (int i = 0; i < 14; i++) {
                waves.add(createWave(
                        20 + (i * 2), EntityType.ZOMBIE,
                        18 + (i * 2), EntityType.SKELETON,
                        12 + (i * 2), EntityType.SPIDER,
                        8 + i, EntityType.CREEPER,
                        5 + i, EntityType.WITCH,
                        3 + (i / 2), EntityType.ENDERMAN,
                        i / 2, EntityType.BLAZE,
                        i / 3, EntityType.RAVAGER
                ));
            }
            // Mega-Boss-Wave
            waves.add(createWave(
                    50, EntityType.ZOMBIE,
                    45, EntityType.SKELETON,
                    30, EntityType.SPIDER,
                    20, EntityType.CREEPER,
                    15, EntityType.WITCH,
                    10, EntityType.ENDERMAN,
                    8, EntityType.BLAZE,
                    3, EntityType.RAVAGER,
                    2, EntityType.WITHER
            ));
        }

        return waves;
    }

    // ==================== HELPER ====================

    /**
     * Hilfsmethode: Erstellt Wave mit Mob-Paaren (Anzahl, Type)
     */
    private static List<EntityType> createWave(Object... mobPairs) {
        List<EntityType> wave = new ArrayList<>();

        for (int i = 0; i < mobPairs.length; i += 2) {
            int count = (Integer) mobPairs[i];
            EntityType type = (EntityType) mobPairs[i + 1];

            for (int j = 0; j < count; j++) {
                wave.add(type);
            }
        }

        return wave;
    }

    /**
     * Gibt Beschreibung für Wave-Anzahl zurück
     */
    public static String getWaveCountDescription(int count) {
        switch (count) {
            case 1: return "§7Schnell & Intensiv";
            case 3: return "§7Klassisch";
            case 5: return "§7Ausgewogen";
            case 10: return "§7Lange Session";
            case 15: return "§7Marathon";
            default: return "§7Custom";
        }
    }
}