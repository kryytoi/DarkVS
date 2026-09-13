package dev.darkvisuals.mixin;

import dev.darkvisuals.client.util.UserTabBadge;
import dev.darkvisuals.modules.impl.utility.UserTab;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

 @Mixin(EntityRenderer.class)
public class UserTabNameTagMixin<T extends Entity> {

    @ModifyVariable(
            method = "renderLabelIfPresent",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Text darkvisuals$addUserTabBadge(Text text) {
        UserTab userTab = UserTab.getInstance();
        if (userTab == null || !userTab.isToggled() || !userTab.getShowAboveHead().getValue() || text == null) {
            return text;
        }

        if (!userTab.containsDarkVisualsUser(text.getString())) return text;

        return UserTabBadge.apply(text);
    }
}
