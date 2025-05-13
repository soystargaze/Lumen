package com.soystargaze.lumen.commands;

import com.soystargaze.lumen.tasks.TaskManager;
import com.soystargaze.lumen.utils.text.TextHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CancelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextHandler.get().logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.cancel")) {
            TextHandler.get().sendMessage(player,"command.no_permission");
            return true;
        }

        if (TaskManager.hasActiveTask(player.getUniqueId())) {
            TaskManager.cancelTask(player.getUniqueId());
            TextHandler.get().sendAndLog(player, "command.cancel.success");
        } else {
            TextHandler.get().sendAndLog(player, "command.cancel.no_task");
        }

        return true;
    }
}