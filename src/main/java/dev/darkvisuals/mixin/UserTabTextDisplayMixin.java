package dev.darkvisuals.mixin;

import dev.darkvisuals.client.util.UserTabBadge;
import dev.darkvisuals.modules.impl.utility.UserTab;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

 
@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public class UserTabTextDisplayMixin {

    @ModifyVariable(
            method = "getLines",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text darkvisuals$addBadgeToTextDisplay(Text text) {
        try {
            UserTab userTab = UserTab.getInstance();
            if (userTab == null || !userTab.isToggled() || !userTab.getShowAboveHead().getValue() || text == null) {
                return text;
            }

             
            if (userTab.hasBadge(text)) return text;

            String name = userTab.findDarkVisualsUserName(text.getString());
            if (name == null) return text;

            return darkvisuals$insertBadgeBeforeName(text, name);
        } catch (Throwable ignored) {
            return text;
        }
    }

 
    @Unique
    private static Text darkvisuals$insertBadgeBeforeName(Text text, String name) {
        MutableText rebuilt = Text.empty();
        String lowerName = name.toLowerCase();
        boolean[] inserted = {false};

        text.visit((style, str) -> {
            if (!inserted[0]) {
                int idx = str.toLowerCase().indexOf(lowerName);
                if (idx != -1) {
                     
                    if (idx > 0) {
                        rebuilt.append(Text.literal(str.substring(0, idx)).setStyle(style));
                    }
                     
                    rebuilt.append(UserTabBadge.apply(Text.empty()));
                     
                    rebuilt.append(Text.literal(str.substring(idx)).setStyle(style));
                    inserted[0] = true;
                    return Optional.empty();
                }
            }
            rebuilt.append(Text.literal(str).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);

         
        if (!inserted[0]) {
            return UserTabBadge.apply(text);
        }

        return rebuilt;
    }
}
