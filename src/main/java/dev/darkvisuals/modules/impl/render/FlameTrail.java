package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * FlameTrail — пламенный след за игроком с искрами.
 */
public class FlameTrail extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 24f, 4f, 80f, 4f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 0.22f, 0.04f, 0.8f, 0.02f);
    private final NumberSetting lifespan = new NumberSetting("Жизнь (сек)", 0.9f, 0.2f, 3f, 0.1f);

    private static final class Flame {
        double x, y, z;
        double vx, vy;
        long born;
        boolean spark;
    }

    private final Deque<Flame> flames = new ArrayDeque<>();
    private final Random rnd = new Random();
    private int tickCounter = 0;

    public FlameTrail() {
        super("FlameTrail", Category.Render, "Пламенный след за игроком");
        getSettings().add(density);
        getSettings().add(speed);
        getSettings().add(size);
        getSettings().add(lifespan);
    }

    @Override
    public void onDisable() {
        flames.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        tickCounter++;
        if (tickCounter % 2 == 0) {
            int count = 1 + (int) (density.getValue() / 12f);
            for (int i = 0; i < count; i++) {
                Flame f = new Flame();
                f.x = mc.player.getX() + (rnd.nextDouble() - 0.5) * 0.9;
                f.y = mc.player.getY() + 0.1 + rnd.nextDouble() * 0.5;
                f.z = mc.player.getZ() + (rnd.nextDouble() - 0.5) * 0.9;
                f.vx = (rnd.nextDouble() - 0.5) * 0.12;
                f.vy = 0.04 + rnd.nextDouble() * 0.14;
                f.born = System.currentTimeMillis();
                f.spark = rnd.nextFloat() < 0.35f;
                flames.addLast(f);
            }
        }

        long lifeMs = (long) (lifespan.getValue() * 1000L);
        long now = System.currentTimeMillis();
        while (!flames.isEmpty() && now - flames.peekFirst().born > lifeMs) flames.pollFirst();
        while (flames.size() > density.getValue()) flames.pollFirst();

        for (Flame f : flames) {
            f.x += f.vx * speed.getValue();
            f.y += f.vy * speed.getValue();
            f.vx *= 0.95;
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (flames.isEmpty() || fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        long lifeMs = (long) (lifespan.getValue() * 1000L);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float baseSize = size.getValue();

        for (Flame f : flames) {
            float age = (now - f.born) / (float) lifeMs;
            if (age >= 1f) continue;
            float fade = 1f - age;
            float sz = baseSize * fade * (f.spark ? 0.5f : 1f);

            matrices.push();
            matrices.translate((float) (f.x - cam.x), (float) (f.y - cam.y), (float) (f.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = (int) (240 * fade);
            // цвет от жёлтого к красному по мере угасания
            int r = 255;
            int g = (int) (180 - 140 * age);
            int b = 40;
            buffer.vertex(m, -sz, -sz, 0).color(r, g, b, alpha);
            buffer.vertex(m, -sz, sz, 0).color(r, g, b, alpha);
            buffer.vertex(m, sz, sz, 0).color(r, g, b, alpha);
            buffer.vertex(m, sz, -sz, 0).color(r, g, b, alpha);
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
