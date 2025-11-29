package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * Command: /spectate [spieler]
 * Teleportiert zu einem kämpfenden Spieler
 */
public class SpectateCommand implements CommandExecutor, TabCompleter {

    private final ChallengePlugin plugin;

    public SpectateCommand(ChallengePlugin plugin) {
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

        // Ohne Argument: Öffne GUI
        if (args.length == 0) {
            plugin.getChallengeManager().getSpectatorManager().openSpectatorGUI(player);
            return true;
        }

        // Mit Argument: Direkt zu Spieler teleportieren
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden: " + args[0]);
            return true;
        }

        plugin.getChallengeManager().getSpectatorManager()
                .teleportToPlayer(player, target.getUniqueId());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
            if (challenge != null) {
                // Schlage alle kämpfenden Spieler vor
                for (UUID playerId : challenge.getParticipants()) {
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null && !challenge.getPlayerData().get(playerId).isHasCompleted()) {
                        completions.add(p.getName());
                    }
                }
            }
        }

        return completions;
    }
}