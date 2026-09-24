package dev.darkvisuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.util.world.WorldUtils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "addWeatherParticlesAndSound", at = @At("HEAD"), cancellable = true)
    private void darkvisuals$cancelWeatherParticles(net.minecraft.client.render.Camera camera, CallbackInfo ci) {
        if (dev.darkvisuals.modules.impl.utility.Optimization.isWeatherHidden()) ci.cancel();
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void darkvisuals$cancelWeatherRender(FrameGraphBuilder builder, net.minecraft.util.math.Vec3d pos,
            float tickDelta, Fog fog, CallbackInfo ci) {
        if (dev.darkvisuals.modules.impl.utility.Optimization.isWeatherHidden()) ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        EventRender3D.World event = new EventRender3D.World(camera, positionMatrix, tickCounter);
        darkvisuals.post(event);
        WorldUtils.lastWorld.set(positionMatrix);
        WorldUtils.lastProj.set(RenderSystem.getProjectionMatrix());
        WorldUtils.lastModelView.set(RenderSystem.getModelViewMatrix());
    }
}