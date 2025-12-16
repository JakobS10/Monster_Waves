package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.managers.BossSetupManager;
import de.challengeplugin.managers.StatisticsManager;
import de.challengeplugin.managers.TeamBuilderGUI;
import de.challengeplugin.models.Challenge;
import de.challengeplugin.models.WavePresets;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener für Boss-Setup-GUIs
 * VERVOLLSTÄNDIGT: Filter-Buttons in Auswertungs-GUI funktionieren jetzt
 */
public class SetupGUIListener implements Listener {

    private final ChallengePlugin plugin;

    public SetupGUIListener(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Verhindert Drag&Drop in Setup-GUIs
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();

        if (title.equals("§6Team-Modus wählen") ||
                title.equals("§6Team-Erstellung") ||
                title.startsWith("§6Wave-Schwierigkeit") ||
                title.equals("§6Welten-Zugang") ||
                title.equals("§6Boss-Teilnahme") ||
                title.equals("§6§lTeam-Builder") ||
                title.equals("§b§lSpectator-Modus") ||
                title.equals("§6§lChallenge-Auswertung")) {

            event.setCancelled(true);
        }
    }

    /**
     * Inventory-Click während Setup
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();

        // Shift-Click aus Player-Inventar blockieren
        if (event.getClickedInventory() != null &&
                event.getClickedInventory().getType() == InventoryType.PLAYER) {

            if (title.startsWith("§6Wave-Schwierigkeit") ||
                    title.equals("§6Team-Modus wählen") ||
                    title.equals("§6Team-Erstellung") ||
                    title.equals("§6Welten-Zugang") ||
                    title.equals("§6Boss-Teilnahme")) {

                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
            }
        }

        // Team-Mode-GUI
        if (title.equals("§6Team-Modus wählen")) {
            event.setCancelled(true);
            handleTeamModeGUI(player, event.getSlot());
        }
        // Team-Erstellung-Modus GUI
        else if (title.equals("§6Team-Erstellung")) {
            event.setCancelled(true);
            handleTeamAssignmentModeGUI(player, event.getSlot());
        }
        // Team-Builder GUI
        else if (title.equals("§6§lTeam-Builder")) {
            event.setCancelled(true);
            TeamBuilderGUI teamBuilder = plugin.getChallengeManager()
                    .getBossSetupManager().getTeamBuilder(player.getUniqueId());

            if (teamBuilder != null) {
                teamBuilder.handleClick(
                        event.getSlot(),
                        event.getClick().isRightClick(),
                        event.getClick().isShiftClick()
                );
            }
        }
        // Preset-Auswahl GUI
        else if (title.startsWith("§6Wave-Schwierigkeit")) {
            event.setCancelled(true);
            handlePresetSelectionGUI(player, event.getSlot());
        }
        // Dimension-GUI
        else if (title.equals("§6Welten-Zugang")) {
            event.setCancelled(true);
            handleDimensionGUI(player, event.getCurrentItem(), event.getSlot());
        }

        // Wave-Count-GUI
        else if (title.equals("§6Wie viele Waves?")) {
            event.setCancelled(true);
            handleWaveCountGUI(player, event.getSlot());
        }


        // Participation-GUI
        else if (title.equals("§6Boss-Teilnahme")) {
            event.setCancelled(true);
            handleParticipationGUI(player, event.getSlot());
        }
        // Wave-Setup: Nur Bestätigen-Button blockieren
        else if (title.equals("§e§lWave-Schwierigkeit")) {
            BossSetupManager.SetupContext context = plugin.getChallengeManager()
                    .getBossSetupManager().getSetupContext(player.getUniqueId());

            if (context != null && context.stage == BossSetupManager.SetupStage.WAVE_SETUP) {
                if (event.getSlot() == 8) {
                    event.setCancelled(true);
                }
            }
        }
        // Spectator-GUI
        else if (title.equals("§b§lSpectator-Modus")) {
            event.setCancelled(true);
            handleSpectatorGUI(player, event.getCurrentItem());
        }
        // VERVOLLSTÄNDIGT: Auswertungs-GUI
        else if (title.equals("§6§lChallenge-Auswertung")) {
            event.setCancelled(true);
            handleEvaluationGUI(player, event.getSlot());
        }
    }

    /**
     * Handhabt Dimension-GUI-Klicks
     */
    private void handleDimensionGUI(Player player, ItemStack item, int slot) {
        if (item == null) return;

        BossSetupManager.SetupContext context = plugin.getChallengeManager()
                .getBossSetupManager().getSetupContext(player.getUniqueId());

        if (context == null) return;

        // Nether toggle (Slot 11)
        if (slot == 11) {
            context.netherEnabled = !context.netherEnabled;
            updateDimensionItem(player.getOpenInventory().getTopInventory(), 11,
                    Material.NETHERRACK, "§cNether", context.netherEnabled);
        }
        // End toggle (Slot 13)
        else if (slot == 13) {
            context.endEnabled = !context.endEnabled;
            updateDimensionItem(player.getOpenInventory().getTopInventory(), 13,
                    Material.END_STONE, "§dEnd", context.endEnabled);
        }
        // Bestätigen (Slot 22)
        else if (slot == 22) {
            player.closeInventory();
            context.dimensionCallback.accept(context.netherEnabled, context.endEnabled);
        }
    }

    /**
     * Aktualisiert Dimension-Item-Status
     */
    private void updateDimensionItem(org.bukkit.inventory.Inventory inv, int slot,
                                     Material material, String name, boolean enabled) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(java.util.Arrays.asList(
                "§7Klicke um zu " + (enabled ? "deaktivieren" : "aktivieren"),
                "",
                "§7Status: " + (enabled ? "§aAktiviert" : "§cDeaktiviert")
        ));
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    /**
     * Handhabt Participation-GUI-Klicks
     */
    private void handleParticipationGUI(Player player, int slot) {
        BossSetupManager.SetupContext context = plugin.getChallengeManager()
                .getBossSetupManager().getSetupContext(player.getUniqueId());

        if (context == null) return;

        boolean participates = false;

        if (slot == 11) { // Ja
            participates = true;
        } else if (slot == 15) { // Nein
            participates = false;
        } else {
            return;
        }

        player.closeInventory();
        context.participationCallback.accept(participates);
    }

    /**
     * Handhabt Spectator-GUI-Klicks
     */
    private void handleSpectatorGUI(Player player, ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        String targetName = org.bukkit.ChatColor.stripColor(
                item.getItemMeta().getDisplayName()
        );

        Player target = org.bukkit.Bukkit.getPlayer(targetName);
        if (target != null) {
            player.closeInventory();
            plugin.getChallengeManager().getSpectatorManager()
                    .teleportToPlayer(player, target.getUniqueId());
        }
    }

    /**
     * VERVOLLSTÄNDIGT: Handhabt Auswertungs-GUI-Klicks
     * Filter-Buttons funktionieren jetzt!
     */
    private void handleEvaluationGUI(Player player, int slot) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;

        StatisticsManager.SortCriteria criteria = null;

        // Filter-Buttons
        if (slot == 10) { // Schnellster
            criteria = StatisticsManager.SortCriteria.FASTEST;
        } else if (slot == 12) { // Wenigste Tode
            criteria = StatisticsManager.SortCriteria.LEAST_DEATHS;
        } else if (slot == 14) { // Wenigster Schaden
            criteria = StatisticsManager.SortCriteria.LEAST_DAMAGE;
        }

        if (criteria != null) {
            // Schließe alte GUI und öffne neue mit gewähltem Filter
            player.closeInventory();
            plugin.getChallengeManager().getStatisticsManager()
                    .openEvaluationGUI(player, challenge, criteria);

            player.sendMessage("§7Filter gewählt: " + criteria.getDisplayName());
        }
    }

    /**
     * Spieler klickt Bestätigen-Button im Inventar (Wave-Setup)
     * FIX: Alle Interact-Typen abfangen!
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // Wave-Bestätigen (Grüne Wolle)
        if (item.getType() == Material.LIME_WOOL && item.hasItemMeta()) {
            String displayName = item.getItemMeta().getDisplayName();
            if (displayName != null && displayName.equals("§a§lWave bestätigen")) {
                event.setCancelled(true);
                plugin.getChallengeManager().getBossSetupManager().confirmCurrentWave(player);
                return;
            }
        }

        // Spectator-Compass
        if (item.getType() == Material.COMPASS && item.hasItemMeta()) {
            String displayName = item.getItemMeta().getDisplayName();
            if (displayName != null && displayName.equals("§b§lSpectator-Navigator")) {
                event.setCancelled(true);
                plugin.getChallengeManager().getSpectatorManager().openSpectatorGUI(player);
                return;
            }
        }
    }

    /**
     * Handhabt Team-Mode-GUI-Klicks
     */
    private void handleTeamModeGUI(Player player, int slot) {
        BossSetupManager.SetupContext context = plugin.getChallengeManager()
                .getBossSetupManager().getSetupContext(player.getUniqueId());

        if (context == null) return;

        Challenge.TeamMode selectedMode = null;

        if (slot == 11) {
            selectedMode = Challenge.TeamMode.SOLO;
        } else if (slot == 13) {
            selectedMode = Challenge.TeamMode.DUO;
        } else if (slot == 15) {
            selectedMode = Challenge.TeamMode.TRIO;
        } else {
            return;
        }

        player.closeInventory();
        player.sendMessage("§aTeam-Modus gewählt: §e" + selectedMode.name());
        context.teamModeCallback.accept(selectedMode);
    }

    /**
     * Handhabt Team-Assignment-Mode-GUI
     */
    private void handleTeamAssignmentModeGUI(Player player, int slot) {
        BossSetupManager.SetupContext context = plugin.getChallengeManager()
                .getBossSetupManager().getSetupContext(player.getUniqueId());

        if (context == null || context.teamAssignmentCallback == null) return;

        boolean isManual = false;

        if (slot == 11) {
            isManual = true;
        } else if (slot == 15) {
            isManual = false;
        } else {
            return;
        }

        player.closeInventory();
        context.teamAssignmentCallback.accept(isManual);
    }

    /**
     * Handhabt Preset-Auswahl
     */
    private void handlePresetSelectionGUI(Player player, int slot) {
        BossSetupManager.SetupContext context = plugin.getChallengeManager()
                .getBossSetupManager().getSetupContext(player.getUniqueId());

        if (context == null || context.presetCallback == null) return;

        WavePresets.Difficulty selected = null;

        if (slot == 10) {
            selected = WavePresets.Difficulty.EASY;
        } else if (slot == 12) {
            selected = WavePresets.Difficulty.MEDIUM;
        } else if (slot == 14) {
            selected = WavePresets.Difficulty.HARD;
        } else if (slot == 16) {
            selected = WavePresets.Difficulty.EXTREME;
        } else if (slot == 22) {
            selected = WavePresets.Difficulty.CUSTOM;
        } else {
            return;
        }

        player.closeInventory();
        context.presetCallback.accept(selected);
    }


    /**
     * Handhabt Wave-Anzahl-Auswahl
     */
    private void handleWaveCountGUI(Player player, int slot) {
        BossSetupManager.SetupContext context = plugin.getChallengeManager()
                .getBossSetupManager().getSetupContext(player.getUniqueId());

        if (context == null || context.waveCountCallback == null) return;

        Integer waveCount = null;

        switch (slot) {
            case 10 -> waveCount = 1;
            case 12 -> waveCount = 3;
            case 14 -> waveCount = 5;
            case 16 -> waveCount = 10;
            case 22 -> waveCount = 15;
            default -> {
                return;
            }
        }

        player.closeInventory();
        context.waveCountCallback.accept(waveCount);
    }


}