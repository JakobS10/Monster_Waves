package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimerToggleCommand implements CommandExecutor {

    private final ChallengePlugin plugin;

    public TimerToggleCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis Command can only be executed by Players!");
            return true;
        }

        Player player = (Player) sender;
        DataManager dm = plugin.getDataManager();

        dm.toggleActionBar(player.getUniqueId());
        dm.saveData();

        boolean enabled = dm.hasActionBarEnabled(player.getUniqueId());
        if (enabled) {
            player.sendMessage("§aActionBar enabled!");
        } else {
            player.sendMessage("§cActionBar disabled!");
        }

        return true;
    }
}