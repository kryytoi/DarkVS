package dev.darkvisuals.client.managers;

import com.mojang.brigadier.CommandDispatcher;
import dev.darkvisuals.client.commands.Command;
import dev.darkvisuals.client.commands.impl.*;
import dev.darkvisuals.client.util.Wrapper;
import lombok.*;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandManager implements Wrapper {

    private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
    private final CommandSource source = new ClientCommandSource(null, mc);
    private final List<Command> commands = new ArrayList<>();
    @Setter private String prefix = ".";

    public CommandManager() {
        addCommands(
                new ConfigCommand(),
                new ConfigAliasCommand(),
                new FriendsCommand(),
                new GpsCommand(),
                new PrefixCommand(),
                new BindCommand(),
                new ResetCommand(),
                new WatermarkCommand(),
                new HudCommand()
        );
    }

    private void addCommands(Command... command) {
        for (Command cmd : command) {
            cmd.register(dispatcher);
            commands.add(cmd);
        }
    }
}