package com.erosmari.lumen.commands;

import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.RemoveLightUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ClearCommand implements CommandExecutor, TabCompleter {

    private static final Map<UUID, Long> confirmationRequests = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 30_000;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            LoggingUtils.logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.clear")) {
            LoggingUtils.sendMessage(player,"command.no_permission");
            return true;
        }

        UUID playerId = player.getUniqueId();

        if (args.length == 0) {
            confirmationRequests.put(playerId, System.currentTimeMillis());
            LoggingUtils.sendAndLog(player, "command.clear.request");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            if (!confirmationRequests.containsKey(playerId)) {
                LoggingUtils.sendAndLog(player, "command.clear.no_request");
                return true;
            }

            long requestTime = confirmationRequests.get(playerId);
            if (System.currentTimeMillis() - requestTime > CONFIRMATION_TIMEOUT) {
                confirmationRequests.remove(playerId);
                LoggingUtils.sendAndLog(player, "command.clear.expired");
                return true;
            }

            List<Location> blocks = LightRegistry.getAllBlocks();

            int removedCount = blocks.stream()
                    .mapToInt(location -> RemoveLightUtils.removeLightBlock(location) ? 1 : 0)
                    .sum();

            LightRegistry.clearAllBlocks();
            confirmationRequests.remove(playerId);

            LoggingUtils.sendAndLog(player, "command.clear.success", removedCount);
            return true;
        }

        LoggingUtils.sendMessage(player,"command.clear.usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return List.of("confirm");
        }
        return new ArrayList<>();
    }
}