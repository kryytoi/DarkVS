package dev.darkvisuals.modules.impl.render.lyrics;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.media.NowPlayingBridge;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class KineticLyrics extends Module {

    private final ListSetting renderMode = new ListSetting("Режим", true,
            new BooleanSetting("Оба", true),
            new BooleanSetting("3D Мир", false),
            new BooleanSetting("Экран (HUD)", false));

    private final ListSetting animation = new ListSetting("Анимация", true,
            new BooleanSetting("LyricFlow", true),
            new BooleanSetting("Typewriter", false),
            new BooleanSetting("PopScale", false),
            new BooleanSetting("KineticSlide", false),
            new BooleanSetting("BeatPulse", false),
            new BooleanSetting("Fade", false));

    private final ListSetting colorMode = new ListSetting("Цвет", true,
            new BooleanSetting("Тема", true),
            new BooleanSetting("Градиент", false),
            new BooleanSetting("Белый", false));

    private final ListSetting fontMode = new ListSetting("Шрифт", true,
            new BooleanSetting("Bold", true),
            new BooleanSetting("Semibold", false),
            new BooleanSetting("Medium", false));

    private final ListSetting splitMode = new ListSetting("Разбиение", true,
            new BooleanSetting("SmartSplit", true),
            new BooleanSetting("FullLine", false));

    private final NumberSetting maxLines = new NumberSetting("Макс. строк", 3.0f, 1.0f, 6.0f, 1.0f);
    private final NumberSetting minDistance = new NumberSetting("Мин. дистанция", 1.8f, 1.0f, 3.5f, 0.1f);
    private final NumberSetting maxDistance = new NumberSetting("Макс. дистанция", 3.5f, 1.8f, 6.0f, 0.1f);
    private final NumberSetting arcSpread = new NumberSetting("Разброс", 45.0f, 15.0f, 75.0f, 5.0f);
    private final NumberSetting floatHeight = new NumberSetting("Высота", 0.4f, 0.1f, 1.5f, 0.1f);
    private final NumberSetting timeOffset = new NumberSetting("Смещение (мс)", 0.0f, -5000.0f, 5000.0f, 50.0f);
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", true);
    private final BooleanSetting debugMode = new BooleanSetting("Debug", false);

    private final List<LyricLine> rawLyrics = new CopyOnWriteArrayList<>();
    private final List<LyricLine> lyricsQueue = new CopyOnWriteArrayList<>();
    private final List<LyricParticle3D> activeParticles = new CopyOnWriteArrayList<>();
    private int currentLyricIndex = 0;

    private long internalAudioClockMs = 0L;
    private long lastUpdateRealTimeMs = 0L;
    private long lastWindowsReportedPosMs = 0L;
    private long lastEffectiveAudioTimeMs = 0L;
    private long trackStartTimeSys = 0L;
    private long lastMediaSeenMs = 0L;
    private boolean isPlaying = false;

    private String lastTrackKey = "";
    private String currentTrackTitle = "";
    private String currentTrackArtist = "";
    private long currentTrackDurationSec = -1L;

    private float subtitleAlpha = 0f;
    private String lastActiveSubtitleText = "";

    private static final long RETRY_DELAY_MIN_MS = 4_000L;
    private static final long RETRY_DELAY_MAX_MS = 15_000L;

    private String fetchStatus = "нет данных";
    private long lastFetchAttemptMs = 0L;
    private int fetchAttempts = 0;
    private long fetchRetryDelayMs = RETRY_DELAY_MIN_MS;

    public KineticLyrics() {
        super("KineticLyrics", Category.Render, "Рендерит слова трека в 3D-пространстве и на экране как субтитры");
        getSettings().add(renderMode);
        getSettings().add(animation);
        getSettings().add(colorMode);
        getSettings().add(fontMode);
        getSettings().add(splitMode);
        getSettings().add(maxLines);
        getSettings().add(minDistance);
        getSettings().add(maxDistance);
        getSettings().add(arcSpread);
        getSettings().add(floatHeight);
        getSettings().add(timeOffset);
        getSettings().add(throughWalls);
        getSettings().add(debugMode);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        new java.io.File("darkvisuals/lyrics").mkdirs();
        resetPlaybackState();
    }

    @Override
    public void onDisable() {
        resetPlaybackState();
        super.onDisable();
    }

    private void resetPlaybackState() {
        activeParticles.clear();
        rawLyrics.clear();
        lyricsQueue.clear();
        lastTrackKey = "";
        currentTrackTitle = "";
        currentTrackArtist = "";
        currentTrackDurationSec = -1L;
        currentLyricIndex = 0;
        internalAudioClockMs = 0L;
        lastEffectiveAudioTimeMs = 0L;
        lastUpdateRealTimeMs = System.currentTimeMillis();
        trackStartTimeSys = System.currentTimeMillis();
        lastWindowsReportedPosMs = 0L;
        isPlaying = false;
        fetchAttempts = 0;
        fetchRetryDelayMs = RETRY_DELAY_MIN_MS;
        fetchStatus = "нет данных";
    }

    public void playTrack(String trackName, String artistName, long durationSec) {
        this.currentTrackTitle = trackName != null ? trackName.trim() : "";
        this.currentTrackArtist = artistName != null ? artistName.trim() : "";
        this.currentTrackDurationSec = durationSec;
        this.lastTrackKey = (currentTrackTitle + " - " + currentTrackArtist).toLowerCase();

        activeParticles.clear();
        rawLyrics.clear();
        lyricsQueue.clear();
        currentLyricIndex = 0;
        internalAudioClockMs = 0L;
        lastEffectiveAudioTimeMs = 0L;
        trackStartTimeSys = System.currentTimeMillis();
        lastUpdateRealTimeMs = System.currentTimeMillis();
        lastWindowsReportedPosMs = 0L;
        isPlaying = false;
        fetchAttempts = 0;
        fetchRetryDelayMs = RETRY_DELAY_MIN_MS;
        fetchStatus = "нет данных";

        String expectedKey = this.lastTrackKey;
        fetchLyrics(currentTrackTitle, currentTrackArtist, expectedKey);
    }

    private void fetchLyrics(String title, String artist, String expectedKey) {
        lastFetchAttemptMs = System.currentTimeMillis();
        fetchAttempts++;
        fetchStatus = "запрос текста (попытка " + fetchAttempts + ")...";

        LrcLibClient.fetchLyricsAsync(title, artist, currentTrackDurationSec, debugMode.getValue()).thenAccept(lines -> {
            if (!this.lastTrackKey.equalsIgnoreCase(expectedKey)) {
                return;
            }

            mc.execute(() -> {
                if (!this.lastTrackKey.equalsIgnoreCase(expectedKey)) {
                    return;
                }

                if (lines == null || lines.isEmpty()) {
                    String err = LrcLibClient.getLastResult();
                    fetchStatus = "текст не найден" + (err != null && !err.isEmpty() ? " (" + err + ")" : "");

                    fetchRetryDelayMs = Math.min(RETRY_DELAY_MAX_MS, fetchRetryDelayMs * 2);
                    return;
                }

                activeParticles.clear();
                rawLyrics.clear();
                lyricsQueue.clear();
                rawLyrics.addAll(lines);
                rebuildLyricsQueue();
                currentLyricIndex = 0;
                isPlaying = !lyricsQueue.isEmpty();
                fetchRetryDelayMs = RETRY_DELAY_MIN_MS;
                fetchStatus = "загружено строк: " + lyricsQueue.size();

                resyncQueue(Math.max(0, internalAudioClockMs + (long) (float) timeOffset.getValue()));
            });
        });
    }

    private void rebuildLyricsQueue() {
        lyricsQueue.clear();
        if (isSmartSplit()) {
            lyricsQueue.addAll(LyricLineSplitter.splitLongLines(rawLyrics));
        } else {
            lyricsQueue.addAll(rawLyrics);
        }
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;
        syncPlaybackTime();
    }

    public void syncPlaybackTime() {
        if (!isToggled() || mc.player == null) return;

        NowPlayingBridge.ensureStarted();

        long now = System.currentTimeMillis();
        long dt = (lastUpdateRealTimeMs > 0) ? (now - lastUpdateRealTimeMs) : 0;
        lastUpdateRealTimeMs = now;

        NowPlayingBridge.Snapshot media = NowPlayingBridge.getCurrent();
        if (media != null && media.title != null && !media.title.isBlank()) {
            lastMediaSeenMs = now;
            String title = media.title.trim();
            String artist = media.artist != null ? media.artist.trim() : "";
            String key = (title + " - " + artist).toLowerCase();

            if (!key.equalsIgnoreCase(lastTrackKey)) {
                playTrack(title, artist, media.durationSec);
                return;
            }

            if (currentTrackDurationSec <= 0 && media.durationSec > 0) {
                currentTrackDurationSec = media.durationSec;
            }

            if (!isPlaying && !lyricsQueue.isEmpty()) {
                isPlaying = true;
            }

            if (lyricsQueue.isEmpty() && now - lastFetchAttemptMs > fetchRetryDelayMs) {
                fetchLyrics(currentTrackTitle, currentTrackArtist, key);
            }

            long mediaPosMs = media.smoothedPositionSec() * 1000L;
            if (now - trackStartTimeSys > 1500L && mediaPosMs > 0 && Math.abs(mediaPosMs - lastWindowsReportedPosMs) > 1500L) {
                lastWindowsReportedPosMs = mediaPosMs;
                internalAudioClockMs = mediaPosMs;
            }

            internalAudioClockMs += dt;

            long effectiveAudioTime = Math.max(0, internalAudioClockMs + (long) (float) timeOffset.getValue());

            if (Math.abs(effectiveAudioTime - lastEffectiveAudioTimeMs) > 1500L) {
                resyncQueue(effectiveAudioTime);
            }
            lastEffectiveAudioTimeMs = effectiveAudioTime;

            update(effectiveAudioTime);
        } else {

            if (now - lastMediaSeenMs > 4000L) {
                if (isPlaying) {
                    isPlaying = false;
                    activeParticles.clear();
                }
            } else if (isPlaying) {
                internalAudioClockMs += dt;
                long effectiveAudioTime = Math.max(0, internalAudioClockMs + (long) (float) timeOffset.getValue());
                update(effectiveAudioTime);
            }
        }
    }

    private void resyncQueue(long audioTimeMs) {
        activeParticles.clear();
        currentLyricIndex = 0;
        long now = System.currentTimeMillis();

        for (int i = 0; i < lyricsQueue.size(); i++) {
            LyricLine line = lyricsQueue.get(i);
            long nextStartMs = (i + 1 < lyricsQueue.size()) ? lyricsQueue.get(i + 1).timestampMs() : line.timestampMs() + 3500L;
            long durationMs = Math.max(1200L, Math.min(5000L, nextStartMs - line.timestampMs()));

            if (audioTimeMs >= line.timestampMs()) {
                long timeSinceLine = audioTimeMs - line.timestampMs();
                if (timeSinceLine < durationMs) {
                    spawnLyricParticle(line.text(), now - timeSinceLine, durationMs);
                }
                currentLyricIndex = i + 1;
            } else {
                break;
            }
        }
    }

    public void update(long audioTimeMs) {
        if (!isToggled() || !isPlaying || lyricsQueue.isEmpty() || mc.player == null) return;

        long now = System.currentTimeMillis();

        while (currentLyricIndex < lyricsQueue.size()) {
            LyricLine line = lyricsQueue.get(currentLyricIndex);
            long nextStartMs = (currentLyricIndex + 1 < lyricsQueue.size()) ? lyricsQueue.get(currentLyricIndex + 1).timestampMs() : line.timestampMs() + 3500L;
            long durationMs = Math.max(1400L, Math.min(5500L, nextStartMs - line.timestampMs()));

            if (audioTimeMs >= line.timestampMs()) {
                long timeSinceLine = audioTimeMs - line.timestampMs();
                if (timeSinceLine < durationMs) {
                    spawnLyricParticle(line.text(), now - timeSinceLine, durationMs);
                }
                currentLyricIndex++;
            } else {
                break;
            }
        }

        activeParticles.removeIf(particle -> particle.isDead(now));
    }

    private Vec3d findNonOverlappingSpawnPos(Vec3d headPos, float cameraYaw) {
        double spread = arcSpread.getValue();
        double minD = minDistance.getValue();
        double maxD = Math.max(minD + 0.5, maxDistance.getValue());

        Vec3d bestPos = null;
        double maxMinDistance = -1.0;

        for (int attempt = 0; attempt < 16; attempt++) {
            double randomAngleOffset = ThreadLocalRandom.current().nextDouble(-spread, spread);
            double targetYawDeg = cameraYaw + randomAngleOffset;
            double distance = ThreadLocalRandom.current().nextDouble(minD, maxD);
            double yOffset = ThreadLocalRandom.current().nextDouble(-0.25, 0.55);

            double yawRad = Math.toRadians(targetYawDeg);
            double dirX = -Math.sin(yawRad);
            double dirZ = Math.cos(yawRad);

            double spawnX = headPos.x + (dirX * distance);
            double spawnY = headPos.y + yOffset;
            double spawnZ = headPos.z + (dirZ * distance);
            Vec3d candidate = new Vec3d(spawnX, spawnY, spawnZ);

            if (activeParticles.isEmpty()) {
                return candidate;
            }

            double minDistanceToOthers = Double.MAX_VALUE;
            for (LyricParticle3D active : activeParticles) {
                double d = candidate.distanceTo(active.getBasePosition());
                if (d < minDistanceToOthers) {
                    minDistanceToOthers = d;
                }
            }

            if (minDistanceToOthers >= 1.35) {
                return candidate;
            }

            if (minDistanceToOthers > maxMinDistance) {
                maxMinDistance = minDistanceToOthers;
                bestPos = candidate;
            }
        }

        return bestPos;
    }

    private void spawnLyricParticle(String text, long spawnTimeMs, long durationMs) {
        if (mc.player == null || text == null || text.isBlank()) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d headPos = mc.player.getEyePos();
        float cameraYaw = camera.getYaw();

        int maxAllowed = (int) (float) maxLines.getValue();
        while (activeParticles.size() >= maxAllowed) {
            activeParticles.remove(0);
        }

        Vec3d spawnPos = findNonOverlappingSpawnPos(headPos, cameraYaw);
        if (spawnPos == null) return;

        float riseHeight = floatHeight.getValue();
        activeParticles.add(new LyricParticle3D(text, spawnPos, spawnTimeMs, durationMs, riseHeight));
    }

    private dev.darkvisuals.client.render.msdf.MsdfFont getSelectedFont() {
        if (isFont("Semibold")) return Fonts.SEMIBOLD.font();
        if (isFont("Medium")) return Fonts.MEDIUM.font();
        return Fonts.BOLD.font();
    }

    private int getActiveColorRgb() {
        if (isColor("Тема")) {
            ThemeManager tm = ThemeManager.getInstance();
            if (tm != null && tm.getCurrentTheme() != null) {
                return tm.getCurrentTheme().getAccentColor().getRGB() & 0x00FFFFFF;
            }
            return 0xFFFFFF;
        }
        if (isColor("Градиент")) {
            ThemeManager tm = ThemeManager.getInstance();
            if (tm != null && tm.getCurrentTheme() != null) {
                Color c1 = tm.getCurrentTheme().getAccentColor();
                Color c2 = tm.getCurrentTheme().getSecondaryBackgroundColor();
                float ratio = (float) (Math.sin(System.currentTimeMillis() / 450.0) * 0.5 + 0.5);
                int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
                int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
                int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
                return (r << 16) | (g << 8) | b;
            }
            return 0xFFFFFF;
        }
        return 0xFFFFFF;
    }

    private boolean isAnimation(String name) {
        BooleanSetting s = animation.getName(name);
        return s != null && s.getValue();
    }

    private boolean isColor(String name) {
        BooleanSetting s = colorMode.getName(name);
        return s != null && s.getValue();
    }

    private boolean isFont(String name) {
        BooleanSetting s = fontMode.getName(name);
        return s != null && s.getValue();
    }

    private boolean isSmartSplit() {
        BooleanSetting s = splitMode.getName("SmartSplit");
        return s == null || s.getValue();
    }

    private String currentAnimation() {
        for (String name : new String[]{"Typewriter", "PopScale", "KineticSlide", "BeatPulse", "Fade"}) {
            if (isAnimation(name)) return name;
        }
        return "LyricFlow";
    }

    private boolean isMode(String name) {
        BooleanSetting s = renderMode.getName(name);
        return s != null && s.getValue();
    }

    public String getActiveLyricText(long audioTimeMs) {
        if (lyricsQueue.isEmpty()) return null;
        for (int i = 0; i < lyricsQueue.size(); i++) {
            LyricLine line = lyricsQueue.get(i);
            long nextStart = (i + 1 < lyricsQueue.size()) ? lyricsQueue.get(i + 1).timestampMs() : line.timestampMs() + 4500L;
            if (audioTimeMs >= line.timestampMs() && audioTimeMs < nextStart) {
                return line.text();
            }
        }
        return null;
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        if (!dev.darkvisuals.client.util.renderer.fonts.Fonts.isLoaded()) return;

        syncPlaybackTime();

        if (!isMode("Оба") && !isMode("3D Мир")) return;
        if (activeParticles.isEmpty()) return;

        Camera camera = mc.gameRenderer.getCamera();
        var font = getSelectedFont();
        String animMode = currentAnimation();
        int colorRgb = getActiveColorRgb();
        long now = System.currentTimeMillis();
        boolean ignoreDepth = throughWalls.getValue();

        for (LyricParticle3D particle : activeParticles) {
            particle.render(event.getMatrices(), camera, font, animMode, colorRgb, now, ignoreDepth);
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck()) return;
        if (!dev.darkvisuals.client.util.renderer.fonts.Fonts.isLoaded()) return;

        long displayTimeMs = Math.max(0, internalAudioClockMs + (long) (float) timeOffset.getValue());

        if (isMode("Оба") || isMode("Экран (HUD)")) {
            String activeLine = getActiveLyricText(displayTimeMs);
            if (activeLine != null && !activeLine.isBlank()) {
                lastActiveSubtitleText = activeLine;
                subtitleAlpha = Math.min(1.0f, subtitleAlpha + 0.12f);
            } else {
                subtitleAlpha = Math.max(0.0f, subtitleAlpha - 0.08f);
            }

            if (subtitleAlpha > 0.01f && lastActiveSubtitleText != null && !lastActiveSubtitleText.isBlank()) {
                renderScreenSubtitle(event.getContext().getMatrices(), lastActiveSubtitleText, subtitleAlpha);
            }
        }

        if (!debugMode.getValue()) return;

        String titleStr = "Track: " + (currentTrackTitle.isEmpty() ? "None" : currentTrackTitle + " - " + currentTrackArtist);
        String timerInfo = String.format("Audio Time: %02d:%02d.%03d (%d ms)",
                (displayTimeMs / 60_000),
                (displayTimeMs % 60_000) / 1000,
                displayTimeMs % 1000,
                displayTimeMs
        );
        String particlesInfo = String.format("Active 3D Particles: %d", activeParticles.size());
        String statusInfo = String.format("Queue: %d / %d lines", currentLyricIndex, lyricsQueue.size());
        String fetchInfo = "Lyrics fetch: " + fetchStatus;
        String retryInfo;
        if (lyricsQueue.isEmpty()) {
            long waitMs = Math.max(0, fetchRetryDelayMs - (System.currentTimeMillis() - lastFetchAttemptMs));
            retryInfo = String.format("Attempts: %d | next retry in %.1fs | duration: %ds",
                    fetchAttempts, waitMs / 1000.0f, currentTrackDurationSec);
        } else {
            retryInfo = String.format("Attempts: %d | duration: %ds", fetchAttempts, currentTrackDurationSec);
        }

        var matrices = event.getContext().getMatrices();
        float x = 10.0f;
        float y = 70.0f;

        Render2D.drawFont(matrices, Fonts.BOLD.getFont(15.0f), "[KineticLyrics Debug]", x, y, new Color(255, 90, 90, 255));
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(14.0f), titleStr, x, y + 16, new Color(220, 220, 220, 255));
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(14.0f), fetchInfo, x, y + 32, new Color(255, 200, 90, 255));
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(14.0f), timerInfo, x, y + 48, new Color(255, 255, 255, 255));
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(14.0f), particlesInfo, x, y + 64, new Color(120, 255, 120, 255));
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(14.0f), statusInfo, x, y + 80, new Color(180, 180, 255, 255));
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(14.0f), retryInfo, x, y + 96, new Color(200, 160, 255, 255));
    }

    private void renderScreenSubtitle(net.minecraft.client.util.math.MatrixStack matrices, String text, float alpha) {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        float fontSize = 13.5f;
        var font = Fonts.BOLD.getFont(fontSize);
        float textWidth = Fonts.BOLD.getWidth(text, fontSize);
        float padX = 16f;
        float pillW = textWidth + padX * 2f;
        float pillH = 26f;

        float pillX = (screenWidth - pillW) / 2f;
        float pillY = screenHeight - 88f;

        int bgAlpha = (int) (175 * alpha);
        int borderAlpha = (int) (120 * alpha);
        int textAlpha = (int) (255 * alpha);

        ThemeManager tm = ThemeManager.getInstance();
        Color accent = tm != null && tm.getCurrentTheme() != null ? tm.getCurrentTheme().getAccentColor() : Color.WHITE;

        Render2D.drawRoundedRect(matrices, pillX, pillY, pillW, pillH, 8f, new Color(12, 12, 16, bgAlpha));
        Render2D.drawBorder(matrices, pillX, pillY, pillW, pillH, 8f, 0f, 1f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), borderAlpha));

        Render2D.drawRoundedRect(matrices, pillX + pillW * 0.25f, pillY + pillH - 2f, pillW * 0.5f, 1.6f, 0.8f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (210 * alpha)));

        float textY = pillY + (pillH - Fonts.BOLD.getHeight(fontSize)) / 2f;
        Render2D.drawFont(matrices, font, text, pillX + padX, textY, new Color(255, 255, 255, textAlpha));
    }
}
