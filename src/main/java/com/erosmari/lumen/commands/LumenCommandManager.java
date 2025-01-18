package com.erosmari.lumen.commands;

import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class LumenCommandManager {

    private final Plugin plugin;
    private PaperCommandManager<CommandSender> commandManager;

    public LumenCommandManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            // Inicializar el Command Manager con un coordinador simple
            commandManager = new PaperCommandManager<>(
                    plugin,
                    CommandExecutionCoordinator.simpleCoordinator(),
                    sender -> sender, // Mapeador hacia CommandSender
                    sender -> sender  // Mapeador hacia CommandSender inverso
            );

            // Registrar los comandos
            registerCommands();

        } catch (Exception e) {
            plugin.getLogger().severe("Error al inicializar el Command Manager: " + e.getMessage());
        }
    }

    private void registerCommands() {
        // Registrar el comando /lumen light
        commandManager.command(
                commandManager.commandBuilder("lumen")
                        .literal("light")
                        .argument(StringArgument.of("type_light")) // Normal o invisible
                        .argument(IntegerArgument.of("range_area")) // Rango del área
                        .argument(BooleanArgument.optional("include_skylight", false)) // Incluye luz del cielo
                        .handler(this::handleLightCommand)
        );

        // Otros comandos como /lumen undo, /lumen reload se registrarán aquí
    }

    private void handleLightCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();
        String typeLight = context.get("type_light");
        int rangeArea = context.get("range_area");
        boolean includeSkylight = context.getOrDefault("include_skylight", false);

        sender.sendMessage("Comando /lumen light ejecutado con:");
        sender.sendMessage(" - Tipo de luz: " + typeLight);
        sender.sendMessage(" - Rango: " + rangeArea);
        sender.sendMessage(" - Incluye skylight: " + includeSkylight);
    }
}
