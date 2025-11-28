package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimerPauseCommand implements CommandExecutor {

    private final ChallengePlugin plugin;

    public TimerPauseCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis Command can only be executed by Players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage("§cYou are not allowed to execute this Command!");
            return true;
        }

        DataManager dm = plugin.getDataManager();

        if (!dm.isTimerRunning()) {
            player.sendMessage("§cTimer is not running!");
            return true;
        }

        dm.pauseTimer();
        dm.saveData();
        player.sendMessage("§eTimer stopped at: §6" + DataManager.formatTime(dm.getTimerTicks()));

        return true;
    }
}