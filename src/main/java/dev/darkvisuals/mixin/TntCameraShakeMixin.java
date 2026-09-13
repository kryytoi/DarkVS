package dev.darkvisuals.mixin;

import dev.darkvisuals.client.util.TntCameraShakeState;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 @Mixin(Camera.class)
public abstract class TntCameraShakeMixin {

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void darkvisuals$applyTntShake(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                            boolean inverseView, float tickDelta, CallbackInfo ci) {
        float[] offset = TntCameraShakeState.getOffset();
        if (offset[0] == 0f && offset[1] == 0f) return;

        Camera self = (Camera) (Object) this;
        this.setRotation(self.getYaw() + offset[0], self.getPitch() + offset[1]);
    }
}
