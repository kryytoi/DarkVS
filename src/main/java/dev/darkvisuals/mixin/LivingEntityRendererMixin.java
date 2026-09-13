package dev.darkvisuals.mixin;

import dev.darkvisuals.client.util.other.FriendRenderContext;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.HitColor;
import dev.darkvisuals.util.HitColorTintState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<S>> {

    private static final ThreadLocal<Boolean> SV_SHOULD_TINT = HitColorTintState.SHOULD_TINT;

    @Shadow
    protected abstract Identifier getTexture(S state);

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void darkvisuals$clearOwner(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        FriendRenderContext.CURRENT.remove();
        SV_SHOULD_TINT.set(false);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void darkvisuals$prepareTint(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        HitColor module = darkvisuals.getInstance().getModuleManager().getModule(HitColor.class);
        if (module != null && module.isToggled() && state.hurt) {
            SV_SHOULD_TINT.set(true);
             
            state.hurt = false;
        }
    }

    /**
     * Replaces the vanilla hurt tint color with the module's theme color when tinting is active.
     *
     * @author DarkVisuals
     * @reason vanilla returns a hardcoded red mix color; we swap it for the HitColor module's theme color
     */
    @Overwrite
    public int getMixColor(S state) {
        HitColor module = darkvisuals.getInstance().getModuleManager().getModule(HitColor.class);
        if (module != null && module.isToggled() && Boolean.TRUE.equals(SV_SHOULD_TINT.get())) {
            java.awt.Color theme = ThemeManager.getInstance().getCurrentTheme().getBackgroundColor();
            int a = (int) (255 * module.alpha.getValue());
            return new java.awt.Color(theme.getRed(), theme.getGreen(), theme.getBlue(), a).getRGB();
        }
        return -1;
    }

    private static final Identifier SV_WHITE = Identifier.of("minecraft", "textures/misc/white.png");

     @Inject(method = "getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;",
            at = @At("HEAD"), cancellable = true)
    private void darkvisuals$forceTranslucentLayer(S state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> cir) {
        HitColor module = darkvisuals.getInstance().getModuleManager().getModule(HitColor.class);
        if (module != null && module.isToggled() && Boolean.TRUE.equals(SV_SHOULD_TINT.get())) {
             
            cir.setReturnValue(RenderLayer.getEntityTranslucent(SV_WHITE));
        }
    }
} 