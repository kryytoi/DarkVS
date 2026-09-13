package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.util.DiscordRichPresenceUtil;
import net.minecraft.client.resource.language.I18n;

public class DiscordRPCModule extends Module {
    public DiscordRPCModule() {
        super("DiscordRPC", Category.Utility, I18n.translate("module.discordrpc.description"));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        DiscordRichPresenceUtil.discordrpc();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        DiscordRichPresenceUtil.shutdownDiscord();
    }
}
