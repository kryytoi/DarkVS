package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventExplosion;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 @Mixin(ClientPlayNetworkHandler.class)
public abstract class ExplosionListenerMixin {

    @Inject(method = "onExplosion", at = @At("HEAD"))
    private void darkvisuals$onExplosion(ExplosionS2CPacket packet, CallbackInfo ci) {
        try {
            darkvisuals.post(new EventExplosion(packet.center()));
        } catch (Throwable ignored) {
             
             
        }
    }
}
