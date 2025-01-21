package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.items.LumenItems;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class GiveCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("give")
                .requires(source -> source.getSender().hasPermission("lumen.give"))
                .then(
                        Commands.argument("target", StringArgumentType.string())
                                .suggests((context, builder) -> suggestTargets(builder))
                                .then(
                                        Commands.argument("torch", StringArgumentType.string())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("light");
                                                    builder.suggest("anti_mob");
                                                    return builder.buildFuture();
                                                })
                                                .then(
                                                        Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> {
                                                                    String target = ctx.getArgument("target", String.class);
                                                                    String torchType = ctx.getArgument("torch", String.class);
                                                                    int amount = ctx.getArgument("amount", Integer.class);
                                                                    return handleGiveCommand(ctx.getSource(), target, torchType, amount);
                                                                })
                                                )
                                )
                );
    }

    private static int handleGiveCommand(CommandSourceStack source, String target, String torchType, int amount) {
        if (amount <= 0) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.give.invalid_amount")).color(NamedTextColor.RED));
            return 0;
        }

        LumenItems lumenItems = Lumen.getInstance().getLumenItems();
        ItemStack torch = lumenItems.getLumenItem(torchType.toLowerCase());

        if (torch == null) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.give.invalid_torch")).color(NamedTextColor.RED));
            return 0;
        }

        torch.setAmount(amount);

        if (target.equalsIgnoreCase("@a")) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.getInventory().addItem(torch.clone());
                player.sendMessage(Component.text(TranslationHandler.getFormatted("command.give.received", amount, torchType)).color(NamedTextColor.GREEN));
            });
            source.getSender().sendMessage(Component.text(TranslationHandler.getFormatted("command.give.success_all", amount, torchType)).color(NamedTextColor.GREEN));
        } else {
            Player player = Bukkit.getPlayerExact(target);
            if (player != null && player.isOnline()) {
                player.getInventory().addItem(torch.clone());
                player.sendMessage(Component.text(TranslationHandler.getFormatted("command.give.received", amount, torchType)).color(NamedTextColor.GREEN));
                source.getSender().sendMessage(Component.text(TranslationHandler.getFormatted("command.give.success_one", target, amount, torchType)).color(NamedTextColor.GREEN));
            } else {
                source.getSender().sendMessage(Component.text(TranslationHandler.get("command.give.invalid_player")).color(NamedTextColor.RED));
            }
        }

        return 1;
    }

    private static CompletableFuture<Suggestions> suggestTargets(SuggestionsBuilder builder) {
        Bukkit.getOnlinePlayers().forEach(player -> builder.suggest(player.getName()));
        builder.suggest("@a");
        return builder.buildFuture();
    }
}