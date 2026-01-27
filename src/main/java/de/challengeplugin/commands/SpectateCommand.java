package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.models.Challenge;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * @deprecated Dieser Command wurde durch /navigate ersetzt!
 *
 * Command: /spectate [spieler]
 * VERALTET: Redirected zu /navigate
 */
@Deprecated
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

        // Hinweis auf neuen Command
        player.sendMessage("§e§l⚠ VERALTET!");
        player.sendMessage("§7Dieser Command wurde ersetzt durch §e/navigate");
        player.sendMessage("§7Redirect...");

        // Redirect zu /navigate
        if (args.length == 0) {
            Bukkit.dispatchCommand(player, "navigate");
        } else {
            Bukkit.dispatchCommand(player, "navigate " + args[0]);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            Challenge challenge = plugin.getChallengeManager().getActiveChallenge();
            if (challenge != null) {
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