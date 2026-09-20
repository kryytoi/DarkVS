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
 * GlitchScreen — глитч-квадраты, хаотично появляющиеся вокруг игрока.
 */
public class GlitchScreen extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 12f, 2f, 40f, 2f);
    private final NumberSetting size = new NumberSetting("Размер", 0.7f, 0.1f, 3f, 0.1f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 4f, 1f, 16f, 1f);

    private static final class Glitch {
        float angle, dist, height, life, maxLife;
    }

    private final List<Glitch> glitches = new ArrayList<>();
    private final Random rnd = new Random();

    public GlitchScreen() {
        super("GlitchScreen", Category.Render, "Глитч-квадраты вокруг игрока");
        getSettings().add(density);
        getSettings().add(size);
        getSettings().add(speed);
        getSettings().add(radius);
    }

    @Override
    public void onDisable() {
        glitches.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        int target = (int) (float) density.getValue();
        while (glitches.size() < target) {
            Glitch g = new Glitch();
            g.angle = rnd.nextFloat() * 6.28318f;
            g.dist = radius.getValue() * (0.2f + 0.8f * rnd.nextFloat());
            g.height = rnd.nextFloat() * 3f;
            g.life = rnd.nextFloat() * 0.8f;
            g.maxLife = 0.4f + rnd.nextFloat() * 0.8f;
            glitches.add(g);
        }
        while (glitches.size() > target) glitches.remove(glitches.size() - 1);

        float sp = speed.getValue() * 0.03f;
        for (Glitch g : glitches) {
            g.life += sp / g.maxLife;
            if (g.life > 1f) {
                g.angle = rnd.nextFloat() * 6.28318f;
                g.dist = radius.getValue() * (0.2f + 0.8f * rnd.nextFloat());
                g.height = rnd.nextFloat() * 3f;
                g.life = 0f;
                g.maxLife = 0.4f + rnd.nextFloat() * 0.8f;
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (glitches.isEmpty() || fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = mc.player.getLerpedPos(event.getTickDelta());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float sz = size.getValue();

        for (Glitch g : glitches) {
            // рваная видимость: квадрат то виден, то нет
            float visible = Math.abs((float) Math.sin(g.life * 30f)) > 0.35f ? 1f : 0f;
            if (visible < 0.5f) continue;

            float fade = (float) Math.sin(g.life * Math.PI);
            float x = (float) (pos.x + Math.cos(g.angle) * g.dist - cam.x);
            float y = (float) (pos.y + g.height - cam.y);
            float z = (float) (pos.z + Math.sin(g.angle) * g.dist - cam.z);

            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = (int) (200 * fade);
            int col = (int) (g.dist * 40f) % 3;
            int r = col == 0 ? 255 : 80;
            int gg = col == 1 ? 255 : 80;
            int b = col == 2 ? 255 : 80;
            buffer.vertex(m, -sz, -sz, 0).color(r, gg, b, alpha);
            buffer.vertex(m, -sz, sz, 0).color(r, gg, b, alpha);
            buffer.vertex(m, sz, sz, 0).color(r, gg, b, alpha);
            buffer.vertex(m, sz, -sz, 0).color(r, gg, b, alpha);
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
