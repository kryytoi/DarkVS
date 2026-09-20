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
 * MoneyTrail — дорожка из летящих за игроком монет и купюр.
 */
public class MoneyTrail extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 12f, 2f, 40f, 2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 0.24f, 0.05f, 0.8f, 0.02f);
    private final NumberSetting lifespan = new NumberSetting("Жизнь (сек)", 1.5f, 0.3f, 5f, 0.1f);

    private static final class Coin {
        double x, y, z;
        long born;
        float spin, spinSpeed;
    }

    private final Deque<Coin> coins = new ArrayDeque<>();
    private final Random rnd = new Random();
    private int tickCounter = 0;

    public MoneyTrail() {
        super("MoneyTrail", Category.Render, "Дорожка из монет за игроком");
        getSettings().add(density);
        getSettings().add(speed);
        getSettings().add(size);
        getSettings().add(lifespan);
    }

    @Override
    public void onDisable() {
        coins.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        tickCounter++;
        if (tickCounter % 3 == 0) {
            Coin c = new Coin();
            c.x = mc.player.getX() + (rnd.nextDouble() - 0.5) * 1.4;
            c.y = mc.player.getY() + 0.4 + rnd.nextDouble() * 1.2;
            c.z = mc.player.getZ() + (rnd.nextDouble() - 0.5) * 1.4;
            c.born = System.currentTimeMillis();
            c.spin = rnd.nextFloat() * 360f;
            c.spinSpeed = (rnd.nextBoolean() ? 1f : -1f) * (3f + rnd.nextFloat() * 8f);
            coins.addLast(c);
        }

        long lifeMs = (long) (lifespan.getValue() * 1000L);
        long now = System.currentTimeMillis();
        while (!coins.isEmpty() && now - coins.peekFirst().born > lifeMs) coins.pollFirst();
        while (coins.size() > density.getValue()) coins.pollFirst();

        for (Coin c : coins) {
            c.spin += c.spinSpeed * speed.getValue();
            c.y -= 0.02 * speed.getValue();
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (coins.isEmpty() || fullNullCheck()) return;

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

        for (Coin c : coins) {
            float age = (now - c.born) / (float) lifeMs;
            if (age >= 1f) continue;
            float fade = 1f - age;
            float sz = baseSize * (0.7f + 0.3f * fade);

            matrices.push();
            matrices.translate((float) (c.x - cam.x), (float) (c.y - cam.y), (float) (c.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(c.spin));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = (int) (230 * fade);
            // монета: яркий круг + тёмный ободок
            buffer.vertex(m, -sz, -sz, 0).color(255, 215, 60, alpha);
            buffer.vertex(m, -sz, sz, 0).color(255, 215, 60, alpha);
            buffer.vertex(m, sz, sz, 0).color(255, 215, 60, alpha);
            buffer.vertex(m, sz, -sz, 0).color(255, 215, 60, alpha);
            float w2 = sz * 0.75f;
            buffer.vertex(m, -w2, -w2, 0).color(180, 140, 20, alpha);
            buffer.vertex(m, -w2, w2, 0).color(180, 140, 20, alpha);
            buffer.vertex(m, w2, w2, 0).color(180, 140, 20, alpha);
            buffer.vertex(m, w2, -w2, 0).color(180, 140, 20, alpha);
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
