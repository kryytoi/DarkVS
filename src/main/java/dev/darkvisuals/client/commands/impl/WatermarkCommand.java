package dev.darkvisuals.client.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.darkvisuals.client.ChatUtils;
import dev.darkvisuals.client.commands.Command;
import dev.darkvisuals.client.ui.hud.CustomLogo;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.HUD;
import net.minecraft.command.CommandSource;

import java.io.File;

public class WatermarkCommand extends Command {

    public WatermarkCommand() { super("watermark"); }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            ChatUtils.sendMessage(".watermark name <текст>  |  .watermark logo <файл.png>  |  .watermark logo off  |  .watermark reset");
            return 1;
        });

        builder.then(literal("name").then(arg("text", StringArgumentType.greedyString()).executes(ctx -> {
            HUD hud = HUD.getInstance();
            if (hud == null) return 1;
            String text = StringArgumentType.getString(ctx, "text");
            hud.setWatermarkName(text);
            ChatUtils.sendMessage("Название ватермарки: " + text);
            save();
            return 1;
        })));

        builder.then(literal("logo")
                .then(literal("off").executes(ctx -> {
                    HUD hud = HUD.getInstance();
                    if (hud == null) return 1;
                    hud.setCustomLogo(false);
                    ChatUtils.sendMessage("Стандартный логотип включён");
                    save();
                    return 1;
                }))
                .then(arg("file", StringArgumentType.greedyString()).executes(ctx -> {
                    HUD hud = HUD.getInstance();
                    if (hud == null) return 1;
                    String file = StringArgumentType.getString(ctx, "file");
                    File f = new File(darkvisuals.getInstance().getGlobalsDir(), file);
                    if (!f.isFile()) {
                        ChatUtils.sendMessage("Файл не найден: " + f.getAbsolutePath());
                        return 1;
                    }
                    hud.setLogoFile(file);
                    hud.setCustomLogo(true);
                    CustomLogo.invalidate();
                    ChatUtils.sendMessage("Логотип: " + file);
                    save();
                    return 1;
                })));

        builder.then(literal("reset").executes(ctx -> {
            HUD hud = HUD.getInstance();
            if (hud == null) return 1;
            hud.setWatermarkName("Dark Visuals");
            hud.setCustomLogo(false);
            ChatUtils.sendMessage("Ватермарка сброшена");
            save();
            return 1;
        }));
    }

    private void save() {
        try { darkvisuals.getInstance().getAutoSaveManager().scheduleAutoSave(); } catch (Throwable ignored) {}
    }
}