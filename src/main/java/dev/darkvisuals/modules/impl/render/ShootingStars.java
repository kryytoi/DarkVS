package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.util.animations.Easing;
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


public class ShootingStars extends Module {

    private final NumberSetting frequency =
            new NumberSetting("Частота (сек)", 8f, 2f, 30f, 1f);
    private final NumberSetting speed =
            new NumberSetting("Скорость", 1.0f, 0.3f, 3.0f, 0.1f);
    private final NumberSetting length =
            new NumberSetting("Длина хвоста", 24f, 6f, 60f, 2f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.8f, 0.3f, 2.0f, 0.1f);
    private final BooleanSetting onlyNight = new BooleanSetting("Только ночью", true);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(230, 240, 255, 255).getRGB());

    private final Random rnd = new Random();
    private final List<Star> stars = new ArrayList<>();
    private long nextSpawnMs = 0;

    public ShootingStars() {
        super("ShootingStars", Category.Render, "Падающие звёзды, прочерчивающие небо");
        getSettings().add(frequency);
        getSettings().add(speed);
        getSettings().add(length);
        getSettings().add(size);
        getSettings().add(onlyNight);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        stars.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        if (now >= nextSpawnMs) {
            nextSpawnMs = now + (long) (frequency.getValue() * 1000 * (0.6 + rnd.nextDouble() * 0.8));
            if (!onlyNight.getValue() || mc.world.isNight()) {
                spawnStar();
            }
        }

        Iterator<Star> it = stars.iterator();
        while (it.hasNext()) {
            Star star = it.next();
            star.progress += 0.02f * speed.getValue();
            if (star.progress >= 1f) it.remove();
        }
    }

    private void spawnStar() {
        // стартовая точка на куполе неба над игроком
        double theta = rnd.nextDouble() * Math.PI * 2;
        double phi = 0.35 + rnd.nextDouble() * 0.45; // высота над горизонтом
        double radius = 140;

        Vec3d center = mc.player.getPos();
        Vec3d start = center.add(
                Math.cos(theta) * Math.cos(phi) * radius,
                Math.sin(phi) * radius,
                Math.sin(theta) * Math.cos(phi) * radius);

        // направление вдоль купола
        double dirAngle = theta + (rnd.nextBoolean() ? 1 : -1) * (0.6 + rnd.nextDouble() * 0.8);
        Vec3d dir = new Vec3d(
                -Math.sin(dirAngle),
                -0.15 - rnd.nextDouble() * 0.2,
                Math.cos(dirAngle)).normalize();

        stars.add(new Star(start, dir));
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (stars.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Color c = color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Star star : stars) {
            float eased = (float) Easing.LINEAR.apply(star.progress);
            Vec3d head = star.start.add(star.dir.multiply(eased * 60f));

            // хвост из сегментов, тающих к началу
            int segments = (int) (float) length.getValue();
            for (int i = 0; i < segments; i++) {
                float f = 1f - i / (float) segments; // 1 у головы
                float segFade = f * f * (1f - MathHelper.clamp(star.progress * 1.15f, 0f, 1f) * 0.3f);
                int alpha = (int) (235 * segFade);
                if (alpha <= 3) continue;

                Vec3d pos = head.subtract(star.dir.multiply(i * 0.8f));
                matrices.push();
                matrices.translate((float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                float half = size.getValue() / 2f * (0.25f + 0.75f * f);
                Color warm = new Color(
                        Math.min(255, c.getRed() + (255 - c.getRed()) / 2),
                        Math.min(255, c.getGreen() + (255 - c.getGreen()) / 2),
                        Math.min(255, c.getBlue() + (255 - c.getBlue()) / 2), alpha);

                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                buffer.vertex(matrix, -half, -half, 0).color(warm.getRGB());
                buffer.vertex(matrix, -half, half, 0).color(warm.getRGB());
                buffer.vertex(matrix, half, half, 0).color(warm.getRGB());
                buffer.vertex(matrix, half, -half, 0).color(warm.getRGB());
                BufferRenderer.drawWithGlobalProgram(buffer.end());
                matrices.pop();
            }
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static final class Star {
        final Vec3d start;
        final Vec3d dir;
        float progress = 0f;

        Star(Vec3d start, Vec3d dir) {
            this.start = start;
            this.dir = dir;
        }
    }
}
