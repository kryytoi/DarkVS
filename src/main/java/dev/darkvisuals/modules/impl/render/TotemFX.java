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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * TotemFX — золотой взрыв при срабатывании тотема: кольца, гейзеры искр и вспышка.
 */
public class TotemFX extends Module {

    private static final long RING_TTL_MS = 700L;
    private static final long SPARK_TTL_MS = 1100L;
    private static final int RING_SEGMENTS = 64;

    private final NumberSetting radius =
            new NumberSetting("Радиус", 1.4f, 0.5f, 4f, 0.1f);
    private final NumberSetting sparkCount =
            new NumberSetting("Искр", 40f, 5f, 120f, 5f);
    private final BooleanSetting showRings = new BooleanSetting("Кольца", true);
    private final BooleanSetting rainbow = new BooleanSetting("Радуга", false);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 215, 90, 255).getRGB());

    private static final class Ring {
        final Vec3d pos;
        final long birth;
        final float delay;
        final boolean vertical;

        Ring(Vec3d pos, float delay, boolean vertical) {
            this.pos = pos;
            this.birth = System.currentTimeMillis();
            this.delay = delay;
            this.vertical = vertical;
        }
    }

    private static final class Spark {
        Vec3d pos;
        final Vec3d velocity;
        final long birth;
        final float size;
        final float phase;

        Spark(Vec3d pos, Vec3d velocity, float size, float phase) {
            this.pos = pos;
            this.velocity = velocity;
            this.birth = System.currentTimeMillis();
            this.size = size;
            this.phase = phase;
        }
    }

    private final List<Ring> rings = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final Random rnd = new Random();

    public TotemFX() {
        super("TotemFX", Category.Render, "Золотой взрыв при срабатывании тотема");
        getSettings().add(radius);
        getSettings().add(sparkCount);
        getSettings().add(showRings);
        getSettings().add(rainbow);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        rings.clear();
        sparks.clear();
        super.onDisable();
    }

    @EventHandler
    public void onPacketReceive(dev.darkvisuals.client.events.impl.EventPacket.Receive e) {
        if (mc.player == null || mc.world == null) return;
        if (!(e.getPacket() instanceof net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket packet)) return;
        // статус 35 — срабатывание тотема бессмертия
        if (packet.getStatus() != 35) return;

        mc.execute(() -> {
            if (mc.player == null || mc.world == null) return;
            net.minecraft.entity.Entity entity = packet.getEntity(mc.world);
            if (entity == null) return;
            if (entity.getPos().distanceTo(mc.player.getPos()) > 48) return;

            Vec3d pos = entity.getPos();

            if (showRings.getValue()) {
                rings.add(new Ring(pos, 0f, false));
                rings.add(new Ring(pos, 0.25f, false));
                rings.add(new Ring(pos, 0.45f, true));
            }

            int count = (int) (float) sparkCount.getValue();
            for (int i = 0; i < count; i++) {
                // сферический разлёт с упором вверх — как настоящий тотем
                double theta = rnd.nextDouble() * Math.PI * 2;
                double phi = Math.acos(1 - rnd.nextDouble() * 1.4);
                double sp = 0.08 + rnd.nextDouble() * 0.14;
                sparks.add(new Spark(
                        pos.add(0, 1, 0),
                        new Vec3d(
                                Math.sin(phi) * Math.cos(theta) * sp,
                                Math.abs(Math.cos(phi)) * sp * 1.3 + 0.02,
                                Math.sin(phi) * Math.sin(theta) * sp),
                        0.5f + rnd.nextFloat(),
                        rnd.nextFloat() * (float) (Math.PI * 2)));
            }
        });
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.world == null) return;

        long now = System.currentTimeMillis();
        rings.removeIf(r -> now - r.birth >= RING_TTL_MS + (long) (r.delay * 1000));

        Iterator<Spark> it = sparks.iterator();
        while (it.hasNext()) {
            Spark spark = it.next();
            if (now - spark.birth >= SPARK_TTL_MS) {
                it.remove();
                continue;
            }
            spark.pos = spark.pos.add(spark.velocity);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (rings.isEmpty() && sparks.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Ring ring : rings) {
            long elapsed = now - ring.birth - (long) (ring.delay * 1000);
            if (elapsed < 0) continue;
            float t = elapsed / (float) RING_TTL_MS;
            if (t >= 1f) continue;

            float grow = (float) dev.darkvisuals.client.util.animations.Easing.EASE_OUT_CIRC.apply(t);
            float ringR = radius.getValue() * grow;
            int alpha = (int) (200 * (1f - t));
            if (alpha <= 3 || ringR <= 0.01f) continue;

            Color c = color(ring.pos.x + ring.delay);

            matrices.push();
            matrices.translate((float) (ring.pos.x - cam.x), (float) (ring.pos.y - cam.y + 1), (float) (ring.pos.z - cam.z));
            if (ring.vertical) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
            }

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float thickness = 0.04f + 0.05f * (1f - t);
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double a0 = (i / (double) RING_SEGMENTS) * Math.PI * 2;
                double a1 = ((i + 1) / (double) RING_SEGMENTS) * Math.PI * 2;
                float x0 = (float) Math.cos(a0), z0 = (float) Math.sin(a0);
                float x1 = (float) Math.cos(a1), z1 = (float) Math.sin(a1);
                buffer.vertex(matrix, x0 * (ringR - thickness), z0 * (ringR - thickness), 0f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
                buffer.vertex(matrix, x0 * (ringR + thickness), z0 * (ringR + thickness), 0f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, x1 * (ringR + thickness), z1 * (ringR + thickness), 0f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, x1 * (ringR - thickness), z1 * (ringR - thickness), 0f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        for (Spark spark : sparks) {
            float lifeT = (now - spark.birth) / (float) SPARK_TTL_MS;
            float fadeIn = MathHelper.clamp(lifeT / 0.08f, 0f, 1f);
            float fadeOut = MathHelper.clamp((1f - lifeT) / 0.35f, 0f, 1f);
            float twinkle = 0.7f + 0.3f * MathHelper.sin(now / 80f + spark.phase * 6f);
            int alpha = (int) (220 * fadeIn * fadeOut * twinkle);
            if (alpha <= 3) continue;

            Color c = color(spark.phase);

            matrices.push();
            matrices.translate((float) (spark.pos.x - cam.x), (float) (spark.pos.y - cam.y), (float) (spark.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = 0.045f * spark.size;
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -half, -half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            buffer.vertex(matrix, -half, half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
            buffer.vertex(matrix, half, half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, -half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private Color color(double seed) {
        if (rainbow.getValue()) {
            float hue = (float) (((System.currentTimeMillis() / 8000.0) + seed * 0.13) % 1.0);
            return new Color(Color.HSBtoRGB(hue, 0.65f, 1f));
        }
        return color.getColor();
    }
}
