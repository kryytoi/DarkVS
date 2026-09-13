package dev.darkvisuals.mixin;

import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.managers.HitDetectionManager;
import net.minecraft.entity.LivingEntity;
import dev.darkvisuals.darkvisuals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class AttackEventMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
         
        if (target instanceof LivingEntity living && living.hurtTime > 2) {
            ci.cancel();
            return;
        }

        EventAttackEntity event = new EventAttackEntity(self, target);
        darkvisuals.post(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
         
        boolean duplicateByTime = !dev.darkvisuals.client.managers.HitDetectionManager.getInstance().canProcessHit(self, target);
        if (duplicateByTime) {
            event.setEffectsAllowed(false);
        }
         
        HitDetectionManager.getInstance().registerHit(self, target);
    }
} 