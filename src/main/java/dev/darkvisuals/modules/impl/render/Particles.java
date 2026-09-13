package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.util.TntCameraShakeState;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class Particles extends Module {

    private static final Identifier GLOW_TEXTURE = darkvisuals.id("hud/glow.png");

    private final EnumSetting<ParticlesMode> mode =
            new EnumSetting<>("Режим", ParticlesMode.METEORITES);

    private final NumberSetting spawnInterval =
            new NumberSetting("Интервал (сек)", 3.0f, 0.5f, 15.0f, 0.5f);

    private final NumberSetting radius =
            new NumberSetting("Радиус", 25f, 5f, 64f, 1f);

    private final BooleanSetting sounds = new BooleanSetting("Звуки", true);
    private final BooleanSetting cameraShake = new BooleanSetting("Тряска камеры", true);
    private final BooleanSetting screenFlash = new BooleanSetting("Вспышка экрана", true);

    private final NumberSetting meteorSize =
            new NumberSetting("Размер метеорита", 1.2f, 0.4f, 3.0f, 0.1f, () -> mode.getValue() == ParticlesMode.METEORITES);
    private final NumberSetting meteorSpeed =
            new NumberSetting("Скорость метеорита", 2.2f, 0.5f, 5.0f, 0.1f, () -> mode.getValue() == ParticlesMode.METEORITES);
    private final ColorSetting meteorColor =
            new ColorSetting("Цвет метеорита", new Color(255, 60, 30, 255).getRGB());
    private final ColorSetting trailColor =
            new ColorSetting("Цвет хвоста", new Color(255, 150, 40, 255).getRGB());

    private final ColorSetting boltColor =
            new ColorSetting("Цвет молнии", new Color(120, 170, 255, 255).getRGB());
    private final NumberSetting boltWidth =
            new NumberSetting("Толщина молнии", 1.0f, 0.3f, 2.5f, 0.1f, () -> mode.getValue() == ParticlesMode.LIGHTNING);
    private final NumberSetting branches =
            new NumberSetting("Ветвления", 3f, 0f, 8f, 1f, () -> mode.getValue() == ParticlesMode.LIGHTNING);

    private final NumberSetting explosionSize =
            new NumberSetting("Размер взрыва", 1.0f, 0.3f, 3.0f, 0.1f);
    private final BooleanSetting debris = new BooleanSetting("3D обломки", true);
    private final NumberSetting debrisCount =
            new NumberSetting("Кол-во обломков", 10f, 0f, 30f, 1f, debris::getValue);
    private final ColorSetting explosionColor =
            new ColorSetting("Цвет взрыва", new Color(255, 120, 40, 255).getRGB());

    private final Random rnd = new Random();

    private final List<Meteor> meteors = new ArrayList<>();
    private final List<Bolt> bolts = new ArrayList<>();
    private final List<Debris> debrisList = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final List<ShockRing> rings = new ArrayList<>();
    private final List<ImpactFlash> flashes = new ArrayList<>();
    private final List<ScreenFlash> screenFlashes = new ArrayList<>();

    private long nextSpawnMs = 0;

    private static final int MAX_METEORS = 8;
    private static final int MAX_BOLTS = 4;
    private static final int MAX_SPARKS = 400;
    private static final int MAX_DEBRIS = 140;
    private static final int RING_SEGMENTS = 80;
    private static final long RING_TTL_MS = 1400L;

    public Particles() {
        super("Particles", Category.Render, "Эффекты частиц: метеориты с 3D обломками и 3D молнии со взрывами");
        getSettings().add(mode);
        getSettings().add(spawnInterval);
        getSettings().add(radius);
        getSettings().add(sounds);
        getSettings().add(cameraShake);
        getSettings().add(screenFlash);
        getSettings().add(meteorSize);
        getSettings().add(meteorSpeed);
        getSettings().add(meteorColor);
        getSettings().add(trailColor);
        getSettings().add(boltColor);
        getSettings().add(boltWidth);
        getSettings().add(branches);
        getSettings().add(explosionSize);
        getSettings().add(debris);
        getSettings().add(debrisCount);
        getSettings().add(explosionColor);

        boltColor.setVisible(() -> mode.getValue() == ParticlesMode.LIGHTNING);
        meteorColor.setVisible(() -> mode.getValue() == ParticlesMode.METEORITES);
        trailColor.setVisible(() -> mode.getValue() == ParticlesMode.METEORITES);
        screenFlash.setVisible(() -> mode.getValue() == ParticlesMode.LIGHTNING);
    }

    @Override
    public void onDisable() {
        meteors.clear();
        bolts.clear();
        debrisList.clear();
        sparks.clear();
        rings.clear();
        flashes.clear();
        screenFlashes.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        if (now >= nextSpawnMs) {
            nextSpawnMs = now + (long) (spawnInterval.getValue() * 1000f);
            if (mode.getValue() == ParticlesMode.METEORITES) spawnMeteor();
            else spawnBolt();
        }

        updateMeteors();
        updateBolts(now);
        updateDebris();
        updateSparks();

        rings.removeIf(r -> now - r.birth >= RING_TTL_MS);
        flashes.removeIf(f -> now - f.birth >= f.life);
        screenFlashes.removeIf(f -> now - f.birth >= f.life);
    }

    // ------------------------------------------------------------------
    // Спавн
    // ------------------------------------------------------------------

    private Vec3d randomGroundPos() {
        double angle = rnd.nextDouble() * Math.PI * 2;
        double dist = radius.getValue() * (0.25 + 0.75 * rnd.nextDouble());
        double x = mc.player.getX() + Math.cos(angle) * dist;
        double z = mc.player.getZ() + Math.sin(angle) * dist;

        double y = mc.player.getY() + 24;
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        for (int i = 0; i < 60; i++) {
            pos = pos.down();
            if (pos.getY() < mc.world.getBottomY() - 8) return null;
            if (!mc.world.getBlockState(pos).isAir()) {
                return new Vec3d(x, pos.up().getY(), z);
            }
        }
        return null;
    }

    private void spawnMeteor() {
        if (meteors.size() >= MAX_METEORS) return;

        Vec3d target = randomGroundPos();
        if (target == null) return;

        Vec3d start = target.add(
                (rnd.nextDouble() - 0.5) * 30,
                35 + rnd.nextDouble() * 20,
                (rnd.nextDouble() - 0.5) * 30);

        Vec3d dir = target.subtract(start).normalize().multiply(meteorSpeed.getValue());
        meteors.add(new Meteor(start, dir, meteorSize.getValue() * 0.6f));
    }

    private void spawnBolt() {
        if (bolts.size() >= MAX_BOLTS) return;

        Vec3d target = randomGroundPos();
        if (target == null) return;

        bolts.add(new Bolt(target, boltWidth.getValue(), (int) (float) branches.getValue()));
        if (mode.getValue() == ParticlesMode.LIGHTNING && screenFlash.getValue()) {
            screenFlashes.add(new ScreenFlash(System.currentTimeMillis(), 260, 0.55f));
        }
        if (sounds.getValue()) {
            mc.world.playSound(target.x, target.y, target.z,
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 2f, 0.8f + rnd.nextFloat() * 0.4f, true);
        }
    }

    // ------------------------------------------------------------------
    // Физика
    // ------------------------------------------------------------------

    private void updateMeteors() {
        Iterator<Meteor> it = meteors.iterator();
        while (it.hasNext()) {
            Meteor meteor = it.next();
            meteor.prevPos = meteor.pos;
            meteor.pos = meteor.pos.add(meteor.velocity);
            meteor.velocity = meteor.velocity.add(0, -0.02, 0);
            meteor.trail.addFirst(meteor.pos);
            while (meteor.trail.size() > 14) meteor.trail.removeLast();

            // огненные искры вдоль полёта
            if (sparks.size() < MAX_SPARKS && rnd.nextInt(2) == 0) {
                Vec3d v = new Vec3d(
                        (rnd.nextDouble() - 0.5) * 0.08,
                        (rnd.nextDouble() - 0.5) * 0.08,
                        (rnd.nextDouble() - 0.5) * 0.08);
                sparks.add(Spark.ember(meteor.pos, v, trailColor.getColor(), meteor.size * 0.3f, 400 + rnd.nextInt(300)));
            }

            BlockPos hitPos = BlockPos.ofFloored(meteor.pos);
            boolean hit = meteor.pos.y < mc.world.getBottomY() - 4 || !mc.world.getBlockState(hitPos).isAir();
            if (hit) {
                it.remove();
                triggerExplosion(meteor.pos, hitPos, 1.0f);
            }
        }
    }

    private void updateBolts(long now) {
        Iterator<Bolt> it = bolts.iterator();
        while (it.hasNext()) {
            Bolt bolt = it.next();
            if (!bolt.exploded && now - bolt.birth >= bolt.explodeDelayMs) {
                bolt.exploded = true;
                BlockPos hitPos = BlockPos.ofFloored(bolt.target);
                triggerExplosion(bolt.target, hitPos, 0.8f);
            }
            if (now - bolt.birth >= bolt.life) it.remove();
        }
    }

    private void triggerExplosion(Vec3d pos, BlockPos hitBlock, float strengthScale) {
        float size = explosionSize.getValue() * strengthScale;

        // ванильная вспышка взрыва
        mc.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 0.3, pos.z, 0, 0, 0);

        flashes.add(new ImpactFlash(pos, size));
        rings.add(new ShockRing(pos.add(0, 0.1, 0), size));

        // искры и дым
        int emberCount = Math.round(24 * size);
        for (int i = 0; i < emberCount && sparks.size() < MAX_SPARKS; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double sp = 0.15 + rnd.nextDouble() * 0.3 * size;
            Vec3d v = new Vec3d(Math.cos(ang) * sp, 0.08 + rnd.nextDouble() * 0.25, Math.sin(ang) * sp);
            sparks.add(Spark.ember(pos, v, explosionColor.getColor(), 0.3f, 350 + rnd.nextInt(300)));
        }
        int smokeCount = Math.round(10 * size);
        for (int i = 0; i < smokeCount && sparks.size() < MAX_SPARKS; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            Vec3d v = new Vec3d(Math.cos(ang) * 0.05, 0.05 + rnd.nextDouble() * 0.06, Math.sin(ang) * 0.05);
            sparks.add(Spark.smoke(pos.add((rnd.nextDouble() - 0.5) * 0.6, rnd.nextDouble() * 0.4, (rnd.nextDouble() - 0.5) * 0.6),
                    v, new Color(80, 75, 70, 200), 1.0f + rnd.nextFloat() * 0.6f, 900 + rnd.nextInt(600)));
        }

        // 3D обломки цвета блока, в который попали
        if (debris.getValue() && debrisCount.getValue() > 0) {
            Color blockColor = blockTint(hitBlock);
            int count = Math.round(debrisCount.getValue());
            for (int i = 0; i < count && debrisList.size() < MAX_DEBRIS; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                double sp = 0.12 + rnd.nextDouble() * 0.28 * size;
                debrisList.add(new Debris(
                        pos.add((rnd.nextDouble() - 0.5) * 0.5, 0.2, (rnd.nextDouble() - 0.5) * 0.5),
                        new Vec3d(Math.cos(ang) * sp, 0.25 + rnd.nextDouble() * 0.4, Math.sin(ang) * sp),
                        0.12f + rnd.nextFloat() * 0.2f,
                        blockColor,
                        1800 + rnd.nextInt(1500)));
            }
        }

        if (cameraShake.getValue()) {
            double dist = mc.player.getPos().distanceTo(pos);
            float prox = (float) MathHelper.clamp(1.0 - dist / (radius.getValue() * 1.5), 0.0, 1.0);
            if (prox > 0.02f) TntCameraShakeState.trigger(5f * size * prox, (long) (450 * size));
        }

        if (sounds.getValue()) {
            mc.world.playSound(pos.x, pos.y, pos.z,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.6f, 0.7f + rnd.nextFloat() * 0.3f, true);
        }
    }

    private Color blockTint(BlockPos pos) {
        try {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isAir()) {
                int rgb = state.getMapColor(mc.world, pos).color;
                Color c = new Color(rgb, false);
                // осветляем тёмные цвета, чтобы обломки читались
                float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                return new Color(Color.HSBtoRGB(hsb[0], Math.min(1f, hsb[1] * 1.15f), Math.max(0.45f, hsb[2] * 1.2f)));
            }
        } catch (Throwable ignored) {
        }
        return new Color(120, 90, 70, 255);
    }

    private void updateDebris() {
        Iterator<Debris> it = debrisList.iterator();
        while (it.hasNext()) {
            Debris d = it.next();
            if (System.currentTimeMillis() - d.birth >= d.life) {
                it.remove();
                continue;
            }
            d.prevPos = d.pos;
            d.vel = d.vel.add(0, -0.035, 0);
            d.pos = d.pos.add(d.vel);
            d.rotX += d.rotSpeedX;
            d.rotY += d.rotSpeedY;
            d.rotZ += d.rotSpeedZ;

            // отскок от земли
            BlockPos below = BlockPos.ofFloored(d.pos.x, d.pos.y - d.size * 0.5, d.pos.z);
            if (!mc.world.getBlockState(below).isAir() && d.pos.y - d.size * 0.5 < below.up().getY()) {
                d.pos = new Vec3d(d.pos.x, below.up().getY() + d.size * 0.5, d.pos.z);
                if (Math.abs(d.vel.y) > 0.04) {
                    d.vel = new Vec3d(d.vel.x * 0.6, -d.vel.y * 0.4, d.vel.z * 0.6);
                    d.rotSpeedX *= 0.5;
                    d.rotSpeedY *= 0.5;
                } else {
                    d.vel = Vec3d.ZERO;
                    d.rotSpeedX *= 0.8;
                    d.rotSpeedY *= 0.8;
                }
            }
        }
    }

    private void updateSparks() {
        Iterator<Spark> it = sparks.iterator();
        while (it.hasNext()) {
            Spark s = it.next();
            long age = System.currentTimeMillis() - s.birth;
            if (age >= s.life) {
                it.remove();
                continue;
            }
            float progress = age / (float) s.life;
            s.prevPos = s.pos;
            s.pos = s.pos.add(s.vel);
            if (s.smoke) {
                s.vel = s.vel.multiply(0.98);
                s.size += 0.012f;
                s.alpha = (1f - progress) * 0.5f;
            } else {
                s.vel = s.vel.multiply(0.94).add(0, -0.005, 0);
                s.alpha = 1f - progress;
            }
        }
    }

    // ------------------------------------------------------------------
    // Рендер
    // ------------------------------------------------------------------

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        renderSparks(matrices, cam);
        renderMeteors(matrices, cam);
        renderBolts(matrices, cam);
        renderFlashes(matrices, cam);
        renderRings(matrices, cam);
        renderDebris(matrices, cam);
    }

    private void renderSparks(MatrixStack matrices, Vec3d cam) {
        if (sparks.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        for (Spark s : sparks) {
            RenderSystem.defaultBlendFunc();
            if (!s.smoke) {
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            }

            matrices.push();
            matrices.translate((float) (s.pos.x - cam.x), (float) (s.pos.y - cam.y), (float) (s.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            drawBillboard(matrices.peek().getPositionMatrix(), s.size, s.color, s.alpha);
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderMeteors(MatrixStack matrices, Vec3d cam) {
        if (meteors.isEmpty()) return;
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        Color core = meteorColor.getColor();
        Color tail = trailColor.getColor();
        Color hot = new Color(255, 230, 180, 255);

        for (Meteor meteor : meteors) {
            // хвост из затухающих свечений
            int i = 0;
            for (Vec3d p : meteor.trail) {
                float f = 1f - i / (float) meteor.trail.size();
                float size = meteor.size * (0.35f + 0.65f * f);
                int alpha = (int) (170 * f * f);
                if (alpha > 2) {
                    matrices.push();
                    matrices.translate((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
                    drawBillboard(matrices.peek().getPositionMatrix(), size, tail, alpha);
                    matrices.pop();
                }
                i++;
            }

            // ядро с пульсацией
            float pulse = 0.85f + 0.15f * MathHelper.sin(now / 60f + meteor.seed);
            matrices.push();
            matrices.translate((float) (meteor.pos.x - cam.x), (float) (meteor.pos.y - cam.y), (float) (meteor.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            drawBillboard(matrices.peek().getPositionMatrix(), meteor.size * 2.4f * pulse, core, 130);
            drawBillboard(matrices.peek().getPositionMatrix(), meteor.size * 1.1f * pulse, hot, 230);
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderBolts(MatrixStack matrices, Vec3d cam) {
        if (bolts.isEmpty()) return;
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Color color = boltColor.getColor();
        Color white = new Color(240, 248, 255, 255);

        for (Bolt bolt : bolts) {
            float t = (now - bolt.birth) / (float) bolt.life;
            float fade = t < 0.7f ? 1f : 1f - (t - 0.7f) / 0.3f;
            // мерцание
            float flicker = MathHelper.clamp(0.7f + 0.3f * MathHelper.sin(now / 25f + bolt.seed * 10f)
                    * MathHelper.sin(now / 61f + bolt.seed * 3f), 0f, 1f);

            drawBoltPath(matrices, cam, bolt.mainPath, bolt.width, color, white, fade * flicker);
            for (Bolt.Branch branch : bolt.branches) {
                drawBoltPath(matrices, cam, branch.points, bolt.width * branch.widthScale, color, white, fade * flicker * 0.75f);
            }

            // свечение в точке удара
            RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            matrices.push();
            matrices.translate((float) (bolt.target.x - cam.x), (float) (bolt.target.y - cam.y), (float) (bolt.target.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            drawBillboard(matrices.peek().getPositionMatrix(), 2.2f * bolt.width, color, (int) (150 * fade * flicker));
            matrices.pop();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawBoltPath(MatrixStack matrices, Vec3d cam, List<Vec3d> points, float width,
                              Color color, Color core, float alphaScale) {
        if (points.size() < 2 || alphaScale <= 0.02f) return;

        // два прохода: широкое цветное свечение + яркое белое ядро
        for (int pass = 0; pass < 2; pass++) {
            float halfWidth = pass == 0 ? width * 0.55f : width * 0.18f;
            Color c = pass == 0 ? color : core;
            int alpha = (int) ((pass == 0 ? 110 : 235) * alphaScale);
            if (alpha <= 3) continue;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (int i = 0; i < points.size() - 1; i++) {
                Vec3d a = points.get(i).subtract(cam);
                Vec3d b = points.get(i + 1).subtract(cam);
                Vec3d dir = b.subtract(a);
                if (dir.lengthSquared() < 1e-6) continue;
                Vec3d mid = a.add(b).multiply(0.5);
                Vec3d normal = dir.crossProduct(mid);
                if (normal.lengthSquared() < 1e-6) continue;
                Vec3d side = normal.normalize().multiply(halfWidth);

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                buffer.vertex(matrix, (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z)).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z)).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, (float) (b.x + side.x), (float) (b.y + side.y), (float) (b.z + side.z)).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, (float) (b.x - side.x), (float) (b.y - side.y), (float) (b.z - side.z)).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private void renderFlashes(MatrixStack matrices, Vec3d cam) {
        if (flashes.isEmpty()) return;
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        Color color = explosionColor.getColor();
        Color hot = new Color(255, 240, 200, 255);

        for (ImpactFlash f : flashes) {
            float progress = (now - f.birth) / (float) f.life;
            float ease = (float) Easing.EASE_OUT_CIRC.apply(1f - progress);
            float size = 2.2f * f.size * (0.6f + 0.9f * progress);
            int alphaOuter = (int) (160 * ease);
            int alphaInner = (int) (240 * ease);

            matrices.push();
            matrices.translate((float) (f.pos.x - cam.x), (float) (f.pos.y - cam.y), (float) (f.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            drawBillboard(matrices.peek().getPositionMatrix(), size * 2f, color, alphaOuter);
            drawBillboard(matrices.peek().getPositionMatrix(), size, hot, alphaInner);
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderRings(MatrixStack matrices, Vec3d cam) {
        if (rings.isEmpty()) return;
        long now = System.currentTimeMillis();

        Color color = explosionColor.getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (ShockRing ring : rings) {
            float t = (now - ring.birth) / (float) RING_TTL_MS;
            float radius = (float) Easing.OUT_QUAD.apply(t) * 6f * ring.size;
            float fade = t < 0.6f ? 1f : (float) Easing.OUT_QUAD.apply(1f - (t - 0.6f) / 0.4f);
            int crestAlpha = (int) (200 * fade);
            if (crestAlpha <= 2 || radius <= 0.05f) continue;

            matrices.push();
            matrices.translate((float) (ring.origin.x - cam.x), (float) (ring.origin.y - cam.y), (float) (ring.origin.z - cam.z));
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            float inner = Math.max(0f, radius - 0.9f);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= RING_SEGMENTS; i++) {
                float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
                float cos = MathHelper.cos(angle);
                float sin = MathHelper.sin(angle);
                buffer.vertex(matrix, cos * inner, 0.05f, sin * inner).color(color.getRed(), color.getGreen(), color.getBlue(), 0);
                buffer.vertex(matrix, cos * radius, 0.05f, sin * radius).color(color.getRed(), color.getGreen(), color.getBlue(), crestAlpha);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderDebris(MatrixStack matrices, Vec3d cam) {
        if (debrisList.isEmpty()) return;
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Debris d : debrisList) {
            long age = now - d.birth;
            float fade = age >= d.life - 500 ? Math.max(0f, (d.life - age) / 500f) : 1f;
            int alpha = (int) (255 * fade);
            if (alpha <= 4) continue;

            matrices.push();
            matrices.translate((float) (d.pos.x - cam.x), (float) (d.pos.y - cam.y), (float) (d.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(d.rotX));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(d.rotY));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(d.rotZ));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            drawShadedCube(matrix, d.size, d.color, alpha);
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void drawShadedCube(Matrix4f matrix, float size, Color color, int alpha) {
        float h = size * 0.5f;
        Color top = shade(color, 1.05f, alpha);
        Color mid = shade(color, 0.85f, alpha);
        Color side = shade(color, 0.68f, alpha);
        Color bottom = shade(color, 0.5f, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // верх / низ
        buffer.vertex(matrix, -h, h, -h).color(top.getRGB());
        buffer.vertex(matrix, -h, h, h).color(top.getRGB());
        buffer.vertex(matrix, h, h, h).color(top.getRGB());
        buffer.vertex(matrix, h, h, -h).color(top.getRGB());
        buffer.vertex(matrix, -h, -h, -h).color(bottom.getRGB());
        buffer.vertex(matrix, h, -h, -h).color(bottom.getRGB());
        buffer.vertex(matrix, h, -h, h).color(bottom.getRGB());
        buffer.vertex(matrix, -h, -h, h).color(bottom.getRGB());

        // север / юг
        buffer.vertex(matrix, -h, -h, h).color(side.getRGB());
        buffer.vertex(matrix, h, -h, h).color(side.getRGB());
        buffer.vertex(matrix, h, h, h).color(mid.getRGB());
        buffer.vertex(matrix, -h, h, h).color(mid.getRGB());
        buffer.vertex(matrix, -h, -h, -h).color(side.getRGB());
        buffer.vertex(matrix, -h, h, -h).color(mid.getRGB());
        buffer.vertex(matrix, h, h, -h).color(mid.getRGB());
        buffer.vertex(matrix, h, -h, -h).color(side.getRGB());

        // восток / запад
        buffer.vertex(matrix, h, -h, -h).color(side.getRGB());
        buffer.vertex(matrix, h, h, -h).color(mid.getRGB());
        buffer.vertex(matrix, h, h, h).color(mid.getRGB());
        buffer.vertex(matrix, h, -h, h).color(side.getRGB());
        buffer.vertex(matrix, -h, -h, -h).color(side.getRGB());
        buffer.vertex(matrix, -h, -h, h).color(side.getRGB());
        buffer.vertex(matrix, -h, h, h).color(mid.getRGB());
        buffer.vertex(matrix, -h, h, -h).color(mid.getRGB());

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static Color shade(Color color, float factor, int alpha) {
        return new Color(
                MathHelper.clamp((int) (color.getRed() * factor), 0, 255),
                MathHelper.clamp((int) (color.getGreen() * factor), 0, 255),
                MathHelper.clamp((int) (color.getBlue() * factor), 0, 255),
                alpha);
    }

    private void drawBillboard(Matrix4f matrix, float size, Color color, int alpha) {
        float half = size / 2f;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        buffer.vertex(matrix, half, half, 0).texture(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawBillboard(Matrix4f matrix, float size, Color color, float alpha) {
        drawBillboard(matrix, size, color, (int) (MathHelper.clamp(alpha, 0f, 1f) * 255));
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (screenFlashes.isEmpty()) return;

        long now = System.currentTimeMillis();
        float peak = 0f;
        for (ScreenFlash f : screenFlashes) {
            float progress = (now - f.birth) / (float) f.life;
            if (progress >= 1f) continue;
            peak = Math.max(peak, f.strength * (1f - progress));
        }
        if (peak <= 0.01f) return;

        int width = event.getContext().getScaledWindowWidth();
        int height = event.getContext().getScaledWindowHeight();
        int alpha = (int) (140 * peak);
        event.getContext().fill(0, 0, width, height, (alpha << 24) | 0xF4F8FF);
    }

    // ------------------------------------------------------------------
    // Внутренние состояния
    // ------------------------------------------------------------------

    private static final class Meteor {
        Vec3d pos, prevPos, velocity;
        final Deque<Vec3d> trail = new ArrayDeque<>();
        final float size;
        final float seed;

        Meteor(Vec3d pos, Vec3d velocity, float size) {
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.size = size;
            this.seed = (float) (Math.random() * 100f);
        }
    }

    private static final class Bolt {
        final List<Vec3d> mainPath = new ArrayList<>();
        final List<Branch> branches = new ArrayList<>();
        final Vec3d target;
        final long birth = System.currentTimeMillis();
        final long life;
        final long explodeDelayMs;
        final float width;
        final float seed = (float) (Math.random() * 100f);
        boolean exploded = false;

        Bolt(Vec3d target, float width, int branchCount) {
            this.target = target;
            this.width = width;

            Random random = new Random();
            double topY = target.y + 45 + random.nextDouble() * 15;
            Vec3d start = new Vec3d(target.x + (random.nextDouble() - 0.5) * 8, topY, target.z + (random.nextDouble() - 0.5) * 8);

            // ломаная от неба к земле
            Vec3d current = start;
            mainPath.add(current);
            Vec3d step = target.subtract(start).normalize();
            double remaining = start.distanceTo(target);
            double segLen = 3.0;
            while (remaining > segLen) {
                double jx = (random.nextDouble() - 0.5) * 2.4;
                double jz = (random.nextDouble() - 0.5) * 2.4;
                double jy = (random.nextDouble() - 0.5) * 1.2;
                current = current.add(step.x * segLen + jx, step.y * segLen + jy, step.z * segLen + jz);
                mainPath.add(current);
                remaining = current.distanceTo(target);
                step = target.subtract(current).normalize();
            }
            mainPath.add(target);

            // ветвления
            for (int i = 0; i < branchCount && mainPath.size() > 4; i++) {
                int anchorIdx = 1 + random.nextInt(mainPath.size() - 3);
                Vec3d anchor = mainPath.get(anchorIdx);
                Vec3d dir = target.subtract(anchor).normalize();
                double ang = (random.nextDouble() - 0.5) * 1.8;
                Vec3d rotated = new Vec3d(
                        dir.x * Math.cos(ang) - dir.z * Math.sin(ang),
                        dir.y + 0.35,
                        dir.x * Math.sin(ang) + dir.z * Math.cos(ang)).normalize();

                Branch branch = new Branch(0.45f + random.nextFloat() * 0.25f);
                Vec3d cur = anchor;
                branch.points.add(cur);
                int segs = 2 + random.nextInt(3);
                for (int s = 0; s < segs; s++) {
                    cur = cur.add(rotated.multiply(1.6 + random.nextDouble() * 1.2))
                            .add((random.nextDouble() - 0.5) * 0.8, (random.nextDouble() - 0.5) * 0.8, (random.nextDouble() - 0.5) * 0.8);
                    branch.points.add(cur);
                }
                branches.add(branch);
            }

            this.life = 500 + random.nextInt(300);
            this.explodeDelayMs = 120 + random.nextInt(120);
        }

        private static final class Branch {
            final List<Vec3d> points = new ArrayList<>();
            final float widthScale;

            Branch(float widthScale) {
                this.widthScale = widthScale;
            }
        }
    }

    private static final class Debris {
        Vec3d pos, prevPos, vel;
        float rotX, rotY, rotZ;
        float rotSpeedX, rotSpeedY, rotSpeedZ;
        final float size;
        final Color color;
        final long birth = System.currentTimeMillis();
        final long life;

        Debris(Vec3d pos, Vec3d vel, float size, Color color, long life) {
            this.pos = pos;
            this.prevPos = pos;
            this.vel = vel;
            this.size = size;
            this.color = color;
            this.life = life;
            Random random = new Random();
            this.rotX = random.nextFloat() * 360f;
            this.rotY = random.nextFloat() * 360f;
            this.rotZ = random.nextFloat() * 360f;
            this.rotSpeedX = (random.nextFloat() - 0.5f) * 24f;
            this.rotSpeedY = (random.nextFloat() - 0.5f) * 24f;
            this.rotSpeedZ = (random.nextFloat() - 0.5f) * 24f;
        }
    }

    private static final class Spark {
        Vec3d pos, prevPos, vel;
        final boolean smoke;
        final Color color;
        float size;
        float alpha = 1f;
        final long birth = System.currentTimeMillis();
        final long life;

        private Spark(Vec3d pos, Vec3d vel, Color color, float size, long life, boolean smoke) {
            this.pos = pos;
            this.prevPos = pos;
            this.vel = vel;
            this.color = color;
            this.size = size;
            this.life = life;
            this.smoke = smoke;
        }

        static Spark ember(Vec3d pos, Vec3d vel, Color color, float size, long life) {
            return new Spark(pos, vel, color, size, life, false);
        }

        static Spark smoke(Vec3d pos, Vec3d vel, Color color, float size, long life) {
            return new Spark(pos, vel, color, size, life, true);
        }
    }

    private static final class ImpactFlash {
        final Vec3d pos;
        final float size;
        final long birth = System.currentTimeMillis();
        final long life = 320L;

        ImpactFlash(Vec3d pos, float size) {
            this.pos = pos;
            this.size = size;
        }
    }

    private static final class ShockRing {
        final Vec3d origin;
        final float size;
        final long birth = System.currentTimeMillis();

        ShockRing(Vec3d origin, float size) {
            this.origin = origin;
            this.size = size;
        }
    }

    private record ScreenFlash(long birth, long life, float strength) {}
}
