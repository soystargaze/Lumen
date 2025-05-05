package com.soystargaze.lumen.commands;

import com.soystargaze.lumen.Lumen;
import com.soystargaze.lumen.utils.LoggingUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LumenCommandExecutor implements CommandExecutor, TabCompleter {

    private final RemoveCommand removeCommand;
    private final UndoCommand undoCommand;
    private final ClearCommand clearCommand;
    private final RedoCommand redoCommand;
    private final LangCommand langCommand;
    private final LightCommand lightCommand;
    private final CancelCommand cancelCommand;
    private final GiveCommand giveCommand;
    private final ReloadCommand reloadCommand;

    public LumenCommandExecutor(Lumen plugin, RemoveCommand removeCommand, UndoCommand undoCommand, ClearCommand clearCommand, RedoCommand redoCommand) {
        this.removeCommand = removeCommand;
        this.undoCommand = undoCommand;
        this.clearCommand = clearCommand;
        this.redoCommand = redoCommand;
        this.langCommand = new LangCommand(plugin);
        this.lightCommand = new LightCommand();
        this.cancelCommand = new CancelCommand();
        this.giveCommand = new GiveCommand();
        this.reloadCommand = new ReloadCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            LoggingUtils.logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.use")) {
            LoggingUtils.sendMessage(player, "command.no_permission");
            return true;
        }

        if (args.length == 0) {
            LoggingUtils.sendMessage(player, "command.usage");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        return switch (subCommand) {
            case "lang" -> langCommand.onCommand(sender, command, label, subArgs);
            case "light" -> lightCommand.onCommand(sender, command, label, subArgs);
            case "cancel" -> cancelCommand.onCommand(sender, command, label, subArgs);
            case "undo" -> undoCommand.onCommand(sender, command, label, subArgs);
            case "redo" -> redoCommand.onCommand(sender, command, label, subArgs);
            case "clear" -> clearCommand.onCommand(sender, command, label, subArgs);
            case "remove" -> removeCommand.onCommand(sender, command, label, subArgs);
            case "give" -> giveCommand.onCommand(sender, command, label, subArgs);
            case "reload" -> reloadCommand.onCommand(sender, command, label, subArgs);
            default -> {
                LoggingUtils.sendMessage(player, "command.usage");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("lumen.lang")) subCommands.add("lang");
            if (sender.hasPermission("lumen.light")) subCommands.add("light");
            if (sender.hasPermission("lumen.cancel")) subCommands.add("cancel");
            if (sender.hasPermission("lumen.undo")) subCommands.add("undo");
            if (sender.hasPermission("lumen.redo")) subCommands.add("redo");
            if (sender.hasPermission("lumen.clear")) subCommands.add("clear");
            if (sender.hasPermission("lumen.remove")) subCommands.add("remove");
            if (sender.hasPermission("lumen.give")) subCommands.add("give");
            if (sender.hasPermission("lumen.reload")) subCommands.add("reload");
            return subCommands;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return switch (subCommand) {
            case "lang" -> langCommand.onTabComplete(sender, command, alias, subArgs);
            case "light" -> lightCommand.onTabComplete(sender, command, alias, subArgs);
            case "clear" -> clearCommand.onTabComplete(sender, command, alias, subArgs);
            case "remove" -> removeCommand.onTabComplete(sender, command, alias, subArgs);
            case "give" -> giveCommand.onTabComplete(sender, command, alias, subArgs);
            default -> new ArrayList<>();
        };
    }
}