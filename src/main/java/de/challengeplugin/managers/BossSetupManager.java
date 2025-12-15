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
 * NEU: Mit Wave-Count-Auswahl (1, 3, 5, 10, 15 Waves)
 */
public class BossSetupManager {

    private final ChallengePlugin plugin;
    private final Map<UUID, SetupContext> activeSetups = new HashMap<>();
    private final Map<UUID, TeamBuilderGUI> activeTeamBuilders = new HashMap<>();

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

        openTeamModeGUI(boss, (teamMode) -> {
            challenge.setTeamMode(teamMode);
            boss.sendMessage("§a✓ Team-Modus: §e" + teamMode.name());

            boss.sendMessage("§7Schritt 2: Deine Teilnahme");
            openParticipationGUI(boss, (participates) -> {
                challenge.setBossParticipates(participates);
                boss.sendMessage("§a✓ Du spielst " + (participates ? "MIT" : "NICHT mit"));

                boss.sendMessage("§7Schritt 3: Team-Erstellung");
                openTeamAssignmentModeGUI(boss, (isManual) -> {

                    if (isManual) {
                        boss.sendMessage("§eManuelles Team-Building wird geöffnet...");
                        openTeamBuilder(boss, (v) -> {
                            boss.sendMessage("§a✓ Teams manuell erstellt");
                            continueAfterTeamCreation(boss);
                        });
                    } else {
                        boss.sendMessage("§eErstelle Teams automatisch...");
                        challenge.createTeams();
                        boss.sendMessage("§a✓ Teams automatisch erstellt");
                        continueAfterTeamCreation(boss);
                    }
                });
            });
        });
    }

    /**
     * NEU: Öffnet Wave-Count-Auswahl GUI
     */
    public void openWaveCountGUI(Player boss, UUID teamId, Consumer<Integer> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Wie viele Waves?");

        // 1 Wave
        ItemStack one = createWaveCountItem(Material.IRON_BLOCK, "§e1 Wave", 1);
        // 3 Waves
        ItemStack three = createWaveCountItem(Material.GOLD_BLOCK, "§e3 Waves", 3);
        // 5 Waves
        ItemStack five = createWaveCountItem(Material.DIAMOND_BLOCK, "§e5 Waves", 5);
        // 10 Waves
        ItemStack ten = createWaveCountItem(Material.EMERALD_BLOCK, "§e10 Waves", 10);
        // 15 Waves
        ItemStack fifteen = createWaveCountItem(Material.NETHERITE_BLOCK, "§e15 Waves", 15);

        inv.setItem(10, one);
        inv.setItem(12, three);
        inv.setItem(14, five);
        inv.setItem(16, ten);
        inv.setItem(22, fifteen);

        boss.openInventory(inv);

        SetupContext context = activeSetups.get(boss.getUniqueId());
        if (context != null) {
            context.waveCountCallback = callback;
            context.stage = SetupStage.WAVE_COUNT_SELECTION;
        }
    }

    private ItemStack createWaveCountItem(Material material, String name, int count) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(
                "",
                WavePresets.getWaveCountDescription(count),
                "",
                "§7Klicke um auszuwählen"
        ));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Öffnet Preset-Auswahl GUI (nach Wave-Count-Auswahl)
     */
    public void openPresetSelectionGUI(Player boss, UUID teamId, int waveCount, Consumer<WavePresets.Difficulty> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Wave-Schwierigkeit");

        ItemStack easy = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta easyMeta = easy.getItemMeta();
        easyMeta.setDisplayName(WavePresets.Difficulty.EASY.getDisplayName());
        easyMeta.setLore(Arrays.asList(
                "§7" + waveCount + " Waves - Einfach",
                "§7Wenige Mobs, einfache Gegner"
        ));
        easy.setItemMeta(easyMeta);

        ItemStack medium = new ItemStack(Material.IRON_SWORD);
        ItemMeta mediumMeta = medium.getItemMeta();
        mediumMeta.setDisplayName(WavePresets.Difficulty.MEDIUM.getDisplayName());
        mediumMeta.setLore(Arrays.asList(
                "§7" + waveCount + " Waves - Mittel",
                "§7Ausgewogene Herausforderung"
        ));
        medium.setItemMeta(mediumMeta);

        ItemStack hard = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta hardMeta = hard.getItemMeta();
        hardMeta.setDisplayName(WavePresets.Difficulty.HARD.getDisplayName());
        hardMeta.setLore(Arrays.asList(
                "§7" + waveCount + " Waves - Schwer",
                "§7Viele Mobs, schwierige Gegner"
        ));
        hard.setItemMeta(hardMeta);

        ItemStack extreme = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta extremeMeta = extreme.getItemMeta();
        extremeMeta.setDisplayName(WavePresets.Difficulty.EXTREME.getDisplayName());
        extremeMeta.setLore(Arrays.asList(
                "§7" + waveCount + " Waves - Extrem",
                "§7Hardcore-Modus mit Boss-Waves"
        ));
        extreme.setItemMeta(extremeMeta);

        ItemStack custom = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta customMeta = custom.getItemMeta();
        customMeta.setDisplayName(WavePresets.Difficulty.CUSTOM.getDisplayName());
        customMeta.setLore(Arrays.asList(
                "§7" + waveCount + " Waves - Custom",
                "§7Definiere Waves selbst mit Spawn Eggs"
        ));
        custom.setItemMeta(customMeta);

        inv.setItem(10, easy);
        inv.setItem(12, medium);
        inv.setItem(14, hard);
        inv.setItem(16, extreme);
        inv.setItem(22, custom);

        boss.openInventory(inv);

        SetupContext context = activeSetups.get(boss.getUniqueId());
        if (context != null) {
            context.presetCallback = callback;
            context.selectedWaveCount = waveCount;
            context.stage = SetupStage.PRESET_SELECTION;
        }
    }

    /**
     * Erstellt Waves aus Preset mit gewählter Anzahl
     */
    private List<Wave> createWavesFromPreset(WavePresets.Difficulty difficulty, UUID teamId, int waveCount) {
        List<List<EntityType>> presetWaves = WavePresets.getPreset(difficulty, waveCount);
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

    public void openTeamModeGUI(Player boss, Consumer<Challenge.TeamMode> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Team-Modus wählen");

        ItemStack solo = new ItemStack(Material.IRON_SWORD);
        ItemMeta soloMeta = solo.getItemMeta();
        soloMeta.setDisplayName("§e§lSolo");
        soloMeta.setLore(Arrays.asList("§7Jeder Spieler kämpft alleine", "", "§7Team-Größe: §e1 Spieler"));
        solo.setItemMeta(soloMeta);

        ItemStack duo = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta duoMeta = duo.getItemMeta();
        duoMeta.setDisplayName("§b§lDuo");
        duoMeta.setLore(Arrays.asList("§7Spieler kämpfen zu zweit", "", "§7Team-Größe: §e2 Spieler"));
        duo.setItemMeta(duoMeta);

        ItemStack trio = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta trioMeta = trio.getItemMeta();
        trioMeta.setDisplayName("§d§lTrio");
        trioMeta.setLore(Arrays.asList("§7Spieler kämpfen zu dritt", "", "§7Team-Größe: §e3 Spieler"));
        trio.setItemMeta(trioMeta);

        inv.setItem(11, solo);
        inv.setItem(13, duo);
        inv.setItem(15, trio);

        boss.openInventory(inv);

        SetupContext context = new SetupContext();
        context.stage = SetupStage.TEAM_MODE;
        context.teamModeCallback = callback;
        activeSetups.put(boss.getUniqueId(), context);
    }

    public void openDimensionSettingsGUI(Player boss, BiConsumer<Boolean, Boolean> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Welten-Zugang");

        ItemStack netherItem = new ItemStack(Material.NETHERRACK);
        ItemMeta netherMeta = netherItem.getItemMeta();
        netherMeta.setDisplayName("§cNether");
        netherMeta.setLore(Arrays.asList("§7Klicke um Nether zu aktivieren", "", "§7Status: §cDeaktiviert"));
        netherItem.setItemMeta(netherMeta);

        ItemStack endItem = new ItemStack(Material.END_STONE);
        ItemMeta endMeta = endItem.getItemMeta();
        endMeta.setDisplayName("§dEnd");
        endMeta.setLore(Arrays.asList("§7Klicke um End zu aktivieren", "", "§7Status: §cDeaktiviert"));
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

    public void openParticipationGUI(Player boss, Consumer<Boolean> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Boss-Teilnahme");

        ItemStack yes = new ItemStack(Material.LIME_WOOL);
        ItemMeta yesMeta = yes.getItemMeta();
        yesMeta.setDisplayName("§a§lJa, ich spiele mit");
        yesMeta.setLore(Arrays.asList("§7Du kämpfst auch gegen die Mobs", "§7die du für dich definiert hast"));
        yes.setItemMeta(yesMeta);

        ItemStack no = new ItemStack(Material.RED_WOOL);
        ItemMeta noMeta = no.getItemMeta();
        noMeta.setDisplayName("§c§lNein, nur organisieren");
        noMeta.setLore(Arrays.asList("§7Du definierst nur die Waves", "§7und kämpfst nicht selbst"));
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

    public void openTeamBuilder(Player boss, Consumer<Void> onComplete) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        boss.sendMessage("§6§l=== Team-Builder ===");
        boss.sendMessage("§7Weise Spieler manuell zu Teams zu");

        TeamBuilderGUI teamBuilder = new TeamBuilderGUI(plugin, boss, challenge);
        activeTeamBuilders.put(boss.getUniqueId(), teamBuilder);

        teamBuilder.setOnCompleteCallback(() -> {
            activeTeamBuilders.remove(boss.getUniqueId());
            showTeamOverview(boss);
            onComplete.accept(null);
        });

        teamBuilder.open();
    }

    public TeamBuilderGUI getTeamBuilder(UUID bossId) {
        return activeTeamBuilders.get(bossId);
    }

    public void removeTeamBuilder(UUID bossId) {
        activeTeamBuilders.remove(bossId);
    }

    private void showTeamOverview(Player boss) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        boss.sendMessage("§6§l=== Team-Übersicht ===");
        boss.sendMessage("§7Modus: §e" + challenge.getTeamMode().name());
        boss.sendMessage("§7Anzahl Teams: §e" + challenge.getTeams().size());

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
    }

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

    private void openTeamAssignmentModeGUI(Player boss, Consumer<Boolean> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Team-Erstellung");

        ItemStack manual = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta manualMeta = manual.getItemMeta();
        manualMeta.setDisplayName("§e§lManuell");
        manualMeta.setLore(Arrays.asList("§7Du weist Spieler selbst", "§7zu Teams zu", "", "§aVolle Kontrolle"));
        manual.setItemMeta(manualMeta);

        ItemStack auto = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta autoMeta = auto.getItemMeta();
        autoMeta.setDisplayName("§a§lAutomatisch");
        autoMeta.setLore(Arrays.asList("§7Spieler werden gleichmäßig", "§7auf Teams verteilt", "", "§aSchnell & einfach"));
        auto.setItemMeta(autoMeta);

        inv.setItem(11, manual);
        inv.setItem(15, auto);

        boss.openInventory(inv);

        SetupContext context = activeSetups.get(boss.getUniqueId());
        if (context == null) {
            context = new SetupContext();
            activeSetups.put(boss.getUniqueId(), context);
        }
        context.teamAssignmentCallback = callback;
    }

    private void continueAfterTeamCreation(Player boss) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge.getTeams().isEmpty()) {
            boss.sendMessage("§c§lFEHLER: Keine Teams erstellt!");
            return;
        }

        boss.sendMessage("§7Nächster Schritt: Dimensionen konfigurieren");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openDimensionSettingsGUI(boss, (netherEnabled, endEnabled) -> {
                challenge.setNetherEnabled(netherEnabled);
                challenge.setEndEnabled(endEnabled);
                boss.sendMessage("§a✓ Dimensionen: Nether=" + netherEnabled + ", End=" + endEnabled);

                boss.sendMessage("§7Nächster Schritt: Waves definieren");
                startWaveSetup(boss);
            });
        }, 20L);
    }

    /**
     * NEU: Setup für nächstes Team mit Wave-Count-Auswahl
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

        // NEU: Erst Wave-Count auswählen
        openWaveCountGUI(boss, teamId, (waveCount) -> {
            boss.sendMessage("§a✓ Wave-Anzahl: §e" + waveCount);

            // Dann Preset/Custom auswählen
            openPresetSelectionGUI(boss, teamId, waveCount, (difficulty) -> {
                if (difficulty == WavePresets.Difficulty.CUSTOM) {
                    startCustomWaveSetup(boss, teamId, memberNames, waveCount);
                } else {
                    List<Wave> waves = createWavesFromPreset(difficulty, teamId, waveCount);
                    challenge.getTeamWaves().put(teamId, waves);

                    boss.sendMessage("§a✓ Preset: " + difficulty.getDisplayName());
                    boss.sendMessage("§7Waves wurden automatisch erstellt");

                    context.currentTeamIndex++;
                    setupNextTeam(boss);
                }
            });
        });
    }

    /**
     * Startet Custom-Wave-Setup mit gewählter Wave-Anzahl
     */
    private void startCustomWaveSetup(Player boss, UUID teamId, List<String> memberNames, int waveCount) {
        SetupContext context = activeSetups.get(boss.getUniqueId());
        context.currentWave = 0;
        context.currentTeamWaves = new ArrayList<>();
        context.selectedWaveCount = waveCount;

        boss.sendMessage("§7Wave: §e1/" + waveCount);
        boss.sendMessage("§7Fülle dein Inventar mit §eSpawn Eggs");
        boss.sendMessage("§7Klicke dann auf den §aGrünen Haken§7.");

        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = confirm.getItemMeta();
        meta.setDisplayName("§a§lWave bestätigen");
        meta.setLore(Arrays.asList("§7Rechtsklick oder Linksklick", "§7um Wave zu bestätigen"));
        confirm.setItemMeta(meta);

        boss.getInventory().setItem(8, confirm);
    }

    /**
     * Bestätigt aktuelle Wave
     */
    public void confirmCurrentWave(Player boss) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        SetupContext context = activeSetups.get(boss.getUniqueId());

        if (context == null || context.stage != SetupStage.WAVE_SETUP) {
            boss.sendMessage("§cSetup-Context-Fehler!");
            return;
        }

        Wave wave = new Wave(context.currentWave + 1, context.teamsToSetup.get(context.currentTeamIndex));

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

        if (context.currentWave < context.selectedWaveCount) {
            boss.sendMessage("§7Wave: §e" + (context.currentWave + 1) + "/" + context.selectedWaveCount);
            boss.sendMessage("§7Fülle dein Inventar mit Spawn Eggs.");

            ItemStack confirm = new ItemStack(Material.LIME_WOOL);
            ItemMeta meta = confirm.getItemMeta();
            meta.setDisplayName("§a§lWave bestätigen");
            meta.setLore(Arrays.asList("§7Rechtsklick oder Linksklick", "§7um Wave zu bestätigen"));
            confirm.setItemMeta(meta);
            boss.getInventory().setItem(8, confirm);
        } else {
            challenge.getTeamWaves().put(context.teamsToSetup.get(context.currentTeamIndex), context.currentTeamWaves);

            context.currentTeamIndex++;
            setupNextTeam(boss);
        }
    }

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

    public static class SetupContext {
        public SetupStage stage;
        public BiConsumer<Boolean, Boolean> dimensionCallback;
        public Consumer<Boolean> participationCallback;
        public Consumer<Challenge.TeamMode> teamModeCallback;
        public Consumer<WavePresets.Difficulty> presetCallback;
        public Consumer<Integer> waveCountCallback; // NEU!
        public Runnable setupCompleteCallback;
        public Consumer<Boolean> teamAssignmentCallback;

        public boolean netherEnabled = false;
        public boolean endEnabled = false;

        public List<UUID> teamsToSetup;
        public int currentTeamIndex;
        public int currentWave;
        public List<Wave> currentTeamWaves;
        public int selectedWaveCount = 3; // NEU!

        public UUID currentPresetTeamId;
        public Map<Integer, List<UUID>> manualTeams;
        public List<UUID> unassignedPlayers;
    }

    public enum SetupStage {
        TEAM_MODE,
        MANUAL_TEAM_BUILDING,
        DIMENSIONS,
        PARTICIPATION,
        WAVE_COUNT_SELECTION,  // NEU!
        PRESET_SELECTION,
        WAVE_SETUP
    }
}