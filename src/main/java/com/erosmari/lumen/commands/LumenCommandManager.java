package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.command.CommandSender;

import java.util.function.Function;

public class LumenCommandManager {

    private final Lumen plugin;
    private final CommandManager<CommandSender> commandManager;

    public LumenCommandManager(Lumen plugin) {
        this.plugin = plugin;

        try {
            // Configuración del CommandManager usando Paper
            this.commandManager = new PaperCommandManager<>(
                    plugin,
                    CommandExecutionCoordinator.simpleCoordinator(),
                    Function.identity(),
                    Function.identity()
            );
        } catch (Exception e) {
            plugin.getLogger().severe(TranslationHandler.getFormatted("commandmanager.init_error", e.getMessage()));
            throw new RuntimeException(TranslationHandler.get("commandmanager.init_failure"), e);
        }
    }

    /**
     * Registra todos los comandos principales y sus subcomandos.
     */
    public void registerCommands() {
        // Crear el comando principal `/lumen`
        Command.Builder<CommandSender> mainCommand = commandManager.commandBuilder("lumen");

        // Registro del comando principal y subcomandos
        registerMainCommand(mainCommand);
        registerSubCommands(mainCommand);
    }

    /**
     * Registro del comando principal `/lumen`.
     *
     * @param mainCommand El constructor del comando principal.
     */
    private void registerMainCommand(Command.Builder<CommandSender> mainCommand) {
        commandManager.command(
                mainCommand
                        .handler(context -> {
                            CommandSender sender = context.getSender();
                            sender.sendMessage(TranslationHandler.get("command.usage"));
                        })
        );
    }

    /**
     * Registro de subcomandos bajo `/lumen`.
     *
     * @param parentBuilder El constructor del comando principal.
     */
    private void registerSubCommands(Command.Builder<CommandSender> parentBuilder) {
        // Subcomando: /lumen light
        LightCommand.register(commandManager, parentBuilder);

        // Subcomando: /lumen cancel
        CancelCommand.register(commandManager, parentBuilder);

        // Subcomando: /lumen undo
        UndoCommand.register(commandManager, parentBuilder);

        // Subcomando: /lumen redo
        RedoCommand.register(commandManager, parentBuilder);

        // Subcomando: /lumen clear
        ClearCommand.register(commandManager, parentBuilder);

        // Subcomando: /lumen remove
        RemoveCommand.register(commandManager, parentBuilder);

        // Subcomando: /lumen reload
        ReloadCommand.register(commandManager, parentBuilder, plugin);
    }
}
