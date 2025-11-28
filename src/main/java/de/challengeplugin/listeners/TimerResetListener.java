package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Listener für Timer-Reset-Confirmation-GUI
 * (aus dem originalen Timer-System)
 */
public class TimerResetListener implements Listener {

    private final ChallengePlugin plugin;

    public TimerResetListener(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (!title.equals("§c§lReset Timer?")) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() == null) return;

        // Bestätigen (Grüne Wolle)
        if (event.getSlot() == 11 && event.getCurrentItem().getType() == Material.LIME_WOOL) {
            plugin.getDataManager().resetTimer();
            plugin.getDataManager().saveData();
            player.closeInventory();
            player.sendMessage("§aTimer wurde zurückgesetzt!");
        }
        // Abbrechen (Rote Wolle)
        else if (event.getSlot() == 15 && event.getCurrentItem().getType() == Material.RED_WOOL) {
            player.closeInventory();
            player.sendMessage("§cTimer-Reset abgebrochen!");
        }
    }
}