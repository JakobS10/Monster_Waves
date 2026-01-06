package de.challengeplugin.listeners;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.commands.TrafficLightCommand;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Respawnt Spieler an Todesposition nach Traffic Light
 */
public class TrafficLightListener implements Listener {

    private final ChallengePlugin plugin;
    private final TrafficLightCommand trafficLightCommand;

    public TrafficLightListener(ChallengePlugin plugin, TrafficLightCommand trafficLightCommand) {
        this.plugin = plugin;
        this.trafficLightCommand = trafficLightCommand;
    }

    @EventHandler
    public void onTrafficLightRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Prüfe ob Traffic Light aktiv war
        Location deathPos = trafficLightCommand.getDeathPositions().get(player.getUniqueId());

        if (deathPos != null) {
            // Respawn an Todesposition (NICHT am Spawn!)
            event.setRespawnLocation(deathPos);

            player.sendMessage("§7Du wurdest an deiner Todesposition respawnt.");

            // Entferne aus Map (nur einmal nutzen)
            trafficLightCommand.getDeathPositions().remove(player.getUniqueId());
        }
    }
}