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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class OrbitingOrbs extends Module {

    private final NumberSetting count =
            new NumberSetting("Количество", 3f, 1f, 8f, 1f);
    private final NumberSetting orbitRadius =
            new NumberSetting("Радиус орбиты", 1.5f, 0.5f, 5.0f, 0.1f);
    private final NumberSetting orbitSpeed =
            new NumberSetting("Скорость орбиты", 1.0f, 0.1f, 4.0f, 0.1f);
    private final NumberSetting heightOffset =
            new NumberSetting("Высота", 1.0f, 0f, 3.0f, 0.1f);
    private final NumberSetting glowSize =
            new NumberSetting("Размер свечения", 0.22f, 0.05f, 0.8f, 0.05f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(140, 190, 255, 255).getRGB());

    private final List<Orb> orbs = new ArrayList<>();

    public OrbitingOrbs() {
        super("OrbitingOrbs", Category.Render, "Светящиеся сферы, вращающиеся вокруг игрока");
        getSettings().add(count);
        getSettings().add(orbitRadius);
        getSettings().add(orbitSpeed);
        getSettings().add(heightOffset);
        getSettings().add(glowSize);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        orbs.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        // подгоняем количество орбов под настройку
        while (orbs.size() < (int) (float) count.getValue()) {
            orbs.add(new Orb(orbs.size()));
        }
        while (orbs.size() > (int) (float) count.getValue()) {
            orbs.remove(orbs.size() - 1);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (orbs.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        double time = System.currentTimeMillis() / 1000.0;
        Color c = color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        double px = mc.player.getX();
        double py = mc.player.getY() + heightOffset.getValue();
        double pz = mc.player.getZ();
        float yaw = -mc.gameRenderer.getCamera().getYaw();
        float pitch = mc.gameRenderer.getCamera().getPitch();

        for (Orb orb : orbs) {
            double angle = time * orbitSpeed.getValue() * orb.direction + orb.phase;
            double height = py + orb.heightOffset + Math.sin(time * 0.8 + orb.phase) * 0.25;

            // ядро орба
            drawOrb(matrices, cam, px + Math.cos(angle) * orbitRadius.getValue(),
                    height, pz + Math.sin(angle) * orbitRadius.getValue(),
                    glowSize.getValue(), c, 1f, yaw, pitch);

            // лёгкий шлейф из призрачных позиций
            for (int i = 1; i <= 5; i++) {
                double trailAngle = angle - i * 0.11 * orbitSpeed.getValue() * orb.direction;
                drawOrb(matrices, cam, px + Math.cos(trailAngle) * orbitRadius.getValue(),
                        height, pz + Math.sin(trailAngle) * orbitRadius.getValue(),
                        glowSize.getValue() * (1f - i * 0.15f), c, 0.55f - i * 0.1f, yaw, pitch);
            }
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawOrb(MatrixStack matrices, Vec3d cam, double x, double y, double z,
                         float size, Color c, float intensity, float yaw, float pitch) {
        // пульсация свечения
        float pulse = 0.75f + 0.25f * MathHelper.sin((float) (System.currentTimeMillis() / 600f));
        int alpha = (int) (200 * intensity * pulse);
        if (alpha <= 3 || size <= 0.01f) return;

        matrices.push();
        matrices.translate((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = size / 2f;
        int mid = alpha / 3;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        // мягкое свечение: прозрачная середина и яркие края
        buffer.vertex(matrix, -half, -half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        buffer.vertex(matrix, -half, half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), mid);
        buffer.vertex(matrix, half, half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        buffer.vertex(matrix, half, -half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), mid);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    private static final class Orb {
        final float phase;
        final double direction;
        final double heightOffset;

        Orb(int index) {
            Random random = new Random();
            this.phase = random.nextFloat() * (float) (Math.PI * 2);
            this.direction = index % 2 == 0 ? 1.0 : -1.0;
            this.heightOffset = (random.nextDouble() - 0.5) * 0.8;
        }
    }
}
