package com.soystargaze.lumen.commands;

import com.soystargaze.lumen.Lumen;
import com.soystargaze.lumen.database.LightRegistry;
import com.soystargaze.lumen.connections.CoreProtectHandler;
import com.soystargaze.lumen.utils.text.TextHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UndoCommand implements CommandExecutor {

    private final Lumen plugin;

    public UndoCommand(Lumen plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextHandler.get().logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.undo")) {
            TextHandler.get().sendMessage(player,"command.no_permission");
            return true;
        }

        int count = 1;
        if (args.length > 0) {
            try {
                count = Integer.parseInt(args[0]);
                if (count < 1) {
                    TextHandler.get().sendAndLog(player, "command.undo.invalid_count");
                    return true;
                }
            } catch (NumberFormatException e) {
                TextHandler.get().sendAndLog(player, "command.undo.invalid_count");
                return true;
            }
        }

        List<Integer> lastOperations = LightRegistry.getLastOperations(count);

        if (lastOperations.isEmpty()) {
            TextHandler.get().sendAndLog(player, "command.undo.no_previous_operations");
            return true;
        }

        int totalRemovedBlocks = removeLightBlocksByOperations(lastOperations, player);

        if (totalRemovedBlocks > 0) {
            TextHandler.get().sendAndLog(player, "command.undo.success", totalRemovedBlocks, count);
        } else {
            TextHandler.get().sendAndLog(player, "command.undo.no_blocks", count);
        }
        return true;
    }

    private int removeLightBlocksByOperations(List<Integer> operationIds, Player player) {
        CoreProtectHandler coreProtectHandler = plugin.getCoreProtectHandler();
        List<Location> allRemovedBlocks = new ArrayList<>();

        for (int operationId : operationIds) {
            List<Location> blocks = LightRegistry.getBlocksByOperationId(operationId);

            if (!blocks.isEmpty()) {
                List<Location> removedBlocks = new ArrayList<>();
                for (Location location : blocks) {
                    if (removeLightBlock(location)) {
                        removedBlocks.add(location);
                    }
                }

                if (!removedBlocks.isEmpty()) {
                    LightRegistry.softDeleteBlocksByOperationId(operationId);
                    allRemovedBlocks.addAll(removedBlocks);
                }
            }
        }

        if (!allRemovedBlocks.isEmpty() && coreProtectHandler != null && coreProtectHandler.isEnabled()) {
            coreProtectHandler.logRemoval(player.getName(), allRemovedBlocks, Material.LIGHT);
        }

        return allRemovedBlocks.size();
    }

    private boolean removeLightBlock(Location location) {
        if (location.getBlock().getType() == Material.LIGHT) {
            location.getBlock().setType(Material.AIR, false);
            return true;
        }
        return false;
    }
}