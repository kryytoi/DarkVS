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
 * WindStreaks — горизонтальные линии ветра, проносящиеся мимо игрока.
 */
public class WindStreaks extends Module {

    private final NumberSetting count = new NumberSetting("Количество", 26f, 6f, 80f, 2f);
    private final NumberSetting length = new NumberSetting("Длина", 2.4f, 0.5f, 8f, 0.2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0.2f, 4f, 0.1f);
    private final NumberSetting spread = new NumberSetting("Разброс", 5f, 1f, 16f, 1f);
    private final NumberSetting height = new NumberSetting("Высота", 3f, 1f, 12f, 0.5f);

    private final List<float[]> active = new ArrayList<>();
    private final Random rnd = new Random();

    public WindStreaks() {
        super("WindStreaks", Category.Render, "Линии ветра, проносящиеся мимо игрока");
        getSettings().add(count);
        getSettings().add(length);
        getSettings().add(speed);
        getSettings().add(spread);
        getSettings().add(height);
    }

    @Override
    public void onDisable() {
        active.clear();
        super.onDisable();
    }

    private void spawn() {
        float[] p = new float[4];
        p[0] = (rnd.nextFloat() - 0.5f) * spread.getValue() * 2f;
        p[1] = rnd.nextFloat() * height.getValue();
        p[2] = rnd.nextBoolean() ? 1f : -1f;
        p[3] = (rnd.nextFloat() - 0.5f) * spread.getValue() * 2f;
        active.add(p);
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        int target = (int) (float) count.getValue();
        while (active.size() < target) spawn();
        while (active.size() > target) active.remove(active.size() - 1);

        float sp = speed.getValue() * 0.25f;
        for (float[] p : active) {
            p[0] += Math.signum(p[2]) * sp;
            p[1] += sp * 0.06f;
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (active.isEmpty() || fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = mc.player.getLerpedPos(event.getTickDelta());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float len = length.getValue();
        float maxSpread = spread.getValue() * 2f;

        for (float[] p : active) {
            if (Math.abs(p[0]) > maxSpread) continue;

            double px = pos.x + p[0] - cam.x;
            double py = pos.y + p[1] - cam.y;
            double pz = pos.z + p[3] - cam.z;

            matrices.push();
            matrices.translate((float) px, (float) py, (float) pz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));

            Matrix4f m = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = 90;
            float h = 0.035f;
            float s = Math.signum(p[2]);
            buffer.vertex(m, 0, -h, 0).color(200, 220, 255, alpha);
            buffer.vertex(m, 0, h, 0).color(200, 220, 255, alpha);
            buffer.vertex(m, s * len, h, 0).color(200, 220, 255, alpha);
            buffer.vertex(m, s * len, -h, 0).color(200, 220, 255, alpha);
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
