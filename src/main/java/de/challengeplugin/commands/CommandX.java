package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Collections;
import java.util.List;

/**
 * Geheimer Command - Versteckt vor Autocomplete
 * Nutzung: /commandx <spieler>
 *
 * HINWEIS: Funktioniert nur wenn in plugin.yml registriert!
 */
public class CommandX implements CommandExecutor, TabCompleter {

    private final ChallengePlugin plugin;

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

        // Finde Target
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden.");
            return true;
        }

        // Kann sich nicht selbst killen
        if (target.equals(player)) {
            player.sendMessage("§c[LegacyError]§7 Selbstreferenzierung nicht erlaubt (ERR-73).");
            return true;
        }

        // Kill Target (setzt Health auf 0)
        target.setHealth(0.0);

        // Broadcast-Message
        Bukkit.broadcastMessage("§e" + player.getName() + " §7hat sich an §e" + target.getName() + " §7gerächt!");

        // Log für Admins (nur in Console)
        plugin.getLogger().info("[CommandX] " + player.getName() + " executed commandx on " + target.getName());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Verstecke vor Autocomplete - returne leere Liste
        return Collections.emptyList();
    }
}