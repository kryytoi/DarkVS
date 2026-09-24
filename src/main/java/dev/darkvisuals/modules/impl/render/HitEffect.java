package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.events.impl.EventRender3D;
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
import net.minecraft.client.render.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class HitEffect extends Module {

    public enum Style implements Nameable {
        BLOOD("Кровь"), IMPACT("Импакт"), GLASS("Стекло"), PULSE("Пульс");
        private final String title;
        Style(String title) { this.title = title; }
        @Override public String getName() { return title; }
    }

    private final EnumSetting<Style> style = new EnumSetting<>("Стиль", Style.BLOOD);
    private final NumberSetting size = new NumberSetting("Размер", 1.0f, 0.3f, 3.0f, 0.05f);
    private final NumberSetting lifeTime = new NumberSetting("Время жизни", 1.0f, 0.3f, 4.0f, 0.1f);
    private final NumberSetting amount = new NumberSetting("Количество", 14f, 4f, 40f, 1f);
    private final BooleanSetting onlyPlayers = new BooleanSetting("Только игроки", false);
    private final BooleanSetting themeColor = new BooleanSetting("Цвет клиента", false);
    private final BooleanSetting sound = new BooleanSetting("Звук", true);
    private final ColorSetting bloodColor = new ColorSetting("Цвет крови", new Color(140, 0, 0).getRGB());

    private static final int MAX_FX = 400;
    private static final float GRAVITY = 16f;
    private static final long GLASS_FREEZE_MS = 90L;
    private static final double HIT_SURFACE_OFFSET = 0.035D;

    private final List<Fx> effects = new CopyOnWriteArrayList<>();
    private long lastNanos = System.nanoTime();

    public HitEffect() {
        super("HitEffect", Category.Render, "Эффект в месте удара по существу");
        bloodColor.setVisible(() -> style.getValue() == Style.BLOOD);
        themeColor.setVisible(() -> style.getValue() == Style.IMPACT || style.getValue() == Style.GLASS);
    }

    @Override
    public void onDisable() {
        effects.clear();
        super.onDisable();
    }

    @EventHandler
    private void onAttack(EventAttackEntity e) {
        if (fullNullCheck()) return;
        if (e.getPlayer() != mc.player) return;
        if (!(e.getTarget() instanceof LivingEntity target) || target == mc.player) return;
        if (!target.isAlive() || target.isRemoved() || !e.isEffectsAllowed()) return;
        if (!e.canProcess()) return;
        if (onlyPlayers.getValue() && !(target instanceof PlayerEntity)) return;
        if (effects.size() >= MAX_FX) return;

        Vec3d hit = findHitPos(target);

        switch (style.getValue()) {
            case BLOOD -> spawnBlood(hit);
            case IMPACT -> effects.add(new Impact(hit));
            case GLASS -> spawnGlass(hit);
            case PULSE -> effects.add(new Pulse(target, hit));
        }

        if (sound.getValue()) playSound(hit);
    }

    private Vec3d findHitPos(LivingEntity entity) {
        Vec3d eye = mc.player.getEyePos();
        Box box = entity.getBoundingBox().expand(0.08D);

        if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == entity) {
            return pushOutside(box, eye, ehr.getPos());
        }

        Vec3d center = box.getCenter();
        Optional<Vec3d> traced = box.raycast(eye, center);
        if (traced.isPresent()) {
            return pushOutside(box, eye, traced.get());
        }

        Vec3d toEye = eye.subtract(center);
        if (toEye.lengthSquared() < 1.0E-6D) {
            return center.add(0.0D, entity.getHeight() * 0.35D, 0.0D);
        }

        Vec3d normal = toEye.normalize();
        double hx = box.getLengthX() * 0.5D;
        double hy = box.getLengthY() * 0.5D;
        double hz = box.getLengthZ() * 0.5D;

        double tx = Math.abs(normal.x) < 1.0E-6D ? Double.POSITIVE_INFINITY : hx / Math.abs(normal.x);
        double ty = Math.abs(normal.y) < 1.0E-6D ? Double.POSITIVE_INFINITY : hy / Math.abs(normal.y);
        double tz = Math.abs(normal.z) < 1.0E-6D ? Double.POSITIVE_INFINITY : hz / Math.abs(normal.z);

        double scale = Math.min(tx, Math.min(ty, tz));
        return center.add(normal.multiply(scale + HIT_SURFACE_OFFSET));
    }

    private Vec3d pushOutside(Box box, Vec3d eye, Vec3d hit) {
        Vec3d center = box.getCenter();
        Vec3d normal = hit.subtract(center);

        if (normal.lengthSquared() < 1.0E-6D) {
            normal = hit.subtract(eye);
        }

        if (normal.lengthSquared() < 1.0E-6D) {
            normal = new Vec3d(0.0D, 1.0D, 0.0D);
        }

        return hit.add(normal.normalize().multiply(HIT_SURFACE_OFFSET));
    }

    private void spawnBlood(Vec3d hit) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Vec3d push = hit.subtract(mc.player.getEyePos()).normalize();
        float s = size.getValue();
        int n = Math.round(amount.getValue());
        int rgb = bloodColor.getValue() & 0xFFFFFF;

        for (int i = 0; i < n; i++) {
            Vec3d vel = push.multiply(1.2 + r.nextDouble() * 1.5)
                    .add(r.nextGaussian() * 1.4, 1.5 + r.nextDouble() * 2.5, r.nextGaussian() * 1.4)
                    .multiply(Math.sqrt(s));
            effects.add(new BloodDrop(hit, vel, (0.035f + r.nextFloat() * 0.035f) * s, rgb));
        }
        DustParticleEffect mist = new DustParticleEffect(rgb, Math.min(4f, 1.2f * s));
        for (int i = 0; i < n / 2; i++) {
            mc.world.addParticle(mist, hit.x + r.nextGaussian() * 0.1, hit.y + r.nextGaussian() * 0.1,
                    hit.z + r.nextGaussian() * 0.1, 0, 0.02, 0);
        }
    }

    private void spawnGlass(Vec3d hit) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Camera cam = mc.gameRenderer.getCamera();
        Vector3f right = new Vector3f(1, 0, 0).rotate(cam.getRotation());
        Vector3f up = new Vector3f(0, 1, 0).rotate(cam.getRotation());
        Vec3d toCam = cam.getPos().subtract(hit).normalize();
        Vec3d center = hit.add(toCam.multiply(0.05));

        float R = 0.3f * size.getValue();
        int n = MathHelper.clamp(Math.round(amount.getValue()) / 2, 5, 14);
        Vec3d[] in = new Vec3d[n], out = new Vec3d[n];
        for (int i = 0; i < n; i++) {
            float ang = (i + r.nextFloat() * 0.6f) * MathHelper.TAU / n;
            in[i] = plane(center, right, up, ang, R * (0.35f + r.nextFloat() * 0.2f));
            out[i] = plane(center, right, up, ang, R * (0.85f + r.nextFloat() * 0.3f));
        }
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            addShard(center, in[i], in[j], center, toCam, r);
            addShard(in[i], out[i], out[j], center, toCam, r);
            addShard(in[i], out[j], in[j], center, toCam, r);
        }
    }

    private void addShard(Vec3d a, Vec3d b, Vec3d d, Vec3d center, Vec3d toCam, ThreadLocalRandom r) {
        Vec3d cen = a.add(b).add(d).multiply(1.0 / 3.0);
        Vec3d outDir = cen.subtract(center);
        outDir = outDir.lengthSquared() < 1e-6 ? new Vec3d(0, 1, 0) : outDir.normalize();
        Vec3d vel = outDir.multiply(1.2 + r.nextDouble() * 2.0)
                .add(toCam.multiply(0.6 + r.nextDouble()))
                .add(0, 1.0 + r.nextDouble() * 1.5, 0);
        effects.add(new Shard(cen, vel, a.subtract(cen), b.subtract(cen), d.subtract(cen)));
    }

    private void playSound(Vec3d p) {
        SoundEvent ev = switch (style.getValue()) {
            case BLOOD -> SoundEvents.ENTITY_SLIME_SQUISH_SMALL;
            case IMPACT -> SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST;
            case GLASS -> SoundEvents.BLOCK_GLASS_BREAK;
            case PULSE -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
        };
        mc.world.playSound(mc.player, p.x, p.y, p.z, ev, SoundCategory.PLAYERS, 0.6f,
                0.9f + ThreadLocalRandom.current().nextFloat() * 0.3f);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        long nn = System.nanoTime();
        float dt = Math.min((nn - lastNanos) / 1_000_000_000f, 0.05f);
        lastNanos = nn;

        effects.removeIf(fx -> !fx.update(dt, now));
        if (effects.isEmpty()) return;

        Camera cam = mc.gameRenderer.getCamera();
        Ctx c = new Ctx();
        c.cam = cam.getPos();
        Quaternionf q = cam.getRotation();
        c.right = new Vector3f(1, 0, 0).rotate(q);
        c.up = new Vector3f(0, 1, 0).rotate(q);
        c.mat = e.getMatrices().peek().getPositionMatrix();
        c.tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        c.buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Fx fx : effects) fx.draw(c, now);
        BuiltBuffer built = c.buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private abstract class Fx {
        final long born = System.currentTimeMillis();
        final long ttl;
        Fx(long ttl) { this.ttl = Math.max(50, ttl); }
        float age(long now) { return MathHelper.clamp((now - born) / (float) ttl, 0f, 1f); }
        boolean update(float dt, long now) { return now - born < ttl; }
        abstract void draw(Ctx c, long now);
    }

    private final class BloodDrop extends Fx {
        Vec3d pos, vel;
        final float sz;
        final int rgb;
        final float spin = ThreadLocalRandom.current().nextFloat() * MathHelper.TAU;
        final float[] jag = new float[10];
        boolean landed;
        long landedAt;

        BloodDrop(Vec3d pos, Vec3d vel, float sz, int rgb) {
            super((long) (lifeTime.getValue() * 2500));
            this.pos = pos; this.vel = vel; this.sz = sz; this.rgb = rgb;
            for (int i = 0; i < jag.length; i++) jag[i] = 0.7f + ThreadLocalRandom.current().nextFloat() * 0.5f;
        }

        @Override
        boolean update(float dt, long now) {
            if (!landed) {
                vel = vel.add(0, -GRAVITY * dt, 0).multiply(1.0 - 0.8 * dt);
                Vec3d next = pos.add(vel.multiply(dt));
                BlockPos bp = BlockPos.ofFloored(next);
                VoxelShape shape = mc.world.getBlockState(bp).getCollisionShape(mc.world, bp);
                if (!shape.isEmpty() && vel.y < 0) {
                    landed = true;
                    landedAt = now;
                    next = new Vec3d(next.x, bp.getY() + shape.getMax(Direction.Axis.Y) + 0.015, next.z);
                }
                pos = next;
            }
            return super.update(dt, now);
        }

        @Override
        void draw(Ctx c, long now) {
            float t = age(now);
            int a = (int) (230 * (t < 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f));
            if (a <= 3) return;

            if (!landed) {
                billboard(c, pos, sz, sz, argb(a, rgb));
                return;
            }
            float grow = Math.min(1f, (now - landedAt) / 150f);
            float rad = sz * (1.5f + 1.5f * grow);
            int col = argb((int) (a * 0.9f), new Color(rgb).darker().getRGB());
            int n = jag.length;
            for (int i = 0; i < n; i++) {
                float a0 = spin + i * MathHelper.TAU / n, a1 = spin + (i + 1) * MathHelper.TAU / n;
                Vec3d p0 = pos.add(Math.cos(a0) * rad * jag[i], 0, Math.sin(a0) * rad * jag[i]);
                Vec3d p1 = pos.add(Math.cos(a1) * rad * jag[(i + 1) % n], 0, Math.sin(a1) * rad * jag[(i + 1) % n]);
                tri(c, pos, p0, p1, col);
            }
        }
    }

    private final class Impact extends Fx {
        final Vec3d pos;
        final float[] radii;
        final float rot;

        Impact(Vec3d pos) {
            super((long) (lifeTime.getValue() * 450));
            this.pos = pos;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            int spikes = 9 + r.nextInt(5);
            radii = new float[spikes * 2];
            for (int i = 0; i < radii.length; i++)
                radii[i] = (i % 2 == 0) ? 0.75f + r.nextFloat() * 0.5f : 0.3f + r.nextFloat() * 0.15f;
            rot = r.nextFloat() * MathHelper.TAU;
        }

        @Override
        void draw(Ctx c, long now) {
            float t = age(now);
            float grow = 1f - (float) Math.pow(1f - Math.min(1f, t / 0.25f), 3);
            float alpha = t < 0.35f ? 1f : 1f - (t - 0.35f) / 0.65f;
            float R = 0.55f * size.getValue() * grow * (1f + 0.3f * t);

            Color outer = themeColor.getValue() ? accent() : new Color(255, 110, 20);
            Color mid = themeColor.getValue() ? accent().brighter() : new Color(255, 215, 70);

            star(c, R, rot, argb((int) (240 * alpha), mid), argb((int) (200 * alpha), outer));
            star(c, R * 0.55f, rot + 0.3f, argb((int) (255 * alpha), 0xFFFFFF), argb((int) (220 * alpha), mid));
        }

        private void star(Ctx c, float R, float rot, int inner, int outerCol) {
            int n = radii.length;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                Vec3d a = plane(pos, c.right, c.up, rot + i * MathHelper.TAU / n, R * radii[i]);
                Vec3d b = plane(pos, c.right, c.up, rot + j * MathHelper.TAU / n, R * radii[j]);
                vtx(c, pos, inner); vtx(c, a, outerCol); vtx(c, b, outerCol); vtx(c, b, outerCol);
            }
        }
    }

    private final class Shard extends Fx {
        Vec3d pos, vel;
        final Vector3f[] local;
        final Vector3f axis;
        final float spinSpeed;
        final int shade;
        float angle;

        Shard(Vec3d pos, Vec3d vel, Vec3d a, Vec3d b, Vec3d d) {
            super((long) (lifeTime.getValue() * 1000));
            ThreadLocalRandom r = ThreadLocalRandom.current();
            this.pos = pos; this.vel = vel;
            float k = 0.92f;
            local = new Vector3f[]{
                    new Vector3f((float) a.x, (float) a.y, (float) a.z).mul(k),
                    new Vector3f((float) b.x, (float) b.y, (float) b.z).mul(k),
                    new Vector3f((float) d.x, (float) d.y, (float) d.z).mul(k)};
            axis = new Vector3f(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f).normalize();
            spinSpeed = 4f + r.nextFloat() * 8f;
            shade = 190 + r.nextInt(66);
        }

        @Override
        boolean update(float dt, long now) {
            if (now - born > GLASS_FREEZE_MS) {
                vel = vel.add(0, -GRAVITY * 0.7 * dt, 0);
                pos = pos.add(vel.multiply(dt));
                angle += spinSpeed * dt;
            }
            return super.update(dt, now);
        }

        @Override
        void draw(Ctx c, long now) {
            float t = age(now);
            float alpha = t < 0.5f ? 1f : 1f - (t - 0.5f) / 0.5f;
            boolean intact = now - born < GLASS_FREEZE_MS;

            Quaternionf q = new Quaternionf().fromAxisAngleRad(axis, angle);
            Vec3d[] p = new Vec3d[3];
            for (int i = 0; i < 3; i++) {
                Vector3f v = new Vector3f(local[i]).rotate(q);
                p[i] = pos.add(v.x, v.y, v.z);
            }
            Color base = themeColor.getValue() ? accent() : new Color(200, 235, 255);
            int rgb = new Color(base.getRed() * shade / 255, base.getGreen() * shade / 255, base.getBlue() * shade / 255).getRGB();
            tri(c, p[0], p[1], p[2], argb((int) ((intact ? 210 : 160) * alpha), rgb));
        }
    }

    private final class Pulse extends Fx {
        final LivingEntity target;
        final Vec3d offset;

        Pulse(LivingEntity target, Vec3d hit) {
            super((long) (lifeTime.getValue() * 900));
            this.target = target;
            this.offset = hit.subtract(target.getPos());
        }

        @Override
        boolean update(float dt, long now) {
            return !target.isRemoved() && super.update(dt, now);
        }

        @Override
        void draw(Ctx c, long now) {
            float t = age(now);
            Vec3d ep = target.getLerpedPos(c.tickDelta);
            Box b = target.getBoundingBox().offset(ep.subtract(target.getPos())).expand(0.04);
            Vec3d hit = ep.add(offset);

            double maxD = Math.sqrt(b.getLengthX() * b.getLengthX() + b.getLengthY() * b.getLengthY() + b.getLengthZ() * b.getLengthZ());
            double r1 = t * maxD * 1.15;
            double r2 = (t - 0.3) * maxD * 1.15;
            float band = 0.10f + 0.08f * size.getValue();
            float fade = 1f - t;
            Color ac = accent();

            face(c, 0, b.minX, b.minY, b.maxY, b.minZ, b.maxZ, hit, r1, r2, band, fade, ac);
            face(c, 0, b.maxX, b.minY, b.maxY, b.minZ, b.maxZ, hit, r1, r2, band, fade, ac);
            face(c, 1, b.minY, b.minX, b.maxX, b.minZ, b.maxZ, hit, r1, r2, band, fade, ac);
            face(c, 1, b.maxY, b.minX, b.maxX, b.minZ, b.maxZ, hit, r1, r2, band, fade, ac);
            face(c, 2, b.minZ, b.minX, b.maxX, b.minY, b.maxY, hit, r1, r2, band, fade, ac);
            face(c, 2, b.maxZ, b.minX, b.maxX, b.minY, b.maxY, hit, r1, r2, band, fade, ac);
        }

        private void face(Ctx c, int axis, double fixed, double a0, double a1, double b0, double b1,
                          Vec3d hit, double r1, double r2, float band, float fade, Color ac) {
            double step = 0.075;
            for (double a = a0; a < a1 - 1e-4; a += step) {
                double a2 = Math.min(a + step, a1);
                for (double bb = b0; bb < b1 - 1e-4; bb += step) {
                    double b2 = Math.min(bb + step, b1);
                    double d = pt(axis, fixed, (a + a2) / 2, (bb + b2) / 2).distanceTo(hit);
                    double w1 = Math.exp(-sq((d - r1) / band));
                    double w2 = r2 > 0 ? Math.exp(-sq((d - r2) / band)) * 0.6 : 0;
                    double glow = Math.exp(-d / 0.25) * 0.5 * fade;
                    float k = (float) (Math.max(Math.max(w1, w2) * fade, glow));
                    if (k < 0.04f) continue;
                    quad(c, pt(axis, fixed, a, bb), pt(axis, fixed, a2, bb),
                            pt(axis, fixed, a2, b2), pt(axis, fixed, a, b2), argb((int) (210 * k), ac));
                }
            }
        }
    }

    private static final class Ctx {
        BufferBuilder buf;
        Matrix4f mat;
        Vec3d cam;
        Vector3f right, up;
        float tickDelta;
    }

    private static void vtx(Ctx c, Vec3d p, int col) {
        c.buf.vertex(c.mat, (float) (p.x - c.cam.x), (float) (p.y - c.cam.y), (float) (p.z - c.cam.z)).color(col);
    }

    private static void quad(Ctx c, Vec3d a, Vec3d b, Vec3d d, Vec3d e, int col) {
        vtx(c, a, col); vtx(c, b, col); vtx(c, d, col); vtx(c, e, col);
    }

    private static void tri(Ctx c, Vec3d a, Vec3d b, Vec3d d, int col) {
        quad(c, a, b, d, d, col);
    }

    private static void billboard(Ctx c, Vec3d p, float w, float h, int col) {
        Vec3d r = new Vec3d(c.right.x * w, c.right.y * w, c.right.z * w);
        Vec3d u = new Vec3d(c.up.x * h, c.up.y * h, c.up.z * h);
        quad(c, p.subtract(r).subtract(u), p.add(r).subtract(u), p.add(r).add(u), p.subtract(r).add(u), col);
    }

    private static Vec3d plane(Vec3d center, Vector3f right, Vector3f up, float ang, float rad) {
        double cs = Math.cos(ang) * rad, sn = Math.sin(ang) * rad;
        return center.add(right.x * cs + up.x * sn, right.y * cs + up.y * sn, right.z * cs + up.z * sn);
    }

    private static Vec3d pt(int axis, double fixed, double a, double b) {
        return switch (axis) {
            case 0 -> new Vec3d(fixed, a, b);
            case 1 -> new Vec3d(a, fixed, b);
            default -> new Vec3d(a, b, fixed);
        };
    }

    private static double sq(double v) { return v * v; }

    private static int argb(int a, int rgb) { return (MathHelper.clamp(a, 0, 255) << 24) | (rgb & 0xFFFFFF); }

    private static int argb(int a, Color c) { return argb(a, c.getRGB()); }

    private Color accent() { return ThemeManager.getInstance().getCurrentTheme().getAccentColor(); }
}