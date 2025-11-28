package de.challengeplugin.commands;

import de.challengeplugin.ChallengePlugin;
import de.challengeplugin.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TimerCommand implements CommandExecutor {

    private final ChallengePlugin plugin;

    public TimerCommand(ChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        DataManager dm = plugin.getDataManager();
        String status = dm.isTimerRunning() ? "§aRunning" : "§cPaused";
        String time = DataManager.formatTime(dm.getTimerTicks());

        sender.sendMessage("§6=== Timer State ===");
        sender.sendMessage("§7State: " + status);
        sender.sendMessage("§7Time: §e" + time);

        return true;
    }
}