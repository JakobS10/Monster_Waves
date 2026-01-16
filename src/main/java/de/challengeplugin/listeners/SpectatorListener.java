package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

/**
 * Listener für Spectator-System
 * VEREINFACHT: Kein Compass mehr - nur GUI-Clicks
 */
public class SpectatorListener implements Listener {

    private final ChallengePlugin plugin;

    public SpectatorListener(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Klick in Navigator-GUI
     */
    @EventHandler
    public void onNavigatorGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.equals("§b§lNavigator")) return;

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

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
     * Verhindert Schaden an Spectators
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
     * Verhindert Item-Drops von Spectators
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (plugin.getChallengeManager().getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Verhindert Item-Pickup von Spectators
     */
    @EventHandler
    public void onSpectatorPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (plugin.getChallengeManager().getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Verhindert dass Spectators Entities schlagen
     */
    @EventHandler
    public void onSpectatorDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        if (plugin.getChallengeManager().getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Verhindert Block-Interaktionen
     */
    @EventHandler
    public void onSpectatorInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getChallengeManager().getSpectatorManager().isSpectator(player.getUniqueId())) {
            return;
        }

        // Blockiere alle Interaktionen
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK ||
                event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.PHYSICAL) {

            event.setCancelled(true);
        }
    }

    /**
     * Verhindert dass Spectators mit Entities interagieren
     */
    @EventHandler
    public void onSpectatorInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (plugin.getChallengeManager().getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Verhindert Block-Break
     */
    @EventHandler
    public void onSpectatorBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (plugin.getChallengeManager().getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Verhindert Block-Place
     */
    @EventHandler
    public void onSpectatorBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (plugin.getChallengeManager().getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}