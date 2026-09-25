package dev.darkvisuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.CustomFog;
import dev.darkvisuals.modules.impl.render.NoFluid;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

	@Inject(method = "applyFog", at = @At("TAIL"))
	private static void onApplyFogTail(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
		NoFluid noFluid = darkvisuals.getInstance().getModuleManager().getModule(NoFluid.class);
		if (noFluid == null) return;

		CameraSubmersionType submersionType = camera.getSubmersionType();
		if (!noFluid.shouldRemoveFog(submersionType)) return;

		if (submersionType == CameraSubmersionType.WATER) {
			// Отодвигаем границы подводного тумана
			RenderSystem.setShaderFogStart(-8.0F);
			RenderSystem.setShaderFogEnd(Math.max(viewDistance, 256.0F));
			RenderSystem.setShaderFogShape(FogShape.SPHERE);
		} else if (submersionType == CameraSubmersionType.LAVA) {
			// Убираем плотный туман лавы для чистой видимости
			RenderSystem.setShaderFogStart(0.0F);
			RenderSystem.setShaderFogEnd(Math.max(viewDistance, 192.0F));
			RenderSystem.setShaderFogShape(FogShape.SPHERE);
		}
	}
}