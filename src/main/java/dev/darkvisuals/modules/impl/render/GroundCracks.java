package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
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
 * GroundCracks — светящиеся трещины, расходящиеся от ног игрока.
 */
public class GroundCracks extends Module {

    private final NumberSetting count = new NumberSetting("Количество", 7f, 3f, 16f, 1f);
    private final NumberSetting length = new NumberSetting("Длина", 2.2f, 0.5f, 8f, 0.25f);
    private final NumberSetting speed = new NumberSetting("Скорость пульсации", 1f, 0f, 3f, 0.1f);

    private float phase = 0f;

    public GroundCracks() {
        super("GroundCracks", Category.Render, "Светящиеся трещины от ног игрока");
        getSettings().add(count);
        getSettings().add(length);
        getSettings().add(speed);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        phase += 0.02f * speed.getValue();
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
        float len = length.getValue();
        float pulse = 0.5f + 0.5f * (float) Math.sin(phase);
        int alpha = (int) (190 * (0.5f + 0.5f * pulse));
        float w = 0.045f;

        for (int i = 0; i < n; i++) {
            double ang = i * Math.PI * 2 / n + phase * 0.1;
            float cx = (float) (pos.x - cam.x);
            float cz = (float) (pos.z - cam.z);
            float ex = (float) (cx + Math.cos(ang) * len);
            float ez = (float) (cz + Math.sin(ang) * len);
            float mx = (cx + ex) / 2f + (float) Math.sin(ang * 3f + phase) * 0.25f;
            float mz = (cz + ez) / 2f + (float) Math.cos(ang * 3f + phase) * 0.25f;

            Matrix4f m = matrices.peek().getPositionMatrix();

            BufferBuilder b1 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            b1.vertex(m, cx, 0.05f, cz - w).color(255, 120, 60, alpha);
            b1.vertex(m, cx, 0.05f, cz + w).color(255, 120, 60, alpha);
            b1.vertex(m, mx, 0.05f, mz + w).color(255, 120, 60, alpha);
            b1.vertex(m, mx, 0.05f, mz - w).color(255, 120, 60, alpha);
            BufferRenderer.drawWithGlobalProgram(b1.end());

            BufferBuilder b2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            b2.vertex(m, mx, 0.05f, mz - w).color(255, 120, 60, alpha);
            b2.vertex(m, mx, 0.05f, mz + w).color(255, 120, 60, alpha);
            b2.vertex(m, ex, 0.05f, ez + w).color(255, 120, 60, alpha);
            b2.vertex(m, ex, 0.05f, ez - w).color(255, 120, 60, alpha);
            BufferRenderer.drawWithGlobalProgram(b2.end());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
