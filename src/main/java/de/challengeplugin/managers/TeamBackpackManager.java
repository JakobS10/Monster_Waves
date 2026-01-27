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
     * WICHTIG: Jedes Team bekommt seinen eigenen, individuellen Backpack!
     */
    public void createBackpacks(Challenge challenge) {
        int teamNumber = 1;
        for (UUID teamId : challenge.getTeams().keySet()) {
            createBackpackForTeam(teamId, teamNumber);
            teamNumber++;
        }
    }

    /**
     * NEU: Erstellt Backpack für ein einzelnes Team (für Late-Joining)
     */
    public void createBackpackForTeam(UUID teamId, int teamNumber) {
        // Erstelle Inventory (Double Chest = 54 Slots)
        Inventory backpack = Bukkit.createInventory(
                null,
                54,
                "§6§lTeam " + teamNumber + " Backpack"
        );

        teamBackpacks.put(teamId, backpack);
        plugin.getLogger().info("[TeamBackpackManager] Backpack für Team " + teamNumber + " erstellt (INDIVIDUELL)");
    }

    /**
     * Öffnet Backpack für Spieler
     * Zeigt IMMER den Backpack des eigenen Teams an!
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

        // Hole Backpack (TEAM-SPEZIFISCH!)
        Inventory backpack = teamBackpacks.get(teamId);
        if (backpack == null) {
            player.sendMessage("§cKein Backpack für dein Team gefunden!");
            return;
        }

        // Öffne Backpack
        player.openInventory(backpack);
        openBackpacks.put(player.getUniqueId(), teamId);

        player.sendMessage("§a§lTeam-Backpack geöffnet!");
        player.sendMessage("§7Alle Team-Mitglieder können auf diesen Backpack zugreifen");
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

    /**
     * NEU: Gibt Map aller Backpacks zurück (für Speicherung)
     */
    public Map<UUID, Inventory> getTeamBackpacks() {
        return teamBackpacks;
    }

    /**
     * NEU: Stellt Backpack-Items nach Server-Neustart wieder her
     */
    public void restoreBackpackItems(Map<UUID, List<ItemStack>> backpackItems) {
        for (Map.Entry<UUID, List<ItemStack>> entry : backpackItems.entrySet()) {
            UUID teamId = entry.getKey();
            List<ItemStack> items = entry.getValue();

            // Hole Backpack für Team
            Inventory backpack = teamBackpacks.get(teamId);
            if (backpack == null) {
                plugin.getLogger().warning("[TeamBackpackManager] Kein Backpack für Team gefunden: " + teamId);
                continue;
            }

            // Fülle Items ein
            int slot = 0;
            for (ItemStack item : items) {
                if (item != null && slot < backpack.getSize()) {
                    backpack.setItem(slot, item);
                    slot++;
                }
            }

            plugin.getLogger().info("[TeamBackpackManager] " + items.size() + " Items wiederhergestellt für Team");
        }
    }
}