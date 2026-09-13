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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * ElytraWind — линии встречного ветра при быстром полёте на элитрах.
 * Полосы вытянуты вдоль скорости и проносятся мимо игрока.
 */
public class ElytraWind extends Module {

    private static final long STREAK_TTL_MS = 380L;

    private final NumberSetting density =
            new NumberSetting("Плотность", 18f, 2f, 50f, 1f);
    private final NumberSetting minSpeed =
            new NumberSetting("Мин. скорость", 0.7f, 0.2f, 3f, 0.05f);
    private final NumberSetting length =
            new NumberSetting("Длина", 2.2f, 0.5f, 6f, 0.1f);
    private final NumberSetting width =
            new NumberSetting("Ширина", 0.045f, 0.01f, 0.2f, 0.01f);
    private final NumberSetting spread =
            new NumberSetting("Разброс", 3.5f, 0.5f, 8f, 0.1f);
    private final BooleanSetting rainbow = new BooleanSetting("Радуга", false);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(200, 230, 255, 255).getRGB());

    private static final class Streak {
        Vec3d pos;
        final Vec3d dir;
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

    public ElytraWind() {
        super("ElytraWind", Category.Render, "Линии ветра при полёте на элитрах");
        getSettings().add(density);
        getSettings().add(minSpeed);
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
            s.pos = s.pos.add(s.dir.multiply(0.25));
            if (now - s.birth >= STREAK_TTL_MS) it.remove();
        }

        if (!mc.player.isGliding()) return;

        Vec3d vel = mc.player.getVelocity();
        if (vel.length() < minSpeed.getValue()) return;

        Vec3d dir = vel.normalize();
        Vec3d perpendicular = new Vec3d(-dir.z, 0, dir.x);

        int target = (int) (float) density.getValue();
        int spawn = Math.max(1, target / 3);
        for (int i = 0; i < spawn && streaks.size() < target; i++) {
            double ahead = rnd.nextDouble() * spread.getValue();
            double offSide = (rnd.nextDouble() - 0.5) * spread.getValue() * 1.5;
            double offY = (rnd.nextDouble() - 0.5) * spread.getValue();
            Vec3d pos = mc.player.getPos()
                    .add(dir.multiply(ahead))
                    .add(perpendicular.multiply(offSide))
                    .add(0, offY, 0);
            streaks.add(new Streak(pos, dir, 0.6f + rnd.nextFloat() * 0.8f));
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (streaks.isEmpty()) return;
        if (fullNullCheck()) return;

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
            float age = (now - s.birth) / (float) STREAK_TTL_MS;
            float alpha = (1f - age) * 0.8f;
            if (alpha < 0.05f) continue;

            Color c;
            if (rainbow.getValue()) {
                int rgb = Color.HSBtoRGB((now / 5000f + age * 0.2f) % 1f, 0.6f, 1f);
                c = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (int) (alpha * 255));
            } else {
                c = new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (alpha * 255));
            }

            // вытянутый вдоль направления полёта «прямоугольник»
            Vec3d start = s.pos.subtract(cam);
            Vec3d end = start.add(s.dir.multiply(length.getValue() * s.sizeMul));
            Vec3d side = new Vec3d(-s.dir.z, 0, s.dir.x).normalize().multiply(width.getValue());

            Vec3d a = start.subtract(side);
            Vec3d b = start.add(side);
            Vec3d c2 = end.add(side);
            Vec3d d = end.subtract(side);

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(c.getRGB());
            buffer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(c.getRGB());
            buffer.vertex(matrix, (float) c2.x, (float) c2.y, (float) c2.z).color(c.getRGB());
            buffer.vertex(matrix, (float) d.x, (float) d.y, (float) d.z).color(c.getRGB());
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
