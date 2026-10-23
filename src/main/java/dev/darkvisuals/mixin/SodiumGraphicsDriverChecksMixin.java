package dev.darkvisuals.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.compatibility.checks.GraphicsDriverChecks", remap = false)
public abstract class SodiumGraphicsDriverChecksMixin {
    @Inject(method = "postContextInit", at = @At("HEAD"), cancellable = true)
    private static void darkvisuals$skipDriverVersionCheck(CallbackInfo ci) {
        ci.cancel();
    }
}
