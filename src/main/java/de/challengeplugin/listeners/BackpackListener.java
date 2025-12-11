package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * NEU: Listener für Team-Backpack
 * Handhabt Öffnen/Schließen und Interaktionen
 */
public class BackpackListener implements Listener {

    private final ChallengePlugin plugin;

    public BackpackListener(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spieler klickt mit Backpack-Item
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        if (item.getType() != Material.CHEST) return;
        if (!item.hasItemMeta()) return;

        String displayName = item.getItemMeta().getDisplayName();
        if (!displayName.equals("§6§lTeam-Backpack")) return;

        // Rechtsklick oder Linksklick
        if (event.getAction() == Action.RIGHT_CLICK_AIR ||
                event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            event.setCancelled(true);
            plugin.getChallengeManager().getBackpackManager().openBackpack(player);
        }
    }

    /**
     * Spieler schließt Inventory
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        // Prüfe ob es ein Backpack war
        if (plugin.getChallengeManager().getBackpackManager().isBackpack(event.getInventory())) {
            plugin.getChallengeManager().getBackpackManager().closeBackpack(player);
        }
    }

    /**
     * Verhindere Verschieben des Backpack-Items
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (item.getType() != Material.CHEST) return;
        if (!item.hasItemMeta()) return;

        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.equals("§6§lTeam-Backpack")) {
            // Erlaube nur Rechtsklick zum Öffnen, verhindere Movement
            if (event.getClick().isShiftClick() || event.getClick().isLeftClick()) {
                event.setCancelled(true);
            }
        }
    }
}