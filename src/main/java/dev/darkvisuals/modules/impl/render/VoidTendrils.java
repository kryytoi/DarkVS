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
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;

/**
 * VoidTendrils — тёмные щупальца, тянущиеся из-под земли вокруг игрока.
 */
public class VoidTendrils extends Module {

    private final NumberSetting count = new NumberSetting("Количество", 7f, 3f, 16f, 1f);
    private final NumberSetting height = new NumberSetting("Высота", 2.2f, 0.5f, 6f, 0.25f);
    private final NumberSetting speed = new NumberSetting("Скорость", 0.8f, 0f, 3f, 0.1f);
    private final NumberSetting thickness = new NumberSetting("Толщина", 0.09f, 0.02f, 0.3f, 0.01f);

    private float time;
    private final Random rnd = Random.create();

    public VoidTendrils() {
        super("VoidTendrils", Category.Render, "Тёмные щупальца из-под земли вокруг игрока");
        getSettings().add(count);
        getSettings().add(height);
        getSettings().add(speed);
        getSettings().add(thickness);
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        time += 0.03f * speed.getValue();
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
        float th = thickness.getValue();

        for (int i = 0; i < n; i++) {
            float baseAng = i * 6.28318f / n;
            float sway = (float) Math.sin(time + i * 1.7f) * 0.5f;
            int segments = 10;

            for (int s = 0; s < segments; s++) {
                float f0 = s / (float) segments;
                float f1 = (s + 1) / (float) segments;
                float y0 = f0 * h;
                float y1 = f1 * h;
                float r0 = (1f - f0) * 0.9f + 0.1f;
                float r1 = (1f - f1) * 0.9f + 0.1f;
                float a0 = baseAng + sway * f0;
                float a1 = baseAng + sway * f1;

                float x0 = (float) (Math.cos(a0) * r0);
                float z0 = (float) (Math.sin(a0) * r0);
                float x1 = (float) (Math.cos(a1) * r1);
                float z1 = (float) (Math.sin(a1) * r1);

                Matrix4f m = matrices.peek().getPositionMatrix();
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                int alpha = (int) (200 * (1f - f0));
                float t0 = th * (1f - f0 * 0.6f);
                float t1 = th * (1f - f1 * 0.6f);

                buffer.vertex(m, (float) (pos.x + x0 - cam.x - t0), (float) (pos.y + y0 - cam.y), (float) (pos.z + z0 - cam.z)).color(60, 0, 90, alpha);
                buffer.vertex(m, (float) (pos.x + x0 - cam.x + t0), (float) (pos.y + y0 - cam.y), (float) (pos.z + z0 - cam.z)).color(60, 0, 90, alpha);
                buffer.vertex(m, (float) (pos.x + x1 - cam.x + t1), (float) (pos.y + y1 - cam.y), (float) (pos.z + z1 - cam.z)).color(60, 0, 90, alpha);
                buffer.vertex(m, (float) (pos.x + x1 - cam.x - t1), (float) (pos.y + y1 - cam.y), (float) (pos.z + z1 - cam.z)).color(60, 0, 90, alpha);
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
