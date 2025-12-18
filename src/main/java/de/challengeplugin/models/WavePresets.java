package de.challengeplugin.models;

import org.bukkit.entity.EntityType;
import java.util.*;

/**
 * ÜBERARBEITET: Keine Creeper & Endermen mehr!
 * - Creeper entfernt (töten andere Mobs)
 * - Endermen entfernt (teleportieren aus Arena)
 * - Wither-Waves sind jetzt SOLO (keine Begleit-Mobs)
 * - Alle anderen Mobs behalten (Barrier-Arena hält alles)
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

    public static List<List<EntityType>> getPreset(Difficulty difficulty) {
        return getPreset(difficulty, 3);
    }

    // ==================== EASY ====================

    private static List<List<EntityType>> getEasyWaves(int count) {
        List<List<EntityType>> waves = new ArrayList<>();

        if (count == 1) {
            waves.add(createWave(
                    10, EntityType.ZOMBIE,
                    5, EntityType.SKELETON
            ));
        } else if (count == 3) {
            waves.add(createWave(10, EntityType.ZOMBIE));
            waves.add(createWave(8, EntityType.SKELETON));
            waves.add(createWave(
                    5, EntityType.ZOMBIE,
                    5, EntityType.SKELETON
            ));
        } else if (count == 5) {
            waves.add(createWave(8, EntityType.ZOMBIE));
            waves.add(createWave(6, EntityType.SKELETON));
            waves.add(createWave(10, EntityType.ZOMBIE));
            waves.add(createWave(8, EntityType.SKELETON, 2, EntityType.SPIDER));
            waves.add(createWave(
                    7, EntityType.ZOMBIE,
                    7, EntityType.SKELETON
            ));
        } else if (count == 10) {
            waves.add(createWave(6, EntityType.ZOMBIE));
            waves.add(createWave(5, EntityType.SKELETON));
            waves.add(createWave(8, EntityType.ZOMBIE));
            waves.add(createWave(6, EntityType.SKELETON));
            waves.add(createWave(5, EntityType.ZOMBIE, 3, EntityType.SPIDER));
            waves.add(createWave(10, EntityType.ZOMBIE));
            waves.add(createWave(8, EntityType.SKELETON));
            waves.add(createWave(6, EntityType.ZOMBIE, 6, EntityType.SKELETON));
            waves.add(createWave(8, EntityType.SKELETON, 4, EntityType.SPIDER));
            waves.add(createWave(
                    10, EntityType.ZOMBIE,
                    8, EntityType.SKELETON
            ));
        } else if (count == 15) {
            for (int i = 0; i < 5; i++) {
                waves.add(createWave(6 + i, EntityType.ZOMBIE));
            }
            for (int i = 0; i < 5; i++) {
                waves.add(createWave(5 + i, EntityType.SKELETON, i, EntityType.SPIDER));
            }
            for (int i = 0; i < 5; i++) {
                waves.add(createWave(
                        5 + i, EntityType.ZOMBIE,
                        5 + i, EntityType.SKELETON
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
                    5, EntityType.SPIDER
            ));
        } else if (count == 3) {
            waves.add(createWave(15, EntityType.ZOMBIE, 5, EntityType.SPIDER));
            waves.add(createWave(10, EntityType.SKELETON));
            waves.add(createWave(
                    8, EntityType.ZOMBIE,
                    8, EntityType.SKELETON,
                    3, EntityType.SPIDER
            ));
        } else if (count == 5) {
            waves.add(createWave(12, EntityType.ZOMBIE, 3, EntityType.SPIDER));
            waves.add(createWave(10, EntityType.SKELETON));
            waves.add(createWave(15, EntityType.ZOMBIE, 5, EntityType.SPIDER));
            waves.add(createWave(12, EntityType.SKELETON));
            waves.add(createWave(
                    10, EntityType.ZOMBIE,
                    10, EntityType.SKELETON,
                    5, EntityType.SPIDER
            ));
        } else if (count == 10) {
            for (int i = 0; i < 10; i++) {
                waves.add(createWave(
                        10 + i, EntityType.ZOMBIE,
                        8 + i, EntityType.SKELETON,
                        3 + (i / 2), EntityType.SPIDER
                ));
            }
        } else if (count == 15) {
            for (int i = 0; i < 15; i++) {
                waves.add(createWave(
                        10 + i, EntityType.ZOMBIE,
                        8 + (i / 2), EntityType.SKELETON,
                        5 + (i / 3), EntityType.SPIDER,
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
                    3, EntityType.WITCH
            ));
        } else if (count == 3) {
            waves.add(createWave(20, EntityType.ZOMBIE, 10, EntityType.SPIDER));
            waves.add(createWave(15, EntityType.SKELETON, 3, EntityType.WITCH));
            waves.add(createWave(
                    15, EntityType.ZOMBIE,
                    15, EntityType.SKELETON,
                    8, EntityType.SPIDER
            ));
        } else if (count == 5) {
            waves.add(createWave(18, EntityType.ZOMBIE, 7, EntityType.SPIDER));
            waves.add(createWave(15, EntityType.SKELETON));
            waves.add(createWave(20, EntityType.ZOMBIE, 10, EntityType.SPIDER, 2, EntityType.WITCH));
            waves.add(createWave(18, EntityType.SKELETON));
            waves.add(createWave(
                    20, EntityType.ZOMBIE,
                    18, EntityType.SKELETON,
                    10, EntityType.SPIDER,
                    3, EntityType.WITCH
            ));
        } else if (count == 10) {
            for (int i = 0; i < 10; i++) {
                waves.add(createWave(
                        15 + (i * 2), EntityType.ZOMBIE,
                        12 + (i * 2), EntityType.SKELETON,
                        8 + i, EntityType.SPIDER,
                        i / 2, EntityType.WITCH
                ));
            }
        } else if (count == 15) {
            for (int i = 0; i < 15; i++) {
                waves.add(createWave(
                        15 + (i * 2), EntityType.ZOMBIE,
                        12 + (i * 2), EntityType.SKELETON,
                        8 + i, EntityType.SPIDER,
                        i / 2, EntityType.WITCH,
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
                    10, EntityType.WITCH,
                    3, EntityType.BLAZE,
                    1, EntityType.RAVAGER
            ));
        } else if (count == 3) {
            waves.add(createWave(30, EntityType.ZOMBIE, 15, EntityType.SPIDER, 5, EntityType.WITCH));
            waves.add(createWave(
                    20, EntityType.SKELETON,
                    5, EntityType.BLAZE,
                    1, EntityType.RAVAGER
            ));
            // Wave 3: WITHER SOLO!
            waves.add(createWave(1, EntityType.WITHER));
        } else if (count == 5) {
            waves.add(createWave(25, EntityType.ZOMBIE, 12, EntityType.SPIDER, 3, EntityType.WITCH));
            waves.add(createWave(20, EntityType.SKELETON));
            waves.add(createWave(30, EntityType.ZOMBIE, 15, EntityType.SPIDER, 5, EntityType.WITCH, 1, EntityType.RAVAGER));
            waves.add(createWave(
                    25, EntityType.SKELETON,
                    5, EntityType.BLAZE
            ));
            // Wave 5: WITHER SOLO!
            waves.add(createWave(1, EntityType.WITHER));
        } else if (count == 10) {
            for (int i = 0; i < 9; i++) {
                waves.add(createWave(
                        20 + (i * 3), EntityType.ZOMBIE,
                        18 + (i * 3), EntityType.SKELETON,
                        12 + (i * 2), EntityType.SPIDER,
                        5 + (i / 2), EntityType.WITCH,
                        i / 2, EntityType.BLAZE,
                        i / 4, EntityType.RAVAGER
                ));
            }
            // Wave 10: WITHER SOLO!
            waves.add(createWave(1, EntityType.WITHER));
        } else if (count == 15) {
            for (int i = 0; i < 14; i++) {
                waves.add(createWave(
                        20 + (i * 2), EntityType.ZOMBIE,
                        18 + (i * 2), EntityType.SKELETON,
                        12 + (i * 2), EntityType.SPIDER,
                        5 + i, EntityType.WITCH,
                        i / 2, EntityType.BLAZE,
                        i / 3, EntityType.RAVAGER
                ));
            }
            // Wave 15: MEGA WITHER SOLO! (2 Wither)
            waves.add(createWave(2, EntityType.WITHER));
        }

        return waves;
    }

    // ==================== HELPER ====================

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