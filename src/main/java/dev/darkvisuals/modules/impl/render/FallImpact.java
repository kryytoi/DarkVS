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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * FallImpact — пыльное кольцо и облачка пыли при жёстком приземлении.
 */
public class FallImpact extends Module {

    private static final long RING_TTL_MS = 600L;
    private static final int RING_SEGMENTS = 48;

    private final NumberSetting minFall =
            new NumberSetting("Мин. высота", 2.5f, 0.5f, 12f, 0.5f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 1.1f, 0.4f, 3f, 0.1f);
    private final NumberSetting dustCount =
            new NumberSetting("Частиц пыли", 14f, 0f, 40f, 1f);
    private final BooleanSetting dust = new BooleanSetting("Пыль", true);
    private final ColorSetting ringColor =
            new ColorSetting("Цвет кольца", new Color(200, 170, 130, 255).getRGB());
    private final ColorSetting dustColor =
            new ColorSetting("Цвет пыли", new Color(190, 160, 120, 255).getRGB());

    private static final class ImpactRing {
        final Vec3d pos;
        final long birth;

        ImpactRing(Vec3d pos) {
            this.pos = pos;
            this.birth = System.currentTimeMillis();
        }
    }

    private static final class DustPuff {
        Vec3d pos;
        Vec3d prevPos;
        Vec3d velocity;
        final long birth;
        final float size;
        final float phase;

        DustPuff(Vec3d pos, Vec3d velocity, float size, float phase) {
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.birth = System.currentTimeMillis();
            this.size = size;
            this.phase = phase;
        }
    }

    private final List<ImpactRing> rings = new ArrayList<>();
    private final List<DustPuff> puffs = new ArrayList<>();
    private final Random rnd = new Random();
    private boolean wasOnGround = true;
    private float pendingFall = 0f;

    public FallImpact() {
        super("FallImpact", Category.Render, "Пыльное кольцо при жёстком приземлении");
        getSettings().add(minFall);
        getSettings().add(radius);
        getSettings().add(dustCount);
        getSettings().add(dust);
        getSettings().add(ringColor);
        getSettings().add(dustColor);
    }

    @Override
    public void onDisable() {
        rings.clear();
        puffs.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        rings.removeIf(r -> now - r.birth >= RING_TTL_MS);

        Iterator<DustPuff> it = puffs.iterator();
        while (it.hasNext()) {
            DustPuff puff = it.next();
            if (now - puff.birth >= 900L) {
                it.remove();
                continue;
            }
            puff.prevPos = puff.pos;
            puff.pos = puff.pos.add(puff.velocity);
            // пыль замедляется и оседает
            puff.velocity = puff.velocity.multiply(0.86).add(0.0, -0.012, 0.0);
        }

        boolean onGround = mc.player.isOnGround();
        if (!onGround) {
            pendingFall = mc.player.fallDistance;
        } else if (!wasOnGround) {
            // только что приземлились
            if (pendingFall >= minFall.getValue()) {
                Vec3d pos = mc.player.getPos();
                rings.add(new ImpactRing(pos));

                if (dust.getValue()) {
                    int count = (int) (float) dustCount.getValue();
                    for (int i = 0; i < count; i++) {
                        double ang = rnd.nextDouble() * Math.PI * 2;
                        double sp = 0.04 + rnd.nextDouble() * 0.08;
                        puffs.add(new DustPuff(
                                pos.add(Math.cos(ang) * 0.3, 0.1, Math.sin(ang) * 0.3),
                                new Vec3d(Math.cos(ang) * sp, 0.05 + rnd.nextDouble() * 0.06, Math.sin(ang) * sp),
                                0.08f + rnd.nextFloat() * 0.12f,
                                rnd.nextFloat() * (float) (Math.PI * 2)));
                    }
                }
            }
            pendingFall = 0f;
        }
        wasOnGround = onGround;
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (rings.isEmpty() && puffs.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color rc = ringColor.getColor();
        Color dc = dustColor.getColor();
        float maxRadius = radius.getValue();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // расширяющиеся кольца на земле
        for (ImpactRing ring : rings) {
            float t = (now - ring.birth) / (float) RING_TTL_MS;
            float grow = (float) dev.darkvisuals.client.util.animations.Easing.EASE_OUT_CIRC.apply(t);
            float ringR = maxRadius * grow;
            float alpha = 170f * (1f - t);
            if (alpha <= 3f || ringR <= 0.01f) continue;

            int segAlpha = (int) alpha;
            matrices.push();
            matrices.translate((float) (ring.pos.x - cam.x), (float) (ring.pos.y - cam.y + 0.05), (float) (ring.pos.z - cam.z));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float thickness = 0.05f + 0.05f * (1f - t);
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double a0 = (i / (double) RING_SEGMENTS) * Math.PI * 2;
                double a1 = ((i + 1) / (double) RING_SEGMENTS) * Math.PI * 2;
                float x0 = (float) Math.cos(a0), z0 = (float) Math.sin(a0);
                float x1 = (float) Math.cos(a1), z1 = (float) Math.sin(a1);
                buffer.vertex(matrix, x0 * (ringR - thickness), 0f, z0 * (ringR - thickness)).color(rc.getRed(), rc.getGreen(), rc.getBlue(), segAlpha / 2);
                buffer.vertex(matrix, x0 * (ringR + thickness), 0f, z0 * (ringR + thickness)).color(rc.getRed(), rc.getGreen(), rc.getBlue(), segAlpha);
                buffer.vertex(matrix, x1 * (ringR + thickness), 0f, z1 * (ringR + thickness)).color(rc.getRed(), rc.getGreen(), rc.getBlue(), segAlpha);
                buffer.vertex(matrix, x1 * (ringR - thickness), 0f, z1 * (ringR - thickness)).color(rc.getRed(), rc.getGreen(), rc.getBlue(), segAlpha / 2);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        // облачка пыли
        for (DustPuff puff : puffs) {
            float lifeT = (now - puff.birth) / 900f;
            float fadeIn = MathHelper.clamp(lifeT / 0.15f, 0f, 1f);
            float fadeOut = MathHelper.clamp((1f - lifeT) / 0.4f, 0f, 1f);
            float wobble = 1f + 0.25f * MathHelper.sin(now / 120f + puff.phase);
            int alpha = (int) (120 * fadeIn * fadeOut);
            if (alpha <= 3) continue;

            matrices.push();
            matrices.translate((float) (puff.pos.x - cam.x), (float) (puff.pos.y - cam.y), (float) (puff.pos.z - cam.z));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = puff.size * wobble * (1f + lifeT * 0.8f);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -half, -half, 0).color(dc.getRed(), dc.getGreen(), dc.getBlue(), 0);
            buffer.vertex(matrix, -half, half, 0).color(dc.getRed(), dc.getGreen(), dc.getBlue(), alpha / 2);
            buffer.vertex(matrix, half, half, 0).color(dc.getRed(), dc.getGreen(), dc.getBlue(), alpha);
            buffer.vertex(matrix, half, -half, 0).color(dc.getRed(), dc.getGreen(), dc.getBlue(), alpha / 2);
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
