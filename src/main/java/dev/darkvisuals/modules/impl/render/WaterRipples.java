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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class WaterRipples extends Module {

    private static final int SEGMENTS = 28;

    private final NumberSetting maxRings =
            new NumberSetting("Максимум колец", 6f, 1f, 16f, 1f);
    private final NumberSetting expandSpeed =
            new NumberSetting("Скорость расширения", 0.35f, 0.05f, 1.5f, 0.05f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 2.0f, 0.5f, 8.0f, 0.25f);
    private final NumberSetting alphaSetting =
            new NumberSetting("Прозрачность", 0.7f, 0.05f, 1.0f, 0.05f);
    private final BooleanSetting onlyMoving =
            new BooleanSetting("Только в движении", false);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(120, 190, 255, 255).getRGB());

    private final List<Ripple> ripples = new ArrayList<>();
    private long lastSpawn = 0L;

    public WaterRipples() {
        super("WaterRipples", Category.Render, "Расходящиеся круги на воде под игроком");
        getSettings().add(maxRings);
        getSettings().add(expandSpeed);
        getSettings().add(radius);
        getSettings().add(alphaSetting);
        getSettings().add(onlyMoving);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        ripples.clear();
        super.onDisable();
    }

    private boolean isMoving() {
        return mc.player.getVelocity().horizontalLength() > 0.06;
    }

    // возвращает Y поверхности воды под игроком, или -1, если воды нет
    private double getWaterSurfaceY() {
        Vec3d pos = mc.player.getPos();
        BlockPos floor = BlockPos.ofFloored(pos);
        if (mc.world.getFluidState(floor).isStill()) {
            return floor.getY() + 1.0;
        }
        BlockPos below = floor.down();
        if (mc.world.getFluidState(below).isStill()) {
            return below.getY() + 1.0;
        }
        return -1.0;
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        // удаляем старые круги и следим за лимитом
        long now = System.currentTimeMillis();
        Iterator<Ripple> it = ripples.iterator();
        while (it.hasNext()) {
            Ripple r = it.next();
            if ((now - r.birth) / 1000.0 * expandSpeed.getValue() > radius.getValue()
                    || (now - r.birth) > 4000L) {
                it.remove();
            }
        }
        while (ripples.size() > (int) (float) maxRings.getValue()) {
            ripples.remove(0);
        }

        // стоим ли на воде
        double surfaceY = getWaterSurfaceY();
        if (surfaceY < 0) return;
        if (Math.abs(mc.player.getY() - surfaceY) > 1.2) return;
        if (onlyMoving.getValue() && !isMoving()) return;

        // новый круг раз в 350 мс
        if (now - lastSpawn < 350L) return;
        lastSpawn = now;
        ripples.add(new Ripple(mc.player.getX(), mc.player.getZ(), surfaceY, now));
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (ripples.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color c = color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Ripple r : ripples) {
            float ageSec = (now - r.birth) / 1000.0f;
            float ringR = ageSec * expandSpeed.getValue();
            // плавное появление и затухание
            float fadeIn = MathHelper.clamp(ageSec / 0.3f, 0f, 1f);
            float fadeOut = MathHelper.clamp((radius.getValue() - ringR) / radius.getValue(), 0f, 1f);
            int alpha = (int) (255 * alphaSetting.getValue() * fadeIn * fadeOut);
            if (alpha <= 3 || ringR <= 0.02f) continue;

            float inner = ringR - 0.05f;
            float outer = ringR + 0.05f;
            float lx = (float) (r.x - cam.x);
            float lz = (float) (r.z - cam.z);
            float ly = (float) (r.y - cam.y) + 0.01f;

            matrices.push();
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < SEGMENTS; i++) {
                double a0 = (double) i / SEGMENTS * Math.PI * 2;
                double a1 = (double) (i + 1) / SEGMENTS * Math.PI * 2;
                buffer.vertex(matrix, lx + (float) Math.cos(a0) * inner, ly, lz + (float) Math.sin(a0) * inner)
                        .color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, lx + (float) Math.cos(a0) * outer, ly, lz + (float) Math.sin(a0) * outer)
                        .color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, lx + (float) Math.cos(a1) * outer, ly, lz + (float) Math.sin(a1) * outer)
                        .color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, lx + (float) Math.cos(a1) * inner, ly, lz + (float) Math.sin(a1) * inner)
                        .color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static final class Ripple {
        final double x, z, y;
        final long birth;

        Ripple(double x, double z, double y, long birth) {
            this.x = x;
            this.z = z;
            this.y = y;
            this.birth = birth;
        }
    }
}
