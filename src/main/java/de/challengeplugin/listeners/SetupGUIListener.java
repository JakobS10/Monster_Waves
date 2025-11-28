package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.managers.BossSetupManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener für Boss-Setup-GUIs
 * Handhabt Klicks in Dimension/Participation/Wave-GUIs
 */
public class SetupGUIListener implements Listener {

    private final ChallengePlugin plugin;

    public SetupGUIListener(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Inventory-Click während Setup
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();

        // Dimension-GUI
        if (title.equals("§6Welten-Zugang")) {
            event.setCancelled(true);
            handleDimensionGUI(player, event.getCurrentItem(), event.getSlot());
        }
        // Participation-GUI
        else if (title.equals("§6Boss-Teilnahme")) {
            event.setCancelled(true);
            handleParticipationGUI(player, event.getSlot());
        }
        // Wave-Setup: Nur Bestätigen-Button blockieren
        else if (title.equals("§e§lWave-Setup")) {
            BossSetupManager.SetupContext context = plugin.getChallengeManager()
                    .getBossSetupManager().getSetupContext(player.getUniqueId());

            if (context != null && context.stage == BossSetupManager.SetupStage.WAVE_SETUP) {
                // Erlaube alles außer Slot 8 (Bestätigen-Button)
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
        // Auswertungs-GUI
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
            return; // Anderer Slot
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
     * Handhabt Auswertungs-GUI-Klicks
     */
    private void handleEvaluationGUI(Player player, int slot) {
        // Filter-Buttons
        if (slot == 10) { // Schnellster
            // TODO: Reload GUI mit FASTEST-Filter
        } else if (slot == 12) { // Wenigste Tode
            // TODO: Reload GUI mit LEAST_DEATHS-Filter
        } else if (slot == 14) { // Wenigster Schaden
            // TODO: Reload GUI mit LEAST_DAMAGE-Filter
        }
    }

    /**
     * Spieler klickt Bestätigen-Button im Inventar (Wave-Setup)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        if (item.getType() != Material.LIME_WOOL) return;
        if (!item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().equals("§a§lWave bestätigen")) return;

        // Bestätige aktuelle Wave
        event.setCancelled(true);
        plugin.getChallengeManager().getBossSetupManager().confirmCurrentWave(player);
    }

    /**
     * Spieler klickt Spectator-Compass
     */
    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        if (item.getType() != Material.COMPASS) return;
        if (!item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().equals("§b§lSpectator-Navigator")) return;

        event.setCancelled(true);
        plugin.getChallengeManager().getSpectatorManager().openSpectatorGUI(player);
    }
}