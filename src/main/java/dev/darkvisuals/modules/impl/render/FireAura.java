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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class FireAura extends Module {

    // время жизни одного уголька
    private static final long EMBER_TTL_MS = 1800L;

    private final NumberSetting density =
            new NumberSetting("Плотность", 5f, 0f, 20f, 1f);
    private final NumberSetting riseSpeed =
            new NumberSetting("Скорость подъёма", 1.0f, 0.2f, 3.0f, 0.1f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 1.2f, 0.3f, 4.0f, 0.1f);
    private final NumberSetting flicker =
            new NumberSetting("Мерцание", 1.0f, 0f, 2.0f, 0.1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.09f, 0.02f, 0.3f, 0.01f);

    private final List<Ember> embers = new ArrayList<>();
    private final Random rnd = new Random();

    public FireAura() {
        super("FireAura", Category.Render, "Огненная аура: угольки поднимаются вокруг игрока");
        getSettings().add(density);
        getSettings().add(riseSpeed);
        getSettings().add(radius);
        getSettings().add(flicker);
        getSettings().add(size);
    }

    @Override
    public void onDisable() {
        embers.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        // спавним угольки у ног игрока
        int count = (int) (float) density.getValue();
        for (int i = 0; i < count; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = radius.getValue() * (0.2 + 0.8 * rnd.nextDouble());
            embers.add(new Ember(mc.player.getPos().add(
                    Math.cos(ang) * dist,
                    -0.4 + rnd.nextDouble() * 0.6,
                    Math.sin(ang) * dist)));
        }

        Iterator<Ember> it = embers.iterator();
        while (it.hasNext()) {
            Ember e = it.next();
            e.age++;
            // ускоряемся вверх и немного колышемся в стороны
            e.vel = e.vel.add(0, 0.006 * riseSpeed.getValue(), 0);
            e.vel = new Vec3d(
                    e.vel.x + (rnd.nextDouble() - 0.5) * 0.004 * riseSpeed.getValue(),
                    e.vel.y,
                    e.vel.z + (rnd.nextDouble() - 0.5) * 0.004 * riseSpeed.getValue());
            e.vel = new Vec3d(e.vel.x * 0.98, e.vel.y * 0.995, e.vel.z * 0.98);
            e.pos = e.pos.add(e.vel);

            if (System.currentTimeMillis() - e.birth >= e.ttl
                    || e.pos.y - mc.player.getPos().y > 3.5) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (embers.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Ember e : embers) {
            float lifeT = (now - e.birth) / (float) e.ttl;
            // угольки ярко вспыхивают и гаснут по мере подъёма
            float fade = MathHelper.clamp(1f - lifeT, 0f, 1f);
            float ignite = MathHelper.clamp(lifeT / 0.1f, 0f, 1f);
            // дрожание пламени
            float flick = 1f - flicker.getValue() * 0.4f * MathHelper.abs(MathHelper.sin(now / 120f + e.phase * 3f));
            int alpha = (int) (230 * fade * ignite * flick);
            if (alpha <= 3) continue;

            // смесь оранжевого и красного в зависимости от возраста уголька
            float hot = MathHelper.clamp(1f - lifeT * 1.4f, 0f, 1f);
            int r = (int) (255);
            int g = (int) (60 + 110 * hot);
            int b = (int) (20 + 40 * hot * hot);

            matrices.push();
            matrices.translate((float) (e.pos.x - cam.x), (float) (e.pos.y - cam.y), (float) (e.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = size.getValue() * (0.6f + 0.6f * flick) * (0.5f + 0.5f * fade);
            int mid = alpha / 2;

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -half, -half, 0).color(r, g, b, 0);
            buffer.vertex(matrix, -half, half, 0).color(r, g, b, mid);
            buffer.vertex(matrix, half, half, 0).color(r, g, b, alpha);
            buffer.vertex(matrix, half, -half, 0).color(r, g, b, mid);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static final class Ember {
        Vec3d pos;
        Vec3d vel;
        final float phase;
        final long birth = System.currentTimeMillis();
        final long ttl;
        int age;

        Ember(Vec3d pos) {
            this.pos = pos;
            Random random = new Random();
            this.vel = new Vec3d(
                    (random.nextDouble() - 0.5) * 0.03,
                    0.02 + random.nextDouble() * 0.03,
                    (random.nextDouble() - 0.5) * 0.03);
            this.phase = random.nextFloat() * (float) (Math.PI * 2);
            this.ttl = EMBER_TTL_MS + (long) (random.nextDouble() * 900L);
        }
    }
}
