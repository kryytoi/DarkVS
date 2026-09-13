package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.animations.infinity.InfinityAnimation;
import dev.darkvisuals.client.util.media.NowPlayingBridge;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.client.util.renderer.fonts.Instance;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

 
public class MusicHUD extends HudElement implements ThemeManager.ThemeChangeListener {

     
     
     
    public enum Source { MUSIC, RECORD, UNKNOWN }

    public static final class Track {
        public final SoundInstance instance;
        public final String title;       
        public final String subtitle;    
        public final Source source;
        public final long detectedAtMs;

        Track(SoundInstance instance, String title, String subtitle, Source source) {
            this.instance = instance;
            this.title = title == null ? "" : title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.source = source == null ? Source.UNKNOWN : source;
            this.detectedAtMs = System.currentTimeMillis();
        }
    }

      
    private static volatile Track currentTrack = null;

     
     
     

      
    public static void onSoundPlayed(SoundInstance sound) {
        try {
            if (sound == null) return;
            SoundCategory category = sound.getCategory();
            if (category != SoundCategory.MUSIC && category != SoundCategory.RECORDS) return;

            Identifier id = sound.getId();
            if (id == null) return;

            String path = id.getPath();
            if (category == SoundCategory.RECORDS) {
                String song = path.startsWith("music_disc.") ? path.substring("music_disc.".length()) : path;
                String key = "jukebox_song." + id.getNamespace() + "." + song;
                String title = I18n.hasTranslation(key) ? I18n.translate(key) : prettify(song);
                currentTrack = new Track(sound, title, "Music Disc", Source.RECORD);
            } else {
                String[] parts = path.split("\\.");
                String title = prettify(parts[parts.length - 1]);
                String subtitle = parts.length > 2 ? prettify(parts[parts.length - 2]) : "Game Music";
                currentTrack = new Track(sound, title, subtitle, Source.MUSIC);
            }
        } catch (Throwable ignored) {
             
        }
    }

      
    public static void onSoundStopped(SoundInstance sound) {
        Track track = currentTrack;
        if (track != null && track.instance == sound) {
            currentTrack = null;
        }
    }

      
    public static void onAllSoundsStopped() {
        currentTrack = null;
    }

      
    private static String prettify(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] words = raw.replace('_', ' ').trim().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) sb.append(w.substring(1));
        }
        return sb.toString();
    }

 
    private static Track validatedTrack() {
        Track track = currentTrack;
        if (track == null) return null;
        if (System.currentTimeMillis() - track.detectedAtMs < 2000L) return track;
        try {
            if (!MinecraftClient.getInstance().getSoundManager().isPlaying(track.instance)) {
                currentTrack = null;
                return null;
            }
        } catch (Throwable ignored) {}
        return track;
    }

     
     
     
    private static AbstractTexture prevTexture;
    private static AbstractTexture nextTexture;
    private static AbstractTexture playTexture;
    private static AbstractTexture pauseTexture;
    private static AbstractTexture noteTexture;  

    private static AbstractTexture prevIcon()  { if (prevTexture == null)  prevTexture  = makeIcon("prev");  return prevTexture; }
    private static AbstractTexture nextIcon()  { if (nextTexture == null)  nextTexture  = makeIcon("next");  return nextTexture; }
    private static AbstractTexture playIcon()  { if (playTexture == null)  playTexture  = makeIcon("play");  return playTexture; }
    private static AbstractTexture pauseIcon() { if (pauseTexture == null) pauseTexture = makeIcon("pause"); return pauseTexture; }
    private static AbstractTexture noteIcon()  { if (noteTexture == null)  noteTexture  = makeIcon("note");  return noteTexture; }

    private static AbstractTexture makeIcon(String kind) {
        try {
            int s = 64;
            BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(java.awt.Color.WHITE);
            switch (kind) {
                case "play": {
                    int[] xs = {16, 16, 52};
                    int[] ys = {10, 54, 32};
                    g.fillPolygon(xs, ys, 3);
                    break;
                }
                case "pause": {
                    g.fillRoundRect(14, 10, 12, 44, 6, 6);
                    g.fillRoundRect(38, 10, 12, 44, 6, 6);
                    break;
                }
                case "next": {
                    g.fillPolygon(new int[]{6, 6, 32},  new int[]{12, 52, 32}, 3);
                    g.fillPolygon(new int[]{32, 32, 58}, new int[]{12, 52, 32}, 3);
                    break;
                }
                case "prev": {
                    g.fillPolygon(new int[]{58, 58, 32}, new int[]{12, 52, 32}, 3);
                    g.fillPolygon(new int[]{32, 32, 6},  new int[]{12, 52, 32}, 3);
                    break;
                }
                case "note": {
                     
                    g.fillOval(14, 40, 14, 11);
                    g.fillOval(38, 36, 14, 11);
                    g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(26, 45, 26, 16);
                    g.drawLine(50, 41, 50, 12);
                    g.drawLine(26, 16, 50, 12);
                    break;
                }
            }
            g.dispose();
            return Render2D.convert(img);
        } catch (Throwable t) {
            return null;
        }
    }

     
     
     
    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color subTextColor;

    private final InfinityAnimation fadeAnimation = new InfinityAnimation(Easing.BOTH_SINE);
    private final InfinityAnimation widthAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation progressAnimation = new InfinityAnimation(Easing.BOTH_SINE);

    public final BooleanSetting showProgress = new BooleanSetting("ShowProgress", true);
    public final BooleanSetting showControls = new BooleanSetting("ShowControls", true);
    public final BooleanSetting showAppIcon = new BooleanSetting("ShowAppIcon", true);

     
     
    private final float[] prevRect = new float[4];
    private final float[] midRect = new float[4];
    private final float[] nextRect = new float[4];
    private volatile boolean controlsClickable = false;

    public MusicHUD() {
        super("Music");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);

        getSettings().add(showProgress);
        getSettings().add(showControls);
        getSettings().add(showAppIcon);
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        this.bgColor = new Color(18, 18, 18, 240);
        this.textColor = Color.WHITE;
        this.subTextColor = new Color(185, 185, 185, 230);
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        Perf.tryBeginFrame();
        try (var __ = Perf.scopeCpu("MusicHUD.onRender2D")) {

             
            NowPlayingBridge.ensureStarted();

            boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

             
            NowPlayingBridge.Snapshot system = NowPlayingBridge.getCurrent();
            Track ingame = system == null ? validatedTrack() : null;

            String title;
            String artist;
            boolean playing;
            long positionSec;
            long durationSec;
            AbstractTexture artTexture = null;
            AbstractTexture appIconTexture = null;

            boolean hasAny = system != null || ingame != null;
            boolean previewMode = chatOpen && !hasAny;

             
            if (!hasAny && !previewMode) {
                fadeAnimation.animate(0f, 160);
                if (fadeAnimation.getValue() <= 0.01f) {
                    controlsClickable = false;
                    return;
                }
            } else {
                fadeAnimation.animate(1f, 160);
            }

            if (system != null) {
                title = system.title;
                artist = system.artist == null || system.artist.isEmpty() ? system.app : system.artist;
                playing = system.playing;
                positionSec = system.smoothedPositionSec();
                durationSec = system.durationSec;
                artTexture = NowPlayingBridge.getThumbTexture();
                appIconTexture = NowPlayingBridge.getAppIconTexture();
            } else if (ingame != null) {
                title = ingame.title;
                artist = ingame.subtitle;
                playing = true;
                positionSec = (System.currentTimeMillis() - ingame.detectedAtMs) / 1000L;
                durationSec = -1;
            } else if (previewMode) {
                title = "Track Title";
                artist = "Artist Name";
                playing = true;
                positionSec = 70;
                durationSec = 75;
            } else {
                 
                title = "";
                artist = "";
                playing = false;
                positionSec = -1;
                durationSec = -1;
            }

            float fade = Math.max(0f, Math.min(1f, fadeAnimation.getValue()));
            int contentAlpha = (int) (255 * fade);
            Color white = new Color(255, 255, 255, contentAlpha);

             
            float posX = getX();
            float posY = getY();
            float padding = 8f;
            float artSize = 24f;
            float appIconSize = 11f;
            float titleFontSize = 7.5f;
            float artistFontSize = 6.5f;
            float timeFontSize = 5.5f;
            float pillH = 9f;
            float progressH = 3f;
            float controlH = 9f;
            float textGap = 7f;

             
            float titleW = Fonts.MEDIUM.getWidth(title, titleFontSize);
            float artistW = Fonts.REGULAR.getWidth(artist, artistFontSize);
            float contentTextW = Math.max(titleW, artistW);
            float targetWidth = padding + artSize + textGap + contentTextW
                    + (showAppIcon.getValue() ? 6f + appIconSize : 0f) + padding;
            targetWidth = Math.max(130f, Math.min(210f, targetWidth));
            widthAnimation.animate(targetWidth, 220);
            float width = widthAnimation.getValue();

            float height = padding + artSize
                    + (showProgress.getValue() ? 6f + pillH : 0f)
                    + (showControls.getValue() ? 5f + controlH : 0f)
                    + padding;

            setBounds(posX, posY, width, height);

            e.getContext().getMatrices().push();
            var matrices = e.getContext().getMatrices();

             
            drawLiquidGlass(matrices, posX, posY, width, height, 9f, fade);

             
            float artX = posX + padding;
            float artY = posY + padding;
            if (artTexture != null) {
                Render2D.drawTexture(matrices, artX, artY, artSize, artSize, 4f, artTexture, white);
            } else {
                Render2D.drawHudPill(matrices, artX, artY, artSize, artSize, 4f, fade);
                AbstractTexture note = noteIcon();
                if (note != null) {
                    float ns = artSize * 0.55f;
                    Render2D.drawTexture(matrices,
                            artX + (artSize - ns) / 2f, artY + (artSize - ns) / 2f, ns, ns, 0f, note, white);
                }
            }

             
            float appIconX = posX + width - padding - appIconSize;
            if (showAppIcon.getValue()) {
                if (appIconTexture != null) {
                    Render2D.drawTexture(matrices,
                            appIconX, artY + (artSize - appIconSize) / 2f,
                            appIconSize, appIconSize, appIconSize / 2f, appIconTexture, white);
                } else {
                    AbstractTexture noteFallback = noteIcon();
                    if (noteFallback != null) {
                        Render2D.drawTexture(matrices,
                                appIconX, artY + (artSize - appIconSize) / 2f,
                                appIconSize, appIconSize, 0f, noteFallback, white);
                    }
                }
            }

             
            float textX = artX + artSize + textGap;
            float textAreaW = (showAppIcon.getValue() ? appIconX - 4f : posX + width - padding) - textX;

            String titleText = truncateToWidth(title, titleFontSize, textAreaW, true);
            String artistText = truncateToWidth(artist, artistFontSize, textAreaW, false);

            float titleH = Fonts.MEDIUM.getHeight(titleFontSize);
            float artistH = Fonts.REGULAR.getHeight(artistFontSize);
            float textBlockH = titleH + 2f + artistH;
            float titleY = artY + (artSize - textBlockH) / 2f;

            drawGlassText(matrices, Fonts.MEDIUM.getFont(titleFontSize), titleText, textX, titleY,
                    new Color(255, 255, 255, contentAlpha));
            if (!artistText.isEmpty()) {
                drawGlassText(matrices, Fonts.REGULAR.getFont(artistFontSize), artistText,
                        textX, titleY + titleH + 2f,
                        new Color(225, 228, 235, (int) (215 * fade)));
            }

            float cursorY = posY + padding + artSize;

             
            if (showProgress.getValue()) {
                cursorY += 6f;

                String elapsedText = positionSec >= 0 ? formatDuration(positionSec) : "-:--";
                String durationText = durationSec > 0 ? formatDuration(durationSec) : "-:--";

                float elapsedW = Fonts.REGULAR.getWidth(elapsedText, timeFontSize) + 6f;
                float durationW = Fonts.REGULAR.getWidth(durationText, timeFontSize) + 6f;

                 
                Render2D.drawHudPill(matrices, posX + padding, cursorY, elapsedW, pillH, pillH / 2f, fade);
                Render2D.drawHudPill(matrices, posX + width - padding - durationW, cursorY, durationW, pillH, pillH / 2f, fade);

                float timeTextY = cursorY + (pillH - Fonts.REGULAR.getHeight(timeFontSize)) / 2f;
                drawGlassText(matrices, Fonts.REGULAR.getFont(timeFontSize), elapsedText,
                        posX + padding + 3f, timeTextY, new Color(235, 238, 245, contentAlpha));
                drawGlassText(matrices, Fonts.REGULAR.getFont(timeFontSize), durationText,
                        posX + width - padding - durationW + 3f, timeTextY, new Color(235, 238, 245, contentAlpha));

                 
                float barX = posX + padding + elapsedW + 5f;
                float barW = (posX + width - padding - durationW - 5f) - barX;
                float barY = cursorY + (pillH - progressH) / 2f;
                Render2D.drawHudBarTrack(matrices, barX, barY, barW, progressH, 1.5f, fade);

                float progress;
                if (durationSec > 0 && positionSec >= 0) {
                    progress = Math.max(0f, Math.min(1f, (float) positionSec / (float) durationSec));
                } else {
                    progress = 0.15f + 0.15f * progressAnimation.getValue();
                    progressAnimation.animate((System.currentTimeMillis() / 1500 % 2 == 0) ? 1f : 0f, 700);
                }
                Render2D.drawRoundedRect(matrices, barX, barY,
                        Math.max(3f, barW * progress), progressH, 1.5f, white);

                cursorY += pillH;
            }

             
            if (showControls.getValue()) {
                cursorY += 5f;

                float midSize = controlH;
                float sideSize = controlH - 2f;
                float gap = 14f;
                float centerX = posX + width / 2f;

                AbstractTexture prev = prevIcon();
                AbstractTexture mid = playing ? pauseIcon() : playIcon();
                AbstractTexture next = nextIcon();

                Color controlColor = new Color(255, 255, 255, contentAlpha);

                float prevX = centerX - gap - sideSize;
                float sideY = cursorY + (midSize - sideSize) / 2f;
                float midX = centerX - midSize / 2f;
                float nextX = centerX + gap;

                if (prev != null) Render2D.drawTexture(matrices, prevX, sideY, sideSize, sideSize, 0f, prev, controlColor);
                if (mid != null)  Render2D.drawTexture(matrices, midX, cursorY, midSize, midSize, 0f, mid, controlColor);
                if (next != null) Render2D.drawTexture(matrices, nextX, sideY, sideSize, sideSize, 0f, next, controlColor);

                float pad = 2.5f;
                setRect(prevRect, prevX - pad, sideY - pad, sideSize + pad * 2f, sideSize + pad * 2f);
                setRect(midRect, midX - pad, cursorY - pad, midSize + pad * 2f, midSize + pad * 2f);
                setRect(nextRect, nextX - pad, sideY - pad, sideSize + pad * 2f, sideSize + pad * 2f);
                controlsClickable = system != null && fade > 0.5f;
            } else {
                controlsClickable = false;
            }

            e.getContext().getMatrices().pop();

             
             
            super.onRender2D(e);
        }
    }

     
     
     
     
    @Override
    public void onMouse(dev.darkvisuals.client.events.impl.EventMouse e) {
        if (fullNullCheck()) return;
        if (!(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) return;

         
         
         
        if (controlsClickable && e.getAction() == 1 && e.getButton() == 0) {
            float mx = mouseX();
            float my = mouseY();

            if (inRect(prevRect, mx, my)) {
                NowPlayingBridge.sendCommand("prev");
                return;  
            }
            if (inRect(midRect, mx, my)) {
                NowPlayingBridge.sendCommand("playpause");
                NowPlayingBridge.optimisticTogglePlaying();  
                return;
            }
            if (inRect(nextRect, mx, my)) {
                NowPlayingBridge.sendCommand("next");
                return;
            }
        }

         
        super.onMouse(e);
    }

    private static boolean inRect(float[] rect, float mx, float my) {
        return mx >= rect[0] && mx <= rect[0] + rect[2]
                && my >= rect[1] && my <= rect[1] + rect[3];
    }

    private static void setRect(float[] rect, float x, float y, float w, float h) {
        rect[0] = x;
        rect[1] = y;
        rect[2] = w;
        rect[3] = h;
    }

     
     
     

    private static String truncateToWidth(String text, float fontSize, float maxWidth, boolean bold) {
        if (text == null) return "";
        var font = bold ? Fonts.MEDIUM : Fonts.REGULAR;
        if (font.getWidth(text, fontSize) <= maxWidth) return text;
        String ellipsis = "...";
        String result = text;
        while (result.length() > 1 && font.getWidth(result + ellipsis, fontSize) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ellipsis;
    }

    private static String formatDuration(long totalSeconds) {
        long seconds = Math.max(0, totalSeconds);
        long minutes = seconds / 60L;
        return String.format("%d:%02d", minutes, seconds % 60L);
    }

      
    private void drawLiquidGlass(net.minecraft.client.util.math.MatrixStack matrices,
                                 float x, float y, float w, float h, float r, float fade) {
        Render2D.drawHudBackground(matrices, x, y, w, h, r, fade);
    }

      
    private void drawGlassText(net.minecraft.client.util.math.MatrixStack matrices,
                               Object font, String text, float x, float y, Color color) {
        Render2D.drawHudText(matrices, (Instance) font, text, x, y, color);
    }
}
