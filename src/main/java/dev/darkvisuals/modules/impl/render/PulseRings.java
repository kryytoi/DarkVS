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

/**
 * PulseRings — расходящиеся от игрока пульсирующие кольца.
 */
public class PulseRings extends Module {

    private final NumberSetting maxRings = new NumberSetting("Количество колец", 4f, 1f, 10f, 1f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.1f, 4f, 0.1f);
    private final NumberSetting maxRadius = new NumberSetting("Макс. радиус", 3f, 0.5f, 12f, 0.5f);

    private float phase = 0f;

    public PulseRings() {
        super("PulseRings", Category.Render, "Расходящиеся от игрока пульсирующие кольца");
        getSettings().add(maxRings);
        getSettings().add(speed);
        getSettings().add(maxRadius);
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        phase += 0.01f * speed.getValue();
        if (phase > 1f) phase -= 1f;
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

        int n = (int) (float) maxRings.getValue();
        float maxR = maxRadius.getValue();
        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int segments = 48;
        for (int ring = 0; ring < n; ring++) {
            float t = (phase + ring / (float) n) % 1f;
            float radius = t * maxR;
            float fade = 1f - t;
            int alpha = (int) (200 * fade);
            float th = 0.05f + 0.04f * fade;

            for (int i = 0; i < segments; i++) {
                double a0 = i * Math.PI * 2 / segments;
                double a1 = (i + 1) * Math.PI * 2 / segments;
                float x0 = (float) (pos.x + Math.cos(a0) * radius - cam.x);
                float z0 = (float) (pos.z + Math.sin(a0) * radius - cam.z);
                float x1 = (float) (pos.x + Math.cos(a1) * radius - cam.x);
                float z1 = (float) (pos.z + Math.sin(a1) * radius - cam.z);

                buffer.vertex(m, x0, 0.1f, z0).color(255, 255, 255, alpha);
                buffer.vertex(m, x0, 0.1f + th, z0).color(255, 255, 255, alpha);
                buffer.vertex(m, x1, 0.1f + th, z1).color(255, 255, 255, alpha);
                buffer.vertex(m, x1, 0.1f, z1).color(255, 255, 255, alpha);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
