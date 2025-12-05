package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import de.challengeplugin.models.Wave;
import de.challengeplugin.models.WavePresets;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Verwaltet Boss-Setup-Prozess
 * GUI für Team-Mode, Dimensionen, Teilnahme und Wave-Definition
 */
public class BossSetupManager {

    private final ChallengePlugin plugin;

    // Tracking für laufende Setup-Prozesse
    private final Map<UUID, SetupContext> activeSetups = new HashMap<>();

    public BossSetupManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Startet kompletten Setup-Prozess
     */
    public void startCompleteSetup(Player boss) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge == null) {
            boss.sendMessage("§cKeine aktive Challenge!");
            return;
        }

        boss.sendMessage("§6§l=== Challenge-Setup ===");
        boss.sendMessage("§7Schritt 1: Team-Modus wählen");

        // Schritt 1: Team-Mode
        openTeamModeGUI(boss, (teamMode) -> {
            challenge.setTeamMode(teamMode);
            boss.sendMessage("§a✓ Team-Modus: §e" + teamMode.name() + " §7(Größe: " + teamMode.getTeamSize() + ")");

            // Schritt 2: Team-Assignment-Mode (Manuell vs Auto)
            boss.sendMessage("§7Schritt 2: Team-Erstellung");
            openTeamAssignmentModeGUI(boss, (isManual) -> {

                if (isManual) {
                    // === MANUELL ===
                    boss.sendMessage("§eManuelles Team-Building wird geöffnet...");

                    // Öffne Team-Builder mit Callback
                    openTeamBuilder(boss, (v) -> {
                        // Nach Team-Builder weiter
                        boss.sendMessage("§a✓ Teams manuell erstellt");
                        continueAfterTeamCreation(boss);
                    });

                } else {
                    // === AUTOMATISCH ===
                    boss.sendMessage("§7Schritt 3: Teilnahme wählen");

                    openParticipationGUI(boss, (participates) -> {
                        challenge.setBossParticipates(participates);
                        boss.sendMessage("§a✓ Du spielst " + (participates ? "MIT" : "NICHT mit"));

                        // Erstelle Teams automatisch
                        boss.sendMessage("§eErstelle Teams automatisch...");
                        challenge.createTeams();
                        boss.sendMessage("§a✓ Teams automatisch erstellt");

                        // Weiter
                        continueAfterTeamCreation(boss);
                    });
                }
            });
        });
    }

    /**
     * Öffnet Preset-Auswahl GUI für ein Team
     */
    public void openPresetSelectionGUI(Player boss, UUID teamId, Consumer<WavePresets.Difficulty> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Wave-Schwierigkeit");

        // Easy
        ItemStack easy = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta easyMeta = easy.getItemMeta();
        easyMeta.setDisplayName(WavePresets.Difficulty.EASY.getDisplayName());
        easyMeta.setLore(Arrays.asList(
                "§710 Zombies",
                "§78 Skeletons",
                "§75 Zombies + 5 Skeletons"
        ));
        easy.setItemMeta(easyMeta);

        // Medium
        ItemStack medium = new ItemStack(Material.IRON_SWORD);
        ItemMeta mediumMeta = medium.getItemMeta();
        mediumMeta.setDisplayName(WavePresets.Difficulty.MEDIUM.getDisplayName());
        mediumMeta.setLore(Arrays.asList(
                "§715 Zombies + 5 Spiders",
                "§710 Skeletons + 3 Creepers",
                "§7Mixed Wave"
        ));
        medium.setItemMeta(mediumMeta);

        // Hard
        ItemStack hard = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta hardMeta = hard.getItemMeta();
        hardMeta.setDisplayName(WavePresets.Difficulty.HARD.getDisplayName());
        hardMeta.setLore(Arrays.asList(
                "§720 Zombies + 10 Spiders",
                "§715 Skeletons + 5 Creepers + 3 Witches",
                "§7Heavy Mixed Wave"
        ));
        hard.setItemMeta(hardMeta);

        // Extreme
        ItemStack extreme = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta extremeMeta = extreme.getItemMeta();
        extremeMeta.setDisplayName(WavePresets.Difficulty.EXTREME.getDisplayName());
        extremeMeta.setLore(Arrays.asList(
                "§730 Zombies + 15 Spiders + 5 Witches",
                "§720 Skeletons + 10 Creepers + 5 Endermen + Ravager",
                "§7Boss Wave: Wither + Support"
        ));
        extreme.setItemMeta(extremeMeta);

        // Custom
        ItemStack custom = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta customMeta = custom.getItemMeta();
        customMeta.setDisplayName(WavePresets.Difficulty.CUSTOM.getDisplayName());
        customMeta.setLore(Arrays.asList(
                "§7Definiere Waves selbst",
                "§7mit Spawn Eggs"
        ));
        custom.setItemMeta(customMeta);

        inv.setItem(10, easy);
        inv.setItem(12, medium);
        inv.setItem(14, hard);
        inv.setItem(16, extreme);
        inv.setItem(22, custom);

        boss.openInventory(inv);

        // Speichere Kontext
        SetupContext context = activeSetups.get(boss.getUniqueId());
        if (context != null) {
            context.presetCallback = callback;
            context.stage = SetupStage.PRESET_SELECTION;
        }
    }

    /**
     * Erstellt Waves aus Preset
     */
    private List<Wave> createWavesFromPreset(WavePresets.Difficulty difficulty, UUID teamId) {
        List<List<EntityType>> presetWaves = WavePresets.getPreset(difficulty);
        List<Wave> waves = new ArrayList<>();

        for (int i = 0; i < presetWaves.size(); i++) {
            Wave wave = new Wave(i + 1, teamId);
            for (EntityType type : presetWaves.get(i)) {
                wave.addMob(type);
            }
            waves.add(wave);
        }

        return waves;
    }

    /**
     * NEU: Öffnet GUI für Team-Mode-Auswahl
     */
    public void openTeamModeGUI(Player boss, Consumer<Challenge.TeamMode> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Team-Modus wählen");

        // Solo-Option
        ItemStack solo = new ItemStack(Material.IRON_SWORD);
        ItemMeta soloMeta = solo.getItemMeta();
        soloMeta.setDisplayName("§e§lSolo");
        soloMeta.setLore(Arrays.asList(
                "§7Jeder Spieler kämpft alleine",
                "",
                "§7Team-Größe: §e1 Spieler"
        ));
        solo.setItemMeta(soloMeta);

        // Duo-Option
        ItemStack duo = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta duoMeta = duo.getItemMeta();
        duoMeta.setDisplayName("§b§lDuo");
        duoMeta.setLore(Arrays.asList(
                "§7Spieler kämpfen zu zweit",
                "",
                "§7Team-Größe: §e2 Spieler"
        ));
        duo.setItemMeta(duoMeta);

        // Trio-Option
        ItemStack trio = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta trioMeta = trio.getItemMeta();
        trioMeta.setDisplayName("§d§lTrio");
        trioMeta.setLore(Arrays.asList(
                "§7Spieler kämpfen zu dritt",
                "",
                "§7Team-Größe: §e3 Spieler"
        ));
        trio.setItemMeta(trioMeta);

        inv.setItem(11, solo);
        inv.setItem(13, duo);
        inv.setItem(15, trio);

        boss.openInventory(inv);

        // Speichere Kontext
        SetupContext context = new SetupContext();
        context.stage = SetupStage.TEAM_MODE;
        context.teamModeCallback = callback;
        activeSetups.put(boss.getUniqueId(), context);
    }

    /**
     * Öffnet GUI für Dimension-Einstellungen
     */
    public void openDimensionSettingsGUI(Player boss, BiConsumer<Boolean, Boolean> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Welten-Zugang");

        ItemStack netherItem = new ItemStack(Material.NETHERRACK);
        ItemMeta netherMeta = netherItem.getItemMeta();
        netherMeta.setDisplayName("§cNether");
        netherMeta.setLore(Arrays.asList(
                "§7Klicke um Nether zu aktivieren",
                "",
                "§7Status: §cDeaktiviert"
        ));
        netherItem.setItemMeta(netherMeta);

        ItemStack endItem = new ItemStack(Material.END_STONE);
        ItemMeta endMeta = endItem.getItemMeta();
        endMeta.setDisplayName("§dEnd");
        endMeta.setLore(Arrays.asList(
                "§7Klicke um End zu aktivieren",
                "",
                "§7Status: §cDeaktiviert"
        ));
        endItem.setItemMeta(endMeta);

        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§lBestätigen");
        confirmMeta.setLore(Arrays.asList("§7Klicke um fortzufahren"));
        confirm.setItemMeta(confirmMeta);

        inv.setItem(11, netherItem);
        inv.setItem(13, endItem);
        inv.setItem(22, confirm);

        boss.openInventory(inv);

        SetupContext context = new SetupContext();
        context.stage = SetupStage.DIMENSIONS;
        context.dimensionCallback = callback;
        activeSetups.put(boss.getUniqueId(), context);
    }

    /**
     * Öffnet GUI für Boss-Teilnahme-Entscheidung
     */
    public void openParticipationGUI(Player boss, Consumer<Boolean> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Boss-Teilnahme");

        ItemStack yes = new ItemStack(Material.LIME_WOOL);
        ItemMeta yesMeta = yes.getItemMeta();
        yesMeta.setDisplayName("§a§lJa, ich spiele mit");
        yesMeta.setLore(Arrays.asList(
                "§7Du kämpfst auch gegen die Mobs",
                "§7die du für dich definiert hast"
        ));
        yes.setItemMeta(yesMeta);

        ItemStack no = new ItemStack(Material.RED_WOOL);
        ItemMeta noMeta = no.getItemMeta();
        noMeta.setDisplayName("§c§lNein, nur organisieren");
        noMeta.setLore(Arrays.asList(
                "§7Du definierst nur die Waves",
                "§7und kämpfst nicht selbst"
        ));
        no.setItemMeta(noMeta);

        inv.setItem(11, yes);
        inv.setItem(15, no);

        boss.openInventory(inv);

        SetupContext context = activeSetups.get(boss.getUniqueId());
        if (context == null) {
            context = new SetupContext();
            activeSetups.put(boss.getUniqueId(), context);
        }
        context.stage = SetupStage.PARTICIPATION;
        context.participationCallback = callback;
    }

    // Tracking für Team-Builder
    private final Map<UUID, TeamBuilderGUI> activeTeamBuilders = new HashMap<>();

    /**
     * Öffnet Team-Builder GUI statt automatischer Team-Erstellung
     */
    /**
     * Öffnet Team-Builder GUI statt automatischer Team-Erstellung
     */
    public void openTeamBuilder(Player boss, Consumer<Void> onComplete) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        boss.sendMessage("§6§l=== Team-Builder ===");
        boss.sendMessage("§7Weise Spieler manuell zu Teams zu");
        boss.sendMessage("");

        TeamBuilderGUI teamBuilder = new TeamBuilderGUI(plugin, boss, challenge);
        activeTeamBuilders.put(boss.getUniqueId(), teamBuilder);

        // NEU: Setze Callback für Fertigstellung
        teamBuilder.setOnCompleteCallback(() -> {
            // Entferne Team-Builder
            activeTeamBuilders.remove(boss.getUniqueId());

            // Zeige Team-Übersicht
            showTeamOverview(boss);

            // Rufe onComplete auf
            onComplete.accept(null);
        });

        teamBuilder.open();
    }

    /**
     * Gibt aktiven Team-Builder zurück
     */
    public TeamBuilderGUI getTeamBuilder(UUID bossId) {
        return activeTeamBuilders.get(bossId);
    }

    /**
     * Entfernt Team-Builder
     */
    public void removeTeamBuilder(UUID bossId) {
        activeTeamBuilders.remove(bossId);
    }

    /**
     * Zeigt Team-Übersicht
     */
    private void showTeamOverview(Player boss) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        boss.sendMessage("§6§l=== Team-Übersicht ===");
        boss.sendMessage("§7Modus: §e" + challenge.getTeamMode().name());
        boss.sendMessage("§7Anzahl Teams: §e" + challenge.getTeams().size());
        boss.sendMessage("");

        int teamNumber = 1;
        for (Map.Entry<UUID, List<UUID>> entry : challenge.getTeams().entrySet()) {
            List<String> names = new ArrayList<>();
            for (UUID playerId : entry.getValue()) {
                Player p = Bukkit.getPlayer(playerId);
                names.add(p != null ? p.getName() : "???");
            }

            boss.sendMessage("§eTeam " + teamNumber + ": §7" + String.join(", ", names));
            teamNumber++;
        }
        boss.sendMessage("");
    }

    /**
     * Startet Wave-Setup für alle Teams
     */
    public void startWaveSetup(Player boss) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        SetupContext context = activeSetups.get(boss.getUniqueId());

        if (context == null) {
            context = new SetupContext();
            activeSetups.put(boss.getUniqueId(), context);
        }

        context.stage = SetupStage.WAVE_SETUP;
        context.teamsToSetup = new ArrayList<>(challenge.getTeams().keySet());
        context.currentTeamIndex = 0;
        context.setupCompleteCallback = () -> {
            plugin.getChallengeManager().startFarmingPhasePublic();
        };

        boss.setGameMode(GameMode.CREATIVE);
        boss.getInventory().clear();

        setupNextTeam(boss);
    }

    /**
     * NEU: GUI für manuelle vs. automatische Team-Erstellung
     */
    private void openTeamAssignmentModeGUI(Player boss, Consumer<Boolean> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Team-Erstellung");

        ItemStack manual = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta manualMeta = manual.getItemMeta();
        manualMeta.setDisplayName("§e§lManuell");
        manualMeta.setLore(Arrays.asList(
                "§7Du weist Spieler selbst",
                "§7zu Teams zu",
                "",
                "§aVolle Kontrolle"
        ));
        manual.setItemMeta(manualMeta);

        ItemStack auto = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta autoMeta = auto.getItemMeta();
        autoMeta.setDisplayName("§a§lAutomatisch");
        autoMeta.setLore(Arrays.asList(
                "§7Spieler werden gleichmäßig",
                "§7auf Teams verteilt",
                "",
                "§aSchnell & einfach"
        ));
        auto.setItemMeta(autoMeta);

        inv.setItem(11, manual);
        inv.setItem(15, auto);

        boss.openInventory(inv);

        SetupContext context = activeSetups.get(boss.getUniqueId());
        if (context == null) {
            context = new SetupContext();
            activeSetups.put(boss.getUniqueId(), context);
        }
        context.stage = SetupStage.TEAM_MODE; // Temp stage
        context.teamAssignmentCallback = callback;
    }

    /**
     * Setzt Setup nach Team-Erstellung fort
     */
    /**
     * Setzt Setup nach Team-Erstellung fort
     */
    private void continueAfterTeamCreation(Player boss) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge.getTeams().isEmpty()) {
            boss.sendMessage("§c§lFEHLER: Keine Teams erstellt!");
            boss.sendMessage("§7Nutze /challenge cancel zum Abbrechen");
            return;
        }

        // Zeige Übersicht (falls nicht schon vom TeamBuilder gemacht)
        // showTeamOverview(boss); // Auskommentiert, da TeamBuilder das schon macht

        // Weiter mit Dimensionen
        boss.sendMessage("§7Nächster Schritt: Dimensionen konfigurieren");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openDimensionSettingsGUI(boss, (netherEnabled, endEnabled) -> {
                challenge.setNetherEnabled(netherEnabled);
                challenge.setEndEnabled(endEnabled);
                boss.sendMessage("§a✓ Dimensionen: Nether=" + netherEnabled + ", End=" + endEnabled);

                // Weiter mit Wave-Setup
                boss.sendMessage("§7Nächster Schritt: Waves definieren");
                startWaveSetup(boss);
            });
        }, 20L); // 1 Sekunde Pause für bessere UX
    }

    /**
     * Setup für nächstes Team (jetzt mit Preset-Option)
     */
    private void setupNextTeam(Player boss) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        SetupContext context = activeSetups.get(boss.getUniqueId());

        if (context.currentTeamIndex >= context.teamsToSetup.size()) {
            finishSetup(boss);
            return;
        }

        UUID teamId = context.teamsToSetup.get(context.currentTeamIndex);
        List<UUID> teamMembers = challenge.getTeamMembers(teamId);

        List<String> memberNames = new ArrayList<>();
        for (UUID playerId : teamMembers) {
            Player p = Bukkit.getPlayer(playerId);
            memberNames.add(p != null ? p.getName() : "???");
        }

        boss.sendMessage("§e§l=== Wave-Setup ===");
        boss.sendMessage("§7Team: §e" + String.join(" & ", memberNames));
        boss.sendMessage("");

        // NEU: Öffne Preset-Auswahl statt direkt Custom-Setup
        openPresetSelectionGUI(boss, teamId, (difficulty) -> {
            if (difficulty == WavePresets.Difficulty.CUSTOM) {
                // Custom: Wie bisher mit Spawn Eggs
                startCustomWaveSetup(boss, teamId, memberNames);
            } else {
                // Preset: Erstelle Waves automatisch
                List<Wave> waves = createWavesFromPreset(difficulty, teamId);
                challenge.getTeamWaves().put(teamId, waves);



                boss.sendMessage("§a✓ Preset gewählt: " + difficulty.getDisplayName());
                boss.sendMessage("§7Waves wurden automatisch erstellt");

                // Nächstes Team
                context.currentTeamIndex++;
                setupNextTeam(boss);
            }
        });
    }

    /**
     * NEU: Startet Custom-Wave-Setup (bisherige Logik)
     */
    private void startCustomWaveSetup(Player boss, UUID teamId, List<String> memberNames) {
        SetupContext context = activeSetups.get(boss.getUniqueId());
        context.currentWave = 0;
        context.currentTeamWaves = new ArrayList<>();

        boss.sendMessage("§7Wave: §e1/3");
        boss.sendMessage("§7Fülle dein Inventar mit §eSpawn Eggs");
        boss.sendMessage("§7Klicke dann auf den §aGrünen Haken§7.");

        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = confirm.getItemMeta();
        meta.setDisplayName("§a§lWave bestätigen");
        confirm.setItemMeta(meta);
        boss.getInventory().setItem(8, confirm);
    }

    /**
     * Bestätigt aktuelle Wave
     */
    public void confirmCurrentWave(Player boss) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        SetupContext context = activeSetups.get(boss.getUniqueId());

        if (context == null || context.stage != SetupStage.WAVE_SETUP) return;

        Wave wave = new Wave(
                context.currentWave + 1,
                context.teamsToSetup.get(context.currentTeamIndex)
        );

        int eggCount = 0;
        for (ItemStack item : boss.getInventory().getContents()) {
            if (item != null && item.getType().toString().endsWith("_SPAWN_EGG")) {
                String typeName = item.getType().toString().replace("_SPAWN_EGG", "");
                try {
                    EntityType entityType = EntityType.valueOf(typeName);

                    for (int i = 0; i < item.getAmount(); i++) {
                        wave.addMob(entityType);
                        eggCount++;
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unbekannter Entity-Type: " + typeName);
                }
            }
        }

        if (eggCount == 0) {
            boss.sendMessage("§cDu musst mindestens ein Spawn Egg platzieren!");
            return;
        }

        context.currentTeamWaves.add(wave);
        boss.getInventory().clear();

        boss.sendMessage("§a§l✓ Wave " + (context.currentWave + 1) + " gespeichert!");
        boss.sendMessage("§7Mobs: §e" + eggCount);

        context.currentWave++;

        if (context.currentWave < 3) {
            UUID teamId = context.teamsToSetup.get(context.currentTeamIndex);
            List<UUID> teamMembers = challenge.getTeamMembers(teamId);

            List<String> memberNames = new ArrayList<>();
            for (UUID playerId : teamMembers) {
                Player p = Bukkit.getPlayer(playerId);
                memberNames.add(p != null ? p.getName() : "???");
            }
            String teamName = String.join(" & ", memberNames);

            boss.sendMessage("");
            boss.sendMessage("§7Team: §e" + teamName);
            boss.sendMessage("§7Wave: §e" + (context.currentWave + 1) + "/3");
            boss.sendMessage("§7Fülle dein Inventar mit Spawn Eggs.");

            ItemStack confirm = new ItemStack(Material.LIME_WOOL);
            ItemMeta meta = confirm.getItemMeta();
            meta.setDisplayName("§a§lWave bestätigen");
            confirm.setItemMeta(meta);
            boss.getInventory().setItem(8, confirm);
        } else {
            challenge.getTeamWaves().put(
                    context.teamsToSetup.get(context.currentTeamIndex),
                    context.currentTeamWaves
            );

            context.currentTeamIndex++;
            setupNextTeam(boss);
        }
    }

    /**
     * Beendet Setup
     */
    private void finishSetup(Player boss) {
        SetupContext context = activeSetups.remove(boss.getUniqueId());

        boss.setGameMode(GameMode.SURVIVAL);
        boss.getInventory().clear();

        boss.sendMessage("§a§l=== Setup abgeschlossen! ===");
        boss.sendMessage("§7Die Challenge startet jetzt...");

        if (context.setupCompleteCallback != null) {
            context.setupCompleteCallback.run();
        }
    }

    public SetupContext getSetupContext(UUID bossId) {
        return activeSetups.get(bossId);
    }

    // === INNER CLASSES ===

    public static class SetupContext {
        public SetupStage stage;
        public BiConsumer<Boolean, Boolean> dimensionCallback;
        public Consumer<Boolean> participationCallback;
        public Consumer<Challenge.TeamMode> teamModeCallback;
        public Consumer<WavePresets.Difficulty> presetCallback; // NEU!
        public Runnable setupCompleteCallback;
        public Consumer<Boolean> teamAssignmentCallback; // NEU: Manuell vs Auto

        public boolean netherEnabled = false;
        public boolean endEnabled = false;

        public List<UUID> teamsToSetup;
        public int currentTeamIndex;
        public int currentWave;
        public List<Wave> currentTeamWaves;

        // NEU: Für Preset-System
        public UUID currentPresetTeamId;

        // NEU: Für manuelles Team-Building
        public Map<Integer, List<UUID>> manualTeams;
        public List<UUID> unassignedPlayers;
    }

    public enum SetupStage {
        TEAM_MODE,
        MANUAL_TEAM_BUILDING,  // NEU!
        DIMENSIONS,
        PARTICIPATION,
        PRESET_SELECTION,      // NEU!
        WAVE_SETUP
    }

}