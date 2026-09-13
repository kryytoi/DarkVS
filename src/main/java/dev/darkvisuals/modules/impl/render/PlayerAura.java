package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.ThemeManager;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;


public class PlayerAura extends Module {

    private static final Identifier GLOW_TEXTURE = darkvisuals.id("hud/glow.png");
    private static final int RING_SEGMENTS = 64;

    private final NumberSetting ringRadius =
            new NumberSetting("Радиус колец", 1.1f, 0.5f, 3.0f, 0.1f);
    private final NumberSetting speed =
            new NumberSetting("Скорость вращения", 1.0f, 0.1f, 4.0f, 0.1f);
    private final NumberSetting orbCount =
            new NumberSetting("Кол-во орбов", 3f, 0f, 8f, 1f);
    private final BooleanSetting secondRing = new BooleanSetting("Второе кольцо", true);
    private final BooleanSetting themeColor = new BooleanSetting("Цвет темы", true);
    private final ColorSetting auraColor =
            new ColorSetting("Цвет ауры", new Color(140, 100, 255, 255).getRGB());

    public PlayerAura() {
        super("PlayerAura", Category.Render, "Вращающиеся кольца и орбы вокруг игрока");
        getSettings().add(ringRadius);
        getSettings().add(speed);
        getSettings().add(orbCount);
        getSettings().add(secondRing);
        getSettings().add(themeColor);
        getSettings().add(auraColor);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        float time = now / 1000f;

        Vec3d playerPos = mc.player.getLerpedPos(event.getTickDelta());
        float ringR = ringRadius.getValue();
        float rot = time * 40f * speed.getValue();

        Color color = themeColor.getValue()
                ? ThemeManager.getInstance().getCurrentTheme().getAccentColor()
                : auraColor.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
        matrices.translate(
                (float) (playerPos.x - cam.x),
                (float) (playerPos.y - cam.y + 0.15),
                (float) (playerPos.z - cam.z));

        drawAuraRing(matrices, ringR, rot, color, 1.0f);
        if (secondRing.getValue()) {
            drawAuraRing(matrices, ringR * 0.72f, -rot * 1.4f, color, 0.7f);
        }
        matrices.pop();

        // светящиеся орбы, вращающиеся вокруг игрока
        int orbs = (int) (float) orbCount.getValue();
        if (orbs > 0) {
            RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            for (int i = 0; i < orbs; i++) {
                float angle = time * 1.6f * speed.getValue() + i * (MathHelper.TAU / orbs);
                float wobble = MathHelper.sin(time * 2.2f + i * 2.1f);
                Vec3d orbPos = playerPos.add(
                        Math.cos(angle) * ringR * 1.05f,
                        1.0 + wobble * 0.35,
                        Math.sin(angle) * ringR * 1.05f);

                matrices.push();
                matrices.translate((float) (orbPos.x - cam.x), (float) (orbPos.y - cam.y), (float) (orbPos.z - cam.z));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                float half = 0.22f;
                int alpha = (int) (170 + 60 * wobble);
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                buffer.vertex(matrix, half, half, 0).texture(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
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

    private void drawAuraRing(MatrixStack matrices, float radius, float rotation, Color color, float strength) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);

            // бегущий градиент яркости по кольцу
            float wave = 0.45f + 0.55f * (0.5f + 0.5f * MathHelper.sin(angle * 3f + (float) Math.toRadians(rotation)));
            int alpha = (int) (170 * wave * strength);
            int innerAlpha = (int) (60 * wave * strength);

            buffer.vertex(matrix, cos * radius * 0.55f, 0, sin * radius * 0.55f)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), innerAlpha);
            buffer.vertex(matrix, cos * radius, 0, sin * radius)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
