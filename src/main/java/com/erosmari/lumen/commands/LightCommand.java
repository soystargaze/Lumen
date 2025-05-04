package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.lights.LightHandler;
import com.erosmari.lumen.utils.LoggingUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LightCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            LoggingUtils.logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.light")) {
            LoggingUtils.sendMessage(player,"command.no_permission");
            return true;
        }

        if (args.length == 0) {
            LoggingUtils.sendMessage(player,"command.light.usage");
            return true;
        }

        try {
            int range = Integer.parseInt(args[0]);
            if (range < 1 || range > 150) {
                LoggingUtils.sendAndLog(player, "command.light.invalid_range");
                return true;
            }

            int lightLevel = args.length > 1 ? Integer.parseInt(args[1]) : 15;
            if (lightLevel < 0 || lightLevel > 15) {
                LoggingUtils.sendAndLog(player, "command.light.invalid_level");
                return true;
            }

            boolean includeSkylight = args.length > 2 && Boolean.parseBoolean(args[2]);

            int operationId = LightRegistry.registerOperation(java.util.UUID.randomUUID(), "Light Operation");

            LightHandler lightHandler = new LightHandler(Lumen.getInstance());
            lightHandler.placeLights(player, range, lightLevel, includeSkylight, operationId);

            LoggingUtils.sendAndLog(player, "command.light.success", lightLevel, operationId);
            return true;
        } catch (NumberFormatException e) {
            LoggingUtils.sendAndLog(player, "command.light.invalid_arguments");
            return true;
        }
    }
}