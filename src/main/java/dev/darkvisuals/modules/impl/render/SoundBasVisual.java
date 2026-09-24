package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventPacket;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.media.NowPlayingBridge;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Nameable;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SoundBasVisual — визуализация басов и громкости музыки прямо в небе:
 * расходящиеся в зените басовые кольца, круговой 3D-эквалайзер и пульсирующая сфера звука.
 */
public class SoundBasVisual extends Module {

    public enum VisualMode implements Nameable {
        All("All"),
        BassRings("Bass Rings"),
        EqualizerCircle("Equalizer"),
        SkyDome("Sky Dome");

        private final String displayName;
        VisualMode(String displayName) { this.displayName = displayName; }
        @Override public String getName() { return displayName; }
    }

    public enum ColorMode implements Nameable {
        Theme("Theme"),
        Rainbow("Rainbow"),
        NeonBlue("Neon Blue"),
        Sunset("Sunset"),
        Custom("Custom");

        private final String displayName;
        ColorMode(String displayName) { this.displayName = displayName; }
        @Override public String getName() { return displayName; }
    }

    private final EnumSetting<VisualMode> visualMode = new EnumSetting<>("Режим", VisualMode.All);
    private final NumberSetting skyHeight = new NumberSetting("Высота в небе", 110f, 40f, 250f, 10f);
    private final NumberSetting ringRadius = new NumberSetting("Радиус колец", 85f, 30f, 180f, 5f);
    private final NumberSetting sensitivity = new NumberSetting("Чувствительность", 1.2f, 0.4f, 3.0f, 0.1f);
    private final NumberSetting bassSpeed = new NumberSetting("Скорость импульса", 1.0f, 0.3f, 2.5f, 0.1f);
    private final NumberSetting opacity = new NumberSetting("Прозрачность", 0.75f, 0.2f, 1.0f, 0.05f);
    private final BooleanSetting reactToGameSounds = new BooleanSetting("Реакция на звуки игры", true);
    private final EnumSetting<ColorMode> colorMode = new EnumSetting<>("Цвет", ColorMode.Theme);
    private final ColorSetting customColor = new ColorSetting("Свой цвет", new Color(0, 225, 255, 240).getRGB());

    private final List<BassRing> activeRings = new ArrayList<>();
    private float smoothBassEnergy = 0.0f;
    private float targetBassEnergy = 0.0f;
    private long lastRingSpawnTime = 0L;
    private float audioPhase = 0f;

    public SoundBasVisual() {
        super("SoundBasVisual", Category.Render, "Визуализация басов музыки и звуков в небе");
        getSettings().add(visualMode);
        getSettings().add(skyHeight);
        getSettings().add(ringRadius);
        getSettings().add(sensitivity);
        getSettings().add(bassSpeed);
        getSettings().add(opacity);
        getSettings().add(reactToGameSounds);
        getSettings().add(colorMode);
        getSettings().add(customColor);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        activeRings.clear();
        smoothBassEnergy = 0f;
        targetBassEnergy = 0f;
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive event) {
        if (!reactToGameSounds.getValue() || fullNullCheck()) return;

        if (event.getPacket() instanceof PlaySoundS2CPacket soundPacket) {
            float vol = soundPacket.getVolume() * sensitivity.getValue();
            if (vol > 0.4f) {
                targetBassEnergy = Math.min(1.8f, targetBassEnergy + vol * 0.35f);
            }
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        boolean musicActive = false;

        // 1. Проверяем внешнюю музыку через NowPlayingBridge (Spotify, браузер и т.д.)
        NowPlayingBridge.Snapshot mediaSnap = NowPlayingBridge.getCurrent();
        if (mediaSnap != null && mediaSnap.playing) {
            musicActive = true;
        }

        // 2. Проверяем воспроизведение музыки дисков / внутриигровой музыки
        if (mc.getSoundManager() != null) {
            // Если играет музыка или медиа
            if (musicActive) {
                float beatSpeed = 2.1f * bassSpeed.getValue();
                audioPhase += 0.05f * beatSpeed;
                // Имитация басовых ударов музыки (Kick + Bassline)
                float beat1 = (float) Math.pow(Math.max(0.0, Math.sin(audioPhase * Math.PI * 2.0)), 4.0);
                float beat2 = (float) Math.pow(Math.max(0.0, Math.sin(audioPhase * Math.PI * 1.0 + 0.3)), 6.0) * 0.7f;
                float currentImpulse = (beat1 + beat2) * sensitivity.getValue();
                targetBassEnergy = Math.max(targetBassEnergy, currentImpulse);
            }
        }

        // Плавная интерполяция энергии баса
        smoothBassEnergy = MathHelper.lerp(0.18f, smoothBassEnergy, targetBassEnergy);
        targetBassEnergy *= 0.88f;

        // Спавн басового кольца при пиковом значении
        if (smoothBassEnergy > 0.45f && (now - lastRingSpawnTime) > 350L / bassSpeed.getValue()) {
            lastRingSpawnTime = now;
            activeRings.add(new BassRing(
                    ringRadius.getValue() * 0.25f,
                    ringRadius.getValue() * (1.1f + smoothBassEnergy * 0.35f),
                    smoothBassEnergy,
                    resolveColor()
            ));
        }

        // Обновление колец
        Iterator<BassRing> it = activeRings.iterator();
        while (it.hasNext()) {
            BassRing ring = it.next();
            ring.progress += 0.025f * bassSpeed.getValue();
            if (ring.progress >= 1.0f) it.remove();
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();

        float yHeight = skyHeight.getValue();
        Color themeColor = resolveColor();
        VisualMode mode = visualMode.getValue();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
        // Привязываем центр эффекта к позиции игрока над его головой в небе
        matrices.translate(0f, yHeight, 0f);
        Matrix4f m = matrices.peek().getPositionMatrix();

        // 1. Расходящиеся басовые кольца (Bass Rings)
        if (mode == VisualMode.All || mode == VisualMode.BassRings) {
            renderBassRings(m, themeColor);
        }

        // 2. Круговой 3D-эквалайзер (Equalizer Circle)
        if (mode == VisualMode.All || mode == VisualMode.EqualizerCircle) {
            renderEqualizerCircle(m, themeColor);
        }

        // 3. Пульсирующий купол звука в зените (Sky Dome)
        if (mode == VisualMode.All || mode == VisualMode.SkyDome) {
            renderSkyDomeGlow(m, themeColor);
        }

        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderBassRings(Matrix4f m, Color col) {
        if (activeRings.isEmpty()) return;

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int segs = 40;

        for (BassRing r : activeRings) {
            float t = r.progress;
            float curR = MathHelper.lerp(t, r.startRadius, r.endRadius);
            float alpha = (1.0f - t) * opacity.getValue() * Math.min(1.0f, r.power);
            if (alpha <= 0.01f) continue;

            float innerR = curR * 0.94f;
            float outerR = curR;
            float heightOffset = t * 24.0f; // Кольцо медленно взмывает выше в небо

            float cr = r.color.getRed() / 255f;
            float cg = r.color.getGreen() / 255f;
            float cb = r.color.getBlue() / 255f;

            for (int i = 0; i < segs; i++) {
                double a1 = (Math.PI * 2.0 / segs) * i;
                double a2 = (Math.PI * 2.0 / segs) * (i + 1);

                float x1 = (float) Math.cos(a1) * innerR, z1 = (float) Math.sin(a1) * innerR;
                float x2 = (float) Math.cos(a2) * innerR, z2 = (float) Math.sin(a2) * innerR;
                float x3 = (float) Math.cos(a2) * outerR, z3 = (float) Math.sin(a2) * outerR;
                float x4 = (float) Math.cos(a1) * outerR, z4 = (float) Math.sin(a1) * outerR;

                buf.vertex(m, x1, heightOffset, z1).color(cr, cg, cb, alpha);
                buf.vertex(m, x2, heightOffset, z2).color(cr, cg, cb, alpha);
                buf.vertex(m, x3, heightOffset, z3).color(cr, cg, cb, 0f);
                buf.vertex(m, x4, heightOffset, z4).color(cr, cg, cb, 0f);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void renderEqualizerCircle(Matrix4f m, Color col) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int bars = 48;
        float radius = ringRadius.getValue() * 0.8f;
        float baseAlpha = opacity.getValue() * (0.35f + smoothBassEnergy * 0.65f);
        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;

        float cr = col.getRed() / 255f;
        float cg = col.getGreen() / 255f;
        float cb = col.getBlue() / 255f;

        for (int i = 0; i < bars; i++) {
            double angle = (Math.PI * 2.0 / bars) * i;
            // Динамическая волна амплитуды эквалайзера
            float barWave = (float) Math.sin(angle * 4.0 + time * 3.0) * 0.35f
                    + (float) Math.cos(angle * 7.0 - time * 2.0) * 0.25f;
            float barHeight = (1.5f + Math.abs(barWave) * 12.0f + smoothBassEnergy * 16.0f);
            float barWidth = radius * (float) (Math.PI * 2.0 / bars) * 0.55f;

            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            float cx = cos * radius;
            float cz = sin * radius;

            // Касательный вектор к окружности для ориентации планки
            float perpX = -sin * (barWidth * 0.5f);
            float perpZ = cos * (barWidth * 0.5f);

            buf.vertex(m, cx - perpX, -barHeight * 0.5f, cz - perpZ).color(cr * 0.6f, cg * 0.6f, cb * 0.6f, baseAlpha * 0.5f);
            buf.vertex(m, cx + perpX, -barHeight * 0.5f, cz + perpZ).color(cr * 0.6f, cg * 0.6f, cb * 0.6f, baseAlpha * 0.5f);
            buf.vertex(m, cx + perpX, barHeight * 0.5f, cz + perpZ).color(cr * 1.3f, cg * 1.3f, cb * 1.3f, baseAlpha);
            buf.vertex(m, cx - perpX, barHeight * 0.5f, cz - perpZ).color(cr * 1.3f, cg * 1.3f, cb * 1.3f, baseAlpha);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void renderSkyDomeGlow(Matrix4f m, Color col) {
        float pulseR = (ringRadius.getValue() * 0.5f) * (0.85f + smoothBassEnergy * 0.35f);
        float alpha = opacity.getValue() * (0.12f + smoothBassEnergy * 0.38f);
        if (alpha <= 0.005f) return;

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int segs = 32;
        float cr = col.getRed() / 255f, cg = col.getGreen() / 255f, cb = col.getBlue() / 255f;

        for (int i = 0; i < segs; i++) {
            double a1 = (Math.PI * 2.0 / segs) * i;
            double a2 = (Math.PI * 2.0 / segs) * (i + 1);

            float x1 = (float) Math.cos(a1) * pulseR, z1 = (float) Math.sin(a1) * pulseR;
            float x2 = (float) Math.cos(a2) * pulseR, z2 = (float) Math.sin(a2) * pulseR;

            buf.vertex(m, 0f, 6.0f, 0f).color(cr * 1.2f, cg * 1.2f, cb * 1.2f, alpha);
            buf.vertex(m, x1, 0f, z1).color(cr, cg, cb, 0f);
            buf.vertex(m, x2, 0f, z2).color(cr, cg, cb, 0f);
            buf.vertex(m, 0f, 6.0f, 0f).color(cr * 1.2f, cg * 1.2f, cb * 1.2f, alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private Color resolveColor() {
        return switch (colorMode.getValue()) {
            case Theme -> ThemeManager.getInstance().getCurrentTheme().getAccentColor();
            case Rainbow -> {
                float hue = (System.currentTimeMillis() % 4500L) / 4500.0f;
                yield Color.getHSBColor(hue, 0.85f, 1.0f);
            }
            case NeonBlue -> new Color(0, 210, 255, 240);
            case Sunset -> new Color(255, 75, 140, 240);
            case Custom -> customColor.getColor();
        };
    }

    private static final class BassRing {
        final float startRadius;
        final float endRadius;
        final float power;
        final Color color;
        float progress;

        BassRing(float startRadius, float endRadius, float power, Color color) {
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.power = power;
            this.color = color;
            this.progress = 0f;
        }
    }
}
