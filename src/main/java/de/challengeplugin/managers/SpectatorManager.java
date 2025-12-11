package de.challengeplugin.managers;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import de.challengeplugin.models.PlayerChallengeData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.*;

/**
 * ÜBERARBEITET: Verwaltet Spectator-Modus mit Adventure Mode statt Spectator
 * - Adventure Mode
 * - Invincible (unverwundbar)
 * - Fly-Modus
 * - Compass zum Teleportieren
 * - Echte Spieler-Köpfe in GUI
 */
public class SpectatorManager {

    private final ChallengePlugin plugin;

    // Tracking für Spectator-Status
    private final Set<UUID> spectators = new HashSet<>();

    public SpectatorManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Aktiviert Spectator-Modus für Spieler
     * NEU: Adventure Mode + Fly + Invincible statt Spectator
     */
    public void enableSpectatorMode(Player player) {
        // Setze Adventure Mode
        player.setGameMode(GameMode.ADVENTURE);

        // Aktiviere Fly
        player.setAllowFlight(true);
        player.setFlying(true);

        // Mache unverwundbar
        player.setInvulnerable(true);

        // Deaktiviere Kollisionen
        player.setCollidable(false);

        // Optional: Mache unsichtbar für Mobs
        player.setInvisible(true);

        // Tracking
        spectators.add(player.getUniqueId());

        // Gib Compass
        giveSpectatorCompass(player);

        player.sendMessage("§b§lSpectator-Modus aktiviert!");
        player.sendMessage("§7Ich hab dir ein paar meiner Superkräfte abgegeben.");
        player.sendMessage("Du kannst fliegen");
        player.sendMessage("§7Nutze den Compass um zu Spielern zu teleportieren");
    }

    /**
     * Deaktiviert Spectator-Modus
     */
    public void disableSpectatorMode(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.setInvisible(false);

        spectators.remove(player.getUniqueId());

        // Entferne Compass
        player.getInventory().remove(Material.COMPASS);
    }

    /**
     * Gibt Spectator-Compass an Spieler
     */
    public void giveSpectatorCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName("§b§lSpectator-Navigator");
        meta.setLore(Arrays.asList(
                "§7Rechtsklick um Spieler-Liste",
                "§7zu öffnen",
                "",
                "§7Teleportiere dich zu kämpfenden",
                "§7Spielern und schau ihnen zu!"
        ));
        compass.setItemMeta(meta);

        player.getInventory().setItem(4, compass); // Slot 5 (Mitte der Hotbar)
        player.sendMessage("§b§lDu kannst jetzt anderen Spielern zuschauen!");
    }

    /**
     * Öffnet Spectator-Auswahl-GUI
     * NEU: Mit echten Spieler-Köpfen (SkullMeta)
     */
    public void openSpectatorGUI(Player spectator) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) {
            spectator.sendMessage("§cKeine aktive Challenge!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§b§lSpectator-Modus");

        int slot = 0;
        for (UUID playerId : challenge.getParticipants()) {
            PlayerChallengeData data = challenge.getPlayerData().get(playerId);

            // Nur Spieler anzeigen die noch kämpfen
            if (data == null || data.isHasCompleted() || data.isHasForfeited()) continue;

            Player target = Bukkit.getPlayer(playerId);
            if (target == null) continue;

            // NEU: Echter Spieler-Kopf mit SkullMeta
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

            // Setze Owner für echten Skin
            skullMeta.setOwningPlayer(target);

            skullMeta.setDisplayName("§e" + target.getName());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Wave: §e" + (data.getCurrentWaveIndex() + 1) + "§7/§e3");
            lore.add("§7Tode: §c" + data.getTotalDeaths());
            lore.add("§7Health: §c" + String.format("%.1f", target.getHealth()) + "§7/§c20.0 HP");
            lore.add("");
            lore.add("§7Klicke zum Zuschauen");
            skullMeta.setLore(lore);

            playerHead.setItemMeta(skullMeta);
            inv.setItem(slot, playerHead);
            slot++;

            if (slot >= 27) break; // Max 27 Spieler
        }

        // Info wenn keine aktiven Spieler
        if (slot == 0) {
            ItemStack noPlayers = new ItemStack(Material.BARRIER);
            ItemMeta meta = noPlayers.getItemMeta();
            meta.setDisplayName("§c§lKeine aktiven Spieler");
            meta.setLore(Arrays.asList(
                    "§7Alle Spieler haben die Challenge",
                    "§7bereits abgeschlossen oder aufgegeben"
            ));
            noPlayers.setItemMeta(meta);
            inv.setItem(13, noPlayers);
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

        // Teleportiere 2 Blöcke über dem Spieler
        Location tpLoc = target.getLocation().clone().add(0, 2, 0);
        spectator.teleport(tpLoc);

        spectator.sendMessage("§aDu schaust jetzt §e" + target.getName() + " §azu!");
        spectator.playSound(spectator.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    /**
     * Prüft ob Spieler im Spectator-Modus ist
     */
    public boolean isSpectator(UUID playerId) {
        return spectators.contains(playerId);
    }

    /**
     * Cleanup beim Challenge-Ende
     */
    public void cleanup() {
        for (UUID spectatorId : new HashSet<>(spectators)) {
            Player player = Bukkit.getPlayer(spectatorId);
            if (player != null) {
                disableSpectatorMode(player);
            }
        }
        spectators.clear();
    }
}