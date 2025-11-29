package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import de.challengeplugin.models.Wave;
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
    public void startCompleteSetup(Player boss, BiConsumer<Boolean, Boolean> finalCallback) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        // Erst Team-Mode auswählen
        openTeamModeGUI(boss, (teamMode) -> {
            challenge.setTeamMode(teamMode);

            // Dann Dimensionen
            openDimensionSettingsGUI(boss, (netherEnabled, endEnabled) -> {
                challenge.setNetherEnabled(netherEnabled);
                challenge.setEndEnabled(endEnabled);

                // Dann Participation
                openParticipationGUI(boss, (participates) -> {
                    challenge.setBossParticipates(participates);

                    // Erstelle Teams
                    challenge.createTeams();
                    showTeamOverview(boss);

                    // Starte Wave-Setup
                    startWaveSetup(boss);
                });
            });
        });
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
     * Setup für nächstes Team
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
        String teamName = String.join(" & ", memberNames);

        context.currentWave = 0;
        context.currentTeamWaves = new ArrayList<>();

        boss.sendMessage("§e§l=== Wave-Setup ===");
        boss.sendMessage("§7Team: §e" + teamName);
        boss.sendMessage("§7Wave: §e1/3");
        boss.sendMessage("");
        boss.sendMessage("§7Fülle dein Inventar mit §eSpawn Eggs");
        boss.sendMessage("§7für die erste Wave dieses Teams.");
        boss.sendMessage("§7Klicke dann auf den §aGrünen Haken§7 im Inventar.");

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
        public Runnable setupCompleteCallback;

        public boolean netherEnabled = false;
        public boolean endEnabled = false;

        public List<UUID> teamsToSetup;
        public int currentTeamIndex;
        public int currentWave;
        public List<Wave> currentTeamWaves;
    }

    public enum SetupStage {
        TEAM_MODE,
        DIMENSIONS,
        PARTICIPATION,
        WAVE_SETUP
    }
}