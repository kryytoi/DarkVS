package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.mixin.accessors.IWorldRenderer;
import dev.darkvisuals.modules.impl.utility.HideACBot;
import dev.darkvisuals.modules.impl.utility.Optimization;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void darkvisuals$hideEntities(Entity entity, double x, double y, double z, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (HideACBot.shouldHideEntity(entity)) {
            ci.cancel();
            return;
        }

        Optimization optimization = darkvisuals.getInstance().getModuleManager().getModule(Optimization.class);
        if (optimization == null || !optimization.isToggled()) return;

         
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null && entity == mc.player && mc.options.getPerspective().isFirstPerson()) return;

        boolean isPlayer = entity instanceof AbstractClientPlayerEntity;
        boolean isItem = entity instanceof ItemEntity;

        // полностью скрываем выпавшие предметы, если включена настройка
        if (isItem && optimization.hideItems.getValue()) {
            ci.cancel();
            return;
        }

        // ограничиваем количество одновременно отрисованных предметов
        if (isItem && !optimization.tryConsumeItemBudget()) {
            ci.cancel();
            return;
        }

        boolean shouldCheckFrustum = (isPlayer && optimization.hidePlayers.getValue())
                || (!isPlayer && optimization.hideEntities.getValue());

        if (!shouldCheckFrustum) return;

        try {
            Frustum frustum = ((IWorldRenderer) mc.worldRenderer).getFrustum();
            if (frustum == null) return;

            Box box = entity.getBoundingBox().expand(0.5);
            if (!frustum.isVisible(box)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }
}