package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventPacket;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.managers.ThemeManager;
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
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
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

/**
 * CrystalBreakFX — визуальный эффект при взрыве эндер-кристаллов:
 * кристаллическая ударная волна, разлёт острых осколков обсидиана и неоновая вспышка.
 */
public class CrystalBreakFX extends Module {

    public enum ColorMode implements Nameable {
        Obsidian("Obsidian"),
        Theme("Theme"),
        Crimson("Crimson"),
        Cyan("Cyan"),
        Custom("Custom");

        private final String displayName;
        ColorMode(String displayName) { this.displayName = displayName; }
        @Override public String getName() { return displayName; }
    }

    private final BooleanSetting shockwave = new BooleanSetting("Ударная волна", true);
    private final BooleanSetting shards = new BooleanSetting("Осколки кристалла", true);
    private final NumberSetting shardCount = new NumberSetting("Количество осколков", 32f, 8f, 70f, 2f);
    private final NumberSetting shardSpeed = new NumberSetting("Скорость разлёта", 1.2f, 0.4f, 3.0f, 0.1f);
    private final BooleanSetting coreFlash = new BooleanSetting("Вспышка ядра", true);
    private final NumberSetting scale = new NumberSetting("Масштаб", 1.0f, 0.4f, 2.5f, 0.1f);
    private final EnumSetting<ColorMode> colorMode = new EnumSetting<>("Цвет", ColorMode.Obsidian);
    private final ColorSetting customColor = new ColorSetting("Свой цвет", new Color(195, 60, 255, 255).getRGB());

    private final List<CrystalShockwave> waveList = new ArrayList<>();
    private final List<CrystalShard> shardList = new ArrayList<>();
    private final List<CoreFlash> flashList = new ArrayList<>();
    private final Map<Integer, Vec3d> knownCrystals = new HashMap<>();
    private final Random rnd = new Random();

    public CrystalBreakFX() {
        super("CrystalBreakFX", Category.Render, "Эффектный взрыв кристаллов с осколками и ударной волной");
        getSettings().add(shockwave);
        getSettings().add(shards);
        getSettings().add(shardCount);
        getSettings().add(shardSpeed);
        getSettings().add(coreFlash);
        getSettings().add(scale);
        getSettings().add(colorMode);
        getSettings().add(customColor);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        waveList.clear();
        shardList.clear();
        flashList.clear();
        knownCrystals.clear();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        // Отслеживаем кристаллы в мире и их исчезновение / взрыв
        Map<Integer, Vec3d> currentCrystals = new HashMap<>();
        for (net.minecraft.entity.Entity e : mc.world.getEntities()) {
            if (e instanceof EndCrystalEntity && e.isAlive()) {
                currentCrystals.put(e.getId(), e.getPos());
            }
        }

        // Если кристалл пропал и игрок рядом — вызываем эффект взрыва
        for (Map.Entry<Integer, Vec3d> entry : knownCrystals.entrySet()) {
            if (!currentCrystals.containsKey(entry.getKey())) {
                Vec3d pos = entry.getValue();
                if (mc.player != null && pos.distanceTo(mc.player.getPos()) < 48) {
                    triggerBreak(pos.add(0, 1.0, 0));
                }
            }
        }

        knownCrystals.clear();
        knownCrystals.putAll(currentCrystals);

        // Обновление состояния волн
        Iterator<CrystalShockwave> wIt = waveList.iterator();
        while (wIt.hasNext()) {
            CrystalShockwave w = wIt.next();
            w.progress++;
            if (w.progress >= w.maxLife) wIt.remove();
        }

        // Обновление состояния вспышек
        Iterator<CoreFlash> fIt = flashList.iterator();
        while (fIt.hasNext()) {
            CoreFlash f = fIt.next();
            f.progress++;
            if (f.progress >= f.maxLife) fIt.remove();
        }

        // Обновление физики осколков
        Iterator<CrystalShard> sIt = shardList.iterator();
        while (sIt.hasNext()) {
            CrystalShard s = sIt.next();
            s.age++;
            s.pos = s.pos.add(s.vel.multiply(0.05));
            s.vel = new Vec3d(s.vel.x * 0.95, s.vel.y * 0.95 - 0.16, s.vel.z * 0.95);
            s.rot = s.rot.add(s.rotSpeed);
            if (s.age >= s.maxAge) sIt.remove();
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive event) {
        if (fullNullCheck()) return;

        // Взрыв по пакету
        if (event.getPacket() instanceof ExplosionS2CPacket explosion) {
            Vec3d expPos = explosion.center();
            mc.execute(() -> {
                if (mc.world == null || mc.player == null) return;
                // Проверяем, был ли рядом кристалл
                for (Vec3d cPos : knownCrystals.values()) {
                    if (cPos.distanceTo(expPos) < 2.5) {
                        triggerBreak(cPos.add(0, 1.0, 0));
                        break;
                    }
                }
            });
        }
    }

    public void triggerBreak(Vec3d pos) {
        float sc = scale.getValue();
        Color col = resolveColor();

        if (coreFlash.getValue()) {
            flashList.add(new CoreFlash(pos, 0.4f * sc, 1.8f * sc, col, 10));
        }

        if (shockwave.getValue()) {
            waveList.add(new CrystalShockwave(pos, 0.3f * sc, 3.2f * sc, 0f, col, 15));
            waveList.add(new CrystalShockwave(pos, 0.2f * sc, 2.6f * sc, 45f, Color.WHITE, 12));
        }

        if (shards.getValue()) {
            int count = (int) (float) shardCount.getValue();
            float spd = shardSpeed.getValue() * sc;
            for (int i = 0; i < count; i++) {
                double theta = rnd.nextDouble() * Math.PI * 2;
                double phi = (rnd.nextDouble() - 0.45) * Math.PI;
                double vSpeed = (1.5 + rnd.nextDouble() * 3.5) * spd;
                Vec3d vel = new Vec3d(
                        Math.cos(phi) * Math.cos(theta) * vSpeed,
                        (Math.sin(phi) + 0.3) * vSpeed,
                        Math.cos(phi) * Math.sin(theta) * vSpeed
                );
                Vec3d rot = new Vec3d(rnd.nextFloat() * 360, rnd.nextFloat() * 360, rnd.nextFloat() * 360);
                Vec3d rotSpeed = new Vec3d(
                        (rnd.nextFloat() - 0.5f) * 50f,
                        (rnd.nextFloat() - 0.5f) * 50f,
                        (rnd.nextFloat() - 0.5f) * 50f
                );
                float sz = (0.07f + rnd.nextFloat() * 0.14f) * sc;
                // Чередуем основной цвет и тёмный обсидиановый оттенок
                Color shardCol = rnd.nextBoolean() ? col : new Color(35, 20, 50, 240);
                shardList.add(new CrystalShard(pos, vel, rot, rotSpeed, shardCol, sz, 26 + rnd.nextInt(18)));
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck() || (waveList.isEmpty() && shardList.isEmpty() && flashList.isEmpty())) return;

        MatrixStack matrices = event.getMatrices();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // 1. Вспышка ядра
        if (!flashList.isEmpty()) {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (CoreFlash f : flashList) {
                float t = (float) f.progress / f.maxLife;
                float curR = MathHelper.lerp(t, f.startRadius, f.endRadius);
                float alpha = (1.0f - t) * (f.color.getAlpha() / 255f);
                if (alpha <= 0.01f) continue;

                matrices.push();
                matrices.translate(f.pos.x - cam.x, f.pos.y - cam.y, f.pos.z - cam.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                Matrix4f m = matrices.peek().getPositionMatrix();

                float cr = f.color.getRed() / 255f, cg = f.color.getGreen() / 255f, cb = f.color.getBlue() / 255f;
                buf.vertex(m, -curR, -curR, 0f).color(cr, cg, cb, alpha);
                buf.vertex(m, curR, -curR, 0f).color(cr, cg, cb, alpha);
                buf.vertex(m, curR, curR, 0f).color(cr, cg, cb, alpha);
                buf.vertex(m, -curR, curR, 0f).color(cr, cg, cb, alpha);

                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // 2. Ударные волны (кольца)
        if (!waveList.isEmpty()) {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (CrystalShockwave w : waveList) {
                float t = (float) w.progress / w.maxLife;
                float curR = MathHelper.lerp(t, w.startRadius, w.endRadius);
                float alpha = (1.0f - t) * (w.color.getAlpha() / 255f);
                if (alpha <= 0.01f) continue;

                int segs = 28;
                float innerR = curR * 0.85f;
                float outerR = curR;

                matrices.push();
                matrices.translate(w.pos.x - cam.x, w.pos.y - cam.y, w.pos.z - cam.z);
                if (w.tilt != 0f) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(w.tilt));
                Matrix4f m = matrices.peek().getPositionMatrix();

                float cr = w.color.getRed() / 255f, cg = w.color.getGreen() / 255f, cb = w.color.getBlue() / 255f;

                for (int i = 0; i < segs; i++) {
                    double a1 = (Math.PI * 2.0 / segs) * i;
                    double a2 = (Math.PI * 2.0 / segs) * (i + 1);

                    float x1 = (float) Math.cos(a1) * innerR, z1 = (float) Math.sin(a1) * innerR;
                    float x2 = (float) Math.cos(a2) * innerR, z2 = (float) Math.sin(a2) * innerR;
                    float x3 = (float) Math.cos(a2) * outerR, z3 = (float) Math.sin(a2) * outerR;
                    float x4 = (float) Math.cos(a1) * outerR, z4 = (float) Math.sin(a1) * outerR;

                    buf.vertex(m, x1, 0f, z1).color(cr, cg, cb, alpha);
                    buf.vertex(m, x2, 0f, z2).color(cr, cg, cb, alpha);
                    buf.vertex(m, x3, 0f, z3).color(cr, cg, cb, 0f);
                    buf.vertex(m, x4, 0f, z4).color(cr, cg, cb, 0f);
                }
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // 3. Осколки кристалла
        if (!shardList.isEmpty()) {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (CrystalShard sh : shardList) {
                float life = 1.0f - ((float) sh.age / sh.maxAge);
                float a = life * (sh.color.getAlpha() / 255f);
                if (a <= 0.01f) continue;

                matrices.push();
                matrices.translate(sh.pos.x - cam.x, sh.pos.y - cam.y, sh.pos.z - cam.z);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) sh.rot.x));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) sh.rot.y));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) sh.rot.z));
                Matrix4f m = matrices.peek().getPositionMatrix();

                float s = sh.size;
                float cr = sh.color.getRed() / 255f, cg = sh.color.getGreen() / 255f, cb = sh.color.getBlue() / 255f;

                // Острый кристаллический ромб
                buf.vertex(m, 0f, s * 1.6f, 0f).color(cr * 1.2f, cg * 1.2f, cb * 1.2f, a);
                buf.vertex(m, s * 0.7f, 0f, 0f).color(cr, cg, cb, a);
                buf.vertex(m, 0f, -s * 1.6f, 0f).color(cr * 0.7f, cg * 0.7f, cb * 0.7f, a);
                buf.vertex(m, -s * 0.7f, 0f, 0f).color(cr, cg, cb, a);

                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private Color resolveColor() {
        return switch (colorMode.getValue()) {
            case Obsidian -> new Color(195, 60, 255, 250);
            case Theme -> ThemeManager.getInstance().getCurrentTheme().getAccentColor();
            case Crimson -> new Color(255, 40, 80, 250);
            case Cyan -> new Color(40, 230, 255, 250);
            case Custom -> customColor.getColor();
        };
    }

    private static final class CrystalShockwave {
        final Vec3d pos;
        final float startRadius;
        final float endRadius;
        final float tilt;
        final Color color;
        final int maxLife;
        int progress;

        CrystalShockwave(Vec3d pos, float startRadius, float endRadius, float tilt, Color color, int maxLife) {
            this.pos = pos;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.tilt = tilt;
            this.color = color;
            this.maxLife = maxLife;
        }
    }

    private static final class CoreFlash {
        final Vec3d pos;
        final float startRadius;
        final float endRadius;
        final Color color;
        final int maxLife;
        int progress;

        CoreFlash(Vec3d pos, float startRadius, float endRadius, Color color, int maxLife) {
            this.pos = pos;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.color = color;
            this.maxLife = maxLife;
        }
    }

    private static final class CrystalShard {
        Vec3d pos;
        Vec3d vel;
        Vec3d rot;
        final Vec3d rotSpeed;
        final Color color;
        final float size;
        final int maxAge;
        int age;

        CrystalShard(Vec3d pos, Vec3d vel, Vec3d rot, Vec3d rotSpeed, Color color, float size, int maxAge) {
            this.pos = pos;
            this.vel = vel;
            this.rot = rot;
            this.rotSpeed = rotSpeed;
            this.color = color;
            this.size = size;
            this.maxAge = maxAge;
        }
    }
}
