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
 * HeartTrail — дорожка из летящих за игроком сердечек.
 */
public class HeartTrail extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 12f, 2f, 40f, 2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 3f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 0.22f, 0.05f, 0.8f, 0.02f);
    private final NumberSetting lifespan = new NumberSetting("Жизнь (сек)", 1.6f, 0.3f, 5f, 0.1f);

    private static final class Heart {
        double x, y, z, vx, vy;
        long born;
    }

    private final Deque<Heart> hearts = new ArrayDeque<>();
    private final Random rnd = new Random();
    private int tickCounter = 0;

    public HeartTrail() {
        super("HeartTrail", Category.Render, "Дорожка из сердечек за игроком");
        getSettings().add(density);
        getSettings().add(speed);
        getSettings().add(size);
        getSettings().add(lifespan);
    }

    @Override
    public void onDisable() {
        hearts.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        tickCounter++;
        if (tickCounter % 3 == 0) {
            Heart h = new Heart();
            h.x = mc.player.getX() + (rnd.nextDouble() - 0.5) * 0.8;
            h.y = mc.player.getY() + 0.6 + rnd.nextDouble() * 0.6;
            h.z = mc.player.getZ() + (rnd.nextDouble() - 0.5) * 0.8;
            h.vx = (rnd.nextDouble() - 0.5) * 0.15;
            h.vy = 0.08 + rnd.nextDouble() * 0.1;
            h.born = System.currentTimeMillis();
            hearts.addLast(h);
        }

        long lifeMs = (long) (lifespan.getValue() * 1000L);
        long now = System.currentTimeMillis();
        while (!hearts.isEmpty() && now - hearts.peekFirst().born > lifeMs) hearts.pollFirst();
        while (hearts.size() > density.getValue()) hearts.pollFirst();

        for (Heart h : hearts) {
            h.x += h.vx * speed.getValue();
            h.y += h.vy * speed.getValue();
            h.vx *= 0.97;
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (hearts.isEmpty() || fullNullCheck()) return;

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

        for (Heart h : hearts) {
            float age = (now - h.born) / (float) lifeMs;
            if (age >= 1f) continue;
            float fade = 1f - age;
            float pulse = 1f + 0.25f * (float) Math.sin(age * 8f);
            float sz = baseSize * fade * pulse;

            matrices.push();
            matrices.translate((float) (h.x - cam.x), (float) (h.y - cam.y), (float) (h.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = (int) (230 * fade);
            // две дуги + клин = сердечко из 3 квадов
            buffer.vertex(m, -sz, -sz * 0.3f, 0).color(255, 80, 120, alpha);
            buffer.vertex(m, -sz, sz * 0.4f, 0).color(255, 80, 120, alpha);
            buffer.vertex(m, 0, sz, 0).color(255, 80, 120, alpha);
            buffer.vertex(m, 0, sz * 0.2f, 0).color(255, 80, 120, alpha);
            buffer.vertex(m, 0, sz * 0.4f, 0).color(255, 80, 120, alpha);
            buffer.vertex(m, 0, sz, 0).color(255, 80, 120, alpha);
            buffer.vertex(m, sz, sz * 0.4f, 0).color(255, 80, 120, alpha);
            buffer.vertex(m, sz, -sz * 0.3f, 0).color(255, 80, 120, alpha);
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
