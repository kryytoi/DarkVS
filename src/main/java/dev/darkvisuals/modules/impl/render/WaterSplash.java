package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class WaterSplash extends Module {

    private final NumberSetting range =
            new NumberSetting("Дистанция", 24f, 4f, 64f, 1f);
    private final NumberSetting ringSize =
            new NumberSetting("Размер кольца", 1.0f, 0.4f, 2.5f, 0.1f);
    private final NumberSetting droplets =
            new NumberSetting("Брызги", 8f, 0f, 20f, 1f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(140, 200, 255, 255).getRGB());

    private final Map<Entity, Boolean> wasInWater = new HashMap<>();
    private final List<Splash> splashes = new ArrayList<>();
    private final List<Droplet> dropletsList = new ArrayList<>();
    private final Random rnd = new Random();

    private static final long SPLASH_TTL_MS = 700L;

    public WaterSplash() {
        super("WaterSplash", Category.Render, "Кольца и брызги, когда сущности входят в воду");
        getSettings().add(range);
        getSettings().add(ringSize);
        getSettings().add(droplets);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        wasInWater.clear();
        splashes.clear();
        dropletsList.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        float maxRange = range.getValue();

        // отслеживаем вход в воду
        Map<Entity, Boolean> current = new HashMap<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity.getPos().distanceTo(mc.player.getPos()) > maxRange) continue;
            boolean inWater = entity.isTouchingWater();
            current.put(entity, inWater);

            Boolean before = wasInWater.get(entity);
            if (inWater && (before == null || !before)) {
                spawnSplash(entity);
            }
        }
        wasInWater.clear();
        wasInWater.putAll(current);

        // физика брызг
        Iterator<Droplet> dit = dropletsList.iterator();
        while (dit.hasNext()) {
            Droplet d = dit.next();
            long age = System.currentTimeMillis() - d.birth;
            if (age >= d.life) {
                dit.remove();
                continue;
            }
            d.prevPos = d.pos;
            d.vel = d.vel.add(0, -0.03, 0);
            d.pos = d.pos.add(d.vel);
        }

        long now = System.currentTimeMillis();
        splashes.removeIf(s -> now - s.birth >= SPLASH_TTL_MS);
    }

    private void spawnSplash(Entity entity) {
        // ищем поверхность воды над сущностью
        double surfaceY = entity.getY() + 0.4;
        Vec3d center = new Vec3d(entity.getX(), surfaceY, entity.getZ());
        splashes.add(new Splash(center, (float) MathHelper.clamp(entity.getWidth(), 0.4f, 1.5f)));

        int count = (int) (float) droplets.getValue();
        for (int i = 0; i < count; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double sp = 0.06 + rnd.nextDouble() * 0.12;
            dropletsList.add(new Droplet(center,
                    new Vec3d(Math.cos(ang) * sp, 0.15 + rnd.nextDouble() * 0.2, Math.sin(ang) * sp)));
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (splashes.isEmpty() && dropletsList.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color c = color.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // расходящиеся эллипсы на поверхности
        for (Splash splash : splashes) {
            float t = (now - splash.birth) / (float) SPLASH_TTL_MS;
            float radius = (float) Easing.OUT_QUAD.apply(t) * ringSize.getValue() * splash.width;
            float fade = 1f - t;
            int alpha = (int) (180 * fade);
            if (alpha <= 3 || radius <= 0.01f) continue;

            matrices.push();
            matrices.translate((float) (splash.pos.x - cam.x), (float) (splash.pos.y - cam.y), (float) (splash.pos.z - cam.z));
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 32; i++) {
                float angle = (float) (i * (Math.PI * 2.0) / 32);
                float cos = MathHelper.cos(angle);
                float sin = MathHelper.sin(angle);
                buffer.vertex(matrix, cos * radius * 0.5f, 0, sin * radius * 0.5f).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
                buffer.vertex(matrix, cos * radius, 0, sin * radius).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        // капли брызг
        for (Droplet d : dropletsList) {
            float progress = (now - d.birth) / (float) d.life;
            int alpha = (int) (200 * (1f - progress));
            if (alpha <= 3) continue;

            matrices.push();
            matrices.translate((float) (d.pos.x - cam.x), (float) (d.pos.y - cam.y), (float) (d.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            float half = 0.03f;
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -half, -half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, -half, half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, half, -half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static final class Splash {
        final Vec3d pos;
        final float width;
        final long birth = System.currentTimeMillis();

        Splash(Vec3d pos, float width) {
            this.pos = pos;
            this.width = width;
        }
    }

    private static final class Droplet {
        Vec3d pos, prevPos, vel;
        final long birth = System.currentTimeMillis();
        final long life = 500 + new Random().nextInt(300);

        Droplet(Vec3d pos, Vec3d vel) {
            this.pos = pos;
            this.prevPos = pos;
            this.vel = vel;
        }
    }
}
