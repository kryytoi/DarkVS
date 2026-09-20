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
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Vortex — вращающаяся воронка частиц вокруг игрока.
 */
public class Vortex extends Module {

    private final NumberSetting count = new NumberSetting("Количество", 40f, 8f, 120f, 4f);
    private final NumberSetting radius = new NumberSetting("Радиус", 2.5f, 1f, 8f, 0.5f);
    private final NumberSetting height = new NumberSetting("Высота", 3f, 1f, 10f, 0.5f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 0.1f, 0.03f, 0.4f, 0.01f);

    private final List<double[]> parts = new ArrayList<>();
    private final Random rnd = new Random();

    public Vortex() {
        super("Vortex", Category.Render, "Вращающаяся воронка частиц вокруг игрока");
        getSettings().add(count);
        getSettings().add(radius);
        getSettings().add(height);
        getSettings().add(speed);
        getSettings().add(size);
    }

    @Override
    public void onDisable() {
        parts.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        int target = (int) (float) count.getValue();
        while (parts.size() < target) parts.add(new double[]{ rnd.nextDouble(), rnd.nextDouble() });
        while (parts.size() > target) parts.remove(parts.size() - 1);

        float sp = speed.getValue() * 0.05f;
        for (double[] p : parts) {
            p[0] = (p[0] + sp) % 1.0;
            p[1] = (p[1] + 0.004 * speed.getValue()) % 1.0;
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (parts.isEmpty() || fullNullCheck()) return;

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

        for (double[] p : parts) {
            double ang = p[0] * Math.PI * 2;
            double y = p[1] * h;
            // спираль сужается к верху
            double curR = r * (1.0 - y / h * 0.6);
            double px = pos.x + Math.cos(ang) * curR - cam.x;
            double py = pos.y + y - cam.y;
            double pz = pos.z + Math.sin(ang) * curR - cam.z;

            matrices.push();
            matrices.translate((float) px, (float) py, (float) pz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float a = 0.6f + 0.4f * (float) Math.sin(p[0] * Math.PI * 4);
            int alpha = (int) (180 * Math.max(0.2, a));
            buffer.vertex(m, -sz, -sz, 0).color(120, 180, 255, alpha);
            buffer.vertex(m, -sz,  sz, 0).color(120, 180, 255, alpha);
            buffer.vertex(m,  sz,  sz, 0).color(120, 180, 255, alpha);
            barrier(m, buffer, sz, alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void barrier(Matrix4f m, BufferBuilder buffer, float sz, int alpha) {
        buffer.vertex(m, sz, -sz, 0).color(120, 180, 255, alpha);
    }

    @SuppressWarnings("unused")
    private Color unused() { return Color.WHITE; }
}
