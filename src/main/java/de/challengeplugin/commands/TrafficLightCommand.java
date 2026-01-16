package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Command: /trafficlight
 * SQUID GAME Style - Bewegung = Tod!
 * Nur für Gammelbrot73!
 */
public class TrafficLightCommand implements CommandExecutor {

    private final ChallengePlugin plugin;
    private static final String ALLOWED_USER = "Gammelbrot73";
    private boolean isActive = false;

    // Spieler-Positionen zum Tracken
    private final Map<UUID, Location> playerStartPositions = new HashMap<>();

    public TrafficLightCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Nur Gammelbrot73 darf nutzen!
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Command nutzen!");
            return true;
        }

        Player executor = (Player) sender;
        if (!executor.getName().equals(ALLOWED_USER)) {
            executor.sendMessage("§c§lDieser Command ist nur für " + ALLOWED_USER + "!");
            return true;
        }

        if (isActive) {
            executor.sendMessage("§cTraffic Light läuft bereits!");
            return true;
        }

        startTrafficLight();
        return true;
    }

    /**
     * Startet Traffic Light Sequenz
     */
    private void startTrafficLight() {
        isActive = true;
        playerStartPositions.clear();

        // === PHASE 1: RED LIGHT (3 Sekunden) ===
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Speichere Start-Position
            playerStartPositions.put(player.getUniqueId(), player.getLocation().clone());

            // Gib Schutz-Effekte
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE, 200, 255, false, false, false));

            // NEU: Mache Spieler unverwundbar statt Knockback Resistance
            // So können sie sich bewegen, aber werden nicht von Mobs geschubst
            player.setInvulnerable(true);

            // Riesige ActionBar-Warnung
            player.sendActionBar(Component.text("⚠ RED LIGHT - NICHT BEWEGEN! ⚠")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD));

            // Lauter Sound
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        }

        Bukkit.broadcastMessage("§4§l════════════════════════════════");
        Bukkit.broadcastMessage("§c§l⚠ RED LIGHT - FREEZE! ⚠");
        Bukkit.broadcastMessage("§4§l════════════════════════════════");

        // === PHASE 2: Nach 3 Sekunden - Bewegungs-Check ===
        new BukkitRunnable() {
            @Override
            public void run() {
                checkMovement();
            }
        }.runTaskLater(plugin, 60L); // 3 Sekunden

        // === PHASE 3: Nach 5 Sekunden - GREEN LIGHT ===
        new BukkitRunnable() {
            @Override
            public void run() {
                greenLight();
            }
        }.runTaskLater(plugin, 100L); // 5 Sekunden
    }

    /**
     * Prüft Bewegung und killt Spieler die sich bewegt haben
     */
    private void checkMovement() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location startPos = playerStartPositions.get(player.getUniqueId());
            if (startPos == null) continue;

            Location currentPos = player.getLocation();

            // Berechne Bewegung in X und Z (nicht Y!)
            double deltaX = Math.abs(currentPos.getX() - startPos.getX());
            double deltaZ = Math.abs(currentPos.getZ() - startPos.getZ());

            // Mehr als 2 Blöcke bewegt?
            if (deltaX > 2.0 || deltaZ > 2.0) {
                // BEWEGUNG ERKANNT - KILL!
                player.setHealth(0);

                Bukkit.broadcastMessage("§c§l💀 " + player.getName() + " hat sich bewegt und wurde eliminiert!");
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
            } else {
                // Spieler hat überlebt
                player.sendActionBar(Component.text("✓ Du hast überlebt!")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            }
        }
    }

    /**
     * GREEN LIGHT - Entwarnung
     */
    private void greenLight() {
        Bukkit.broadcastMessage("§a§l════════════════════════════════");
        Bukkit.broadcastMessage("§a§l✓ GREEN LIGHT - SAFE! ✓");
        Bukkit.broadcastMessage("§a§l════════════════════════════════");

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Entferne Invulnerability
            player.setInvulnerable(false);

            player.sendActionBar(Component.text("✓ GREEN LIGHT ✓")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD));

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        }

        // Cleanup
        playerStartPositions.clear();
        isActive = false;
    }

    /**
     * Gibt Map mit Todes-Positionen zurück (für Respawn-Listener)
     */
    public Map<UUID, Location> getDeathPositions() {
        return playerStartPositions;
    }

    public boolean isActive() {
        return isActive;
    }
}