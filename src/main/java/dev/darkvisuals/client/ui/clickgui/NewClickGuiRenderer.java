package dev.darkvisuals.client.ui.clickgui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.util.animations.SmoothAnimation;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Font;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.modules.settings.impl.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.Window;
import net.minecraft.sound.SoundEvent;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Стиль «New» — компактный чёрный фрейм с сильным скруглением,
 * карточки модулей с описанием-тултипом, pill-навбар из 6 иконок
 * и строка поиска под фреймом.
 */
public class NewClickGuiRenderer {

    // ─── Главный фрейм ────────────────────────────────────────────────
    public static final float FRAME_W  = 460f;
    public static final float FRAME_H  = 320f;
    public static final float FRAME_R  = 30f;   // сильное скругление

    // ─── Шапка ───────────────────────────────────────────────────────
    private static final float HEADER_H = 38f;

    // ─── Карточки модулей (сетка из двух колонок) ──────────────────────
    private static final int   GRID_COLS    =  2;
    private static final float CARD_MARGIN  = 14f;  // отступ от боковых краёв фрейма
    private static final float CARD_GAP     =  8f;
    private static final float CARD_W       = (FRAME_W - CARD_MARGIN * 2f - CARD_GAP * (GRID_COLS - 1)) / GRID_COLS;
    private static final float CARD_H       = 64f;  // высота карточки-заголовка модуля (всегда фиксирована)
    private static final float CARD_R       = 12f;
    private static final float CARD_LIST_TOP_PAD = 8f;
    private static final float EXPAND_GAP      =  6f;  // отступ между карточкой и панелью настроек
    private static final float SUB_PAD_TOP     = 11f;
    private static final float SUB_PAD_BOT     = 11f;
    private static final float SUB_PAD_X       = 12f;

    // ─── Навбар: единая вытянутая по горизонтали плашка со скруглёнными
    //     углами, внутри которой ряд из 6 иконок.
    private static final int   NAV_COUNT    =  6;
    private static final float NAV_PAD      =  4f;  // внутренний отступ плашки
    private static final float NAV_SLOT_W   = 42f;  // широкие слоты — плашка вытянута по X
    private static final float NAV_H        = 24f;  // низкая плашка
    private static final float NAV_PILL_R   = 16f;
    private static final float NAV_ICON_R   =  7f;  // скругление активного squircle
    private static final float NAV_ICON     = 12f;  // размер PNG-иконки
    private static final float NAV_BOTTOM   = 14f;

    // ─── Боковые квадратные кнопки навбара (искры слева, сетка справа от пилюли) ─
    private static final float NAV_SIDE_GAP = 8f;   // отступ между боковой кнопкой и пилюлей
    private static final float NAV_SIDE_R   = 12f;  // скругление боковой кнопки (совпадает с высотой пилюли)

    // ─── Строка поиска ────────────────────────────────────────────────
    private static final float SEARCH_W     = 180f;
    private static final float SEARCH_H     = 28f;
    private static final float SEARCH_R     = 14f;
    private static final float SEARCH_GAP   = 10f;

    // ─── Строки ввода (Friends/Markers/Config) ─────────────────────────
    private static final float INPUT_H      = 22f;
    private static final float INPUT_R      =  9f;
    private static final float BTN_W        = 40f;
    private static final float LIST_ROW_H   = 26f;
    private static final float LIST_ROW_GAP =  6f;
    private static final float ROWSET_GAP   = 10f;

    // ─── PNG-иконки навбара (те же текстуры, что в ClickGuiRenderer) ───
    private static final Identifier ICON_RENDER  = Identifier.of("darkvisuals", "textures/clickgui/render.png");
    private static final Identifier ICON_UTILITY = Identifier.of("darkvisuals", "textures/clickgui/utilities.png");
    private static final Identifier ICON_THEME   = Identifier.of("darkvisuals", "hud/theme.png");
    private static final Identifier ICON_CONFIG  = Identifier.of("darkvisuals", "hud/cloud.png");
    private static final Identifier ICON_FRIENDS = Identifier.of("darkvisuals", "hud/friends.png");
    private static final Identifier ICON_MARKERS = Identifier.of("darkvisuals", "textures/tags.png");

    // ─── Настройки ───────────────────────────────────────────────────
    private static final float SET_R        =  5f;
    private static final float SET_GAP      =  5f;

    // ─── Цвета ───────────────────────────────────────────────────────
    private static final int C_BG       = 0xFF161619;   // #161619 — фон главного окна
    private static final int C_CARD     = 0xFF232328;   // #232328 — фон карточки
    private static final int C_CARD_HOV = 0xFF26262B;   // #26262b — ховер карточки
    private static final int C_NAV      = 0xFF232328;
    private static final int C_LINE     = 0x12FFFFFF;   // white/[0.07]
    private static final int C_DIV      = 0x33FFFFFF;
    private static final int C_BORDER   = 0x1AFFFFFF;   // white/10 — обводка фрейма и карточек
    private static final int C_TEXT     = 0xFFFFFFFF;
    private static final int C_TEXT_DIM = 0xFF8E8E95;   // #8e8e95
    private static final int C_ICON_DIM = 0xFF8E8E93;

    /** #7c3aed — активная вкладка навбара. */
    private static final int C_ACCENT   = 0xFF7C3AED;
    /** #8b5cf6 — бегунок тогла со свечением. */
    private static final int C_THUMB    = 0xFF8B5CF6;

    // ─── Категории в навбаре ──────────────────────────────────────────
    public static final Category[] NAV_CATS = {
        Category.Render, Category.Utility, Category.Friends,
        Category.Markers, Category.Theme, Category.Config
    };

    // ─── Анимации ────────────────────────────────────────────────────
    private final Map<String, SmoothAnimation> anims = new HashMap<>();
    private final SmoothAnimation openFade = new SmoothAnimation(0f, 10f);

    // Моменты последних событий — для пружинящих/затухающих импульсов
    private final Map<String, Long> cardPressTimes = new HashMap<>();
    private final Map<String, Long> toggleTimes    = new HashMap<>();

    // Модуль, на карточке которого сейчас курсор (для тултипа)
    private Module hoveredModule;

    // Имя модуля, чья карточка сейчас развёрнута (показывает дочерние тоглы), как на референсе
    private String expandedModule;

    /** Режим правой части: список модулей / чат DARKA / настройки. */
    public enum ViewMode { MODULES, DARKA, SETTINGS }
    private ViewMode viewMode = ViewMode.MODULES;

    /** Открыто ли всплывающее окно инструкции Hugging Face. */
    private boolean hfWindowOpen;

    // ── Ввод чата DARKA-0.1 ─────────────────────────────────────────────
    private final StringBuilder darkaInput = new StringBuilder();
    private boolean darkaInputFocused;
    private float darkaScroll = 0f;

    // Плавный скролл: значение, смягчённое анимацией на текущем кадре (кэш —
    // считается один раз за кадр в рендере и переиспользуется в клик-тестах)
    private float frameScroll = 0f;

    // ─── Singleton ───────────────────────────────────────────────────
    private static NewClickGuiRenderer instance;
    public static NewClickGuiRenderer getInstance() {
        if (instance == null) instance = new NewClickGuiRenderer();
        return instance;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Координатные хелперы
    // ═════════════════════════════════════════════════════════════════

    public static float frameX(Window w) { return (w.getScaledWidth()  - FRAME_W) / 2f; }
    public static float frameY(Window w) { return (w.getScaledHeight() - FRAME_H) / 2f; }

    /** X левой колонки карточек */
    private static float cardX(float fx) { return fx + CARD_MARGIN; }

    /** X колонки col (0..GRID_COLS-1) */
    private static float cardX(float fx, int col) {
        return fx + CARD_MARGIN + col * (CARD_W + CARD_GAP);
    }

    /** Высота одной строки сетки (все карточки-заголовки фиксированной высоты) */
    private static float gridRowH() { return CARD_H + CARD_GAP; }

    /** Высота карточки настроек — рамка + сумма высот всех видимых настроек. */
    private float expandPanelContentH(Module m) {
        float h = SUB_PAD_TOP + SUB_PAD_BOT;
        for (Setting<?> s : m.getSettings()) {
            if (!s.isVisible()) continue;
            h += settingHeight(s) + SET_GAP;
        }
        return Math.max(h - SET_GAP, SUB_PAD_TOP + SUB_PAD_BOT);
    }

    /** Индекс развёрнутого модуля в текущем видимом списке, либо -1. */
    private int indexOfExpanded(List<Module> mods) {
        if (expandedModule == null) return -1;
        for (int i = 0; i < mods.size(); i++)
            if (mods.get(i).getName().equals(expandedModule)) return i;
        return -1;
    }

    /** Верхняя граница прокручиваемой зоны модулей */
    private static float listTop(float fy) { return fy + HEADER_H + 2f; }

    /** Высота плашки навбара */
    private static float navPillH() { return NAV_H + NAV_PAD * 2f; }

    /** Нижняя граница прокручиваемой зоны (выше навбара) */
    private static float listBottom(float fy) {
        return fy + FRAME_H - NAV_BOTTOM - navPillH() - 4f;
    }

    /** Верхний Y плашки навбара */
    private static float navPillY(float fy) {
        return fy + FRAME_H - NAV_BOTTOM - navPillH();
    }

    public static boolean hit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static long now() { return System.currentTimeMillis(); }

    // ── Звук вкл/выкл функции в клик-гуй ───────────────────────────────

    private static final Map<String, SoundEvent> SOUND_CACHE = new HashMap<>();

    private void playToggleSound(boolean enabled) {
        var ui = dev.darkvisuals.darkvisuals.getInstance().getModuleManager()
                .getModule(dev.darkvisuals.modules.impl.render.UI.class);
        if (ui == null || !ui.isGuiSound()) return;

        String id = enabled ? "darkvisuals:clickgui_enable" : "darkvisuals:clickgui_disable";
        try {
            SoundEvent sound = SOUND_CACHE.computeIfAbsent(id, i -> SoundEvent.of(Identifier.of(i)));
            MinecraftClient.getInstance().getSoundManager()
                    .play(PositionedSoundInstance.master(sound, 1.0f, 0.7f));
        } catch (Throwable ignored) {
            // Звука ещё нет в ресурсах — тишина, без вылетов
        }
    }

    private static float pulse(long t, long period) {
        return (float) (Math.sin(t * Math.PI * 2d / period) + 1d) / 2f;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Главный рендер
    // ═════════════════════════════════════════════════════════════════

    public void render(DrawContext context, int mouseX, int mouseY, float delta,
                       Window window, ClickGuiState state, float openProgress) {
        if (window == null) return;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        openProgress = Math.max(0f, Math.min(1f, openProgress));
        if (openProgress <= 0.01f) return;

        openFade.setTarget(openProgress);
        float fade = openFade.update();

        float sw = window.getScaledWidth(), sh = window.getScaledHeight();
        float fx = frameX(window), fy = frameY(window);

        // Затемнение фона
        rect(context, 0, 0, sw, sh, 0f, withAlpha(0xFF000000, (int)(160f * fade)));

        // Лёгкий scale-in с пружинным овершутом при открытии
        float pop = aval("openpop", openProgress, 8f);
        float scale = 0.94f + 0.06f * easeOutBack(pop);

        var ms = context.getMatrices();
        ms.push();
        ms.translate(sw / 2f, sh / 2f, 0f);
        ms.scale(scale, scale, 1f);
        ms.translate(-sw / 2f, -sh / 2f, 0f);

        // Мягкое свечение по краю фрейма, «дышащее» поверх открытого GUI
        float glow = aval("frglow", pulse(now(), 2000L) * openProgress, 6f);
        if (glow > 0.02f) {
            rect(context, fx - 3f, fy - 3f, FRAME_W + 6f, FRAME_H + 6f, FRAME_R + 3f,
                    themeA((int)(26f * fade * glow)));
        }

        // ── Главный фрейм: обводка снизу, заливка сверху — видна только тонкая рамка ──
        rect(context, fx - 1f, fy - 1f, FRAME_W + 2f, FRAME_H + 2f, FRAME_R + 1f,
                withAlpha(C_BORDER, (int)(255f * fade)));
        rect(context, fx, fy, FRAME_W, FRAME_H, FRAME_R, withAlpha(C_BG, (int)(255f * fade)));

        // ── Шапка: название категории по центру + анимированное подчёркивание ─
        renderHeader(context, fx, fy, fade);

        // ── Тонкий разделитель под шапкой ──────────────────────────
        rect(context, fx + FRAME_R, fy + HEADER_H - 1f,
                FRAME_W - FRAME_R * 2f, 1f, 0f, withAlpha(C_LINE, (int)(255f * fade)));

        // ── Список модулей с раскрывающимися карточками настроек ────
        renderModuleList(context, fx, fy, mouseX, mouseY, state, fade);

        // ── Тултип с описанием модуля (поверх списка, не обрезается ножницами)
        renderTooltip(context, fx, fy, fade);

        // ── Скроллбар ─────────────────────────────────────────────
        renderScrollBar(context, fx, fy, state, fade);

        // ── Навбар ─────────────────────────────────────────────────
        renderNavBar(context, fx, fy, mouseX, mouseY, fade);

        // ── Строка поиска (ниже фрейма) ────────────────────────────
        renderSearchBar(context, fx, fy, fade);

        ms.pop();
    }

    // ═════════════════════════════════════════════════════════════════
    //  Шапка
    // ═════════════════════════════════════════════════════════════════

    private static final float HEADER_TEXT_SIZE = 11f;

    private void renderHeader(DrawContext ctx, float fx, float fy, float fade) {
        String label = catLabel(ClickGuiRenderer.currentCategory);
        float lw = Fonts.SEMIBOLD.getWidth(label, HEADER_TEXT_SIZE);
        float tx = fx + (FRAME_W - lw) / 2f;
        float ty = fy + (HEADER_H - HEADER_TEXT_SIZE) / 2f + 1f;
        text(ctx, Fonts.SEMIBOLD, label, tx, ty, withAlpha(C_TEXT, (int)(255f * fade)), HEADER_TEXT_SIZE);

        // Анимированное подчёркивание: вырастает от центра при смене категории
        float uT = aval("headerunderline", 1f, 12f);
        if (uT > 0.02f) {
            float uw = lw * uT;
            rect(ctx, fx + (FRAME_W - uw) / 2f, fy + HEADER_H - 5f, uw, 1.5f, 0.75f,
                    themeA((int)(220f * fade * uT)));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Тултип с описанием модуля
    // ═════════════════════════════════════════════════════════════════

    private void renderTooltip(DrawContext ctx, float fx, float fy, float fade) {
        boolean show = hoveredModule != null;
        float tT = aval("tooltip", show ? 1f : 0f, 12f);
        float sT = aval("tooltipslide", show ? 1f : 0f, 12f);
        hoveredModule = null;
        if (tT <= 0.02f) return;

        String desc = hoveredDesc;
        if (desc == null || desc.isBlank()) return;

        float maxW = FRAME_W - 20f;
        List<String> lines = wrap(desc, maxW, 8.5f);
        float y = listTop(fy) + 2f - (1f - sT) * 8f;
        for (String line : lines) {
            float lw = Fonts.MEDIUM.getWidth(line, 8.5f);
            text(ctx, Fonts.MEDIUM, line, fx + (FRAME_W - lw) / 2f, y,
                    withAlpha(C_TEXT, (int)(235f * fade * tT)), 8.5f);
            y += 10.5f;
        }
    }

    private String hoveredDesc;

    private List<String> wrap(String s, float maxW, float size) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
            if (cur.length() == 0) cur.append(word);
            else if (Fonts.MEDIUM.getWidth(cur + " " + word, size) <= maxW) cur.append(' ').append(word);
            else { out.add(cur.toString()); cur.setLength(0); cur.append(word); }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Скроллбар
    // ═════════════════════════════════════════════════════════════════

    private void renderScrollBar(DrawContext ctx, float fx, float fy, ClickGuiState state, float fade) {
        Category cat = ClickGuiRenderer.currentCategory;
        float max = maxScroll(state, cat);
        float sT = aval("scrollfade", max > 0.01f ? 1f : 0f, 8f);
        if (sT <= 0.02f) return;

        float cur = frameScroll;
        float lTop = listTop(fy), lBot = listBottom(fy);
        float trackH = lBot - lTop;
        float content = max + trackH;
        float thumbH = Math.max(14f, trackH * trackH / content);
        float thumbY = lTop + (trackH - thumbH) * (content > 0 ? cur / content : 0f);

        rect(ctx, fx + FRAME_W - 6f, thumbY, 3f, thumbH, 1.5f,
                withAlpha(C_TEXT, (int)(70f * fade * sT)));
    }

    // ═════════════════════════════════════════════════════════════════
    //  Список модулей
    // ═════════════════════════════════════════════════════════════════

    private void renderModuleList(DrawContext ctx, float fx, float fy, int mx, int my,
                                  ClickGuiState state, float fade) {
        Category cat = ClickGuiRenderer.currentCategory;
        float cx    = cardX(fx);
        float cw    = CARD_W;
        float lTop  = listTop(fy);
        float lBot  = listBottom(fy);

        // Плавное появление содержимого при смене категории
        float catT = aval("catfade", 1f, 12f);

        // Плавный скролл: смягчаем целевое значение из state один раз за кадр
        frameScroll = aval("scroll:" + cat.name(), state.getScroll(cat), 16f);

        // ── Спец-режимы: чат DARKA-0.1 и настройки ──
        if (viewMode == ViewMode.DARKA) {
            renderDarkaChat(ctx, fx, fy, mx, my, fade);
            return;
        }
        if (viewMode == ViewMode.SETTINGS) {
            renderSettingsView(ctx, fx, fy, mx, my, fade);
            return;
        }

        // Не-модульные вкладки
        if (!ClickGuiRenderer.isModuleTab(cat)) {
            ctx.enableScissor((int)cx, (int)lTop, (int)(cx + cw), (int)lBot);
            float y = lTop + CARD_LIST_TOP_PAD - frameScroll;
            if      (cat == Category.Theme)   renderThemes(ctx, cx, cw, y, fade);
            else if (cat == Category.Friends) renderFriends(ctx, cx, cw, y, fade);
            else if (cat == Category.Markers) renderMarkers(ctx, cx, cw, y, fade);
            else if (cat == Category.Config)  renderConfigs(ctx, cx, cw, y, fade);
            ctx.disableScissor();
            return;
        }

        List<Module> mods = ClickGuiRenderer.visibleModules(state);
        float scroll = frameScroll;
        int expIdx = indexOfExpanded(mods);

        ctx.enableScissor((int)cx, (int)lTop, (int)(cx + (CARD_W + CARD_GAP) * GRID_COLS - CARD_GAP), (int)lBot);

        int row = 0;
        for (Module m : mods) {
            int col = row % GRID_COLS, rIdx = row / GRID_COLS;
            float cardX = cardX(fx, col);
            float y = lTop + CARD_LIST_TOP_PAD - scroll + rIdx * gridRowH();
            row++;
            if (y + CARD_H < lTop) continue;
            if (y > lBot) break;

            boolean expanded = m.getName().equals(expandedModule);
            boolean hov = hit(mx, my, cardX, y, CARD_W, CARD_H);

            float togT = aval("tog:" + m.getName(), m.isToggled() ? 1f : 0f, 12f);
            float hovT = aval("hov:" + m.getName(), hov ? 1f : 0f, 10f);
            float expT = aval("exp:" + m.getName(), expanded ? 1f : 0f, 12f);
            float glowT = aval("cardglow:" + m.getName(), m.isToggled() ? 1f : 0f, 10f);
            float pressRaw = clamp01(1f - (now() - cardPressTimes.getOrDefault(m.getName(), 0L)) / 300f);
            float press = aval("cardpress:" + m.getName(), pressRaw, 16f);
            float ringRaw = clamp01(1f - (now() - toggleTimes.getOrDefault(m.getName(), 0L)) / 480f);
            float ringT = aval("togpulse:" + m.getName(), ringRaw, 12f);

            var ms = ctx.getMatrices();
            ms.push();
            ms.translate(cardX + CARD_W / 2f, y + CARD_H / 2f, 0f);
            float cs = 1f - 0.03f * press;
            ms.scale(cs, cs, 1f);
            ms.translate(-(cardX + CARD_W / 2f), -(y + CARD_H / 2f), 0f);

            if (glowT > 0.02f) {
                rect(ctx, cardX - 2f, y - 2f, CARD_W + 4f, CARD_H + 4f, CARD_R + 2f,
                        withAlpha(C_THUMB, (int)(30f * fade * glowT)));
            }

            // Фон карточки-заголовка (обводка снизу, чтобы заливка перекрыла внутреннюю часть)
            rect(ctx, cardX - 0.5f, y - 0.5f, CARD_W + 1f, CARD_H + 1f, CARD_R + 0.5f,
                    withAlpha(0x0DFFFFFF, (int)(255f * fade * catT)));
            rect(ctx, cardX, y, CARD_W, CARD_H, CARD_R,
                    lerpColor(withAlpha(C_CARD, (int)(255f * fade * catT)),
                              withAlpha(C_CARD_HOV, (int)(255f * fade * catT)), hovT));

            // Название — сверху слева
            String name = net.minecraft.client.resource.language.I18n.translate(m.getName());
            text(ctx, Fonts.BOLD, name, cardX + 14f, y + 11f,
                    withAlpha(C_TEXT, (int)(255f * fade * catT)), 11.5f);

            // Тонкая разделительная линия между названием и описанием
            rect(ctx, cardX + 12f, y + 34f, CARD_W - 24f, 1f, 0f,
                    withAlpha(C_LINE, (int)(220f * fade * catT)));

            // Описание — снизу слева, ближе к нижнему краю карточки
            String desc = net.minecraft.client.resource.language.I18n.translate(m.getDescription());
            if (desc != null && !desc.isBlank()) {
                String line = desc;
                while (Fonts.MEDIUM.getWidth(line, 8.5f) > CARD_W - 28f && line.length() > 1)
                    line = line.substring(0, line.length() - 1);
                text(ctx, Fonts.MEDIUM, line, cardX + 14f, y + CARD_H - 19f,
                        withAlpha(C_TEXT_DIM, (int)(220f * fade * catT)), 8.5f);
            }

            // Шеврон разворота — крайний правый верхний угол
            drawChevron(ctx, cardX + CARD_W - 18f, y + 15f, expT, fade * catT);

            // Кольцо-импульс вокруг тогла при переключении
            if (ringT > 0.02f) {
                float r = 8f + ringT * 16f;
                rect(ctx, cardX + CARD_W - 56f + 12f - r / 2f, y + 12f + 10f - r / 2f, r, r, r / 2f,
                        withAlpha(C_THUMB, (int)(180f * fade * (1f - ringT))));
            }

            // iOS-тогл справа сверху, левее шеврона
            drawToggle(ctx, cardX + CARD_W - 58f, y + 11f, togT, fade * catT);

            ms.pop();
        }

        // ── Карточка настроек развёрнутого модуля: такая же, как карточка функции,
        //    но размер строго под количество настроек ──
        if (expIdx >= 0) {
            Module em = mods.get(expIdx);
            float expFade = aval("exp:" + em.getName(), 1f, 12f);
            if (expFade > 0.02f) {
                int col = expIdx % GRID_COLS, rIdx = expIdx / GRID_COLS;
                float cardX = cardX(fx, col);
                float rowY = lTop + CARD_LIST_TOP_PAD - scroll + rIdx * gridRowH();
                float boxY = rowY + CARD_H + EXPAND_GAP;
                float boxH = expandPanelContentH(em);

                var ms = ctx.getMatrices();
                ms.push();
                ms.translate(0, (1f - expFade) * -6f, 0f);

                rect(ctx, cardX - 1f, boxY - 1f, CARD_W + 2f, boxH + 2f, CARD_R + 1f,
                        withAlpha(C_BORDER, (int)(255f * fade * catT * expFade)));
                rect(ctx, cardX, boxY, CARD_W, boxH, CARD_R,
                        lerpColor(withAlpha(C_CARD, (int)(255f * fade * catT * expFade)),
                                  withAlpha(C_CARD_HOV, (int)(255f * fade * catT * expFade)), 0.5f));

                float sw = CARD_W - SUB_PAD_X * 2f;
                float sy = boxY + SUB_PAD_TOP;
                boolean any = false;
                for (Setting<?> s : em.getSettings()) {
                    if (!s.isVisible()) continue;
                    any = true;
                    renderSetting(ctx, s, cardX + SUB_PAD_X, sy, sw, fade * catT * expFade);
                    sy += settingHeight(s) + SET_GAP;
                }
                if (!any) {
                    text(ctx, Fonts.MEDIUM, "Нет настроек", cardX + SUB_PAD_X, sy + 2f,
                            withAlpha(C_TEXT_DIM, (int)(200f * fade * catT * expFade)), 8f);
                }
                ms.pop();
            }
        }

        if (mods.isEmpty()) {
            float p = aval("emptypulse", pulse(now(), 1500L), 5f);
            text(ctx, Fonts.MEDIUM, "Нет модулей",
                    cx + 8f, lTop + 8f,
                    withAlpha(C_TEXT_DIM, (int)((140f + 60f * p) * fade)), 8f);
        }

        ctx.disableScissor();
    }

    // ═════════════════════════════════════════════════════════════════
    //  Чат с DARKA-0.1
    // ═════════════════════════════════════════════════════════════════

    private static final float CHAT_PAD_X    = 14f;
    private static final float CHAT_BUBBLE_R = 10f;
    private static final float CHAT_INPUT_H  = 26f;
    private static final float CHAT_SEND_W   = 52f;
    private static final float CHAT_GAP      = 6f;

    private void renderDarkaChat(DrawContext ctx, float fx, float fy, int mx, int my, float fade) {
        float cx = cardX(fx);
        float cw = (CARD_W + CARD_GAP) * GRID_COLS - CARD_GAP;  // чат на всю ширину сетки
        float lTop = listTop(fy) + 4f;
        float lBot = listBottom(fy);

        // Заголовок чата
        text(ctx, Fonts.BOLD, "DARKA-0.1", cx, lTop, withAlpha(C_TEXT, (int)(255f * fade)), 13f);
        float subW = Fonts.MEDIUM.getWidth("ИИ-ассистент на базе Hugging Face", 8f);
        text(ctx, Fonts.MEDIUM, "ИИ-ассистент на базе Hugging Face", cx + cw - subW,
                lTop + 3f, withAlpha(C_TEXT_DIM, (int)(200f * fade)), 8f);

        float bodyTop = lTop + 22f;
        float bodyBot = lBot - CHAT_INPUT_H - CHAT_GAP;
        float bodyH = bodyBot - bodyTop;

        // Фон области сообщений
        rect(ctx, cx, bodyTop, cw, bodyH, CARD_R, withAlpha(0xFF141417, (int)(235f * fade)));

        ctx.enableScissor((int)cx, (int)bodyTop, (int)(cx + cw), (int)bodyBot);

        var history = dev.darkvisuals.client.managers.DarkaManager.getHistory();

        // Сначала считаем высоту всех пузырей, чтобы выровнять снизу-вверх
        List<float[]> layouts = new ArrayList<>();
        float totalH = 0f;
        for (var msg : history) {
            float maxBW = cw - CHAT_PAD_X * 2f - 60f;
            List<String> lines = wrap(msg.text, maxBW, 8.5f);
            float bh = 10f + lines.size() * 11f;
            layouts.add(new float[]{ bh, lines.size() });
            totalH += bh + 6f;
        }
        if (dev.darkvisuals.client.managers.DarkaManager.isBusy()) totalH += 20f;

        // Целевой скролл: показываем последний кусок диалога
        float maxScroll = Math.max(0f, totalH - bodyH + 8f);
        darkaScroll = aval("darkascroll", maxScroll, 14f);
        float scroll = Math.min(darkaScroll, maxScroll);

        float y = bodyBot + scroll - totalH;
        for (int i = 0; i < history.size(); i++) {
            var msg = history.get(i);
            float bh = layouts.get(i)[0];
            int lines = (int) layouts.get(i)[1];
            float maxBW = cw - CHAT_PAD_X * 2f - 60f;
            List<String> linesList = wrap(msg.text, maxBW, 8.5f);

            float bw = 0f;
            for (String ln : linesList) bw = Math.max(bw, Fonts.MEDIUM.getWidth(ln, 8.5f));
            bw = Math.min(maxBW, bw + 14f);

            float bx = msg.user ? cx + cw - CHAT_PAD_X - bw : cx + CHAT_PAD_X;

            // пузырь
            rect(ctx, bx, y, bw, bh, CHAT_BUBBLE_R,
                    msg.user ? withAlpha(0xFF2A2340, (int)(255f * fade))
                             : withAlpha(0xFF232328, (int)(255f * fade)));
            // текст
            float ty = y + 5f;
            for (String ln : linesList) {
                float lw = Fonts.MEDIUM.getWidth(ln, 8.5f);
                float tx = msg.user ? bx + bw - lw - 7f : bx + 7f;
                text(ctx, Fonts.MEDIUM, ln, tx, ty,
                        withAlpha(msg.user ? 0xFFE4DCFF : C_TEXT, (int)(240f * fade)), 8.5f);
                ty += 11f;
            }
            y += bh + 6f;
        }

        // Индикатор "печатает..."
        if (dev.darkvisuals.client.managers.DarkaManager.isBusy()) {
            float p = pulse(now(), 800L);
            String dots = ".".repeat(1 + (int)(p * 2));
            text(ctx, Fonts.MEDIUM, "DARKA печатает" + dots, cx + CHAT_PAD_X + 2f, y + 4f,
                    withAlpha(C_TEXT_DIM, (int)(140f + 80f * p) & 0xFF), 8.5f);
        }

        ctx.disableScissor();

        // Пустое состояние
        if (history.isEmpty()) {
            String hint = "Напиши сообщение ниже, чтобы начать";
            float hw = Fonts.MEDIUM.getWidth(hint, 8.5f);
            text(ctx, Fonts.MEDIUM, hint, cx + (cw - hw) / 2f, bodyTop + bodyH / 2f,
                    withAlpha(C_TEXT_DIM, (int)(160f * fade)), 8.5f);
        }

        // ── Поле ввода + кнопка отправки ──
        float inY = lBot - CHAT_INPUT_H;
        boolean sendHov = hit(mx, my, cx + cw - CHAT_SEND_W, inY, CHAT_SEND_W, CHAT_INPUT_H);
        float sendT = aval("sendhov", sendHov ? 1f : 0f, 12f);

        rect(ctx, cx, inY, cw - CHAT_SEND_W - CHAT_GAP, CHAT_INPUT_H, 13f,
                withAlpha(darkaInputFocused ? C_CARD_HOV : 0xFF1C1C20, (int)(240f * fade)));
        rect(ctx, cx + cw - CHAT_SEND_W, inY, CHAT_SEND_W, CHAT_INPUT_H, 13f,
                lerpColor(withAlpha(C_ACCENT, (int)(190f * fade)),
                          withAlpha(C_THUMB,   (int)(255f * fade)), sendT));

        String shown = darkaInput.toString();
        if (shown.isEmpty() && !darkaInputFocused) {
            text(ctx, Fonts.MEDIUM, "Сообщение для DARKA...", cx + 10f, inY + 9f,
                    withAlpha(C_TEXT_DIM, (int)(170f * fade)), 8.5f);
        } else {
            // прокрутка длинного ввода
            while (Fonts.MEDIUM.getWidth(shown, 8.5f) > cw - CHAT_SEND_W - CHAT_GAP - 24f && shown.length() > 1)
                shown = shown.substring(1);
            float blink = aval("darkablink", (now() / 500L) % 2L == 0L ? 1f : 0f, 6f);
            text(ctx, Fonts.MEDIUM, shown, cx + 10f, inY + 9f,
                    withAlpha(C_TEXT, (int)(255f * fade)), 8.5f);
            if (darkaInputFocused && blink > 0.5f) {
                float cw2 = Fonts.MEDIUM.getWidth(shown, 8.5f);
                rect(ctx, cx + 10f + cw2 + 1f, inY + 6f, 1f, CHAT_INPUT_H - 12f, 0f,
                        withAlpha(C_TEXT, (int)(200f * fade)));
            }
        }

        String sendLabel = "Отправить";
        float slw = Fonts.BOLD.getWidth(sendLabel, 8f);
        text(ctx, Fonts.BOLD, sendLabel, cx + cw - CHAT_SEND_W / 2f - slw / 2f, inY + 9f,
                withAlpha(C_TEXT, (int)(255f * fade)), 8f);
    }

    private boolean sendDarkaMessage() {
        String msg = darkaInput.toString().trim();
        if (msg.isEmpty()) return false;
        darkaInput.setLength(0);
        darkaScroll = 0f;
        dev.darkvisuals.client.managers.DarkaManager.ask(msg);
        return true;
    }

    public boolean isKeyOrInputFocused() {
        return darkaKeyFocused || darkaInputFocused;
    }

    /** Обработка ввода текста для полей New-стиля: чат DARKA и API-ключ. */
    public boolean handleCharTyped(char c) {
        if (Character.isISOControl(c)) return darkaInputFocused || darkaKeyFocused;
        if (darkaInputFocused) {
            if (darkaInput.length() < 200) darkaInput.append(c);
            return true;
        }
        if (darkaKeyFocused) {
            var ui = dev.darkvisuals.darkvisuals.getInstance().getModuleManager()
                    .getModule(dev.darkvisuals.modules.impl.render.UI.class);
            if (ui != null) {
                String cur = ui.getDarkaApiKey();
                if (cur.length() < 256) {
                    ui.setDarkaApiKey(cur + c);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean handleKeyPressed(int k, int m) {
        boolean ctrl = Screen.hasControlDown() || (m & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean paste = Screen.isPaste(k) || (ctrl && k == GLFW.GLFW_KEY_V);

        if (darkaInputFocused) {
            if (paste) {
                String clip = ClickGuiInputHandler.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    String clean = clip.replace("\r", "").replace("\n", "");
                    if (darkaInput.length() + clean.length() <= 200) {
                        darkaInput.append(clean);
                    }
                }
                return true;
            }
            if (k == GLFW.GLFW_KEY_BACKSPACE) {
                if (darkaInput.length() > 0) darkaInput.deleteCharAt(darkaInput.length() - 1);
            } else if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) {
                sendDarkaMessage();
            } else if (k == GLFW.GLFW_KEY_ESCAPE) {
                darkaInputFocused = false;
            }
            return true;
        }
        if (darkaKeyFocused) {
            var ui = dev.darkvisuals.darkvisuals.getInstance().getModuleManager()
                    .getModule(dev.darkvisuals.modules.impl.render.UI.class);
            if (paste && ui != null) {
                String clip = ClickGuiInputHandler.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    ui.setDarkaApiKey(clip.trim().replace("\r", "").replace("\n", ""));
                }
                return true;
            }
            if (k == GLFW.GLFW_KEY_BACKSPACE && ui != null) {
                String cur = ui.getDarkaApiKey();
                if (!cur.isEmpty()) ui.setDarkaApiKey(cur.substring(0, cur.length() - 1));
                return true;
            }
            if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER || k == GLFW.GLFW_KEY_ESCAPE) {
                darkaKeyFocused = false;
                return true;
            }
            return true;
        }
        return false;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Настройки ClickGUI + DARKA-0.1
    // ═════════════════════════════════════════════════════════════════

    private static final float SET_VIEW_PAD = 14f;

    private void renderSettingsView(DrawContext ctx, float fx, float fy, int mx, int my, float fade) {
        float cx = cardX(fx);
        float cw = (CARD_W + CARD_GAP) * GRID_COLS - CARD_GAP;
        float lTop = listTop(fy) + 4f;
        float lBot = listBottom(fy);

        var ui = dev.darkvisuals.darkvisuals.getInstance().getModuleManager()
                .getModule(dev.darkvisuals.modules.impl.render.UI.class);

        float y = lTop;

        // ── Раздел: Настройки ClickGUI ──
        text(ctx, Fonts.BOLD, "Настройки ClickGUI", cx, y, withAlpha(C_TEXT, (int)(255f * fade)), 10f);
        y += 20f;

        if (ui != null) {
            // Звук вкл/выкл функций
            boolean soundOn = ui.isGuiSound();
            float soundT = aval("setguisound", soundOn ? 1f : 0f, 12f);
            float rowH = 30f;
            boolean rowHov = hit(mx, my, cx, y, cw, rowH);
            float rowHovT = aval("setrowguisound", rowHov ? 1f : 0f, 12f);
            rect(ctx, cx, y, cw, rowH, 10f,
                    lerpColor(withAlpha(C_CARD, (int)(235f * fade)),
                              withAlpha(C_CARD_HOV, (int)(235f * fade)), rowHovT));
            text(ctx, Fonts.MEDIUM, "Звук при включении функции", cx + 12f, y + 11f,
                    withAlpha(C_TEXT, (int)(245f * fade)), 8.5f);
            drawToggle(ctx, cx + cw - 12f - 36f, y + 5f, soundT, fade);
            y += rowH + 8f;
        }

        // ── Раздел: Настройки DARKA-0.1 ──
        y += 6f;
        text(ctx, Fonts.BOLD, "Настройки DARKA-0.1", cx, y, withAlpha(C_TEXT, (int)(255f * fade)), 10f);
        y += 20f;

        // API-ключ + кнопка "i"
        float inputW = cw - 30f - 26f;
        boolean keyFocused = darkaKeyFocused;
        drawInput(ctx, cx, y, inputW, keyFocused,
                ui == null ? "" : ui.getDarkaApiKey(), "Вставьте API-ключ...", fade);

        // круглая кнопка "i"
        float iX = cx + inputW + 6f;
        float iSize = 22f;
        boolean iHov = hit(mx, my, iX, y, iSize, iSize);
        float iT = aval("ibtn", iHov ? 1f : 0f, 12f);
        rect(ctx, iX, y, iSize, iSize, iSize / 2f,
                lerpColor(withAlpha(C_CARD, (int)(235f * fade)),
                          withAlpha(C_CARD_HOV, (int)(255f * fade)), iT));
        String iSym = "i";
        float iw = Fonts.BOLD.getWidth(iSym, 9f);
        text(ctx, Fonts.BOLD, iSym, iX + iSize / 2f - iw / 2f, y + 6f,
                withAlpha(C_TEXT, (int)(255f * fade)), 9f);

        // подсказка под полем
        String hint = ui != null && ui.getDarkaApiKey().isEmpty()
                ? "Ключ не задан — DARKA-0.1 недоступна" : "Ключ сохранён";
        text(ctx, Fonts.MEDIUM, hint, cx + 2f, y + 26f,
                withAlpha(C_TEXT_DIM, (int)(190f * fade)), 7.5f);

        // Окно инструкции Hugging Face (правее ClickGUI)
        if (hfWindowOpen) {
            renderHfWindow(ctx, fx, fy, mx, my, fade);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Окно инструкции Hugging Face
    // ═════════════════════════════════════════════════════════════════

    private static final float HF_W = 250f;
    private static final float HF_H = 300f;
    private static final float HF_R = 16f;
    private static final float HF_TITLE_H = 34f;
    private float hfScroll = 0f;
    private boolean darkaKeyFocused;

    private void renderHfWindow(DrawContext ctx, float fx, float fy, int mx, int my, float fade) {
        float hx = fx + FRAME_W + 12f;
        float hy = fy;

        // фон + обводка
        rect(ctx, hx - 1f, hy - 1f, HF_W + 2f, HF_H + 2f, HF_R + 1f,
                withAlpha(C_BORDER, (int)(255f * fade)));
        rect(ctx, hx, hy, HF_W, HF_H, HF_R, withAlpha(C_BG, (int)(250f * fade)));

        // шапка
        rect(ctx, hx, hy, HF_W, HF_TITLE_H, HF_R, withAlpha(0xFF1E1E23, (int)(255f * fade)));
        text(ctx, Fonts.BOLD, "Hugging Face", hx + 14f, hy + 11f,
                withAlpha(C_TEXT, (int)(255f * fade)), 10f);

        // крестик
        float closeX = hx + HF_W - 24f, closeY = hy + 8f, closeS = 18f;
        boolean closeHov = hit(mx, my, closeX, closeY, closeS, closeS);
        float closeT = aval("hfclose", closeHov ? 1f : 0f, 12f);
        rect(ctx, closeX, closeY, closeS, closeS, closeS / 2f,
                lerpColor(withAlpha(0xFF232328, (int)(200f * fade)),
                          withAlpha(0xFFFF4D4D, (int)(255f * fade)), closeT));
        float cs = 4f;
        rect(ctx, closeX + closeS / 2f - 0.8f, closeY + closeS / 2f - cs / 2f, 1.6f, cs, 0.8f,
                withAlpha(C_TEXT, (int)(230f * fade)));
        rect(ctx, closeX + closeS / 2f - cs / 2f, closeY + closeS / 2f - 0.8f, cs, 1.6f, 0.8f,
                withAlpha(C_TEXT, (int)(230f * fade)));

        // тело с прокруткой
        float bodyTop = hy + HF_TITLE_H + 6f;
        float bodyBot = hy + HF_H - 8f;
        float bodyH = bodyBot - bodyTop;

        String[] blocks = HF_INSTRUCTIONS;
        // считаем суммарную высоту
        List<String[]> wrapped = new ArrayList<>();
        float totalH = 0f;
        for (String blk : blocks) {
            List<String> lines = wrap(blk, HF_W - 24f, 7.5f);
            wrapped.add(lines.toArray(new String[0]));
            totalH += lines.size() * 10.5f + 6f;
        }
        float maxScroll = Math.max(0f, totalH - bodyH + 4f);
        hfScroll = Math.min(hfScroll, maxScroll);
        float scroll = aval("hfscroll", maxScroll, 1f); // мягко доводим до низа

        ctx.enableScissor((int)hx, (int)bodyTop, (int)(hx + HF_W), (int)bodyBot);
        float y = bodyTop - scroll;
        for (String[] lines : wrapped) {
            for (String ln : lines) {
                text(ctx, Fonts.MEDIUM, ln, hx + 12f, y, withAlpha(C_TEXT, (int)(225f * fade)), 7.5f);
                y += 10.5f;
            }
            y += 6f;
        }
        ctx.disableScissor();

        // тонкий скроллбар
        if (maxScroll > 0.5f) {
            float thumbH = Math.max(10f, bodyH * bodyH / totalH);
            float thumbY = bodyTop + (bodyH - thumbH) * (hfScroll / maxScroll);
            rect(ctx, hx + HF_W - 5f, thumbY, 2.5f, thumbH, 1.25f,
                    withAlpha(C_TEXT, (int)(60f * fade)));
        }
    }

    private static final String[] HF_INSTRUCTIONS = {
        "Зарегистрируйтесь или войдите в аккаунт\nЕсли у вас еще нет аккаунта Hugging Face, создайте его. Регистрация бесплатна и не требует привязки банковской карты.",
        "* Перейдите на страницу регистрации: https://huggingface.co/join",
        "1. Перейдите в настройки токенов\nПосле входа в систему перейдите на страницу управления токенами доступа. Это можно сделать по прямой ссылке или через меню.\n* Прямая ссылка: https://huggingface.co/settings/tokens\n* Или нажмите на иконку вашего профиля в правом верхнем углу и выберите Settings (Настройки), затем в боковом меню выберите Access Tokens (Токены доступа).",
        "2. Создайте новый токен\nНа странице настроек нажмите кнопку \"New token\" (Новый токен) или \"+ Create new token\".",
        "3. Настройте и сгенерируйте токен\nВ открывшемся окне вам нужно:\n* Ввести имя токена (Token name): Дайте ему понятное название, например, \"Local Testing\", \"My Project\" или \"rag-analysis\", чтобы в будущем понимать, для чего он используется.\n* Выбрать тип токена (Token type / Permissions): Это важный шаг, определяющий права доступа.\n  * Read (Чтение): Позволяет скачивать публичные модели и датасеты, а также отправлять запросы к Inference API. Подходит для большинства базовых задач.\n  * Write (Запись): Дает право не только читать, но и загружать свои файлы, создавать репозитории и изменять их содержимое.\n  * Fine-grained (Детализированный): Позволяет вручную настроить права для конкретных репозиториев или организаций.\n* Установить срок действия (по желанию): Вы можете задать дату истечения срока действия токена или оставить бессрочным.\n* Нажмите кнопку \"Generate a token\" (Сгенерировать токен) или \"Create token\" (Создать токен).",
        "4. Скопируйте и сохраните токен\nСразу же после создания скопируйте полученный токен. По соображениям безопасности он больше не будет отображаться на сайте."
    };

    // ─── Иконка-«ползунки» (три горизонтальные черты с кружочками) ───

    private void drawSliderIcon(DrawContext ctx, float x, float y, float fade) {
        int c = withAlpha(C_TEXT, (int)(200f * fade));
        float lh = 1.5f;
        rect(ctx, x,      y,      10f, lh, 0.75f, c);
        rect(ctx, x,      y + 4f, 10f, lh, 0.75f, c);
        rect(ctx, x,      y + 8f, 10f, lh, 0.75f, c);
        rect(ctx, x + 5.5f, y - 1f, 3.5f, 3.5f, 1f, c);
        rect(ctx, x + 2f,   y + 3f, 3.5f, 3.5f, 1f, c);
        rect(ctx, x + 5.5f, y + 7f, 3.5f, 3.5f, 1f, c);
    }

    // ─── iOS-style тогл ───────────────────────────────────────────────

    private void drawToggle(DrawContext ctx, float x, float y, float togT, float fade) {
        float tw = 36f, th = 20f, tr = 10f;
        // Трек всегда тёмный: #18181c выкл → #212126 вкл (как на референсе)
        int track = lerpColor(
                withAlpha(0xFF18181C, (int)(255f * fade)),
                withAlpha(0xFF212126, (int)(255f * fade)), togT);
        rect(ctx, x, y, tw, th, tr, track);

        // Свечение бегунка при вкл (box-shadow 0 0 10px rgba(139,92,246,0.6))
        if (togT > 0.05f) {
            float g = 5f * togT;
            float tcx = x + 3f + (tw - th) * togT + (th - 6f) / 2f;
            float tcy = y + th / 2f;
            rect(ctx, tcx - 7f - g / 2f, tcy - 7f - g / 2f, 14f + g, 14f + g, (14f + g) / 2f,
                    withAlpha(C_THUMB, (int)(90f * fade * togT)));
        }

        // Бегунок: #4a4a52 выкл → #8b5cf6 вкл, с пружинящим овершутом
        float knobPop = aval("knobpop:" + x + ":" + y, togT, 9f);
        float overshoot = 1f + 0.22f * (float) Math.sin(Math.min(1f, Math.max(0f, knobPop)) * Math.PI) * togT;
        float knobSize = (th - 6f) * overshoot;
        float thumbCX = x + 3f + (tw - th) * togT + (th - 6f) / 2f;
        float thumbCY = y + th / 2f;
        rect(ctx, thumbCX - knobSize / 2f, thumbCY - knobSize / 2f, knobSize, knobSize, knobSize / 2f,
                lerpColor(withAlpha(0xFF4A4A52, (int)(255f * fade)),
                          withAlpha(C_THUMB,   (int)(255f * fade)), togT));
    }

    /** V-образная стрелка-шеврон, разворачивающаяся на 180° при экспандинге карточки. */
    private void drawChevron(DrawContext ctx, float cx, float cy, float expT, float fade) {
        int col = withAlpha(C_TEXT_DIM, (int)(220f * fade));
        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, cy, 0f);
        ms.multiply(new org.joml.Quaternionf().rotateZ((float)(Math.PI * expT)));

        ms.push();
        ms.multiply(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(45)));
        rect(ctx, -1.05f, -4.5f, 2.1f, 6.3f, 1.05f, col);
        ms.pop();

        ms.push();
        ms.multiply(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(-45)));
        rect(ctx, -1.05f, -4.5f, 2.1f, 6.3f, 1.05f, col);
        ms.pop();

        ms.pop();
    }

    /** Простая векторная иконка-«искра» (4-лучевая звезда) для левой боковой кнопки навбара. */
    private void drawSparkleIcon(DrawContext ctx, float cx, float cy, float r, int color) {
        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, cy, 0f);
        ms.multiply(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(45)));
        rect(ctx, -r * 0.32f, -r, r * 0.64f, r * 2f, r * 0.32f, color);
        rect(ctx, -r, -r * 0.32f, r * 2f, r * 0.64f, r * 0.32f, color);
        ms.pop();
    }

    /** Иконка-лупа для строки поиска: кольцо + ручка. */
    private void drawSearchIcon(DrawContext ctx, float cx, float cy, float r, int color) {
        if (((color >>> 24) & 0xFF) == 0) return;
        float lw = 1.7f;
        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(0, 0, 150);
        Render2D.drawBorder(ms, cx - r, cy - r, r * 2f, r * 2f, r, 1f, 1f, Render2D.cachedColor(color));
        ms.pop();
        ms.push();
        ms.translate(cx + r * 0.72f, cy + r * 0.72f, 150);
        ms.multiply(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(45)));
        Render2D.drawRoundedRect(ms, -lw / 2f, 0f, lw, r * 0.95f, lw / 2f, Render2D.cachedColor(color));
        ms.pop();
    }

    /** Простая векторная иконка-«сетка» (2×2 квадрата) для правой боковой кнопки навбара. */
    private void drawGridIcon(DrawContext ctx, float cx, float cy, float size, int color) {        float cell = size * 0.42f, gap = size * 0.16f;
        float x0 = cx - cell - gap / 2f, y0 = cy - cell - gap / 2f;
        rect(ctx, x0, y0, cell, cell, 1.5f, color);
        rect(ctx, x0 + cell + gap, y0, cell, cell, 1.5f, color);
        rect(ctx, x0, y0 + cell + gap, cell, cell, 1.5f, color);
        rect(ctx, x0 + cell + gap, y0 + cell + gap, cell, cell, 1.5f, color);
    }

    private void renderSetting(DrawContext ctx, Setting<?> s,
                                float sx, float y, float sw, float fade) {
        String name = net.minecraft.client.resource.language.I18n.translate(s.getName());

        if (s instanceof ListSetting ls) {
            text(ctx, Fonts.MEDIUM, name, sx, y + 2f, withAlpha(C_TEXT, (int)(255f * fade)), 7.5f);
            float px = sx, py = y + 12f;
            for (BooleanSetting opt : ls.getValue()) {
                String lbl = net.minecraft.client.resource.language.I18n.translate(opt.getName());
                float pw = Fonts.MEDIUM.getWidth(lbl, 7f) + 10f;
                float t  = aval("opt:" + System.identityHashCode(opt), opt.getValue() ? 1f : 0f, 12f);
                rect(ctx, px, py, pw, 12f, SET_R,
                        lerpColor(withAlpha(C_CARD, (int)(255f * fade)), themeA((int)(200f * fade)), t));
                text(ctx, Fonts.MEDIUM, lbl, px + 5f, py + 3f,
                        lerpColor(withAlpha(C_TEXT_DIM, (int)(255f * fade)),
                                  withAlpha(C_TEXT, (int)(255f * fade)), t), 7f);
                px += pw + 3f;
                if (px > sx + sw - 20f) { px = sx; py += 15f; }
            }
        } else if (s instanceof BooleanSetting b) {
            float t = aval("set:" + System.identityHashCode(s), b.getValue() ? 1f : 0f, 12f);
            text(ctx, Fonts.MEDIUM, name, sx, y + 2f,
                    lerpColor(withAlpha(C_TEXT_DIM, (int)(255f * fade)),
                              withAlpha(C_TEXT, (int)(255f * fade)), t), 7.5f);
            drawToggle(ctx, sx + sw - 40f, y, t, fade);
        } else if (s instanceof NumberSetting n) {
            text(ctx, Fonts.MEDIUM, name + ": " + fmtNum(n.getValue()), sx, y + 1f,
                    withAlpha(C_TEXT, (int)(255f * fade)), 7.5f);
            float p  = clamp01((n.getValue() - n.getMin()) / (n.getMax() - n.getMin()));
            float sp = aval("set:" + System.identityHashCode(s), p, 16f);
            // пульсация заполнения слайдера при наведении
            float slP = aval("sliderpulse:" + System.identityHashCode(s),
                    hit(lastMouseX, lastMouseY, sx, y + 11f, sw, 9f) ? pulse(now(), 700L) : 0f, 8f);
            rect(ctx, sx, y + 13f, sw, 3f, 1.5f, withAlpha(0xFF2A2A2A, (int)(255f * fade)));
            if (sp > 0.01f)
                rect(ctx, sx, y + 13f, sw * sp, 3f, 1.5f,
                        themeA((int)(Math.min(255f, 230f + 25f * slP) * fade)));
            rect(ctx, sx + sw * sp - 3f, y + 11f, 6f, 7f, 2f, withAlpha(C_TEXT, (int)(255f * fade)));
        } else if (s instanceof BindSetting bs) {
            boolean binding = ClickGuiRenderer.bindingSetting == bs;
            String bt = binding ? "..." : keyName(bs.getValue().getKey());
            float bw = Math.max(28f, Fonts.MEDIUM.getWidth(bt, 7f) + 10f);
            text(ctx, Fonts.MEDIUM, name, sx, y + 2f, withAlpha(C_TEXT, (int)(255f * fade)), 7.5f);
            // вспышка на кнопке бинда, пока ждём нажатия
            float bP = aval("bindpulse:" + System.identityHashCode(s),
                    binding ? pulse(now(), 500L) : 0f, 8f);
            rect(ctx, sx + sw - bw, y, bw, 12f, SET_R,
                    binding ? themeA((int)((160f + 95f * bP) * fade))
                            : withAlpha(C_CARD, (int)(255f * fade)));
            text(ctx, Fonts.MEDIUM, bt, sx + sw - bw + 5f, y + 2.5f,
                    withAlpha(C_TEXT, (int)(255f * fade)), 7f);
        } else if (s instanceof EnumSetting<?> es) {
            String val = net.minecraft.client.resource.language.I18n.translate(es.currentEnumName());
            float vw = Fonts.MEDIUM.getWidth(val, 7f) + 10f;
            text(ctx, Fonts.MEDIUM, name, sx, y + 2f, withAlpha(C_TEXT, (int)(255f * fade)), 7.5f);
            rect(ctx, sx + sw - vw, y, vw, 12f, SET_R, withAlpha(C_CARD, (int)(255f * fade)));
            text(ctx, Fonts.MEDIUM, val, sx + sw - vw + 5f, y + 2.5f,
                    withAlpha(C_TEXT_DIM, (int)(255f * fade)), 7f);
        } else if (s instanceof ButtonSetting) {
            rect(ctx, sx, y, sw, 14f, SET_R, withAlpha(C_CARD, (int)(255f * fade)));
            float lw = Fonts.MEDIUM.getWidth(name, 7.5f);
            text(ctx, Fonts.MEDIUM, name, sx + (sw - lw) / 2f, y + 3f,
                    withAlpha(C_TEXT, (int)(255f * fade)), 7.5f);
        } else if (s instanceof StringSetting ss) {
            boolean editing = ClickGuiRenderer.editingStringSetting == ss;
            text(ctx, Fonts.MEDIUM, name, sx, y, withAlpha(C_TEXT_DIM, (int)(255f * fade)), 7.5f);
            rect(ctx, sx, y + 10f, sw, 13f, SET_R,
                    editing ? themeA((int)(160f * fade)) : withAlpha(C_CARD, (int)(255f * fade)));
            String val = ss.getValue() == null ? "" : ss.getValue();
            String shown = val;
            while (Fonts.MEDIUM.getWidth(shown, 7f) > sw - 14f && shown.length() > 1) shown = shown.substring(1);
            // плавное мигание каретки
            float blink = aval("strblink", (now() / 500L) % 2L == 0L ? 1f : 0f, 6f);
            text(ctx, Fonts.MEDIUM, shown + (editing && blink > 0.5f ? "|" : ""), sx + 4f, y + 13f,
                    withAlpha(C_TEXT, (int)(255f * fade)), 7f);
        }
    }

    float settingHeight(Setting<?> s) {
        if (s instanceof NumberSetting)  return 22f;
        if (s instanceof BooleanSetting) return 16f;
        if (s instanceof StringSetting)  return 26f;
        if (s instanceof ListSetting ls) {
            float cw = CARD_W;
            float px = 0; int rows = 1;
            for (BooleanSetting opt : ls.getValue()) {
                float pw = Fonts.MEDIUM.getWidth(
                        net.minecraft.client.resource.language.I18n.translate(opt.getName()), 7f) + 10f;
                if (px + pw > cw - 20f && px > 0) { rows++; px = 0; }
                px += pw + 3f;
            }
            return 12f + rows * 15f + 3f;
        }
        return 16f;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Навбар
    // ═════════════════════════════════════════════════════════════════

    private static float navRowW()  { return NAV_SLOT_W * NAV_COUNT; }
    private static float navPillW() { return navRowW() + NAV_PAD * 2f; }

    private void renderNavBar(DrawContext ctx, float fx, float fy, int mx, int my, float fade) {
        float pillW = navPillW();
        float pillH = navPillH();
        float pillX = fx + (FRAME_W - pillW) / 2f;
        // плашка плавно выезжает снизу при открытии
        float railT = aval("navrail", 1f, 10f);
        float pillY = navPillY(fy) + (1f - railT) * 10f;

        // ── Боковые квадратные кнопки (слева искра, справа сетка) ───────
        float sideW = pillH;
        float sparkleX = pillX - NAV_SIDE_GAP - sideW;
        float gridX    = pillX + pillW + NAV_SIDE_GAP;

        boolean sparkleHov = hit(mx, my, sparkleX, pillY, sideW, pillH);
        boolean gridHov    = hit(mx, my, gridX, pillY, sideW, pillH);
        float sparkleHovT = aval("navside:sparkle", sparkleHov ? 1f : 0f, 12f);
        float gridHovT    = aval("navside:grid", gridHov ? 1f : 0f, 12f);

        rect(ctx, sparkleX, pillY, sideW, pillH, NAV_SIDE_R,
                lerpColor(withAlpha(C_NAV, (int)(235f * fade)), withAlpha(C_CARD_HOV, (int)(235f * fade)), sparkleHovT));
        rect(ctx, gridX, pillY, sideW, pillH, NAV_SIDE_R,
                lerpColor(withAlpha(C_NAV, (int)(235f * fade)), withAlpha(C_CARD_HOV, (int)(235f * fade)), gridHovT));

        drawSparkleIcon(ctx, sparkleX + sideW / 2f, pillY + pillH / 2f, 6.5f,
                withAlpha(C_TEXT, (int)(255f * fade)));
        drawGridIcon(ctx, gridX + sideW / 2f, pillY + pillH / 2f, 9f,
                withAlpha(C_TEXT, (int)(255f * fade)));

        // единая тёмная плашка-подложка со скруглёнными углами
        rect(ctx, pillX, pillY, pillW, pillH, NAV_PILL_R, withAlpha(C_NAV, (int)(235f * fade)));

        // разделители между боковыми кнопками и пилюлей разделов
        rect(ctx, pillX - NAV_SIDE_GAP / 2f - 0.5f, pillY + NAV_PAD + 2f, 1f, NAV_H - 4f, 0.5f,
                withAlpha(C_DIV, (int)(220f * fade)));
        rect(ctx, pillX + pillW + NAV_SIDE_GAP / 2f - 0.5f, pillY + NAV_PAD + 2f, 1f, NAV_H - 4f, 0.5f,
                withAlpha(C_DIV, (int)(220f * fade)));
        // разделители между самими разделами
        for (int i = 1; i < NAV_COUNT; i++) {
            float dx = pillX + NAV_PAD + i * NAV_SLOT_W;
            rect(ctx, dx - 0.5f, pillY + NAV_PAD + 3f, 1f, NAV_H - 6f, 0.5f,
                    withAlpha(C_DIV, (int)(170f * fade)));
        }

        float ix = pillX + NAV_PAD;
        float slotY = pillY + NAV_PAD;
        for (int i = 0; i < NAV_COUNT; i++) {
            Category c = NAV_CATS[i];
            boolean active = c == ClickGuiRenderer.currentCategory;
            boolean hov = hit(mx, my, ix, slotY, NAV_SLOT_W, NAV_H);

            float selT = aval("navsel:" + c.name(), active ? 1f : 0f, 12f);
            float hovT = aval("navhov:" + c.name(), hov ? 1f : 0f, 10f);
            float pop  = aval("navpop:" + c.name(), active ? 1f : 0f, 8f);
            float overshoot = 1f + 0.18f * (float) Math.sin(Math.min(1f, Math.max(0f, pop)) * Math.PI) * selT;
            // «дышащее» свечение активной вкладки
            float glw = aval("navglow:" + c.name(), active ? pulse(now(), 900L) : 0f, 6f);
            // маленький бейдж-точка сверху активной вкладки
            float badgeT = aval("navbadge:" + c.name(), active ? 1f : 0f, 12f);

            float slotMin = Math.min(NAV_SLOT_W - 4f, NAV_H - 4f);

            // свечение позади активной вкладки (box-shadow 0 0 12px rgba(124,58,237,0.5))
            if (glw > 0.02f) {
                float g = 3f * glw;
                rect(ctx, ix + 2f - g, slotY + 2f - g, NAV_SLOT_W - 4f + g * 2f, NAV_H - 4f + g * 2f,
                        NAV_ICON_R + g, themeA((int)(110f * fade * glw)));
            }
            // фиолетовый squircle активной вкладки (морф круг → скруглённый квадрат)
            if (selT > 0.02f) {
                float r = lerpFloat(slotMin / 2f, NAV_ICON_R, selT);
                rect(ctx, ix + 2f, slotY + 2f, NAV_SLOT_W - 4f, NAV_H - 4f, r,
                        withAlpha(theme(), (int)(255f * fade * selT)));
            }
            // лёгкая подсветка при наведении на неактивную иконку
            if (hovT > 0.02f && selT < 0.5f) {
                rect(ctx, ix + 2f, slotY + 2f, NAV_SLOT_W - 4f, NAV_H - 4f, NAV_ICON_R,
                        withAlpha(C_CARD_HOV, (int)(160f * fade * hovT)));
            }

            // бейдж-точка
            if (badgeT > 0.02f) {
                rect(ctx, ix + NAV_SLOT_W / 2f - 2f, slotY - 1f - (1f - badgeT) * 4f, 4f, 4f, 2f,
                        withAlpha(C_TEXT, (int)(255f * fade * badgeT)));
            }

            Identifier icon = navIcon(i);
            float isz = NAV_ICON * overshoot;
            // неактивные иконки — серые, активная — белая
            int tint = active ? withAlpha(C_TEXT, (int)(255f * fade))
                             : lerpColor(withAlpha(C_ICON_DIM, (int)(230f * fade)),
                                         withAlpha(C_TEXT, (int)(255f * fade)), hovT);
            if (icon != null) {
                tex(ctx, icon, ix + (NAV_SLOT_W - isz) / 2f, slotY + (NAV_H - isz) / 2f, isz, tint);
            }

            ix += NAV_SLOT_W;
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  Строка поиска
    // ═════════════════════════════════════════════════════════════════

    private void renderSearchBar(DrawContext ctx, float fx, float fy, float fade) {
        float barX = fx + (FRAME_W - SEARCH_W) / 2f;
        float barY = fy + FRAME_H + SEARCH_GAP;

        boolean focused = ClickGuiRenderer.searchInputFocused;
        float focusT = aval("searchglow", focused ? 1f : 0f, 12f);
        if (focusT > 0.02f) {
            rect(ctx, barX - 1.5f, barY - 1.5f, SEARCH_W + 3f, SEARCH_H + 3f,
                    SEARCH_R + 1.5f, themeA((int)(120f * fade * focusT)));
        }

        rect(ctx, barX, barY, SEARCH_W, SEARCH_H, SEARCH_R,
                withAlpha(focused ? C_CARD_HOV : C_NAV, (int)(220f * fade)));

        String query  = ClickGuiRenderer.searchBuffer.toString();
        String display = query.isEmpty() ? "Поиск" : query;
        // «дышащий» плейсхолдер
        float phP = query.isEmpty() ? aval("searchpulse", pulse(now(), 1200L), 5f) : 0f;
        int tcol = query.isEmpty()
                ? withAlpha(C_TEXT_DIM, (int)((170f + 60f * phP) * fade))
                : withAlpha(C_TEXT,     (int)(255f * fade));
        text(ctx, Fonts.MEDIUM, display, barX + 12f,
                barY + (SEARCH_H - 8f) / 2f + 1f, tcol, 8f);

        drawSearchIcon(ctx, barX + SEARCH_W - 16f, barY + SEARCH_H / 2f, 4.5f,
                withAlpha(C_TEXT_DIM, (int)(200f * fade)));
    }

    // ═════════════════════════════════════════════════════════════════
    //  Не-модульные вкладки
    // ═════════════════════════════════════════════════════════════════

    private void renderThemes(DrawContext ctx, float cx, float cw, float y, float fade) {
        boolean addHov = hit(lastMouseX, lastMouseY, cx, y, cw, 24f);
        float addT = aval("customtheme:hov", addHov ? 1f : 0f, 12f);
        rect(ctx, cx, y, cw, 24f, SET_R, lerpColor(withAlpha(C_CARD, (int)(255f * fade)), themeA((int)(200f * fade)), addT));
        text(ctx, Fonts.BOLD, "+ Своя тема", cx + 10f, y + 8f, withAlpha(C_TEXT, (int)(255f * fade)), 8f);
        y += 24f + CARD_GAP;

        // карточки тем — по две в ряд
        float thW = CARD_W;
        float thH = CARD_H * 0.75f;
        var themes = ClickGuiRenderer.getThemes();
        for (int i = 0; i < themes.length; i++) {
            var t = themes[i];
            float tx = cx + (i % 2) * (thW + CARD_GAP);
            float ty = y + (i / 2) * (thH + CARD_GAP);

            boolean sel = dev.darkvisuals.client.managers.ThemeManager.getInstance()
                    .getCurrentTheme().getName().equals(t.getName());
            boolean hov = hit(lastMouseX, lastMouseY, tx, ty, thW, thH);
            float selT = aval("thsel:" + t.getName(), sel ? 1f : 0f, 12f);
            // пульсация выбранной темы
            float thP = aval("thpulse:" + t.getName(), sel ? pulse(now(), 800L) : 0f, 6f);
            float hovT = aval("thhov:" + t.getName(), hov ? 1f : 0f, 12f);
            int bg = lerpColor(lerpColor(withAlpha(C_CARD, (int)(255f * fade)), withAlpha(C_CARD_HOV, (int)(255f * fade)), hovT),
                    themeA((int)((180f + 60f * thP) * fade)), selT);
            rect(ctx, tx, ty, thW, thH, CARD_R, bg);
            java.awt.Color tc = t.getAccentColor();
            rect(ctx, tx + thW - 18f, ty + 6f, 10f, 10f, 2.5f,
                    withAlpha(tc.getRGB() | 0xFF000000, (int)(255f * fade)));
            text(ctx, Fonts.MEDIUM, t.getName(), tx + 8f, ty + 7f,
                    withAlpha(C_TEXT, (int)(255f * fade)), 8f);
        }
    }

    private void renderFriends(DrawContext ctx, float cx, float cw, float y, float fade) {
        float inputW = cw - BTN_W - 6f, addX = cx + inputW + 6f;
        drawInput(ctx, cx, y, inputW, ClickGuiRenderer.friendInputFocused,
                ClickGuiRenderer.friendNameBuffer.toString(), "Ник игрока...", fade);
        drawButton(ctx, addX, y, BTN_W, "ADD", "friendadd", fade);

        if (ClickGuiRenderer.friendStatus != null && System.currentTimeMillis() < ClickGuiRenderer.friendStatusUntil) {
            text(ctx, Fonts.MEDIUM, ClickGuiRenderer.friendStatus, cx, y + INPUT_H + 3f,
                    withAlpha(0xFF9BE39B, (int)(255f * fade)), 7.5f);
        }
        y += INPUT_H + ROWSET_GAP;

        List<String> friends = dev.darkvisuals.client.managers.FriendsManager.getFriends();
        for (String f : friends) {
            float rowT = aval("rowin:friend:" + f, 1f, 10f);
            float rx = cx + (1f - rowT) * 12f;
            boolean online = ClickGuiRenderer.isFriendOnline(f);
            boolean hov = hit(lastMouseX, lastMouseY, rx, y, cw, LIST_ROW_H);
            float hovT = aval("friendrow:" + f, hov ? 1f : 0f, 12f);
            rect(ctx, rx, y, cw, LIST_ROW_H, SET_R,
                    lerpColor(withAlpha(C_CARD, (int)(255f * fade * rowT)), withAlpha(C_CARD_HOV, (int)(255f * fade)), hovT));
            rect(ctx, rx + 8f, y + 10f, 6f, 6f, 3f, withAlpha(online ? 0xFF4CD964 : 0xFF666666, (int)(255f * fade)));
            text(ctx, Fonts.MEDIUM, f, rx + 20f, y + 9f, withAlpha(C_TEXT, (int)(255f * fade * rowT)), 8f);
            String st = online ? "online" : "offline";
            float stw = Fonts.MEDIUM.getWidth(st, 7f);
            text(ctx, Fonts.MEDIUM, st, rx + cw - stw - 36f, y + 10f,
                    online ? withAlpha(0xFF4CD964, (int)(255f * fade)) : withAlpha(C_TEXT_DIM, (int)(255f * fade)), 7f);
            float delHovT = aval("frienddel:" + f, hit(lastMouseX, lastMouseY, rx + cw - 32f, y, 28f, LIST_ROW_H) ? 1f : 0f, 14f);
            text(ctx, Fonts.BOLD, "DEL", rx + cw - 28f, y + 9f,
                    lerpColor(withAlpha(0xFFFF7070, (int)(255f * fade)), withAlpha(0xFFFFFFFF, (int)(255f * fade)), delHovT), 7f);
            y += LIST_ROW_H + LIST_ROW_GAP;
        }
        if (friends.isEmpty()) {
            text(ctx, Fonts.MEDIUM, "Нет друзей", cx + 2f, y + 4f, withAlpha(C_TEXT_DIM, (int)(180f * fade)), 8f);
        }
    }

    private void renderMarkers(DrawContext ctx, float cx, float cw, float y, float fade) {
        float addW = 34f, hereW = 36f, coordW = 34f, gap = 4f;
        float nameW = cw - coordW * 2f - addW - hereW - gap * 4f;
        float xX = cx + nameW + gap, zX = xX + coordW + gap;
        float addX = zX + coordW + gap, hereX = addX + addW + gap;

        drawInput(ctx, cx, y, nameW, ClickGuiRenderer.markerFocusedField == 1,
                ClickGuiRenderer.markerNameBuffer.toString(), "Имя...", fade);
        drawInput(ctx, xX, y, coordW, ClickGuiRenderer.markerFocusedField == 2,
                ClickGuiRenderer.markerXBuffer.toString(), "X", fade);
        drawInput(ctx, zX, y, coordW, ClickGuiRenderer.markerFocusedField == 3,
                ClickGuiRenderer.markerZBuffer.toString(), "Z", fade);
        drawButton(ctx, addX, y, addW, "ADD", "markeradd", fade);
        drawButton(ctx, hereX, y, hereW, "HERE", "markerhere", fade);

        if (ClickGuiRenderer.markerStatus != null && System.currentTimeMillis() < ClickGuiRenderer.markerStatusUntil) {
            text(ctx, Fonts.MEDIUM, ClickGuiRenderer.markerStatus, cx, y + INPUT_H + 3f,
                    withAlpha(0xFF9BE39B, (int)(255f * fade)), 7.5f);
        }
        y += INPUT_H + ROWSET_GAP;

        var markers = dev.darkvisuals.client.managers.WaypointManager.list();
        for (var w : markers) {
            float rowT = aval("rowin:marker:" + w.name, 1f, 10f);
            float rx = cx + (1f - rowT) * 12f;
            boolean hov = hit(lastMouseX, lastMouseY, rx, y, cw, LIST_ROW_H);
            float hovT = aval("markerrow:" + w.name, hov ? 1f : 0f, 12f);
            rect(ctx, rx, y, cw, LIST_ROW_H, SET_R,
                    lerpColor(withAlpha(C_CARD, (int)(255f * fade * rowT)), withAlpha(C_CARD_HOV, (int)(255f * fade)), hovT));
            rect(ctx, rx + 8f, y + 10f, 6f, 6f, 3f, withAlpha(theme(), (int)(255f * fade)));
            text(ctx, Fonts.MEDIUM, w.name, rx + 20f, y + 9f, withAlpha(C_TEXT, (int)(255f * fade * rowT)), 8f);
            String coords = (int) w.pos.x + ", " + (int) w.pos.z;
            float cw2 = Fonts.MEDIUM.getWidth(coords, 7f);
            text(ctx, Fonts.MEDIUM, coords, rx + cw - cw2 - 36f, y + 10f,
                    withAlpha(C_TEXT_DIM, (int)(255f * fade)), 7f);
            float delHovT = aval("markerdel:" + w.name, hit(lastMouseX, lastMouseY, rx + cw - 32f, y, 28f, LIST_ROW_H) ? 1f : 0f, 14f);
            text(ctx, Fonts.BOLD, "DEL", rx + cw - 28f, y + 9f,
                    lerpColor(withAlpha(0xFFFF7070, (int)(255f * fade)), withAlpha(0xFFFFFFFF, (int)(255f * fade)), delHovT), 7f);
            y += LIST_ROW_H + LIST_ROW_GAP;
        }
        if (markers.isEmpty()) {
            text(ctx, Fonts.MEDIUM, "Нет меток", cx + 2f, y + 4f, withAlpha(C_TEXT_DIM, (int)(180f * fade)), 8f);
        }
    }

    private void renderConfigs(DrawContext ctx, float cx, float cw, float y, float fade) {
        // Контент вкладки — по центру фрейма
        float centerX = cx + cw / 2f;

        float inputW = Math.min(cw - BTN_W - 6f, 240f);
        float rowW = inputW + 6f + BTN_W;
        float rowX = centerX - rowW / 2f;
        float saveX = rowX + inputW + 6f;
        drawInput(ctx, rowX, y, inputW, ClickGuiRenderer.configInputFocused,
                ClickGuiRenderer.configNameBuffer.toString(), "Имя конфига...", fade);
        drawButton(ctx, saveX, y, BTN_W, "SAVE", "configsave", fade);

        if (ClickGuiRenderer.configStatus != null && System.currentTimeMillis() < ClickGuiRenderer.configStatusUntil) {
            float stw = Fonts.MEDIUM.getWidth(ClickGuiRenderer.configStatus, 7.5f);
            text(ctx, Fonts.MEDIUM, ClickGuiRenderer.configStatus, centerX - stw / 2f, y + INPUT_H + 3f,
                    withAlpha(0xFF9BE39B, (int)(255f * fade)), 7.5f);
        }
        y += INPUT_H + ROWSET_GAP;

        String[] configs = dev.darkvisuals.darkvisuals.getInstance().getConfigManager().getConfigList();
        for (String cfg : configs) {
            float rowT = aval("rowin:cfg:" + cfg, 1f, 10f);
            float rx = centerX - rowW / 2f + (1f - rowT) * 12f;
            boolean hov = hit(lastMouseX, lastMouseY, rx, y, rowW, LIST_ROW_H);
            float hovT = aval("cfgrow:" + cfg, hov ? 1f : 0f, 12f);
            rect(ctx, rx, y, rowW, LIST_ROW_H, SET_R,
                    lerpColor(withAlpha(C_CARD, (int)(255f * fade * rowT)), withAlpha(C_CARD_HOV, (int)(255f * fade)), hovT));
            text(ctx, Fonts.MEDIUM, cfg, rx + 8f, y + 9f, withAlpha(C_TEXT, (int)(255f * fade * rowT)), 8f);
            float loadHovT = aval("cfgload:" + cfg, hit(lastMouseX, lastMouseY, rx + rowW - 74f, y, 36f, LIST_ROW_H) ? 1f : 0f, 14f);
            float delHovT  = aval("cfgdel:"  + cfg, hit(lastMouseX, lastMouseY, rx + rowW - 32f, y, 28f, LIST_ROW_H) ? 1f : 0f, 14f);
            text(ctx, Fonts.BOLD, "LOAD", rx + rowW - 70f, y + 9f,
                    lerpColor(withAlpha(0xFF9BC7FF, (int)(255f * fade)), withAlpha(0xFFFFFFFF, (int)(255f * fade)), loadHovT), 7f);
            text(ctx, Fonts.BOLD, "DEL",  rx + rowW - 30f, y + 9f,
                    lerpColor(withAlpha(0xFFFF7070, (int)(255f * fade)), withAlpha(0xFFFFFFFF, (int)(255f * fade)), delHovT), 7f);
            y += LIST_ROW_H + LIST_ROW_GAP;
        }
        if (configs.length == 0) {
            String msg = "Нет сохранённых конфигов";
            float mw = Fonts.MEDIUM.getWidth(msg, 8f);
            text(ctx, Fonts.MEDIUM, msg, centerX - mw / 2f, y + 4f, withAlpha(C_TEXT_DIM, (int)(180f * fade)), 8f);
        }
    }

    // ─── Мини-инпут и мини-кнопка для Friends/Markers/Config ────────────

    private void drawInput(DrawContext ctx, float x, float y, float w, boolean focused, String value, String placeholder, float fade) {
        float focusT = aval("inputfocus:" + x + ":" + y, focused ? 1f : 0f, 14f);
        if (focusT > 0.02f) {
            rect(ctx, x - 1.5f, y - 1.5f, w + 3f, INPUT_H + 3f, INPUT_R + 1.5f, withAlpha(theme(), (int)(255f * fade * focusT)));
        }
        rect(ctx, x, y, w, INPUT_H, INPUT_R, withAlpha(focused ? C_CARD_HOV : C_CARD, (int)(255f * fade)));
        boolean ph = value.isEmpty() && !focused;
        String shown = ph ? placeholder : value;
        while (Fonts.MEDIUM.getWidth(shown, 8f) > w - 12f && shown.length() > 1) shown = shown.substring(1);
        text(ctx, Fonts.MEDIUM, shown, x + 6f, y + 7f,
                withAlpha(ph ? C_TEXT_DIM : C_TEXT, (int)(255f * fade)), 8f);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            rect(ctx, x + 6f + Fonts.MEDIUM.getWidth(shown, 8f) + 1f, y + 4f, 1f, INPUT_H - 8f, 0f,
                    withAlpha(C_TEXT, (int)(255f * fade)));
        }
    }

    private void drawButton(DrawContext ctx, float x, float y, float w, String label, String animKey, float fade) {
        boolean hov = hit(lastMouseX, lastMouseY, x, y, w, INPUT_H);
        float hovT = aval("btn:" + animKey, hov ? 1f : 0f, 14f);
        // блеск, проезжающий по кнопке при наведении
        float shine = aval("btnshine:" + animKey, hov ? pulse(now(), 900L) : 0f, 6f);
        rect(ctx, x, y, w, INPUT_H, SET_R, lerpColor(themeA((int)(190f * fade)), withAlpha(theme(), (int)(255f * fade)), hovT));
        if (shine > 0.02f) {
            rect(ctx, x + 3f + shine * (w - 10f), y + 2f, 1.5f, INPUT_H - 4f, 0.75f,
                    withAlpha(C_TEXT, (int)(70f * fade * shine)));
        }
        float lw = Fonts.BOLD.getWidth(label, 7.5f);
        text(ctx, Fonts.BOLD, label, x + w / 2f - lw / 2f, y + 7f, withAlpha(C_TEXT, (int)(255f * fade)), 7.5f);
    }

    /** Снимает фокус со всех текстовых полей New-стиля (кроме поиска, который управляется отдельно). */
    private void unfocusAllInputs() {
        ClickGuiRenderer.friendInputFocused = false;
        ClickGuiRenderer.markerFocusedField = 0;
        ClickGuiRenderer.configInputFocused = false;
        ClickGuiRenderer.editingStringSetting = null;
    }

    /** Сбросить «одноразовые» анимации появления при смене категории. */
    private void resetEntryAnimations() {
        anims.remove("catfade");
        anims.remove("headerunderline");
        anims.keySet().removeIf(k -> k.startsWith("rowin:"));
    }

    // Последняя известная позиция курсора — нужна рендеру не-модульных вкладок
    private int lastMouseX, lastMouseY;

    // ═════════════════════════════════════════════════════════════════
    //  Ввод: клики
    // ═════════════════════════════════════════════════════════════════

    public boolean mouseClicked(double mx, double my, int button, Window window, ClickGuiState state) {
        float fx = frameX(window), fy = frameY(window);

        float searchX = fx + (FRAME_W - SEARCH_W) / 2f;
        float searchY = fy + FRAME_H + SEARCH_GAP;
        if (hit(mx, my, searchX, searchY, SEARCH_W, SEARCH_H)) {
            ClickGuiRenderer.searchInputFocused = true;
            unfocusAllInputs();
            return true;
        }
        ClickGuiRenderer.searchInputFocused = false;

        // Окно инструкции Hugging Face (перекрывает всё, клик по крестику или внутри)
        if (hfWindowOpen && handleHfWindowClick(mx, my, fx, fy)) return true;

        if (!hit(mx, my, fx, fy, FRAME_W, FRAME_H)) { unfocusAllInputs(); return false; }

        // ── Навбар ──────────────────────────────────────────────────
        float pillW = navPillW();
        float pillH = navPillH();
        float pillX = fx + (FRAME_W - pillW) / 2f;
        float pillY = navPillY(fy);

        // Боковые кнопки: слева — чат DARKA-0.1, справа — настройки
        float sideW = pillH;
        float sparkleX = pillX - NAV_SIDE_GAP - sideW;
        float gridX    = pillX + pillW + NAV_SIDE_GAP;
        if (hit(mx, my, sparkleX, pillY, sideW, pillH)) {
            if (viewMode != ViewMode.DARKA) {
                viewMode = ViewMode.DARKA;
                expandedModule = null;
                ClickGuiRenderer.searchInputFocused = false;
                unfocusAllInputs();
            }
            return true;
        }
        if (hit(mx, my, gridX, pillY, sideW, pillH)) {
            if (viewMode != ViewMode.SETTINGS) {
                viewMode = ViewMode.SETTINGS;
                expandedModule = null;
                ClickGuiRenderer.searchInputFocused = false;
                unfocusAllInputs();
            }
            return true;
        }

        if (hit(mx, my, pillX, pillY, pillW, pillH)) {
            float ix = pillX + NAV_PAD;
            float slotY = pillY + NAV_PAD;
            for (int i = 0; i < NAV_COUNT; i++) {
                if (hit(mx, my, ix, slotY, NAV_SLOT_W, NAV_H)) {
                    Category c = NAV_CATS[i];
                    if (ClickGuiRenderer.currentCategory != c) {
                        ClickGuiRenderer.currentCategory = c;
                        state.setScroll(c, 0f);
                        state.setSettingsScroll(c, 0f);
                        ClickGuiRenderer.openedSettingsModule = null;
                        ClickGuiRenderer.bindingSetting = null;
                        expandedModule = null;
                        viewMode = ViewMode.MODULES;
                        unfocusAllInputs();
                        resetEntryAnimations();
                    }
                    return true;
                }
                ix += NAV_SLOT_W;
            }
            return true;
        }

        // ── Клик по списку модулей ───────────────────────────────────
        Category cat = ClickGuiRenderer.currentCategory;
        float cx   = cardX(fx);
        float cw   = CARD_W;
        float lTop = listTop(fy);
        float lBot = listBottom(fy);

        // ── Спец-режимы обрабатываются отдельно ──
        if (viewMode == ViewMode.DARKA)  return handleDarkaClick(mx, my, fx, fy);
        if (viewMode == ViewMode.SETTINGS) return handleSettingsClick(mx, my, fx, fy);

        // Окно инструкции Hugging Face (перекрывает всё, клик по крестику)
        if (hfWindowOpen && handleHfWindowClick(mx, my, fx, fy)) return true;

        if (hit(mx, my, cx, lTop, (CARD_W + CARD_GAP) * GRID_COLS - CARD_GAP, lBot - lTop)) {
            if (!ClickGuiRenderer.isModuleTab(cat)) {
                return handleNonModuleClick(mx, my, cx, cw, lTop, state, cat);
            }
            float scroll = frameScroll;
            List<Module> mods = ClickGuiRenderer.visibleModules(state);

            // ── Сначала проверяем клик по панели настроек развёрнутого модуля ──
            int expIdx = indexOfExpanded(mods);
            if (expIdx >= 0) {
                Module em = mods.get(expIdx);
                int ecol = expIdx % GRID_COLS, erow = expIdx / GRID_COLS;
                float ecardX = cardX(fx, ecol);
                float erowY = lTop + CARD_LIST_TOP_PAD - scroll + erow * gridRowH();
                float boxY = erowY + CARD_H + EXPAND_GAP;
                float boxH = expandPanelContentH(em);
                if (hit(mx, my, ecardX, boxY, CARD_W, boxH)) {
                    float sw = CARD_W - SUB_PAD_X * 2f;
                    float sy = boxY + SUB_PAD_TOP;
                    for (Setting<?> s : em.getSettings()) {
                        if (!s.isVisible()) continue;
                        float sh = settingHeight(s);
                        if (hit(mx, my, ecardX + SUB_PAD_X, sy, sw, sh)) {
                            clickSetting(s, mx, my, ecardX + SUB_PAD_X, sy, sw);
                            return true;
                        }
                        sy += sh + SET_GAP;
                    }
                    return true; // клик внутри панели, но мимо строк — просто поглощаем
                }
            }

            int row = 0;
            for (Module m : mods) {
                int col = row % GRID_COLS, rIdx = row / GRID_COLS;
                float cardX = cardX(fx, col);
                float y = lTop + CARD_LIST_TOP_PAD - scroll + rIdx * gridRowH();
                row++;
                if (!hit(mx, my, cardX, y, CARD_W, CARD_H)) continue;

                cardPressTimes.put(m.getName(), now());

                // Основной тогл модуля (правый верхний угол, левее шеврона)
                if (hit(mx, my, cardX + CARD_W - 58f, y + 11f, 36f, 20f)) {
                    m.setToggled(!m.isToggled());
                    playToggleSound(m.isToggled());
                    toggleTimes.put(m.getName(), now());
                    return true;
                }

                // Шеврон — разворачивает/сворачивает панель настроек прямо под карточкой
                if (hit(mx, my, cardX + CARD_W - 24f, y + 4f, 20f, 22f)) {
                    expandedModule = m.getName().equals(expandedModule) ? null : m.getName();
                    return true;
                }

                // Остальная часть карточки — тоже разворачивает панель настроек
                expandedModule = m.getName().equals(expandedModule) ? null : m.getName();
                return true;
            }
            return true;
        }

        return true;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Действия: Friends / Markers / Config
    // ═════════════════════════════════════════════════════════════════

    private void addCurrentFriend() {
        String name = ClickGuiRenderer.friendNameBuffer.toString().trim();
        if (name.isEmpty()) { setFriendStatus("Введите ник игрока"); return; }
        var mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getGameProfile().getName().equalsIgnoreCase(name)) {
            setFriendStatus("Нельзя добавить себя");
            return;
        }
        for (String f : dev.darkvisuals.client.managers.FriendsManager.getFriends()) {
            if (f.equalsIgnoreCase(name)) { setFriendStatus("Уже в друзьях: " + f); return; }
        }
        dev.darkvisuals.client.managers.FriendsManager.addFriend(name);
        ClickGuiRenderer.friendNameBuffer.setLength(0);
        setFriendStatus("Добавлен: " + name);
    }

    private void addCurrentMarker() {
        String name = ClickGuiRenderer.markerNameBuffer.toString().trim();
        String xs = ClickGuiRenderer.markerXBuffer.toString().trim();
        String zs = ClickGuiRenderer.markerZBuffer.toString().trim();
        if (xs.isEmpty() || zs.isEmpty()) { setMarkerStatus("Введите X и Z"); return; }
        double x, z;
        try { x = Double.parseDouble(xs); z = Double.parseDouble(zs); }
        catch (NumberFormatException e) { setMarkerStatus("Неверные координаты"); return; }
        if (name.isEmpty()) name = (int) x + " " + (int) z;
        dev.darkvisuals.client.managers.WaypointManager.add(name, new net.minecraft.util.math.Vec3d(x + 0.5, 60.0, z + 0.5));
        setMarkerStatus("Добавлена: " + name);
        ClickGuiRenderer.markerNameBuffer.setLength(0);
        ClickGuiRenderer.markerXBuffer.setLength(0);
        ClickGuiRenderer.markerZBuffer.setLength(0);
        ClickGuiRenderer.markerFocusedField = 0;
    }

    private void addMarkerHere() {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) { setMarkerStatus("Игрок не в мире"); return; }
        String name = ClickGuiRenderer.markerNameBuffer.toString().trim();
        var pos = mc.player.getPos();
        if (name.isEmpty()) name = (int) pos.x + " " + (int) pos.z;
        dev.darkvisuals.client.managers.WaypointManager.add(name, pos);
        setMarkerStatus("Добавлена: " + name);
        ClickGuiRenderer.markerNameBuffer.setLength(0);
        ClickGuiRenderer.markerFocusedField = 0;
    }

    private void saveCurrentConfig() {
        String name = ClickGuiRenderer.configNameBuffer.toString().trim();
        if (name.isEmpty()) { setConfigStatus("Введите имя конфига"); return; }
        dev.darkvisuals.darkvisuals.getInstance().getConfigManager().saveConfig(name)
                .thenAccept(ok -> setConfigStatus(ok ? "Сохранено: " + name : "Ошибка сохранения: " + name));
    }

    private void setFriendStatus(String text) {
        onMain(() -> { ClickGuiRenderer.friendStatus = text; ClickGuiRenderer.friendStatusUntil = System.currentTimeMillis() + 3000L; });
    }

    private void setMarkerStatus(String text) {
        ClickGuiRenderer.markerStatus = text;
        ClickGuiRenderer.markerStatusUntil = System.currentTimeMillis() + 2500L;
    }

    private void setConfigStatus(String text) {
        onMain(() -> { ClickGuiRenderer.configStatus = text; ClickGuiRenderer.configStatusUntil = System.currentTimeMillis() + 3000L; });
    }

    private void onMain(Runnable r) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) r.run(); else client.execute(r);
    }

    private void clickSetting(Setting<?> s, double mx, double my, float sx, float sy, float sw) {
        if (s instanceof ListSetting ls) {
            float px = sx, py = sy + 12f;
            for (BooleanSetting opt : ls.getValue()) {
                String lbl = net.minecraft.client.resource.language.I18n.translate(opt.getName());
                float pw = Fonts.MEDIUM.getWidth(lbl, 7f) + 10f;
                if (hit(mx, my, px, py, pw, 12f)) {
                    if (ls.isSingleSelect()) {
                        if (!opt.getValue()) { ls.getValue().forEach(o -> o.setValue(false)); opt.setValue(true); }
                    } else opt.setValue(!opt.getValue());
                    return;
                }
                px += pw + 3f;
                if (px > sx + sw - 20f) { px = sx; py += 15f; }
            }
        } else if (s instanceof BooleanSetting b) {
            b.setValue(!b.getValue());
            playToggleSound(b.getValue());
        } else if (s instanceof NumberSetting n) {
            ClickGuiRenderer.draggingSlider = n;
            ClickGuiRenderer.sliderTrackX  = sx;
            ClickGuiRenderer.sliderTrackW  = sw;
            applySlider(n, mx);
        } else if (s instanceof BindSetting bs) ClickGuiRenderer.bindingSetting = bs;
        else if (s instanceof ButtonSetting btn) btn.click();
        else if (s instanceof EnumSetting<?> es) es.increaseEnum();
        else if (s instanceof StringSetting ss) ClickGuiRenderer.editingStringSetting = ss;
    }

    private boolean handleNonModuleClick(double mx, double my, float cx, float cw,
                                         float lTop, ClickGuiState state, Category cat) {
        float y = lTop + CARD_LIST_TOP_PAD - state.getScroll(cat);

        if (cat == Category.Theme) {
            if (hit(mx, my, cx, y, cw, 24f)) {
                MinecraftClient.getInstance().setScreen(new dev.darkvisuals.client.ui.colorgui.CustomThemeScreen());
                return true;
            }
            y += 24f + CARD_GAP;
            float thW = CARD_W, thH = CARD_H * 0.75f;
            var themes = ClickGuiRenderer.getThemes();
            for (int i = 0; i < themes.length; i++) {
                float tx = cx + (i % 2) * (thW + CARD_GAP);
                float ty = y + (i / 2) * (thH + CARD_GAP);
                if (hit(mx, my, tx, ty, thW, thH)) {
                    dev.darkvisuals.client.managers.ThemeManager.getInstance().setTheme(themes[i]);
                    ClickGuiRenderer.themeColor = ClickGuiRenderer.liveThemeColor();
                    return true;
                }
            }
            return true;
        }

        if (cat == Category.Friends) {
            float inputW = cw - BTN_W - 6f, addX = cx + inputW + 6f;
            if (hit(mx, my, cx, y, inputW, INPUT_H)) {
                unfocusAllInputs();
                ClickGuiRenderer.friendInputFocused = true;
                return true;
            }
            if (hit(mx, my, addX, y, BTN_W, INPUT_H)) { addCurrentFriend(); return true; }
            y += INPUT_H + ROWSET_GAP;
            for (String f : dev.darkvisuals.client.managers.FriendsManager.getFriends()) {
                if (hit(mx, my, cx + cw - 32f, y, 28f, LIST_ROW_H)) {
                    dev.darkvisuals.client.managers.FriendsManager.removeFriend(f);
                    setFriendStatus("Удалён: " + f);
                    return true;
                }
                y += LIST_ROW_H + LIST_ROW_GAP;
            }
            unfocusAllInputs();
            return true;
        }

        if (cat == Category.Markers) {
            float addW = 34f, hereW = 36f, coordW = 34f, gap = 4f;
            float nameW = cw - coordW * 2f - addW - hereW - gap * 4f;
            float xX = cx + nameW + gap, zX = xX + coordW + gap;
            float addX = zX + coordW + gap, hereX = addX + addW + gap;
            if (hit(mx, my, cx, y, nameW, INPUT_H)) { unfocusAllInputs(); ClickGuiRenderer.markerFocusedField = 1; return true; }
            if (hit(mx, my, xX, y, coordW, INPUT_H)) { unfocusAllInputs(); ClickGuiRenderer.markerFocusedField = 2; return true; }
            if (hit(mx, my, zX, y, coordW, INPUT_H)) { unfocusAllInputs(); ClickGuiRenderer.markerFocusedField = 3; return true; }
            if (hit(mx, my, addX, y, addW, INPUT_H)) { addCurrentMarker(); return true; }
            if (hit(mx, my, hereX, y, hereW, INPUT_H)) { addMarkerHere(); return true; }
            y += INPUT_H + ROWSET_GAP;
            for (var w : dev.darkvisuals.client.managers.WaypointManager.list()) {
                if (hit(mx, my, cx + cw - 32f, y, 28f, LIST_ROW_H)) {
                    dev.darkvisuals.client.managers.WaypointManager.remove(w.name);
                    setMarkerStatus("Удалена: " + w.name);
                    return true;
                }
                y += LIST_ROW_H + LIST_ROW_GAP;
            }
            unfocusAllInputs();
            return true;
        }

        if (cat == Category.Config) {
            float centerX = cx + cw / 2f;
            float inputW = Math.min(cw - BTN_W - 6f, 240f);
            float rowW = inputW + 6f + BTN_W;
            float rowX = centerX - rowW / 2f;
            float saveX = rowX + inputW + 6f;
            if (hit(mx, my, rowX, y, inputW, INPUT_H)) { unfocusAllInputs(); ClickGuiRenderer.configInputFocused = true; return true; }
            if (hit(mx, my, saveX, y, BTN_W, INPUT_H)) { saveCurrentConfig(); return true; }
            y += INPUT_H + ROWSET_GAP;
            var cm = dev.darkvisuals.darkvisuals.getInstance().getConfigManager();
            for (String cfg : cm.getConfigList()) {
                if (hit(mx, my, rowX + rowW - 74f, y, 36f, LIST_ROW_H)) {
                    cm.loadConfig(cfg).thenAccept(ok -> setConfigStatus(ok ? "Загружено: " + cfg : "Ошибка загрузки: " + cfg));
                    return true;
                }
                if (hit(mx, my, rowX + rowW - 32f, y, 28f, LIST_ROW_H)) {
                    boolean ok = cm.deleteConfig(cfg);
                    setConfigStatus(ok ? "Удалено: " + cfg : "Ошибка удаления: " + cfg);
                    return true;
                }
                y += LIST_ROW_H + LIST_ROW_GAP;
            }
            unfocusAllInputs();
            return true;
        }
        return true;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Клики: чат DARKA-0.1
    // ═════════════════════════════════════════════════════════════════

    private boolean handleDarkaClick(double mx, double my, float fx, float fy) {
        float cx = cardX(fx);
        float cw = (CARD_W + CARD_GAP) * GRID_COLS - CARD_GAP;
        float lBot = listBottom(fy);
        float inY = lBot - CHAT_INPUT_H;

        // Кнопка отправки
        if (hit(mx, my, cx + cw - CHAT_SEND_W, inY, CHAT_SEND_W, CHAT_INPUT_H)) {
            sendDarkaMessage();
            return true;
        }
        // Поле ввода
        if (hit(mx, my, cx, inY, cw - CHAT_SEND_W - CHAT_GAP, CHAT_INPUT_H)) {
            darkaInputFocused = true;
            unfocusAllInputs();
            return true;
        }
        darkaInputFocused = false;
        return true;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Клики: настройки ClickGUI + DARKA-0.1
    // ═════════════════════════════════════════════════════════════════

    private boolean handleSettingsClick(double mx, double my, float fx, float fy) {
        var ui = dev.darkvisuals.darkvisuals.getInstance().getModuleManager()
                .getModule(dev.darkvisuals.modules.impl.render.UI.class);

        float cx = cardX(fx);
        float cw = (CARD_W + CARD_GAP) * GRID_COLS - CARD_GAP;
        float lTop = listTop(fy) + 4f;

        // Окно инструкции перекрывает — обрабатываем первым
        if (hfWindowOpen && handleHfWindowClick(mx, my, fx, fy)) return true;

        // ── Строка "Звук при включении функции" ──
        float soundRowY = lTop + 20f;
        if (ui != null && hit(mx, my, cx, soundRowY, cw, 30f)) {
            // клик по тоглу — переключаем, по строке — тоже
            ui.setGuiSoundInverted();
            playToggleSound(ui.isGuiSound());
            return true;
        }

        // ── API-ключ + кнопка "i" ──
        float keyRowY = soundRowY + 30f + 8f + 6f + 20f;
        float inputW = cw - 30f - 26f;
        if (hit(mx, my, cx, keyRowY, inputW, INPUT_H)) {
            darkaKeyFocused = true;
            unfocusAllInputs();
            return true;
        }
        // круглая кнопка "i"
        float iX = cx + inputW + 6f;
        if (hit(mx, my, iX, keyRowY, 22f, 22f)) {
            hfWindowOpen = true;
            hfScroll = 0f;
            return true;
        }

        darkaKeyFocused = false;
        return true;
    }

    // ═════════════════════════════════════════════════════════════════
    //  Клики: окно инструкции Hugging Face
    // ═════════════════════════════════════════════════════════════════

    private boolean handleHfWindowClick(double mx, double my, float fx, float fy) {
        float hx = fx + FRAME_W + 12f;
        float hy = fy;
        if (!hit(mx, my, hx, hy, HF_W, HF_H)) return false;

        // крестик
        float closeX = hx + HF_W - 24f, closeY = hy + 8f;
        if (hit(mx, my, closeX, closeY, 18f, 18f)) {
            hfWindowOpen = false;
            return true;
        }
        return true; // клик внутри окна — просто поглощаем
    }

    // ═════════════════════════════════════════════════════════════════
    //  Скролл
    // ═════════════════════════════════════════════════════════════════

    public boolean scroll(double mx, double my, double amount, Window window, ClickGuiState state) {
        float fx = frameX(window), fy = frameY(window);
        if (!hit(mx, my, fx, fy, FRAME_W, FRAME_H)) return false;

        Category cat = ClickGuiRenderer.currentCategory;

        float max = maxScroll(state, cat);
        float cur = state.getScroll(cat);
        state.setScroll(cat, Math.max(0f, Math.min(max, cur - (float)amount * 18f)));
        return true;
    }

    private float maxScroll(ClickGuiState state, Category cat) {
        float visible = listBottom(0) - listTop(0) - CARD_LIST_TOP_PAD;
        float content;
        if (!ClickGuiRenderer.isModuleTab(cat)) {
            if (cat == Category.Theme) {
                int thRows = (ClickGuiRenderer.getThemes().length + 1) / 2;
                content = 24f + CARD_GAP + thRows * (CARD_H * 0.75f + CARD_GAP);
            } else if (cat == Category.Friends) {
                content = INPUT_H + ROWSET_GAP
                        + dev.darkvisuals.client.managers.FriendsManager.getFriends().size() * (LIST_ROW_H + LIST_ROW_GAP);
            } else if (cat == Category.Markers) {
                content = INPUT_H + ROWSET_GAP
                        + dev.darkvisuals.client.managers.WaypointManager.list().size() * (LIST_ROW_H + LIST_ROW_GAP);
            } else {
                content = INPUT_H + ROWSET_GAP
                        + dev.darkvisuals.darkvisuals.getInstance().getConfigManager().getConfigList().length * (LIST_ROW_H + LIST_ROW_GAP);
            }
        } else {
            List<Module> mods = ClickGuiRenderer.visibleModules(state);
            int rows = (mods.size() + GRID_COLS - 1) / GRID_COLS;
            content = rows * gridRowH();
            // Если развёрнута карточка в последней строке — панель тоглов добавляет высоту сетке
            int expIdx = indexOfExpanded(mods);
            if (expIdx >= 0 && expIdx / GRID_COLS == rows - 1) {
                content += EXPAND_GAP + expandPanelContentH(mods.get(expIdx));
            }
        }
        return Math.max(0f, content - visible);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Утилиты
    // ═════════════════════════════════════════════════════════════════

    private float aval(String key, float target, float speed) {
        SmoothAnimation a = anims.computeIfAbsent(key, k -> new SmoothAnimation(0f, speed));
        a.setTarget(target);
        return a.update();
    }

    /** New-стиль всегда использует фиксированный фиолетовый акцент, а не пользовательскую тему. */
    static int theme() {
        return C_ACCENT;
    }

    private static int themeA(int alpha) {
        return (alpha << 24) | (theme() & 0x00FFFFFF);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float t) {
        t = clamp01(t);
        return (lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, t) << 24)
             | (lerp((from >> 16)  & 0xFF, (to >> 16)  & 0xFF, t) << 16)
             | (lerp((from >>  8)  & 0xFF, (to >>  8)  & 0xFF, t) <<  8)
             |  lerp( from         & 0xFF,  to          & 0xFF, t);
    }

    private static int lerp(int a, int b, float t) { return (int)(a + (b - a) * t); }
    private static float lerpFloat(float a, float b, float t) { return a + (b - a) * clamp01(t); }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
    }

    private Identifier navIcon(int i) {
        return switch (NAV_CATS[i]) {
            case Render  -> ICON_RENDER;
            case Utility -> ICON_UTILITY;
            case Friends -> ICON_FRIENDS;
            case Markers -> ICON_MARKERS;
            case Theme   -> ICON_THEME;
            case Config  -> ICON_CONFIG;
            default      -> null;
        };
    }

    private String catLabel(Category c) {
        return switch (c) {
            case Render  -> "Visuals";
            case Utility -> "Utility";
            case Friends -> "Friends";
            case Markers -> "Markers";
            case Theme   -> "Themes";
            case Config  -> "Configs";
            default      -> c.name();
        };
    }

    private String fmtNum(float v) {
        return v == (int)v ? String.valueOf((int)v) : String.format("%.1f", v);
    }

    private String keyName(int key) {
        if (key <= 0) return "NONE";
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_ESCAPE        -> "ESC";
            case GLFW.GLFW_KEY_LEFT_SHIFT    -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT   -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL  -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT      -> "LALT";
            default                           -> "KEY_" + key;
        };
    }

    private void applySlider(NumberSetting s, double mx) {
        float p   = clamp01((float)((mx - ClickGuiRenderer.sliderTrackX) / ClickGuiRenderer.sliderTrackW));
        float val = s.getMin() + p * (s.getMax() - s.getMin());
        if (s.getIncrement() > 0) val = Math.round(val / s.getIncrement()) * s.getIncrement();
        s.setValue(Math.max(s.getMin(), Math.min(s.getMax(), val)));
    }

    // ─── Низкоуровневый рендеринг ─────────────────────────────────────

    private void rect(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        if (((color >>> 24) & 0xFF) == 0 || w <= 0 || h <= 0) return;
        Render2D.drawRoundedRect(ctx.getMatrices(), x, y, w, h,
                Math.min(r, Math.min(w, h) / 2f), Render2D.cachedColor(color));
    }

    private void text(DrawContext ctx, Font fnt, String s, float x, float y, int color, float size) {
        if (s == null || s.isBlank()) return;
        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(0, 0, 150);
        try {
            Render2D.drawFont(ms, fnt.getFont(size), s, x, y, Render2D.cachedColor(color));
        } catch (IllegalStateException ignored) {}
        ms.pop();
    }

    /** Рисует PNG-иконку (те же текстуры, что использует ClickGuiRenderer) с тинтом. */
    private void tex(DrawContext ctx, Identifier id, float x, float y, float size, int tint) {
        RenderSystem.setShaderColor(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f,
                (tint & 0xFF) / 255f, ((tint >>> 24) & 0xFF) / 255f);
        ctx.drawTexture(RenderLayer::getGuiTextured, id, (int) x, (int) y, 0, 0,
                (int) size, (int) size, (int) size, (int) size);
        ctx.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}