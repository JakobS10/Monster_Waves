package de.challengeplugin.models;

import java.util.*;

/**
 * Repräsentiert eine laufende Challenge-Instanz
 */
public class Challenge {

    // Challenge-Identifikation
    private final UUID challengeId;
    private final long startTime; // System.currentTimeMillis()

    // Konfiguration
    private final long farmDurationTicks;   // Farmzeit in Ticks (20 ticks = 1 sec)
    private final UUID bossPlayerId;
    private final boolean netherEnabled;
    private final boolean endEnabled;
    private final boolean bossParticipates;

    // Spieler-Daten
    private final Set<UUID> participants;   // Alle teilnehmenden Spieler
    private final Map<UUID, PlayerChallengeData> playerData;

    // Wave-Definitionen (vom Boss vorbereitet)
    private final Map<UUID, List<Wave>> playerWaves; // Pro Spieler 3 Waves

    // Status
    private ChallengePhase currentPhase;
    private long phaseStartTick;           // Timer-Tick bei Phasenstart

    // Arenen
    private final Map<UUID, UUID> playerArenaMapping; // Spieler -> ArenaInstance-ID

    public enum ChallengePhase {
        SETUP,           // Boss bereitet Waves vor
        FARMING,         // Spieler farmen
        COMBAT,          // Kämpfe laufen
        EVALUATION       // Auswertung
    }

    // Konstruktor und Getter/Setter...
}