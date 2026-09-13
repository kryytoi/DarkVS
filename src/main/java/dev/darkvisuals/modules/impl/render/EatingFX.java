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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * EatingFX — крошки и кусочки еды, летящие изо рта при поедании.
 */
public class EatingFX extends Module {

    private static final long CRUMB_TTL_MS = 700L;

    private final NumberSetting density =
            new NumberSetting("Плотность", 8f, 2f, 24f, 1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.05f, 0.02f, 0.12f, 0.005f);

    // тёплая «еда»-палитра
    private static final Color[] CRUMB_COLORS = {
            new Color(200, 140, 70), new Color(170, 110, 55),
            new Color(225, 180, 100), new Color(150, 95, 60)
    };

    private static final class Crumb {
        Vec3d pos;
        final Vec3d velocity;
        final long birth;
        final float size;
        final Color color;
        final float phase;

        Crumb(Vec3d pos, Vec3d velocity, float size, Color color, float phase) {
            this.pos = pos;
            this.velocity = velocity;
            this.birth = System.currentTimeMillis();
            this.size = size;
            this.color = color;
            this.phase = phase;
        }
    }

    private final List<Crumb> crumbs = new ArrayList<>();
    private final Random rnd = new Random();

    public EatingFX() {
        super("EatingFX", Category.Render, "Крошки при поедании еды");
        getSettings().add(density);
        getSettings().add(size);
    }

    @Override
    public void onDisable() {
        crumbs.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        Iterator<Crumb> it = crumbs.iterator();
        while (it.hasNext()) {
            Crumb crumb = it.next();
            if (now - crumb.birth >= CRUMB_TTL_MS) {
                it.remove();
                continue;
            }
            crumb.pos = crumb.pos.add(crumb.velocity);
        }

        if (!mc.player.isUsingItem()) return;

        // точка «рта» — перед лицом игрока
        float yawRad = (float) Math.toRadians(mc.player.getYaw());
        float pitchRad = (float) Math.toRadians(mc.player.getPitch());
        Vec3d look = new Vec3d(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad));
        Vec3d mouth = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()) - 0.25, 0).add(look.multiply(0.35));

        if (mc.player.age % 2 != 0) return;
        int count = (int) (float) density.getValue() / 4 + 1;
        for (int i = 0; i < count && crumbs.size() < density.getValue(); i++) {
            Vec3d vel = look.multiply(0.06)
                    .add((rnd.nextDouble() - 0.5) * 0.05, -rnd.nextDouble() * 0.03, (rnd.nextDouble() - 0.5) * 0.05);
            crumbs.add(new Crumb(
                    mouth.add((rnd.nextDouble() - 0.5) * 0.15, (rnd.nextDouble() - 0.5) * 0.1, (rnd.nextDouble() - 0.5) * 0.15),
                    vel,
                    0.6f + rnd.nextFloat() * 0.8f,
                    CRUMB_COLORS[rnd.nextInt(CRUMB_COLORS.length)],
                    rnd.nextFloat() * (float) (Math.PI * 2)));
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (crumbs.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Crumb crumb : crumbs) {
            float lifeT = (now - crumb.birth) / (float) CRUMB_TTL_MS;
            float fadeIn = MathHelper.clamp(lifeT / 0.1f, 0f, 1f);
            float fadeOut = MathHelper.clamp((1f - lifeT) / 0.4f, 0f, 1f);
            float spin = MathHelper.sin(now / 90f + crumb.phase) * 0.5f;
            int alpha = (int) (230 * fadeIn * fadeOut);
            if (alpha <= 3) continue;

            matrices.push();
            matrices.translate((float) (crumb.pos.x - cam.x), (float) (crumb.pos.y - cam.y), (float) (crumb.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch() + spin * 20f));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = size.getValue() * crumb.size;
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -half, -half, 0).color(crumb.color.getRed(), crumb.color.getGreen(), crumb.color.getBlue(), alpha);
            buffer.vertex(matrix, -half, half, 0).color(crumb.color.getRed(), crumb.color.getGreen(), crumb.color.getBlue(), alpha);
            buffer.vertex(matrix, half, half, 0).color(crumb.color.getRed(), crumb.color.getGreen(), crumb.color.getBlue(), alpha);
            buffer.vertex(matrix, half, -half, 0).color(crumb.color.getRed(), crumb.color.getGreen(), crumb.color.getBlue(), alpha);
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
