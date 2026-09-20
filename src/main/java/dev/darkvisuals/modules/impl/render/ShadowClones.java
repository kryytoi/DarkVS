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
 * ShadowClones — тёмные копии игрока, появляющиеся рядом на короткое время.
 */
public class ShadowClones extends Module {

    private final NumberSetting max = new NumberSetting("Количество клонов", 3f, 1f, 8f, 1f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 3f, 0.1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 2f, 0.5f, 6f, 0.25f);
    private final NumberSetting lifespan = new NumberSetting("Жизнь (сек)", 2f, 0.5f, 6f, 0.25f);

    private static final class Clone {
        double x, y, z;
        long born;
    }

    private final List<Clone> clones = new ArrayList<>();
    private final Random rnd = new Random();
    private long lastSpawn = 0L;

    public ShadowClones() {
        super("ShadowClones", Category.Render, "Тёмные копии игрока рядом");
        getSettings().add(max);
        getSettings().add(speed);
        getSettings().add(radius);
        getSettings().add(lifespan);
    }

    @Override
    public void onDisable() {
        clones.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        long lifeMs = (long) (lifespan.getValue() * 1000L);
        clones.removeIf(c -> now - c.born > lifeMs);

        long interval = (long) (600L / speed.getValue());
        if (clones.size() < max.getValue() && now - lastSpawn > interval) {
            lastSpawn = now;
            Clone c = new Clone();
            float a = rnd.nextFloat() * 6.28318f;
            c.x = mc.player.getX() + Math.cos(a) * radius.getValue();
            c.z = mc.player.getZ() + Math.sin(a) * radius.getValue();
            c.y = mc.player.getY();
            c.born = now;
            clones.add(c);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (clones.isEmpty() || fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        long lifeMs = (long) (lifespan.getValue() * 1000L);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Clone c : clones) {
            float age = (now - c.born) / (float) lifeMs;
            if (age >= 1f) continue;
            float fade = 1f - age;

            matrices.push();
            matrices.translate((float) (c.x - cam.x), (float) (c.y - cam.y), (float) (c.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = (int) (140 * fade);
            float w = 0.3f, h = 1.8f;
            buffer.vertex(m, -w, 0, 0).color(30, 0, 50, alpha);
            buffer.vertex(m, -w, h, 0).color(30, 0, 50, alpha);
            buffer.vertex(m, w, h, 0).color(30, 0, 50, alpha);
            buffer.vertex(m, w, 0, 0).color(30, 0, 50, alpha);
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
