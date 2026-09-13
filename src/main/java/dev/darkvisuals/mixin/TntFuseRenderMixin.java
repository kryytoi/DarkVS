package dev.darkvisuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.TnTEffect;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.TntEntityRenderer;
import net.minecraft.client.render.entity.state.TntEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 @Mixin(TntEntityRenderer.class)
public abstract class TntFuseRenderMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void darkvisuals$onFuseRenderHead(TntEntityRenderState state, MatrixStack matrices,
                                               VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        TnTEffect module = darkvisuals.getInstance().getModuleManager().getModule(TnTEffect.class);
        if (module == null || !module.isToggled() || !module.fusePulse.getValue()) return;

        float scale = module.getFusePulseScale(state.fuse);
        if (scale == 1f) return;

         
         
         
         
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, -0.5, -0.5);
         
         
         
        darkvisuals$pendingPop = true;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void darkvisuals$onFuseRenderTail(TntEntityRenderState state, MatrixStack matrices,
                                               VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (darkvisuals$pendingPop) {
            matrices.pop();
            darkvisuals$pendingPop = false;
        }

        TnTEffect module = darkvisuals.getInstance().getModuleManager().getModule(TnTEffect.class);
        if (module == null || !module.isToggled() || !module.fuseGlow.getValue()) return;

        RenderSystem.assertOnRenderThread();
        module.queueFuseGlow(state);
    }

    @Unique
    private boolean darkvisuals$pendingPop = false;
}
