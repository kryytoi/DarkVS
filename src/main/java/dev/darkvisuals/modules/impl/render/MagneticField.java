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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MagneticField — искажающиеся линии магнитного поля вокруг игрока.
 */
public class MagneticField extends Module {

    private final NumberSetting count = new NumberSetting("Количество линий", 10f, 3f, 30f, 1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 2f, 0.5f, 6f, 0.2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0f, 3f, 0.1f);
    private final NumberSetting thickness = new NumberSetting("Толщина", 0.05f, 0.01f, 0.2f, 0.01f);

    private float phase = 0f;
    private final Random rnd = new Random();
    private final List<Float> seeds = new ArrayList<>();

    public MagneticField() {
        super("MagneticField", Category.Render, "Линии магнитного поля вокруг игрока");
        getSettings().add(count);
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
        while (seeds.size() < 32) seeds.add(rnd.nextFloat());
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
        float r = radius.getValue();
        float th = thickness.getValue();
        int segments = 24;

        for (int i = 0; i < n; i++) {
            float seed = seeds.isEmpty() ? 0f : seeds.get(i % seeds.size());
            float tilt = (seed - 0.5f) * 1.2f;
            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = 130;

            for (int s = 0; s < segments; s++) {
                float f0 = s / (float) segments;
                float f1 = (s + 1) / (float) segments;
                double a0 = f0 * Math.PI * 2 + phase + seed * 6.28f;
                double a1 = f1 * Math.PI * 2 + phase + seed * 6.28f;
                float y0 = (float) Math.sin(f0 * Math.PI * 2 + phase * 2f) * r * tilt;
                float y1 = (float) Math.sin(f1 * Math.PI * 2 + phase * 2f) * r * tilt;

                float x0 = (float) (pos.x + Math.cos(a0) * r - cam.x);
                float z0 = (float) (pos.z + Math.sin(a0) * r - cam.z);
                float x1 = (float) (pos.x + Math.cos(a1) * r - cam.x);
                float z1 = (float) (pos.z + Math.sin(a1) * r - cam.z);

                buffer.vertex(m, x0, y0, z0).color(90, 220, 255, alpha);
                buffer.vertex(m, x0, y0 + th, z0).color(90, 220, 255, alpha);
                buffer.vertex(m, x1, y1 + th, z1).color(90, 220, 255, alpha);
                buffer.vertex(m, x1, y1, z1).color(90, 220, 255, alpha);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
