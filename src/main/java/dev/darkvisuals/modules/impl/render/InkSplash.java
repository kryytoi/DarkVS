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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * InkSplash — чернильные брызги, летящие от игрока при движении.
 */
public class InkSplash extends Module {

    private final NumberSetting density = new NumberSetting("Плотность", 14f, 2f, 50f, 2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 0.14f, 0.03f, 0.6f, 0.02f);
    private final NumberSetting lifespan = new NumberSetting("Жизнь (сек)", 1.2f, 0.2f, 4f, 0.1f);

    private static final class Drop {
        double x, y, z, vx, vy, vz;
        long born;
        float sizeMul;
    }

    private final List<Drop> drops = new ArrayList<>();
    private final Random rnd = new Random();
    private double prevX, prevZ;

    public InkSplash() {
        super("InkSplash", Category.Render, "Чернильные брызги от игрока при движении");
        getSettings().add(density);
        getSettings().add(speed);
        getSettings().add(size);
        getSettings().add(lifespan);
    }

    @Override
    public void onDisable() {
        drops.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        double cx = mc.player.getX(), cz = mc.player.getZ();
        double dx = cx - prevX, dz = cz - prevZ;
        double speedSq = dx * dx + dz * dz;

        if (speedSq > 0.002) {
            int count = (int) Math.min(density.getValue(), speedSq * 60);
            for (int i = 0; i < count; i++) {
                Drop d = new Drop();
                d.x = cx + (rnd.nextDouble() - 0.5) * 0.6;
                d.y = mc.player.getY() + 0.1;
                d.z = cz + (rnd.nextDouble() - 0.5) * 0.6;
                d.vx = -dx * 0.8 + (rnd.nextDouble() - 0.5) * 0.3;
                d.vy = rnd.nextDouble() * 0.25;
                d.vz = -dz * 0.8 + (rnd.nextDouble() - 0.5) * 0.3;
                d.born = System.currentTimeMillis();
                d.sizeMul = 0.5f + rnd.nextFloat() * 0.8f;
                drops.add(d);
            }
        }

        prevX = cx;
        prevZ = cz;

        long lifeMs = (long) (lifespan.getValue() * 1000L);
        long now = System.currentTimeMillis();
        drops.removeIf(d -> now - d.born > lifeMs);

        for (Drop d : drops) {
            d.x += d.vx * speed.getValue() * 0.5;
            d.y += d.vy * speed.getValue() * 0.5;
            d.z += d.vz * speed.getValue() * 0.5;
            d.vy -= 0.02 * speed.getValue();
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (drops.isEmpty() || fullNullCheck()) return;

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

        for (Drop d : drops) {
            float age = (now - d.born) / (float) lifeMs;
            if (age >= 1f) continue;
            float fade = 1f - age;
            float sz = baseSize * d.sizeMul * fade;

            matrices.push();
            matrices.translate((float) (d.x - cam.x), (float) (d.y - cam.y), (float) (d.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = (int) (220 * fade);
            buffer.vertex(m, -sz, -sz, 0).color(20, 15, 40, alpha);
            buffer.vertex(m, -sz, sz, 0).color(20, 15, 40, alpha);
            buffer.vertex(m, sz, sz, 0).color(20, 15, 40, alpha);
            buffer.vertex(m, sz, -sz, 0).color(20, 15, 40, alpha);
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
