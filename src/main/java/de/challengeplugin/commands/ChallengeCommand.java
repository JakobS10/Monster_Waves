package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;


/**
 * Hauptbefehl: /challenge start <farmTimeSeconds> <bossPlayer>
 */
public class ChallengeCommand implements CommandExecutor, TabCompleter {

    private final ChallengePlugin plugin;

    public ChallengeCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen!");
            return true;
        }

        Player player = (Player) sender;

        // Kein Argument = Hilfe
        if (args.length == 0) {
            player.sendMessage("§6§l=== Challenge Commands ===");
            player.sendMessage("§e/challenge start <Sekunden> <Boss> §7- Challenge starten");
            player.sendMessage("§e/challenge cancel §7- Challenge abbrechen");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // === START ===
        if (subCommand.equals("start")) {
            if (!player.hasPermission("challenge.start")) {
                player.sendMessage("§cDu hast keine Berechtigung für diesen Befehl!");
                return true;
            }

            if (args.length < 3) {
                player.sendMessage("§cFalsche Verwendung!");
                player.sendMessage("§7Verwendung: /challenge start <FarmZeit-Sekunden> <Boss-Spieler>");
                return true;
            }

            // Parse Farm-Zeit
            int farmTimeSeconds;
            try {
                farmTimeSeconds = Integer.parseInt(args[1]);
                if (farmTimeSeconds <= 0) {
                    player.sendMessage("§cFarmzeit muss positiv sein!");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cUngültige Farmzeit: " + args[1]);
                return true;
            }

            // Finde Boss-Spieler
            Player bossPlayer = Bukkit.getPlayer(args[2]);
            if (bossPlayer == null || !bossPlayer.isOnline()) {
                player.sendMessage("§cSpieler nicht gefunden: " + args[2]);
                return true;
            }

            // Starte Challenge
            plugin.getChallengeManager().startChallenge(farmTimeSeconds, bossPlayer, player);
            return true;
        }

        // === CANCEL ===
        else if (subCommand.equals("cancel")) {
            if (!player.hasPermission("challenge.admin")) {
                player.sendMessage("§cDu hast keine Berechtigung für diesen Befehl!");
                return true;
            }

            if (plugin.getChallengeManager().getActiveChallenge() == null) {
                player.sendMessage("§cEs läuft keine Challenge!");
                return true;
            }

            player.sendMessage("§e§lChallenge wird abgebrochen...");
            plugin.getChallengeManager().cancelChallenge();
            player.sendMessage("§aChallenge erfolgreich abgebrochen!");

            return true;
        }

        // Unbekannter Subcommand
        else {
            player.sendMessage("§cUnbekannter Befehl!");
            player.sendMessage("§7Nutze: /challenge start oder /challenge cancel");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("start");
            completions.add("cancel");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            completions.add("300");  // 5 Minuten als Vorschlag
            completions.add("600");  // 10 Minuten
        } else if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            // Alle Online-Spieler vorschlagen
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }

        return completions;
    }
}