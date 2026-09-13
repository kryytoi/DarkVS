package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Footprints — следы, остающиеся на земле при ходьбе.
 * Каждые ~0.55 блока пути оставляем плоский квад, чередуя левую/правую ногу.
 */
public class Footprints extends Module {

    private static final long PRINT_TTL_MS = 4000L;
    private static final float STEP_DISTANCE = 0.55f;

    private final NumberSetting lifetime =
            new NumberSetting("Длительность", 4f, 1f, 10f, 0.5f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.22f, 0.1f, 0.5f, 0.01f);
    private final NumberSetting maxPrints =
            new NumberSetting("Максимум", 60f, 10f, 200f, 10f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(90, 70, 50, 255).getRGB());

    private static final class Footprint {
        final Vec3d pos;
        final float yaw;
        final long birth;
        final float side; // -1 левая, +1 правая

        Footprint(Vec3d pos, float yaw, float side) {
            this.pos = pos;
            this.yaw = yaw;
            this.birth = System.currentTimeMillis();
            this.side = side;
        }
    }

    private final List<Footprint> prints = new ArrayList<>();
    private double stepAccum = 0.0;
    private float nextSide = 1f;

    public Footprints() {
        super("Footprints", Category.Render, "Следы на земле при ходьбе");
        getSettings().add(lifetime);
        getSettings().add(size);
        getSettings().add(maxPrints);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        prints.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        long ttlMs = (long) (lifetime.getValue() * 1000L);
        long now = System.currentTimeMillis();

        Iterator<Footprint> it = prints.iterator();
        while (it.hasNext()) {
            if (now - it.next().birth >= ttlMs) it.remove();
        }

        if (!mc.player.isOnGround()) {
            stepAccum = 0.0;
            return;
        }

        Vec3d vel = mc.player.getVelocity();
        double horizontalSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (horizontalSpeed < 0.08) {
            stepAccum = 0.0;
            return;
        }

        stepAccum += horizontalSpeed;
        if (stepAccum < STEP_DISTANCE) return;
        stepAccum = 0.0;

        // точка на земле чуть позади игрока, со смещением в сторону "ступни"
        float yawRad = (float) Math.toRadians(mc.player.getYaw());
        float side = nextSide;
        nextSide = -nextSide;

        double behind = 0.1;
        double lateral = 0.14;
        double x = mc.player.getX() + Math.cos(yawRad) * behind - Math.sin(yawRad) * lateral * side;
        double z = mc.player.getZ() - Math.sin(yawRad) * behind - Math.cos(yawRad) * lateral * side;

        // ищем поверхность под ногами
        BlockPos feet = BlockPos.ofFloored(x, mc.player.getY() - 0.1, z);
        double surfaceY = mc.player.getY() - 0.02;
        if (!mc.world.getBlockState(feet).isAir()) {
            surfaceY = feet.getY() + 1.02;
        }

        prints.add(new Footprint(new Vec3d(x, surfaceY, z), mc.player.getYaw(), side));
        while (prints.size() > (int) (float) maxPrints.getValue()) {
            prints.remove(0);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (prints.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        long ttlMs = (long) (lifetime.getValue() * 1000L);
        Color c = color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float half = size.getValue();

        for (Footprint print : prints) {
            float lifeT = (now - print.birth) / (float) ttlMs;
            float fadeIn = MathHelper.clamp(lifeT / 0.1f, 0f, 1f);
            float fadeOut = MathHelper.clamp((1f - lifeT) / 0.35f, 0f, 1f);
            int alpha = (int) (150 * fadeIn * fadeOut);
            if (alpha <= 3) continue;

            matrices.push();
            matrices.translate((float) (print.pos.x - cam.x), (float) (print.pos.y - cam.y), (float) (print.pos.z - cam.z));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-print.yaw));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            // плоский квад на земле, слегка вытянутый по направлению движения
            float len = half * 1.5f;
            float wid = half * 0.6f;
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -len / 2f, 0f, -wid / 2f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, -len / 2f, 0f, wid / 2f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, len / 2f, 0f, wid / 2f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, len / 2f, 0f, -wid / 2f).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
