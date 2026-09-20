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
 * LeafCyclone — спиральный вихрь листьев вокруг игрока.
 */
public class LeafCyclone extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 30f, 6f, 80f, 2f);
    private final NumberSetting radius = new NumberSetting("Радиус", 2.5f, 0.5f, 8f, 0.25f);
    private final NumberSetting height = new NumberSetting("Высота", 2f, 0.5f, 6f, 0.25f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 0.16f, 0.05f, 0.4f, 0.01f);

    private final List<float[]> leaves = new ArrayList<>();
    private final Random rnd = new Random();

    public LeafCyclone() {
        super("LeafCyclone", Category.Render, "Спиральный вихрь листьев вокруг игрока");
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(height);
        getSettings().add(speed);
        getSettings().add(size);
    }

    @Override
    public void onDisable() {
        leaves.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        int target = (int) (float) density.getValue();
        while (leaves.size() < target) leaves.add(new float[]{rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()});
        while (leaves.size() > target) leaves.remove(leaves.size() - 1);

        for (float[] l : leaves) l[0] = (l[0] + 0.01f * speed.getValue()) % 1f;
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (leaves.isEmpty() || fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = mc.player.getLerpedPos(event.getTickDelta());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float r = radius.getValue();
        float h = height.getValue();
        float sz = size.getValue();
        float spin = System.currentTimeMillis() / 1000f;

        for (float[] l : leaves) {
            double ang = l[0] * Math.PI * 2;
            double y = l[1] * h;
            double curR = r * (1.0 - y / h * 0.5);
            float x = (float) (pos.x + Math.cos(ang) * curR - cam.x);
            float yy = (float) (pos.y + y - cam.y);
            float z = (float) (pos.z + Math.sin(ang) * curR - cam.z);

            matrices.push();
            matrices.translate(x, yy, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin * 180f * speed.getValue() + l[2] * 360f));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = 200;
            int green = 110 + (int) (60 * l[2]);
            buffer.vertex(m, -sz * 0.6f, -sz, 0).color(90, green, 40, alpha);
            buffer.vertex(m, sz * 0.6f, -sz, 0).color(90, green, 40, alpha);
            buffer.vertex(m, sz * 0.6f, sz, 0).color(90, green, 40, alpha);
            buffer.vertex(m, -sz * 0.6f, sz, 0).color(90, green, 40, alpha);
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
