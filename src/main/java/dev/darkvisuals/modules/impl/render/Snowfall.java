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
 * Snowfall — личный снегопад вокруг игрока в любом биоме.
 * Снежинки — billboard-квады, медленно падают с покачиванием.
 */
public class Snowfall extends Module {

    private final NumberSetting density =
            new NumberSetting("Плотность", 24f, 4f, 80f, 2f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 14f, 4f, 32f, 1f);
    private final NumberSetting fallSpeed =
            new NumberSetting("Скорость падения", 1.0f, 0.2f, 3.0f, 0.1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.09f, 0.03f, 0.3f, 0.01f);
    private final BooleanSetting blizzard =
            new BooleanSetting("Метель", false);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(240, 248, 255, 220).getRGB());

    private static final class Flake {
        Vec3d pos;
        final float swaySpeed;
        final float swayPhase;
        int age;

        Flake(Vec3d pos, float swaySpeed, float swayPhase) {
            this.pos = pos;
            this.swaySpeed = swaySpeed;
            this.swayPhase = swayPhase;
        }
    }

    private final List<Flake> flakes = new ArrayList<>();
    private final Random rnd = new Random();

    public Snowfall() {
        super("Snowfall", Category.Render, "Снегопад вокруг игрока в любом биоме");
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(fallSpeed);
        getSettings().add(size);
        getSettings().add(blizzard);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        flakes.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        int target = (int) (float) density.getValue();
        float r = radius.getValue();

        while (flakes.size() < target) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = r * (0.2 + 0.8 * rnd.nextDouble());
            flakes.add(new Flake(
                    mc.player.getPos().add(Math.cos(ang) * dist, 5 + rnd.nextDouble() * 8, Math.sin(ang) * dist),
                    0.04f + rnd.nextFloat() * 0.08f,
                    rnd.nextFloat() * (float) Math.PI * 2));
        }

        float wind = blizzard.getValue() ? 0.12f : 0.03f;
        float speed = fallSpeed.getValue();

        Iterator<Flake> it = flakes.iterator();
        while (it.hasNext()) {
            Flake f = it.next();
            f.age++;
            f.pos = f.pos.add(
                    Math.sin(f.age * f.swaySpeed + f.swayPhase) * wind,
                    -0.045 * speed,
                    Math.cos(f.age * f.swaySpeed * 0.8 + f.swayPhase) * wind);

            if (f.pos.y < mc.player.getY() - 10
                    || f.pos.distanceTo(mc.player.getPos()) > r * 1.6) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (flakes.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float camYaw = mc.gameRenderer.getCamera().getYaw();
        float camPitch = mc.gameRenderer.getCamera().getPitch();
        Color base = color.getColor();
        float half = size.getValue();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Flake f : flakes) {
            float fade = MathHelper.clamp(f.age / 12f, 0f, 1f);
            int alpha = (int) (base.getAlpha() * fade);
            if (alpha <= 4) continue;
            Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);

            matrices.push();
            matrices.translate((float) (f.pos.x - cam.x), (float) (f.pos.y - cam.y), (float) (f.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -half, -half, 0).color(c.getRGB());
            buffer.vertex(matrix, half, -half, 0).color(c.getRGB());
            buffer.vertex(matrix, half, half, 0).color(c.getRGB());
            buffer.vertex(matrix, -half, half, 0).color(c.getRGB());
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
