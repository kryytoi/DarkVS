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
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

public class TotemHoldEffect extends Module {

    private static final int   RUNE_COUNT  = 6;
    private static final int   RING_SEGS   = 48;

    private final NumberSetting runeRadius   = new NumberSetting("Радиус рун",   1.1f, 0.4f, 3f,  0.1f);
    private final NumberSetting sparkCount   = new NumberSetting("Искр",          8f,  2f,  24f,  1f);
    private final BooleanSetting lowHpBoost  = new BooleanSetting("Усиление на лоу-хп", true);
    private final BooleanSetting rainbow     = new BooleanSetting("Радуга",       false);
    private final ColorSetting  colorSetting = new ColorSetting("Цвет", new Color(255, 200, 50, 220).getRGB());

    private static final class Spark {
        double x, z;
        float  phase;
        float  speed;
        float  yOff;
        Spark(double x, double z, float phase, float speed) {
            this.x = x; this.z = z; this.phase = phase; this.speed = speed; this.yOff = 0f;
        }
    }

    private final java.util.List<Spark> sparks = new java.util.ArrayList<>();
    private long lastSpawnMs = 0L;

    public TotemHoldEffect() {
        super("TotemHoldEffect", Category.Render, "Аура вокруг игрока при держании тотема в руке");
        getSettings().add(runeRadius);
        getSettings().add(sparkCount);
        getSettings().add(lowHpBoost);
        getSettings().add(rainbow);
        getSettings().add(colorSetting);
    }

    @Override
    public void onDisable() {
        sparks.clear();
        super.onDisable();
    }

    private boolean holdsTotem() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)
                || mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck() || !holdsTotem()) {
            sparks.clear();
            return;
        }

        long now = System.currentTimeMillis();

        sparks.removeIf(s -> s.yOff > 2.5f);

        for (Spark s : sparks) s.yOff += s.speed;

        int cnt = (int)(float) sparkCount.getValue();
        long interval = 1200 / Math.max(1, cnt);
        if (now - lastSpawnMs > interval) {
            lastSpawnMs = now;
            float ang = (float)(Math.random() * Math.PI * 2);
            float r   = runeRadius.getValue() * (0.7f + (float)Math.random() * 0.3f);
            sparks.add(new Spark(
                    mc.player.getX() + Math.cos(ang) * r,
                    mc.player.getZ() + Math.sin(ang) * r,
                    (float)(Math.random() * Math.PI * 2),
                    0.025f + (float)(Math.random() * 0.03f)));
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck() || !holdsTotem()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam  = mc.gameRenderer.getCamera().getPos();
        long  now  = System.currentTimeMillis();

        float hpRatio = mc.player.getHealth() / mc.player.getMaxHealth();
        float boost   = lowHpBoost.getValue()
                ? 1f + MathHelper.clamp((1f - hpRatio) * 1.5f, 0f, 1.2f)
                : 1f;

        Color c = getColor(now);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Vec3d ppos = mc.player.getPos();

        float pulse = 0.88f + 0.12f * MathHelper.sin(now / 350f);
        float ringR = runeRadius.getValue() * pulse * boost;
        int   ringA = (int)(160 * boost * pulse);

        matrices.push();
        matrices.translate((float)(ppos.x - cam.x), (float)(ppos.y - cam.y + 0.05f), (float)(ppos.z - cam.z));
        Matrix4f m = matrices.peek().getPositionMatrix();
        drawRing(m, c, Math.min(255, ringA), ringR, 0.035f);

        drawRing(m, c, Math.min(255, ringA / 2), ringR * 0.6f, 0.02f);
        matrices.pop();

        float spin = (now / 2000f) % (float)(Math.PI * 2);
        for (int i = 0; i < RUNE_COUNT; i++) {
            float ang  = spin + i * (float)(Math.PI * 2 / RUNE_COUNT);
            float runeX = (float)(ppos.x + Math.cos(ang) * runeRadius.getValue());
            float runeZ = (float)(ppos.z + Math.sin(ang) * runeRadius.getValue());
            float runeY = (float)(ppos.y + 1f + 0.15f * MathHelper.sin(now / 600f + i));

            matrices.push();
            matrices.translate(runeX - (float)cam.x, runeY - (float)cam.y, runeZ - (float)cam.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)Math.toDegrees(ang) + 90f));

            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));

            float sz = 0.15f * boost;
            Matrix4f rm = matrices.peek().getPositionMatrix();
            int ra = Math.min(255, (int)(200 * boost));

            BufferBuilder buf = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buf.vertex(rm, 0f, sz, 0f).color(c.getRed(), c.getGreen(), c.getBlue(), ra);
            for (int k = 0; k <= 3; k++) {
                float a = (float)(k * Math.PI * 2 / 3);
                buf.vertex(rm, MathHelper.cos(a) * sz, 0f, MathHelper.sin(a) * sz)
                   .color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
            matrices.pop();
        }

        for (Spark s : sparks) {
            float lifeT  = s.yOff / 2.5f;
            float fadeO  = MathHelper.clamp((1f - lifeT) / 0.4f, 0f, 1f);
            int   sAlpha = (int)(200 * fadeO * boost);
            if (sAlpha <= 3) continue;

            float twinkle = 0.7f + 0.3f * MathHelper.sin(now / 120f + s.phase * 5f);
            sAlpha = (int)(sAlpha * twinkle);

            matrices.push();
            matrices.translate((float)(s.x - cam.x), (float)(ppos.y - cam.y + s.yOff), (float)(s.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            Matrix4f sm = matrices.peek().getPositionMatrix();
            float half  = 0.035f * boost;
            BufferBuilder buf = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buf.vertex(sm, -half, -half, 0f).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            buf.vertex(sm, -half,  half, 0f).color(c.getRed(), c.getGreen(), c.getBlue(), sAlpha / 2);
            buf.vertex(sm,  half,  half, 0f).color(c.getRed(), c.getGreen(), c.getBlue(), sAlpha);
            buf.vertex(sm,  half, -half, 0f).color(c.getRed(), c.getGreen(), c.getBlue(), sAlpha / 2);
            BufferRenderer.drawWithGlobalProgram(buf.end());
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawRing(Matrix4f m, Color c, int alpha, float radius, float thickness) {
        if (alpha <= 3 || radius <= 0.01f) return;
        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < RING_SEGS; i++) {
            double a0 = i * Math.PI * 2 / RING_SEGS;
            double a1 = (i + 1) * Math.PI * 2 / RING_SEGS;
            float x0 = (float)Math.cos(a0), z0 = (float)Math.sin(a0);
            float x1 = (float)Math.cos(a1), z1 = (float)Math.sin(a1);
            buf.vertex(m, x0*(radius-thickness), 0f, z0*(radius-thickness)).color(c.getRed(),c.getGreen(),c.getBlue(),alpha/2);
            buf.vertex(m, x0*(radius+thickness), 0f, z0*(radius+thickness)).color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
            buf.vertex(m, x1*(radius+thickness), 0f, z1*(radius+thickness)).color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
            buf.vertex(m, x1*(radius-thickness), 0f, z1*(radius-thickness)).color(c.getRed(),c.getGreen(),c.getBlue(),alpha/2);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private Color getColor(long now) {
        if (rainbow.getValue()) {
            float hue = (float)((now / 3000.0) % 1.0);
            return new Color(Color.HSBtoRGB(hue, 0.65f, 1f));
        }
        return colorSetting.getColor();
    }
}
