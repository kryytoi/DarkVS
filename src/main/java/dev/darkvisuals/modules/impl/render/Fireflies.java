package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class Fireflies extends Module {

    private static final Identifier FIREFLY_TEXTURE = darkvisuals.id("particles/firefly.png");

    private final NumberSetting density =
            new NumberSetting("Плотность", 24f, 4f, 80f, 2f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 14f, 5f, 32f, 1f);
    private final NumberSetting speed =
            new NumberSetting("Скорость", 1.0f, 0.2f, 3.0f, 0.1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.25f, 0.1f, 0.8f, 0.05f);
    private final BooleanSetting onlyNight = new BooleanSetting("Только ночью", true);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(190, 255, 120, 255).getRGB());

    private final Random rnd = new Random();
    private final List<Firefly> fireflies = new ArrayList<>();

    public Fireflies() {
        super("Fireflies", Category.Render, "Светлячки, летающие вокруг игрока ночью");
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(speed);
        getSettings().add(size);
        getSettings().add(onlyNight);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        fireflies.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        if (onlyNight.getValue() && !mc.world.isNight()) {
            fireflies.clear();
            return;
        }

        // поддерживаем популяцию
        while (fireflies.size() < (int) (float) density.getValue()) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = radius.getValue() * (0.3 + 0.7 * rnd.nextDouble());
            fireflies.add(new Firefly(mc.player.getPos().add(
                    Math.cos(ang) * dist,
                    rnd.nextDouble() * 4 - 0.5,
                    Math.sin(ang) * dist)));
        }
        while (fireflies.size() > (int) (float) density.getValue()) {
            fireflies.remove(fireflies.size() - 1);
        }

        Iterator<Firefly> it = fireflies.iterator();
        while (it.hasNext()) {
            Firefly f = it.next();
            // блуждание
            f.vel = f.vel.add(
                    (rnd.nextDouble() - 0.5) * 0.012 * speed.getValue(),
                    (rnd.nextDouble() - 0.5) * 0.008 * speed.getValue(),
                    (rnd.nextDouble() - 0.5) * 0.012 * speed.getValue());
            f.vel = f.vel.multiply(0.96);
            f.pos = f.pos.add(f.vel);

            // не даём улетать далеко и проваливаться под землю
            Vec3d center = mc.player.getPos();
            if (f.pos.distanceTo(center) > radius.getValue()) {
                f.vel = f.vel.add(center.subtract(f.pos).normalize().multiply(0.02));
            }
            BlockPos below = BlockPos.ofFloored(f.pos).down();
            if (!mc.world.getBlockState(below).isAir() && f.pos.y < below.up().getY() + 0.3) {
                f.vel = f.vel.add(0, 0.01, 0);
                f.pos = new Vec3d(f.pos.x, below.up().getY() + 0.3, f.pos.z);
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fireflies.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, FIREFLY_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        Color c = color.getColor();
        for (Firefly f : fireflies) {
            // мерцание: светлячок мигает своим ритмом
            float blink = 0.35f + 0.65f * MathHelper.clamp(
                    (float) Math.sin(now / 900.0 * f.blinkSpeed + f.blinkPhase), 0f, 1f);
            int alpha = (int) (200 * blink);

            matrices.push();
            matrices.translate((float) (f.pos.x - cam.x), (float) (f.pos.y - cam.y), (float) (f.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = size.getValue() / 2f * (0.8f + 0.4f * blink);

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, half, 0).texture(1, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static final class Firefly {
        Vec3d pos;
        Vec3d vel = Vec3d.ZERO;
        final float blinkSpeed;
        final float blinkPhase;

        Firefly(Vec3d pos) {
            this.pos = pos;
            Random random = new Random();
            this.blinkSpeed = 0.7f + random.nextFloat() * 1.6f;
            this.blinkPhase = random.nextFloat() * (float) (Math.PI * 2);
        }
    }
}
