package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.util.animations.Easing;
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


public class RegenHearts extends Module {

    private static final Identifier HEART_TEXTURE = darkvisuals.id("hud/heart.png");
    private static final long HEART_TTL_MS = 1100L;

    private final NumberSetting heartsPerRegen =
            new NumberSetting("Сердец за реген", 2f, 1f, 5f, 1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.4f, 0.15f, 1.0f, 0.05f);
    private final NumberSetting riseSpeed =
            new NumberSetting("Скорость подъёма", 0.8f, 0.2f, 2.0f, 0.1f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 90, 110, 255).getRGB());

    private final List<Heart> hearts = new ArrayList<>();
    private final Random rnd = new Random();
    private float lastHealth = -1f;

    public RegenHearts() {
        super("RegenHearts", Category.Render, "Сердечки поднимаются над игроком при восстановлении здоровья");
        getSettings().add(heartsPerRegen);
        getSettings().add(size);
        getSettings().add(riseSpeed);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        hearts.clear();
        lastHealth = -1f;
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        float health = mc.player.getHealth();
        if (lastHealth > 0f && health > lastHealth + 0.01f) {
            int count = (int) (float) heartsPerRegen.getValue();
            for (int i = 0; i < count; i++) {
                hearts.add(new Heart(
                        mc.player.getPos().add(
                                (rnd.nextDouble() - 0.5) * 0.7,
                                1.9 + rnd.nextDouble() * 0.4,
                                (rnd.nextDouble() - 0.5) * 0.7)));
            }
        }
        lastHealth = health;

        Iterator<Heart> it = hearts.iterator();
        while (it.hasNext()) {
            Heart heart = it.next();
            heart.prevPos = heart.pos;
            heart.pos = heart.pos.add(
                    Math.sin((heart.birth % 1000) / 300f + heart.phase) * 0.008,
                    0.025 * riseSpeed.getValue(),
                    0);
            if (System.currentTimeMillis() - heart.birth >= HEART_TTL_MS) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (hearts.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color c = color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, HEART_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        for (Heart heart : hearts) {
            float t = (now - heart.birth) / (float) HEART_TTL_MS;
            // разгорание и мягкое угасание
            float fadeIn = (float) Easing.EASE_OUT_CIRC.apply(Math.min(t * 4f, 1f));
            float fadeOut = 1f - (float) Easing.FAST_IN.apply(Math.max(0f, (t - 0.6f) / 0.4f));
            // лёгкая пульсация сердечка
            float pulse = 1f + 0.12f * MathHelper.sin(now / 120f + heart.phase * 3f);
            int alpha = (int) (220 * fadeIn * fadeOut);
            if (alpha <= 3) continue;

            float half = size.getValue() * pulse * 0.5f;

            matrices.push();
            matrices.translate((float) (heart.pos.x - cam.x), (float) (heart.pos.y - cam.y), (float) (heart.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
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

    private static final class Heart {
        Vec3d pos, prevPos;
        final float phase;
        final long birth = System.currentTimeMillis();

        Heart(Vec3d pos) {
            this.pos = pos;
            this.prevPos = pos;
            this.phase = new Random().nextFloat() * (float) (Math.PI * 2);
        }
    }
}
