package dev.darkvisuals.mixin;

import dev.darkvisuals.modules.impl.render.HitColor;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.util.HitColorTintState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FeatureRenderer.class)
public abstract class FeatureRendererMixin {

    private static final Identifier SV_WHITE = Identifier.of("minecraft", "textures/misc/white.png");

     
    @Redirect(method = "*",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getArmorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer darkvisuals$armorLayer(Identifier texture) {
        HitColor module = darkvisuals.getInstance().getModuleManager().getModule(HitColor.class);
        if (module != null && module.isToggled() && Boolean.TRUE.equals(HitColorTintState.SHOULD_TINT.get())) {
            return RenderLayer.getEntityTranslucent(SV_WHITE);
        }
        return RenderLayer.getArmorCutoutNoCull(texture);
    }
}


