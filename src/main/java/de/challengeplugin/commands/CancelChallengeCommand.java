package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command: /challenge cancel
 * Bricht laufende Challenge ab und räumt auf
 */
public class CancelChallengeCommand implements CommandExecutor {

    private final ChallengePlugin plugin;

    public CancelChallengeCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission prüfen
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("challenge.admin")) {
            player.sendMessage("§cDu hast keine Berechtigung für diesen Befehl!");
            return true;
        }

        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge == null) {
            player.sendMessage("§cEs läuft keine Challenge!");
            return true;
        }

        // Bestätigungs-Nachricht
        player.sendMessage("§e§lChallenge wird abgebrochen...");

        // Broadcast an alle
        Bukkit.broadcastMessage("§c§l=== CHALLENGE ABGEBROCHEN ===");
        Bukkit.broadcastMessage("§7Die Challenge wurde von " + player.getName() + " abgebrochen");

        // Cleanup durchführen
        plugin.getChallengeManager().cancelChallenge();

        player.sendMessage("§aChallenge erfolgreich abgebrochen!");

        return true;
    }
}