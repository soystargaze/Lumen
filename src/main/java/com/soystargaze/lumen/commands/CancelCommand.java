package com.soystargaze.lumen.commands;

import com.soystargaze.lumen.tasks.TaskManager;
import com.soystargaze.lumen.utils.LoggingUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CancelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            LoggingUtils.logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.cancel")) {
            LoggingUtils.sendMessage(player,"command.no_permission");
            return true;
        }

        if (TaskManager.hasActiveTask(player.getUniqueId())) {
            TaskManager.cancelTask(player.getUniqueId());
            LoggingUtils.sendAndLog(player, "command.cancel.success");
        } else {
            LoggingUtils.sendAndLog(player, "command.cancel.no_task");
        }

        return true;
    }
}