package dev.darkvisuals.mixin;

import dev.darkvisuals.client.util.render.BoundsCapturingProvider;
import dev.darkvisuals.client.util.render.Wrapper;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.SwingAnimation;
import dev.darkvisuals.modules.impl.render.ViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin implements Wrapper {

    @Shadow @Final private MinecraftClient client;
    @Shadow private ItemStack mainHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private ItemStack offHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private float prevEquipProgressOffHand;

    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Shadow
    protected abstract void swingArm(float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm);

    @Shadow
    protected abstract void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    public void injectAfterMatrixPushHandPosition(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = darkvisuals.getInstance().getModuleManager().getModule(ViewModel.class);
        boolean isMainHand = hand == Hand.MAIN_HAND;
        Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();

        if (viewModel.isToggled() && !item.isEmpty() && !item.contains(DataComponentTypes.MAP_ID)) {
            float x = isMainHand ? viewModel.mainX.getValue() : viewModel.offX.getValue();
            float y = isMainHand ? viewModel.mainY.getValue() : viewModel.offY.getValue();
            float z = isMainHand ? viewModel.mainZ.getValue() : viewModel.offZ.getValue();

            boolean isActiveItem = client.player.getActiveItem().getItem() == item.getItem();
            boolean isFoodOrPotion = item.contains(DataComponentTypes.FOOD) || item.getItem() instanceof PotionItem;

            if (isFoodOrPotion && isActiveItem) {
                x = 0.0f;
                z = 0.0f;
            }

            if (arm == Arm.LEFT) {
                x = -x;  
            }

            matrices.translate(x, y, z);
        }
    }

     @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
            )
    )
    public void redirectRenderItemWithOutline(HeldItemRenderer instance,
                                              LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode,
                                              boolean leftHanded, MatrixStack itemMatrices, VertexConsumerProvider itemVertexConsumers, int itemLight,
                                               
                                              AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                              float swingProgress, ItemStack item, float equipProgress,
                                              MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        ViewModel viewModel = darkvisuals.getInstance().getModuleManager().getModule(ViewModel.class);

        boolean outline = false;
        if (viewModel.isConfiguring()) {
            ViewModel.EditedHand edited = (hand == Hand.MAIN_HAND) ? ViewModel.EditedHand.MAIN : ViewModel.EditedHand.OFF;
            outline = viewModel.getEditedHand() == edited && !stack.isEmpty();
        }

        if (!outline) {
            this.renderItem(entity, stack, renderMode, leftHanded, itemMatrices, itemVertexConsumers, itemLight);
            return;
        }

         
        BoundsCapturingProvider capture = new BoundsCapturingProvider(itemVertexConsumers);
        this.renderItem(entity, stack, renderMode, leftHanded, itemMatrices, capture, itemLight);

        if (capture.hasBounds()) {
             
            float pad = 0.006f;
            MatrixStack identity = new MatrixStack();
            VertexConsumer lines = itemVertexConsumers.getBuffer(RenderLayer.getLines());
            VertexRendering.drawBox(identity, lines,
                    capture.minX - pad, capture.minY - pad, capture.minZ - pad,
                    capture.maxX + pad, capture.maxY + pad, capture.maxZ + pad,
                    1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
                    ordinal = 2
            )
    )
    public void redirectSwingArmForCustomAnim(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
        SwingAnimation swingAnimation = darkvisuals.getInstance().getModuleManager().getModule(SwingAnimation.class);
        if (swingAnimation.isToggled()) {
            if (arm == Arm.RIGHT) {
                swingAnimation.renderSwordAnimation(matrices, swingProgress, equipProgress, arm);
            } else {
                this.swingArm(swingProgress, equipProgress, matrices, armX, arm);
            }
        } else {
            this.swingArm(swingProgress, equipProgress, matrices, armX, arm);
        }
    }
}
