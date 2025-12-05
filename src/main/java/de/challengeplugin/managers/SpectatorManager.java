package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import de.challengeplugin.models.PlayerChallengeData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

/**
 * Verwaltet Spectator-Modus
 * Compass-Item + Teleportation zu kämpfenden Spielern
 */
public class SpectatorManager {

    private final ChallengePlugin plugin;

    public SpectatorManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gibt Spectator-Compass an Spieler
     */
    public void giveSpectatorCompass(Player player) {
        player.sendMessage("§b§lDu kannst jetzt anderen Spielern zuschauen!");
        player.sendMessage("§7Oder du schaust stattdessen TikTok...");
    }

    /**
     * Öffnet Spectator-Auswahl-GUI
     */
    public void openSpectatorGUI(Player spectator) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, "§b§lSpectator-Modus");

        int slot = 0;
        for (UUID playerId : challenge.getParticipants()) {
            PlayerChallengeData data = challenge.getPlayerData().get(playerId);

            // Nur Spieler anzeigen die noch kämpfen
            if (data.isHasCompleted() || data.isHasForfeited()) continue;

            Player target = Bukkit.getPlayer(playerId);
            if (target == null) continue;

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = playerHead.getItemMeta();
            meta.setDisplayName("§e" + target.getName());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Wave: §e" + (data.getCurrentWaveIndex() + 1) + "§7/§e3");
            lore.add("§7Tode: §c" + data.getTotalDeaths());
            lore.add("");
            lore.add("§7Klicke zum Zuschauen");
            meta.setLore(lore);

            playerHead.setItemMeta(meta);
            inv.setItem(slot, playerHead);
            slot++;
        }

        spectator.openInventory(inv);
    }

    /**
     * Teleportiert Spectator zu Spieler
     */
    public void teleportToPlayer(Player spectator, UUID targetPlayerId) {
        Player target = Bukkit.getPlayer(targetPlayerId);

        if (target == null) {
            spectator.sendMessage("§cSpieler nicht online!");
            return;
        }

        spectator.teleport(target.getLocation());
        spectator.sendMessage("§aDu schaust jetzt §e" + target.getName() + " §azu!");
    }
}