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


public class AmbientMotes extends Module {

    private static final long MOTE_TTL_MS = 6000L;

    private final NumberSetting density =
            new NumberSetting("Плотность", 40f, 5f, 150f, 5f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 8f, 3f, 20f, 1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.06f, 0.02f, 0.2f, 0.01f);
    private final BooleanSetting onlyNight = new BooleanSetting("Только ночью", false);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 220, 140, 255).getRGB());

    private final List<Mote> motes = new ArrayList<>();
    private final Random rnd = new Random();

    public AmbientMotes() {
        super("AmbientMotes", Category.Render, "Золотая пыль, парящая вокруг игрока");
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(size);
        getSettings().add(onlyNight);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        motes.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        if (onlyNight.getValue() && !mc.world.isNight()) {
            motes.clear();
            return;
        }

        // поддерживаем популяцию пылинок
        while (motes.size() < density.getValue()) {
            motes.add(new Mote(mc.player.getPos().add(
                    (rnd.nextDouble() - 0.5) * 2 * radius.getValue(),
                    (rnd.nextDouble() - 0.5) * 4,
                    (rnd.nextDouble() - 0.5) * 2 * radius.getValue())));
        }
        while (motes.size() > density.getValue()) {
            motes.remove(motes.size() - 1);
        }

        Iterator<Mote> it = motes.iterator();
        while (it.hasNext()) {
            Mote mote = it.next();
            mote.prevPos = mote.pos;
            mote.age++;

            // медленный дрейф по синусоидальным орбитам
            double t = mote.age / 40.0;
            mote.pos = mote.pos.add(
                    Math.sin(t * 0.35 + mote.phase) * 0.012,
                    Math.sin(t * 0.22 + mote.phase * 2.0) * 0.008 + 0.002,
                    Math.cos(t * 0.28 + mote.phase * 0.7) * 0.012);

            long age = System.currentTimeMillis() - mote.birth;
            if (age >= MOTE_TTL_MS
                    || mote.pos.distanceTo(mc.player.getPos()) > radius.getValue() * 1.6) {
                // переспавн вокруг игрока
                mote.pos = mc.player.getPos().add(
                        (rnd.nextDouble() - 0.5) * 2 * radius.getValue(),
                        (rnd.nextDouble() - 0.5) * 4,
                        (rnd.nextDouble() - 0.5) * 2 * radius.getValue());
                mote.prevPos = mote.pos;
                mote.birth = System.currentTimeMillis();
                mote.age = 0;
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (motes.isEmpty()) return;

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

        for (Mote mote : motes) {
            float lifeT = (now - mote.birth) / (float) MOTE_TTL_MS;
            // плавное появление и исчезновение
            float fadeIn = MathHelper.clamp(lifeT / 0.15f, 0f, 1f);
            float fadeOut = MathHelper.clamp((1f - lifeT) / 0.15f, 0f, 1f);
            // мерцание пылинки
            float twinkle = 0.6f + 0.4f * MathHelper.sin(now / 400f + mote.phase * 5f);
            int alpha = (int) (160 * fadeIn * fadeOut * twinkle);
            if (alpha <= 3) continue;

            matrices.push();
            matrices.translate((float) (mote.pos.x - cam.x), (float) (mote.pos.y - cam.y), (float) (mote.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = size.getValue() * twinkle;
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

    private static final class Mote {
        Vec3d pos, prevPos;
        final float phase;
        long birth = System.currentTimeMillis();
        int age;

        Mote(Vec3d pos) {
            this.pos = pos;
            this.prevPos = pos;
            this.phase = new Random().nextFloat() * (float) (Math.PI * 2);
        }
    }
}
