package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.mixin.accessors.IWorldRenderer;
import dev.darkvisuals.modules.impl.utility.Optimization;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Inject(
            method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void darkvisuals$hideBlockEntities(BlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        Optimization optimization = darkvisuals.getInstance().getModuleManager().getModule(Optimization.class);
        if (optimization == null || !optimization.isToggled() || !optimization.hideBlockEntities.getValue()) return;

        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            Frustum frustum = ((IWorldRenderer) mc.worldRenderer).getFrustum();
            if (frustum == null) return;

            BlockPos pos = blockEntity.getPos();
            Box box = new Box(pos).expand(0.5);
            if (!frustum.isVisible(box)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }
}