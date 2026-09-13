package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventRender3D;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;


public class ItemGlow extends Module {

    private static final Identifier GLOW_TEXTURE = darkvisuals.id("hud/glow.png");

    private final NumberSetting range =
            new NumberSetting("Дистанция", 24f, 4f, 64f, 1f);
    private final NumberSetting size =
            new NumberSetting("Размер свечения", 0.8f, 0.3f, 2.0f, 0.1f);
    private final BooleanSetting xpOrbs = new BooleanSetting("XP сферы", true);
    private final BooleanSetting pulse = new BooleanSetting("Пульсация", true);
    private final ColorSetting itemColor =
            new ColorSetting("Цвет предметов", new Color(255, 230, 150, 255).getRGB());
    private final ColorSetting orbColor =
            new ColorSetting("Цвет XP сфер", new Color(120, 255, 140, 255).getRGB());

    public ItemGlow() {
        super("ItemGlow", Category.Render, "Мягкое свечение вокруг выпавших предметов и XP сфер");
        getSettings().add(range);
        getSettings().add(size);
        getSettings().add(xpOrbs);
        getSettings().add(pulse);
        getSettings().add(itemColor);
        getSettings().add(orbColor);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        float maxRange = range.getValue();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        for (Entity entity : mc.world.getEntities()) {
            boolean isItem = entity instanceof ItemEntity;
            boolean isOrb = entity instanceof ExperienceOrbEntity;
            if (!isItem && !(isOrb && xpOrbs.getValue())) continue;

            double dist = entity.getPos().distanceTo(mc.player.getPos());
            if (dist > maxRange) continue;

            // плавное исчезновение на границе дистанции
            float distFade = MathHelper.clamp((float) ((maxRange - dist) / 4f), 0f, 1f);
            // только что заспавненные предметы разгораются
            float ageFade = MathHelper.clamp(entity.age / 8f, 0f, 1f);
            float pulseMod = pulse.getValue()
                    ? 0.8f + 0.2f * MathHelper.sin(now / 350f + entity.getId() * 1.7f)
                    : 1f;

            Color c = isOrb ? orbColor.getColor() : itemColor.getColor();
            int alpha = (int) (120 * distFade * ageFade * pulseMod);
            if (alpha <= 3) continue;

            Vec3d pos = entity.getLerpedPos(event.getTickDelta());
            float glowSize = size.getValue() * (isOrb ? 0.7f : 1f)
                    * (pulse.getValue() ? 0.9f + 0.1f * MathHelper.sin(now / 350f + entity.getId() * 1.7f) : 1f);

            matrices.push();
            matrices.translate((float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = glowSize / 2f;

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, half, 0).texture(1, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
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
