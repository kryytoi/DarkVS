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
 * SpiralAura — двойная светящаяся спираль, обвивающая игрока.
 */
public class SpiralAura extends Module {

    private final NumberSetting turns = new NumberSetting("Витков", 3f, 1f, 8f, 0.5f);
    private final NumberSetting radius = new NumberSetting("Радиус", 1.2f, 0.4f, 4f, 0.1f);
    private final NumberSetting height = new NumberSetting("Высота", 2.4f, 0.8f, 8f, 0.2f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1f, 0f, 4f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 0.09f, 0.02f, 0.3f, 0.01f);

    private float angle;
    private final Random rnd = new Random();
    private final List<Float> phases = new ArrayList<>();

    public SpiralAura() {
        super("SpiralAura", Category.Render, "Двойная светящаяся спираль вокруг игрока");
        getSettings().add(turns);
        getSettings().add(radius);
        getSettings().add(height);
        getSettings().add(speed);
        getSettings().add(size);
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        angle += 0.04f * speed.getValue();
        while (phases.size() < 64) phases.add(rnd.nextFloat() * 6.28f);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = mc.player.getLerpedPos(event.getTickDelta());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float h = height.getValue();
        float r = radius.getValue();
        float t = turns.getValue();
        float sz = size.getValue();
        int points = 64;

        for (int strand = 0; strand < 2; strand++) {
            float baseAng = angle + strand * 3.14159f;
            for (int i = 0; i < points; i++) {
                float frac = i / (float) (points - 1);
                float a = baseAng + frac * t * 6.28318f;
                float y = frac * h;
                float px = (float) (pos.x + Math.cos(a) * r - cam.x);
                float py = (float) (pos.y + y - cam.y);
                float pz = (float) (pos.z + Math.sin(a) * r - cam.z);

                matrices.push();
                matrices.translate(px, py, pz);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

                Matrix4f m = matrices.peek().getPositionMatrix();
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                float fade = 0.35f + 0.65f * (float) Math.sin(frac * Math.PI);
                int alpha = (int) (220 * fade);
                int col = strand == 0 ? 255 : 170;
                buffer.vertex(m, -sz, -sz, 0).color(col, 120, 255, alpha);
                buffer.vertex(m, -sz, sz, 0).color(col, 120, 255, alpha);
                buffer.vertex(m, sz, sz, 0).color(col, 120, 255, alpha);
                buffer.vertex(m, sz, -sz, 0).color(col, 120, 255, alpha);
                BufferRenderer.drawWithGlobalProgram(buffer.end());
                matrices.pop();
            }
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
