package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.commands.RainbowCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Verhindert dass Rainbow-Items gedroppt oder verschoben werden
 */
public class RainbowListener implements Listener {

    private final ChallengePlugin plugin;
    private final RainbowCommand rainbowCommand;

    public RainbowListener(ChallengePlugin plugin, RainbowCommand rainbowCommand) {
        this.plugin = plugin;
        this.rainbowCommand = rainbowCommand;
    }

    /**
     * Verhindert Item-Drop von Rainbow-Items
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRainbowDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (rainbowCommand.isRainbowItem(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDu kannst Rainbow-Items nicht droppen!");
        }
    }

    /**
     * Verhindert Verschieben von Rainbow-Items im Inventar
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRainbowMove(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Prüfe beide Items
        if ((current != null && rainbowCommand.isRainbowItem(current)) ||
                (cursor != null && rainbowCommand.isRainbowItem(cursor))) {

            // Erlaube nur wenn Spieler Rainbow aktiv hat (für Updates)
            Player player = (Player) event.getWhoClicked();
            if (!rainbowCommand.hasRainbow(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage("§cDu kannst Rainbow-Items nicht verschieben!");
            }
        }
    }
}