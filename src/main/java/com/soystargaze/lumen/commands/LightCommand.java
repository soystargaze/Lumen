package com.soystargaze.lumen.commands;

import com.soystargaze.lumen.Lumen;
import com.soystargaze.lumen.database.LightRegistry;
import com.soystargaze.lumen.lights.LightHandler;
import com.soystargaze.lumen.utils.text.TextHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class LightCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextHandler.get().logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.light")) {
            TextHandler.get().sendMessage(player,"command.no_permission");
            return true;
        }

        if (args.length == 0) {
            TextHandler.get().sendMessage(player,"command.light.usage");
            return true;
        }

        try {
            int range = Integer.parseInt(args[0]);
            if (range < 1 || range > 150) {
                TextHandler.get().sendAndLog(player, "command.light.invalid_range");
                return true;
            }

            int lightLevel = args.length > 1 ? Integer.parseInt(args[1]) : 15;
            if (lightLevel < 0 || lightLevel > 15) {
                TextHandler.get().sendAndLog(player, "command.light.invalid_level");
                return true;
            }

            boolean includeSkylight = args.length > 2 && Boolean.parseBoolean(args[2]);

            int operationId = LightRegistry.registerOperation(java.util.UUID.randomUUID(), "Light Operation");

            LightHandler lightHandler = new LightHandler(Lumen.getInstance());
            lightHandler.placeLights(player, range, lightLevel, includeSkylight, operationId);

            TextHandler.get().sendAndLog(player, "command.light.success", lightLevel, operationId);
            return true;
        } catch (NumberFormatException e) {
            TextHandler.get().sendAndLog(player, "command.light.invalid_arguments");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> suggestions = new ArrayList<>();
        if (!sender.hasPermission("lumen.light")) {
            return suggestions;
        }

        if (args.length == 1) {
            suggestions.add("<range>");
        } else if (args.length == 2) {
            suggestions.addAll(IntStream.rangeClosed(0, 15)
                    .mapToObj(String::valueOf)
                    .toList());
        } else if (args.length == 3) {
            suggestions.addAll(Arrays.asList("true", "false"));
        }

        return suggestions;
    }
}