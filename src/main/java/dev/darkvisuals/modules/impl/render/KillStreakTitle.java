package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.events.impl.EventPacket;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KillStreakTitle extends Module {

    private static final long KILL_WINDOW_MS   = 6_000L;
    private static final long ATTR_WINDOW_MS   = 4_000L;
    private static final long DISPLAY_MS       = 2_400L;

    private static final String[] TITLES = {
            "First Blood",
            "Double Kill",
            "Triple Kill",
            "Killing Spree",
            "Rampage",
            "Unstoppable!",
            "GODLIKE!!!"
    };

    private final NumberSetting resetTime = new NumberSetting("Сброс (сек)", 6f, 2f, 15f, 0.5f);
    private final BooleanSetting shakeScreen = new BooleanSetting("Тряска экрана", true);
    private final BooleanSetting rainbowTitle = new BooleanSetting("Радуга", false);
    private final NumberSetting textSize = new NumberSetting("Размер текста", 12f, 6f, 22f, 1f);

    private final Map<Integer, Long> recentTargets = new ConcurrentHashMap<>();

    private int killStreak   = 0;
    private long lastKillMs  = 0L;
    private long showSince   = -99_999L;
    private String shownTitle = "";

    public KillStreakTitle() {
        super("KillStreakTitle", Category.Render, "Надпись на экране при килл-стрике (как в шутерах)");
        getSettings().add(resetTime);
        getSettings().add(textSize);
        getSettings().add(shakeScreen);
        getSettings().add(rainbowTitle);
    }

    @Override
    public void onDisable() {
        recentTargets.clear();
        killStreak = 0;
        lastKillMs = 0;
        super.onDisable();
    }

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (fullNullCheck() || e.getPlayer() != mc.player) return;
        if (!(e.getTarget() instanceof LivingEntity)) return;
        if (!e.canProcess()) return;
        e.registerHit();
        recentTargets.put(e.getTarget().getId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPacket(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        if (!(e.getPacket() instanceof EntityStatusS2CPacket pkt)) return;
        if (pkt.getStatus() != 3) return;

        mc.execute(() -> {
            if (fullNullCheck()) return;
            Entity entity = pkt.getEntity(mc.world);
            if (entity == null) return;
            Long hitTime = recentTargets.remove(entity.getId());
            if (hitTime == null) return;
            if (System.currentTimeMillis() - hitTime > ATTR_WINDOW_MS) return;

            long now = System.currentTimeMillis();
            long reset = (long)(resetTime.getValue() * 1000);
            if (lastKillMs > 0 && (now - lastKillMs) > reset) killStreak = 0;

            killStreak++;
            lastKillMs = now;

            int idx = Math.min(killStreak - 1, TITLES.length - 1);
            shownTitle = TITLES[idx];
            showSince  = now;
        });
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;

        long age = System.currentTimeMillis() - showSince;
        if (age < 0 || age > DISPLAY_MS) return;

        float t        = age / (float) DISPLAY_MS;
        float fadeIn   = MathHelper.clamp(age / 200f, 0f, 1f);
        float fadeOut  = MathHelper.clamp((DISPLAY_MS - age) / 400f, 0f, 1f);
        float alpha    = fadeIn * fadeOut;

        float slideY   = (1f - MathHelper.clamp(age / 250f, 0f, 1f)) * -20f;

        float scale    = 0.8f + 0.2f * MathHelper.clamp(age / 200f, 0f, 1f);

        int cx = e.getContext().getScaledWindowWidth()  / 2;
        int cy = e.getContext().getScaledWindowHeight() / 2;

        float sz   = textSize.getValue() * scale;
        Color base = rainbowTitle.getValue()
                ? new Color(Color.HSBtoRGB((System.currentTimeMillis() / 2000f) % 1f, 0.75f, 1f))
                : ThemeManager.getInstance().getThemeColor();

        Color col = new Color(base.getRed(), base.getGreen(), base.getBlue(), (int)(255 * alpha));

        Color shadow = new Color(0, 0, 0, (int)(180 * alpha));

        net.minecraft.client.util.math.MatrixStack ms = e.getContext().getMatrices();

        float yBase = cy * 0.55f + slideY;

        float shakeX = 0f, shakeY = 0f;
        if (shakeScreen.getValue() && killStreak >= 3 && age < 300) {
            float shk = (1f - age / 300f) * (killStreak >= 5 ? 4f : 2f);
            shakeX = (float)(Math.random() * 2 - 1) * shk;
            shakeY = (float)(Math.random() * 2 - 1) * shk;
        }

        String text    = shownTitle;
        float textW    = text.length() * sz * 2.1f;
        float tx       = cx - textW / 2f + shakeX;
        float ty       = yBase - sz + shakeY;

        String sub     = killStreak + " Kill" + (killStreak > 1 ? "s" : "");
        Color subCol   = new Color(255, 255, 255, (int)(180 * alpha * 0.8f));
        float subSz    = sz * 0.55f;
        float subW     = sub.length() * subSz * 2.1f;

        Render2D.drawFont(ms, Fonts.BOLD.getFont(sz), text, tx + 0.8f, ty + 0.8f, shadow);
        Render2D.drawFont(ms, Fonts.BOLD.getFont(sz), text, tx, ty, col);
        Render2D.drawFont(ms, Fonts.REGULAR.getFont(subSz), sub, cx - subW / 2f + shakeX, ty + sz * 2.3f + shakeY, subCol);
    }
}
