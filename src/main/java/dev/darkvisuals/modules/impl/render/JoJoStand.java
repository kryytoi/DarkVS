package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

public class JoJoStand extends Module {

    private final NumberSetting opacity     = new NumberSetting("Прозрачность", 0.55f, 0.1f, 1f, 0.05f);
    private final NumberSetting offsetBack  = new NumberSetting("Отступ назад", 1.4f,  0.3f, 3f, 0.1f);
    private final NumberSetting offsetUp    = new NumberSetting("Высота",       0.5f,  0f,   2f, 0.1f);
    private final NumberSetting scale       = new NumberSetting("Масштаб",      1.1f,  0.5f, 2f, 0.05f);
    private final ColorSetting  colorBody   = new ColorSetting("Цвет тела",   new Color(180, 60, 255, 200).getRGB());
    private final ColorSetting  colorGlow   = new ColorSetting("Цвет свечения", new Color(255, 120, 255, 60).getRGB());
    private final BooleanSetting pulsate    = new BooleanSetting("Пульсация в бою", true);

    private double lerpX, lerpY, lerpZ;
    private float  lerpYaw;
    private boolean initialized = false;

    public JoJoStand() {
        super("JoJoStand", Category.Render, "Боевой призрак-ДжоДжо за спиной игрока");
        getSettings().add(opacity);
        getSettings().add(offsetBack);
        getSettings().add(offsetUp);
        getSettings().add(scale);
        getSettings().add(colorBody);
        getSettings().add(colorGlow);
        getSettings().add(pulsate);
    }

    @Override
    public void onDisable() {
        initialized = false;
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        float  yaw = mc.player.getYaw();
        if (!initialized) {
            lerpX = px; lerpY = py; lerpZ = pz; lerpYaw = yaw;
            initialized = true;
        }

        double speed = 0.15;
        lerpX   += (px - lerpX) * speed;
        lerpY   += (py - lerpY) * speed;
        lerpZ   += (pz - lerpZ) * speed;

        float dy = yaw - lerpYaw;
        while (dy >  180) dy -= 360;
        while (dy < -180) dy += 360;
        lerpYaw += dy * 0.12f;
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck() || !initialized) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now  = System.currentTimeMillis();

        float pulse = 1f;
        if (pulsate.getValue()) {
            boolean active = mc.options.sprintKey.isPressed() ||
                    mc.options.attackKey.isPressed();
            float beat = 0.5f + 0.5f * MathHelper.sin(now / 180f);
            pulse = active ? (1f + 0.10f * beat) : 1f;
        }

        float sc   = scale.getValue() * pulse;
        float alpha = Math.min(1f, opacity.getValue() * pulse);

        float yawRad = (float) Math.toRadians(lerpYaw);
        double backX =  Math.sin(yawRad) * offsetBack.getValue();
        double backZ = -Math.cos(yawRad) * offsetBack.getValue();

        double drawX = lerpX + backX;
        double drawY = lerpY + offsetUp.getValue();
        double drawZ = lerpZ + backZ;

        Color body = colorBody.getColor();
        Color glow = colorGlow.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
        matrices.translate(
                (float)(drawX - cam.x),
                (float)(drawY - cam.y),
                (float)(drawZ - cam.z));

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-(lerpYaw + 180f)));

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15f));

        matrices.scale(sc, sc, sc);

        Matrix4f m = matrices.peek().getPositionMatrix();
        renderStand(m, body, glow, (int)(255 * alpha), now);

        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderStand(Matrix4f m, Color body, Color glow, int alpha, long now) {
        int r = body.getRed(), g = body.getGreen(), b = body.getBlue();
        int gr = glow.getRed(), gg = glow.getGreen(), gb = glow.getBlue(), ga = Math.min(255, glow.getAlpha());

        float breathe = 0.92f + 0.08f * MathHelper.sin(now / 400f);
        int glowAlpha = (int)(ga * breathe);

        drawBox(m, -0.20f, 1.75f, -0.05f, 0.40f, 0.40f, 0.10f, r, g, b, alpha);
        drawBox(m, -0.24f, 1.71f, -0.09f, 0.48f, 0.48f, 0.18f, gr, gg, gb, glowAlpha);

        drawBox(m, -0.06f, 1.60f, -0.04f, 0.12f, 0.16f, 0.08f, r, g, b, alpha);

        drawBox(m, -0.24f, 0.90f, -0.09f, 0.48f, 0.72f, 0.18f, r, g, b, alpha);
        drawBox(m, -0.29f, 0.86f, -0.13f, 0.58f, 0.80f, 0.26f, gr, gg, gb, glowAlpha);

        drawBox(m,  0.24f, 1.30f, -0.07f, 0.14f, 0.40f, 0.14f, r, g, b, alpha);

        matrices_drawBoxRotated(m,  0.26f, 1.50f, 0f,  0.12f, 0.40f, 0.12f,
                                30f, r, g, b, alpha);

        drawBox(m, -0.38f, 1.10f, -0.07f, 0.14f, 0.38f, 0.14f, r, g, b, alpha);
        drawBox(m, -0.54f, 0.80f, -0.06f, 0.12f, 0.38f, 0.12f, r, g, b, alpha);

        drawBox(m,  0.04f, 0.10f, -0.07f, 0.20f, 0.82f, 0.14f, r, g, b, alpha);

        drawBox(m, -0.24f, 0.10f, -0.07f, 0.20f, 0.82f, 0.14f, r, g, b, alpha);
    }

    private void drawBox(Matrix4f m,
                         float x, float y, float z,
                         float w, float h, float d,
                         int r, int g, int b, int a) {
        if (a <= 2) return;
        Tessellator tess = Tessellator.getInstance();

        float fz = z + d / 2f;
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(m,   x,   y, fz).color(r, g, b, a);
        buf.vertex(m, x+w,   y, fz).color(r, g, b, a);
        buf.vertex(m, x+w, y+h, fz).color(r, g, b, a);
        buf.vertex(m,   x, y+h, fz).color(r, g, b, a / 2);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        int da = (int)(a * 0.6f);
        buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(m,   x,   y, z).color(r, g, b, da);
        buf.vertex(m, x+w,   y, z).color(r, g, b, da);
        buf.vertex(m, x+w, y+h, z).color(r, g, b, da);
        buf.vertex(m,   x, y+h, z).color(r, g, b, da / 2);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void matrices_drawBoxRotated(Matrix4f m,
                                         float cx, float cy, float cz,
                                         float w, float h, float d,
                                         float angleDegX,
                                         int r, int gg, int b, int a) {

        float rad = (float) Math.toRadians(angleDegX);
        float dy   =  MathHelper.cos(rad) * h;
        float dz   = -MathHelper.sin(rad) * h;

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float hw = w / 2f;
        buf.vertex(m, cx - hw, cy,      cz     ).color(r, gg, b, a);
        buf.vertex(m, cx + hw, cy,      cz     ).color(r, gg, b, a);
        buf.vertex(m, cx + hw, cy + dy, cz + dz).color(r, gg, b, a / 2);
        buf.vertex(m, cx - hw, cy + dy, cz + dz).color(r, gg, b, a / 2);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}
