package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Collections;
import java.util.List;

/**
 * Geheimer Command - Versteckt vor Autocomplete + PIN-geschützt
 * Nutzung: /commandx <spieler> <PIN>
 * PIN: 7876
 */
public class CommandX implements CommandExecutor, TabCompleter {

    private final ChallengePlugin plugin;
    private static final String CORRECT_PIN = "7876";

    public CommandX(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Nur Spieler können ausführen
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Command ausführen.");
            return true;
        }

        Player player = (Player) sender;

        // Keine Argumente = Verwirrende Error-Message
        if (args.length == 0) {
            player.sendMessage("§c[LegacyError]§7 CommandX konnte nicht ausgeführt werden: API mismatch (ERR-42). Bitte wende dich an deinen Systemadministrator.");
            return true;
        }

        // Nur Spielername ohne PIN = Kryptischer Auth-Error
        if (args.length == 1) {
            player.sendMessage("§c[AuthenticationError]§7 Zugriff verweigert: Fehlende Authentifizierungsschlüssel (ERR-AUTH-103). Bitte überprüfe deine Credentials.");
            return true;
        }

        // PIN-Validierung
        String enteredPin = args[1];
        if (!enteredPin.equals(CORRECT_PIN)) {
            player.sendMessage("§c[SecurityException]§7 Ungültige Autorisierung: Token-Mismatch (ERR-SEC-401). Zugriff abgelehnt.");
            // Log falscher Versuch (für Admins in Console)
            plugin.getLogger().warning("[CommandX] Falscher PIN-Versuch von " + player.getName() + " mit PIN: " + enteredPin);
            return true;
        }

        // Finde Target
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§c[TargetError]§7 Zielentität konnte nicht aufgelöst werden (ERR-404). Bitte überprüfe die Eingabe.");
            return true;
        }

        // Kann sich nicht selbst killen
        if (target.equals(player)) {
            player.sendMessage("§c[LogicError]§7 Selbstreferenzierung nicht erlaubt: Rekursive Operation blockiert (ERR-73).");
            return true;
        }

        // Kill Target (setzt Health auf 0)
        target.setHealth(0.0);

        // Broadcast-Message
        Bukkit.broadcastMessage("§e" + player.getName() + " §7hat sich an §e" + target.getName() + " §7gerächt!");

        // Log für Admins (nur in Console)
        plugin.getLogger().info("[CommandX] " + player.getName() + " executed commandx on " + target.getName() + " with correct PIN");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Verstecke ALLES vor Autocomplete - auch den PIN
        return Collections.emptyList();
    }
}