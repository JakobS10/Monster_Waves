package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command: /backpack oder /bp
 * Öffnet den Team-Backpack
 *
 * Jedes Team hat seinen eigenen individuellen Backpack!
 */
public class BackpackCommand implements CommandExecutor {

    private final ChallengePlugin plugin;

    public BackpackCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Nur Spieler können Backpack öffnen
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können den Backpack öffnen!");
            return true;
        }

        Player player = (Player) sender;

        // Prüfe ob Challenge aktiv ist
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
        if (challenge == null) {
            player.sendMessage("§cKeine aktive Challenge!");
            player.sendMessage("§7Der Team-Backpack ist nur während einer Challenge verfügbar.");
            return true;
        }

        // Prüfe ob Spieler in Challenge ist
        if (!challenge.getParticipants().contains(player.getUniqueId())) {
            player.sendMessage("§cDu nimmst nicht an der Challenge teil!");
            return true;
        }

        // Öffne Team-Backpack
        plugin.getChallengeManager().getBackpackManager().openBackpack(player);

        return true;
    }
}