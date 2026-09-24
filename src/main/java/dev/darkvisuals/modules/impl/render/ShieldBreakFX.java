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
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
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
 * ShieldBreakFX — эффектный визуальный оверхол при пробитии щита:
 * ударная 3D-волна, разлёт металлических искр и осколков щита с физикой.
 */
public class ShieldBreakFX extends Module {

    public enum ColorMode implements Nameable {
        Theme("Theme"),
        Custom("Custom"),
        Golden("Golden"),
        Electric("Electric");

        private final String displayName;
        ColorMode(String displayName) { this.displayName = displayName; }
        @Override public String getName() { return displayName; }
    }

    private final BooleanSetting shockwave = new BooleanSetting("Ударная волна", true);
    private final BooleanSetting sparks = new BooleanSetting("Искры", true);
    private final NumberSetting sparkCount = new NumberSetting("Количество искр", 36f, 10f, 90f, 5f);
    private final BooleanSetting shards = new BooleanSetting("Осколки щита", true);
    private final NumberSetting shardCount = new NumberSetting("Количество осколков", 16f, 4f, 40f, 2f);
    private final NumberSetting scale = new NumberSetting("Масштаб", 1.0f, 0.4f, 2.5f, 0.1f);
    private final EnumSetting<ColorMode> colorMode = new EnumSetting<>("Режим цвета", ColorMode.Theme);
    private final ColorSetting customColor = new ColorSetting("Свой цвет", new Color(255, 180, 50, 255).getRGB());

    private final List<ShockRing> rings = new ArrayList<>();
    private final List<Spark> sparkList = new ArrayList<>();
    private final List<Shard> shardList = new ArrayList<>();
    private final Random rnd = new Random();

    public ShieldBreakFX() {
        super("ShieldBreakFX", Category.Render, "Визуальный эффект при пробитии или отключении щита");
        getSettings().add(shockwave);
        getSettings().add(sparks);
        getSettings().add(sparkCount);
        getSettings().add(shards);
        getSettings().add(shardCount);
        getSettings().add(scale);
        getSettings().add(colorMode);
        getSettings().add(customColor);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        rings.clear();
        sparkList.clear();
        shardList.clear();
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive event) {
        if (fullNullCheck()) return;

        if (event.getPacket() instanceof EntityStatusS2CPacket packet) {
            // Статус 30 — отключение щита топором (BREAK_SHIELD)
            if (packet.getStatus() == 30) {
                mc.execute(() -> {
                    if (mc.world == null || mc.player == null) return;
                    Entity target = packet.getEntity(mc.world);
                    if (target != null && target.getPos().distanceTo(mc.player.getPos()) < 48) {
                        triggerEffect(target.getPos().add(0, target.getEyeHeight(target.getPose()) * 0.65, 0));
                    }
                });
            }
        } else if (event.getPacket() instanceof PlaySoundS2CPacket soundPacket) {
            if (soundPacket.getSound().value().equals(SoundEvents.ITEM_SHIELD_BREAK)) {
                mc.execute(() -> {
                    if (mc.player == null) return;
                    Vec3d soundPos = new Vec3d(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ());
                    if (soundPos.distanceTo(mc.player.getPos()) < 48) {
                        triggerEffect(soundPos);
                    }
                });
            }
        }
    }

    public void triggerEffect(Vec3d pos) {
        float sc = scale.getValue();
        Color col = resolveColor();

        if (shockwave.getValue()) {
            rings.add(new ShockRing(pos, 0.2f * sc, 2.4f * sc, col, 14));
            rings.add(new ShockRing(pos, 0.1f * sc, 1.8f * sc, Color.WHITE, 10));
        }

        if (sparks.getValue()) {
            int count = (int) (float) sparkCount.getValue();
            for (int i = 0; i < count; i++) {
                double theta = rnd.nextDouble() * Math.PI * 2;
                double phi = (rnd.nextDouble() - 0.5) * Math.PI * 0.9;
                double speed = (1.5 + rnd.nextDouble() * 3.5) * sc;
                Vec3d vel = new Vec3d(
                        Math.cos(phi) * Math.cos(theta) * speed,
                        (Math.sin(phi) + 0.4) * speed,
                        Math.cos(phi) * Math.sin(theta) * speed
                );
                sparkList.add(new Spark(pos, vel, col, 0.04f * sc, 16 + rnd.nextInt(12)));
            }
        }

        if (shards.getValue()) {
            int count = (int) (float) shardCount.getValue();
            for (int i = 0; i < count; i++) {
                double theta = rnd.nextDouble() * Math.PI * 2;
                double speed = (1.0 + rnd.nextDouble() * 2.8) * sc;
                Vec3d vel = new Vec3d(
                        Math.cos(theta) * speed,
                        (rnd.nextDouble() * 2.0 + 1.2) * sc,
                        Math.sin(theta) * speed
                );
                Vec3d rot = new Vec3d(rnd.nextFloat() * 360, rnd.nextFloat() * 360, rnd.nextFloat() * 360);
                Vec3d rotSpeed = new Vec3d(
                        (rnd.nextFloat() - 0.5f) * 40f,
                        (rnd.nextFloat() - 0.5f) * 40f,
                        (rnd.nextFloat() - 0.5f) * 40f
                );
                float sz = (0.08f + rnd.nextFloat() * 0.12f) * sc;
                shardList.add(new Shard(pos, vel, rot, rotSpeed, col, sz, 24 + rnd.nextInt(16)));
            }
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        Iterator<ShockRing> rIt = rings.iterator();
        while (rIt.hasNext()) {
            ShockRing r = rIt.next();
            r.progress++;
            if (r.progress >= r.maxLife) rIt.remove();
        }

        Iterator<Spark> sIt = sparkList.iterator();
        while (sIt.hasNext()) {
            Spark s = sIt.next();
            s.age++;
            s.pos = s.pos.add(s.vel.multiply(0.05));
            s.vel = new Vec3d(s.vel.x * 0.94, s.vel.y * 0.94 - 0.12, s.vel.z * 0.94);
            if (s.age >= s.maxAge) sIt.remove();
        }

        Iterator<Shard> shIt = shardList.iterator();
        while (shIt.hasNext()) {
            Shard sh = shIt.next();
            sh.age++;
            sh.pos = sh.pos.add(sh.vel.multiply(0.05));
            sh.vel = new Vec3d(sh.vel.x * 0.96, sh.vel.y * 0.96 - 0.18, sh.vel.z * 0.96);
            sh.rot = sh.rot.add(sh.rotSpeed);
            if (sh.age >= sh.maxAge) shIt.remove();
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck() || (rings.isEmpty() && sparkList.isEmpty() && shardList.isEmpty())) return;

        MatrixStack matrices = event.getMatrices();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // 1. Ударные волны (кольца)
        if (!rings.isEmpty()) {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (ShockRing r : rings) {
                float t = (float) r.progress / r.maxLife;
                float curR = MathHelper.lerp(t, r.startRadius, r.endRadius);
                float alpha = (1.0f - t) * (r.color.getAlpha() / 255.0f);
                if (alpha <= 0.01f) continue;

                int segs = 24;
                float innerR = curR * 0.82f;
                float outerR = curR;

                matrices.push();
                matrices.translate(r.pos.x - cam.x, r.pos.y - cam.y, r.pos.z - cam.z);
                Matrix4f m = matrices.peek().getPositionMatrix();

                for (int i = 0; i < segs; i++) {
                    double a1 = (Math.PI * 2.0 / segs) * i;
                    double a2 = (Math.PI * 2.0 / segs) * (i + 1);

                    float x1 = (float) Math.cos(a1) * innerR, z1 = (float) Math.sin(a1) * innerR;
                    float x2 = (float) Math.cos(a2) * innerR, z2 = (float) Math.sin(a2) * innerR;
                    float x3 = (float) Math.cos(a2) * outerR, z3 = (float) Math.sin(a2) * outerR;
                    float x4 = (float) Math.cos(a1) * outerR, z4 = (float) Math.sin(a1) * outerR;

                    float cr = r.color.getRed() / 255f, cg = r.color.getGreen() / 255f, cb = r.color.getBlue() / 255f;

                    buf.vertex(m, x1, 0f, z1).color(cr, cg, cb, alpha);
                    buf.vertex(m, x2, 0f, z2).color(cr, cg, cb, alpha);
                    buf.vertex(m, x3, 0f, z3).color(cr, cg, cb, 0f);
                    buf.vertex(m, x4, 0f, z4).color(cr, cg, cb, 0f);
                }
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // 2. Искры
        if (!sparkList.isEmpty()) {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (Spark sp : sparkList) {
                float life = 1.0f - ((float) sp.age / sp.maxAge);
                float a = life * (sp.color.getAlpha() / 255f);
                if (a <= 0.01f) continue;

                matrices.push();
                matrices.translate(sp.pos.x - cam.x, sp.pos.y - cam.y, sp.pos.z - cam.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                Matrix4f m = matrices.peek().getPositionMatrix();

                float s = sp.size * (0.4f + life * 0.6f);
                float cr = sp.color.getRed() / 255f, cg = sp.color.getGreen() / 255f, cb = sp.color.getBlue() / 255f;

                buf.vertex(m, -s, -s, 0f).color(cr, cg, cb, a);
                buf.vertex(m, s, -s, 0f).color(cr, cg, cb, a);
                buf.vertex(m, s, s, 0f).color(cr, cg, cb, a);
                buf.vertex(m, -s, s, 0f).color(cr, cg, cb, a);

                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // 3. Осколки щита
        if (!shardList.isEmpty()) {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (Shard sh : shardList) {
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

                // Рисуем трёхмерный осколок в форме треугольной пластинки
                buf.vertex(m, -s, -s * 0.5f, 0f).color(cr, cg, cb, a);
                buf.vertex(m, s * 1.2f, 0f, 0f).color(cr, cg, cb, a);
                buf.vertex(m, 0f, s * 1.4f, 0f).color(cr * 1.2f, cg * 1.2f, cb * 1.2f, a);
                buf.vertex(m, -s * 0.4f, s * 0.2f, 0f).color(cr, cg, cb, a);

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
            case Theme -> ThemeManager.getInstance().getCurrentTheme().getAccentColor();
            case Custom -> customColor.getColor();
            case Golden -> new Color(255, 215, 0, 240);
            case Electric -> new Color(80, 210, 255, 240);
        };
    }

    private static final class ShockRing {
        final Vec3d pos;
        final float startRadius;
        final float endRadius;
        final Color color;
        final int maxLife;
        int progress;

        ShockRing(Vec3d pos, float startRadius, float endRadius, Color color, int maxLife) {
            this.pos = pos;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.color = color;
            this.maxLife = maxLife;
        }
    }

    private static final class Spark {
        Vec3d pos;
        Vec3d vel;
        final Color color;
        final float size;
        final int maxAge;
        int age;

        Spark(Vec3d pos, Vec3d vel, Color color, float size, int maxAge) {
            this.pos = pos;
            this.vel = vel;
            this.color = color;
            this.size = size;
            this.maxAge = maxAge;
        }
    }

    private static final class Shard {
        Vec3d pos;
        Vec3d vel;
        Vec3d rot;
        final Vec3d rotSpeed;
        final Color color;
        final float size;
        final int maxAge;
        int age;

        Shard(Vec3d pos, Vec3d vel, Vec3d rot, Vec3d rotSpeed, Color color, float size, int maxAge) {
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
