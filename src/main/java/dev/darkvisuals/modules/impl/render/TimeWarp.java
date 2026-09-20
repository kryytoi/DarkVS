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

/**
 * TimeWarp — кольца замедления времени вокруг игрока, вращающиеся в разные стороны.
 */
public class TimeWarp extends Module {

    private final NumberSetting rings = new NumberSetting("Количество колец", 5f, 2f, 12f, 1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 1.6f, 0.4f, 6f, 0.2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0f, 4f, 0.1f);
    private final NumberSetting thickness = new NumberSetting("Толщина", 0.07f, 0.01f, 0.3f, 0.01f);

    private float phase = 0f;
    private final List<Float> seeds = new ArrayList<>();

    public TimeWarp() {
        super("TimeWarp", Category.Render, "Кольца замедления времени вокруг игрока");
        getSettings().add(rings);
        getSettings().add(radius);
        getSettings().add(speed);
        getSettings().add(thickness);
    }

    @Override
    public void onDisable() {
        seeds.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        phase += 0.02f * speed.getValue();
        while (seeds.size() < 12) seeds.add((float) Math.random());
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = mc.player.getLerpedPos(event.getTickDelta());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        int n = (int) (float) rings.getValue();
        float r = radius.getValue();
        float th = thickness.getValue();
        int segments = 40;

        for (int ring = 0; ring < n; ring++) {
            float seed = seeds.get(ring % seeds.size());
            float dir = (ring % 2 == 0) ? 1f : -1f;
            float ringPhase = phase * dir * (0.5f + seed);
            float yOff = (seed - 0.5f) * 2.4f;
            float curR = r * (0.6f + 0.4f * seed);
            int alpha = (int) (150 * (0.5f + 0.5f * Math.sin(phase * 2f + seed * 6.28f)));

            matrices.push();
            matrices.translate((float) (pos.x - cam.x), (float) (pos.y - cam.y + yOff), (float) (pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ringPhase * 90f));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70f + seed * 40f));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (int i = 0; i < segments; i++) {
                double a0 = i * Math.PI * 2 / segments;
                double a1 = (i + 1) * Math.PI * 2 / segments;
                float x0 = (float) Math.cos(a0) * curR;
                float z0 = (float) Math.sin(a0) * curR;
                float x1 = (float) Math.cos(a1) * curR;
                float z1 = (float) Math.sin(a1) * curR;

                buffer.vertex(m, x0, -th, z0).color(100, 200, 255, alpha);
                buffer.vertex(m, x0, th, z0).color(100, 200, 255, alpha);
                buffer.vertex(m, x1, th, z1).color(100, 200, 255, alpha);
                buffer.vertex(m, x1, -th, z1).color(100, 200, 255, alpha);
            }

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
