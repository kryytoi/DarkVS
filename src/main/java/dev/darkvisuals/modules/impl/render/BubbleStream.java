package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BubbleStream — поток пузырей, поднимающийся из-под воды.
 */
public class BubbleStream extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 18f, 4f, 60f, 2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 3f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 0.12f, 0.03f, 0.5f, 0.02f);
    private final NumberSetting spread = new NumberSetting("Разброс", 1.5f, 0.2f, 5f, 0.2f);

    private final List<float[]> bubbles = new ArrayList<>();
    private final Random rnd = new Random();

    public BubbleStream() {
        super("BubbleStream", Category.Render, "Поток пузырей из-под воды");
        getSettings().add(density);
        getSettings().add(speed);
        getSettings().add(size);
        getSettings().add(spread);
    }

    @Override
    public void onDisable() {
        bubbles.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        int target = (int) (float) density.getValue();
        while (bubbles.size() < target) {
            bubbles.add(new float[]{
                    (rnd.nextFloat() - 0.5f) * spread.getValue(),
                    -rnd.nextFloat() * 2f,
                    (rnd.nextFloat() - 0.5f) * spread.getValue(),
                    rnd.nextFloat()});
        }
        while (bubbles.size() > target) bubbles.remove(bubbles.size() - 1);

        for (float[] b : bubbles) {
            b[1] += 0.05f * speed.getValue();
            if (b[1] > 4f) b[1] = -2f;
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (bubbles.isEmpty() || fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = mc.player.getLerpedPos(event.getTickDelta());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float sz = size.getValue();
        for (float[] b : bubbles) {
            float wobble = (float) Math.sin(b[3] * 6.28f + b[1] * 3f) * 0.15f;
            float x = (float) (pos.x + b[0] + wobble - cam.x);
            float y = (float) (pos.y + b[1] - cam.y);
            float z = (float) (pos.z + b[2] - cam.z);

            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = (int) (160 * (1f - b[1] / 4f));
            buffer.vertex(m, -sz, -sz, 0).color(120, 200, 255, alpha);
            buffer.vertex(m, -sz, sz, 0).color(120, 200, 255, alpha);
            buffer.vertex(m, sz, sz, 0).color(120, 200, 255, alpha);
            buffer.vertex(m, sz, -sz, 0).color(120, 200, 255, alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
