package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

/**
 * Aurora — северное сияние над игроком: волнистые светящиеся ленты в небе.
 * Ленты строятся каждый кадр по синусоидам, цвета плавно перетекают.
 */
public class Aurora extends Module {

    private final NumberSetting height =
            new NumberSetting("Высота", 55f, 30f, 100f, 1f);
    private final NumberSetting ribbonHeight =
            new NumberSetting("Высота ленты", 18f, 6f, 40f, 1f);
    private final NumberSetting waves =
            new NumberSetting("Волны", 2.0f, 0.5f, 5.0f, 0.1f);
    private final NumberSetting alpha =
            new NumberSetting("Прозрачность", 0.5f, 0.1f, 1.0f, 0.05f);
    private final BooleanSetting nightOnly =
            new BooleanSetting("Только ночью", true);
    private final ColorSetting colorA =
            new ColorSetting("Цвет 1", new Color(80, 255, 180, 255).getRGB());
    private final ColorSetting colorB =
            new ColorSetting("Цвет 2", new Color(140, 100, 255, 255).getRGB());

    public Aurora() {
        super("Aurora", Category.Render, "Северное сияние в небе");
        getSettings().add(height);
        getSettings().add(ribbonHeight);
        getSettings().add(waves);
        getSettings().add(alpha);
        getSettings().add(nightOnly);
        getSettings().add(colorA);
        getSettings().add(colorB);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        if (nightOnly.getValue() && !isNight()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();

        double baseY = mc.player.getY() + height.getValue();
        double rh = ribbonHeight.getValue();
        float waveFreq = waves.getValue();
        float a = alpha.getValue();

        Color ca = colorA.getColor();
        Color cb = colorB.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // три ленты со сдвигом фазы и высоты
        for (int ribbon = 0; ribbon < 3; ribbon++) {
            float phase = ribbon * 1.9f;
            double yOff = ribbon * 4.0;
            drawRibbon(matrices, cam, baseY + yOff, rh, waveFreq, phase, a, ca, cb, now, ribbon);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawRibbon(MatrixStack matrices, Vec3d cam, double baseY, double rh,
                            float waveFreq, float phase, float alphaMul,
                            Color ca, Color cb, long now, int ribbonIdx) {
        double t = now / 12000.0 + phase;
        int segments = 48;
        double halfLen = 90.0;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            double p1 = i / (double) segments;
            double p2 = (i + 1) / (double) segments;
            double x1 = mc.player.getX() - halfLen + p1 * halfLen * 2;
            double x2 = mc.player.getX() - halfLen + p2 * halfLen * 2;

            double z1 = mc.player.getZ() + Math.sin(p1 * Math.PI * 2 * waveFreq + t) * 12 + ribbonIdx * 9.0;
            double z2 = mc.player.getZ() + Math.sin(p2 * Math.PI * 2 * waveFreq + t) * 12 + ribbonIdx * 9.0;

            // волна по высоте тоже дышит
            double top1 = baseY + Math.sin(p1 * Math.PI * 2 * waveFreq * 0.7 + t * 1.3) * rh * 0.4 + rh;
            double top2 = baseY + Math.sin(p2 * Math.PI * 2 * waveFreq * 0.7 + t * 1.3) * rh * 0.4 + rh;

            int topAlpha = (int) (140 * alphaMul);
            int bottomAlpha = (int) (30 * alphaMul);

            Color top = lerp(ca, cb, (float) p1);
            Color bottom = lerp(ca, cb, (float) p1 * 0.5f);

            buffer.vertex(matrix, (float) (x1 - cam.x), (float) (baseY - cam.y), (float) (z1 - cam.z))
                    .color(bottom.getRed(), bottom.getGreen(), bottom.getBlue(), bottomAlpha);
            buffer.vertex(matrix, (float) (x2 - cam.x), (float) (baseY - cam.y), (float) (z2 - cam.z))
                    .color(bottom.getRed(), bottom.getGreen(), bottom.getBlue(), bottomAlpha);
            buffer.vertex(matrix, (float) (x2 - cam.x), (float) (top2 - cam.y), (float) (z2 - cam.z))
                    .color(top.getRed(), top.getGreen(), top.getBlue(), topAlpha);
            buffer.vertex(matrix, (float) (x1 - cam.x), (float) (top1 - cam.y), (float) (z1 - cam.z))
                    .color(top.getRed(), top.getGreen(), top.getBlue(), topAlpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl, 255);
    }

    private boolean isNight() {
        long timeOfDay = mc.world.getTimeOfDay() % 24000;
        return timeOfDay >= 13000 && timeOfDay <= 23000;
    }
}
