package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
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
 * Fireworks — праздничный фейерверк вокруг игрока.
 * Ракета взмывает в небо и на случайной высоте разрывается
                шарам искр, которые медленно гаснут и падают.
 */
public class Fireworks extends Module {

    // праздничная палитра красок
    private static final Color[] PALETTE = {
            new Color(255, 80, 80),
            new Color(255, 160, 60),
            new Color(255, 230, 90),
            new Color(90, 255, 120),
            new Color(90, 180, 255),
            new Color(180, 100, 255),
            new Color(255, 120, 200)
    };

    private final NumberSetting frequency =
            new NumberSetting("Частота (сек)", 3f, 1f, 15f, 1f);
    private final NumberSetting explosionSize =
            new NumberSetting("Размер взрыва", 4f, 1f, 12f, 0.5f);
    private final NumberSetting gravity =
            new NumberSetting("Гравитация", 1.0f, 0f, 2f, 0.1f);
    private final BooleanSetting randomColors =
            new BooleanSetting("Случайные цвета", true);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 200, 120, 255).getRGB());

    private final Random rnd = new Random();
    private final List<Rocket> rockets = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private long nextSpawnMs = 0;

    public Fireworks() {
        super("Fireworks", Category.Render, "Праздничный фейерверк над головой игрока");
        getSettings().add(frequency);
        getSettings().add(explosionSize);
        getSettings().add(gravity);
        getSettings().add(randomColors);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        rockets.clear();
        sparks.clear();
        nextSpawnMs = 0;
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        if (now >= nextSpawnMs) {
            nextSpawnMs = now + (long) (frequency.getValue() * 1000 * (0.6 + rnd.nextDouble() * 0.8));
            spawnRocket();
        }

        // ракеты летят вверх и взрываются на нужной высоте
        Iterator<Rocket> rit = rockets.iterator();
        while (rit.hasNext()) {
            Rocket r = rit.next();
            r.age++;
            r.pos = r.pos.add(r.vel);
            // лёгкое покачивание ракеты в полёте
            r.pos = r.pos.add(
                    Math.sin(r.age * 0.3 + r.swayPhase) * 0.008,
                    0,
                    Math.cos(r.age * 0.27 + r.swayPhase) * 0.008);

            if (r.pos.y >= r.targetY || r.age > 220) {
                explode(r);
                rit.remove();
            }
        }

        // искры разлетающиеся и тающие
        Iterator<Spark> sit = sparks.iterator();
        while (sit.hasNext()) {
            Spark s = sit.next();
            s.age++;
            s.pos = s.pos.add(s.vel);
            s.vel = s.vel.add(0, -0.006 * gravity.getValue(), 0);
            s.vel = s.vel.multiply(0.97);
            if (s.age >= s.maxAge) sit.remove();
        }
    }

    private void spawnRocket() {
        Vec3d base = mc.player.getPos();
        double ang = rnd.nextDouble() * Math.PI * 2;
        double dist = 2 + rnd.nextDouble() * 7;

        Vec3d pos = base.add(Math.cos(ang) * dist, 1 + rnd.nextDouble() * 2, Math.sin(ang) * dist);
        double maxY = mc.world.getHeight() - 4;
        double targetY = Math.min(pos.y + 8 + rnd.nextDouble() * 10, maxY);

        Vec3d vel = new Vec3d(
                (rnd.nextDouble() - 0.5) * 0.02,
                0.32 + rnd.nextDouble() * 0.08,
                (rnd.nextDouble() - 0.5) * 0.02);

        rockets.add(new Rocket(pos, vel, targetY, pickColor(), (float) (rnd.nextDouble() * Math.PI * 2)));
    }

    private void explode(Rocket rocket) {
        float size = explosionSize.getValue();
        int count = (int) (30 + size * 8);

        for (int i = 0; i < count; i++) {
            // равномерное распределение точек по сфере
            double u = rnd.nextDouble();
            double v = rnd.nextDouble();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            double speed = (0.12 + rnd.nextDouble() * 0.1) * size;

            Vec3d dir = new Vec3d(
                    Math.sin(phi) * Math.cos(theta),
                    Math.cos(phi),
                    Math.sin(phi) * Math.sin(theta));

            sparks.add(new Spark(rocket.pos, dir.multiply(speed), rocket.color,
                    (int) (35 + size * 5 + rnd.nextInt(15))));
        }
    }

    private Color pickColor() {
        if (randomColors.getValue()) return PALETTE[rnd.nextInt(PALETTE.length)];
        return color.getColor();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (rockets.isEmpty() && sparks.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float camYaw = mc.gameRenderer.getCamera().getYaw();
        float camPitch = mc.gameRenderer.getCamera().getPitch();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // искры взрывов
        for (Spark s : sparks) {
            float life = 1f - (float) s.age / s.maxAge;
            // мерцание угасающих искр
            float twinkle = 0.65f + 0.35f * (float) Math.sin(s.age * 0.9 + s.twinklePhase);
            int alpha = (int) (235 * life * twinkle);
            if (alpha <= 4) continue;

            float half = 0.085f * (0.45f + 0.55f * life);
            Color c = new Color(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), alpha);

            matrices.push();
            matrices.translate((float) (s.pos.x - cam.x), (float) (s.pos.y - cam.y), (float) (s.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));
            drawQuad(matrices.peek().getPositionMatrix(), half, c);
            matrices.pop();
        }

        // ракеты со светящимся хвостом
        for (Rocket r : rockets) {
            Vec3d trail = r.vel.normalize().multiply(0.22);
            for (int i = 3; i >= 0; i--) {
                Vec3d pos = i == 0 ? r.pos : r.pos.subtract(trail.multiply(i));
                float f = 1f - i * 0.22f;
                int alpha = (int) (60 + 160 * f);
                Color c = new Color(
                        Math.min(255, r.color.getRed() + 60),
                        Math.min(255, r.color.getGreen() + 60),
                        Math.min(255, r.color.getBlue() + 60), alpha);

                matrices.push();
                matrices.translate((float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));
                drawQuad(matrices.peek().getPositionMatrix(), 0.075f * f, c);
                matrices.pop();
            }
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

    private static final class Rocket {
        Vec3d pos;
        final Vec3d vel;
        final double targetY;
        final Color color;
        final float swayPhase;
        int age;

        Rocket(Vec3d pos, Vec3d vel, double targetY, Color color, float swayPhase) {
            this.pos = pos;
            this.vel = vel;
            this.targetY = targetY;
            this.color = color;
            this.swayPhase = swayPhase;
        }
    }

    private static final class Spark {
        Vec3d pos;
        Vec3d vel;
        final Color color;
        final float twinklePhase;
        final int maxAge;
        int age;

        Spark(Vec3d pos, Vec3d vel, Color color, int maxAge) {
            this.pos = pos;
            this.vel = vel;
            this.color = color;
            this.maxAge = maxAge;
            this.twinklePhase = (float) (Math.random() * Math.PI * 2);
        }
    }
}
