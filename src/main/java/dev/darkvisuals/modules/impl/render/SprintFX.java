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
 * SprintFX — скоростные линии-полосы вокруг игрока при спринте.
 * Полосы вытянуты вдоль направления движения и быстро гаснут.
 */
public class SprintFX extends Module {

    private static final long STREAK_TTL_MS = 320L;

    private final NumberSetting density =
            new NumberSetting("Плотность", 14f, 2f, 40f, 1f);
    private final NumberSetting length =
            new NumberSetting("Длина", 1.4f, 0.4f, 4f, 0.1f);
    private final NumberSetting width =
            new NumberSetting("Ширина", 0.05f, 0.01f, 0.2f, 0.01f);
    private final NumberSetting spread =
            new NumberSetting("Разброс", 2.2f, 0.5f, 6f, 0.1f);
    private final BooleanSetting rainbow = new BooleanSetting("Радуга", false);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(160, 200, 255, 255).getRGB());

    private static final class Streak {
        Vec3d pos;
        final Vec3d dir; // нормализованное направление движения
        final long birth;
        final float sizeMul;

        Streak(Vec3d pos, Vec3d dir, float sizeMul) {
            this.pos = pos;
            this.dir = dir;
            this.birth = System.currentTimeMillis();
            this.sizeMul = sizeMul;
        }
    }

    private final List<Streak> streaks = new ArrayList<>();
    private final Random rnd = new Random();

    public SprintFX() {
        super("SprintFX", Category.Render, "Скоростные линии при спринте");
        getSettings().add(density);
        getSettings().add(length);
        getSettings().add(width);
        getSettings().add(spread);
        getSettings().add(rainbow);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        streaks.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        Iterator<Streak> it = streaks.iterator();
        while (it.hasNext()) {
            Streak s = it.next();
            s.pos = s.pos.add(s.dir.multiply(0.18));
            if (now - s.birth >= STREAK_TTL_MS) it.remove();
        }

        if (!mc.player.isSprinting()) return;

        Vec3d vel = mc.player.getVelocity();
        Vec3d flat = new Vec3d(vel.x, 0, vel.z);
        if (flat.lengthSquared() < 0.02) return;
        Vec3d dir = flat.normalize();

        int target = (int) (float) density.getValue();
        int spawn = Math.max(1, target / 3);
        for (int i = 0; i < spawn && streaks.size() < target; i++) {
            // полосы появляются вокруг игрока и позади него
            double behind = 0.6 + rnd.nextDouble() * spread.getValue() * 0.5;
            double offSide = (rnd.nextDouble() - 0.5) * spread.getValue();
            double offY = (rnd.nextDouble() - 0.35) * 1.6;
            Vec3d perpendicular = new Vec3d(-dir.z, 0, dir.x);
            Vec3d pos = mc.player.getPos()
                    .subtract(dir.multiply(behind))
                    .add(perpendicular.multiply(offSide))
                    .add(0, offY, 0);
            streaks.add(new Streak(pos, dir, 0.6f + rnd.nextFloat() * 0.8f));
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (streaks.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color base = color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Streak s : streaks) {
            float lifeT = (now - s.birth) / (float) STREAK_TTL_MS;
            float fadeIn = MathHelper.clamp(lifeT / 0.12f, 0f, 1f);
            float fadeOut = 1f - lifeT;
            int alpha = (int) (150 * fadeIn * fadeOut * fadeOut);
            if (alpha <= 3) continue;

            Color c = base;
            if (rainbow.getValue()) {
                float hue = (now / 4000f + s.birth % 1000f / 1000f) % 1f;
                c = new Color(Color.HSBtoRGB(hue, 0.7f, 1f));
            }

            float len = length.getValue() * s.sizeMul;
            float wid = width.getValue() * s.sizeMul;

            Vec3d d = s.dir;
            Vec3d p = new Vec3d(-d.z, 0, d.x);
            Vec3d center = s.pos.subtract(cam);

            Vec3d a = center.subtract(d.multiply(len / 2)).subtract(p.multiply(wid / 2));
            Vec3d b = center.subtract(d.multiply(len / 2)).add(p.multiply(wid / 2));
            Vec3d c2 = center.add(d.multiply(len / 2)).add(p.multiply(wid / 2));
            Vec3d e = center.add(d.multiply(len / 2)).subtract(p.multiply(wid / 2));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            buffer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, (float) c2.x, (float) c2.y, (float) c2.z).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, (float) e.x, (float) e.y, (float) e.z).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
