package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.Fullbright;
import dev.darkvisuals.modules.impl.utility.Optimization;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {

    @Final @Shadow private SimpleFramebuffer lightmapFramebuffer;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    public void darkvisuals$slowerLight(float delta, CallbackInfo ci) {
         
         
        Optimization optimization = darkvisuals.getInstance().getModuleManager().getModule(Optimization.class);
        if (optimization != null && optimization.isToggled() && optimization.slowerLight.getValue()) {
            Optimization.lightFrameCounter++;
            if (Optimization.lightFrameCounter % 4 != 0) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/SimpleFramebuffer;endWrite()V", shift = At.Shift.BEFORE))
    public void update(float delta, CallbackInfo ci) {
        if (darkvisuals.getInstance().getModuleManager().getModule(Fullbright.class).isToggled()) lightmapFramebuffer.clear();
    }
}