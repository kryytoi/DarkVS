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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Butterflies — бабочки, порхающие вокруг игрока днём.
 * Два «крыла» машут (сжимаются по X), траектория — блуждание с синус-покачиванием.
 */
public class Butterflies extends Module {

    private final NumberSetting count =
            new NumberSetting("Количество", 6f, 1f, 16f, 1f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 8f, 3f, 20f, 1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.16f, 0.06f, 0.4f, 0.02f);
    private final ColorSetting colorA =
            new ColorSetting("Цвет 1", new Color(255, 170, 220, 255).getRGB());
    private final ColorSetting colorB =
            new ColorSetting("Цвет 2", new Color(150, 190, 255, 255).getRGB());

    private static final class Butterfly {
        Vec3d pos;
        Vec3d vel;
        final float flapSpeed;
        final float bobPhase;
        final boolean variantB;
        int age;
        int nextTurn;

        Butterfly(Vec3d pos, Vec3d vel, float flapSpeed, float bobPhase, boolean variantB) {
            this.pos = pos;
            this.vel = vel;
            this.flapSpeed = flapSpeed;
            this.bobPhase = bobPhase;
            this.variantB = variantB;
        }
    }

    private final List<Butterfly> butterflies = new ArrayList<>();
    private final Random rnd = new Random();

    public Butterflies() {
        super("Butterflies", Category.Render, "Бабочки, порхающие вокруг игрока");
        getSettings().add(count);
        getSettings().add(radius);
        getSettings().add(size);
        getSettings().add(colorA);
        getSettings().add(colorB);
    }

    @Override
    public void onDisable() {
        butterflies.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        int target = (int) (float) count.getValue();
        float r = radius.getValue();

        while (butterflies.size() < target) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = r * (0.3 + 0.7 * rnd.nextDouble());
            Vec3d pos = mc.player.getPos().add(Math.cos(ang) * dist, 1 + rnd.nextDouble() * 2, Math.sin(ang) * dist);
            Vec3d vel = new Vec3d(rnd.nextDouble() - 0.5, 0, rnd.nextDouble() - 0.5).normalize().multiply(0.12);
            butterflies.add(new Butterfly(pos, vel,
                    0.35f + rnd.nextFloat() * 0.3f,
                    rnd.nextFloat() * (float) Math.PI * 2,
                    rnd.nextBoolean()));
        }

        Iterator<Butterfly> it = butterflies.iterator();
        while (it.hasNext()) {
            Butterfly b = it.next();
            b.age++;

            // случайные повороты направления
            if (b.age >= b.nextTurn) {
                b.nextTurn = b.age + 10 + rnd.nextInt(30);
                b.vel = new Vec3d(rnd.nextDouble() - 0.5, (rnd.nextDouble() - 0.5) * 0.2, rnd.nextDouble() - 0.5)
                        .normalize().multiply(0.12);
            }

            // держимся в радиусе от игрока
            Vec3d toPlayer = mc.player.getPos().add(0, 1.5, 0).subtract(b.pos);
            if (toPlayer.lengthSquared() > r * r) {
                b.vel = toPlayer.normalize().multiply(0.14);
            }

            b.pos = b.pos.add(b.vel.x, b.vel.y + Math.sin(b.age * 0.2 + b.bobPhase) * 0.02, b.vel.z);

            if (b.age > 2400) it.remove(); // время от времени пересоздаём
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (butterflies.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float camYaw = mc.gameRenderer.getCamera().getYaw();
        float camPitch = mc.gameRenderer.getCamera().getPitch();
        Color ca = colorA.getColor();
        Color cb = colorB.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Butterfly b : butterflies) {
            Color base = b.variantB ? cb : ca;
            // машущие крылья: ширина пульсирует
            float flap = (float) Math.abs(Math.sin(b.age * b.flapSpeed));
            float wing = Math.max(0.15f, flap) * size.getValue();
            float body = size.getValue() * 0.35f;

            matrices.push();
            matrices.translate((float) (b.pos.x - cam.x), (float) (b.pos.y - cam.y), (float) (b.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int rgba = base.getRGB();
            // левое крыло
            buffer.vertex(matrix, -wing, -body, 0).color(rgba);
            buffer.vertex(matrix, -body, -body, 0).color(rgba);
            buffer.vertex(matrix, -body, body, 0).color(rgba);
            buffer.vertex(matrix, -wing, body, 0).color(rgba);
            // правое крыло
            buffer.vertex(matrix, body, -body, 0).color(rgba);
            buffer.vertex(matrix, wing, -body, 0).color(rgba);
            buffer.vertex(matrix, wing, body, 0).color(rgba);
            buffer.vertex(matrix, body, body, 0).color(rgba);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
