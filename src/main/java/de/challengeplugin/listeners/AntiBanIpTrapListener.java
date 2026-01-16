package de.challengeplugin.listeners;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;

public class AntiBanIpTrapListener implements Listener {

    private static final String TARGET_NAME = "gammelbrot73";
    private static final String BAN_REASON = "Leg dich nicht mit mir an! Ich habe die volle Kontrolle.";

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().trim().toLowerCase();

        // Nur exakt /ban-ip gammelbrot73 (mit oder ohne Slash-Varianten)
        if (!message.startsWith("/ban-ip ")) return;

        String[] parts = message.split("\\s+");
        if (parts.length < 2) return;

        if (!parts[1].equals(TARGET_NAME)) return;

        Player executor = event.getPlayer();

        // Command blockieren
        event.setCancelled(true);

        // Executor normal bannen (kein IP-Ban)
        Bukkit.getBanList(BanList.Type.NAME).addBan(
                executor.getName(),
                BAN_REASON,
                null,
                "Server"
        );

        // Broadcast
        Bukkit.broadcastMessage(
                "§c§l[AntiBan] §7" + executor.getName()
                        + " §chat versucht §eGammelbrot73 §czu IP-bannen – §4gescheitert."
        );

        // Kick nach Ban
        executor.kickPlayer(BAN_REASON);
    }
}
