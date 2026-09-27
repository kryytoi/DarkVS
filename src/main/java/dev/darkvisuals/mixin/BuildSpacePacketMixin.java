package dev.darkvisuals.mixin;

import dev.darkvisuals.client.managers.BuildSpaceManager;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class BuildSpacePacketMixin {

    @Inject(method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void darkvisuals$freezePackets(Packet<?> packet, CallbackInfo ci) {
        if (!BuildSpaceManager.getInstance().isActive()) return;

        String name = packet.getClass().getSimpleName();
        if (name.equals("KeepAliveC2SPacket") || name.equals("PingResultC2SPacket")) return;

        ci.cancel();
    }
}
