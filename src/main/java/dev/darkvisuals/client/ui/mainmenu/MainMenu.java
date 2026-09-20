package dev.darkvisuals.client.ui.mainmenu;

import dev.darkvisuals.darkvisuals;
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
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Главное меню Dark Visuals Recode — верстка по макету:
 *
 *  ┌──────────────────────────────────────────────┐
 *  │  ⌃  Dark Visuals Recode                       │
 *  │                                                │
 *  │  [ Сетевая игра ]      [ Одиночная Игра ]      │
 *  │  [ Аккаунты    ]       [ Настройки     ]       │
 *  │  [           Выйти                     ]       │
 *  └──────────────────────────────────────────────┘
 *
 * Фон: обычный тёмный + фиолетовое солнце с лучами строго позади интерфейса.
 * Плашка и кнопки: «жидкое стекло» — прозрачный фон с настоящим
 * размытием того, что находится позади (drawShaderBlurRect).
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

    // ── Цвета (тёмный + фиолетовый) ───────────────────────────────────────────
    private static final Color BG_BASE        = new Color(12, 9, 19);        // обычный тёмный фон
    private static final Color PANEL_TINT     = new Color(12, 9, 20, 78);    // тёмное стекло панели
    private static final Color GLASS_DARK     = new Color(18, 13, 28, 52);   // тёмное стекло кнопок
    private static final Color GLASS_PURPLE   = new Color(153, 0, 255, 90);  // фиолетовое стекло
    private static final Color GLASS_BTN_TEXT = new Color(235, 235, 240, 255);
    private static final Color MUTED_TEXT     = new Color(200, 200, 210, 210);
    private static final Color ACCENT_PURPLE  = new Color(0x99, 0x00, 0xFF);
    private static final Color ACCENT_SOFT    = new Color(0xB8, 0x64, 0xFF);

    // ── Солнце (строго на фоне, позади плашки и кнопок) ──────────────────────
    private static final float SUN_CORE_R     = 48f;   // радиус ядра
    private static final int   SUN_RAYS        = 14;   // лучей (чередуются длинные/короткие)
    private static final float SUN_RAY_LONG    = 300f;
    private static final float SUN_RAY_SHORT   = 180f;
    private static final float SUN_SPIN_SPEED  = 0.055f; // рад/с — медленное вращение лучей

    // ── Заголовок: переливается между тёмным фиолетовым и тёмно-серым ────────
    private static final Color TITLE_PURPLE = new Color(122, 44, 172);
    private static final Color TITLE_GRAY   = new Color(96, 96, 106);
    private static final float SHIMMER_PERIOD = 1600f; // мс на полный цикл перелива

    private static final String TITLE   = "Dark Visuals Recode";
    private static final String VERSION = "darkvisuals recode v1.0";

    // Кэш реально отрисованной раскладки — клики всегда совпадают с картинкой.
    private float lastPanelX, lastPanelY, lastPanelW, lastPanelH;
    private float lastLogoX, lastLogoY;
    private float lastY1, lastY2, lastY3;
    private float lastBtnH = BTN_H;
    private float lastColX1, lastColX2, lastColW1, lastColW2;
    private float lastExitX, lastExitY, lastExitW, lastExitH;
    private float lastAchX, lastAchY;
    private final List<float[]> lastAchievementHitboxes = new ArrayList<>();
    private final List<Achievement> lastAchievementList = new ArrayList<>();
    private static final float ACH_ICON_SIZE = 18f;
    private static final float ACH_ICON_GAP  = 6f;

    public MainMenu() {
        super(Text.of(TITLE));
    }

    @Override
    protected void init() {
        super.init();
        entrance = new Animation(500L, 1.0, true, Easing.EASE_OUT_CUBIC);
        shownAtMs = System.currentTimeMillis();
        loadCustomLogoIfNeeded();
        AchievementManager.loadIfNeeded();
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
                    String msg = "Dark Visuals Recode: загрузка шрифтов…";
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

    private void renderMenu(DrawContext context, int mouseX, int mouseY, float delta) {
        float t = (float) entrance.getValue();

        MatrixStack stack = context.getMatrices();

        int W = this.width, H = this.height;

        // ── Фон: обычный тёмный + виньетка + фиолетовое солнце ─────────────────────
        Render2D.drawRect(stack, 0, 0, W, H, BG_BASE);

        Render2D.drawGradientRect(stack, 0, 0, W, H * 0.30f,
                new Color(0, 0, 0, 120), new Color(0, 0, 0, 0), false);
        Render2D.drawGradientRect(stack, 0, H * 0.70f, W, H * 0.30f,
                new Color(0, 0, 0, 0), new Color(0, 0, 0, 150), false);

        drawSun(stack, W, H, t);

        // Стартовый GIF-интро, если он сейчас воспроизводится (поверх солнца).
        Identifier introFrame = dev.darkvisuals.client.util.IntroManager.currentFrame();
        if (introFrame != null) {
            Render2D.drawTexture(stack, 0, 0, W, H, 0f, introFrame, Color.WHITE);
        }

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

        // мягкое фиолетовое свечение, растекающееся вокруг панели
        Render2D.drawBlurredRect(stack, panelX - 6f, panelY - 6f, panelW + 12f, contentH + 12f,
                PANEL_RADIUS + 6f, 16f, withAlpha(ACCENT_PURPLE, (int) (42f * t)));

        // плашка — тёмное «жидкое стекло»: прозрачная, с размытием фона за ней
        drawGlass(stack, panelX, panelY, panelW, contentH, PANEL_RADIUS, 14f, t, PANEL_TINT);

        // пульсирующая акцентная линия под верхним краем панели
        float accentPulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 720.0);
        Render2D.drawRoundedRect(stack, panelX + 28f, panelY + 1f, panelW - 56f, 1.5f, 1f,
                withAlpha(ACCENT_SOFT, (int) (95f * t * accentPulse)));

        this.lastPanelX = panelX; this.lastPanelY = panelY;
        this.lastPanelW = panelW; this.lastPanelH = contentH;

        float innerX = panelX + PANEL_PAD_X;
        float innerW = panelW - PANEL_PAD_X * 2f;

        // ── Шапка: логотип-галочка + "Dark Visuals Recode" — по центру панели ────
        float headerY = panelY + PANEL_PAD_TOP;

        float titleSz    = 20f;
        String titleText = TITLE;
        float titleW     = Fonts.BOLD.getWidth(titleText, titleSz);
        float logoTextGap = 14f;

        float headerBlockW = HEADER_LOGO_SIZE + logoTextGap + titleW;
        float headerBlockX = innerX + innerW / 2f - headerBlockW / 2f;

        float logoX = headerBlockX;
        // логотип мягко покачивается
        float logoBob = (float) Math.sin((System.currentTimeMillis() - shownAtMs) / 900.0) * 1.5f;
        drawHeaderLogo(stack, logoX, headerY + logoBob, HEADER_LOGO_SIZE, t);
        this.lastLogoX = logoX; this.lastLogoY = headerY;

        float titleX = logoX + HEADER_LOGO_SIZE + logoTextGap;
        float titleY = headerY + HEADER_LOGO_SIZE / 2f - Fonts.BOLD.getHeight(titleSz) / 2f;
        // заголовок переливается между тёмным фиолетовым и тёмно-серым
        float titlePulse = 0.85f + 0.15f * (float) Math.sin((System.currentTimeMillis() - shownAtMs) / 1100.0);
        float shimmer = 0.5f + 0.5f * (float) Math.sin((System.currentTimeMillis() - shownAtMs) / SHIMMER_PERIOD);
        Color titleColor = lerpColor(TITLE_PURPLE, TITLE_GRAY, shimmer);
        // мягкий блик, дышащий в такт переливу
        Render2D.drawFont(stack, Fonts.BOLD.getFont(titleSz), titleText,
                titleX + 0.6f, titleY + 0.6f,
                withAlpha(ACCENT_SOFT, (int) (60 * t * shimmer)));
        Render2D.drawFont(stack, Fonts.BOLD.getFont(titleSz), titleText,
                titleX, titleY, withAlpha(titleColor, (int) (235 * t * titlePulse)));
        Render2D.drawRoundedRect(stack, headerBlockX, headerY + HEADER_LOGO_SIZE + 7f, headerBlockW, 1.2f, 0.6f,
                withAlpha(titleColor, (int) (110 * t * titlePulse)));

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

        // Ряд 1 ──────────────────────────────────────────────────────────────────
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

        // Ряд 2 ──────────────────────────────────────────────────────────────────
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

        // Ряд 3 — "Выйти" на всю ширину ─────────────────────────────────────────
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
        drawAchievements(stack, 20f, 20f, mouseX, mouseY, t);

        // ── Версия ─────────────────────────────────────────────────────────────
        float verSz = 8f;
        float verW  = Fonts.REGULAR.getWidth(VERSION, verSz);
        float verT = entranceStep(System.currentTimeMillis() - shownAtMs, 650L);
        Render2D.drawFont(stack, Fonts.REGULAR.getFont(verSz), VERSION,
                W - verW - 10f, H - verSz - 10f,
                new Color(210, 210, 220, (int) (180 * t * verT)));
    }

    /**
     * Фиолетовое солнце в центре экрана — только элемент фона: рисуется
     * ДО плашки и кнопок, так что интерфейс всегда остаётся поверх.
     * Состоит из медленно вращающихся лучей, мягкого ореола и яркого ядра.
     */
    private void drawSun(MatrixStack stack, int W, int H, float alpha) {
        if (alpha <= 0.01f) return;

        float time = (System.currentTimeMillis() - shownAtMs) / 1000f;
        float cx = W / 2f, cy = H / 2f;
        float breathe = 1f + 0.03f * (float) Math.sin(time * 1.1f); // солнце «дышит»
        float coreR = SUN_CORE_R * breathe * (0.7f + 0.3f * alpha);
        int a = (int) (255f * alpha);

        // ── Лучи: чередующиеся длинные/короткие, медленно вращаются ─────────────
        for (int i = 0; i < SUN_RAYS; i++) {
            boolean longRay = (i % 2 == 0);
            float len    = longRay ? SUN_RAY_LONG : SUN_RAY_SHORT;
            float halfW  = longRay ? 4.5f : 3f;
            float rayA   = (longRay ? 62f : 44f) * alpha;

            stack.push();
            stack.translate(cx, cy, 0f);
            stack.multiply(RotationAxis.POSITIVE_Z.rotation(
                    time * SUN_SPIN_SPEED + i * (float) (Math.PI * 2.0 / SUN_RAYS)));

            // мягкое свечение луча
            Render2D.drawBlurredRect(stack, coreR * 0.7f, -halfW * 2.2f,
                    len, halfW * 4.4f, halfW * 2.2f, 7f,
                    new Color(153, 0, 255, (int) rayA));
            // ядро луча — ярче и у́же, выцветает к концу
            Render2D.drawRoundedRect(stack, coreR * 0.7f, -halfW, len, halfW * 2f, halfW,
                    new Color(196, 120, 255, (int) (rayA * 1.5f)));
            stack.pop();
        }

        // ── Ореол: широкое фиолетовое свечение вокруг ядра ───────────────────────
        float glowR = coreR * 3.6f;
        Render2D.drawBlurredRect(stack, cx - glowR, cy - glowR, glowR * 2f, glowR * 2f, glowR, 26f,
                new Color(153, 0, 255, (int) (85f * alpha)));

        // ── Ядро: от фиолетового края к горячему светлому центру ────────────────
        Render2D.drawRoundedRect(stack, cx - coreR, cy - coreR, coreR * 2f, coreR * 2f, coreR,
                new Color(153, 0, 255, (int) (225f * alpha)));
        float r2 = coreR * 0.72f;
        Render2D.drawRoundedRect(stack, cx - r2, cy - r2, r2 * 2f, r2 * 2f, r2,
                new Color(186, 90, 255, (int) (235f * alpha)));
        float r3 = coreR * 0.46f;
        Render2D.drawRoundedRect(stack, cx - r3, cy - r3, r3 * 2f, r3 * 2f, r3,
                new Color(224, 168, 255, (int) (245f * alpha)));
        float r4 = coreR * 0.22f;
        Render2D.drawRoundedRect(stack, cx - r4, cy - r4, r4 * 2f, r4 * 2f, r4,
                new Color(255, 240, 255, a));
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

    /**
     * «Жидкое стекло»: настоящее размытие того, что находится позади
     * прямоугольника (shader blur), полупрозрачный тинт и стеклянные кромки.
     * Сам фон элемента остаётся прозрачным — видно размытый фон за ним.
     *
     * @param fade  0..1 — прозрачность/интенсивность стекла (для анимаций)
     * @param tint  цвет и альфа стеклянной заливки
     */
    private void drawGlass(MatrixStack stack, float x, float y, float w, float h,
                           float radius, float blur, float fade, Color tint) {
        drawGlass(stack, x, y, w, h, radius, blur, fade, tint, true);
    }

    /**
     * @param sheen рисовать ли стеклянные блики/свечение (верхняя линия,
     *             вертикальный градиент, белая кромка). Для кнопок — false,
     *             чтобы они были матовыми и без свечения.
     */
    private void drawGlass(MatrixStack stack, float x, float y, float w, float h,
                           float radius, float blur, float fade, Color tint, boolean sheen) {
        if (fade <= 0.01f) return;

        // размытие того, что позади элемента
        Render2D.drawShaderBlurRect(stack, x, y, w, h, radius, blur * fade,
                new Color(255, 255, 255, (int) (255f * fade)));

        // полупрозрачная стеклянная заливка (не перекрывает фон, только тонирует)
        Render2D.drawRoundedRect(stack, x, y, w, h, radius,
                withAlpha(tint, (int) (tint.getAlpha() * fade)));

        if (!sheen) {
            // для кнопок — только тихая кромка цветом тинта, без свечения
            Render2D.drawBorder(stack, x, y, w, h, radius, 0f, 1f,
                    withAlpha(tint, (int) (110f * fade)));
            return;
        }

        // вертикальный блик стекла
        Render2D.drawGradientRect(stack, x, y + radius * 0.5f, w, h - radius,
                new Color(255, 255, 255, (int) (26f * fade)),
                new Color(Math.max(0, tint.getRed() - 25), Math.max(0, tint.getGreen() - 25),
                        Math.max(0, tint.getBlue() - 25), (int) (50f * fade)),
                false);

        // внутренняя белая кромка
        Render2D.drawBorder(stack, x + 0.5f, y + 0.5f, w - 1f, h - 1f, radius - 0.5f, 2.5f, 0f,
                new Color(255, 255, 255, (int) (34f * fade)));

        // внешняя кромка цветом тинта
        Render2D.drawBorder(stack, x, y, w, h, radius, 0f, 1f,
                withAlpha(tint, (int) (110f * fade)));

        // верхняя световая линия
        Render2D.drawGradientRect(stack, x + radius, y + 0.5f, w - radius * 2f, 1f,
                new Color(255, 255, 255, (int) (140f * fade)),
                new Color(255, 255, 255, (int) (20f * fade)),
                true);
    }

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
     * Фиолетовое «жидкое стекло». iconLeft = true — иконка слева от текста,
     * иначе иконка справа (не используется здесь, оставлено для единообразия
     * сигнатуры с drawGlassButton).
     */
    private void drawFilledButton(MatrixStack stack, float x, float y, float w, float h,
                                  String label, Identifier icon, Color accent, double hover, float alpha,
                                  boolean iconLeft) {
        drawGlass(stack, x, y, w, h, RADIUS, 6f, alpha, GLASS_PURPLE, false);
        // подчёркивание, «проезжающее» при наведении
        if (hover > 0.01) {
            Render2D.drawRoundedRect(stack, x, y, w, h, RADIUS,
                    withAlpha(lighten(accent, 0.3f), (int) (40 * hover * alpha)));
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
        drawGlass(stack, x, y, w, h, RADIUS, 6f, alpha, GLASS_DARK, false);
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

    /** Широкая тёмная кнопка "Выйти" с фиолетовым крестиком слева. */
    private void drawExitButton(MatrixStack stack, float x, float y, float w, float h,
                                String label, double hover, float alpha) {
        drawGlass(stack, x, y, w, h, RADIUS, 6f, alpha, GLASS_DARK, false);
        if (hover > 0.01) {
            Render2D.drawRoundedRect(stack, x, y, w, h, RADIUS,
                    withAlpha(ACCENT_PURPLE, (int) (40 * hover * alpha)));
            // фиолетовое подчёркивание при наведении
            float uw = (w - 16f) * (float) hover;
            Render2D.drawRoundedRect(stack, x + 8f + (w - 16f - uw) / 2f, y + h - 2f, uw, 1.4f, 0.7f,
                    withAlpha(ACCENT_SOFT, (int) (185 * hover * alpha)));
        }

        float crossSz = Math.min(14f, h - 14f);
        float pad = 12f;
        float crossX = x + pad;
        float crossY = y + h / 2f - crossSz / 2f;
        drawCrossIcon(stack, crossX, crossY, crossSz, withAlpha(ACCENT_SOFT, (int) (255 * alpha)));

        float sz = 7.5f;
        String text = label;
        float textW = Fonts.BOLD.getWidth(text, sz);
        // Текст крестика центрируется относительно всей кнопки (как на макете).
        float textX = x + w / 2f - textW / 2f;
        Render2D.drawFont(stack, Fonts.BOLD.getFont(sz), text, textX, y + h / 2f - sz / 2f,
                new Color(255, 255, 255, (int) (255 * alpha)));
    }

    /** Рисует фиолетовый крестик 'X' двумя диагональными линиями (без зависимости от текстур). */
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
                    new Color(20, 14, 32, (int) (215 * alpha)));
            Render2D.drawBorder(stack, curX, iconY, ACH_ICON_SIZE, ACH_ICON_SIZE, 4f, 0f, 1f,
                    withAlpha(hovered ? ACCENT_SOFT : new Color(255, 255, 255),
                            (int) ((hovered ? 170 : 45) * alpha)));

            AbstractTexture icon = AchievementManager.getIcon(a);
            if (icon != null) {
                float pad = 2f;
                Render2D.drawTexture(stack, curX + pad, iconY + pad, ACH_ICON_SIZE - pad * 2, ACH_ICON_SIZE - pad * 2,
                        2f, icon, new Color(255, 255, 255, (int) (255 * alpha)));
            } else {
                float sz = 8f;
                String fallback = "★";
                float fw = Fonts.BOLD.getWidth(fallback, sz);
                Render2D.drawFont(stack, Fonts.BOLD.getFont(sz), fallback,
                        curX + ACH_ICON_SIZE / 2f - fw / 2f, iconY + ACH_ICON_SIZE / 2f - sz / 2f,
                        new Color(186, 110, 255, (int) (230 * alpha)));
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

    private void drawAchievementTooltip(MatrixStack stack, Achievement a, float mouseX, float mouseY, float alpha) {
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

        // стеклянный тултип с размытием фона
        drawGlass(stack, boxX, boxY, boxW, boxH, 5f, 5f, alpha, new Color(16, 11, 26, 235));

        float textX = boxX + 10f;
        float textY = boxY + 8f;
        Render2D.drawFont(stack, Fonts.BOLD.getFont(titleSz), title, textX, textY,
                new Color(200, 140, 255, (int) (255 * alpha)));
        textY += 12f;
        Render2D.drawFont(stack, Fonts.REGULAR.getFont(bodySz), desc, textX, textY,
                new Color(230, 230, 235, (int) (230 * alpha)));

        if (unlock != null) {
            textY += 12f;
            Render2D.drawFont(stack, Fonts.SEMIBOLD.getFont(bodySz), unlock, textX, textY,
                    new Color(150, 220, 170, (int) (240 * alpha)));
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

    /** Линейная интерполяция цвета — для перелива заголовка. */
    private static Color lerpColor(Color a, Color b, float t) {
        int r = (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl, 255);
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
