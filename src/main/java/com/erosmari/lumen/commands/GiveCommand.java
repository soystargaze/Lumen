package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.items.LumenItems;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GiveCommand {

    /**
     * Registra el comando `/lumen give`.
     *
     * @param commandManager El administrador de comandos.
     * @param parentBuilder  El constructor del comando principal.
     */
    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder) {
        commandManager.command(
                parentBuilder.literal("give")
                        .argument(StringArgument.<CommandSender>newBuilder("target")
                                .withSuggestionsProvider((context, input) -> suggestTargets()))
                        .argument(StringArgument.<CommandSender>newBuilder("torch")
                                .withSuggestionsProvider((context, input) -> List.of("light", "mob")))
                        .argument(IntegerArgument.of("amount"))
                        .handler(GiveCommand::handleGiveCommand)
        );
    }

    /**
     * Maneja el comando `/lumen give`.
     *
     * @param context Contexto del comando.
     */
    private static void handleGiveCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        // Obtener argumentos
        String target = context.get("target");
        String torchType = context.get("torch");
        int amount = context.get("amount");

        // Validar cantidad
        if (amount <= 0) {
            sender.sendMessage(TranslationHandler.get("command.give.invalid_amount"));
            return;
        }

        // Obtener el ítem personalizado desde LumenItems
        LumenItems lumenItems = Lumen.getInstance().getLumenItems();
        ItemStack torch = lumenItems.getLumenItem(torchType.toLowerCase());

        if (torch == null) {
            sender.sendMessage(TranslationHandler.get("command.give.invalid_torch"));
            return;
        }

        // Establecer cantidad
        torch.setAmount(amount);

        // Dar a todos los jugadores o a uno en particular
        if (target.equalsIgnoreCase("@a")) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.getInventory().addItem(torch.clone());
                onlinePlayer.sendMessage(TranslationHandler.getFormatted("command.give.received", amount, torchType));
            }
            sender.sendMessage(TranslationHandler.getFormatted("command.give.success_all", amount, torchType));
        } else {
            Player player = Bukkit.getPlayerExact(target);
            if (player != null && player.isOnline()) {
                player.getInventory().addItem(torch);
                player.sendMessage(TranslationHandler.getFormatted("command.give.received", amount, torchType));
                sender.sendMessage(TranslationHandler.getFormatted("command.give.success_one", target, amount, torchType));
            } else {
                sender.sendMessage(TranslationHandler.get("command.give.invalid_player"));
            }
        }
    }

    /**
     * Proveedor de sugerencias para el argumento "target".
     *
     * @return Lista de sugerencias dinámicas.
     */
    private static List<String> suggestTargets() {
        List<String> suggestions = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
        suggestions.add("@a");
        return suggestions;
    }
}
