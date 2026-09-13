package dev.darkvisuals.mixin;

import dev.darkvisuals.modules.impl.utility.NameProtect;
import dev.darkvisuals.modules.impl.utility.RWHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public class ChatHudNameProtectMixin {

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true)
    private Text sv$modifyAddMessage(Text message) {
         
        RWHelper rwHelper = RWHelper.getInstance();
        if (rwHelper != null && rwHelper.isToggled() && message != null) {
            try {
                rwHelper.handleIncomingMessage(message.getString());
            } catch (Throwable ignored) {}
        }

        NameProtect np = NameProtect.getInstance();
        if (np == null || !np.isToggled() || message == null) return message;
        String before = message.getString();
        String after = np.replaceNames(before);
        return before.equals(after) ? message : Text.of(after);
    }
}
