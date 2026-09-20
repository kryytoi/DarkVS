package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
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

import java.awt.Color;

/**
 * Rainbow — большая радуга, висящая в небе рядом с игроком.
 * Полукруг из семи параллельных цветных полос, всегда повёрнут
                к камере и подсвечен мягким аддитивным свечением.
 */
public class Rainbow extends Module {

    // классические семь цветов радуги
    private static final Color[] RAINBOW_COLORS = {
            new Color(255, 0, 0),
            new Color(255, 127, 0),
            new Color(255, 255, 0),
            new Color(0, 200, 0),
            new Color(0, 150, 255),
            new Color(75, 0, 160),
            new Color(170, 0, 220)
    };

    private final NumberSetting radius =
            new NumberSetting("Радиус", 32f, 8f, 80f, 1f);
    private final NumberSetting thickness =
            new NumberSetting("Толщина полос", 1.2f, 0.2f, 4f, 0.1f);
    private final NumberSetting alpha =
            new NumberSetting("Прозрачность", 0.5f, 0f, 1f, 0.05f);
    private final NumberSetting distance =
            new NumberSetting("Дистанция", 18f, 0f, 64f, 1f);
    private final BooleanSetting onlyRain =
            new BooleanSetting("Только в дождь", false);

    public Rainbow() {
        super("Rainbow", Category.Render, "Радуга, висящая в небе рядом с игроком");
        getSettings().add(radius);
        getSettings().add(thickness);
        getSettings().add(alpha);
        getSettings().add(distance);
        getSettings().add(onlyRain);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        if (onlyRain.getValue() && !mc.world.isRaining()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float yaw = mc.gameRenderer.getCamera().getYaw();
        float pitch = mc.gameRenderer.getCamera().getPitch();

        // направление взгляда по горизонтали — туда и смотрит радуга
        float rad = (float) Math.toRadians(yaw);
        double fwdX = -Math.sin(rad);
        double fwdZ = Math.cos(rad);

        float r = radius.getValue();
        Vec3d player = mc.player.getLerpedPos(event.getTickDelta());
        Vec3d center = player.add(fwdX * distance.getValue(), r * 0.5, fwdZ * distance.getValue());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
        matrices.translate((float) (center.x - cam.x), (float) (center.y - cam.y), (float) (center.z - cam.z));
        // поворот к камере — арка всегда лицом к игроку
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

        drawArc(matrices.peek().getPositionMatrix(), r);

        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // верхний полукруг из семи вложенных цветных полос
    private void drawArc(Matrix4f matrix, float radius) {
        float th = thickness.getValue();
        float a = alpha.getValue();
        int segments = 56;

        for (int band = 0; band < RAINBOW_COLORS.length; band++) {
            float rIn = radius + band * th;
            float rOut = rIn + th * 0.92f;
            // мягкие края: полосы в середине ярче
            float bandFade = 0.35f + 0.65f * (float) Math.sin(Math.PI * (band + 0.5f) / RAINBOW_COLORS.length);
            int alpha = (int) (255 * a * bandFade);
            if (alpha <= 4) continue;

            Color c = RAINBOW_COLORS[band];
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (int i = 0; i < segments; i++) {
                double a0 = Math.PI * i / segments;
                double a1 = Math.PI * (i + 1) / segments;

                float x0i = (float) (Math.cos(a0) * rIn), y0i = (float) (Math.sin(a0) * rIn);
                float x1i = (float) (Math.cos(a1) * rIn), y1i = (float) (Math.sin(a1) * rIn);
                float x0o = (float) (Math.cos(a0) * rOut), y0o = (float) (Math.sin(a0) * rOut);
                float x1o = (float) (Math.cos(a1) * rOut), y1o = (float) (Math.sin(a1) * rOut);

                buffer.vertex(matrix, x0i, y0i, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, x1i, y1i, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, x1o, y1o, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, x0o, y0o, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }
}
