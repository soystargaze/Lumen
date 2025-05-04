package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.connections.CoreProtectHandler;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.RemoveLightUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RemoveCommand implements CommandExecutor, TabCompleter {

    private final Lumen plugin;

    public RemoveCommand(Lumen plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            LoggingUtils.logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.remove")) {
            LoggingUtils.sendMessage(player,"command.no_permission");
            return true;
        }



        if (args.length != 2 || !args[0].equalsIgnoreCase("area")) {
            LoggingUtils.sendMessage(player,"command.remove.usage");
            return true;
        }

        try {
            int range = Integer.parseInt(args[1]);
            if (range < 1 || range > 100) {
                LoggingUtils.sendAndLog(player, "command.remove.invalid_range");
                return true;
            }

            Location playerLocation = player.getLocation();
            List<Location> blocks = LightRegistry.getBlocksInRange(playerLocation, range);

            if (blocks.isEmpty()) {
                LoggingUtils.sendAndLog(player, "command.remove.area.no_blocks", range);
                return true;
            }

            CoreProtectHandler coreProtectHandler = plugin.getCoreProtectHandler();
            boolean coreProtectAvailable = coreProtectHandler != null && coreProtectHandler.isEnabled();

            if (!coreProtectAvailable) {
                LoggingUtils.sendAndLog(player, "command.remove.coreprotect_not_available");
            }

            int removedCount = removeAndLogBlocks(blocks, player, coreProtectAvailable ? coreProtectHandler : null);

            LoggingUtils.sendAndLog(player, "command.remove.area.success", removedCount, range);
            return true;
        } catch (NumberFormatException e) {
            LoggingUtils.sendAndLog(player, "command.remove.invalid_range");
            return true;
        }
    }

    private int removeAndLogBlocks(List<Location> blocks, Player player, CoreProtectHandler coreProtectHandler) {
        int removedCount = 0;

        for (Location block : blocks) {
            if (RemoveLightUtils.removeLightBlock(block)) {
                removedCount++;
            }
        }

        if (removedCount > 0 && coreProtectHandler != null) {
            coreProtectHandler.logRemoval(player.getName(), blocks, Material.LIGHT);
        }

        return removedCount;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return List.of("area");
        }
        return new ArrayList<>();
    }
}