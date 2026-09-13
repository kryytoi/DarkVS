package dev.darkvisuals.client.commands.impl;

import dev.darkvisuals.client.commands.Command;
import dev.darkvisuals.client.ChatUtils;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.client.ui.hud.HudElement;
import net.minecraft.command.CommandSource;
import net.minecraft.client.resource.language.I18n;
import dev.darkvisuals.client.managers.ThemeManager;

public class ResetCommand extends Command {
    private static long lastRequestMs = 0L;
    private static final long CONFIRM_WINDOW_MS = 10_000L;
    public ResetCommand() {
        super("reset");
    }

    @Override
    public void execute(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            long now = System.currentTimeMillis();
            if (now - lastRequestMs > CONFIRM_WINDOW_MS) {
                lastRequestMs = now;
                ChatUtils.sendMessage(I18n.translate("cmd.reset.confirm"));
                return 1;
            }

             
            darkvisuals.getInstance().getCommandManager().setPrefix(".");

             
            for (Module module : darkvisuals.getInstance().getModuleManager().getModules()) {
                if (module.isToggled()) module.setToggled(false);
                for (Setting<?> setting : module.getSettings()) {
                    setting.reset();
                }
            }

             
            darkvisuals.getInstance().getModuleManager().resetBindsToDefaults();

             
            for (HudElement hud : darkvisuals.getInstance().getHudManager().getHudElements()) {
                for (Setting<?> setting : hud.getSettings()) {
                    setting.reset();
                }
                if (!hud.isToggled()) hud.setToggled(true);
            }

             
            try {
                ThemeManager.getInstance().setTheme(new ThemeManager.LightTheme());
            } catch (Throwable ignored) {}

            ChatUtils.sendMessage(I18n.translate("cmd.reset.done"));

            try {
                darkvisuals.getInstance().getAutoSaveManager().forceSave();
            } catch (Throwable ignored) {}
            lastRequestMs = 0L;
            return 1;
        });
    }
}
