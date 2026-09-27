package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventAttackEntity;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class CriticalHitVFX extends Module {

    private static final int RUNE_SEGMENTS = 3;
    private static final long SPARK_TTL = 700L;
    private static final long RING_TTL  = 500L;

    private final NumberSetting sparkCount  = new NumberSetting("Искр",        16f, 4f,  60f, 2f);
    private final NumberSetting ringRadius  = new NumberSetting("Радиус",       1.2f, 0.3f, 3f, 0.1f);
    private final NumberSetting runeCount   = new NumberSetting("Рун",          5f,  2f,  12f, 1f);
    private final BooleanSetting onlyCreative = new BooleanSetting("Криты моба", false);
    private final ColorSetting colorSetting = new ColorSetting("Цвет", new Color(200, 120, 255, 255).getRGB());
    private final BooleanSetting rainbow    = new BooleanSetting("Радуга", false);

    private static final class CritSpark {
        Vec3d pos;
        final Vec3d vel;
        final long born;
        final float size;
        CritSpark(Vec3d pos, Vec3d vel, float size) {
            this.pos = pos; this.vel = vel; this.born = System.currentTimeMillis(); this.size = size;
        }
    }

    private static final class CritRing {
        final Vec3d pos;
        final long born;
        final float baseAngle;
        CritRing(Vec3d pos, float baseAngle) {
            this.pos = pos; this.born = System.currentTimeMillis(); this.baseAngle = baseAngle;
        }
    }

    private final List<CritSpark> sparks = new ArrayList<>();
    private final List<CritRing>  rings  = new ArrayList<>();
    private final Random rnd = new Random();

    public CriticalHitVFX() {
        super("CriticalHitVFX", Category.Render, "Взрыв рун и молний при критическом ударе");
        getSettings().add(sparkCount);
        getSettings().add(ringRadius);
        getSettings().add(runeCount);
        getSettings().add(onlyCreative);
        getSettings().add(colorSetting);
        getSettings().add(rainbow);
    }

    @Override
    public void onDisable() {
        sparks.clear();
        rings.clear();
        super.onDisable();
    }

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (fullNullCheck()) return;
        if (e.getPlayer() != mc.player) return;
        if (!(e.getTarget() instanceof LivingEntity)) return;
        if (!e.canProcess()) return;

        boolean isCrit = mc.player.fallDistance > 0f && !mc.player.isOnGround()
                && !mc.player.isClimbing() && !mc.player.isTouchingWater();
        if (!isCrit && !onlyCreative.getValue()) return;

        Vec3d target = e.getTarget().getPos().add(0, e.getTarget().getHeight() * 0.6f, 0);

        int numRunes = (int)(float) runeCount.getValue();
        for (int i = 0; i < numRunes; i++) {
            rings.add(new CritRing(target, (float) (i * Math.PI * 2 / numRunes)));
        }

        int cnt = (int)(float) sparkCount.getValue();
        for (int i = 0; i < cnt; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double pitch = (rnd.nextDouble() - 0.3) * Math.PI;
            double spd = 0.06 + rnd.nextDouble() * 0.12;
            sparks.add(new CritSpark(
                    target,
                    new Vec3d(Math.cos(ang) * Math.cos(pitch) * spd,
                              Math.sin(pitch) * spd + 0.04,
                              Math.sin(ang) * Math.cos(pitch) * spd),
                    0.4f + rnd.nextFloat() * 0.8f));
        }
    }

    @EventHandler
    public void onTick(EventTick e) {
        long now = System.currentTimeMillis();
        rings.removeIf(r -> now - r.born >= RING_TTL);
        Iterator<CritSpark> it = sparks.iterator();
        while (it.hasNext()) {
            CritSpark s = it.next();
            if (now - s.born >= SPARK_TTL) { it.remove(); continue; }
            s.pos = s.pos.add(s.vel);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (sparks.isEmpty() && rings.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (CritRing ring : rings) {
            float t = (now - ring.born) / (float) RING_TTL;
            float grow  = (float) Math.pow(Math.min(1f, t / 0.25f), 0.5f);
            float fade  = 1f - t;
            int alpha = (int) (200 * fade);
            if (alpha <= 3) continue;

            Color c = getColor(ring.baseAngle);
            float r = ringRadius.getValue() * grow;
            float spin = ring.baseAngle + t * (float) Math.PI * 4f;

            matrices.push();
            matrices.translate(
                    (float)(ring.pos.x - cam.x),
                    (float)(ring.pos.y - cam.y),
                    (float)(ring.pos.z - cam.z));

            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) Math.toDegrees(spin)));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buf.vertex(m, 0f, 0f, 0f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            for (int i = 0; i <= RUNE_SEGMENTS; i++) {
                float ang = (float)(i * Math.PI * 2 / RUNE_SEGMENTS);
                buf.vertex(m, MathHelper.cos(ang) * r, 0f, MathHelper.sin(ang) * r)
                   .color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
            matrices.pop();
        }

        for (CritSpark s : sparks) {
            float lt = (now - s.born) / (float) SPARK_TTL;
            float fi = MathHelper.clamp(lt / 0.1f, 0f, 1f);
            float fo = MathHelper.clamp((1f - lt) / 0.4f, 0f, 1f);
            int alpha = (int)(220 * fi * fo);
            if (alpha <= 3) continue;

            Color c = getColor(s.vel.x);

            matrices.push();
            matrices.translate((float)(s.pos.x-cam.x),(float)(s.pos.y-cam.y),(float)(s.pos.z-cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            Matrix4f m = matrices.peek().getPositionMatrix();
            float h = 0.04f * s.size;
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buf.vertex(m,-h,-h,0).color(c.getRed(),c.getGreen(),c.getBlue(),0);
            buf.vertex(m,-h, h,0).color(c.getRed(),c.getGreen(),c.getBlue(),alpha/2);
            buf.vertex(m, h, h,0).color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
            buf.vertex(m, h,-h,0).color(c.getRed(),c.getGreen(),c.getBlue(),alpha/2);
            BufferRenderer.drawWithGlobalProgram(buf.end());
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private Color getColor(double seed) {
        if (rainbow.getValue()) {
            float hue = (float)(((System.currentTimeMillis() / 6000.0) + seed * 0.1) % 1.0);
            return new Color(Color.HSBtoRGB(hue, 0.7f, 1f));
        }
        return colorSetting.getColor();
    }
}
