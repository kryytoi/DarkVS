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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * SkyLanterns — небесные фонарики, поднимающиеся в небо.
 * Медленный подъём с лёгким покачиванием и мягкое
                тёплое аддитивное свечение вокруг каждого фонарика.
 */
public class SkyLanterns extends Module {

    private final NumberSetting density =
            new NumberSetting("Плотность", 10f, 2f, 30f, 1f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 16f, 5f, 40f, 1f);
    private final NumberSetting riseSpeed =
            new NumberSetting("Скорость подъёма", 1.0f, 0.2f, 3f, 0.1f);
    private final NumberSetting sway =
            new NumberSetting("Покачивание", 1.0f, 0f, 2f, 0.1f);
    private final NumberSetting glowSize =
            new NumberSetting("Размер свечения", 0.7f, 0.2f, 2.5f, 0.05f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 185, 90, 255).getRGB());

    private final Random rnd = new Random();
    private final List<Lantern> lanterns = new ArrayList<>();

    public SkyLanterns() {
        super("SkyLanterns", Category.Render, "Небесные фонарики, взмывающие в небо");
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(riseSpeed);
        getSettings().add(sway);
        getSettings().add(glowSize);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        lanterns.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        int target = (int) (float) density.getValue();
        float r = radius.getValue();

        while (lanterns.size() < target) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = r * (0.2 + 0.8 * rnd.nextDouble());
            lanterns.add(new Lantern(
                    mc.player.getPos().add(Math.cos(ang) * dist, 0.5 + rnd.nextDouble() * 1.5, Math.sin(ang) * dist),
                    0.025f + rnd.nextFloat() * 0.05f,
                    rnd.nextFloat() * (float) Math.PI * 2,
                    0.85f + rnd.nextFloat() * 0.35f,
                    rnd.nextFloat() * (float) Math.PI * 2,
                    0.9f + rnd.nextFloat() * 0.2f,
                    0.85f + rnd.nextFloat() * 0.3f));
        }
        while (lanterns.size() > target) {
            lanterns.remove(lanterns.size() - 1);
        }

        float speed = riseSpeed.getValue();
        float swayAmount = sway.getValue();

        Iterator<Lantern> it = lanterns.iterator();
        while (it.hasNext()) {
            Lantern l = it.next();
            l.age++;
            // медленный подъём и плавное покачивание
            l.pos = l.pos.add(
                    Math.sin(l.age * l.swaySpeed + l.swayPhase) * 0.015 * swayAmount,
                    0.03 * speed * l.riseScale,
                    Math.cos(l.age * l.swaySpeed * 0.85 + l.swayPhase) * 0.015 * swayAmount);

            if (l.pos.y > mc.player.getY() + 45
                    || l.pos.distanceTo(mc.player.getPos()) > r * 2.4) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (lanterns.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float camYaw = mc.gameRenderer.getCamera().getYaw();
        float camPitch = mc.gameRenderer.getCamera().getPitch();
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Color base = color.getColor();

        for (Lantern l : lanterns) {
            // мягкое дрожание пламени внутри фонарика
            float flicker = 0.78f + 0.22f * (float) Math.sin(now / 550.0 * l.flickerSpeed + l.flickerPhase);
            float glow = glowSize.getValue() * flicker;

            // плавное появление и угасание на большой высоте
            float fade = Math.min(l.age / 14f, 1f);
            float heightFade = Math.max(0f, 1f - (float) ((l.pos.y - mc.player.getY()) / 42f));
            float a = fade * heightFade;
            if (a <= 0.02f) continue;

            // тёплый оттенок с лёгким разбросом по фонарикам
            Color warm = new Color(
                    Math.min(255, base.getRed()),
                    Math.min(255, (int) (base.getGreen() * l.hueShift)),
                    Math.min(255, (int) (base.getBlue() * l.hueShift)));

            matrices.push();
            matrices.translate((float) (l.pos.x - cam.x), (float) (l.pos.y - cam.y), (float) (l.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            // внешнее мягкое свечение
            Color glowColor = new Color(warm.getRed(), warm.getGreen(), warm.getBlue(), (int) (95 * a));
            drawQuad(matrix, glow, glowColor);

            // яркое ядро фонарика
            Color coreColor = new Color(
                    Math.min(255, warm.getRed() + 60),
                    Math.min(255, warm.getGreen() + 50),
                    Math.min(255, warm.getBlue() + 40),
                    (int) (220 * a));
            drawQuad(matrix, glow * 0.42f, coreColor);

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // billboard-квад в локальной плоскости XY
    private void drawQuad(Matrix4f matrix, float half, Color color) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, -half, -half, 0).color(color.getRGB());
        buffer.vertex(matrix, half, -half, 0).color(color.getRGB());
        buffer.vertex(matrix, half, half, 0).color(color.getRGB());
        buffer.vertex(matrix, -half, half, 0).color(color.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static final class Lantern {
        Vec3d pos;
        final float swaySpeed;
        final float swayPhase;
        final float riseScale;
        final float flickerSpeed;
        final float flickerPhase;
        final float hueShift;
        int age;

        Lantern(Vec3d pos, float swaySpeed, float swayPhase, float riseScale,
                float flickerSpeed, float flickerPhase, float hueShift) {
            this.pos = pos;
            this.swaySpeed = swaySpeed;
            this.swayPhase = swayPhase;
            this.riseScale = riseScale;
            this.flickerSpeed = flickerSpeed;
            this.flickerPhase = flickerPhase;
            this.hueShift = hueShift;
        }
    }
}
