package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

/**
 * NEU: Verwaltet Team-Backpacks
 * Jedes Team hat einen gemeinsamen Backpack (54 Slots) für Items
 */
public class TeamBackpackManager {

    private final ChallengePlugin plugin;

    // Backpack-Inventare pro Team
    private final Map<UUID, Inventory> teamBackpacks = new HashMap<>();

    // Tracking welcher Spieler gerade welchen Backpack offen hat
    private final Map<UUID, UUID> openBackpacks = new HashMap<>();

    public TeamBackpackManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Erstellt Backpacks für alle Teams
     */
    public void createBackpacks(Challenge challenge) {
        int teamNumber = 1;
        for (UUID teamId : challenge.getTeams().keySet()) {
            // Erstelle Inventory (Double Chest = 54 Slots)
            Inventory backpack = Bukkit.createInventory(
                    null,
                    54,
                    "§6§lTeam " + teamNumber + " Backpack"
            );

            teamBackpacks.put(teamId, backpack);
            plugin.getLogger().info("[TeamBackpackManager] Backpack für Team " + teamNumber + " erstellt");
            teamNumber++;
        }
    }

    /**
     * Öffnet Backpack für Spieler
     */
    public void openBackpack(Player player) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) {
            player.sendMessage("§cKeine aktive Challenge!");
            return;
        }

        // Finde Team des Spielers
        UUID teamId = challenge.getTeamOfPlayer(player.getUniqueId());
        if (teamId == null) {
            player.sendMessage("§cDu bist in keinem Team!");
            return;
        }

        // Hole Backpack
        Inventory backpack = teamBackpacks.get(teamId);
        if (backpack == null) {
            player.sendMessage("§cKein Backpack für dein Team gefunden!");
            return;
        }

        // Öffne Backpack
        player.openInventory(backpack);
        openBackpacks.put(player.getUniqueId(), teamId);

        player.sendMessage("§aTeam-Backpack geöffnet!");
    }

    /**
     * Prüft ob Spieler einen Backpack offen hat
     */
    public boolean hasBackpackOpen(Player player) {
        return openBackpacks.containsKey(player.getUniqueId());
    }

    /**
     * Schließt Backpack für Spieler
     */
    public void closeBackpack(Player player) {
        openBackpacks.remove(player.getUniqueId());
    }

    /**
     * Gibt Backpack-Öffner-Item
     */
    public ItemStack getBackpackItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lTeam-Backpack");
        meta.setLore(Arrays.asList(
                "§7Rechtsklick zum Öffnen",
                "",
                "§7Geteiltes Inventar für",
                "§7alle Team-Mitglieder"
        ));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Gibt Backpack-Item allen Team-Mitgliedern
     */
    public void giveBackpackItemsToTeam(Challenge challenge) {
        ItemStack backpackItem = getBackpackItem();

        for (UUID playerId : challenge.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Gib Item in Slot 8 (letzter Hotbar-Slot)
                player.getInventory().setItem(8, backpackItem);
            }
        }

        plugin.getLogger().info("[TeamBackpackManager] Backpack-Items an alle Spieler gegeben");
    }

    /**
     * Entfernt alle Backpacks
     */
    public void cleanup() {
        // Schließe alle offenen Backpacks
        for (UUID playerId : new HashSet<>(openBackpacks.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.closeInventory();
            }
        }
        openBackpacks.clear();

        // Leere Backpacks
        for (Inventory backpack : teamBackpacks.values()) {
            backpack.clear();
        }
        teamBackpacks.clear();

        plugin.getLogger().info("[TeamBackpackManager] Backpacks aufgeräumt");
    }

    /**
     * Gibt Backpack für Team zurück (für Tests/Admin)
     */
    public Inventory getBackpackForTeam(UUID teamId) {
        return teamBackpacks.get(teamId);
    }

    /**
     * Prüft ob Inventory ein Backpack ist
     */
    public boolean isBackpack(Inventory inventory) {
        if (inventory == null) return false;
        String title = inventory.getType() == org.bukkit.event.inventory.InventoryType.CHEST
                ? "UNKNOWN" // Fallback, da getTitle() deprecated ist
                : null;

        // Prüfe ob es einer unserer Backpacks ist
        return teamBackpacks.containsValue(inventory);
    }

    /**
     * Gibt Team-ID für offenen Backpack zurück
     */
    public UUID getOpenBackpackTeam(Player player) {
        return openBackpacks.get(player.getUniqueId());
    }
}