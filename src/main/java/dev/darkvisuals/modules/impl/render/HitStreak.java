package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.world.WorldUtils;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

 public class HitStreak extends Module {

    private final NumberSetting resetSeconds = new NumberSetting("setting.resetSeconds", 3f, 1f, 10f, 0.5f);
    private final NumberSetting lifeTime = new NumberSetting("setting.lifeTime", 1.2f, 0.4f, 3f, 0.1f);
    private final NumberSetting textSize = new NumberSetting("setting.textSize", 4f, 2f, 8f, 0.5f);
    private final BooleanSetting showStreak = new BooleanSetting("setting.showStreak", true);
    private final BooleanSetting showHits = new BooleanSetting("setting.showHits", true);

    private final List<StreakParticle> particles = new CopyOnWriteArrayList<>();

    private int streakCount = 0;
    private long lastHitTime = 0L;

    public HitStreak() {
        super("HitStreak", Category.Render, "Показывает счётчик ударов с плавающим текстом");
        getSettings().add(resetSeconds);
        getSettings().add(lifeTime);
        getSettings().add(textSize);
        getSettings().add(showStreak);
        getSettings().add(showHits);
    }

    @Override
    public void onDisable() {
        particles.clear();
        streakCount = 0;
        super.onDisable();
    }

    @EventHandler
    private void onAttackEntity(EventAttackEntity e) {
        if (fullNullCheck()) return;
        if (!(e.getTarget() instanceof LivingEntity entity)) return;
        if (!entity.isAlive()) return;
        if (!e.isEffectsAllowed()) return;
        if (!e.canProcess()) return;

        long now = System.currentTimeMillis();
        long resetMs = (long) (resetSeconds.getValue() * 1000);

         
        if (lastHitTime != 0 && (now - lastHitTime) > resetMs) {
            streakCount = 0;
        }
        lastHitTime = now;
        streakCount++;

         
        Vec3d hitPos = entity.getPos().add(0, entity.getHeight() / 2f + 0.2f, 0);

         
        StringBuilder sb = new StringBuilder();
        if (showHits.getValue()) sb.append("+1");
        if (showStreak.getValue() && streakCount > 1) {
            if (sb.length() > 0) sb.append("  ");
            sb.append("Streak x").append(streakCount);
        }
        if (sb.length() == 0) sb.append("Hit");

        particles.add(new StreakParticle(hitPos, sb.toString(),
                (long) (lifeTime.getValue() * 1000), streakCount));
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();
        particles.removeIf(p -> (now - p.spawnTime) > p.ttl);

        if (particles.isEmpty()) return;

        Color themeColor = ThemeManager.getInstance().getThemeColor();

        for (StreakParticle p : particles) {
            long age = now - p.spawnTime;
            float progress = Math.min(1f, age / (float) p.ttl);

             
            float fadeIn = Math.min(1f, age / 150f);
            float fadeOut = 1f - progress;
            float alpha = Math.max(0f, Math.min(1f, fadeIn * fadeOut));

             
            double rise = progress * 1.2f;
            Vec3d screenPos = WorldUtils.getPosition(p.pos.add(0, rise, 0));

             
            if (screenPos.z >= 1f) continue;

            int a = (int) (255 * alpha);
            if (a <= 2) continue;

            Color drawColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), a);

             
            float size = textSize.getValue() * (1f - progress * 0.2f);

            String text = p.text;
            int textWidth = (int) (text.length() * size * 2.2f);  
            float x = (float) screenPos.x - textWidth / 2f;
            float y = (float) screenPos.y - size;

            Render2D.drawFont(event.getContext().getMatrices(),
                    Fonts.BOLD.getFont(size), text, x, y, drawColor);
        }
    }

    private static class StreakParticle {
        final Vec3d pos;
        final String text;
        final long ttl;
        final long spawnTime;

        StreakParticle(Vec3d pos, String text, long ttl, int streak) {
            this.pos = pos;
            this.text = text;
            this.ttl = ttl;
            this.spawnTime = System.currentTimeMillis();
        }
    }
}
