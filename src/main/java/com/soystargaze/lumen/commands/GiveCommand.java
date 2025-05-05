package com.soystargaze.lumen.commands;

import com.soystargaze.lumen.Lumen;
import com.soystargaze.lumen.items.LumenItems;
import com.soystargaze.lumen.utils.LoggingUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GiveCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            LoggingUtils.logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.give")) {
            LoggingUtils.sendMessage(player,"command.no_permission");
            return true;
        }

        if (args.length < 2 || args.length > 3) {
            LoggingUtils.sendMessage(player,"command.give.usage");
            return true;
        }

        String target = args[0];
        String torchType = args[1];
        int amount = args.length == 3 ? parseAmount(args[2], sender) : 1;
        if (amount <= 0) {
            return true;
        }

        LumenItems lumenItems = Lumen.getInstance().getLumenItems();
        ItemStack torch = lumenItems.getLumenItem(torchType.toLowerCase());

        if (torch == null) {
            LoggingUtils.sendMessage(player,"command.give.invalid_torch");
            LoggingUtils.logTranslated("command.give.invalid_torch");
            return true;
        }

        torch.setAmount(amount);

        if (target.equalsIgnoreCase("all") || target.equalsIgnoreCase("@a")) {
            Bukkit.getOnlinePlayers().forEach(p -> {
                player.getInventory().addItem(torch.clone());
                LoggingUtils.sendAndLog(player, "command.give.received", amount, torchType);
            });
            LoggingUtils.sendMessage(player,"command.give.success_all", amount, torchType);
            LoggingUtils.logTranslated("command.give.success_all", amount, torchType);
        } else {
            Player p = Bukkit.getPlayerExact(target);
            if (p != null && player.isOnline()) {
                player.getInventory().addItem(torch.clone());
                LoggingUtils.sendAndLog(player, "command.give.received", amount, torchType);
                LoggingUtils.sendMessage(player,"command.give.success_one", target, amount, torchType);
                LoggingUtils.logTranslated("command.give.success_one", target, amount, torchType);
            } else {
                LoggingUtils.sendMessage(player,"command.give.invalid_player");
                LoggingUtils.logTranslated("command.give.invalid_player");
            }
        }

        return true;
    }

    private int parseAmount(String arg, CommandSender sender) {
        if (!(sender instanceof Player player)) {
            LoggingUtils.logTranslated("command.only_players");
            return -1;
        }
        try {
            int amount = Integer.parseInt(arg);
            if (amount <= 0) {
                LoggingUtils.sendMessage(player,"command.give.invalid_amount");
                LoggingUtils.logTranslated("command.give.invalid_amount");
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            LoggingUtils.sendMessage(player,"command.give.invalid_amount");
            LoggingUtils.logTranslated("command.give.invalid_amount");
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
            suggestions.add("all");
        } else if (args.length == 2) {
            suggestions.add("torch");
            suggestions.add("guard");
        }
        return suggestions;
    }
}