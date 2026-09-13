package dev.darkvisuals.mixin;

import dev.darkvisuals.modules.impl.utility.RWHelper;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 
@Mixin(ClientPlayNetworkHandler.class)
public class AntiRodPullMixin {

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void sv$cancelRodPull(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        RWHelper rwHelper = RWHelper.getInstance();
        if (rwHelper == null || !rwHelper.isToggled()) return;

        try {
            if (rwHelper.shouldCancelVelocityPacket(packet.getEntityId())) {
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }

    @Inject(method = "onExplosion", at = @At("HEAD"), cancellable = true)
    private void sv$cancelExplosionPull(ExplosionS2CPacket packet, CallbackInfo ci) {
        RWHelper rwHelper = RWHelper.getInstance();
        if (rwHelper == null || !rwHelper.isToggled()) return;

        try {
            if (rwHelper.shouldCancelExplosionKnockback()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }

 
    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"), cancellable = true)
    private void sv$cancelPositionVelocityPull(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        RWHelper rwHelper = RWHelper.getInstance();
        if (rwHelper == null || !rwHelper.isToggled()) return;

        try {
            if (rwHelper.shouldCancelPositionLookVelocity(packet)) {
                 
                ((ClientPlayNetworkHandler) (Object) this)
                        .sendPacket(new TeleportConfirmC2SPacket(packet.teleportId()));
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }
}
