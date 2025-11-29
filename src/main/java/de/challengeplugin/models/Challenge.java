package de.challengeplugin.models;

import java.util.*;

/**
 * Repräsentiert eine laufende Challenge-Instanz
 */
public class Challenge {

    // Challenge-Identifikation
    private UUID challengeId;
    private long startTime;

    // Konfiguration
    private long farmDurationTicks;
    private UUID bossPlayerId;
    private boolean netherEnabled;
    private boolean endEnabled;
    private boolean bossParticipates;

    // NEU: Team-Konfiguration
    private TeamMode teamMode;
    private Map<UUID, List<UUID>> teams; // Team-ID -> Liste von Spieler-UUIDs
    private Map<UUID, UUID> playerToTeam; // Spieler-UUID -> Team-ID

    // Spieler-Daten
    private Set<UUID> participants;
    private Map<UUID, PlayerChallengeData> playerData;

    // Wave-Definitionen (NEU: Pro Team statt pro Spieler!)
    private Map<UUID, List<Wave>> teamWaves; // Team-ID -> 3 Waves

    // Status
    private ChallengePhase currentPhase;
    private long phaseStartTick;

    // Arenen (NEU: Pro Team!)
    private Map<UUID, UUID> teamArenaMapping; // Team-ID -> ArenaInstance-ID

    public enum ChallengePhase {
        SETUP,
        FARMING,
        COMBAT,
        EVALUATION
    }

    // NEU: Team-Modi
    public enum TeamMode {
        SOLO(1),
        DUO(2),
        TRIO(3);

        private final int teamSize;

        TeamMode(int teamSize) {
            this.teamSize = teamSize;
        }

        public int getTeamSize() {
            return teamSize;
        }
    }

    // Konstruktor
    public Challenge() {
        this.challengeId = UUID.randomUUID();
        this.startTime = System.currentTimeMillis();
        this.participants = new HashSet<>();
        this.playerData = new HashMap<>();
        this.teamWaves = new HashMap<>();
        this.teamArenaMapping = new HashMap<>();
        this.teams = new HashMap<>();
        this.playerToTeam = new HashMap<>();
        this.currentPhase = ChallengePhase.SETUP;
        this.teamMode = TeamMode.SOLO; // Standard
    }

    // Konstruktor mit Parametern
    public Challenge(UUID challengeId, long farmDurationTicks, UUID bossPlayerId) {
        this();
        this.challengeId = challengeId;
        this.farmDurationTicks = farmDurationTicks;
        this.bossPlayerId = bossPlayerId;
    }

    // Methode zum Hinzufügen von Teilnehmern
    public void addParticipant(UUID playerId) {
        this.participants.add(playerId);
    }

    /**
     * NEU: Erstellt Teams basierend auf TeamMode
     */
    public void createTeams() {
        teams.clear();
        playerToTeam.clear();

        List<UUID> allPlayers = new ArrayList<>(participants);
        // Entferne Boss wenn er nicht mitspielt
        if (!bossParticipates) {
            allPlayers.remove(bossPlayerId);
        }

        int teamSize = teamMode.getTeamSize();
        int teamCount = (int) Math.ceil((double) allPlayers.size() / teamSize);

        for (int i = 0; i < teamCount; i++) {
            UUID teamId = UUID.randomUUID();
            List<UUID> teamMembers = new ArrayList<>();

            // Fülle Team mit Spielern
            int start = i * teamSize;
            int end = Math.min(start + teamSize, allPlayers.size());

            for (int j = start; j < end; j++) {
                UUID playerId = allPlayers.get(j);
                teamMembers.add(playerId);
                playerToTeam.put(playerId, teamId);
            }

            teams.put(teamId, teamMembers);
        }
    }

    /**
     * NEU: Gibt Team eines Spielers zurück
     */
    public UUID getTeamOfPlayer(UUID playerId) {
        return playerToTeam.get(playerId);
    }

    /**
     * NEU: Gibt alle Mitglieder eines Teams zurück
     */
    public List<UUID> getTeamMembers(UUID teamId) {
        return teams.getOrDefault(teamId, new ArrayList<>());
    }

    // === GETTER UND SETTER ===

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getFarmDurationTicks() {
        return farmDurationTicks;
    }

    public void setFarmDurationTicks(long farmDurationTicks) {
        this.farmDurationTicks = farmDurationTicks;
    }

    public UUID getBossPlayerId() {
        return bossPlayerId;
    }

    public void setBossPlayerId(UUID bossPlayerId) {
        this.bossPlayerId = bossPlayerId;
    }

    public boolean isNetherEnabled() {
        return netherEnabled;
    }

    public void setNetherEnabled(boolean netherEnabled) {
        this.netherEnabled = netherEnabled;
    }

    public boolean isEndEnabled() {
        return endEnabled;
    }

    public void setEndEnabled(boolean endEnabled) {
        this.endEnabled = endEnabled;
    }

    public boolean isBossParticipates() {
        return bossParticipates;
    }

    public void setBossParticipates(boolean bossParticipates) {
        this.bossParticipates = bossParticipates;
    }

    // NEU: Team-Getter/Setter
    public TeamMode getTeamMode() {
        return teamMode;
    }

    public void setTeamMode(TeamMode teamMode) {
        this.teamMode = teamMode;
    }

    public Map<UUID, List<UUID>> getTeams() {
        return teams;
    }

    public void setTeams(Map<UUID, List<UUID>> teams) {
        this.teams = teams;
    }

    public Map<UUID, UUID> getPlayerToTeam() {
        return playerToTeam;
    }

    public void setPlayerToTeam(Map<UUID, UUID> playerToTeam) {
        this.playerToTeam = playerToTeam;
    }

    public Map<UUID, List<Wave>> getTeamWaves() {
        return teamWaves;
    }

    public void setTeamWaves(Map<UUID, List<Wave>> teamWaves) {
        this.teamWaves = teamWaves;
    }

    public Map<UUID, UUID> getTeamArenaMapping() {
        return teamArenaMapping;
    }

    public void setTeamArenaMapping(Map<UUID, UUID> teamArenaMapping) {
        this.teamArenaMapping = teamArenaMapping;
    }

    // Bestehende Getter/Setter
    public Set<UUID> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<UUID> participants) {
        this.participants = participants;
    }

    public Map<UUID, PlayerChallengeData> getPlayerData() {
        return playerData;
    }

    public void setPlayerData(Map<UUID, PlayerChallengeData> playerData) {
        this.playerData = playerData;
    }

    public ChallengePhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(ChallengePhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public long getPhaseStartTick() {
        return phaseStartTick;
    }

    public void setPhaseStartTick(long phaseStartTick) {
        this.phaseStartTick = phaseStartTick;
    }

    // Alte Getter für Kompatibilität (deprecated)
    @Deprecated
    public Map<UUID, UUID> getPlayerArenaMapping() {
        return teamArenaMapping;
    }

    @Deprecated
    public Map<UUID, List<Wave>> getPlayerWaves() {
        return teamWaves;
    }
}