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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * StaticField — мерцающее статическое поле под ногами игрока.
 */
public class StaticField extends Module {

    private final NumberSetting size = new NumberSetting("Размер", 3f, 1f, 10f, 0.5f);
    private final NumberSetting speed = new NumberSetting("Скорость мерцания", 1f, 0.1f, 4f, 0.1f);

    private long t = 0;

    public StaticField() {
        super("StaticField", Category.Render, "Мерцающее статическое поле под игроком");
        getSettings().add(size);
        getSettings().add(speed);
    }

    @EventHandler
    public void onTick(EventTick event) {
        t = System.currentTimeMillis();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = mc.player.getLerpedPos(event.getTickDelta());
        float time = t / 1000f * speed.getValue();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float s = size.getValue();
        int grid = 8;
        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < grid; i++) {
            for (int j = 0; j < grid; j++) {
                float x0 = (float) (pos.x - s + (i * 2f * s) / grid - cam.x);
                float z0 = (float) (pos.z - s + (j * 2f * s) / grid - cam.z);
                float step = 2f * s / grid;
                float flick = (float) Math.sin(time * 8f + i * 2.1f + j * 1.3f);
                if (flick < 0.6f) continue;
                int alpha = (int) (120 * flick);
                buffer.vertex(m, x0, 0.02f, z0).color(180, 180, 200, alpha);
                buffer.vertex(m, x0, 0.02f, z0 + step).color(180, 180, 200, alpha);
                buffer.vertex(m, x0 + step, 0.02f, z0 + step).color(180, 180, 200, alpha);
                buffer.vertex(m, x0 + step, 0.02f, z0).color(180, 180, 200, alpha);
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
