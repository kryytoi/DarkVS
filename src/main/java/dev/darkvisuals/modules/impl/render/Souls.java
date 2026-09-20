package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.darkvisuals;
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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static dev.darkvisuals.client.util.Wrapper.mc;

/**
 * Души — полупрозрачные призрачные огоньки, медленно дрейфующие вокруг
 * игрока. Каждая душа — объёмная дрожащая икосфера (настоящая 3D-геометрия,
 * а не плоский спрайт), медленно вращающаяся и перекатывающаяся волнами.
 * Рисуется с аддитивным блендингом (как души в TargetEsp), цвет берётся из
 * активной темы и обновляется каждый кадр, поэтому плавно переливается на
 * градиентных темах.
 */
public class Souls extends Module {

    private static final long SOUL_TTL_MS = 9000L;

    // ── Единичная икосфера: общая база для всех душ, строится один раз ─────────
    private static final float[] ICO_V;   // координаты вершин на единичной сфере
    private static final int[] ICO_I;      // индексы треугольников

    static {
        List<float[]> verts = new ArrayList<>();
        double t = (1.0 + Math.sqrt(5.0)) / 2.0;
        verts.add(nrm(-1, t, 0));   verts.add(nrm(1, t, 0));
        verts.add(nrm(-1, -t, 0));  verts.add(nrm(1, -t, 0));
        verts.add(nrm(0, -1, t));   verts.add(nrm(0, 1, t));
        verts.add(nrm(0, -1, -t));  verts.add(nrm(0, 1, -t));
        verts.add(nrm(t, 0, -1));   verts.add(nrm(t, 0, 1));
        verts.add(nrm(-t, 0, -1));  verts.add(nrm(-t, 0, 1));

        int[][] faces = {
                {0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},
                {1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},
                {3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},
                {4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}
        };
        // два деления: 20 → 80 треугольников, 42 вершины
        Map<Long, Integer> midCache = new HashMap<>();
        for (int level = 0; level < 2; level++) {
            int[][] nf = new int[faces.length * 4][];
            int n = 0;
            for (int[] f : faces) {
                int a = midpoint(verts, midCache, f[0], f[1]);
                int b = midpoint(verts, midCache, f[1], f[2]);
                int c = midpoint(verts, midCache, f[2], f[0]);
                nf[n++] = new int[]{f[0], a, c};
                nf[n++] = new int[]{f[1], b, a};
                nf[n++] = new int[]{f[2], c, b};
                nf[n++] = new int[]{a, b, c};
            }
            faces = nf;
        }
        ICO_V = new float[verts.size() * 3];
        for (int i = 0; i < verts.size(); i++) {
            float[] v = verts.get(i);
            ICO_V[i * 3] = v[0];
            ICO_V[i * 3 + 1] = v[1];
            ICO_V[i * 3 + 2] = v[2];
        }
        ICO_I = new int[faces.length * 3];
        for (int i = 0; i < faces.length; i++) {
            ICO_I[i * 3] = faces[i][0];
            ICO_I[i * 3 + 1] = faces[i][1];
            ICO_I[i * 3 + 2] = faces[i][2];
        }
    }

    private static float[] nrm(double x, double y, double z) {
        double len = Math.sqrt(x * x + y * y + z * z);
        return new float[]{(float) (x / len), (float) (y / len), (float) (z / len)};
    }

    /** Середина ребра, спроецированная на единичную сферу (с кэшем по ребру). */
    private static int midpoint(List<float[]> verts, Map<Long, Integer> cache, int i, int j) {
        long key = ((long) Math.min(i, j) << 32) | Math.max(i, j);
        Integer cached = cache.get(key);
        if (cached != null) return cached;
        float[] a = verts.get(i), b = verts.get(j);
        float x = (a[0] + b[0]) * 0.5f, y = (a[1] + b[1]) * 0.5f, z = (a[2] + b[2]) * 0.5f;
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        int idx = verts.size();
        verts.add(new float[]{x / len, y / len, z / len});
        cache.put(key, idx);
        return idx;
    }

    private final NumberSetting count   = new NumberSetting("Количество", 14, 1, 40, 1);
    private final NumberSetting radius  = new NumberSetting("Радиус", 10, 3, 30, 1);
    private final NumberSetting size    = new NumberSetting("Размер", 0.55f, 0.1f, 2.0f, 0.05f);
    private final NumberSetting speed   = new NumberSetting("Скорость", 1.0f, 0.1f, 3.0f, 0.1f);
    private final NumberSetting alpha   = new NumberSetting("Прозрачность", 0.6f, 0.05f, 1.0f, 0.05f);
    private final BooleanSetting glow   = new BooleanSetting("Свечение", true);
    private final BooleanSetting themeColor = new BooleanSetting("Цвет темы", true);
    private final ColorSetting customColor = new ColorSetting("Свой цвет", new Color(180, 120, 255, 255).getRGB());

    private final List<Soul> souls = new ArrayList<>();
    private final Random rnd = new Random();

    private final ThemeManager themeManager = ThemeManager.getInstance();

    public Souls() {
        super("Souls", Category.Render, "Призрачные души, парящие вокруг игрока");
        getSettings().add(count);
        getSettings().add(radius);
        getSettings().add(size);
        getSettings().add(speed);
        getSettings().add(alpha);
        getSettings().add(glow);
        getSettings().add(themeColor);
        getSettings().add(customColor);
        customColor.setVisible(() -> !themeColor.getValue());
    }

    @Override
    public void onDisable() {
        souls.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        float r = radius.getValue().floatValue();

        // поддерживаем популяцию душ
        while (souls.size() < count.getValue().intValue()) {
            souls.add(new Soul(mc.player.getPos().add(
                    (rnd.nextDouble() - 0.5) * 2 * r,
                    (rnd.nextDouble() - 0.5) * 4,
                    (rnd.nextDouble() - 0.5) * 2 * r)));
        }
        while (souls.size() > count.getValue().intValue()) {
            souls.remove(souls.size() - 1);
        }

        double spd = speed.getValue().doubleValue();

        Iterator<Soul> it = souls.iterator();
        while (it.hasNext()) {
            Soul soul = it.next();
            soul.age++;

            // медленный дрейф по мягким синусоидальным орбитам — «парение»
            double t = soul.age / 60.0;
            soul.pos = soul.pos.add(
                    Math.sin(t * 0.30 + soul.phase) * 0.016 * spd,
                    (Math.sin(t * 0.19 + soul.phase * 2.1) * 0.010 + 0.004) * spd,
                    Math.cos(t * 0.24 + soul.phase * 0.8) * 0.016 * spd);

            long age = System.currentTimeMillis() - soul.birth;
            if (age >= SOUL_TTL_MS || soul.pos.distanceTo(mc.player.getPos()) > r * 1.8) {
                // переспавн поблизости от игрока
                soul.pos = mc.player.getPos().add(
                        (rnd.nextDouble() - 0.5) * 2 * r,
                        (rnd.nextDouble() - 0.5) * 4,
                        (rnd.nextDouble() - 0.5) * 2 * r);
                soul.birth = System.currentTimeMillis();
                soul.age = 0;
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (souls.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        float time = now / 1000f;

        // цвет темы обновляется каждый кадр — поддерживает градиентные темы
        Color theme = themeManager.getThemeColor();
        Color c = themeColor.getValue() ? theme : customColor.getColor();
        int cr = c.getRed(), cg = c.getGreen(), cb = c.getBlue();

        float alphaMul = alpha.getValue().floatValue();
        float baseSize = size.getValue().floatValue();
        boolean renderGlow = glow.getValue();

        RenderSystem.enableBlend();
        // аддитивный бленд — призрачное свечение, как в SoulRenderer (TargetEsp)
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // ── Мягкое свечение-биллборд вокруг каждой души ──────────────────────
        if (renderGlow) {
            RenderSystem.setShaderTexture(0, darkvisuals.id("hud/bloom.png"));
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            Quaternionf camRot = mc.gameRenderer.getCamera().getRotation();
            BufferBuilder glowBuffer = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            boolean anyGlow = false;
            for (Soul soul : souls) {
                float a = soulAlpha(soul, now, alphaMul);
                if (a <= 0.01f) continue;

                float gr = baseSize * soul.sizeJitter * 2.1f
                        * (0.92f + 0.08f * (float) Math.sin(time * 2f + soul.phase));
                int glowAlpha = clamp255(85 * a);

                matrices.push();
                matrices.translate(
                        (float) (soul.pos.x - cam.x),
                        (float) (soul.pos.y - cam.y),
                        (float) (soul.pos.z - cam.z));
                matrices.multiply(camRot);

                Matrix4f m = matrices.peek().getPositionMatrix();
                glowBuffer.vertex(m, -gr, gr, 0).texture(0f, 1f).color(cr, cg, cb, glowAlpha);
                glowBuffer.vertex(m, gr, gr, 0).texture(1f, 1f).color(cr, cg, cb, glowAlpha);
                glowBuffer.vertex(m, gr, -gr, 0).texture(1f, 0f).color(cr, cg, cb, glowAlpha);
                glowBuffer.vertex(m, -gr, -gr, 0).texture(0f, 0f).color(cr, cg, cb, glowAlpha);
                matrices.pop();
                anyGlow = true;
            }
            // все души могут быть в стадии появления/растворения — буфер пуст, end() бросил бы исключение
            if (anyGlow) BufferRenderer.drawWithGlobalProgram(glowBuffer.end());
        }

        // ── 3D-тело души: дрожащая, вращающаяся икосфера ──────────────────────
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        boolean anyBody = false;
        for (Soul soul : souls) {
            float a = soulAlpha(soul, now, alphaMul);
            if (a <= 0.01f) continue;

            float s = baseSize * soul.sizeJitter;
            float spin = time * soul.spinSpeed + soul.phase;
            float cos = (float) Math.cos(spin), sin = (float) Math.sin(spin);
            float ox = (float) (soul.pos.x - cam.x);
            float oy = (float) (soul.pos.y - cam.y);
            float oz = (float) (soul.pos.z - cam.z);
            // амплитуда «дрожи» поверхности тоже плавно меняется
            float wobbleAmp = 0.15f * (0.8f + 0.2f * (float) Math.sin(time * 1.7f + soul.phase * 2f));

            for (int i = 0; i < ICO_I.length; i += 3) {
                for (int k = 0; k < 3; k++) {
                    int vi = ICO_I[i + k];
                    float px = ICO_V[vi * 3];
                    float py = ICO_V[vi * 3 + 1];
                    float pz = ICO_V[vi * 3 + 2];

                    // поверхность перекатывается волнами — сумма синусоид
                    float d = 1f + wobbleAmp * (
                            (float) Math.sin(time * 1.3f + soul.phase + px * 2.5f + py * 1.7f)
                                    + (float) Math.sin(time * 0.9f + soul.phase * 1.7f + pz * 2.2f));

                    float vx = px * d * s;
                    float vy = py * d * s * 1.4f;   // вытянута вверх — силуэт духа
                    float vz = pz * d * s;

                    // вращение вокруг оси Y
                    float rx = vx * cos - vz * sin;
                    float rz = vx * sin + vz * cos;

                    // выше к верхушке — ярче; аддитивный бленд соберёт светящееся ядро
                    int va = clamp255((50 + 125 * (py * 0.5f + 0.5f)) * a);
                    buffer.vertex(rx + ox, vy + oy, rz + oz).color(cr, cg, cb, va);
                }
            }
            anyBody = true;
        }
        if (anyBody) BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Плавное появление, растворение и мерцание души. */
    private float soulAlpha(Soul soul, long now, float alphaMul) {
        float lifeT = (now - soul.birth) / (float) SOUL_TTL_MS;
        float fadeIn = clamp01(lifeT / 0.12f);
        float fadeOut = clamp01((1f - lifeT) / 0.18f);
        float pulse = 0.72f + 0.28f * (float) Math.sin(now / 650f + soul.phase * 4f);
        return fadeIn * fadeOut * pulse * alphaMul;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static int clamp255(double v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return (int) Math.round(v);
    }

    private static final class Soul {
        Vec3d pos;
        final float phase;
        final float sizeJitter;
        final float spinSpeed;   // рад/с — медленное собственное вращение
        long birth = System.currentTimeMillis();
        int age;

        Soul(Vec3d pos) {
            this.pos = pos;
            Random r = new Random();
            this.phase = r.nextFloat() * (float) (Math.PI * 2);
            this.sizeJitter = 0.8f + r.nextFloat() * 0.4f;
            this.spinSpeed = 0.3f + r.nextFloat() * 0.6f;
        }
    }
}
