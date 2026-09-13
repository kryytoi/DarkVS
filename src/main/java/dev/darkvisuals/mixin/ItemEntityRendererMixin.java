package dev.darkvisuals.mixin;

import dev.darkvisuals.modules.impl.render.ItemPhysic;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 @Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

	@Shadow @Final private Random random;

	@Unique
	private boolean itemPhysicsEnabled() {
		ItemPhysic mod = ItemPhysic.get();
		return mod != null && mod.isToggled();
	}

	@Inject(
			method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
			at = @At("RETURN")
	)
	private void onUpdateRenderState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
		 
		if (itemPhysicsEnabled()) {
			state.uniqueOffset = 0f;
		}
	}

	@Inject(
			method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void onRender(ItemEntityRenderState state, MatrixStack matrices,
	                      VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		renderWithPhysics(state, matrices, vertexConsumers, light, ci);
		ci.cancel();
	}

	@Unique
	private void renderWithPhysics(ItemEntityRenderState itemEntityRenderState, MatrixStack matrices,
	                               VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		if (itemEntityRenderState.itemRenderState.isEmpty()) {
			return;
		}

		matrices.push();

		float scaleY = itemEntityRenderState.itemRenderState.getTransformation().scale.y();

		 
		float g = MathHelperSin(itemEntityRenderState.age, itemEntityRenderState.uniqueOffset);

		ItemPhysic module = ItemPhysic.get();
		boolean enabled = module != null && module.isToggled();

		if (!enabled) {
			matrices.translate(0.0F, g + 0.25F * scaleY, 0.0F);
		} else {
			 
			matrices.translate(0.0F, 0.06F * scaleY, 0.0F);
		}

		float rotation = ItemEntity.getRotation(itemEntityRenderState.age, itemEntityRenderState.uniqueOffset);

		if (!enabled) {
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation));
		} else {
			 
			 
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
		}

		renderItemStack(matrices, vertexConsumers, light, itemEntityRenderState);

		matrices.pop();
	}

	@Unique
	private float MathHelperSin(float age, float uniqueOffset) {
		return (float) Math.sin(age / 10.0F + uniqueOffset) * 0.1F + 0.1F;
	}

	@Unique
	private void renderItemStack(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
	                             int light, ItemStackEntityRenderState state) {
		this.random.setSeed(state.seed);
		int renderedAmount = state.renderedAmount;
		ItemRenderState itemRenderState = state.itemRenderState;
		boolean hasDepth = itemRenderState.hasDepth();
		float scaleX = itemRenderState.getTransformation().scale.x();
		float scaleY = itemRenderState.getTransformation().scale.y();
		float scaleZ = itemRenderState.getTransformation().scale.z();

		if (!hasDepth) {
			float offsetX = -0.0F * (float)(renderedAmount - 1) * 0.5F * scaleX;
			float offsetY = -0.0F * (float)(renderedAmount - 1) * 0.5F * scaleY;
			float offsetZ = -0.09375F * (float)(renderedAmount - 1) * 0.5F * scaleZ;
			matrices.translate(offsetX, offsetY, offsetZ);
		}

		for (int i = 0; i < renderedAmount; ++i) {
			matrices.push();
			if (i > 0) {
				if (hasDepth) {
					float offsetX = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
					float offsetY = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
					float offsetZ = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
					matrices.translate(offsetX, offsetY, offsetZ);
				} else {
					float offsetX = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
					float offsetY = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
					matrices.translate(offsetX, offsetY, 0.0F);
				}
			}

			itemRenderState.render(matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
			matrices.pop();

			if (!hasDepth) {
				matrices.translate(0.0F * scaleX, 0.0F * scaleY, 0.09375F * scaleZ);
			}
		}
	}
}
