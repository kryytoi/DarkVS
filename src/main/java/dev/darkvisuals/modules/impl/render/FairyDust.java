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


public class FairyDust extends Module {

    // время жизни одной пылинки
    private static final long DUST_TTL_MS = 1400L;

    private final NumberSetting density =
            new NumberSetting("Плотность", 4f, 0f, 16f, 0.5f);
    private final NumberSetting spawnRadius =
            new NumberSetting("Радиус спавна", 0.9f, 0.2f, 3.0f, 0.1f);
    private final NumberSetting twinkleSpeed =
            new NumberSetting("Скорость мерцания", 2.0f, 0.2f, 6.0f, 0.1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.08f, 0.02f, 0.25f, 0.01f);
    private final BooleanSetting onlyMoving =
            new BooleanSetting("Только в движении", true);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 205, 105, 255).getRGB());

    private final List<Dust> dusts = new ArrayList<>();
    private final Random rnd = new Random();

    public FairyDust() {
        super("FairyDust", Category.Render, "Золотая мерцающая пыль, тянущаяся за игроком");
        getSettings().add(density);
        getSettings().add(spawnRadius);
        getSettings().add(twinkleSpeed);
        getSettings().add(size);
        getSettings().add(onlyMoving);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        dusts.clear();
        super.onDisable();
    }

    private boolean isMoving() {
        return mc.player.getVelocity().horizontalLength() > 0.06;
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        // спавним новую порцию пыли, пока игрок двигается
        if (!onlyMoving.getValue() || isMoving()) {
            int count = Math.round(density.getValue());
            for (int i = 0; i < count; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                double dist = spawnRadius.getValue() * rnd.nextDouble();
                dusts.add(new Dust(mc.player.getPos().add(
                        Math.cos(ang) * dist,
                        rnd.nextDouble() * 1.8,
                        Math.sin(ang) * dist)));
            }
        }

        Iterator<Dust> it = dusts.iterator();
        while (it.hasNext()) {
            Dust d = it.next();
            d.age++;
            // пыль парит и медленно оседает
            d.pos = d.pos.add(d.vel);
            d.vel = d.vel.add(0, -0.0008, 0).multiply(0.97);
            d.pos = d.pos.add(
                    Math.sin(d.age / 30.0 + d.phase) * 0.0025,
                    0,
                    Math.cos(d.age / 27.0 + d.phase) * 0.0025);

            if (System.currentTimeMillis() - d.birth >= d.ttl) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (dusts.isEmpty()) return;

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

        for (Dust d : dusts) {
            float lifeT = (now - d.birth) / (float) d.ttl;
            // плавное появление и исчезновение
            float fadeIn = MathHelper.clamp(lifeT / 0.2f, 0f, 1f);
            float fadeOut = MathHelper.clamp((1f - lifeT) / 0.25f, 0f, 1f);
            // мерцание искры
            float twinkle = 0.3f + 0.7f * MathHelper.clamp(
                    MathHelper.sin(now / 1000f * twinkleSpeed.getValue() * 2f + d.phase * 4f), 0f, 1f);
            int alpha = (int) (220 * fadeIn * fadeOut * twinkle);
            if (alpha <= 3) continue;

            matrices.push();
            matrices.translate((float) (d.pos.x - cam.x), (float) (d.pos.y - cam.y), (float) (d.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = size.getValue() * (0.7f + 0.5f * twinkle);
            int mid = alpha / 2;

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -half, -half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            buffer.vertex(matrix, -half, half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), mid);
            buffer.vertex(matrix, half, half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, -half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), mid);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static final class Dust {
        Vec3d pos;
        Vec3d vel;
        final float phase;
        final long birth = System.currentTimeMillis();
        final long ttl;
        int age;

        Dust(Vec3d pos) {
            this.pos = pos;
            Random random = new Random();
            this.vel = new Vec3d(
                    (random.nextDouble() - 0.5) * 0.02,
                    random.nextDouble() * 0.02,
                    (random.nextDouble() - 0.5) * 0.02);
            this.phase = random.nextFloat() * (float) (Math.PI * 2);
            this.ttl = DUST_TTL_MS + (long) (random.nextDouble() * 700L);
        }
    }
}
