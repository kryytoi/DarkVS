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
 * DiamondTrail — дорожка из вращающихся алмазов за игроком.
 */
public class DiamondTrail extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 10f, 2f, 30f, 2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 0.28f, 0.06f, 1f, 0.02f);
    private final NumberSetting lifespan = new NumberSetting("Жизнь (сек)", 1.4f, 0.3f, 5f, 0.1f);

    private static final class Gem {
        double x, y, z;
        long born;
        float spin, spinSpeed;
    }

    private final Deque<Gem> gems = new ArrayDeque<>();
    private final Random rnd = new Random();
    private int tickCounter = 0;

    public DiamondTrail() {
        super("DiamondTrail", Category.Render, "Дорожка из вращающихся алмазов");
        getSettings().add(density);
        getSettings().add(speed);
        getSettings().add(size);
        getSettings().add(lifespan);
    }

    @Override
    public void onDisable() {
        gems.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        tickCounter++;
        if (tickCounter % 4 == 0) {
            Gem g = new Gem();
            g.x = mc.player.getX() + (rnd.nextDouble() - 0.5) * 1.2;
            g.y = mc.player.getY() + 0.3 + rnd.nextDouble() * 0.9;
            g.z = mc.player.getZ() + (rnd.nextDouble() - 0.5) * 1.2;
            g.born = System.currentTimeMillis();
            g.spin = rnd.nextFloat() * 360f;
            g.spinSpeed = (rnd.nextBoolean() ? 1f : -1f) * (2f + rnd.nextFloat() * 6f);
            gems.addLast(g);
        }

        long lifeMs = (long) (lifespan.getValue() * 1000L);
        long now = System.currentTimeMillis();
        while (!gems.isEmpty() && now - gems.peekFirst().born > lifeMs) gems.pollFirst();
        while (gems.size() > density.getValue()) gems.pollFirst();

        for (Gem g : gems) g.spin += g.spinSpeed * speed.getValue();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (gems.isEmpty() || fullNullCheck()) return;

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

        for (Gem g : gems) {
            float age = (now - g.born) / (float) lifeMs;
            if (age >= 1f) continue;
            float fade = 1f - age;
            float sz = baseSize * (0.6f + 0.4f * fade);

            matrices.push();
            matrices.translate((float) (g.x - cam.x), (float) (g.y - cam.y), (float) (g.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g.spin));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = (int) (230 * fade);
            // ромб из 3 квадов: верхняя половина, нижняя половина и блик
            buffer.vertex(m, 0, -sz, 0).color(100, 220, 255, alpha);
            buffer.vertex(m, -sz * 0.5f, 0, 0).color(100, 220, 255, alpha);
            buffer.vertex(m, 0, sz, 0).color(100, 220, 255, alpha);
            buffer.vertex(m, sz * 0.5f, 0, 0).color(100, 220, 255, alpha);
            buffer.vertex(m, -sz * 0.5f, 0, 0).color(180, 240, 255, alpha);
            buffer.vertex(m, -sz * 0.25f, sz * 0.4f, 0).color(255, 255, 255, alpha);
            buffer.vertex(m, 0, sz * 0.7f, 0).color(180, 240, 255, alpha);
            buffer.vertex(m, 0, sz * 0.2f, 0).color(180, 240, 255, alpha);
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
