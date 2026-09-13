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
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
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


public class FallingLeaves extends Module {

    private final NumberSetting density =
            new NumberSetting("Плотность", 18f, 4f, 60f, 2f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 12f, 4f, 30f, 1f);
    private final NumberSetting fallSpeed =
            new NumberSetting("Скорость падения", 1.0f, 0.3f, 3.0f, 0.1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.18f, 0.08f, 0.5f, 0.02f);
    private final ColorSetting leafColor =
            new ColorSetting("Цвет листьев", new Color(90, 180, 60, 255).getRGB());
    private final ColorSetting autumnColor =
            new ColorSetting("Осенний цвет", new Color(230, 150, 40, 255).getRGB());

    private final Random rnd = new Random();
    private final List<Leaf> leaves = new ArrayList<>();

    public FallingLeaves() {
        super("FallingLeaves", Category.Render, "Падающие листья, кружащиеся вокруг игрока");
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(fallSpeed);
        getSettings().add(size);
        getSettings().add(leafColor);
        getSettings().add(autumnColor);
    }

    @Override
    public void onDisable() {
        leaves.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        while (leaves.size() < (int) (float) density.getValue()) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = radius.getValue() * (0.2 + 0.8 * rnd.nextDouble());
            leaves.add(new Leaf(mc.player.getPos().add(
                    Math.cos(ang) * dist,
                    4 + rnd.nextDouble() * 5,
                    Math.sin(ang) * dist)));
        }

        Iterator<Leaf> it = leaves.iterator();
        while (it.hasNext()) {
            Leaf leaf = it.next();
            leaf.prevPos = leaf.pos;

            // порхание: покачивание по синусоиде + вращение
            leaf.pos = leaf.pos.add(
                    Math.sin(leaf.age * leaf.swaySpeed) * 0.05,
                    -0.03 * fallSpeed.getValue(),
                    Math.cos(leaf.age * leaf.swaySpeed * 0.8) * 0.05);
            leaf.age++;
            leaf.rotY += leaf.rotSpeedY;
            leaf.rotX += leaf.rotSpeedX;

            BlockPos below = BlockPos.ofFloored(leaf.pos).down();
            BlockState ground = mc.world.getBlockState(below);
            boolean landed = !ground.isAir() && leaf.pos.y < below.up().getY() + 0.05;
            if (landed || leaf.pos.y < mc.player.getY() - 12) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (leaves.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Color green = leafColor.getColor();
        Color autumn = autumnColor.getColor();

        for (Leaf leaf : leaves) {
            // мягкое появление
            float fade = MathHelper.clamp(leaf.age / 10f, 0f, 1f);
            Color base = leaf.autumn ? autumn : green;
            int alpha = (int) (220 * fade);
            Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);

            matrices.push();
            matrices.translate((float) (leaf.pos.x - cam.x), (float) (leaf.pos.y - cam.y), (float) (leaf.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(leaf.rotY));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(leaf.rotX));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float h = size.getValue() / 2f;
            // ромбовидный листик
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, 0, -h, 0).color(c.getRGB());
            buffer.vertex(matrix, h, 0, 0).color(c.getRGB());
            buffer.vertex(matrix, 0, h, 0).color(c.getRGB());
            buffer.vertex(matrix, -h, 0, 0).color(c.getRGB());
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static final class Leaf {
        Vec3d pos, prevPos;
        float rotX, rotY;
        final float rotSpeedX, rotSpeedY, swaySpeed;
        final boolean autumn;
        int age;

        Leaf(Vec3d pos) {
            this.pos = pos;
            this.prevPos = pos;
            Random random = new Random();
            this.rotX = random.nextFloat() * 360f;
            this.rotY = random.nextFloat() * 360f;
            this.rotSpeedX = (random.nextFloat() - 0.5f) * 8f;
            this.rotSpeedY = (random.nextFloat() - 0.5f) * 14f;
            this.swaySpeed = 0.05f + random.nextFloat() * 0.06f;
            this.autumn = random.nextInt(3) == 0; // каждый третий лист — осенний
        }
    }
}
