package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.events.impl.EventGameShutdown;
import dev.darkvisuals.client.util.math.Counter;
import dev.darkvisuals.client.managers.HitDetectionManager;
import dev.darkvisuals.client.util.Wrapper;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements Wrapper {

    private static int tickCounter = 0;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void darkvisuals$replaceDeathScreen(net.minecraft.client.gui.screen.Screen screen, CallbackInfo ci) {
        if (!(screen instanceof net.minecraft.client.gui.screen.DeathScreen)) return;
        if (!dev.darkvisuals.modules.impl.render.CustomDeathScreen.shouldReplace()) return;

        ci.cancel();
        dev.darkvisuals.client.ui.death.DarkDeathScreen custom =
                new dev.darkvisuals.client.ui.death.DarkDeathScreen(
                        ((dev.darkvisuals.mixin.accessors.IDeathScreenAccessor) screen).darkvisuals$getMessage(),
                        ((dev.darkvisuals.mixin.accessors.IDeathScreenAccessor) screen).darkvisuals$isHardcore());
        ((MinecraftClient) (Object) this).setScreen(custom);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        EventTick event = new EventTick();
        darkvisuals.post(event);
        Counter.updateFPS();

         
        tickCounter++;
        if (tickCounter >= 20) {
            HitDetectionManager.getInstance().cleanup();
            tickCounter = 0;
        }
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    public void updateWindowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("DarkVisuals Release 0.1");
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
         
        dev.darkvisuals.util.DiscordRichPresenceUtil.shutdownDiscord();
        dev.darkvisuals.client.managers.CosmeticsSyncManager.shutdown();
        dev.darkvisuals.client.managers.UserTabManager.shutdown();

         
        darkvisuals.post(new EventGameShutdown());
    }
}