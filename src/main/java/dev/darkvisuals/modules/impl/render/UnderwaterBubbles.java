package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
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
import net.minecraft.util.Identifier;
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
 * UnderwaterBubbles — пузырьки, поднимающиеся вокруг игрока под водой.
 */
public class UnderwaterBubbles extends Module {

    private static final Identifier BUBBLE_TEXTURE = darkvisuals.id("textures/bubble.png");

    private final NumberSetting density =
            new NumberSetting("Плотность", 26f, 4f, 80f, 1f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 2.5f, 1f, 8f, 0.5f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.09f, 0.03f, 0.25f, 0.01f);
    private final NumberSetting riseSpeed =
            new NumberSetting("Скорость", 0.055f, 0.01f, 0.2f, 0.005f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(180, 220, 255, 255).getRGB());

    private static final class Bubble {
        Vec3d pos;
        final float size;
        final float phase;
        final float wobbleSpeed;

        Bubble(Vec3d pos, float size, float phase, float wobbleSpeed) {
            this.pos = pos;
            this.size = size;
            this.phase = phase;
            this.wobbleSpeed = wobbleSpeed;
        }
    }

    private final List<Bubble> bubbles = new ArrayList<>();
    private final Random rnd = new Random();

    public UnderwaterBubbles() {
        super("UnderwaterBubbles", Category.Render, "Пузырьки, поднимающиеся под водой");
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(size);
        getSettings().add(riseSpeed);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        bubbles.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        // вне воды пузырьки исчезают
        if (!mc.player.isSubmergedInWater()) {
            bubbles.clear();
            return;
        }

        int target = (int) (float) density.getValue();
        while (bubbles.size() < target) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = radius.getValue() * Math.sqrt(rnd.nextDouble());
            bubbles.add(new Bubble(
                    mc.player.getPos().add(Math.cos(ang) * dist, -1.2 + rnd.nextDouble() * 1.5, Math.sin(ang) * dist),
                    0.5f + rnd.nextFloat(),
                    rnd.nextFloat() * (float) (Math.PI * 2),
                    1.5f + rnd.nextFloat() * 3f));
        }
        while (bubbles.size() > target) {
            bubbles.remove(bubbles.size() - 1);
        }

        Iterator<Bubble> it = bubbles.iterator();
        while (it.hasNext()) {
            Bubble bubble = it.next();
            double t = mc.player.age / 10.0;
            bubble.pos = bubble.pos.add(
                    Math.sin(t * bubble.wobbleSpeed + bubble.phase) * 0.01,
                    riseSpeed.getValue() * bubble.size,
                    Math.cos(t * bubble.wobbleSpeed * 0.8 + bubble.phase) * 0.01);

            // всплыли к поверхности или уплыли далеко — пересоздаём рядом
            boolean aboveSurface = bubble.pos.y > mc.world.getSeaLevel() + 1
                    && mc.world.getFluidState(net.minecraft.util.math.BlockPos.ofFloored(bubble.pos)).isEmpty();
            if (aboveSurface || bubble.pos.distanceTo(mc.player.getPos()) > radius.getValue() * 2.5) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (bubbles.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color c = color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, BUBBLE_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        for (Bubble bubble : bubbles) {
            // мягкое покачивание размера
            float wobble = 1f + 0.15f * MathHelper.sin(now / 180f + bubble.phase * 4f);
            int alpha = 170;
            float half = size.getValue() * bubble.size * wobble;

            matrices.push();
            matrices.translate((float) (bubble.pos.x - cam.x), (float) (bubble.pos.y - cam.y), (float) (bubble.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
            buffer.vertex(matrix, half, half, 0).texture(1, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
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
