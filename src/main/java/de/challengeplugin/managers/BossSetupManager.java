package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
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
 * GUI für Dimensionen, Teilnahme und Wave-Definition
 */
public class BossSetupManager {

    private final ChallengePlugin plugin;

    // Tracking für laufende Setup-Prozesse
    private final Map<UUID, SetupContext> activeSetups = new HashMap<>();

    public BossSetupManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Öffnet GUI für Dimension-Einstellungen
     */
    public void openDimensionSettingsGUI(Player boss, BiConsumer<Boolean, Boolean> callback) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Welten-Zugang");

        // Nether-Option
        ItemStack netherItem = new ItemStack(Material.NETHERRACK);
        ItemMeta netherMeta = netherItem.getItemMeta();
        netherMeta.setDisplayName("§cNether");
        netherMeta.setLore(Arrays.asList(
                "§7Klicke um Nether zu aktivieren",
                "",
                "§7Status: §cDeaktiviert"
        ));
        netherItem.setItemMeta(netherMeta);

        // End-Option
        ItemStack endItem = new ItemStack(Material.END_STONE);
        ItemMeta endMeta = endItem.getItemMeta();
        endMeta.setDisplayName("§dEnd");
        endMeta.setLore(Arrays.asList(
                "§7Klicke um End zu aktivieren",
                "",
                "§7Status: §cDeaktiviert"
        ));
        endItem.setItemMeta(endMeta);

        // Bestätigen-Button
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§lBestätigen");
        confirmMeta.setLore(Arrays.asList("§7Klicke um fortzufahren"));
        confirm.setItemMeta(confirmMeta);

        inv.setItem(11, netherItem);
        inv.setItem(13, endItem);
        inv.setItem(22, confirm);

        boss.openInventory(inv);

        // Speichere Kontext
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

        // Ja-Option
        ItemStack yes = new ItemStack(Material.LIME_WOOL);
        ItemMeta yesMeta = yes.getItemMeta();
        yesMeta.setDisplayName("§a§lJa, ich spiele mit");
        yesMeta.setLore(Arrays.asList(
                "§7Du kämpfst auch gegen die Mobs",
                "§7die du für dich definiert hast"
        ));
        yes.setItemMeta(yesMeta);

        // Nein-Option
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

        // Speichere Kontext
        SetupContext context = activeSetups.get(boss.getUniqueId());
        if (context == null) {
            context = new SetupContext();
            activeSetups.put(boss.getUniqueId(), context);
        }
        context.stage = SetupStage.PARTICIPATION;
        context.participationCallback = callback;
    }

    /**
     * Startet Wave-Setup-Prozess für alle Spieler
     */
    public void startWaveSetup(Player boss, List<UUID> players, Runnable onComplete) {
        SetupContext context = activeSetups.get(boss.getUniqueId());
        if (context == null) {
            context = new SetupContext();
            activeSetups.put(boss.getUniqueId(), context);
        }

        context.stage = SetupStage.WAVE_SETUP;
        context.playersToSetup = new ArrayList<>(players);
        context.currentPlayerIndex = 0;
        context.setupCompleteCallback = onComplete;

        // Setze Boss auf Creative
        boss.setGameMode(GameMode.CREATIVE);
        boss.getInventory().clear();

        // Starte Setup für ersten Spieler
        setupNextPlayer(boss);
    }

    /**
     * Setup für nächsten Spieler
     */
    private void setupNextPlayer(Player boss) {
        SetupContext context = activeSetups.get(boss.getUniqueId());

        if (context.currentPlayerIndex >= context.playersToSetup.size()) {
            // Alle Spieler fertig
            finishSetup(boss);
            return;
        }

        UUID playerId = context.playersToSetup.get(context.currentPlayerIndex);
        Player targetPlayer = Bukkit.getPlayer(playerId);
        String playerName = targetPlayer != null ? targetPlayer.getName() : "Unbekannt";

        context.currentWave = 0;
        context.currentPlayerWaves = new ArrayList<>();

        boss.sendMessage("§e§l=== Wave-Setup ===");
        boss.sendMessage("§7Spieler: §e" + playerName);
        boss.sendMessage("§7Wave: §e1/3");
        boss.sendMessage("");
        boss.sendMessage("§7Fülle dein Inventar mit §eSpawn Eggs");
        boss.sendMessage("§7für die erste Wave.");
        boss.sendMessage("§7Klicke dann auf den §aGrünen Haken§7 im Inventar.");

        // Gib Bestätigen-Item
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = confirm.getItemMeta();
        meta.setDisplayName("§a§lWave bestätigen");
        confirm.setItemMeta(meta);
        boss.getInventory().setItem(8, confirm);
    }

    /**
     * Bestätigt aktuelle Wave und geht zur nächsten
     */
    public void confirmCurrentWave(Player boss) {
        SetupContext context = activeSetups.get(boss.getUniqueId());
        if (context == null || context.stage != SetupStage.WAVE_SETUP) return;

        // Sammle Spawn Eggs aus Inventar
        Wave wave = new Wave(
                context.currentWave + 1,
                context.playersToSetup.get(context.currentPlayerIndex)
        );

        int eggCount = 0;
        for (ItemStack item : boss.getInventory().getContents()) {
            if (item != null && item.getType().toString().endsWith("_SPAWN_EGG")) {
                // Hole EntityType aus Spawn Egg
                String typeName = item.getType().toString().replace("_SPAWN_EGG", "");
                try {
                    EntityType entityType = EntityType.valueOf(typeName);

                    // Füge so viele Mobs hinzu wie Items im Stack
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

        context.currentPlayerWaves.add(wave);
        boss.getInventory().clear();

        boss.sendMessage("§a§l✓ Wave " + (context.currentWave + 1) + " gespeichert!");
        boss.sendMessage("§7Mobs: §e" + eggCount);

        context.currentWave++;

        // Nächste Wave oder nächster Spieler?
        if (context.currentWave < 3) {
            // Nächste Wave für gleichen Spieler
            UUID playerId = context.playersToSetup.get(context.currentPlayerIndex);
            Player targetPlayer = Bukkit.getPlayer(playerId);
            String playerName = targetPlayer != null ? targetPlayer.getName() : "Unbekannt";

            boss.sendMessage("");
            boss.sendMessage("§7Spieler: §e" + playerName);
            boss.sendMessage("§7Wave: §e" + (context.currentWave + 1) + "/3");
            boss.sendMessage("§7Fülle dein Inventar mit Spawn Eggs.");

            // Gib Bestätigen-Item zurück
            ItemStack confirm = new ItemStack(Material.LIME_WOOL);
            ItemMeta meta = confirm.getItemMeta();
            meta.setDisplayName("§a§lWave bestätigen");
            confirm.setItemMeta(meta);
            boss.getInventory().setItem(8, confirm);
        } else {
            // 3 Waves fertig, speichere und gehe zum nächsten Spieler
            plugin.getChallengeManager().getActiveChallenge()
                    .getPlayerWaves()
                    .put(context.playersToSetup.get(context.currentPlayerIndex), context.currentPlayerWaves);

            context.currentPlayerIndex++;
            setupNextPlayer(boss);
        }
    }

    /**
     * Beendet Setup-Prozess
     */
    private void finishSetup(Player boss) {
        SetupContext context = activeSetups.remove(boss.getUniqueId());

        // Setze Boss zurück auf Survival
        boss.setGameMode(GameMode.SURVIVAL);
        boss.getInventory().clear();

        boss.sendMessage("§a§l=== Setup abgeschlossen! ===");
        boss.sendMessage("§7Die Challenge startet jetzt...");

        // Callback aufrufen
        if (context.setupCompleteCallback != null) {
            context.setupCompleteCallback.run();
        }
    }

    /**
     * Gibt aktuellen Setup-Kontext zurück
     */
    public SetupContext getSetupContext(UUID bossId) {
        return activeSetups.get(bossId);
    }

    /**
     * Setup-Kontext-Klasse
     */
    public static class SetupContext {
        public SetupStage stage;
        public BiConsumer<Boolean, Boolean> dimensionCallback;
        public Consumer<Boolean> participationCallback;
        public Runnable setupCompleteCallback;

        // Dimension-Einstellungen
        public boolean netherEnabled = false;
        public boolean endEnabled = false;

        // Wave-Setup
        public List<UUID> playersToSetup;
        public int currentPlayerIndex;
        public int currentWave;
        public List<Wave> currentPlayerWaves;
    }

    public enum SetupStage {
        DIMENSIONS,
        PARTICIPATION,
        WAVE_SETUP
    }
}