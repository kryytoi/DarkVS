package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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
 * TorchGlow — тёплое свечение вокруг факелов и других светящихся блоков,
 * с редкими искрами, поднимающимися над ними.
 */
public class TorchGlow extends Module {

    private static final Identifier GLOW_TEXTURE = darkvisuals.id("hud/glow.png");

    private final NumberSetting radius =
            new NumberSetting("Радиус поиска", 7f, 3f, 12f, 1f);
    private final NumberSetting glowSize =
            new NumberSetting("Размер свечения", 0.55f, 0.2f, 1.5f, 0.05f);
    private final NumberSetting minLight =
            new NumberSetting("Мин. светимость", 10f, 1f, 15f, 1f);
    private final BooleanSetting sparks = new BooleanSetting("Искры", true);
    private final ColorSetting glowColor =
            new ColorSetting("Цвет свечения", new Color(255, 170, 80, 255).getRGB());

    private static final class Ember {
        Vec3d pos;
        final long birth;
        final float size;
        final float phase;

        Ember(Vec3d pos, float size, float phase) {
            this.pos = pos;
            this.birth = System.currentTimeMillis();
            this.size = size;
            this.phase = phase;
        }
    }

    private final List<BlockPos> lightSources = new ArrayList<>();
    private final List<Ember> embers = new ArrayList<>();
    private final Random rnd = new Random();
    private int scanCooldown = 0;

    public TorchGlow() {
        super("TorchGlow", Category.Render, "Свечение факелов и светящихся блоков");
        getSettings().add(radius);
        getSettings().add(glowSize);
        getSettings().add(minLight);
        getSettings().add(sparks);
        getSettings().add(glowColor);
    }

    @Override
    public void onDisable() {
        lightSources.clear();
        embers.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        // пересканируем блоки не каждый тик — экономим CPU
        if (--scanCooldown <= 0) {
            scanCooldown = 20;
            lightSources.clear();
            int r = (int) (float) radius.getValue();
            int minLum = (int) (float) minLight.getValue();
            BlockPos center = mc.player.getBlockPos();
            for (BlockPos pos : BlockPos.iterateOutwards(center, r, r / 2 + 2, r)) {
                if (mc.world.getBlockState(pos).getLuminance() >= minLum) {
                    lightSources.add(pos.toImmutable());
                }
            }
        }

        long now = System.currentTimeMillis();
        Iterator<Ember> it = embers.iterator();
        while (it.hasNext()) {
            Ember ember = it.next();
            if (now - ember.birth >= 1400L) {
                it.remove();
                continue;
            }
            double t = (now - ember.birth) / 200.0;
            ember.pos = ember.pos.add(
                    Math.sin(t * 0.8 + ember.phase) * 0.006,
                    0.012 + Math.sin(t + ember.phase) * 0.004,
                    Math.cos(t * 0.7 + ember.phase) * 0.006);
        }

        if (sparks.getValue() && !lightSources.isEmpty() && mc.player.age % 3 == 0) {
            BlockPos pos = lightSources.get(rnd.nextInt(lightSources.size()));
            if (rnd.nextFloat() < 0.35f) {
                embers.add(new Ember(
                        Vec3d.of(pos).add(0.5 + (rnd.nextDouble() - 0.5) * 0.3, 0.4, 0.5 + (rnd.nextDouble() - 0.5) * 0.3),
                        0.4f + rnd.nextFloat() * 0.7f,
                        rnd.nextFloat() * (float) (Math.PI * 2)));
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (lightSources.isEmpty() && embers.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color c = glowColor.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        // мерцающее свечение над источниками света
        for (BlockPos pos : lightSources) {
            double dist = Vec3d.ofCenter(pos).distanceTo(mc.player.getPos());
            float distFade = MathHelper.clamp((float) ((radius.getValue() * 1.4 - dist) / 4f), 0f, 1f);
            float flicker = 0.82f + 0.18f * MathHelper.sin(now / 260f + (pos.getX() * 31 + pos.getZ() * 17) % 10);
            int alpha = (int) (95 * distFade * flicker);
            if (alpha <= 3) continue;

            float half = glowSize.getValue();

            matrices.push();
            matrices.translate((float) (pos.getX() + 0.5 - cam.x), (float) (pos.getY() + 0.55 - cam.y), (float) (pos.getZ() + 0.5 - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
            buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
            buffer.vertex(matrix, half, half, 0).texture(1, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 2);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        // искры-угольки
        for (Ember ember : embers) {
            float lifeT = (now - ember.birth) / 1400f;
            float fadeIn = MathHelper.clamp(lifeT / 0.15f, 0f, 1f);
            float fadeOut = MathHelper.clamp((1f - lifeT) / 0.3f, 0f, 1f);
            int alpha = (int) (200 * fadeIn * fadeOut);
            if (alpha <= 3) continue;

            float half = 0.035f * ember.size;

            matrices.push();
            matrices.translate((float) (ember.pos.x - cam.x), (float) (ember.pos.y - cam.y), (float) (ember.pos.z - cam.z));
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
