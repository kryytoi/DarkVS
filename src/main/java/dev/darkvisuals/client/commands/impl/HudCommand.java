package dev.darkvisuals.client.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.darkvisuals.client.commands.Command;
import dev.darkvisuals.client.ui.hud.HudEditorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;

public class HudCommand extends Command {

    public HudCommand() { super("hud"); }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.send(() -> mc.setScreen(new HudEditorScreen()));
            return 1;
        });
    }
}