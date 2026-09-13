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
import dev.darkvisuals.client.util.models.GibBrain;
import dev.darkvisuals.client.util.models.GibModel;
import meteordevelopment.orbit.EventHandler;

public class Gib extends Module {

    private final GibBrain brain = new GibBrain();
    private final GibModel model = new GibModel();

    public Gib() {
        super("Gib", Category.Render, "Гыб — полукот :3");
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
        Vec3d gibWorldPos = brain.getPos(tickDelta);

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();

        MatrixStack ms = event.getMatrices();
        ms.push();
        ms.translate(gibWorldPos.x - cam.x, gibWorldPos.y - cam.y, gibWorldPos.z - cam.z);

        float age = mc.player.age + tickDelta;
        model.setRotationAngles(age, brain);

        BlockPos lightPos = BlockPos.ofFloored(gibWorldPos);
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
