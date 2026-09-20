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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * StarShower — метеорный дождь вокруг игрока.
 */
public class StarShower extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 20f, 4f, 80f, 2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 16f, 5f, 40f, 1f);
    private final NumberSetting height = new NumberSetting("Высота", 20f, 5f, 60f, 1f);

    private static final class Star {
        float angle, dist, fall, drift;
    }

    private final Deque<Star> stars = new ArrayDeque<>();

    public StarShower() {
        super("StarShower", Category.Render, "Метеорный дождь вокруг игрока");
        getSettings().add(density);
        getSettings().add(speed);
        getSettings().add(radius);
        getSettings().add(height);
    }

    @Override
    public void onDisable() {
        stars.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        int target = (int) (float) density.getValue();
        while (stars.size() < target) {
            Star s = new Star();
            s.angle = (float) (Math.random() * Math.PI * 2);
            s.dist = (float) (radius.getValue() * (0.4 + 0.6 * Math.random()));
            s.fall = (float) Math.random();
            s.drift = (float) (Math.random() - 0.5f) * 0.5f;
            stars.addLast(s);
        }
        while (stars.size() > target) stars.pollFirst();

        float sp = speed.getValue() * 0.01f;
        for (Star s : stars) {
            s.fall -= sp;
            s.dist += s.drift * sp;
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (stars.isEmpty() || fullNullCheck()) return;

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

        for (Star s : stars) {
            if (s.fall < 0f) s.fall = 1f;
            float x = (float) (pos.x + Math.cos(s.angle) * s.dist - cam.x);
            float z = (float) (pos.z + Math.sin(s.angle) * s.dist - cam.z);
            float y = (float) (pos.y + h * s.fall - cam.y);

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float len = 0.6f * speed.getValue();
            int alpha = 200;
            float w = 0.04f;
            buffer.vertex(m, x - w, y, z).color(255, 240, 200, alpha);
            buffer.vertex(m, x + w, y, z).color(255, 240, 200, alpha);
            buffer.vertex(m, x + w + s.drift, y + len, z).color(255, 240, 200, alpha);
            buffer.vertex(m, x - w + s.drift, y + len, z).color(255, 240, 200, alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
