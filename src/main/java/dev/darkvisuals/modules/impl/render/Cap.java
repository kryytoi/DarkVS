package dev.darkvisuals.modules.impl.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.LightType;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.client.util.models.CapModel;
import meteordevelopment.orbit.EventHandler;

public class Cap extends Module {

    private final CapModel model = new CapModel();
    private float propellerRotation = 0.0f;
    private float propellerSpeed = 0.0f;

    public Cap() {
        super("Cap", Category.Render, "Кепка с пропеллером на игроке");
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        if (!mc.player.isOnGround() && mc.player.getVelocity().y < -0.08) {
            propellerSpeed = Math.min(propellerSpeed + 4.0f, 60.0f);
        } else {
            propellerSpeed = Math.max(propellerSpeed - 2.0f, 0.0f);
        }
        propellerRotation += propellerSpeed;
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.options.getPerspective().isFirstPerson()) return;

        float tickDelta = event.getTickDelta();

        double x = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX());
        double y = MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY());
        double z = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ());

        Camera camera = mc.gameRenderer.getCamera();
        net.minecraft.util.math.Vec3d cam = camera.getPos();

        MatrixStack ms = event.getMatrices();
        ms.push();

        double headOffsetY = mc.player.isSneaking() ? 1.25D : 1.5D;
        ms.translate(x - cam.x, y + headOffsetY - cam.y, z - cam.z);

        float yaw = MathHelper.lerp(tickDelta, mc.player.prevHeadYaw, mc.player.headYaw);
        float pitch = MathHelper.lerp(tickDelta, mc.player.prevPitch, mc.player.getPitch());

        ms.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(yaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

        BlockPos lightPos = BlockPos.ofFloored(x, y + headOffsetY, z);
        int light = LightmapTextureManager.pack(
                mc.world.getLightLevel(LightType.BLOCK, lightPos),
                mc.world.getLightLevel(LightType.SKY, lightPos)
        );

         
         
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        model.render(ms, vcp, light, propellerRotation);
        vcp.draw();

        ms.pop();
    }
}