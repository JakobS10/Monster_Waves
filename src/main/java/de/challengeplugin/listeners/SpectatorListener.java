package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener für Spectator-System
 * - Compass-Klicks
 * - Spectator-GUI-Klicks
 * - Verhindert Schaden an Spectators
 * - Verhindert Item-Drops von Spectators (NEU!)
 */
public class SpectatorListener implements Listener {

    private final ChallengePlugin plugin;

    public SpectatorListener(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spieler klickt mit Spectator-Compass
     */
    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        if (item.getType() != Material.COMPASS) return;
        if (!item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().equals("§b§lSpectator-Navigator")) return;

        // Nur Rechtsklick
        if (event.getAction() == Action.RIGHT_CLICK_AIR ||
                event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            event.setCancelled(true);
            plugin.getChallengeManager().getSpectatorManager().openSpectatorGUI(player);
        }
    }

    /**
     * Klick in Spectator-GUI
     */
    @EventHandler
    public void onSpectatorGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.equals("§b§lSpectator-Modus")) return;

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        // Extrahiere Spielernamen aus Display-Name
        String displayName = item.getItemMeta().getDisplayName();
        String targetName = org.bukkit.ChatColor.stripColor(displayName);

        Player target = org.bukkit.Bukkit.getPlayer(targetName);
        if (target != null) {
            player.closeInventory();
            plugin.getChallengeManager().getSpectatorManager()
                    .teleportToPlayer(player, target.getUniqueId());
        } else {
            player.sendMessage("§cSpieler nicht mehr online!");
        }
    }

    /**
     * Backup: Verhindert Schaden an Spectators
     * (sollte durch Invincible bereits abgedeckt sein)
     */
    @EventHandler
    public void onSpectatorDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (plugin.getChallengeManager().getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * NEU: Verhindert Item-Drops von Spectators
     */
    @EventHandler
    public void onSpectatorDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // Prüfe ob Spieler im Spectator-Modus ist
        if (plugin.getChallengeManager().getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cDu kannst als Spectator keine Items droppen!");
        }
    }
}