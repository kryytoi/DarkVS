package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.managers.FriendsManager;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CubeTrails extends Module {

    private final NumberSetting size = new NumberSetting("Размер кубов", 0.45f, 0.2f, 1.5f, 0.05f);
    private final NumberSetting maxSize = new NumberSetting("Максимальный размер", 2.0f, 0.5f, 5.0f, 0.1f);
    private final NumberSetting spacing = new NumberSetting("Расстояние", 0.5f, 0.1f, 2.0f, 0.05f);
    private final NumberSetting lifeTime = new NumberSetting("Время Жизни", 4.0f, 0.5f, 12.0f, 0.5f);
    private final NumberSetting mergeDistance = new NumberSetting("Расстояние слияния", 0.9f, 0.3f, 1.5f, 0.05f);
    private final NumberSetting opacity = new NumberSetting("Непрозрачность", 0.4f, 0.05f, 1.0f, 0.05f);
    private final BooleanSetting outline = new BooleanSetting("Обводка", true);
    private final BooleanSetting showFriends = new BooleanSetting("setting.showFriends", true);

    private static final int MAX_BLOBS = 60;

    private final List<Blob> blobs = new ArrayList<>();
    private final Map<UUID, Vec3d> lastSpawn = new HashMap<>();
    private final ThemeManager themeManager = ThemeManager.getInstance();

    public CubeTrails() {
        super("CubeTrails", Category.Render, "Спавнит кубы за вами при ходьбе");
        getSettings().add(size);
        getSettings().add(maxSize);
        getSettings().add(spacing);
        getSettings().add(lifeTime);
        getSettings().add(mergeDistance);
        getSettings().add(opacity);
        getSettings().add(outline);
        getSettings().add(showFriends);
    }

    @Override
    public void onDisable() {
        synchronized (blobs) {
            blobs.clear();
        }
        lastSpawn.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;
        long now = System.currentTimeMillis();
        float baseR = size.getValue();
        long ttl = (long) (lifeTime.getValue() * 1000L);
        double minGap = spacing.getValue();
        double minGapSq = minGap * minGap;

        synchronized (blobs) {
            blobs.removeIf(b -> (now - b.spawnTime) > b.ttl);

            for (PlayerEntity p : mc.world.getPlayers()) {
                boolean self = p == mc.player;
                if (!self) {
                    if (!showFriends.getValue() || !FriendsManager.checkFriend(p.getGameProfile().getName())) continue;
                }

                Vec3d feet = new Vec3d(p.getX(), p.getY() + 0.02, p.getZ());
                UUID id = p.getUuid();
                Vec3d prev = lastSpawn.get(id);
                double dx = prev == null ? Double.MAX_VALUE : feet.x - prev.x;
                double dz = prev == null ? Double.MAX_VALUE : feet.z - prev.z;
                if (prev == null || (dx * dx + dz * dz) >= minGapSq) {
                    blobs.add(new Blob(feet, baseR, ttl, now));
                    lastSpawn.put(id, feet);
                }
            }

            mergePass();

            while (blobs.size() > MAX_BLOBS) {
                int oldest = 0;
                for (int i = 1; i < blobs.size(); i++) {
                    if (blobs.get(i).spawnTime < blobs.get(oldest).spawnTime) oldest = i;
                }
                blobs.remove(oldest);
            }
        }
    }

    private void mergePass() {
        float cap = maxSize.getValue();
        float factor = mergeDistance.getValue();
        boolean merged = true;
        int guard = 0;
        while (merged && guard++ < 128) {
            merged = false;
            outer:
            for (int i = 0; i < blobs.size(); i++) {
                for (int j = i + 1; j < blobs.size(); j++) {
                    Blob a = blobs.get(i);
                    Blob b = blobs.get(j);
                    double dx = a.pos.x - b.pos.x;
                    double dz = a.pos.z - b.pos.z;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < (a.radius + b.radius) * factor) {
                        blobs.set(i, mergeBlobs(a, b, cap));
                        blobs.remove(j);
                        merged = true;
                        break outer;
                    }
                }
            }
        }
    }

    private Blob mergeBlobs(Blob a, Blob b, float cap) {
        float r = Math.min(cap, (float) Math.sqrt(a.radius * a.radius + b.radius * b.radius));
        double wa = a.radius * a.radius;
        double wb = b.radius * b.radius;
        double sum = wa + wb;
        double x = (a.pos.x * wa + b.pos.x * wb) / sum;
        double y = (a.pos.y * wa + b.pos.y * wb) / sum;
        double z = (a.pos.z * wa + b.pos.z * wb) / sum;
        long spawn = (long) ((a.spawnTime * wa + b.spawnTime * wb) / sum);
        return new Blob(new Vec3d(x, y, z), r, Math.max(a.ttl, b.ttl), spawn);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        try (var __ = Perf.scopeCpu("CubeTrails.onRender3D")) {
            long now = System.currentTimeMillis();

            List<Blob> snapshot;
            synchronized (blobs) {
                snapshot = new ArrayList<>(blobs);
            }
            if (snapshot.isEmpty()) return;

            // полупрозрачный цвет фона темы с примесью акцентного — кубы читаются и не спорят с темой
            Color bg = themeManager.getCurrentTheme().getBackgroundColor();
            Color ac = themeManager.getCurrentTheme().getAccentColor();
            int tr = (bg.getRed() + ac.getRed()) / 2;
            int tg = (bg.getGreen() + ac.getGreen()) / 2;
            int tb = (bg.getBlue() + ac.getBlue()) / 2;
            float op = opacity.getValue();
            boolean drawOutline = outline.getValue();

            for (Blob c : snapshot) {
                float t = Math.max(0f, Math.min(1f, (now - c.spawnTime) / (float) c.ttl));
                float alphaK;
                if (t < 0.15f) alphaK = t / 0.15f;
                else alphaK = (float) Math.pow(1f - (t - 0.15f) / 0.85f, 0.8f);
                alphaK = Math.max(0f, Math.min(1f, alphaK));

                int fillA = clampAlpha(op * 255f * alphaK);
                int lineA = clampAlpha(Math.min(1f, op * 1.6f + 0.25f) * 255f * alphaK);
                if (fillA <= 2 && !(drawOutline && lineA > 2)) continue;

                float grow = t < 0.2f ? (0.6f + 0.4f * (t / 0.2f)) : 1f;
                double r = c.radius * grow;
                Vec3d center = new Vec3d(c.pos.x, c.pos.y + r, c.pos.z);

                Color fill = new Color(tr, tg, tb, fillA);
                Color line = new Color(Math.min(255, tr + 40), Math.min(255, tg + 40), Math.min(255, tb + 40), lineA);
                Render3D.renderCube(e.getMatrices(), center, r * 2.0, true, fill, drawOutline, line);
            }
        }
    }

    private static int clampAlpha(float v) {
        int i = (int) v;
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    private static class Blob {
        final Vec3d pos;
        final float radius;
        final long ttl;
        final long spawnTime;

        Blob(Vec3d pos, float radius, long ttl, long spawnTime) {
            this.pos = pos;
            this.radius = radius;
            this.ttl = ttl;
            this.spawnTime = spawnTime;
        }
    }
}
