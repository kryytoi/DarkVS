package dev.darkvisuals.client.ui.mainmenu;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.animations.Animation;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.achievements.Achievement;
import dev.darkvisuals.client.util.achievements.AchievementManager;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.minecraft.util.math.RotationAxis;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.FileDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Главное меню Dark Visuals — верстка по макету:
 *
 *  ┌──────────────────────────────────────────────┐
 *  │  ⌃  Dark Visuals                              │
 *  │                                                │
 *  │  [ Сетевая игра ]      [ Одиночная Игра ]      │
 *  │  [ Аккаунты    ]       [ Настройки     ]       │
 *  │  [           Выйти                     ]       │
 *  └──────────────────────────────────────────────┘
 *
 * Sk3d Expensive 1.21 ( SK3D )
 */
public class MainMenu extends Screen implements Wrapper {

    // ── Анимации ──────────────────────────────────────────────────────────────
    private Animation entrance     = new Animation(500L, 1.0, true, Easing.EASE_OUT_CUBIC);
    private final Animation mpHover   = new Animation(150L, 1.0, false, Easing.EASE_OUT_CUBIC);
    private final Animation spHover   = new Animation(150L, 1.0, false, Easing.EASE_OUT_CUBIC);
    private final Animation accHover  = new Animation(150L, 1.0, false, Easing.EASE_OUT_CUBIC);
    private final Animation setHover  = new Animation(150L, 1.0, false, Easing.EASE_OUT_CUBIC);
    private final Animation exitHover = new Animation(150L, 1.0, false, Easing.EASE_OUT_CUBIC);
    private final Animation logoHover = new Animation(150L, 1.0, false, Easing.EASE_OUT_CUBIC);

    // момент появления меню — для каскадной анимации кнопок
    private long shownAtMs = 0L;

    // ── Иконки (из assets/darkvisuals/textures/mainmenu) ────────────────────
    private static final Identifier ICO_MULTI   = Identifier.of("darkvisuals", "textures/mainmenu/multi.png");
    private static final Identifier ICO_SOLO    = Identifier.of("darkvisuals", "textures/mainmenu/solo.png");
    private static final Identifier ICO_ACCOUNT = Identifier.of("darkvisuals", "textures/mainmenu/account.png");
    private static final Identifier ICO_OPTIONS = Identifier.of("darkvisuals", "textures/mainmenu/options.png");
    private static final Identifier ICO_EXIT    = Identifier.of("darkvisuals", "textures/mainmenu/exit.png");
    private static final Identifier ICO_LOGO    = Identifier.of("darkvisuals", "textures/mainmenu/logo.png");

    // ── Фон ───────────────────────────────────────────────────────────────────
    private static final Identifier DEFAULT_BG_ID = Identifier.of("darkvisuals", "textures/background.png");

    // ── Кастомный логотип (клик по кружку профиля) ────────────────────────────
    private static final String LOGO_FILE_NAME = "custom_logo.png";
    private static AbstractTexture customLogoTexture = null;
    private static boolean logoLoadAttempted = false;

    // ── Геометрия панели (пропорции макета) ────────────────────────────────────
    private static final float PANEL_W       = 480f;   // ширина плашки-контейнера
    private static final float PANEL_PAD_X   = 34f;    // внутренние отступы по X
    private static final float PANEL_PAD_TOP = 30f;    // отступ сверху до шапки
    private static final float PANEL_PAD_BOT = 30f;    // отступ снизу после кнопки "Выйти"
    private static final float PANEL_RADIUS  = 18f;

    private static final float HEADER_LOGO_SIZE = 36f;
    private static final float HEADER_GAP       = 44f; // отступ между шапкой и первым рядом кнопок

    private static final float BTN_H      = 34f;
    private static final float ROW_GAP    = 10f;   // вертикальный зазор между рядами кнопок
    private static final float COL_GAP    = 10f;   // горизонтальный зазор между кнопками в ряду
    private static final float RADIUS     = 8f;

    // ── Цвета ─────────────────────────────────────────────────────────────────
    private static final Color OVERLAY_COLOR   = new Color(4, 4, 8, 90);
    private static final Color PANEL_FILL      = new Color(15, 15, 15, 191);   // rgba(15,15,15,0.75)
    private static final Color PANEL_BORDER    = new Color(255, 255, 255, 18);
    private static final Color GLASS_BTN_FILL  = new Color(0, 0, 0, 220);
    private static final Color GLASS_BTN_TEXT  = new Color(235, 235, 240, 255);
    private static final Color MUTED_TEXT      = new Color(200, 200, 210, 210);
    private static final Color ACCENT_PURPLE   = new Color(0x99, 0x00, 0xFF);
    private static final Color EXIT_RED        = new Color(0xE5, 0x3E, 0x3E);

    private static final String VERSION = "darkvisuals v1.0";

    // ═══════════════════════════════════════════════════════════════════════════
    //  ЛИСТОПАД — воксельные (блочные) осенние листья, падающие сверху экрана
    // ═══════════════════════════════════════════════════════════════════════════
    private static final Color[] LEAF_PALETTE = new Color[] {
            new Color(214, 110, 40),   // оранжевый
            new Color(178, 58, 38),    // красно-коричневый
            new Color(224, 168, 54),   // жёлто-оранжевый
            new Color(150, 75, 40),    // коричневый
            new Color(196, 84, 50),    // терракотовый
            new Color(232, 140, 46)    // янтарный
    };

    private static final int LEAF_COUNT_BACK  = 22; // листья за плашкой (фон)
    private static final int LEAF_COUNT_FRONT = 12; // листья перед плашкой (передний план)

    private final Random leafRandom = new Random();
    private final List<Leaf> leavesBack  = new ArrayList<>();
    private final List<Leaf> leavesFront = new ArrayList<>();
    private long lastLeafNanoTime = 0L;

    // Тонкие фоновые частицы: добавляют глубину, но не перекрывают интерфейс.
    private static final int AMBIENT_PARTICLE_COUNT = 34;
    private final List<AmbientParticle> ambientParticles = new ArrayList<>();
    private long lastAmbientNanoTime = 0L;

    private static final class AmbientParticle {
        float x, y, speed, radius, phase;
        Color color;
    }

    private void initAmbientParticles() {
        ambientParticles.clear();
        Random random = new Random(0xD4A2B17L);
        for (int i = 0; i < AMBIENT_PARTICLE_COUNT; i++) {
            AmbientParticle particle = new AmbientParticle();
            particle.x = random.nextFloat() * Math.max(1, width);
            particle.y = random.nextFloat() * Math.max(1, height);
            particle.speed = 5f + random.nextFloat() * 13f;
            particle.radius = 0.7f + random.nextFloat() * 1.8f;
            particle.phase = random.nextFloat() * (float) (Math.PI * 2f);
            particle.color = random.nextBoolean() ? new Color(153, 0, 255) : new Color(76, 190, 255);
            ambientParticles.add(particle);
        }
        lastAmbientNanoTime = System.nanoTime();
    }

    private void updateAmbientParticles(float width, float height, float dt) {
        for (AmbientParticle particle : ambientParticles) {
            particle.phase += dt * 0.8f;
            particle.y -= particle.speed * dt;
            particle.x += (float) Math.sin(particle.phase) * 5f * dt;
            if (particle.y < -8f) {
                particle.y = height + 8f;
                particle.x = (particle.x + width * 0.37f) % width;
            }
        }
    }

    private void drawAmbientParticles(MatrixStack stack, float alpha) {
        for (AmbientParticle particle : ambientParticles) {
            float pulse = 0.45f + 0.55f * (float) Math.sin(particle.phase * 1.7f);
            int outerAlpha = (int) (22f * alpha * pulse);
            int coreAlpha = (int) (105f * alpha * pulse);
            Render2D.drawRoundedRect(stack, particle.x - particle.radius * 2f, particle.y - particle.radius * 2f,
                    particle.radius * 4f, particle.radius * 4f, particle.radius * 2f,
                    withAlpha(particle.color, outerAlpha));
            Render2D.drawRoundedRect(stack, particle.x - particle.radius / 2f, particle.y - particle.radius / 2f,
                    particle.radius, particle.radius, particle.radius / 2f,
                    withAlpha(lighten(particle.color, 0.45f), coreAlpha));
        }
    }

    /** Один воксельный лист-частица. */
    private static final class Leaf {
        float x, y;
        float vy;
        float swayPhase, swaySpeed, swayAmp;
        float rotation, rotSpeed;
        float size;
        Color color;
        Color shade;
        Color highlight;
    }

    private Leaf spawnLeaf(float width, float height, boolean initial, boolean foreground, Random r) {
        Leaf l = new Leaf();
        l.size = foreground ? (9f + r.nextFloat() * 6f) : (4f + r.nextFloat() * 4f);
        l.x = r.nextFloat() * width;
        l.y = initial
                ? r.nextFloat() * (height + 200f) - 200f
                : -l.size * 2f - r.nextFloat() * 160f;
        l.vy = (foreground ? (26f + r.nextFloat() * 26f) : (14f + r.nextFloat() * 16f));
        l.swayAmp = 8f + r.nextFloat() * 20f;
        l.swaySpeed = 0.5f + r.nextFloat() * 1.0f;
        l.swayPhase = r.nextFloat() * (float) (Math.PI * 2f);
        l.rotation = r.nextFloat() * (float) (Math.PI * 2f);
        l.rotSpeed = (r.nextBoolean() ? 1f : -1f) * (0.4f + r.nextFloat() * 1.1f);
        l.color = LEAF_PALETTE[r.nextInt(LEAF_PALETTE.length)];
        l.shade = darken(l.color, 0.30f);
        l.highlight = lighten(l.color, 0.35f);
        return l;
    }

    private void initLeaves() {
        leavesBack.clear();
        leavesFront.clear();
        float w = Math.max(1, this.width);
        float h = Math.max(1, this.height);
        for (int i = 0; i < LEAF_COUNT_BACK; i++) {
            leavesBack.add(spawnLeaf(w, h, true, false, leafRandom));
        }
        for (int i = 0; i < LEAF_COUNT_FRONT; i++) {
            leavesFront.add(spawnLeaf(w, h, true, true, leafRandom));
        }
        lastLeafNanoTime = System.nanoTime();
    }

    private void updateLeaves(float width, float height, float dt) {
        updateLeafList(leavesBack, width, height, dt, false);
        updateLeafList(leavesFront, width, height, dt, true);
    }

    private void updateLeafList(List<Leaf> list, float width, float height, float dt, boolean foreground) {
        for (int i = 0; i < list.size(); i++) {
            Leaf l = list.get(i);
            l.swayPhase += l.swaySpeed * dt;
            l.x += (float) Math.sin(l.swayPhase) * l.swayAmp * dt;
            l.y += l.vy * dt;
            l.rotation += l.rotSpeed * dt;

            if (l.y - l.size > height + 20f) {
                list.set(i, spawnLeaf(width, height, false, foreground, leafRandom));
            }
        }
    }

    /** Рисует один воксельный лист: базовый пиксельный блок + тень + блик. */
    private void drawLeaf(MatrixStack stack, Leaf leaf, float globalAlpha) {
        int a = (int) (215 * globalAlpha);
        if (a <= 2) return;

        stack.push();
        stack.translate(leaf.x, leaf.y, 0f);
        stack.multiply(RotationAxis.POSITIVE_Z.rotation(leaf.rotation));

        float s = leaf.size;
        Render2D.drawRoundedRect(stack, -s / 2f, -s / 2f, s, s, 1f, withAlpha(leaf.color, a));

        float shadeSize = s * 0.42f;
        Render2D.drawRoundedRect(stack, 0f, 0f, shadeSize, shadeSize, 0.5f, withAlpha(leaf.shade, a));

        float hiSize = s * 0.32f;
        Render2D.drawRoundedRect(stack, -s / 2f, -s / 2f, hiSize, hiSize, 0.5f,
                withAlpha(leaf.highlight, (int) (a * 0.85f)));

        stack.pop();
    }

    private void drawLeaves(MatrixStack stack, List<Leaf> list, float globalAlpha) {
        for (Leaf l : list) {
            drawLeaf(stack, l, globalAlpha);
        }
    }

    // Кэш реально отрисованной раскладки — клики всегда совпадают с картинкой.
    private float lastPanelX, lastPanelY, lastPanelW, lastPanelH;
    private float lastLogoX, lastLogoY;
    private float lastY1, lastY2, lastY3;
    private float lastBtnH = BTN_H;
    private float lastColX1, lastColX2, lastColW1, lastColW2;
    private float lastExitX, lastExitY, lastExitW, lastExitH;
    private float lastAchX, lastAchY;
    private final List<float[]> lastAchievementHitboxes = new java.util.ArrayList<>();
    private final List<Achievement> lastAchievementList = new java.util.ArrayList<>();
    private static final float ACH_ICON_SIZE = 18f;
    private static final float ACH_ICON_GAP  = 6f;

    public MainMenu() {
        super(Text.of("darkvisuals"));
    }

    @Override
    protected void init() {
        super.init();
        entrance = new Animation(500L, 1.0, true, Easing.EASE_OUT_CUBIC);
        shownAtMs = System.currentTimeMillis();
        loadCustomLogoIfNeeded();
        AchievementManager.loadIfNeeded();
        initLeaves();
        initAmbientParticles();
    }

    /** Каскадный шаг появления: 0..1 после задержки delayMs. */
    private static float entranceStep(long elapsedMs, long delayMs) {
        float v = (elapsedMs - delayMs) / 450f;
        return Math.max(0f, Math.min(1f, v));
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean shouldPause() { return false; }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CUSTOM LOGO — сохранение и загрузка
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Кандидаты на папку с логотипом (по приоритету):
     * 1. Папка исходников dev-сборки (как просил пользователь);
     * 2. Та же папка, найденная относительно run-директории (переносимость);
     * 3. Папка в run-директории игры (fallback для собранного мода).
     */
    private static File[] logoDirCandidates() {
        return new File[] {
                new File("C:\\Users\\NewDarko\\Desktop\\1.21.4-Stable-main\\src\\main\\resources\\assets\\darkvisuals\\textures\\mainmenu"),
                new File(mc.runDirectory, ".." + File.separator + "src" + File.separator + "main"
                        + File.separator + "resources" + File.separator + "assets"
                        + File.separator + "darkvisuals" + File.separator + "textures"
                        + File.separator + "mainmenu"),
                new File(mc.runDirectory, "darkvisuals")
        };
    }

    /** Ищет ранее сохранённый custom_logo.png по всем кандидатам. */
    private static File findSavedLogo() {
        for (File dir : logoDirCandidates()) {
            File f = new File(dir, LOGO_FILE_NAME);
            if (f.isFile()) return f;
        }
        return null;
    }

    /** Загружает сохранённый логотип один раз при открытии меню. */
    private static void loadCustomLogoIfNeeded() {
        if (logoLoadAttempted) return;
        logoLoadAttempted = true;
        try {
            File saved = findSavedLogo();
            if (saved == null) return;
            BufferedImage img = ImageIO.read(saved);
            if (img != null) {
                customLogoTexture = Render2D.convert(img);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Сохраняет выбранную картинку как custom_logo.png.
     * Пишем в первую доступную папку из кандидатов (в существующие src-папки —
     * без создания, чтобы не плодить чужие пути; run-папку создаём при необходимости).
     */
    private static void saveLogoFile(BufferedImage img) {
        File[] dirs = logoDirCandidates();
        for (int i = 0; i < dirs.length; i++) {
            File dir = dirs[i];
            boolean isFallback = (i == dirs.length - 1);
            if (!dir.isDirectory()) {
                if (!isFallback) continue;      // src-папки не создаём на чужих машинах
                if (!dir.mkdirs()) continue;
            }
            try {
                File target = new File(dir, LOGO_FILE_NAME);
                ImageIO.write(img, "PNG", target);
                return;                          // сохранили — выходим
            } catch (Throwable ignored) {
            }
        }
    }

    /** Открывает системный диалог выбора файла и применяет новый логотип. */
    private void openLogoChooser() {
        new Thread(() -> {
            try {
                System.setProperty("java.awt.headless", "false");
                FileDialog fd = new FileDialog((java.awt.Frame) null, "Выбор логотипа", FileDialog.LOAD);
                fd.setFile("*.png;*.jpg;*.jpeg");
                fd.setVisible(true);
                String dir = fd.getDirectory();
                String file = fd.getFile();
                fd.dispose();
                if (dir == null || file == null) return;

                BufferedImage img = ImageIO.read(new File(dir, file));
                if (img == null) return;

                saveLogoFile(img);

                // Создание текстуры — только в render-потоке игры
                mc.execute(() -> customLogoTexture = Render2D.convert(img));
            } catch (Throwable ignored) {
            }
        }, "MainMenu-LogoChooser").start();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        if (!Fonts.isLoaded() && !Fonts.load()) {
            try {
                this.renderBackground(context, mouseX, mouseY, delta);
                if (this.textRenderer != null) {
                    String msg = "Dark Visuals: загрузка шрифтов…";
                    context.drawCenteredTextWithShadow(this.textRenderer, msg,
                            this.width / 2, this.height / 2, 0xFFFFFFFF);
                }
            } catch (Throwable ignored) {
                // На самом раннем кадре даже ванильный фон может быть не готов —
                // тогда просто пропускаем кадр.
            }
            return;
        }

        // Весь основной рендер оборачиваем в try/catch: даже если что-то внутри
        // MSDF-рендера пойдёт не так, меню не должно ронять игру.
        try {
            renderMenu(context, mouseX, mouseY, delta);
        } catch (Throwable t) {
            darkvisuals.LOGGER.error("[DarkVisuals] Ошибка отрисовки главного меню:", t);
            try { this.renderBackground(context, mouseX, mouseY, delta); } catch (Throwable ignored) {}
        }
    }

    private int lastLeafScreenW = -1, lastLeafScreenH = -1;

    private void renderMenu(DrawContext context, int mouseX, int mouseY, float delta) {
        float t = (float) entrance.getValue();

        MatrixStack stack = context.getMatrices();

        int W = this.width, H = this.height;

        if (leavesBack.isEmpty() && leavesFront.isEmpty()) {
            initLeaves();
        }
        if (ambientParticles.isEmpty()) {
            initAmbientParticles();
        }
        lastLeafScreenW = W;
        lastLeafScreenH = H;

        // ── Фон (панорама / текущая логика фона без изменений) ─────────────────
        net.minecraft.util.Identifier introFrame = dev.darkvisuals.client.util.IntroManager.currentFrame();
        if (introFrame != null) {
            Render2D.drawTexture(stack, 0, 0, W, H, 0f, introFrame, Color.WHITE);
        } else {
            Render2D.drawTexture(stack, 0, 0, W, H, 0f, DEFAULT_BG_ID, Color.WHITE);
        }
        Render2D.drawRect(stack, 0, 0, W, H, OVERLAY_COLOR);

        // мягкая виньетка по краям экрана — добавляет глубину
        Render2D.drawGradientRect(stack, 0, 0, W, H * 0.28f,
                new Color(0, 0, 0, 115), new Color(0, 0, 0, 0), false);
        Render2D.drawGradientRect(stack, 0, H * 0.72f, W, H * 0.28f,
                new Color(0, 0, 0, 0), new Color(0, 0, 0, 140), false);

        // ── Листопад: обновляем частицы и рисуем фоновый слой (за плашкой) ──────
        long nowNanos = System.nanoTime();
        float leafDt = lastLeafNanoTime == 0L ? 0f
                : Math.min(0.05f, (nowNanos - lastLeafNanoTime) / 1_000_000_000f);
        lastLeafNanoTime = nowNanos;
        updateLeaves(W, H, leafDt);
        drawLeaves(stack, leavesBack, t);

        long ambientNow = System.nanoTime();
        float ambientDt = lastAmbientNanoTime == 0L ? 0f
                : Math.min(0.05f, (ambientNow - lastAmbientNanoTime) / 1_000_000_000f);
        lastAmbientNanoTime = ambientNow;
        updateAmbientParticles(W, H, ambientDt);
        drawAmbientParticles(stack, t);

        // ── Раскладка панели (высота считается снизу вверх по контенту) ────────
        float panelW = Math.min(PANEL_W, W - 40f);

        float headerH   = HEADER_LOGO_SIZE;
        float contentH  = PANEL_PAD_TOP + headerH + HEADER_GAP
                + BTN_H + ROW_GAP
                + BTN_H + ROW_GAP
                + BTN_H
                + PANEL_PAD_BOT;

        float panelX = (W - panelW) / 2f;
        float panelY = (H - contentH) / 2f;

        // панель выезжает снизу и слегка «раздается» при появлении
        stack.push();
        stack.translate(W / 2f, H / 2f, 0f);
        float panelScale = 0.965f + 0.035f * t;
        stack.scale(panelScale, panelScale, 1f);
        stack.translate(-W / 2f, -H / 2f, 0f);
        stack.translate(0f, (1f - t) * 16f, 0f);

        Render2D.drawRoundedRect(stack, panelX, panelY, panelW, contentH, PANEL_RADIUS,
                withAlpha(PANEL_FILL, (int) (PANEL_FILL.getAlpha() * t)));
        Render2D.drawBorder(stack, panelX, panelY, panelW, contentH, PANEL_RADIUS, 0f, 1f,
                withAlpha(PANEL_BORDER, (int) (PANEL_BORDER.getAlpha() * t)));
        float accentPulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 720.0);
        Render2D.drawRoundedRect(stack, panelX + 28f, panelY + 1f, panelW - 56f, 1.5f, 1f,
                new Color(153, 0, 255, (int) (85f * t * accentPulse)));

        this.lastPanelX = panelX; this.lastPanelY = panelY;
        this.lastPanelW = panelW; this.lastPanelH = contentH;

        float innerX = panelX + PANEL_PAD_X;
        float innerW = panelW - PANEL_PAD_X * 2f;

        // ── Шапка: логотип-галочка + "Dark Visuals" — по центру панели, крупнее ──
        float headerY = panelY + PANEL_PAD_TOP;

        float titleSz    = 20f;
        String titleText = "Dark Visuals";
        float titleW     = Fonts.BOLD.getWidth(titleText, titleSz);
        float logoTextGap = 16f;

        float headerBlockW = HEADER_LOGO_SIZE + logoTextGap + titleW;
        float headerBlockX = innerX + innerW / 2f - headerBlockW / 2f;

        float logoX = headerBlockX;
        // логотип мягко покачивается
        float logoBob = (float) Math.sin((System.currentTimeMillis() - shownAtMs) / 900.0) * 1.5f;
        drawHeaderLogo(stack, logoX, headerY + logoBob, HEADER_LOGO_SIZE, t);
        this.lastLogoX = logoX; this.lastLogoY = headerY;

        float titleX = logoX + HEADER_LOGO_SIZE + logoTextGap;
        float titleY = headerY + HEADER_LOGO_SIZE / 2f - Fonts.BOLD.getHeight(titleSz) / 2f;
        // заголовок дышит: яркость и акцентная линия под ним
        float titlePulse = 0.85f + 0.15f * (float) Math.sin((System.currentTimeMillis() - shownAtMs) / 1100.0);
        Render2D.drawFont(stack, Fonts.BOLD.getFont(titleSz), titleText,
                titleX, titleY, new Color(255, 255, 255, (int) (150 * t * titlePulse)));
        Render2D.drawRoundedRect(stack, headerBlockX, headerY + HEADER_LOGO_SIZE + 7f, headerBlockW, 1.2f, 0.6f,
                withAlpha(ACCENT_PURPLE, (int) (95 * t * titlePulse)));

        // ── Кнопки ────────────────────────────────────────────────────────────
        float y1 = headerY + headerH + HEADER_GAP;
        float y2 = y1 + BTN_H + ROW_GAP;
        float y3 = y2 + BTN_H + ROW_GAP;

        float colW = (innerW - COL_GAP) / 2f;
        float col1X = innerX;
        float col2X = innerX + colW + COL_GAP;

        this.lastColX1 = col1X; this.lastColX2 = col2X;
        this.lastColW1 = colW;  this.lastColW2 = colW;
        this.lastY1 = y1; this.lastY2 = y2; this.lastY3 = y3;
        this.lastBtnH = BTN_H;

        // Ряд 1 ──────────────────────────────────────────────��───────────────
        long elapsedMs = System.currentTimeMillis() - shownAtMs;

        float b1 = entranceStep(elapsedMs, 120L);
        mpHover.update(isHovered(mouseX, mouseY, col1X, y1, colW, BTN_H));
        if (b1 > 0.01f) {
            stack.push();
            stack.translate(0f, (1f - b1) * 16f - 2f * (float) mpHover.getValue(), 0f);
            drawFilledButton(stack, col1X, y1, colW, BTN_H, "Сетевая игра",
                    ICO_MULTI, ACCENT_PURPLE, mpHover.getValue(), t * b1, true);
            stack.pop();
        }

        float b2 = entranceStep(elapsedMs, 190L);
        spHover.update(isHovered(mouseX, mouseY, col2X, y1, colW, BTN_H));
        if (b2 > 0.01f) {
            stack.push();
            stack.translate(0f, (1f - b2) * 16f - 2f * (float) spHover.getValue(), 0f);
            drawGlassButton(stack, col2X, y1, colW, BTN_H, "Одиночная Игра",
                    ICO_SOLO, ACCENT_PURPLE, spHover.getValue(), t * b2, false);
            stack.pop();
        }

        // Ряд 2 ──────────────────────────────────────────────────────────────
        float b3 = entranceStep(elapsedMs, 260L);
        accHover.update(isHovered(mouseX, mouseY, col1X, y2, colW, BTN_H));
        if (b3 > 0.01f) {
            stack.push();
            stack.translate(0f, (1f - b3) * 16f - 2f * (float) accHover.getValue(), 0f);
            drawGlassButton(stack, col1X, y2, colW, BTN_H, "Аккаунты",
                    ICO_ACCOUNT, ACCENT_PURPLE, accHover.getValue(), t * b3, true);
            stack.pop();
        }

        float b4 = entranceStep(elapsedMs, 330L);
        setHover.update(isHovered(mouseX, mouseY, col2X, y2, colW, BTN_H));
        if (b4 > 0.01f) {
            stack.push();
            stack.translate(0f, (1f - b4) * 16f - 2f * (float) setHover.getValue(), 0f);
            drawGlassButton(stack, col2X, y2, colW, BTN_H, "Настройки",
                    ICO_OPTIONS, ACCENT_PURPLE, setHover.getValue(), t * b4, false);
            stack.pop();
        }

        // Ряд 3 — "Выйти" на всю ширину ─────────────────────────────────────
        float b5 = entranceStep(elapsedMs, 400L);
        float exitW = innerW;
        exitHover.update(isHovered(mouseX, mouseY, col1X, y3, exitW, BTN_H));
        if (b5 > 0.01f) {
            stack.push();
            stack.translate(0f, (1f - b5) * 16f - 2f * (float) exitHover.getValue(), 0f);
            drawExitButton(stack, col1X, y3, exitW, BTN_H, "Выйти", exitHover.getValue(), t * b5);
            stack.pop();
        }

        stack.pop(); // конец трансформации появления панели

        this.lastExitX = col1X; this.lastExitY = y3;
        this.lastExitW = exitW; this.lastExitH = BTN_H;

        // ── Профиль / достижения — в верхнем левом углу экрана, поверх фона ─────
        String username = mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        drawAchievements(stack, 20f, 20f, mouseX, mouseY, t);

        // ── Версия ─────────────────────────────────────────────────────
        float verSz = 8f;
        float verW  = Fonts.REGULAR.getWidth(VERSION, verSz);
        float verT = entranceStep(System.currentTimeMillis() - shownAtMs, 650L);
        Render2D.drawFont(stack, Fonts.REGULAR.getFont(verSz), VERSION,
                W - verW - 10f, H - verSz - 10f,
                new Color(210, 210, 220, (int) (180 * t * verT)));

        // ── Листопад: передний слой (крупные листья поверх интерфейса) ──────────
        drawLeaves(stack, leavesFront, t);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MOUSE CLICK
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return false;

        // Ряд 1
        if (isHovered(mx, my, lastColX1, lastY1, lastColW1, lastBtnH)) {
            mc.setScreen(new MultiplayerScreen(this));
            return true;
        }
        if (isHovered(mx, my, lastColX2, lastY1, lastColW2, lastBtnH)) {
            mc.setScreen(new SelectWorldScreen(this));
            return true;
        }

        // Ряд 2
        if (isHovered(mx, my, lastColX1, lastY2, lastColW1, lastBtnH)) {
            mc.setScreen(new AltManagerScreen(this));
            return true;
        }
        if (isHovered(mx, my, lastColX2, lastY2, lastColW2, lastBtnH)) {
            mc.setScreen(new OptionsScreen(this, mc.options));
            return true;
        }

        // Ряд 3 — Выйти
        if (isHovered(mx, my, lastExitX, lastExitY, lastExitW, lastExitH)) {
            mc.scheduleStop();
            new Thread(() -> {
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException ignored) {
                }
                System.exit(0);
            }, "MainMenu-ForceExit").start();
            return true;
        }

        // Клик по логотипу — смена кастомного лого
        if (isHovered(mx, my, lastLogoX, lastLogoY, HEADER_LOGO_SIZE, HEADER_LOGO_SIZE)) {
            openLogoChooser();
            return true;
        }

        // Клики по иконкам достижений
        for (int i = 0; i < lastAchievementHitboxes.size(); i++) {
            float[] hb = lastAchievementHitboxes.get(i);
            if (isHovered(mx, my, hb[0], hb[1], hb[2], hb[3])) {
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DRAW HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Фиолетовый логотип-галочка в шапке (или кастомный логотип пользователя). */
    private void drawHeaderLogo(MatrixStack stack, float x, float y, float size, float alpha) {
        if (customLogoTexture != null) {
            Render2D.drawTexture(stack, x, y, size, size, 4f,
                    customLogoTexture, new Color(255, 255, 255, (int) (255 * alpha)));
        } else {
            Render2D.drawTexture(stack, x, y, size, size, 0f,
                    ICO_LOGO, withAlpha(ACCENT_PURPLE, (int) (255 * alpha)));
        }
    }

    /**
     * Заполненная (выделенная) кнопка — используется для "Сетевая игра".
     * iconLeft = true — иконка слева от текста, иначе иконка справа (не используется здесь,
     * оставлено для единообразия сигнатуры с drawGlassButton).
     */
    private void drawFilledButton(MatrixStack stack, float x, float y, float w, float h,
                                  String label, Identifier icon, Color accent, double hover, float alpha,
                                  boolean iconLeft) {
        Color fill = hover > 0.01 ? lighten(accent, (float) (0.10f * hover)) : accent;
        Render2D.drawRoundedRect(stack, x, y, w, h, RADIUS, withAlpha(fill, (int) (255 * alpha)));
        Render2D.drawBorder(stack, x, y, w, h, RADIUS, 0f, 1f,
                new Color(255, 255, 255, (int) (55 * alpha)));
        // подчёркивание, «проезжающее» при наведении
        if (hover > 0.01) {
            float uw = (w - 16f) * (float) hover;
            Render2D.drawRoundedRect(stack, x + 8f + (w - 16f - uw) / 2f, y + h - 2f, uw, 1.4f, 0.7f,
                    new Color(255, 255, 255, (int) (175 * hover * alpha)));
        }

        float iconSz = Math.min(15f, h - 12f);
        float pad = 12f;
        float sz = 7.5f;

        // иконка мягко придвигается к тексту при наведении
        float iconX = x + pad + 2f * (float) hover;
        float textX = iconX + iconSz + 8f;

        Render2D.drawTexture(stack, iconX, y + h / 2f - iconSz / 2f, iconSz, iconSz, 0f, icon,
                new Color(255, 255, 255, (int) (255 * alpha)));
        Render2D.drawFont(stack, Fonts.BOLD.getFont(sz), label, textX, y + h / 2f - sz / 2f,
                new Color(255, 255, 255, (int) (255 * alpha)));
    }

    /**
     * Тёмная («стеклянная») кнопка. iconLeft управляет тем, с какой стороны
     * рисуется иконка: слева ("Аккаунты") или справа ("Одиночная Игра", "Настройки").
     */
    private void drawGlassButton(MatrixStack stack, float x, float y, float w, float h,
                                 String label, Identifier icon, Color accent, double hover, float alpha,
                                 boolean iconLeft) {
        Render2D.drawRoundedRect(stack, x, y, w, h, RADIUS,
                withAlpha(GLASS_BTN_FILL, (int) (GLASS_BTN_FILL.getAlpha() * alpha)));
        Render2D.drawBorder(stack, x, y, w, h, RADIUS, 0f, 1f,
                new Color(255, 255, 255, (int) (28 * alpha)));
        if (hover > 0.01) {
            Render2D.drawRoundedRect(stack, x, y, w, h, RADIUS,
                    withAlpha(accent, (int) (35 * hover * alpha)));
            // подчёркивание, «проезжающее» при наведении
            float uw = (w - 16f) * (float) hover;
            Render2D.drawRoundedRect(stack, x + 8f + (w - 16f - uw) / 2f, y + h - 2f, uw, 1.4f, 0.7f,
                    withAlpha(accent, (int) (185 * hover * alpha)));
        }

        float iconSz = Math.min(14f, h - 14f);
        float pad = 12f;
        float sz = 7.5f;
        float slide = 2f * (float) hover;

        if (iconLeft) {
            float iconX = x + pad + slide;
            float textX = iconX + iconSz + 8f;
            Render2D.drawTexture(stack, iconX, y + h / 2f - iconSz / 2f, iconSz, iconSz, 0f, icon,
                    new Color(255, 255, 255, (int) (230 * alpha)));
            Render2D.drawFont(stack, Fonts.SEMIBOLD.getFont(sz), label, textX, y + h / 2f - sz / 2f,
                    withAlpha(GLASS_BTN_TEXT, (int) (255 * alpha)));
        } else {
            float iconX = x + w - pad - iconSz - slide;
            float textW = Fonts.SEMIBOLD.getWidth(label, sz);
            float textX = x + w / 2f - (textW + iconSz + 8f) / 2f;
            // Если текст не помещается по центру с учётом иконки — прижимаем влево.
            if (textX < x + pad) textX = x + pad;
            Render2D.drawFont(stack, Fonts.SEMIBOLD.getFont(sz), label, textX, y + h / 2f - sz / 2f,
                    withAlpha(GLASS_BTN_TEXT, (int) (255 * alpha)));
            Render2D.drawTexture(stack, iconX, y + h / 2f - iconSz / 2f, iconSz, iconSz, 0f, icon,
                    new Color(255, 255, 255, (int) (230 * alpha)));
        }
    }

    /** Широкая тёмная кнопка "Выйти" с красным крестиком слева. */
    private void drawExitButton(MatrixStack stack, float x, float y, float w, float h,
                                String label, double hover, float alpha) {
        Render2D.drawRoundedRect(stack, x, y, w, h, RADIUS,
                withAlpha(GLASS_BTN_FILL, (int) (GLASS_BTN_FILL.getAlpha() * alpha)));
        Render2D.drawBorder(stack, x, y, w, h, RADIUS, 0f, 1f,
                new Color(255, 255, 255, (int) (28 * alpha)));
        if (hover > 0.01) {
            Render2D.drawRoundedRect(stack, x, y, w, h, RADIUS,
                    withAlpha(EXIT_RED, (int) (30 * hover * alpha)));
            // красное подчёркивание при наведении
            float uw = (w - 16f) * (float) hover;
            Render2D.drawRoundedRect(stack, x + 8f + (w - 16f - uw) / 2f, y + h - 2f, uw, 1.4f, 0.7f,
                    withAlpha(EXIT_RED, (int) (185 * hover * alpha)));
        }

        float crossSz = Math.min(14f, h - 14f);
        float pad = 12f;
        float crossX = x + pad;
        float crossY = y + h / 2f - crossSz / 2f;
        drawCrossIcon(stack, crossX, crossY, crossSz, withAlpha(EXIT_RED, (int) (255 * alpha)));

        float sz = 7.5f;
        String text = label;
        float textW = Fonts.BOLD.getWidth(text, sz);
        // Текст крестика центрируется относительно всей кнопки (как на макет��).
        float textX = x + w / 2f - textW / 2f;
        Render2D.drawFont(stack, Fonts.BOLD.getFont(sz), text, textX, y + h / 2f - sz / 2f,
                new Color(255, 255, 255, (int) (255 * alpha)));
    }

    /** Рисует красный крестик 'X' двумя диагональными линиями (без зависимости от текстур). */
    private void drawCrossIcon(MatrixStack stack, float x, float y, float size, Color color) {
        float thickness = Math.max(1.5f, size * 0.14f);
        Render2D.drawLine(stack, x, y, x + size, y + size, thickness, color);
        Render2D.drawLine(stack, x + size, y, x, y + size, thickness, color);
    }

    private void drawAchievements(MatrixStack stack, float x, float y, int mouseX, int mouseY, float alpha) {
        lastAchievementHitboxes.clear();
        lastAchievementList.clear();
        lastAchX = x;
        lastAchY = y;

        List<Achievement> list = AchievementManager.getAchievements();
        if (list.isEmpty()) return;

        Render2D.drawFont(stack, Fonts.SEMIBOLD.getFont(4f), "ДОСТИЖЕНИЯ",
                x, y, withAlpha(MUTED_TEXT, (int) (200 * alpha)));

        float iconY = y + 8f;
        float curX = x;
        Achievement hoveredAchievement = null;

        for (Achievement a : list) {
            boolean hovered = mouseX >= curX && mouseX <= curX + ACH_ICON_SIZE
                    && mouseY >= iconY && mouseY <= iconY + ACH_ICON_SIZE;

            Render2D.drawRoundedRect(stack, curX, iconY, ACH_ICON_SIZE, ACH_ICON_SIZE, 4f,
                    new Color(24, 20, 8, (int) (215 * alpha)));
            Render2D.drawBorder(stack, curX, iconY, ACH_ICON_SIZE, ACH_ICON_SIZE, 4f, 0f, 1f,
                    withAlpha(hovered ? new Color(255, 200, 60) : new Color(255, 255, 255), (int) ((hovered ? 160 : 45) * alpha)));

            AbstractTexture icon = AchievementManager.getIcon(a);
            if (icon != null) {
                float pad = 2f;
                Render2D.drawTexture(stack, curX + pad, iconY + pad, ACH_ICON_SIZE - pad * 2, ACH_ICON_SIZE - pad * 2,
                        2f, icon, new Color(255, 255, 255, (int) (255 * alpha)));
            } else {
                float sz = 8f;
                String fallback = "🏆";
                float fw = Fonts.BOLD.getWidth(fallback, sz);
                Render2D.drawFont(stack, Fonts.BOLD.getFont(sz), fallback,
                        curX + ACH_ICON_SIZE / 2f - fw / 2f, iconY + ACH_ICON_SIZE / 2f - sz / 2f,
                        new Color(255, 210, 90, (int) (230 * alpha)));
            }

            lastAchievementHitboxes.add(new float[]{curX, iconY, ACH_ICON_SIZE, ACH_ICON_SIZE});
            lastAchievementList.add(a);

            if (hovered) hoveredAchievement = a;
            curX += ACH_ICON_SIZE + ACH_ICON_GAP;
        }

        if (hoveredAchievement != null) {
            drawAchievementTooltip(stack, hoveredAchievement, mouseX, mouseY, alpha);
        }
    }

    private void drawAchievementTooltip(MatrixStack stack, Achievement a, int mouseX, int mouseY, float alpha) {
        float titleSz = 6f, bodySz = 5f;
        String title = a.name;
        String desc = a.description == null ? "" : a.description;
        String unlock = (a.unlockFeature == null || a.unlockFeature.isEmpty())
                ? null : "Разблокирует: " + a.unlockFeature;

        float maxTextW = Math.max(Fonts.BOLD.getWidth(title, titleSz), Fonts.REGULAR.getWidth(desc, bodySz));
        if (unlock != null) maxTextW = Math.max(maxTextW, Fonts.SEMIBOLD.getWidth(unlock, bodySz));

        float boxW = maxTextW + 20f;
        float boxH = 14f + 12f + (unlock != null ? 12f : 0f);

        float boxX = mouseX + 14f;
        float boxY = mouseY + 14f;
        if (boxX + boxW > this.width) boxX = this.width - boxW - 6f;
        if (boxY + boxH > this.height) boxY = this.height - boxH - 6f;

        Render2D.drawRoundedRect(stack, boxX, boxY, boxW, boxH, 5f,
                new Color(14, 12, 18, (int) (245 * alpha)));
        Render2D.drawBorder(stack, boxX, boxY, boxW, boxH, 5f, 0f, 1f,
                new Color(255, 200, 60, (int) (120 * alpha)));

        float textX = boxX + 10f;
        float textY = boxY + 8f;
        Render2D.drawFont(stack, Fonts.BOLD.getFont(titleSz), title, textX, textY,
                new Color(255, 210, 90, (int) (255 * alpha)));
        textY += 12f;
        Render2D.drawFont(stack, Fonts.REGULAR.getFont(bodySz), desc, textX, textY,
                new Color(230, 230, 235, (int) (230 * alpha)));

        if (unlock != null) {
            textY += 12f;
            Render2D.drawFont(stack, Fonts.SEMIBOLD.getFont(bodySz), unlock, textX, textY,
                    new Color(120, 220, 140, (int) (240 * alpha)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UTILS
    // ═══════════════════════════════════════════════════════════════════════════

    private static boolean isHovered(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                Math.max(0, Math.min(255, alpha)));
    }

    private static Color lighten(Color c, float amount) {
        int r = (int) Math.min(255, c.getRed()   + (255 - c.getRed())   * amount);
        int g = (int) Math.min(255, c.getGreen() + (255 - c.getGreen()) * amount);
        int b = (int) Math.min(255, c.getBlue()  + (255 - c.getBlue())  * amount);
        return new Color(r, g, b, c.getAlpha());
    }

    private static Color darken(Color c, float amount) {
        int r = (int) Math.max(0, c.getRed()   * (1f - amount));
        int g = (int) Math.max(0, c.getGreen() * (1f - amount));
        int b = (int) Math.max(0, c.getBlue()  * (1f - amount));
        return new Color(r, g, b, c.getAlpha());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ANIMATED BUTTON (утилитарный класс — оставлен для совместимости с другими
    //  экранами меню, использующими этот же паттерн кнопок)
    // ═══════════════════════════════════════════════════════════════════════════
    public static class AnimatedButton {
        private final float x, y, width, height;
        private final String message;
        private final PressAction action;
        private final Animation hoverAnimation;

        public AnimatedButton(float x, float y, float width, float height, String message, PressAction action) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.message = message;
            this.action = action;
            this.hoverAnimation = new Animation(200L, 1.0, false, Easing.EASE_OUT_CUBIC);
        }

        public void resetAnimations() {
            hoverAnimation.reset();
        }

        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta, float alpha) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            hoverAnimation.update(hovered);

            float hoverProgress = (float) hoverAnimation.getValue();
            MatrixStack stack = drawContext.getMatrices();

            Render2D.drawRoundedRect(stack, x, y, width, height, 8f,
                    new Color(50, 50, 60, clamp255((int) (alpha * 150 + hoverProgress * 100))));

            Color base  = new Color(220, 220, 240);
            Color hover = new Color(255, 255, 255);
            int r = (int) (base.getRed()   + (hover.getRed()   - base.getRed())   * hoverProgress);
            int g = (int) (base.getGreen() + (hover.getGreen() - base.getGreen()) * hoverProgress);
            int b = (int) (base.getBlue()  + (hover.getBlue()  - base.getBlue())  * hoverProgress);

            Color textColor = new Color(r, g, b, clamp255((int) (alpha * 255)));
            float sz = 7f;
            float textX = x + width  / 2f - Fonts.REGULAR.getWidth(message, sz) / 2f;
            float textY = y + height / 2f - sz / 2f;

            Render2D.drawFont(stack, Fonts.REGULAR.getFont(sz), message, textX, textY, textColor);
        }

        public boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        public void onPress() {
            action.onPress(this);
        }

        private static int clamp255(int v) {
            return Math.max(0, Math.min(255, v));
        }
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(AnimatedButton button);
    }
}
