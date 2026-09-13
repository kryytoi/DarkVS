package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.client.util.models.OwlBrain;
import dev.darkvisuals.client.util.models.OwlModel;
import meteordevelopment.orbit.EventHandler;

public class Owl extends Module {

    private final OwlBrain brain = new OwlBrain();
    private final OwlModel model = new OwlModel();

    public Owl() {
        super("Sova", Category.Render, "Совушка со скакалкой :3");
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        brain.setEntity(mc.player);
        brain.onUpdate();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (mc.player == null || mc.world == null) return;

        float tickDelta = event.getTickDelta();
        Vec3d owlWorldPos = brain.getPos(tickDelta);

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();

        MatrixStack ms = event.getMatrices();
        ms.push();
        ms.translate(owlWorldPos.x - cam.x, owlWorldPos.y - cam.y, owlWorldPos.z - cam.z);
         
        ms.scale(0.8F, 0.8F, 0.8F);

        float age = mc.player.age + tickDelta;
        model.setRotationAngles(age, tickDelta, brain);

        BlockPos lightPos = BlockPos.ofFloored(owlWorldPos);
        int light = LightmapTextureManager.pack(
                mc.world.getLightLevel(LightType.BLOCK, lightPos),
                mc.world.getLightLevel(LightType.SKY, lightPos)
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        model.render(ms, vcp, brain, light);
        vcp.draw();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            brain.setEntity(mc.player);
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
