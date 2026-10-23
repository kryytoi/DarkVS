package dev.darkvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.modules.impl.render.NoRender;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.renderer.Render3D;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.darkvisuals.modules.impl.render.Zoom;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements Wrapper {

	@Inject(method = "renderWorld", at = @At("HEAD"))
	public void renderWorld(RenderTickCounter renderTickCounter, CallbackInfo ci) {
		Render3D.prepare();

		Render3D.clearCustomHitBoxQueues();
		Render3D.clearBlockOverlayQueues();
	}

	@WrapOperation(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
	private void darkvisuals$renderWorldEffects(WorldRenderer worldRenderer, ObjectAllocator allocator, RenderTickCounter renderTickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, Operation<Void> original) {
		original.call(worldRenderer, allocator, renderTickCounter, renderBlockOutline, camera, gameRenderer, positionMatrix, projectionMatrix);

		// Use the actual world-render arguments instead of unstable local variables.
		RenderSystem.getModelViewStack().pushMatrix();
		try {
			RenderSystem.getModelViewStack().mul(positionMatrix);
			Render3D.setTickDelta(renderTickCounter.getTickDelta(true));
			// View bobbing is already applied to the projection matrix.
			EventRender3D.Game event = new EventRender3D.Game(renderTickCounter, new MatrixStack());
			darkvisuals.post(event);

			Render3D.draw(Render3D.QUADS, Render3D.DEBUG_LINES, false);
			Render3D.draw(Render3D.SHINE_QUADS, Render3D.SHINE_DEBUG_LINES, true);
		} finally {
			RenderSystem.getModelViewStack().popMatrix();
		}
	}

	@Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
	public void showFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
		if (darkvisuals.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && darkvisuals.getInstance().getModuleManager().getModule(NoRender.class).totem.getValue()) ci.cancel();
	}

	@Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
	public void tiltViewWhenHurt(CallbackInfo ci) {
		if (darkvisuals.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && darkvisuals.getInstance().getModuleManager().getModule(NoRender.class).hurtCam.getValue()) ci.cancel();
	}

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void applyZoomFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		Zoom zoom = darkvisuals.getInstance().getModuleManager().getModule(Zoom.class);
		if (zoom != null && zoom.isToggled()) {
			float base = cir.getReturnValue();
			float adjusted = zoom.applyZoom(base, tickDelta);
			cir.setReturnValue(adjusted);
		}
	}
}
