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

/**
 * EchoTrail — «эхо» силуэтов игрока, остающееся за спиной при движении.
 */
public class EchoTrail extends Module {

    private final NumberSetting max = new NumberSetting("Количество эхо", 6f, 2f, 16f, 1f);
    private final NumberSetting interval = new NumberSetting("Интервал (тик)", 3f, 1f, 20f, 1f);
    private final NumberSetting lifespan = new NumberSetting("Жизнь (сек)", 1.5f, 0.3f, 5f, 0.1f);

    private static final class Echo {
        final double x, y, z;
        final long born;

        Echo(double x, double y, double z, long born) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.born = born;
        }
    }

    private final Deque<Echo> echoes = new ArrayDeque<>();
    private int tickCounter = 0;

    public EchoTrail() {
        super("EchoTrail", Category.Render, "Эхо-силуэты игрока при движении");
        getSettings().add(max);
        getSettings().add(interval);
        getSettings().add(lifespan);
    }

    @Override
    public void onDisable() {
        echoes.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        tickCounter++;
        if (tickCounter >= interval.getValue()) {
            tickCounter = 0;
            echoes.addLast(new Echo(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    System.currentTimeMillis()));
        }

        long lifeMs = (long) (lifespan.getValue() * 1000L);
        long now = System.currentTimeMillis();
        while (!echoes.isEmpty() && now - echoes.peekFirst().born > lifeMs) echoes.pollFirst();
        while (echoes.size() > max.getValue()) echoes.pollFirst();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (echoes.isEmpty() || fullNullCheck()) return;

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

        for (Echo e : echoes) {
            float age = (now - e.born) / (float) lifeMs;
            if (age >= 1f) continue;
            float fade = 1f - age;

            matrices.push();
            matrices.translate((float) (e.x - cam.x), (float) (e.y - cam.y), (float) (e.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = (int) (170 * fade);
            float w = 0.3f;
            float h = 1.8f;
            buffer.vertex(m, -w, 0, 0).color(120, 200, 255, alpha);
            buffer.vertex(m, -w, h, 0).color(120, 200, 255, alpha);
            buffer.vertex(m, w, h, 0).color(120, 200, 255, alpha);
            buffer.vertex(m, w, 0, 0).color(120, 200, 255, alpha);
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
