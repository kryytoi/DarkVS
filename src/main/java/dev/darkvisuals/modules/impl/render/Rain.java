package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Rain extends Module {

    public enum Preset implements Nameable {
        Drizzle("Drizzle"),
        Rain("Rain"),
        Downpour("Downpour"),
        Storm("Storm");

        private final String displayName;
        Preset(String displayName) { this.displayName = displayName; }
        @Override public String getName() { return displayName; }
    }

    public enum ColorMode implements Nameable {
        Realistic("Realistic"),
        Theme("Theme"),
        Custom("Custom");

        private final String displayName;
        ColorMode(String displayName) { this.displayName = displayName; }
        @Override public String getName() { return displayName; }
    }

    private final EnumSetting<Preset> preset = new EnumSetting<>("Пресет", Preset.Rain);
    private final NumberSetting density = new NumberSetting("Плотность", 1.0f, 0.1f, 3.0f, 0.1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 26f, 8f, 64f, 1f);
    private final NumberSetting altitude = new NumberSetting("Высота", 16f, 6f, 40f, 1f);
    private final NumberSetting dropSize = new NumberSetting("Размер капель", 1.0f, 0.3f, 2.5f, 0.1f);
    private final NumberSetting fallSpeed = new NumberSetting("Скорость падения", 1.0f, 0.3f, 2.5f, 0.1f);
    private final NumberSetting wind = new NumberSetting("Ветер", 1.0f, 0.0f, 3.0f, 0.1f);
    private final NumberSetting opacity = new NumberSetting("Прозрачность", 1.0f, 0.1f, 2.0f, 0.05f);
    private final BooleanSetting splashes = new BooleanSetting("Всплески", true);
    private final BooleanSetting droplets = new BooleanSetting("Брызги", true, splashes::getValue);
    private final BooleanSetting groundMist = new BooleanSetting("Туман у земли", true);
    private final BooleanSetting lightning = new BooleanSetting("Вспышки молнии", false);
    private final BooleanSetting skyCheck = new BooleanSetting("Проверка крыши", true);
    private final BooleanSetting onlyWhenRaining = new BooleanSetting("Только при дожде в мире", false);
    private final EnumSetting<ColorMode> colorMode = new EnumSetting<>("Цвет", ColorMode.Realistic);
    private final ColorSetting customColor = new ColorSetting("Свой цвет", 0xFFBFD8E6);

    private static final double GRAVITY = 22.0;
    private static final double TERMINAL_VELOCITY = 34.0;
    private static final int RIPPLE_SEGMENTS = 14;
    private static final int MAX_DROPS = 4200;
    private static final int MAX_MIST = 90;

    private final List<Drop> drops = new ArrayList<>();
    private final List<Splash> splashList = new ArrayList<>();
    private final List<Droplet> dropletList = new ArrayList<>();
    private final List<Mist> mistList = new ArrayList<>();
    private final Map<Long, Integer> groundCache = new HashMap<>();
    private final BlockPos.Mutable scratchPos = new BlockPos.Mutable();
    private final Random random = new Random();

    private long lastFrameNanos;
    private float clock;
    private float flash;
    private float nextFlashIn = 9.0f;
    private int cacheTicks;

    public Rain() {
        super("Rain", Category.Render, "Реалистичный объёмный дождь с каплями, всплесками и туманом");
        getSettings().add(preset);
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(altitude);
        getSettings().add(dropSize);
        getSettings().add(fallSpeed);
        getSettings().add(wind);
        getSettings().add(opacity);
        getSettings().add(splashes);
        getSettings().add(droplets);
        getSettings().add(groundMist);
        getSettings().add(lightning);
        getSettings().add(skyCheck);
        getSettings().add(onlyWhenRaining);
        getSettings().add(colorMode);
        getSettings().add(customColor);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastFrameNanos = 0L;
        clearAll();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearAll();
    }

    private void clearAll() {
        synchronized (drops) {
            drops.clear();
        }
        splashList.clear();
        dropletList.clear();
        mistList.clear();
        groundCache.clear();
        flash = 0.0f;
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) {
            clearAll();
            return;
        }
        if (++cacheTicks > 30) {
            cacheTicks = 0;
            groundCache.clear();
        }
    }

    private float presetDensity() {
        return switch (preset.getValue()) {
            case Drizzle -> 0.45f;
            case Downpour -> 1.9f;
            case Storm -> 2.8f;
            case Rain -> 1.0f;
        };
    }

    private float presetWind() {
        return switch (preset.getValue()) {
            case Drizzle -> 0.5f;
            case Downpour -> 1.35f;
            case Storm -> 2.4f;
            case Rain -> 1.0f;
        };
    }

    private float presetMass() {
        return switch (preset.getValue()) {
            case Drizzle -> 0.55f;
            case Downpour -> 1.25f;
            case Storm -> 1.5f;
            case Rain -> 1.0f;
        };
    }

    private int targetDropCount() {
        float r = radius.getValue();
        float area = r * r * 0.0155f;
        int count = (int) (area * density.getValue() * presetDensity() * 26.0f);
        return MathHelper.clamp(count, 40, MAX_DROPS);
    }

    private float[] resolveColor() {
        int color;
        if (colorMode.getValue() == ColorMode.Custom) {
            color = customColor.getValue();
        } else if (colorMode.getValue() == ColorMode.Theme) {
            Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
            color = accent.getRGB();
        } else {
            color = 0xFFC8DCE8;
        }
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        return new float[]{r, g, b};
    }

    private float random(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private int surfaceY(int x, int z) {
        long key = (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
        Integer cached = groundCache.get(key);
        if (cached != null) return cached;

        int top;
        try {
            if (mc.world != null) {
                top = mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
            } else {
                top = 64;
            }
        } catch (Throwable ignored) {
            top = mc.world != null ? mc.world.getBottomY() : 0;
        }
        if (groundCache.size() < 20000) groundCache.put(key, top);
        return top;
    }

    private boolean isBlocking(double x, double y, double z) {
        if (mc.world == null) return false;
        scratchPos.set(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        if (!mc.world.getChunkManager().isChunkLoaded(scratchPos.getX() >> 4, scratchPos.getZ() >> 4)) return false;

        BlockState state = mc.world.getBlockState(scratchPos);
        if (!state.getFluidState().isEmpty()) return true;
        if (state.isAir()) return false;
        try {
            return !state.getCollisionShape(mc.world, scratchPos).isEmpty();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private boolean isWater(double x, double y, double z) {
        if (mc.world == null) return false;
        scratchPos.set(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        try {
            return !mc.world.getBlockState(scratchPos).getFluidState().isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean skyVisible(double x, double y, double z) {
        if (!skyCheck.getValue() || mc.world == null) return true;
        scratchPos.set(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        try {
            return mc.world.isSkyVisible(scratchPos);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private void spawnDrop(Vec3d center, boolean fromTop) {
        float r = radius.getValue();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = Math.sqrt(random.nextDouble()) * r;

        double x = center.x + Math.cos(angle) * dist;
        double z = center.z + Math.sin(angle) * dist;
        double y = fromTop
                ? center.y + altitude.getValue() * random(0.75f, 1.0f)
                : center.y + random(-4.0f, altitude.getValue());

        if (!skyVisible(x, y, z)) return;

        Drop drop = new Drop();
        drop.x = x;
        drop.y = y;
        drop.z = z;
        drop.mass = random(0.6f, 1.4f) * presetMass();
        drop.vy = -random(6.0f, 11.0f) * drop.mass;
        drop.phase = random(0.0f, 62.8f);
        drop.brightness = random(0.55f, 1.0f);
        drop.ground = surfaceY(MathHelper.floor(x), MathHelper.floor(z));
        drops.add(drop);
    }

    private void updateDrops(Vec3d center, float dt) {
        int target = targetDropCount();

        drops.removeIf(d -> {
            double dx = d.x - center.x;
            double dz = d.z - center.z;
            float r = radius.getValue() + 6.0f;
            return d.dead || dx * dx + dz * dz > r * r || d.y < center.y - altitude.getValue() - 12.0;
        });

        boolean fill = drops.size() < target / 2;
        int spawnBudget = fill ? Math.min(target - drops.size(), 900) : Math.min(target - drops.size(), 260);
        for (int i = 0; i < spawnBudget; i++) {
            spawnDrop(center, !fill);
        }

        float speedMul = fallSpeed.getValue();
        float windMul = wind.getValue() * presetWind();

        for (int i = 0, size = drops.size(); i < size; i++) {
            Drop d = drops.get(i);

            double gust = 0.65 + 0.35 * Math.sin(clock * 0.55 + d.phase * 0.15)
                    + 0.18 * Math.sin(clock * 1.7 + d.phase);
            double windX = windMul * 3.1 * gust;
            double windZ = windMul * 1.7 * Math.sin(clock * 0.31 + d.phase * 0.05) * gust;

            double drag = 1.0 - Math.min(0.92, Math.abs(d.vy) / (TERMINAL_VELOCITY * d.mass));
            d.vy -= GRAVITY * d.mass * drag * dt;
            if (d.vy < -TERMINAL_VELOCITY * d.mass) d.vy = -TERMINAL_VELOCITY * d.mass;

            d.vx += (windX - d.vx) * Math.min(1.0, dt * 3.2);
            d.vz += (windZ - d.vz) * Math.min(1.0, dt * 3.2);

            double nx = d.x + d.vx * dt * speedMul;
            double ny = d.y + d.vy * dt * speedMul;
            double nz = d.z + d.vz * dt * speedMul;

            d.prevX = d.x;
            d.prevY = d.y;
            d.prevZ = d.z;

            boolean hit = false;
            if (ny <= d.ground + 2.5) {
                if (isBlocking(nx, ny, nz)) {
                    hit = true;
                } else if (ny <= d.ground - 3.0) {
                    d.dead = true;
                }
            }

            if (hit) {
                if (splashes.getValue()) addSplash(nx, ny, nz, d);
                d.dead = true;
                continue;
            }

            d.x = nx;
            d.y = ny;
            d.z = nz;
        }
    }

    private void addSplash(double x, double y, double z, Drop d) {
        if (splashList.size() > 700) return;

        double impactY = Math.floor(y) + 1.0005;
        Splash splash = new Splash();
        splash.x = x;
        splash.y = impactY;
        splash.z = z;
        splash.life = 0.0f;
        splash.maxLife = random(0.30f, 0.52f);
        splash.scale = (float) (0.32 + 0.22 * d.mass);
        splash.water = isWater(x, y - 0.15, z);
        splashList.add(splash);

        if (!droplets.getValue() || dropletList.size() > 1400) return;

        int shards = splash.water ? 4 : 3;
        for (int i = 0; i < shards; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double sp = random(1.1f, 2.6f) * d.mass;
            Droplet drop = new Droplet();
            drop.x = x;
            drop.y = impactY + 0.02;
            drop.z = z;
            drop.vx = Math.cos(a) * sp * 0.45 + d.vx * 0.08;
            drop.vz = Math.sin(a) * sp * 0.45 + d.vz * 0.08;
            drop.vy = random(1.9f, 4.1f);
            drop.maxLife = random(0.28f, 0.5f);
            dropletList.add(drop);
        }
    }

    private void updateDroplets(float dt) {
        for (int i = dropletList.size() - 1; i >= 0; i--) {
            Droplet dr = dropletList.get(i);
            dr.life += dt;
            if (dr.life >= dr.maxLife) {
                dropletList.remove(i);
                continue;
            }
            dr.prevX = dr.x;
            dr.prevY = dr.y;
            dr.prevZ = dr.z;
            dr.vy -= GRAVITY * 0.55 * dt;
            dr.x += dr.vx * dt;
            dr.y += dr.vy * dt;
            dr.z += dr.vz * dt;
        }
    }

    private void updateSplashes(float dt) {
        for (int i = splashList.size() - 1; i >= 0; i--) {
            Splash s = splashList.get(i);
            s.life += dt;
            if (s.life >= s.maxLife) splashList.remove(i);
        }
    }

    private void updateMist(Vec3d center, float dt) {
        if (!groundMist.getValue()) {
            mistList.clear();
            return;
        }

        int target = (int) MathHelper.clamp(22 * density.getValue() * presetDensity(), 6, MAX_MIST);

        mistList.removeIf(m -> {
            m.life += dt;
            double dx = m.x - center.x;
            double dz = m.z - center.z;
            float r = radius.getValue();
            m.x += m.vx * dt;
            m.z += m.vz * dt;
            return m.life > m.maxLife || dx * dx + dz * dz > r * r;
        });

        while (mistList.size() < target) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(random.nextDouble()) * radius.getValue() * 0.85;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            int ground = surfaceY(MathHelper.floor(x), MathHelper.floor(z));

            Mist m = new Mist();
            m.x = x;
            m.z = z;
            m.y = ground + random(0.05f, 0.9f);
            m.size = random(2.2f, 5.6f);
            m.maxLife = random(2.4f, 5.5f);
            m.life = random(0.0f, 1.0f);
            m.vx = random(-0.35f, 0.35f) * wind.getValue();
            m.vz = random(-0.35f, 0.35f) * wind.getValue();
            mistList.add(m);
        }
    }

    private void updateLightning(float dt) {
        if (!lightning.getValue()) {
            flash = 0.0f;
            return;
        }
        flash = Math.max(0.0f, flash - dt * 3.4f);
        nextFlashIn -= dt;
        if (nextFlashIn <= 0.0f) {
            flash = random(0.55f, 1.0f);
            nextFlashIn = random(6.0f, 16.0f);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (!isToggled() || fullNullCheck() || mc.gameRenderer == null) return;
        if (onlyWhenRaining.getValue() && mc.world != null && !mc.world.isRaining()) {
            clearAll();
            return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return;

        long now = System.nanoTime();
        if (lastFrameNanos == 0L) lastFrameNanos = now;
        float dt = (float) ((now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;
        dt = MathHelper.clamp(dt, 0.0f, 0.05f);
        clock += dt;

        Vec3d cam = camera.getPos();

        updateDrops(cam, dt);
        updateSplashes(dt);
        updateDroplets(dt);
        updateMist(cam, dt);
        updateLightning(dt);
        if (drops.isEmpty() && splashList.isEmpty() && dropletList.isEmpty() && mistList.isEmpty()) return;

        float[] rgb = resolveColor();
        float ambient = ambientLight(camera);
        float brightnessMul = MathHelper.clamp(ambient + flash * 0.9f, 0.0f, 1.65f);
        float alphaMul = opacity.getValue() * brightnessMul;

        MatrixStack matrices = event.getMatrices();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        if (groundMist.getValue()) drawMist(matrices, camera, cam, rgb, alphaMul);
        drawStreaks(matrices, camera, cam, rgb, alphaMul, false);
        if (splashes.getValue()) drawRipples(matrices, cam, rgb, alphaMul);

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawStreaks(matrices, camera, cam, rgb, alphaMul, true);
        if (splashes.getValue()) {
            drawCrowns(matrices, camera, cam, rgb, alphaMul);
            if (droplets.getValue()) drawDroplets(matrices, camera, cam, rgb, alphaMul);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawStreaks(MatrixStack ms, Camera camera, Vec3d cam,
                             float[] rgb, float alphaMul, boolean highlight) {
        if (drops.isEmpty()) return;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float sizeMul = dropSize.getValue();
        float r = radius.getValue();
        float fadeStart = r * 0.62f;

        for (int i = 0, size = drops.size(); i < size; i++) {
            Drop d = drops.get(i);

            double dx = d.x - cam.x;
            double dy = d.y - cam.y;
            double dz = d.z - cam.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            double dist = Math.sqrt(distSq);
            if (dist > r + 4.0) continue;

            float distanceFade = dist <= fadeStart ? 1.0f
                    : 1.0f - MathHelper.clamp((float) ((dist - fadeStart) / (r - fadeStart + 1.0f)), 0.0f, 1.0f);
            if (distanceFade <= 0.02f) continue;

            float nearFade = dist < 0.7 ? (float) (dist / 0.7) : 1.0f;

            double speed = Math.sqrt(d.vx * d.vx + d.vy * d.vy + d.vz * d.vz);
            if (speed < 0.0001) continue;

            float length = (float) MathHelper.clamp(speed * 0.055 * sizeMul, 0.16, 2.6);
            float width = (float) (0.014 + 0.019 * d.mass) * sizeMul * (highlight ? 0.45f : 1.0f);

            float alpha = alphaMul * d.brightness * distanceFade * nearFade * (highlight ? 0.30f : 0.42f);
            if (alpha <= 0.006f) continue;
            alpha = Math.min(alpha, 0.95f);

            float yawRad = (float) Math.toRadians(camera.getYaw());
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);
            double horizontal = d.vx * rightX + d.vz * rightZ;
            float tilt = (float) Math.toDegrees(Math.atan2(horizontal, Math.abs(d.vy) + 0.0001));
            tilt = MathHelper.clamp(tilt, -55.0f, 55.0f);

            ms.push();
            ms.translate(dx, dy, dz);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-tilt));

            Matrix4f m = ms.peek().getPositionMatrix();

            float half = width * 0.5f;
            float top = length * 0.5f;
            float bottom = -length * 0.5f;

            float headAlpha = alpha;
            float tailAlpha = 0.0f;
            float headWidth = half;
            float tailWidth = half * 0.35f;

            buffer.vertex(m, -tailWidth, top, 0.0f).color(rgb[0], rgb[1], rgb[2], tailAlpha);
            buffer.vertex(m, tailWidth, top, 0.0f).color(rgb[0], rgb[1], rgb[2], tailAlpha);
            buffer.vertex(m, headWidth, bottom, 0.0f).color(rgb[0], rgb[1], rgb[2], headAlpha);
            buffer.vertex(m, -headWidth, bottom, 0.0f).color(rgb[0], rgb[1], rgb[2], headAlpha);

            if (!highlight) {
                float halo = half * 2.6f;
                float haloAlpha = alpha * 0.22f;
                buffer.vertex(m, -halo * 0.5f, top, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
                buffer.vertex(m, halo * 0.5f, top, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
                buffer.vertex(m, halo, bottom, 0.0f).color(rgb[0], rgb[1], rgb[2], haloAlpha);
                buffer.vertex(m, -halo, bottom, 0.0f).color(rgb[0], rgb[1], rgb[2], haloAlpha);
            } else {
                float lensTop = bottom + length * 0.34f;
                float lensWidth = half * 1.25f;
                buffer.vertex(m, -lensWidth, lensTop, 0.0f).color(1.0f, 1.0f, 1.0f, 0.0f);
                buffer.vertex(m, lensWidth, lensTop, 0.0f).color(1.0f, 1.0f, 1.0f, 0.0f);
                buffer.vertex(m, lensWidth, bottom, 0.0f).color(1.0f, 1.0f, 1.0f, alpha * 0.85f);
                buffer.vertex(m, -lensWidth, bottom, 0.0f).color(1.0f, 1.0f, 1.0f, alpha * 0.85f);
            }

            ms.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawRipples(MatrixStack ms, Vec3d cam, float[] rgb, float alphaMul) {
        if (splashList.isEmpty()) return;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int i = 0, size = splashList.size(); i < size; i++) {
            Splash s = splashList.get(i);
            float progress = MathHelper.clamp(s.life / s.maxLife, 0.0f, 1.0f);
            float eased = 1.0f - (1.0f - progress) * (1.0f - progress);

            float outer = s.scale * (0.18f + eased * (s.water ? 1.35f : 0.85f));
            float inner = outer * (0.55f + 0.35f * eased);
            float alpha = alphaMul * (1.0f - progress) * (s.water ? 0.5f : 0.38f);
            if (alpha <= 0.01f) continue;

            double dx = s.x - cam.x;
            double dy = s.y - cam.y;
            double dz = s.z - cam.z;

            ms.push();
            ms.translate(dx, dy, dz);
            Matrix4f m = ms.peek().getPositionMatrix();

            for (int seg = 0; seg < RIPPLE_SEGMENTS; seg++) {
                double a0 = (Math.PI * 2.0 / RIPPLE_SEGMENTS) * seg;
                double a1 = (Math.PI * 2.0 / RIPPLE_SEGMENTS) * (seg + 1);

                float ix0 = (float) (Math.cos(a0) * inner);
                float iz0 = (float) (Math.sin(a0) * inner);
                float ix1 = (float) (Math.cos(a1) * inner);
                float iz1 = (float) (Math.sin(a1) * inner);
                float ox0 = (float) (Math.cos(a0) * outer);
                float oz0 = (float) (Math.sin(a0) * outer);
                float ox1 = (float) (Math.cos(a1) * outer);
                float oz1 = (float) (Math.sin(a1) * outer);

                buffer.vertex(m, ix0, 0.0f, iz0).color(rgb[0], rgb[1], rgb[2], alpha);
                buffer.vertex(m, ix1, 0.0f, iz1).color(rgb[0], rgb[1], rgb[2], alpha);
                buffer.vertex(m, ox1, 0.0f, oz1).color(rgb[0], rgb[1], rgb[2], 0.0f);
                buffer.vertex(m, ox0, 0.0f, oz0).color(rgb[0], rgb[1], rgb[2], 0.0f);
            }

            ms.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawCrowns(MatrixStack ms, Camera camera, Vec3d cam, float[] rgb, float alphaMul) {
        if (splashList.isEmpty()) return;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int i = 0, size = splashList.size(); i < size; i++) {
            Splash s = splashList.get(i);
            float progress = MathHelper.clamp(s.life / s.maxLife, 0.0f, 1.0f);
            if (progress > 0.55f) continue;

            float local = progress / 0.55f;
            float height = s.scale * (0.55f * (float) Math.sin(local * Math.PI));
            float alpha = alphaMul * (1.0f - local) * 0.32f;
            if (alpha <= 0.01f || height <= 0.005f) continue;

            ms.push();
            ms.translate(s.x - cam.x, s.y - cam.y, s.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            Matrix4f m = ms.peek().getPositionMatrix();

            float base = s.scale * 0.16f;
            buffer.vertex(m, -base * 0.25f, height, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, base * 0.25f, height, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, base, 0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);
            buffer.vertex(m, -base, 0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);

            ms.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawDroplets(MatrixStack ms, Camera camera, Vec3d cam, float[] rgb, float alphaMul) {
        if (dropletList.isEmpty()) return;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float sizeMul = dropSize.getValue();

        for (int i = 0, size = dropletList.size(); i < size; i++) {
            Droplet d = dropletList.get(i);
            float progress = MathHelper.clamp(d.life / d.maxLife, 0.0f, 1.0f);
            float alpha = alphaMul * (1.0f - progress) * 0.5f;
            if (alpha <= 0.01f) continue;

            double speed = Math.sqrt(d.vx * d.vx + d.vy * d.vy + d.vz * d.vz);
            float length = (float) MathHelper.clamp(speed * 0.03 * sizeMul, 0.04, 0.4);
            float width = 0.016f * sizeMul;

            float yawRad = (float) Math.toRadians(camera.getYaw());
            double horizontal = d.vx * Math.cos(yawRad) + d.vz * Math.sin(yawRad);
            float tilt = (float) Math.toDegrees(Math.atan2(horizontal, -d.vy + 0.0001));

            ms.push();
            ms.translate(d.x - cam.x, d.y - cam.y, d.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-tilt));
            Matrix4f m = ms.peek().getPositionMatrix();

            buffer.vertex(m, -width * 0.4f, length * 0.5f, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, width * 0.4f, length * 0.5f, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, width, -length * 0.5f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);
            buffer.vertex(m, -width, -length * 0.5f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);

            ms.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawMist(MatrixStack ms, Camera camera, Vec3d cam, float[] rgb, float alphaMul) {
        if (mistList.isEmpty()) return;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int i = 0, size = mistList.size(); i < size; i++) {
            Mist mist = mistList.get(i);
            float progress = MathHelper.clamp(mist.life / mist.maxLife, 0.0f, 1.0f);
            float fade = (float) Math.sin(progress * Math.PI);
            float alpha = alphaMul * fade * 0.05f;
            if (alpha <= 0.004f) continue;

            ms.push();
            ms.translate(mist.x - cam.x, mist.y - cam.y, mist.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            Matrix4f m = ms.peek().getPositionMatrix();

            float half = mist.size * 0.5f;
            float top = mist.size * 0.42f;

            buffer.vertex(m, -half, top, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, half, top, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, half, 0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);
            buffer.vertex(m, -half, 0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);

            ms.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private float ambientLight(Camera camera) {
        try {
            if (mc.world != null) {
                int light = mc.world.getLightLevel(camera.getBlockPos());
                return MathHelper.clamp(light / 15.0f, 0.3f, 1.0f);
            }
        } catch (Throwable ignored) {}
        return 1.0f;
    }

    private static final class Drop {
        double x, y, z;
        double prevX, prevY, prevZ;
        double vx, vy, vz;
        double mass = 1.0;
        double ground;
        float phase;
        float brightness = 1.0f;
        boolean dead;
    }

    private static final class Droplet {
        double x, y, z;
        double prevX, prevY, prevZ;
        double vx, vy, vz;
        float life;
        float maxLife = 0.4f;
    }

    private static final class Splash {
        double x, y, z;
        float life;
        float maxLife = 0.4f;
        float scale = 0.4f;
        boolean water;
    }

    private static final class Mist {
        double x, y, z;
        double vx, vz;
        float size = 3.0f;
        float life;
        float maxLife = 4.0f;
    }
}
