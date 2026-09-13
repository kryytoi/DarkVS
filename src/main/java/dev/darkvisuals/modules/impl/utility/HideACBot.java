package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;

public class HideACBot extends Module {
    public HideACBot() {
        super("Hide AC Bot", Category.Utility, I18n.translate("module.hideacbot.description"));
    }

    public static boolean shouldHideEntity(Entity entity) {
        if (darkvisuals.getInstance() == null || darkvisuals.getInstance().getModuleManager() == null) return false;

        Module module = darkvisuals.getInstance().getModuleManager().getModuleByName("Hide AC Bot");
        if (!(module instanceof HideACBot hideACBot) || !hideACBot.isToggled()) return false;

        if (entity instanceof OtherClientPlayerEntity other) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return false;

            double dist = mc.player.distanceTo(other);
            if (dist < 8.0) {
                if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(other.getUuid()) == null) {
                    return true;
                }

                if (other.getAbilities().flying || other.getY() > mc.player.getY() + 2.0 && dist < 5.0) {
                    return true;
                }
            }
        }

        return false;
    }
}
