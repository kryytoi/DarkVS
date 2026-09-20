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
 * FrostTrail — ледяной след за игроком с морозными кристаллами.
 */
public class FrostTrail extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 20f, 4f, 60f, 4f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 0.2f, 0.04f, 0.7f, 0.02f);
    private final NumberSetting lifespan = new NumberSetting("Жизнь (сек)", 1.1f, 0.2f, 4f, 0.1f);

    private static final class Frost {
        double x, y, z;
        long born;
        float spin;
    }

    private final Deque<Frost> crystals = new ArrayDeque<>();
    private final Random rnd = new Random();
    private int tickCounter = 0;

    public FrostTrail() {
        super("FrostTrail", Category.Render, "Ледяной след с морозными кристаллами");
        getSettings().add(density);
        getSettings().add(speed);
        getSettings().add(size);
        getSettings().add(lifespan);
    }

    @Override
    public void onDisable() {
        crystals.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        tickCounter++;
        if (tickCounter % 3 == 0) {
            int count = 1 + (int) (density.getValue() / 14f);
            for (int i = 0; i < count; i++) {
                Frost f = new Frost();
                f.x = mc.player.getX() + (rnd.nextDouble() - 0.5) * 1.1;
                f.y = mc.player.getY() + 0.1 + rnd.nextDouble() * 0.6;
                f.z = mc.player.getZ() + (rnd.nextDouble() - 0.5) * 1.1;
                f.born = System.currentTimeMillis();
                f.spin = rnd.nextFloat() * 360f;
                crystals.addLast(f);
            }
        }

        long lifeMs = (long) (lifespan.getValue() * 1000L);
        long now = System.currentTimeMillis();
        while (!crystals.isEmpty() && now - crystals.peekFirst().born > lifeMs) crystals.pollFirst();
        while (crystals.size() > density.getValue()) crystals.pollFirst();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (crystals.isEmpty() || fullNullCheck()) return;

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

        for (Frost f : crystals) {
            float age = (now - f.born) / (float) lifeMs;
            if (age >= 1f) continue;
            float fade = 1f - age;
            float pulse = 0.85f + 0.15f * (float) Math.sin(age * 10f + f.spin);
            float sz = baseSize * fade * pulse;

            matrices.push();
            matrices.translate((float) (f.x - cam.x), (float) (f.y - cam.y), (float) (f.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f.spin + age * 60f * speed.getValue()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = (int) (230 * fade);
            // кристалл: два скрещённых квадрата
            buffer.vertex(m, 0, -sz, 0).color(140, 220, 255, alpha);
            buffer.vertex(m, -sz, 0, 0).color(140, 220, 255, alpha);
            buffer.vertex(m, 0, sz, 0).color(140, 220, 255, alpha);
            buffer.vertex(m, sz, 0, 0).color(140, 220, 255, alpha);
            buffer.vertex(m, -sz, 0, 0).color(200, 240, 255, alpha);
            buffer.vertex(m, 0, 0, -sz).color(200, 240, 255, alpha);
            buffer.vertex(m, sz, 0, 0).color(200, 240, 255, alpha);
            buffer.vertex(m, 0, 0, sz).color(200, 240, 255, alpha);
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
