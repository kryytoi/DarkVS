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
 * DamageFX — красная вспышка и искры при получении урона.
 */
public class DamageFX extends Module {

    private static final long SPARK_TTL_MS = 600L;
    private static final long RING_TTL_MS = 450L;
    private static final int RING_SEGMENTS = 40;

    private final NumberSetting sparkCount =
            new NumberSetting("Искр", 18f, 4f, 60f, 1f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 0.7f, 0.3f, 2f, 0.1f);
    private final BooleanSetting ring = new BooleanSetting("Кольцо", true);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 60, 60, 255).getRGB());

    private static final class Spark {
        Vec3d pos;
        final Vec3d velocity;
        final long birth;
        final float size;

        Spark(Vec3d pos, Vec3d velocity, float size) {
            this.pos = pos;
            this.velocity = velocity;
            this.birth = System.currentTimeMillis();
            this.size = size;
        }
    }

    private static final class ImpactRing {
        final Vec3d pos;
        final long birth;

        ImpactRing(Vec3d pos) {
            this.pos = pos;
            this.birth = System.currentTimeMillis();
        }
    }

    private final List<Spark> sparks = new ArrayList<>();
    private final List<ImpactRing> rings = new ArrayList<>();
    private final Random rnd = new Random();
    private float lastHealth = -1f;

    public DamageFX() {
        super("DamageFX", Category.Render, "Искры при получении урона");
        getSettings().add(sparkCount);
        getSettings().add(radius);
        getSettings().add(ring);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        sparks.clear();
        rings.clear();
        lastHealth = -1f;
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        Iterator<Spark> it = sparks.iterator();
        while (it.hasNext()) {
            Spark spark = it.next();
            if (now - spark.birth >= SPARK_TTL_MS) {
                it.remove();
                continue;
            }
            spark.pos = spark.pos.add(spark.velocity);
        }
        rings.removeIf(r -> now - r.birth >= RING_TTL_MS);

        float health = mc.player.getHealth();
        if (lastHealth > 0f && health < lastHealth - 0.01f) {
            Vec3d pos = mc.player.getPos().add(0, 1, 0);

            int count = (int) (float) sparkCount.getValue();
            for (int i = 0; i < count; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                double sp = 0.05 + rnd.nextDouble() * 0.1;
                double up = 0.02 + rnd.nextDouble() * 0.08;
                sparks.add(new Spark(
                        pos,
                        new Vec3d(Math.cos(ang) * sp, up, Math.sin(ang) * sp),
                        0.5f + rnd.nextFloat()));
            }
            if (ring.getValue()) {
                rings.add(new ImpactRing(mc.player.getPos()));
            }
        }
        lastHealth = health;
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (sparks.isEmpty() && rings.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color c = color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (ImpactRing impact : rings) {
            float t = (now - impact.birth) / (float) RING_TTL_MS;
            float grow = (float) dev.darkvisuals.client.util.animations.Easing.EASE_OUT_CIRC.apply(t);
            float ringR = radius.getValue() * grow;
            int alpha = (int) (160 * (1f - t));
            if (alpha <= 3 || ringR <= 0.01f) continue;

            matrices.push();
            matrices.translate((float) (impact.pos.x - cam.x), (float) (impact.pos.y - cam.y + 1), (float) (impact.pos.z - cam.z));
            // вертикальное кольцо, повёрнутое к камере по горизонтали
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float thickness = 0.035f + 0.04f * (1f - t);
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double a0 = (i / (double) RING_SEGMENTS) * Math.PI * 2;
                double a1 = ((i + 1) / (double) RING_SEGMENTS) * Math.PI * 2;
                float x0 = (float) Math.cos(a0), y0 = (float) Math.sin(a0);
                float x1 = (float) Math.cos(a1), y1 = (float) Math.sin(a1);
                buffer.vertex(matrix, x0 * (ringR - thickness), y0 * (ringR - thickness), 0f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
                buffer.vertex(matrix, x0 * (ringR + thickness), y0 * (ringR + thickness), 0f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, x1 * (ringR + thickness), y1 * (ringR + thickness), 0f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, x1 * (ringR - thickness), y1 * (ringR - thickness), 0f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        for (Spark spark : sparks) {
            float lifeT = (now - spark.birth) / (float) SPARK_TTL_MS;
            float fadeIn = MathHelper.clamp(lifeT / 0.08f, 0f, 1f);
            float fadeOut = MathHelper.clamp((1f - lifeT) / 0.4f, 0f, 1f);
            int alpha = (int) (210 * fadeIn * fadeOut);
            if (alpha <= 3) continue;

            matrices.push();
            matrices.translate((float) (spark.pos.x - cam.x), (float) (spark.pos.y - cam.y), (float) (spark.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = 0.05f * spark.size;
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
}
