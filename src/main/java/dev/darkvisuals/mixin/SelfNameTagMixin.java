package dev.darkvisuals.mixin;

import dev.darkvisuals.modules.impl.utility.MyNick;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

 
@Mixin(LivingEntityRenderer.class)
public class SelfNameTagMixin {

    @Inject(
            method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sv$forceOwnNameTag(LivingEntity entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        try {
            MyNick myNick = MyNick.getInstance();
            if (myNick != null && myNick.shouldForceOwnLabel(entity)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable ignored) {}
    }
}