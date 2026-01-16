package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command: /as <victim> [whisper to <player>] <message>
 * Sendet Nachrichten unter falschem Namen
 *
 * Nur für Gammelbrot73!
 *
 * Beispiele:
 * /as Steve Hallo an alle!
 * /as Steve whisper to Alex Du bist cool!
 */
public class AsCommand implements CommandExecutor, TabCompleter {

    private final ChallengePlugin plugin;
    private static final String ALLOWED_USER = "Gammelbrot73";

    public AsCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Nur Gammelbrot73 darf nutzen
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Command nutzen!");
            return true;
        }

        Player executor = (Player) sender;
        if (!executor.getName().equals(ALLOWED_USER)) {
            executor.sendMessage("§c§lDieser Command ist nur für " + ALLOWED_USER + "!");
            return true;
        }

        // Mindestens Victim + Message
        if (args.length < 2) {
            executor.sendMessage("§cFalsche Verwendung!");
            executor.sendMessage("§7/as <Victim> <Message>");
            executor.sendMessage("§7/as <Victim> whisper to <Player> <Message>");
            return true;
        }

        // Finde Victim
        String victimName = args[0];
        Player victim = Bukkit.getPlayerExact(victimName);

        if (victim == null) {
            executor.sendMessage("§cSpieler nicht gefunden: " + victimName);
            return true;
        }

        // Prüfe ob "whisper to" dabei ist
        boolean isWhisper = false;
        Player whisperTarget = null;
        int messageStartIndex = 1;

        if (args.length >= 4 && args[1].equalsIgnoreCase("whisper") && args[2].equalsIgnoreCase("to")) {
            isWhisper = true;
            whisperTarget = Bukkit.getPlayerExact(args[3]);

            if (whisperTarget == null) {
                executor.sendMessage("§cWhisper-Ziel nicht gefunden: " + args[3]);
                return true;
            }

            messageStartIndex = 4;
        }

        // Prüfe ob Message vorhanden ist
        if (messageStartIndex >= args.length) {
            executor.sendMessage("§cKeine Nachricht angegeben!");
            return true;
        }

        // Baue Message zusammen
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = messageStartIndex; i < args.length; i++) {
            if (i > messageStartIndex) {
                messageBuilder.append(" ");
            }
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString();

        // Sende Nachricht
        if (isWhisper) {
            // Whisper-Modus
            sendFakeWhisper(victim, whisperTarget, message);
            executor.sendMessage("§a✓ Fake-Whisper gesendet!");
            executor.sendMessage("§7Von: §e" + victim.getName() + " §7→ §e" + whisperTarget.getName());
            executor.sendMessage("§7Message: §f" + message);
        } else {
            // Public Chat
            sendFakeMessage(victim, message);
            executor.sendMessage("§a✓ Fake-Message gesendet!");
            executor.sendMessage("§7Als: §e" + victim.getName());
            executor.sendMessage("§7Message: §f" + message);
        }

        return true;
    }

    /**
     * Sendet eine gefälschte öffentliche Nachricht
     */
    private void sendFakeMessage(Player fakeAuthor, String message) {
        // Vanilla Minecraft Chat-Format: <Name> Message
        String fakeMessage = "<" + fakeAuthor.getName() + "> " + message;

        // Sende an alle Online-Spieler
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(fakeMessage);
        }

        // Log in Console (damit man nachvollziehen kann was passiert ist)
        plugin.getLogger().info("[FAKE CHAT] " + fakeMessage + " (real sender: " + ALLOWED_USER + ")");
    }

    /**
     * Sendet einen gefälschten Whisper
     */
    private void sendFakeWhisper(Player fakeAuthor, Player target, String message) {
        // Vanilla Minecraft Whisper-Format
        String whisperToTarget = "§7" + fakeAuthor.getName() + " flüstert dir zu: §f" + message;

        // Sende nur an Ziel
        target.sendMessage(whisperToTarget);

        // Log in Console
        plugin.getLogger().info("[FAKE WHISPER] " + fakeAuthor.getName() + " -> " + target.getName() + ": " + message + " (real sender: " + ALLOWED_USER + ")");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Nur für Gammelbrot73
        if (!(sender instanceof Player) || !((Player) sender).getName().equals(ALLOWED_USER)) {
            return completions;
        }

        if (args.length == 1) {
            // Victim: Alle Online-Spieler
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            // Entweder Message oder "whisper"
            completions.add("whisper");
            completions.add("Hallo!");
            completions.add("Du");
            completions.add("Ich");
        } else if (args.length == 3 && args[1].equalsIgnoreCase("whisper")) {
            // Nach "whisper" kommt "to"
            completions.add("to");
        } else if (args.length == 4 && args[1].equalsIgnoreCase("whisper") && args[2].equalsIgnoreCase("to")) {
            // Whisper-Ziel: Alle Online-Spieler
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        return completions;
    }
}