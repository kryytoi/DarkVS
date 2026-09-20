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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * CherryBlossom — лепестки сакуры, кружящиеся вокруг игрока.
 * Маленькие розово-белые квады медленно падают, качаются
                и имитируют вращение через осцилляцию ширины билборда.
 */
public class CherryBlossom extends Module {

    // оттенки лепестков: от белого до насыщенно-розового
    private static final Color[] PETAL_COLORS = {
            new Color(255, 240, 245),
            new Color(255, 214, 224),
            new Color(255, 182, 193),
            new Color(255, 150, 178),
            new Color(255, 198, 212)
    };

    private final NumberSetting density =
            new NumberSetting("Плотность", 26f, 4f, 80f, 2f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 14f, 4f, 32f, 1f);
    private final NumberSetting fallSpeed =
            new NumberSetting("Скорость падения", 1.0f, 0.2f, 3f, 0.1f);
    private final NumberSetting sway =
            new NumberSetting("Покачивание", 1.0f, 0f, 2f, 0.1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.16f, 0.05f, 0.4f, 0.01f);

    private final Random rnd = new Random();
    private final List<Petal> petals = new ArrayList<>();

    public CherryBlossom() {
        super("CherryBlossom", Category.Render, "Падающие лепестки сакуры вокруг игрока");
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(fallSpeed);
        getSettings().add(sway);
        getSettings().add(size);
    }

    @Override
    public void onDisable() {
        petals.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        int target = (int) (float) density.getValue();
        float r = radius.getValue();

        while (petals.size() < target) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = r * (0.2 + 0.8 * rnd.nextDouble());
            petals.add(new Petal(
                    mc.player.getPos().add(Math.cos(ang) * dist, 5 + rnd.nextDouble() * 8, Math.sin(ang) * dist),
                    PETAL_COLORS[rnd.nextInt(PETAL_COLORS.length)],
                    0.035f + rnd.nextFloat() * 0.07f,
                    rnd.nextFloat() * (float) Math.PI * 2,
                    0.05f + rnd.nextFloat() * 0.09f,
                    rnd.nextFloat() * (float) Math.PI * 2,
                    0.8f + rnd.nextFloat() * 0.5f));
        }

        float speed = fallSpeed.getValue();
        float swayAmount = sway.getValue();

        Iterator<Petal> it = petals.iterator();
        while (it.hasNext()) {
            Petal p = it.next();
            p.age++;
            // падение и зигзугообразное скольжение по ветру
            p.pos = p.pos.add(
                    Math.sin(p.age * p.swaySpeed + p.swayPhase) * 0.012 * swayAmount,
                    -0.028 * speed,
                    Math.cos(p.age * p.swaySpeed * 0.8 + p.swayPhase) * 0.012 * swayAmount);

            if (p.pos.y < mc.player.getY() - 10
                    || p.pos.distanceTo(mc.player.getPos()) > r * 1.7) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (petals.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float camYaw = mc.gameRenderer.getCamera().getYaw();
        float camPitch = mc.gameRenderer.getCamera().getPitch();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float base = size.getValue();

        for (Petal p : petals) {
            // плавное появление лепестка
            float fade = Math.min(p.age / 12f, 1f);
            // имитация вращения: ширина лепестка колеблется
            float spin = (float) Math.sin(p.age * p.rotSpeed + p.rotPhase);
            float halfW = base * p.sizeScale * (0.35f + 0.65f * Math.abs(spin));
            float halfH = base * p.sizeScale * (0.85f + 0.15f * spin);

            int alpha = (int) (230 * fade);
            if (alpha <= 4) continue;
            Color c = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha);

            matrices.push();
            matrices.translate((float) (p.pos.x - cam.x), (float) (p.pos.y - cam.y), (float) (p.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -halfW, -halfH, 0).color(c.getRGB());
            buffer.vertex(matrix, halfW, -halfH, 0).color(c.getRGB());
            buffer.vertex(matrix, halfW, halfH, 0).color(c.getRGB());
            buffer.vertex(matrix, -halfW, halfH, 0).color(c.getRGB());
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static final class Petal {
        Vec3d pos;
        final Color color;
        final float swaySpeed;
        final float swayPhase;
        final float rotSpeed;
        final float rotPhase;
        final float sizeScale;
        int age;

        Petal(Vec3d pos, Color color, float swaySpeed, float swayPhase,
              float rotSpeed, float rotPhase, float sizeScale) {
            this.pos = pos;
            this.color = color;
            this.swaySpeed = swaySpeed;
            this.swayPhase = swayPhase;
            this.rotSpeed = rotSpeed;
            this.rotPhase = rotPhase;
            this.sizeScale = sizeScale;
        }
    }
}
