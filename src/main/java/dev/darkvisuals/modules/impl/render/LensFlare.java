package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.ThemeManager;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;


public class LensFlare extends Module {

    private static final Identifier GLOW_TEXTURE = darkvisuals.id("hud/glow.png");

    private final NumberSetting intensity =
            new NumberSetting("Интенсивность", 1.0f, 0.2f, 2.5f, 0.1f);
    private final NumberSetting distance =
            new NumberSetting("Дистанция", 80f, 30f, 150f, 5f);
    private final BooleanSetting moonFlare = new BooleanSetting("Луна", true);
    private final BooleanSetting themeColor = new BooleanSetting("Цвет темы", true);

    public LensFlare() {
        super("LensFlare", Category.Render, "Блики линз от солнца и луны");
        getSettings().add(intensity);
        getSettings().add(distance);
        getSettings().add(moonFlare);
        getSettings().add(themeColor);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        // направление на солнце по времени суток (восход — +X, полдень — верх)
        float skyAngle = getSkyAngle();
        double theta = skyAngle * Math.PI * 2;
        Vec3d sunDir = new Vec3d(Math.cos(theta), Math.sin(theta), 0);

        boolean sunUp = sunDir.y > 0.03;
        boolean moonUp = -sunDir.y > 0.03;
        if (!sunUp && (!moonFlare.getValue() || !moonUp)) return;

        Vec3d lightDir = sunUp ? sunDir : sunDir.negate();
        float strength = intensity.getValue() * (sunUp
                ? MathHelper.clamp((float) (lightDir.y * 4f), 0.15f, 1f)
                : 0.6f);

        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        float yawRad = (float) Math.toRadians(-mc.gameRenderer.getCamera().getYaw());
        float pitchRad = (float) Math.toRadians(mc.gameRenderer.getCamera().getPitch());
        float cp = MathHelper.cos(pitchRad);
        Vec3d forward = new Vec3d(Math.sin(yawRad) * cp, -Math.sin(pitchRad), Math.cos(yawRad) * cp);

        // не рисуем, если свет за спиной
        if (lightDir.dotProduct(forward) < 0.15) return;

        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        Color warm = new Color(255, 220, 160, 255);
        Color cool = new Color(180, 200, 255, 255);
        Color base = sunUp ? warm : cool;
        Color tint = themeColor.getValue() ? accent : base;

        MatrixStack matrices = event.getMatrices();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        // яркое ядро у самого светила
        drawFlare(matrices, camPos, lightDir, distance.getValue() * 0.98f, 14f * strength, tint, (int) (150 * strength));

        // призраки вдоль линии светило — центр экрана
        float[] ghosts = {0.35f, 0.6f, 0.85f, 1.15f, 1.45f, -0.3f};
        float[] sizes = {4f, 7f, 3f, 9f, 5f, 3.5f};
        for (int i = 0; i < ghosts.length; i++) {
            float t = ghosts[i];
            Vec3d ghostDir = lightDir.multiply(1f - t).add(forward.multiply(t)).normalize();
            if (ghostDir.dotProduct(forward) < 0.2) continue;
            int alpha = (int) ((i % 2 == 0 ? 70 : 45) * strength);
            drawFlare(matrices, camPos, ghostDir, distance.getValue(), sizes[i] * strength, tint, alpha);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private float getSkyAngle() {
        float timeOfDay = mc.world.getTimeOfDay() % 24000f / 24000f;
        float f = timeOfDay - 0.25f;
        if (f < 0) f++;
        float g = 1.0f - ((MathHelper.cos(f * (float) Math.PI) + 1.0f) / 2.0f);
        return f + (g - f) / 3.0f;
    }

    private void drawFlare(MatrixStack matrices, Vec3d camPos, Vec3d dir, float dist, float size, Color color, int alpha) {
        if (alpha <= 3) return;
        Vec3d pos = camPos.add(dir.multiply(dist));

        matrices.push();
        matrices.translate((float) (pos.x - camPos.x), (float) (pos.y - camPos.y), (float) (pos.z - camPos.z));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = size / 2f;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        buffer.vertex(matrix, half, half, 0).texture(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }
}
