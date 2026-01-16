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
 * ERWEITERT: Verwaltet ECHTEN Spectator-Modus
 * - Echter Spectator GameMode
 * - GUI über /navigate Command (kein Compass mehr)
 * - NEU: Spectators sehen Bossbars der beobachteten Spieler!
 */
public class SpectatorManager {

    private final ChallengePlugin plugin;

    // Tracking für Spectator-Status
    private final Set<UUID> spectators = new HashSet<>();

    // NEU: Tracking welchen Spieler der Spectator gerade beobachtet
    private final Map<UUID, UUID> spectatorWatching = new HashMap<>();

    public SpectatorManager(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Aktiviert ECHTEN Spectator-Modus für Spieler
     */
    public void enableSpectatorMode(Player player) {
        // Setze ECHTEN Spectator Mode
        player.setGameMode(GameMode.SPECTATOR);

        // Tracking
        spectators.add(player.getUniqueId());

        player.sendMessage("§b§l=== Spectator-Modus aktiviert ===");
        player.sendMessage("§7Du kannst nun anderen Spielern zuschauen");
        player.sendMessage("§7Nutze §e/navigate §7um zu Spielern zu teleportieren");
        player.sendMessage("");
        player.sendMessage("§7§oDu siehst dann auch ihre Wave-Fortschritt!");
    }

    /**
     * Deaktiviert Spectator-Modus
     */
    public void disableSpectatorMode(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        spectators.remove(player.getUniqueId());

        // NEU: Entferne Bossbars
        hideWaveInfoFromSpectator(player);
        spectatorWatching.remove(player.getUniqueId());
    }

    /**
     * Öffnet Spectator-Auswahl-GUI (über /navigate Command)
     */
    public void openSpectatorGUI(Player spectator) {
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) {
            spectator.sendMessage("§cKeine aktive Challenge!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§b§lNavigator");

        int slot = 0;
        for (UUID playerId : challenge.getParticipants()) {
            PlayerChallengeData data = challenge.getPlayerData().get(playerId);

            // Nur Spieler anzeigen die noch kämpfen
            if (data == null || data.isHasCompleted() || data.isHasForfeited()) continue;

            Player target = Bukkit.getPlayer(playerId);
            if (target == null) continue;

            // Echter Spieler-Kopf mit SkullMeta
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

            // Setze Owner für echten Skin
            skullMeta.setOwningPlayer(target);

            skullMeta.setDisplayName("§e" + target.getName());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Wave: §e" + (data.getCurrentWaveIndex() + 1));
            lore.add("§7Tode: §c" + data.getTotalDeaths());
            lore.add("§7Health: §c" + String.format("%.1f", target.getHealth()) + "§7/§c20.0 HP");
            lore.add("");
            lore.add("§7Klicke zum Zuschauen");
            lore.add("§d§o+ Wave-Bossbar wird angezeigt!");
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
     * NEU: Zeigt auch die Bossbar des Ziels an!
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

        // NEU: Zeige Wave-Info (Bossbar)
        showWaveInfoToSpectator(spectator, targetPlayerId);
    }

    /**
     * NEU: Zeigt Spectator die Wave-Bossbar eines Spielers
     */
    private void showWaveInfoToSpectator(Player spectator, UUID targetPlayerId) {
        // Entferne alte Bossbars falls vorhanden
        hideWaveInfoFromSpectator(spectator);

        // Zeige neue Bossbar
        plugin.getChallengeManager().getWaveManager()
                .showBossbarToSpectator(spectator, targetPlayerId);

        // Tracking
        spectatorWatching.put(spectator.getUniqueId(), targetPlayerId);

        Player target = Bukkit.getPlayer(targetPlayerId);
        if (target != null) {
            spectator.sendMessage("§d§o✨ Du siehst jetzt " + target.getName() + "'s Wave-Fortschritt!");
        }
    }

    /**
     * NEU: Entfernt Wave-Info von Spectator
     */
    private void hideWaveInfoFromSpectator(Player spectator) {
        plugin.getChallengeManager().getWaveManager()
                .hideAllBossbarsFromSpectator(spectator);
    }

    /**
     * Prüft ob Spieler im Spectator-Modus ist
     */
    public boolean isSpectator(UUID playerId) {
        return spectators.contains(playerId);
    }

    /**
     * NEU: Gibt zurück welchen Spieler der Spectator gerade beobachtet
     */
    public UUID getWatchingPlayer(UUID spectatorId) {
        return spectatorWatching.get(spectatorId);
    }

    /**
     * Cleanup beim Challenge-Ende
     * NEU: Auch Bossbars werden entfernt!
     */
    public void cleanup() {
        for (UUID spectatorId : new HashSet<>(spectators)) {
            Player player = Bukkit.getPlayer(spectatorId);
            if (player != null) {
                // Entferne Bossbars
                hideWaveInfoFromSpectator(player);
                disableSpectatorMode(player);
            }
        }
        spectators.clear();
        spectatorWatching.clear();
    }
}