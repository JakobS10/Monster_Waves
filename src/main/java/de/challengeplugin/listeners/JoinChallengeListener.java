package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.commands.JoinChallengeCommand;
import de.challengeplugin.models.Challenge;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Listener für Join-Challenge GUI
 */
public class JoinChallengeListener implements Listener {

    private final ChallengePlugin plugin;
    private final JoinChallengeCommand joinCommand;

    public JoinChallengeListener(ChallengePlugin plugin, JoinChallengeCommand joinCommand) {
        this.plugin = plugin;
        this.joinCommand = joinCommand;
    }

    @EventHandler
    public void onJoinChallengeGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.equals("§6§lChallenge Beitreten")) return;

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) {
            player.sendMessage("§cKeine aktive Challenge!");
            player.closeInventory();
            return;
        }

        // Neues Team erstellen (Emerald)
        if (item.getType() == Material.EMERALD) {
            player.closeInventory();
            joinCommand.createNewTeam(player);
            return;
        }

        // Bestehendem Team beitreten (Player Head)
        if (item.getType() == Material.PLAYER_HEAD) {
            String displayName = item.getItemMeta().getDisplayName();

            // Parse Team-Nummer aus "§aTeam X"
            if (displayName.startsWith("§aTeam ")) {
                try {
                    String numberStr = displayName.substring(7).trim();
                    int teamNumber = Integer.parseInt(numberStr);

                    // Finde Team-ID basierend auf Nummer
                    UUID targetTeamId = getTeamByNumber(challenge, teamNumber);

                    if (targetTeamId != null) {
                        player.closeInventory();
                        joinCommand.joinExistingTeam(player, targetTeamId);
                    } else {
                        player.sendMessage("§cTeam nicht gefunden!");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§cFehler beim Parsen der Team-Nummer!");
                }
            }
            return;
        }
    }

    /**
     * Findet Team-ID basierend auf Team-Nummer
     */
    private UUID getTeamByNumber(Challenge challenge, int targetNumber) {
        int currentNumber = 1;

        // Nur Teams mit freiem Platz durchgehen
        int teamSize = challenge.getTeamMode().getTeamSize();

        for (Map.Entry<UUID, List<UUID>> entry : challenge.getTeams().entrySet()) {
            if (entry.getValue().size() < teamSize) {
                if (currentNumber == targetNumber) {
                    return entry.getKey();
                }
                currentNumber++;
            }
        }

        return null;
    }
}