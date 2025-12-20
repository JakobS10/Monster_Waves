package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Command: /freedom
 * Gibt allen Spielern für 5 Sekunden Spectator-Mode (No-Clip)
 * Funktioniert NUR wenn keine Challenge läuft!
 */
public class FreedomCommand implements CommandExecutor {

    private final ChallengePlugin plugin;

    // Tracke originale GameModes
    private final Map<UUID, GameMode> originalGameModes = new HashMap<>();

    // Cooldown pro Spieler (um Spam zu verhindern)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 10000; // 10 Sekunden

    public FreedomCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Nur Spieler können ausführen
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Command nutzen!");
            return true;
        }

        Player player = (Player) sender;

        // Prüfe ob Challenge läuft
        if (plugin.getChallengeManager().isChallengeActive()) {
            player.sendMessage("§c§lFreedom ist während einer Challenge deaktiviert!");
            player.sendMessage("§7Warte bis die Challenge vorbei ist.");
            return true;
        }

        // Prüfe Cooldown
        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + COOLDOWN_TIME) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage("§cCooldown! Warte noch §e" + (timeLeft / 1000) + "§c Sekunden.");
                return true;
            }
        }

        // Setze Cooldown
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Aktiviere Freedom für alle Online-Spieler
        int freedomCount = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            activateFreedom(p);
            freedomCount++;
        }

        // Broadcast
        Bukkit.broadcastMessage("§b§lFREEDOM AKTIVIERT!");
        Bukkit.broadcastMessage("§7" + freedomCount + " Spieler können nun 5 Sekunden lang fliegen!");
        Bukkit.broadcastMessage("§7Aktiviert von: §e" + player.getName());

        // Sound für alle
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
        }

        // Countdown in Chat
        startCountdown();

        return true;
    }

    /**
     * Aktiviert Freedom-Modus für einen Spieler
     */
    private void activateFreedom(Player player) {
        // Speichere originalen GameMode
        originalGameModes.put(player.getUniqueId(), player.getGameMode());

        // Setze Spectator (No-Clip!)
        player.setGameMode(GameMode.SPECTATOR);

        player.sendMessage("§b§lFREEDOM!");
        player.sendMessage("§7Du kannst jetzt durch Blöcke fliegen!");
        player.sendMessage("§7Aber in fünf Sekunden solltest du dich wieder auf dem Boden der Tatsachen befinden...");
    }

    /**
     * Deaktiviert Freedom-Modus für einen Spieler
     */
    private void deactivateFreedom(Player player) {
        if (!player.isOnline()) return;

        // Stelle originalen GameMode wieder her
        GameMode original = originalGameModes.getOrDefault(player.getUniqueId(), GameMode.SURVIVAL);
        player.setGameMode(original);

        originalGameModes.remove(player.getUniqueId());

        player.sendMessage("§7Freedom beendet!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    /**
     * Countdown und Auto-Deaktivierung nach 5 Sekunden
     */
    private void startCountdown() {
        // Nach 3 Sekunden: Warnung
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("§e⚠ Freedom endet in 2 Sekunden!");

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
                }
            }
        }.runTaskLater(plugin, 60L); // 3 Sekunden

        // Nach 5 Sekunden: Deaktivieren
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (originalGameModes.containsKey(p.getUniqueId())) {
                        deactivateFreedom(p);
                    }
                }

                Bukkit.broadcastMessage("§7Freedom beendet!");
            }
        }.runTaskLater(plugin, 100L); // 5 Sekunden
    }
}