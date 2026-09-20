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
 * DigitalRain — вертикальный «цифровой дождь» вокруг игрока.
 */
public class DigitalRain extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 40f, 8f, 120f, 4f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 12f, 4f, 32f, 1f);
    private final NumberSetting height = new NumberSetting("Высота", 6f, 2f, 20f, 1f);

    private final List<float[]> drops = new ArrayList<>();
    private final Random rnd = new Random();

    public DigitalRain() {
        super("DigitalRain", Category.Render, "Цифровой дождь вокруг игрока");
        getSettings().add(density);
        getSettings().add(speed);
        getSettings().add(radius);
        getSettings().add(height);
    }

    @Override
    public void onDisable() {
        drops.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        int target = (int) (float) density.getValue();
        while (drops.size() < target) {
            float a = rnd.nextFloat() * 6.28318f;
            float d = radius.getValue() * (0.3f + 0.7f * rnd.nextFloat());
            drops.add(new float[]{a, d, rnd.nextFloat(), rnd.nextFloat()});
        }
        while (drops.size() > target) drops.remove(drops.size() - 1);

        for (float[] d : drops) d[2] -= 0.02f * speed.getValue();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (drops.isEmpty() || fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = mc.player.getLerpedPos(event.getTickDelta());
        float h = height.getValue();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (float[] d : drops) {
            if (d[2] < 0f) d[2] = 1f;
            float x = (float) (pos.x + Math.cos(d[0]) * d[1] - cam.x);
            float z = (float) (pos.z + Math.sin(d[0]) * d[1] - cam.z);
            float y = (float) (pos.y + h * d[2] - cam.y);

            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = 180;
            float w = 0.03f;
            float len = 0.35f * speed.getValue();
            buffer.vertex(m, -w, 0, 0).color(40, 255, 120, alpha);
            buffer.vertex(m, -w, len, 0).color(40, 255, 120, alpha);
            buffer.vertex(m, w, len, 0).color(40, 255, 120, alpha);
            buffer.vertex(m, w, 0, 0).color(40, 255, 120, alpha);
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
