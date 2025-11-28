package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Command: /aufgeben
 * Spieler gibt Challenge auf
 */
public class ForfeitCommand implements CommandExecutor {

    private final ChallengePlugin plugin;

    public ForfeitCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen!");
            return true;
        }

        Player player = (Player) sender;
        Challenge challenge = plugin.getChallengeManager().getActiveChallenge();

        if (challenge == null) {
            player.sendMessage("§cEs läuft keine Challenge!");
            return true;
        }

        if (challenge.getCurrentPhase() != Challenge.ChallengePhase.COMBAT) {
            player.sendMessage("§cDu kannst nur während der Kampfphase aufgeben!");
            return true;
        }

        // Aufgeben
        plugin.getChallengeManager().onPlayerForfeited(player.getUniqueId());

        return true;
    }
}