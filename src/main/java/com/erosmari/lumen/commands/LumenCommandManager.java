package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("UnstableApiUsage")
public class LumenCommandManager {

    private final Lumen plugin;
    private final RemoveCommand removeCommand;
    private final UndoCommand undoCommand;
    private final ClearCommand clearCommand;
    private final RedoCommand redoCommand;

    public LumenCommandManager(Lumen plugin) {
        this.plugin = plugin;
        this.removeCommand = new RemoveCommand(plugin);
        this.undoCommand = new UndoCommand(plugin);
        this.clearCommand = new ClearCommand();
        this.redoCommand = new RedoCommand(plugin);
    }

    public void registerCommands() {
        LifecycleEventManager<@org.jetbrains.annotations.NotNull Plugin> manager = plugin.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            registerCommand(commands, "lumen");
            registerCommand(commands, "lu");
            registerCommand(commands, "l");
        });
    }

    private void registerCommand(Commands commands, String commandName) {
        commands.register(
                Commands.literal(commandName)
                        .requires(source -> source.getSender().hasPermission("lumen.use"))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.usage"));
                            return 1;
                        })
                        .then(LangCommand.register(plugin))
                        .then(LightCommand.register())
                        .then(CancelCommand.register())
                        .then(undoCommand.register())
                        .then(redoCommand.register())
                        .then(clearCommand.register())
                        .then(removeCommand.register())
                        .then(GiveCommand.register())
                        .then(ReloadCommand.register(plugin))
                        .build()
        );
    }
}