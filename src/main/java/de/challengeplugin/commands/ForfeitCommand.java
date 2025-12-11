package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import de.challengeplugin.models.PlayerChallengeData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Command: /aufgeben
 * FIX: Spieler gibt nur SEINE aktuelle Wave auf, nicht die nächste
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

        PlayerChallengeData data = challenge.getPlayerData().get(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§cDu nimmst nicht an der Challenge teil!");
            return true;
        }

        // Prüfe ob Spieler bereits aufgegeben oder fertig ist
        if (data.isHasForfeited()) {
            player.sendMessage("§cDu hast bereits aufgegeben!");
            return true;
        }

        if (data.isHasCompleted()) {
            player.sendMessage("§cDu hast die Challenge bereits abgeschlossen!");
            return true;
        }

        // Aufgeben
        plugin.getChallengeManager().onPlayerForfeited(player.getUniqueId());

        return true;
    }
}