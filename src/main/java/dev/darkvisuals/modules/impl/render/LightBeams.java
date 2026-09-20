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
 * LightBeams — вертикальные лучи света, падающие с неба вокруг игрока.
 */
public class LightBeams extends Module {

    private final NumberSetting count = new NumberSetting("Количество", 6f, 2f, 20f, 1f);
    private final NumberSetting height = new NumberSetting("Высота", 14f, 4f, 40f, 1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 2f, 0.3f, 6f, 0.2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0f, 3f, 0.1f);
    private final NumberSetting fade = new NumberSetting("Прозрачность", 0.5f, 0.05f, 1f, 0.05f);

    private float phase = 0f;
    private final Random rnd = new Random();
    private final List<Float> seeds = new ArrayList<>();

    public LightBeams() {
        super("LightBeams", Category.Render, "Вертикальные лучи света с неба");
        getSettings().add(count);
        getSettings().add(height);
        getSettings().add(radius);
        getSettings().add(speed);
        getSettings().add(fade);
    }

    @Override
    public void onDisable() {
        seeds.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        phase += 0.01f * speed.getValue();
        while (seeds.size() < 24) seeds.add(rnd.nextFloat());
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

        int n = (int) (float) count.getValue();
        float h = height.getValue();
        float r = radius.getValue();
        float baseAlpha = fade.getValue();

        for (int i = 0; i < n; i++) {
            float seed = seeds.isEmpty() ? 0f : seeds.get(i % seeds.size());
            double ang = phase * (0.3f + seed) + i * 2.39f;
            float x = (float) (pos.x + Math.cos(ang) * (r + seed * 2f) - cam.x);
            float z = (float) (pos.z + Math.sin(ang) * (r + seed * 2f) - cam.z);

            matrices.push();
            matrices.translate(x, (float) (pos.y - cam.y), z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float pulse = 0.6f + 0.4f * (float) Math.sin(phase * 2f + seed * 6.28f);
            int alpha = (int) (120 * baseAlpha * pulse);
            float w = 0.22f;
            buffer.vertex(m, -w, 0, 0).color(255, 250, 210, alpha);
            buffer.vertex(m, -w, h, 0).color(255, 250, 210, alpha);
            buffer.vertex(m, w, h, 0).color(255, 250, 210, alpha);
            buffer.vertex(m, w, 0, 0).color(255, 250, 210, alpha);
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
