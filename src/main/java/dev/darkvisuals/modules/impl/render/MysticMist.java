package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class MysticMist extends Module {

    private static final long MIST_TTL_MS = 7000L;

    private final NumberSetting density =
            new NumberSetting("Плотность", 14f, 2f, 40f, 1f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 4.0f, 1.5f, 12.0f, 0.5f);
    private final NumberSetting driftSpeed =
            new NumberSetting("Скорость дрейфа", 1.0f, 0.1f, 3.0f, 0.1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 1.6f, 0.4f, 4.0f, 0.2f);
    private final NumberSetting alphaSetting =
            new NumberSetting("Прозрачность", 0.35f, 0.05f, 1.0f, 0.05f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(150, 110, 220, 255).getRGB());

    private final List<Mist> mists = new ArrayList<>();
    private final Random rnd = new Random();

    public MysticMist() {
        super("MysticMist", Category.Render, "Мистический туман, стелющийся у ног игрока");
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(driftSpeed);
        getSettings().add(size);
        getSettings().add(alphaSetting);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        mists.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        // поддерживаем нужное количество облаков тумана
        while (mists.size() < (int) (float) density.getValue()) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = radius.getValue() * (0.2 + 0.8 * rnd.nextDouble());
            mists.add(new Mist(mc.player.getPos().add(
                    Math.cos(ang) * dist,
                    0.05 + rnd.nextDouble() * 0.4,
                    Math.sin(ang) * dist), size.getValue()));
        }
        while (mists.size() > (int) (float) density.getValue()) {
            mists.remove(mists.size() - 1);
        }

        Iterator<Mist> it = mists.iterator();
        while (it.hasNext()) {
            Mist m = it.next();
            m.age++;
            // медленный дрейф по кругу вокруг своей точки
            double t = m.age / 40.0;
            m.pos = m.pos.add(
                    Math.sin(t * 0.2 * driftSpeed.getValue() + m.phase) * 0.02 * driftSpeed.getValue(),
                    Math.sin(t * 0.15 + m.phase * 2.0) * 0.004,
                    Math.cos(t * 0.17 * driftSpeed.getValue() + m.phase * 0.6) * 0.02 * driftSpeed.getValue());

            // дыхание тумана: размер колеблется
            m.currentSize = m.baseSize * (0.8f + 0.2f * MathHelper.sin((float) t * driftSpeed.getValue() + m.phase));

            long age = System.currentTimeMillis() - m.birth;
            if (age >= MIST_TTL_MS || m.pos.distanceTo(mc.player.getPos()) > radius.getValue() * 1.8) {
                // переспавн рядом с игроком
                double ang = rnd.nextDouble() * Math.PI * 2;
                double dist = radius.getValue() * (0.2 + 0.8 * rnd.nextDouble());
                m.pos = mc.player.getPos().add(Math.cos(ang) * dist, 0.05 + rnd.nextDouble() * 0.4, Math.sin(ang) * dist);
                m.birth = System.currentTimeMillis();
                m.age = 0;
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (mists.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color c = color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Mist m : mists) {
            float lifeT = (now - m.birth) / (float) MIST_TTL_MS;
            float fadeIn = MathHelper.clamp(lifeT / 0.2f, 0f, 1f);
            float fadeOut = MathHelper.clamp((1f - lifeT) / 0.25f, 0f, 1f);
            // туман дышит прозрачностью
            float breathe = 0.7f + 0.3f * MathHelper.sin(now / 1400f + m.phase * 2f);
            int alpha = (int) (90 * alphaSetting.getValue() * fadeIn * fadeOut * breathe);
            if (alpha <= 3) continue;

            matrices.push();
            matrices.translate((float) (m.pos.x - cam.x), (float) (m.pos.y - cam.y), (float) (m.pos.z - cam.z));
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            float half = m.currentSize / 2f;
            // два повёрнутых друг к другу квадрата для объёмности
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -half, 0, -half).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, -half, 0, half).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, 0, half).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, 0, -half).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);

            float q = half * 0.7f;
            buffer.vertex(matrix, -q, 0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, 0, 0, q).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, q, 0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, 0, 0, -q).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static final class Mist {
        Vec3d pos;
        final float phase;
        final float baseSize;
        float currentSize;
        long birth = System.currentTimeMillis();
        int age;

        Mist(Vec3d pos, float sizeScale) {
            this.pos = pos;
            Random random = new Random();
            this.phase = random.nextFloat() * (float) (Math.PI * 2);
            this.baseSize = sizeScale * (0.7f + random.nextFloat() * 0.6f);
            this.currentSize = this.baseSize;
        }
    }
}
