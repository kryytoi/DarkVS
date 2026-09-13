package dev.darkvisuals.mixin;

import dev.darkvisuals.modules.impl.utility.RWHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 
@Mixin(FishingBobberEntity.class)
public class FishingBobberPullMixin {

    @Inject(method = "pullHookedEntity", at = @At("HEAD"), cancellable = true)
    private void sv$cancelPullHookedEntity(Entity entity, CallbackInfo ci) {
        RWHelper rwHelper = RWHelper.getInstance();
        if (rwHelper == null || !rwHelper.isToggled()) return;
        if (!rwHelper.getAntiRodPull().getValue()) return;

        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && entity == mc.player) {
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }
}