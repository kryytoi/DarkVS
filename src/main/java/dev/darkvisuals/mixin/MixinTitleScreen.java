package dev.darkvisuals.mixin;

import dev.darkvisuals.client.ui.mainmenu.MainMenu;
import dev.darkvisuals.client.util.Wrapper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 @Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen implements Wrapper {

    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    public void init(CallbackInfo ci) {
        mc.setScreen(new MainMenu());
    }
}
