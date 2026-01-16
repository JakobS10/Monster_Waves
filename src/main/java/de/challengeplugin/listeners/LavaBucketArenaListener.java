package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Verwandelt Lavaeimer während Combat-Phase automatisch zu normalen Eimern
 * FIX: Läuft für ALLE Spieler, nicht nur in Arena (war das Problem!)
 */
public class LavaBucketArenaListener implements Listener {

    private final ChallengePlugin plugin;
    private int taskId = -1;

    public LavaBucketArenaListener(ChallengePlugin plugin) {
        this.plugin = plugin;
        startLavaCheck();
    }

    /**
     * Startet den Lava-Check Task
     * FIX: Prüft ALLE Spieler, nicht nur die in Arenen!
     */
    private void startLavaCheck() {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

                // Nur während Combat-Phase aktiv
                if (challenge == null || challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) {
                    return;
                }

                // FIX: Prüfe ALLE Teilnehmer (nicht nur in Arena!)
                for (java.util.UUID playerId : challenge.getParticipants()) {
                    Player player = org.bukkit.Bukkit.getPlayer(playerId);
                    if (player == null || !player.isOnline()) continue;

                    // Konvertiere alle Lavaeimer im Inventar
                    convertLavaBuckets(player);
                }
            }
        };

        // Läuft jedes Tick (1 Tick = 50ms)
        taskId = task.runTaskTimer(plugin, 0L, 1L).getTaskId();
    }

    /**
     * Konvertiert alle Lavaeimer zu normalen Eimern
     */
    private void convertLavaBuckets(Player player) {
        boolean converted = false;

        // Prüfe alle Inventory-Slots
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);

            if (item != null && item.getType() == Material.LAVA_BUCKET) {
                // Ersetze durch normalen Eimer
                player.getInventory().setItem(i, new ItemStack(Material.BUCKET, item.getAmount()));
                converted = true;
            }
        }

        // Prüfe Offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == Material.LAVA_BUCKET) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.BUCKET, offhand.getAmount()));
            converted = true;
        }

        // Feedback (nur einmal pro Konvertierung)
        if (converted) {
            player.sendMessage("§cLavaeimer sind während der Kampfphase nicht erlaubt!");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
        }
    }

    /**
     * Cleanup beim Plugin-Disable
     */
    public void cleanup() {
        if (taskId != -1) {
            org.bukkit.Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}