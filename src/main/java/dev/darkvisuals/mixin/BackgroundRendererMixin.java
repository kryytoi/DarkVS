package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.CustomFog;
import dev.darkvisuals.modules.impl.render.NoFluid;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

	@Inject(
			method = "getFogColor",
			at = @At("RETURN"),
			cancellable = true
	)
	private static void onGetFogColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
		CustomFog customFog = darkvisuals.getInstance().getModuleManager().getModule(CustomFog.class);

		if (customFog != null && customFog.isToggled()) {
			var color = customFog.getSkyColor();
			float red = color.getRed() / 255.0f;
			float green = color.getGreen() / 255.0f;
			float blue = color.getBlue() / 255.0f;
			float alpha = color.getAlpha() / 255.0f;
			cir.setReturnValue(new Vector4f(red, green, blue, alpha));
		}
	}

	@ModifyVariable(method = "applyFog", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static float darkvisuals$applyCustomFogDistance(float viewDistance) {
		CustomFog customFog = darkvisuals.getInstance().getModuleManager().getModule(CustomFog.class);
		if (customFog != null && customFog.isToggled()) {
			return Math.max(0.0f, customFog.getFogDistance());
		}
		return viewDistance;
	}

	@Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
	private static void onApplyFogReturn(
			Camera camera,
			BackgroundRenderer.FogType fogType,
			Vector4f color,
			float viewDistance,
			boolean thickFog,
			float tickDelta,
			CallbackInfoReturnable<Fog> cir
	) {
		NoFluid noFluid = darkvisuals.getInstance().getModuleManager().getModule(NoFluid.class);
		if (noFluid == null) return;

		CameraSubmersionType submersionType = camera.getSubmersionType();
		if (!noFluid.shouldRemoveFog(submersionType)) return;

		Fog originalFog = cir.getReturnValue();
		float red = originalFog != null ? originalFog.red() : color.x;
		float green = originalFog != null ? originalFog.green() : color.y;
		float blue = originalFog != null ? originalFog.blue() : color.z;
		float alpha = originalFog != null ? originalFog.alpha() : color.w;

		if (submersionType == CameraSubmersionType.WATER) {
			float fogEnd = Math.max(viewDistance, 256.0F);
			cir.setReturnValue(new Fog(-8.0F, fogEnd, FogShape.SPHERE, red, green, blue, alpha));
		} else if (submersionType == CameraSubmersionType.LAVA) {
			float fogEnd = Math.max(viewDistance, 192.0F);
			cir.setReturnValue(new Fog(0.0F, fogEnd, FogShape.SPHERE, red, green, blue, alpha));
		} else if (submersionType == CameraSubmersionType.POWDER_SNOW) {
			float fogEnd = Math.max(viewDistance, 128.0F);
			cir.setReturnValue(new Fog(0.0F, fogEnd, FogShape.SPHERE, red, green, blue, alpha));
		}
	}
}