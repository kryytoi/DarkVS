package dev.darkvisuals.client.ui.clickgui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.util.animations.SmoothAnimation;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Font;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.impl.render.UI;
import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.modules.settings.impl.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.Window;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.*;

public class NounClickGuiRenderer {

    // ─── Главный фрейм ────────────────────────────────────────────────
    public static final float FRAME_W  = 476f;
    public static final float FRAME_H  = 326f;
    public static final float FRAME_R  = 22f;

    // ─── Сайдбар и Шапка ──────────────────────────────────────────────
    private static final float SIDEBAR_W  = 46f;
    private static final float HEADER_H   = 38f;
    private static final float CONTENT_PAD = 12f;
    private static final float SIDEBAR_SLOT_H = 42f;

    // ─── Карточки модулей (сетка из двух колонок) ──────────────────────
    private static final int   GRID_COLS   = 2;
    private static final float CARD_GAP    = 8f;
    private static final float CARD_W      = (FRAME_W - SIDEBAR_W - CONTENT_PAD * 2f - CARD_GAP) / GRID_COLS;
    private static final float CARD_H      = 64f;
    private static final float CARD_R      = 12f;
    private static final float EXPAND_GAP  = 6f;
    private static final float SUB_PAD_TOP = 11f;
    private static final float SUB_PAD_BOT = 11f;
    private static final float SUB_PAD_X   = 12f;

    // ─── Настройки ───────────────────────────────────────────────────
    private static final float SET_R       = 5f;
    private static final float SET_GAP     = 5f;

    // ─── Верхняя панель: кнопки AI, Grid и Search ─────────────────────
    private static final float AI_BTN_W    = 28f;
    private static final float AI_BTN_H    = 22f;
    private static final float GRID_BTN_W  = 22f;
    private static final float GRID_BTN_H  = 22f;
    private static final float SEARCH_W    = 156f;
    private static final float SEARCH_H    = 22f;
    private static final float SEARCH_R    = 11f;

    // ─── Цвета ───────────────────────────────────────────────────────
    private static final int C_BG          = 0xFF121215; // Тёмный матовый фон главного окна
    private static final int C_CARD        = 0xFF1C1C20; // Фон карточки модуля (стиль New)
    private static final int C_CARD_HOV    = 0xFF232328;
    private static final int C_LINE        = 0x1AFFFFFF; // Тонкие разделители
    private static final int C_SIDE_DIV    = 0xFF2A2A30; // Серая разделительная линия сайдбара
    private static final int C_SIDE_HOV    = 0xFF26262C; // Серый фон кнопки раздела при наведении
    private static final int C_TEXT        = 0xFFFFFFFF;
    private static final int C_TEXT_DIM    = 0xFF8E8E95;
    private static final int C_ICON_DIM    = 0xFF7A7A82;

    private static final int C_ACCENT      = 0xFF7C3AED; // Фиолетовый акцент
    private static final int C_THUMB       = 0xFF8B5CF6; // Бегунок тогла

    // ─── Вспомогательные инпуты (Friends/Markers/Config) ───────────────
    private static final float INPUT_H      = 22f;
    private static final float INPUT_R      = 7f;
    private static final float BTN_W        = 42f;
    private static final float LIST_ROW_H   = 24f;
    private static final float LIST_ROW_GAP = 5f;
    private static final float ROWSET_GAP   = 8f;

    // ─── Иконки и текстуры ─────────────────────────────────────────────
    private static final Identifier ICON_LOGO    = Identifier.of("darkvisuals", "textures/logo.png");
    private static final Identifier ICON_SKULL   = Identifier.of("darkvisuals", "hud/skull.png");
    private static final Identifier ICON_AMONGUS = Identifier.of("darkvisuals", "hud/amongus.png");
    private static final Identifier ICON_CROWN   = Identifier.of("darkvisuals", "hud/crown.png");
    private static final Identifier ICON_SEARCH  = Identifier.of("darkvisuals", "hud/search.png");

    private static final Identifier ICON_RENDER  = Identifier.of("darkvisuals", "textures/clickgui/render.png");
    private static final Identifier ICON_UTILITY = Identifier.of("darkvisuals", "textures/clickgui/utilities.png");
    private static final Identifier ICON_FRIENDS = Identifier.of("darkvisuals", "hud/friends.png");
    private static final Identifier ICON_CONFIG  = Identifier.of("darkvisuals", "hud/cloud.png");
    private static final Identifier ICON_THEME   = Identifier.of("darkvisuals", "hud/theme.png");
    private static final Identifier ICON_MARKERS = Identifier.of("darkvisuals", "textures/tags.png");

    private static final Category[] SIDEBAR_CATS = {
            Category.Render, Category.Utility, Category.Friends,
            Category.Config, Category.Theme, Category.Markers
    };

    public enum ViewMode { MODULES, DARKA, SETTINGS }
    private ViewMode viewMode = ViewMode.MODULES;

    private boolean welcomeScreen = true;
    private String expandedModule = null;

    private final Map<String, SmoothAnimation> anims = new HashMap<>();
    private final SmoothAnimation openFade = new SmoothAnimation(0f, 10f);
    private final Map<String, Long> cardPressTimes = new HashMap<>();
    private final Map<String, Long> toggleTimes    = new HashMap<>();

    private float frameScroll = 0f;
    private float darkaScroll = 0f;
    private float settingsScroll = 0f;

    private final StringBuilder darkaInput = new StringBuilder();
    private boolean darkaInputFocused = false;
    private boolean darkaKeyFocused = false;
    private boolean hfWindowOpen = false;
    private float hfScroll = 0f;

    private float aiX, aiY, aiW, aiH;
    private float gridX, gridY, gridW, gridH;
    private float searchBoxX, searchBoxY, searchBoxW, searchBoxH;

    private float guiSoundRowX, guiSoundRowY, guiSoundRowW, guiSoundRowH;
    private float apiFieldX, apiFieldY, apiFieldW, apiFieldH;

    private int lastMouseX, lastMouseY;
    private String hoveredDesc = null;

    private static NounClickGuiRenderer instance;
    public static NounClickGuiRenderer getInstance() {
        if (instance == null) instance = new NounClickGuiRenderer();
        return instance;
    }

    private NounClickGuiRenderer() {}

    public static float frameX(Window w) { return (w.getScaledWidth()  - FRAME_W) / 2f; }
    public static float frameY(Window w) { return (w.getScaledHeight() - FRAME_H) / 2f; }

    private static float contentLeft(float fx) { return fx + SIDEBAR_W + CONTENT_PAD; }
    private static float contentWidth() { return FRAME_W - SIDEBAR_W - CONTENT_PAD * 2f; }

    private static float listTop(float fy) { return fy + HEADER_H + 6f; }
    private static float listBottom(float fy) { return fy + FRAME_H - 8f; }

    private static float cardX(float fx, int col) {
        return contentLeft(fx) + col * (CARD_W + CARD_GAP);
    }
    private static float gridRowH() { return CARD_H + CARD_GAP; }

    public static boolean hit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static long now() { return System.currentTimeMillis(); }

    private float expandPanelContentH(Module m) {
        float h = SUB_PAD_TOP + SUB_PAD_BOT;
        for (Setting<?> s : m.getSettings()) {
            if (!s.isVisible()) continue;
            h += settingHeight(s) + SET_GAP;
        }
        return Math.max(h - SET_GAP, SUB_PAD_TOP + SUB_PAD_BOT);
    }

    private int indexOfExpanded(List<Module> mods) {
        if (expandedModule == null) return -1;
        for (int i = 0; i < mods.size(); i++) {
            if (mods.get(i).getName().equals(expandedModule)) return i;
        }
        return -1;
    }

    public static boolean isLiquidGlass() {
        UI ui = darkvisuals.getInstance().getModuleManager().getModule(UI.class);
        return ui != null && ui.getUiMode() == UI.UIMode.LiquidGlass;
    }

    private static int glassTint(float alpha01) {
        return withAlpha(0xFFC8D7E6, (int) (Math.max(0f, Math.min(1f, alpha01)) * 255f));
    }

    private static final Map<String, SoundEvent> SOUND_CACHE = new HashMap<>();

    public static void playSound(String soundId) {
        UI ui = darkvisuals.getInstance().getModuleManager().getModule(UI.class);
        if (ui == null || !ui.isGuiSound()) return;

        String id = soundId.contains(":") ? soundId : "darkvisuals:" + soundId;
        try {
            SoundEvent sound = SOUND_CACHE.computeIfAbsent(id, i -> SoundEvent.of(Identifier.of(i)));
            MinecraftClient.getInstance().getSoundManager()
                    .play(PositionedSoundInstance.master(sound, 1.0f, 1.0f));
        } catch (Throwable ignored) {}
    }

    public static void playUiSound(boolean open) {
        playSound(open ? "clickguinon" : "clickguioff");
    }

    public static void playModuleSound(boolean enabled, Module m) {
        if (m != null && (m instanceof UI || m.getName().equalsIgnoreCase("UI"))) {
            return;
        }
        playSound(enabled ? "enable" : "disable");
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta,
                       Window window, ClickGuiState state, float openProgress) {
        if (window == null) return;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        hoveredDesc = null;

        openProgress = Math.max(0f, Math.min(1f, openProgress));
        if (openProgress <= 0.01f) return;

        openFade.setTarget(openProgress);
        float fade = openFade.update();

        float sw = window.getScaledWidth(), sh = window.getScaledHeight();
        float fx = frameX(window), fy = frameY(window);

        int dimAlpha = isLiquidGlass() ? (int) (90f * fade) : (int) (140f * fade);
        rect(context, 0, 0, sw, sh, 0f, withAlpha(0xFF000000, dimAlpha));

        float pop = aval("openpop", openProgress, 8f);
        float scale = 0.95f + 0.05f * easeOutBack(pop);

        var ms = context.getMatrices();
        ms.push();
        ms.translate(sw / 2f, sh / 2f, 0f);
        ms.scale(scale, scale, 1f);
        ms.translate(-sw / 2f, -sh / 2f, 0f);

        // ── Основа главного окна (матовая или жидкое стекло) ───────────
        if (isLiquidGlass()) {
            Render2D.drawGlass(ms, fx, fy, FRAME_W, FRAME_H, fade, FRAME_R, glassTint(0.20f * fade), 8f, 3f, 3.5f, 2f);
            Render2D.drawGradientRect(ms, fx + FRAME_R, fy + 0.5f, FRAME_W - FRAME_R * 2f, 1f,
                    new Color(255, 255, 255, (int) (120f * fade)),
                    new Color(255, 255, 255, (int) (20f * fade)), true);
        } else {
            rect(context, fx, fy, FRAME_W, FRAME_H, FRAME_R, withAlpha(C_BG, (int) (255f * fade)));
        }

        // ── Линии-разделители сайдбара и шапки ─────────────────────────
        int lineCol = isLiquidGlass() ? withAlpha(0xFFFFFFFF, (int) (26f * fade)) : withAlpha(C_LINE, (int) (255f * fade));
        fill(context, fx + SIDEBAR_W, fy, 1f, FRAME_H, lineCol);
        fill(context, fx, fy + HEADER_H, FRAME_W, 1f, lineCol);

        // ── Логотип в левом верхнем углу сайдбара ──────────────────────
        renderTopLeftLogo(context, fx, fy, fade);

        // ── Сайдбар с категориями и серыми разделителями ──────────────
        renderSidebar(context, fx, fy, fade);

        // ── Верхняя панель (Заголовок, AI, Grid, Поиск) ───────────────
        renderTopBar(context, fx, fy, fade);

        // ── Основное содержимое ───────────────────────────────────────
        if (welcomeScreen && !ClickGuiRenderer.isSearching() && viewMode == ViewMode.MODULES) {
            renderWelcomeScreen(context, fx, fy, fade);
        } else if (viewMode == ViewMode.DARKA) {
            renderDarkaChat(context, fx, fy, mouseX, mouseY, fade);
        } else if (viewMode == ViewMode.SETTINGS) {
            renderSettingsView(context, fx, fy, mouseX, mouseY, fade);
        } else {
            renderModules(context, fx, fy, mouseX, mouseY, state, fade);
            renderScrollBar(context, fx, fy, state, fade);
        }

        // ── Амонгус сверху справа ─────────────────────────────────────
        renderSittingAmongus(context, fx, fy, fade);

        // ── Корона слева сверху ───────────────────────────────────────
        renderFloatingCrown(context, fx, fy, fade);

        // Описание при наведении на функцию
        renderTooltip(context, fx, fy, fade);

        ms.pop();
    }

    private void renderFloatingCrown(DrawContext ctx, float fx, float fy, float fade) {
        float crownSize = 40f;
        float cx = fx - 12f;
        float cy = fy - 18f;

        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx + crownSize / 2f, cy + crownSize / 2f, 200f);
        ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(-22f));

        tex(ctx, ICON_CROWN, -crownSize / 2f, -crownSize / 2f, crownSize, crownSize,
                withAlpha(C_TEXT, (int) (255f * fade)));
        ms.pop();
    }

    private void renderSittingAmongus(DrawContext ctx, float fx, float fy, float fade) {
        float auW = 28f;
        float auH = 30f;
        float auX = fx + FRAME_W - 125f;
        float auY = fy - 23f;

        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(0, 0, 180f);
        tex(ctx, ICON_AMONGUS, auX, auY, auW, auH, withAlpha(C_TEXT, (int) (255f * fade)));
        ms.pop();
    }

    private void renderTopLeftLogo(DrawContext ctx, float fx, float fy, float fade) {
        float logoSize = 28f;
        float lx = fx + (SIDEBAR_W - logoSize) / 2f;
        float ly = fy + (HEADER_H - logoSize) / 2f;

        tex(ctx, ICON_LOGO, lx, ly, logoSize, logoSize, withAlpha(C_THUMB, (int) (255f * fade)));
    }

    private void renderWelcomeScreen(DrawContext ctx, float fx, float fy, float fade) {
        float cx = fx + SIDEBAR_W + (FRAME_W - SIDEBAR_W) / 2f;
        float cy = fy + HEADER_H + (FRAME_H - HEADER_H) / 2f;

        float skullSize = 56f;
        float sx = cx - skullSize / 2f;
        float sy = cy - 38f;

        var ms = ctx.getMatrices();
        ms.push();
        Render2D.drawBlurredRect(ms, sx - 8f, sy - 8f, skullSize + 16f, skullSize + 16f,
                skullSize / 2f, 16f, new Color(255, 255, 255, (int) (65f * fade)));

        tex(ctx, ICON_SKULL, sx, sy, skullSize, skullSize, withAlpha(C_TEXT, (int) (255f * fade)));
        ms.pop();

        String label = "Выбери раздел :<";
        float lw = Fonts.BOLD.getWidth(label, 13f);
        text(ctx, Fonts.BOLD, label, cx - lw / 2f, sy + skullSize + 18f,
                withAlpha(C_TEXT, (int) (255f * fade)), 13f);
    }

    private void renderSidebar(DrawContext ctx, float fx, float fy, float fade) {
        float startY = fy + HEADER_H;
        float step   = SIDEBAR_SLOT_H;
        float iconSize = 16f;

        int divCol = isLiquidGlass() ? withAlpha(0xFFFFFFFF, (int) (30f * fade)) : withAlpha(C_SIDE_DIV, (int) (255f * fade));
        fill(ctx, fx + 8f, startY, SIDEBAR_W - 16f, 1f, divCol);

        for (int i = 0; i < SIDEBAR_CATS.length; i++) {
            Category cat = SIDEBAR_CATS[i];
            boolean active = (ClickGuiRenderer.currentCategory == cat) && (viewMode == ViewMode.MODULES);
            float y = startY + i * step;

            boolean hov = hit(lastMouseX, lastMouseY, fx, y, SIDEBAR_W, step);
            float hovT = aval("sidehov:" + cat.name(), hov ? 1f : 0f, 12f);
            float selT = aval("sidesel:" + cat.name(), active ? 1f : 0f, 14f);

            if (hovT > 0.02f) {
                int hovBg = isLiquidGlass() ? withAlpha(0xFFFFFFFF, (int) (40f * fade * hovT)) : withAlpha(C_SIDE_HOV, (int) (255f * fade * hovT));
                rect(ctx, fx + 6f, y + 4f, SIDEBAR_W - 12f, step - 8f, 7f, hovBg);
            }

            if (selT > 0.02f) {
                float barH = 18f * selT;
                rect(ctx, fx + 3f, y + (step - barH) / 2f, 3f, barH, 1.5f,
                        withAlpha(C_ACCENT, (int) (255f * fade * selT)));
            }

            Identifier icon = iconForCategory(cat);
            int iconColor = active
                    ? withAlpha(C_TEXT, (int) (255f * fade))
                    : lerpColor(withAlpha(C_ICON_DIM, (int) (220f * fade)),
                    withAlpha(C_TEXT,     (int) (255f * fade)), hovT);

            if (icon != null) {
                float ix = fx + (SIDEBAR_W - iconSize) / 2f;
                float iy = y + (step - iconSize) / 2f;
                tex(ctx, icon, ix, iy, iconSize, iconSize, iconColor);
            }

            fill(ctx, fx + 8f, y + step, SIDEBAR_W - 16f, 1f, divCol);
        }
    }

    private void renderTopBar(DrawContext ctx, float fx, float fy, float fade) {
        String title = ClickGuiRenderer.isSearching() ? "Поиск" : catLabel(ClickGuiRenderer.currentCategory);
        text(ctx, Fonts.BOLD, title, fx + SIDEBAR_W + 14f, fy + 12f,
                withAlpha(C_TEXT, (int) (255f * fade)), 12.5f);

        searchBoxW = SEARCH_W;
        searchBoxH = SEARCH_H;
        searchBoxX = fx + FRAME_W - 12f - searchBoxW;
        searchBoxY = fy + (HEADER_H - searchBoxH) / 2f;

        gridW = GRID_BTN_W;
        gridH = GRID_BTN_H;
        gridX = searchBoxX - 8f - gridW;
        gridY = fy + (HEADER_H - gridH) / 2f;

        aiW = AI_BTN_W;
        aiH = AI_BTN_H;
        aiX = gridX - 6f - aiW;
        aiY = fy + (HEADER_H - aiH) / 2f;

        // Кнопка AI
        boolean aiActive = (viewMode == ViewMode.DARKA);
        boolean aiHov = hit(lastMouseX, lastMouseY, aiX, aiY, aiW, aiH);
        float aiT = aval("btnai", (aiHov || aiActive) ? 1f : 0f, 12f);
        rect(ctx, aiX, aiY, aiW, aiH, 6f,
                lerpColor(withAlpha(0xFF26262B, (int) (255f * fade)),
                        withAlpha(aiActive ? C_ACCENT : 0xFF323238, (int) (255f * fade)), aiT));
        float aiTextW = Fonts.BOLD.getWidth("AI", 9f);
        text(ctx, Fonts.BOLD, "AI", aiX + (aiW - aiTextW) / 2f, aiY + 6.5f,
                withAlpha(C_TEXT, (int) (255f * fade)), 9f);

        // Кнопка Grid (Настройки)
        boolean gridActive = (viewMode == ViewMode.SETTINGS);
        boolean gridHov = hit(lastMouseX, lastMouseY, gridX, gridY, gridW, gridH);
        float gridT = aval("btngrid", (gridHov || gridActive) ? 1f : 0f, 12f);
        rect(ctx, gridX, gridY, gridW, gridH, 6f,
                lerpColor(withAlpha(0xFF26262B, (int) (255f * fade)),
                        withAlpha(gridActive ? C_ACCENT : 0xFF323238, (int) (255f * fade)), gridT));
        drawGridGlyph(ctx, gridX + (gridW - 10f) / 2f, gridY + (gridH - 10f) / 2f,
                withAlpha(C_TEXT, (int) (255f * fade)));

        // Поле Поиска
        boolean focused = ClickGuiRenderer.searchInputFocused;
        float searchFocusT = aval("searchfocus", focused ? 1f : 0f, 12f);
        rect(ctx, searchBoxX, searchBoxY, searchBoxW, searchBoxH, SEARCH_R,
                lerpColor(withAlpha(0xFF1E1E22, (int) (255f * fade)),
                        withAlpha(0xFF25252B, (int) (255f * fade)), searchFocusT));

        String query = ClickGuiRenderer.searchBuffer.toString();
        boolean placeholder = query.isEmpty() && !focused;
        String display = placeholder ? "Поиск..." : query;

        float maxTextW = searchBoxW - 32f;
        String textToRender = display;
        if (!placeholder) {
            while (Fonts.MEDIUM.getWidth(textToRender, 8.5f) > maxTextW && textToRender.length() > 1) {
                textToRender = textToRender.substring(1);
            }
        }

        text(ctx, Fonts.MEDIUM, textToRender, searchBoxX + 10f, searchBoxY + 6.5f,
                placeholder ? withAlpha(C_TEXT_DIM, (int) (210f * fade)) : withAlpha(C_TEXT, (int) (255f * fade)), 8.5f);

        if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float cw = placeholder ? 0f : Fonts.MEDIUM.getWidth(textToRender, 8.5f);
            fill(ctx, searchBoxX + 10f + cw + 1f, searchBoxY + 5f, 1f, searchBoxH - 10f,
                    withAlpha(C_TEXT, (int) (240f * fade)));
        }

        if (!query.isEmpty()) {
            float closeW = Fonts.BOLD.getWidth("×", 9.5f);
            text(ctx, Fonts.BOLD, "×", searchBoxX + searchBoxW - 14f - closeW / 2f, searchBoxY + 5.5f,
                    withAlpha(C_TEXT_DIM, (int) (240f * fade)), 9.5f);
        } else {
            float searchIconSize = 11f;
            tex(ctx, ICON_SEARCH, searchBoxX + searchBoxW - 16f, searchBoxY + (searchBoxH - searchIconSize) / 2f,
                    searchIconSize, searchIconSize, withAlpha(C_TEXT_DIM, (int) (240f * fade)));
        }
    }

    private void renderModules(DrawContext ctx, float fx, float fy, int mx, int my,
                               ClickGuiState state, float fade) {
        Category cat = ClickGuiRenderer.currentCategory;
        float lTop = listTop(fy);
        float lBot = listBottom(fy);

        frameScroll = aval("scroll:" + cat.name(), state.getScroll(cat), 16f);

        if (!ClickGuiRenderer.isModuleTab(cat)) {
            float cx = contentLeft(fx);
            float cw = contentWidth();
            ctx.enableScissor((int) cx, (int) lTop, (int) (cx + cw), (int) lBot);
            float y = lTop + 4f - frameScroll;
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

        ctx.enableScissor((int) contentLeft(fx), (int) lTop, (int) (fx + FRAME_W - CONTENT_PAD), (int) lBot);

        int row = 0;
        for (Module m : mods) {
            int col = row % GRID_COLS, rIdx = row / GRID_COLS;
            float cX = cardX(fx, col);
            float y  = lTop + 4f - scroll + rIdx * gridRowH();
            row++;
            if (y + CARD_H < lTop) continue;
            if (y > lBot) break;

            boolean expanded = m.getName().equals(expandedModule);
            boolean hov = hit(mx, my, cX, y, CARD_W, CARD_H);

            float togT = aval("tog:" + m.getName(), m.isToggled() ? 1f : 0f, 12f);
            float hovT = aval("hov:" + m.getName(), hov ? 1f : 0f, 10f);
            float expT = aval("exp:" + m.getName(), expanded ? 1f : 0f, 12f);

            int cardBase = isLiquidGlass() ? withAlpha(0xFF1E2026, (int) (110f * fade)) : withAlpha(C_CARD, (int) (255f * fade));
            int cardHov  = isLiquidGlass() ? withAlpha(0xFF2B2E38, (int) (155f * fade)) : withAlpha(C_CARD_HOV, (int) (255f * fade));
            rect(ctx, cX, y, CARD_W, CARD_H, CARD_R, lerpColor(cardBase, cardHov, hovT));

            if (isLiquidGlass()) {
                fill(ctx, cX + 8f, y + 0.5f, CARD_W - 16f, 1f, withAlpha(0xFFFFFFFF, (int) (26f * fade)));
            }

            String name = I18n.translate(m.getName());
            text(ctx, Fonts.BOLD, name, cX + 14f, y + 11f,
                    withAlpha(C_TEXT, (int) (255f * fade)), 11.5f);

            int sepCol = isLiquidGlass() ? withAlpha(0xFFFFFFFF, (int) (24f * fade)) : withAlpha(C_LINE, (int) (220f * fade));
            rect(ctx, cX + 12f, y + 34f, CARD_W - 24f, 1f, 0f, sepCol);

            String desc = I18n.translate(m.getDescription());
            if (desc != null && !desc.isBlank()) {
                String line = desc;
                while (Fonts.MEDIUM.getWidth(line, 8.5f) > CARD_W - 28f && line.length() > 1) {
                    line = line.substring(0, line.length() - 1);
                }
                text(ctx, Fonts.MEDIUM, line, cX + 14f, y + CARD_H - 19f,
                        withAlpha(C_TEXT_DIM, (int) (220f * fade)), 8.5f);
            }

            drawChevron(ctx, cX + CARD_W - 18f, y + 15f, expT, fade);
            drawToggle(ctx, cX + CARD_W - 58f, y + 11f, togT, fade);

            if (hov && desc != null && !desc.isBlank()) {
                hoveredDesc = desc;
            }
        }

        if (expIdx >= 0 && expIdx < mods.size()) {
            Module em = mods.get(expIdx);
            float expFade = aval("exp:" + em.getName(), 1f, 12f);
            if (expFade > 0.02f) {
                int col = expIdx % GRID_COLS, erow = expIdx / GRID_COLS;
                float cX = cardX(fx, col);
                float rowY = lTop + 4f - scroll + erow * gridRowH();
                float boxY = rowY + CARD_H + EXPAND_GAP;
                float boxH = expandPanelContentH(em);

                var ms = ctx.getMatrices();
                ms.push();
                ms.translate(0, (1f - expFade) * -5f, 0f);

                int expBg = isLiquidGlass()
                        ? withAlpha(0xFF181920, (int) (130f * fade * expFade))
                        : lerpColor(withAlpha(C_CARD,     (int) (255f * fade * expFade)),
                        withAlpha(C_CARD_HOV, (int) (255f * fade * expFade)), 0.4f);

                rect(ctx, cX, boxY, CARD_W, boxH, CARD_R, expBg);

                if (isLiquidGlass()) {
                    fill(ctx, cX + 8f, boxY + 0.5f, CARD_W - 16f, 1f, withAlpha(0xFFFFFFFF, (int) (25f * fade * expFade)));
                }

                float sw = CARD_W - SUB_PAD_X * 2f;
                float sy = boxY + SUB_PAD_TOP;
                boolean any = false;
                for (Setting<?> s : em.getSettings()) {
                    if (!s.isVisible()) continue;
                    any = true;
                    renderSetting(ctx, s, cX + SUB_PAD_X, sy, sw, fade * expFade);
                    sy += settingHeight(s) + SET_GAP;
                }
                if (!any) {
                    text(ctx, Fonts.MEDIUM, "Нет настроек", cX + SUB_PAD_X, sy + 2f,
                            withAlpha(C_TEXT_DIM, (int) (200f * fade * expFade)), 8f);
                }
                ms.pop();
            }
        }

        if (mods.isEmpty()) {
            text(ctx, Fonts.MEDIUM, "Ничего не найдено", contentLeft(fx) + 8f, lTop + 8f,
                    withAlpha(C_TEXT_DIM, (int) (180f * fade)), 8.5f);
        }

        ctx.disableScissor();
    }

    private void renderSetting(DrawContext ctx, Setting<?> s, float sx, float y, float sw, float fade) {
        String name = I18n.translate(s.getName());

        if (s instanceof ListSetting ls) {
            text(ctx, Fonts.MEDIUM, name, sx, y + 2f, withAlpha(C_TEXT, (int) (255f * fade)), 7.5f);
            float px = sx, py = y + 12f;
            for (BooleanSetting opt : ls.getValue()) {
                String lbl = I18n.translate(opt.getName());
                float pw = Fonts.MEDIUM.getWidth(lbl, 7f) + 10f;
                float t  = aval("opt:" + System.identityHashCode(opt), opt.getValue() ? 1f : 0f, 12f);
                rect(ctx, px, py, pw, 12f, SET_R,
                        lerpColor(withAlpha(0xFF222227, (int) (255f * fade)),
                                withAlpha(C_ACCENT,   (int) (200f * fade)), t));
                text(ctx, Fonts.MEDIUM, lbl, px + 5f, py + 3f,
                        lerpColor(withAlpha(C_TEXT_DIM, (int) (255f * fade)),
                                withAlpha(C_TEXT,     (int) (255f * fade)), t), 7f);
                px += pw + 3f;
                if (px > sx + sw - 20f) { px = sx; py += 15f; }
            }
        } else if (s instanceof BooleanSetting b) {
            float t = aval("set:" + System.identityHashCode(s), b.getValue() ? 1f : 0f, 12f);
            text(ctx, Fonts.MEDIUM, name, sx, y + 2f,
                    lerpColor(withAlpha(C_TEXT_DIM, (int) (255f * fade)),
                            withAlpha(C_TEXT,     (int) (255f * fade)), t), 7.5f);
            drawToggle(ctx, sx + sw - 40f, y, t, fade);
        } else if (s instanceof NumberSetting n) {
            text(ctx, Fonts.MEDIUM, name + ": " + fmtNum(n.getValue()), sx, y + 1f,
                    withAlpha(C_TEXT, (int) (255f * fade)), 7.5f);
            float p  = clamp01((n.getValue() - n.getMin()) / (n.getMax() - n.getMin()));
            float sp = aval("set:" + System.identityHashCode(s), p, 16f);
            rect(ctx, sx, y + 13f, sw, 3f, 1.5f, withAlpha(0xFF26262B, (int) (255f * fade)));
            if (sp > 0.01f) {
                rect(ctx, sx, y + 13f, sw * sp, 3f, 1.5f, withAlpha(C_ACCENT, (int) (240f * fade)));
            }
            rect(ctx, sx + sw * sp - 3f, y + 11f, 6f, 7f, 2f, withAlpha(C_TEXT, (int) (255f * fade)));
        } else if (s instanceof BindSetting bs) {
            boolean binding = ClickGuiRenderer.bindingSetting == bs;
            String bt = binding ? "..." : keyName(bs.getValue().getKey());
            float bw = Math.max(28f, Fonts.MEDIUM.getWidth(bt, 7f) + 10f);
            text(ctx, Fonts.MEDIUM, name, sx, y + 2f, withAlpha(C_TEXT, (int) (255f * fade)), 7.5f);
            rect(ctx, sx + sw - bw, y, bw, 12f, SET_R,
                    binding ? withAlpha(C_ACCENT, (int) (200f * fade)) : withAlpha(0xFF26262B, (int) (255f * fade)));
            text(ctx, Fonts.MEDIUM, bt, sx + sw - bw + 5f, y + 2.5f, withAlpha(C_TEXT, (int) (255f * fade)), 7f);
        } else if (s instanceof EnumSetting<?> es) {
            String val = I18n.translate(es.currentEnumName());
            float vw = Fonts.MEDIUM.getWidth(val, 7f) + 10f;
            text(ctx, Fonts.MEDIUM, name, sx, y + 2f, withAlpha(C_TEXT, (int) (255f * fade)), 7.5f);
            rect(ctx, sx + sw - vw, y, vw, 12f, SET_R, withAlpha(0xFF26262B, (int) (255f * fade)));
            text(ctx, Fonts.MEDIUM, val, sx + sw - vw + 5f, y + 2.5f, withAlpha(C_TEXT_DIM, (int) (255f * fade)), 7f);
        } else if (s instanceof ButtonSetting) {
            rect(ctx, sx, y, sw, 14f, SET_R, withAlpha(0xFF26262B, (int) (255f * fade)));
            float lw = Fonts.MEDIUM.getWidth(name, 7.5f);
            text(ctx, Fonts.MEDIUM, name, sx + (sw - lw) / 2f, y + 3f, withAlpha(C_TEXT, (int) (255f * fade)), 7.5f);
        } else if (s instanceof StringSetting ss) {
            boolean editing = ClickGuiRenderer.editingStringSetting == ss;
            text(ctx, Fonts.MEDIUM, name, sx, y, withAlpha(C_TEXT_DIM, (int) (255f * fade)), 7.5f);
            rect(ctx, sx, y + 10f, sw, 13f, SET_R,
                    editing ? withAlpha(C_ACCENT, (int) (180f * fade)) : withAlpha(0xFF26262B, (int) (255f * fade)));
            String val = ss.getValue() == null ? "" : ss.getValue();
            String shown = val;
            while (Fonts.MEDIUM.getWidth(shown, 7f) > sw - 14f && shown.length() > 1) {
                shown = shown.substring(1);
            }
            float blink = (now() / 500L) % 2L == 0L ? 1f : 0f;
            text(ctx, Fonts.MEDIUM, shown + (editing && blink > 0.5f ? "|" : ""), sx + 4f, y + 13f,
                    withAlpha(C_TEXT, (int) (255f * fade)), 7f);
        }
    }

    private float settingHeight(Setting<?> s) {
        if (s instanceof NumberSetting)  return 22f;
        if (s instanceof BooleanSetting) return 16f;
        if (s instanceof StringSetting)  return 26f;
        if (s instanceof ListSetting ls) {
            float cw = CARD_W;
            float px = 0; int rows = 1;
            for (BooleanSetting opt : ls.getValue()) {
                float pw = Fonts.MEDIUM.getWidth(I18n.translate(opt.getName()), 7f) + 10f;
                if (px + pw > cw - 20f && px > 0) { rows++; px = 0; }
                px += pw + 3f;
            }
            return 12f + rows * 15f + 3f;
        }
        return 16f;
    }

    private void drawToggle(DrawContext ctx, float x, float y, float togT, float fade) {
        float tw = 36f, th = 20f, tr = 10f;
        int track = lerpColor(
                withAlpha(0xFF18181C, (int) (255f * fade)),
                withAlpha(0xFF212126, (int) (255f * fade)), togT);
        rect(ctx, x, y, tw, th, tr, track);

        if (togT > 0.05f) {
            float g = 5f * togT;
            float tcx = x + 3f + (tw - th) * togT + (th - 6f) / 2f;
            float tcy = y + th / 2f;
            rect(ctx, tcx - 7f - g / 2f, tcy - 7f - g / 2f, 14f + g, 14f + g, (14f + g) / 2f,
                    withAlpha(C_THUMB, (int) (90f * fade * togT)));
        }

        float knobPop = aval("knobpop:" + x + ":" + y, togT, 9f);
        float overshoot = 1f + 0.22f * (float) Math.sin(Math.min(1f, Math.max(0f, knobPop)) * Math.PI) * togT;
        float knobSize = (th - 6f) * overshoot;
        float thumbCX = x + 3f + (tw - th) * togT + (th - 6f) / 2f;
        float thumbCY = y + th / 2f;
        rect(ctx, thumbCX - knobSize / 2f, thumbCY - knobSize / 2f, knobSize, knobSize, knobSize / 2f,
                lerpColor(withAlpha(0xFF4A4A52, (int) (255f * fade)),
                        withAlpha(C_THUMB,   (int) (255f * fade)), togT));
    }

    private void drawChevron(DrawContext ctx, float cx, float cy, float expT, float fade) {
        int col = withAlpha(C_TEXT_DIM, (int) (220f * fade));
        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, cy, 0f);
        ms.multiply(new org.joml.Quaternionf().rotateZ((float) (Math.PI * expT)));

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

    private void drawGridGlyph(DrawContext ctx, float x, float y, int color) {
        fill(ctx, x,      y,      4f, 4f, color);
        fill(ctx, x + 6f, y,      4f, 4f, color);
        fill(ctx, x,      y + 6f, 4f, 4f, color);
        fill(ctx, x + 6f, y + 6f, 4f, 4f, color);
    }

    private void renderDarkaChat(DrawContext ctx, float fx, float fy, int mx, int my, float fade) {
        float cx = contentLeft(fx);
        float cw = contentWidth();
        float lTop = listTop(fy);
        float lBot = listBottom(fy);

        text(ctx, Fonts.BOLD, "DARKA-0.1", cx, lTop + 2f, withAlpha(C_TEXT, (int) (255f * fade)), 12f);
        text(ctx, Fonts.MEDIUM, "ИИ-ассистент на базе Hugging Face", cx + 76f, lTop + 4.5f,
                withAlpha(C_TEXT_DIM, (int) (200f * fade)), 8f);

        float chatInH = 24f;
        float sendW   = 48f;
        float bodyTop = lTop + 22f;
        float bodyBot = lBot - chatInH - 6f;
        float bodyH   = bodyBot - bodyTop;

        rect(ctx, cx, bodyTop, cw, bodyH, 10f, withAlpha(0xFF16161A, (int) (235f * fade)));

        ctx.enableScissor((int) cx, (int) bodyTop, (int) (cx + cw), (int) bodyBot);
        var history = dev.darkvisuals.client.managers.DarkaManager.getHistory();
        float totalH = 0f;
        List<List<String>> wrappedMsgs = new ArrayList<>();
        for (var m : history) {
            List<String> lines = wrap(m.text, cw - 60f, 8f);
            wrappedMsgs.add(lines);
            totalH += 8f + lines.size() * 10.5f + 4f;
        }
        float maxScroll = Math.max(0f, totalH - bodyH + 6f);
        darkaScroll = aval("darkascroll", maxScroll, 14f);

        float y = bodyBot + darkaScroll - totalH;
        for (int i = 0; i < history.size(); i++) {
            var msg = history.get(i);
            List<String> lines = wrappedMsgs.get(i);
            float bh = 8f + lines.size() * 10.5f;

            float bw = 0f;
            for (String ln : lines) bw = Math.max(bw, Fonts.MEDIUM.getWidth(ln, 8f));
            bw = Math.min(cw - 40f, bw + 14f);

            float bx = msg.user ? cx + cw - 10f - bw : cx + 10f;
            rect(ctx, bx, y, bw, bh, 8f,
                    msg.user ? withAlpha(0xFF2A2340, (int) (255f * fade)) : withAlpha(0xFF202025, (int) (255f * fade)));

            float ty = y + 4f;
            for (String ln : lines) {
                float lw = Fonts.MEDIUM.getWidth(ln, 8f);
                float tx = msg.user ? bx + bw - lw - 6f : bx + 6f;
                text(ctx, Fonts.MEDIUM, ln, tx, ty,
                        withAlpha(msg.user ? 0xFFE4DCFF : C_TEXT, (int) (240f * fade)), 8f);
                ty += 10.5f;
            }
            y += bh + 4f;
        }
        ctx.disableScissor();

        float inY = lBot - chatInH;
        rect(ctx, cx, inY, cw - sendW - 6f, chatInH, 8f,
                withAlpha(darkaInputFocused ? 0xFF24242A : 0xFF1C1C20, (int) (240f * fade)));
        rect(ctx, cx + cw - sendW, inY, sendW, chatInH, 8f, withAlpha(C_ACCENT, (int) (230f * fade)));

        String shown = darkaInput.toString();
        if (shown.isEmpty() && !darkaInputFocused) {
            text(ctx, Fonts.MEDIUM, "Запрос для DARKA...", cx + 8f, inY + 8f,
                    withAlpha(C_TEXT_DIM, (int) (180f * fade)), 8f);
        } else {
            text(ctx, Fonts.MEDIUM, shown, cx + 8f, inY + 8f, withAlpha(C_TEXT, (int) (255f * fade)), 8f);
        }
        text(ctx, Fonts.BOLD, "SEND", cx + cw - sendW / 2f - Fonts.BOLD.getWidth("SEND", 8f) / 2f, inY + 8f,
                withAlpha(C_TEXT, (int) (255f * fade)), 8f);
    }

    private void renderSettingsView(DrawContext ctx, float fx, float fy, int mx, int my, float fade) {
        UI ui = darkvisuals.getInstance().getModuleManager().getModule(UI.class);
        float cx = contentLeft(fx);
        float cw = contentWidth();
        float y = listTop(fy);

        text(ctx, Fonts.BOLD, "Настройки ClickGUI", cx, y, withAlpha(C_TEXT, (int) (255f * fade)), 11f);
        y += 18f;

        guiSoundRowX = cx;
        guiSoundRowY = y;
        guiSoundRowW = cw;
        guiSoundRowH = 28f;

        boolean sndHov = hit(mx, my, guiSoundRowX, guiSoundRowY, guiSoundRowW, guiSoundRowH);
        rect(ctx, guiSoundRowX, guiSoundRowY, guiSoundRowW, guiSoundRowH, 8f,
                withAlpha(sndHov ? C_CARD_HOV : C_CARD, (int) (235f * fade)));

        text(ctx, Fonts.MEDIUM, "Звук при включении функции", guiSoundRowX + 10f, guiSoundRowY + 10f,
                withAlpha(C_TEXT, (int) (255f * fade)), 8.5f);
        drawToggle(ctx, guiSoundRowX + guiSoundRowW - 44f, guiSoundRowY + 4f,
                ui != null && ui.isGuiSound() ? 1f : 0f, fade);

        y += guiSoundRowH + 12f;

        text(ctx, Fonts.BOLD, "DARKA API Key", cx, y, withAlpha(C_TEXT, (int) (255f * fade)), 11f);
        y += 18f;

        apiFieldX = cx;
        apiFieldY = y;
        apiFieldW = cw - 30f;
        apiFieldH = 24f;

        rect(ctx, apiFieldX, apiFieldY, apiFieldW, apiFieldH, 8f,
                withAlpha(darkaKeyFocused ? 0xFF2A2A30 : C_CARD, (int) (235f * fade)));

        String key = ui == null ? "" : ui.getDarkaApiKey();
        String shownKey = key.isEmpty() && !darkaKeyFocused ? "Вставьте API-ключ..." : key;
        text(ctx, Fonts.MEDIUM, shownKey, apiFieldX + 8f, apiFieldY + 8f,
                key.isEmpty() && !darkaKeyFocused ? withAlpha(C_TEXT_DIM, (int) (180f * fade)) : withAlpha(C_TEXT, (int) (255f * fade)),
                8f);

        float iX = apiFieldX + apiFieldW + 6f;
        float iY = apiFieldY;
        float iS = apiFieldH;
        boolean iHov = hit(mx, my, iX, iY, iS, iS);
        rect(ctx, iX, iY, iS, iS, 6f, withAlpha(iHov ? C_CARD_HOV : C_CARD, (int) (235f * fade)));
        text(ctx, Fonts.BOLD, "i", iX + iS / 2f - Fonts.BOLD.getWidth("i", 9f) / 2f, iY + 7f,
                withAlpha(C_TEXT, (int) (255f * fade)), 9f);

        if (hfWindowOpen) {
            renderHfWindow(ctx, fx, fy, mx, my, fade);
        }
    }

    private void renderHfWindow(DrawContext ctx, float fx, float fy, int mx, int my, float fade) {
        float hx = fx + FRAME_W + 10f;
        float hy = fy;
        float hw = 230f;
        float hh = 290f;

        rect(ctx, hx, hy, hw, hh, 14f, withAlpha(C_BG, (int) (250f * fade)));
        text(ctx, Fonts.BOLD, "Hugging Face Токен", hx + 12f, hy + 10f, withAlpha(C_TEXT, (int) (255f * fade)), 9.5f);

        float cx = hx + hw - 20f, cy = hy + 8f, cs = 14f;
        boolean cHov = hit(mx, my, cx, cy, cs, cs);
        rect(ctx, cx, cy, cs, cs, 4f, withAlpha(cHov ? 0xFFFF4444 : 0xFF282830, (int) (240f * fade)));
        text(ctx, Fonts.BOLD, "x", cx + 4f, cy + 3f, withAlpha(C_TEXT, (int) (240f * fade)), 8f);

        ctx.enableScissor((int) hx, (int) (hy + 28f), (int) (hx + hw), (int) (hy + hh - 8f));
        float y = hy + 30f - hfScroll;
        String[] instrs = {
                "1. Зарегистрируйтесь на https://huggingface.co",
                "2. Перейдите в Settings -> Access Tokens",
                "3. Нажмите 'New token' и выберите тип 'Read'",
                "4. Скопируйте токен и вставьте в поле DARKA API Key"
        };
        for (String s : instrs) {
            for (String ln : wrap(s, hw - 20f, 7.5f)) {
                text(ctx, Fonts.MEDIUM, ln, hx + 10f, y, withAlpha(C_TEXT_DIM, (int) (220f * fade)), 7.5f);
                y += 10f;
            }
            y += 4f;
        }
        ctx.disableScissor();
    }

    private void renderThemes(DrawContext ctx, float cx, float cw, float y, float fade) {
        boolean addHov = hit(lastMouseX, lastMouseY, cx, y, cw, 24f);
        rect(ctx, cx, y, cw, 24f, SET_R, withAlpha(addHov ? C_CARD_HOV : C_CARD, (int) (255f * fade)));
        text(ctx, Fonts.BOLD, "+ Своя тема", cx + 10f, y + 8f, withAlpha(C_TEXT, (int) (255f * fade)), 8.5f);
        y += 24f + CARD_GAP;

        float thW = CARD_W;
        float thH = 46f;
        var themes = ClickGuiRenderer.getThemes();
        for (int i = 0; i < themes.length; i++) {
            var t = themes[i];
            float tx = cx + (i % 2) * (thW + CARD_GAP);
            float ty = y + (i / 2) * (thH + CARD_GAP);

            boolean sel = dev.darkvisuals.client.managers.ThemeManager.getInstance()
                    .getCurrentTheme().getName().equals(t.getName());
            boolean hov = hit(lastMouseX, lastMouseY, tx, ty, thW, thH);

            rect(ctx, tx, ty, thW, thH, CARD_R,
                    sel ? withAlpha(C_ACCENT, (int) (180f * fade)) : withAlpha(hov ? C_CARD_HOV : C_CARD, (int) (255f * fade)));
            rect(ctx, tx + thW - 18f, ty + 6f, 10f, 10f, 2.5f, withAlpha(t.getAccentColor().getRGB() | 0xFF000000, (int) (255f * fade)));
            text(ctx, Fonts.MEDIUM, t.getName(), tx + 8f, ty + 7f, withAlpha(C_TEXT, (int) (255f * fade)), 8f);
        }
    }

    private void renderFriends(DrawContext ctx, float cx, float cw, float y, float fade) {
        float inputW = cw - BTN_W - 6f, addX = cx + inputW + 6f;
        drawInput(ctx, cx, y, inputW, ClickGuiRenderer.friendInputFocused,
                ClickGuiRenderer.friendNameBuffer.toString(), "Ник игрока...", fade);
        drawButton(ctx, addX, y, BTN_W, "ADD", fade);

        if (ClickGuiRenderer.friendStatus != null && System.currentTimeMillis() < ClickGuiRenderer.friendStatusUntil) {
            text(ctx, Fonts.MEDIUM, ClickGuiRenderer.friendStatus, cx, y + INPUT_H + 3f,
                    withAlpha(0xFF9BE39B, (int) (255f * fade)), 7.5f);
        }
        y += INPUT_H + ROWSET_GAP;

        for (String f : dev.darkvisuals.client.managers.FriendsManager.getFriends()) {
            boolean online = ClickGuiRenderer.isFriendOnline(f);
            rect(ctx, cx, y, cw, LIST_ROW_H, SET_R, withAlpha(C_CARD, (int) (255f * fade)));
            rect(ctx, cx + 8f, y + 9f, 6f, 6f, 3f, withAlpha(online ? 0xFF4CD964 : 0xFF666666, (int) (255f * fade)));
            text(ctx, Fonts.MEDIUM, f, cx + 20f, y + 8f, withAlpha(C_TEXT, (int) (255f * fade)), 8f);
            text(ctx, Fonts.BOLD, "DEL", cx + cw - 28f, y + 8f, withAlpha(0xFFFF7070, (int) (255f * fade)), 7f);
            y += LIST_ROW_H + LIST_ROW_GAP;
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
        drawButton(ctx, addX, y, addW, "ADD", fade);
        drawButton(ctx, hereX, y, hereW, "HERE", fade);

        y += INPUT_H + ROWSET_GAP;
        for (var w : dev.darkvisuals.client.managers.WaypointManager.list()) {
            rect(ctx, cx, y, cw, LIST_ROW_H, SET_R, withAlpha(C_CARD, (int) (255f * fade)));
            text(ctx, Fonts.MEDIUM, w.name, cx + 10f, y + 8f, withAlpha(C_TEXT, (int) (255f * fade)), 8f);
            text(ctx, Fonts.BOLD, "DEL", cx + cw - 28f, y + 8f, withAlpha(0xFFFF7070, (int) (255f * fade)), 7f);
            y += LIST_ROW_H + LIST_ROW_GAP;
        }
    }

    private void renderConfigs(DrawContext ctx, float cx, float cw, float y, float fade) {
        float inputW = cw - BTN_W - 6f, saveX = cx + inputW + 6f;
        drawInput(ctx, cx, y, inputW, ClickGuiRenderer.configInputFocused,
                ClickGuiRenderer.configNameBuffer.toString(), "Имя конфига...", fade);
        drawButton(ctx, saveX, y, BTN_W, "SAVE", fade);

        y += INPUT_H + ROWSET_GAP;
        for (String cfg : dev.darkvisuals.darkvisuals.getInstance().getConfigManager().getConfigList()) {
            rect(ctx, cx, y, cw, LIST_ROW_H, SET_R, withAlpha(C_CARD, (int) (255f * fade)));
            text(ctx, Fonts.MEDIUM, cfg, cx + 10f, y + 8f, withAlpha(C_TEXT, (int) (255f * fade)), 8f);
            text(ctx, Fonts.BOLD, "LOAD", cx + cw - 64f, y + 8f, withAlpha(0xFF9BC7FF, (int) (255f * fade)), 7f);
            text(ctx, Fonts.BOLD, "DEL",  cx + cw - 28f, y + 8f, withAlpha(0xFFFF7070, (int) (255f * fade)), 7f);
            y += LIST_ROW_H + LIST_ROW_GAP;
        }
    }

    private void drawInput(DrawContext ctx, float x, float y, float w, boolean focused, String value, String placeholder, float fade) {
        rect(ctx, x, y, w, INPUT_H, INPUT_R, withAlpha(focused ? C_CARD_HOV : C_CARD, (int) (255f * fade)));
        boolean ph = value.isEmpty() && !focused;
        String shown = ph ? placeholder : value;
        text(ctx, Fonts.MEDIUM, shown, x + 6f, y + 7f,
                withAlpha(ph ? C_TEXT_DIM : C_TEXT, (int) (255f * fade)), 8f);
    }

    private void drawButton(DrawContext ctx, float x, float y, float w, String label, float fade) {
        rect(ctx, x, y, w, INPUT_H, SET_R, withAlpha(C_ACCENT, (int) (230f * fade)));
        float lw = Fonts.BOLD.getWidth(label, 7.5f);
        text(ctx, Fonts.BOLD, label, x + (w - lw) / 2f, y + 7f, withAlpha(C_TEXT, (int) (255f * fade)), 7.5f);
    }

    private void renderTooltip(DrawContext ctx, float fx, float fy, float fade) {
        if (hoveredDesc == null || hoveredDesc.isBlank()) return;
        float tw = Fonts.MEDIUM.getWidth(hoveredDesc, 8.5f) + 16f;
        float tx = fx + (FRAME_W - tw) / 2f;
        float ty = fy + FRAME_H - 24f;
        rect(ctx, tx, ty, tw, 18f, 9f, withAlpha(0xE616161A, (int) (255f * fade)));
        text(ctx, Fonts.MEDIUM, hoveredDesc, tx + 8f, ty + 5f, withAlpha(C_TEXT, (int) (240f * fade)), 8.5f);
    }

    private void renderScrollBar(DrawContext ctx, float fx, float fy, ClickGuiState state, float fade) {
        Category cat = ClickGuiRenderer.currentCategory;
        float max = maxScroll(state, cat);
        if (max <= 0.01f) return;

        float lTop = listTop(fy), lBot = listBottom(fy);
        float trackH = lBot - lTop;
        float thumbH = Math.max(16f, trackH * trackH / (max + trackH));
        float thumbY = lTop + (trackH - thumbH) * (frameScroll / max);

        rect(ctx, fx + FRAME_W - 5f, thumbY, 2.5f, thumbH, 1.25f,
                withAlpha(C_TEXT, (int) (60f * fade)));
    }

    public boolean mouseClicked(double mx, double my, int button, Window window, ClickGuiState state) {
        float fx = frameX(window), fy = frameY(window);

        float sbW = SEARCH_W;
        float sbH = SEARCH_H;
        float sbX = fx + FRAME_W - 12f - sbW;
        float sbY = fy + (HEADER_H - sbH) / 2f;
        searchBoxX = sbX; searchBoxY = sbY; searchBoxW = sbW; searchBoxH = sbH;

        if (hit(mx, my, searchBoxX, searchBoxY, searchBoxW, searchBoxH)) {
            if (button == 1) {
                ClickGuiRenderer.searchBuffer.setLength(0);
            } else if (hit(mx, my, searchBoxX + searchBoxW - 20f, searchBoxY, 20f, searchBoxH)
                    && ClickGuiRenderer.searchBuffer.length() > 0) {
                ClickGuiRenderer.searchBuffer.setLength(0);
            }

            unfocusAll();
            ClickGuiRenderer.searchInputFocused = true;
            welcomeScreen = false;
            return true;
        }

        ClickGuiRenderer.searchInputFocused = false;

        if (hfWindowOpen && hit(mx, my, fx + FRAME_W + 10f, fy, 230f, 290f)) {
            if (hit(mx, my, fx + FRAME_W + 10f + 210f, fy + 8f, 14f, 14f)) {
                hfWindowOpen = false;
            }
            return true;
        }

        if (!hit(mx, my, fx, fy, FRAME_W, FRAME_H)) {
            unfocusAll();
            return false;
        }

        if (hit(mx, my, fx, fy, SIDEBAR_W, HEADER_H)) {
            welcomeScreen = true;
            viewMode = ViewMode.MODULES;
            expandedModule = null;
            unfocusAll();
            return true;
        }

        if (hit(mx, my, aiX, aiY, aiW, aiH)) {
            viewMode = (viewMode == ViewMode.DARKA) ? ViewMode.MODULES : ViewMode.DARKA;
            welcomeScreen = false;
            expandedModule = null;
            unfocusAll();
            return true;
        }

        if (hit(mx, my, gridX, gridY, gridW, gridH)) {
            viewMode = (viewMode == ViewMode.SETTINGS) ? ViewMode.MODULES : ViewMode.SETTINGS;
            welcomeScreen = false;
            expandedModule = null;
            unfocusAll();
            return true;
        }

        float startY = fy + HEADER_H;
        float step   = SIDEBAR_SLOT_H;
        for (int i = 0; i < SIDEBAR_CATS.length; i++) {
            Category cat = SIDEBAR_CATS[i];
            float y = startY + i * step;
            if (hit(mx, my, fx, y, SIDEBAR_W, step)) {
                ClickGuiRenderer.currentCategory = cat;
                state.setScroll(cat, 0f);
                welcomeScreen = false;
                viewMode = ViewMode.MODULES;
                expandedModule = null;
                unfocusAll();
                return true;
            }
        }

        if (viewMode == ViewMode.DARKA) {
            float cx = contentLeft(fx), cw = contentWidth();
            float inY = listBottom(fy) - 24f;
            if (hit(mx, my, cx + cw - 48f, inY, 48f, 24f)) {
                sendDarkaMessage();
                return true;
            }
            if (hit(mx, my, cx, inY, cw - 48f - 6f, 24f)) {
                unfocusAll();
                darkaInputFocused = true;
                return true;
            }
            darkaInputFocused = false;
            return true;
        }

        if (viewMode == ViewMode.SETTINGS) {
            UI ui = darkvisuals.getInstance().getModuleManager().getModule(UI.class);
            if (ui != null && hit(mx, my, guiSoundRowX, guiSoundRowY, guiSoundRowW, guiSoundRowH)) {
                ui.setGuiSoundInverted();
                playUiSound(ui.isGuiSound());
                return true;
            }
            if (hit(mx, my, apiFieldX, apiFieldY, apiFieldW, apiFieldH)) {
                unfocusAll();
                darkaKeyFocused = true;
                return true;
            }
            if (hit(mx, my, apiFieldX + apiFieldW + 6f, apiFieldY, apiFieldH, apiFieldH)) {
                hfWindowOpen = !hfWindowOpen;
                return true;
            }
            darkaKeyFocused = false;
            return true;
        }

        if (welcomeScreen && !ClickGuiRenderer.isSearching()) {
            return true;
        }

        Category cat = ClickGuiRenderer.currentCategory;
        if (!ClickGuiRenderer.isModuleTab(cat)) {
            return handleNonModuleClick(mx, my, contentLeft(fx), contentWidth(), listTop(fy), state, cat);
        }

        List<Module> mods = ClickGuiRenderer.visibleModules(state);
        float scroll = frameScroll;
        float lTop = listTop(fy);

        int expIdx = indexOfExpanded(mods);
        if (expIdx >= 0 && expIdx < mods.size()) {
            Module em = mods.get(expIdx);
            int ecol = expIdx % GRID_COLS, erow = expIdx / GRID_COLS;
            float ecardX = cardX(fx, ecol);
            float erowY = lTop + 4f - scroll + erow * gridRowH();
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
                return true;
            }
        }

        int row = 0;
        for (Module m : mods) {
            int col = row % GRID_COLS, rIdx = row / GRID_COLS;
            float cX = cardX(fx, col);
            float y  = lTop + 4f - scroll + rIdx * gridRowH();
            row++;
            if (!hit(mx, my, cX, y, CARD_W, CARD_H)) continue;

            cardPressTimes.put(m.getName(), now());

            if (hit(mx, my, cX + CARD_W - 58f, y + 11f, 36f, 20f)) {
                m.setToggled(!m.isToggled());
                playModuleSound(m.isToggled(), m);
                toggleTimes.put(m.getName(), now());
                return true;
            }

            expandedModule = m.getName().equals(expandedModule) ? null : m.getName();
            return true;
        }

        return true;
    }

    private void clickSetting(Setting<?> s, double mx, double my, float sx, float sy, float sw) {
        if (s instanceof ListSetting ls) {
            float px = sx, py = sy + 12f;
            for (BooleanSetting opt : ls.getValue()) {
                String lbl = I18n.translate(opt.getName());
                float pw = Fonts.MEDIUM.getWidth(lbl, 7f) + 10f;
                if (hit(mx, my, px, py, pw, 12f)) {
                    if (ls.isSingleSelect()) {
                        if (!opt.getValue()) {
                            ls.getValue().forEach(o -> o.setValue(false));
                            opt.setValue(true);
                        }
                    } else opt.setValue(!opt.getValue());
                    return;
                }
                px += pw + 3f;
                if (px > sx + sw - 20f) { px = sx; py += 15f; }
            }
        } else if (s instanceof BooleanSetting b) {
            b.setValue(!b.getValue());
            playModuleSound(b.getValue(), null);
        } else if (s instanceof NumberSetting n) {
            ClickGuiRenderer.draggingSlider = n;
            ClickGuiRenderer.sliderTrackX  = sx;
            ClickGuiRenderer.sliderTrackW  = sw;
            applySlider(n, mx);
        } else if (s instanceof BindSetting bs) {
            ClickGuiRenderer.bindingSetting = bs;
        } else if (s instanceof ButtonSetting btn) {
            btn.click();
        } else if (s instanceof EnumSetting<?> es) {
            es.increaseEnum();
        } else if (s instanceof StringSetting ss) {
            ClickGuiRenderer.editingStringSetting = ss;
        }
    }

    private void applySlider(NumberSetting s, double mx) {
        float p = clamp01((float) ((mx - ClickGuiRenderer.sliderTrackX) / ClickGuiRenderer.sliderTrackW));
        float val = s.getMin() + p * (s.getMax() - s.getMin());
        if (s.getIncrement() > 0) val = Math.round(val / s.getIncrement()) * s.getIncrement();
        s.setValue(Math.max(s.getMin(), Math.min(s.getMax(), val)));
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ClickGuiRenderer.draggingSlider = null;
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (ClickGuiRenderer.draggingSlider != null) {
            applySlider(ClickGuiRenderer.draggingSlider, mouseX);
            return true;
        }
        return false;
    }

    public boolean scroll(double mouseX, double mouseY, double amount, Window window, ClickGuiState state) {
        float fx = frameX(window), fy = frameY(window);
        if (!hit(mouseX, mouseY, fx, fy, FRAME_W, FRAME_H)) return false;

        if (viewMode == ViewMode.DARKA) {
            darkaScroll = Math.max(0f, darkaScroll - (float) amount * 18f);
            return true;
        }
        if (viewMode == ViewMode.SETTINGS) {
            settingsScroll = Math.max(0f, settingsScroll - (float) amount * 18f);
            return true;
        }

        Category cat = ClickGuiRenderer.currentCategory;
        float max = maxScroll(state, cat);
        float cur = state.getScroll(cat);
        state.setScroll(cat, Math.max(0f, Math.min(max, cur - (float) amount * 18f)));
        return true;
    }

    private float maxScroll(ClickGuiState state, Category cat) {
        float visible = listBottom(0) - listTop(0) - 4f;
        float content;
        if (!ClickGuiRenderer.isModuleTab(cat)) {
            if (cat == Category.Theme) {
                int thRows = (ClickGuiRenderer.getThemes().length + 1) / 2;
                content = 24f + CARD_GAP + thRows * (46f + CARD_GAP);
            } else if (cat == Category.Friends) {
                content = INPUT_H + ROWSET_GAP + dev.darkvisuals.client.managers.FriendsManager.getFriends().size() * (LIST_ROW_H + LIST_ROW_GAP);
            } else if (cat == Category.Markers) {
                content = INPUT_H + ROWSET_GAP + dev.darkvisuals.client.managers.WaypointManager.list().size() * (LIST_ROW_H + LIST_ROW_GAP);
            } else {
                content = INPUT_H + ROWSET_GAP + dev.darkvisuals.darkvisuals.getInstance().getConfigManager().getConfigList().length * (LIST_ROW_H + LIST_ROW_GAP);
            }
        } else {
            List<Module> mods = ClickGuiRenderer.visibleModules(state);
            int rows = (mods.size() + GRID_COLS - 1) / GRID_COLS;
            content = rows * gridRowH();
            int expIdx = indexOfExpanded(mods);
            if (expIdx >= 0 && expIdx / GRID_COLS == rows - 1) {
                content += EXPAND_GAP + expandPanelContentH(mods.get(expIdx));
            }
        }
        return Math.max(0f, content - visible);
    }

    private boolean handleNonModuleClick(double mx, double my, float cx, float cw, float lTop, ClickGuiState state, Category cat) {
        float y = lTop + 4f - state.getScroll(cat);
        if (cat == Category.Theme) {
            if (hit(mx, my, cx, y, cw, 24f)) {
                MinecraftClient.getInstance().setScreen(new dev.darkvisuals.client.ui.colorgui.CustomThemeScreen());
                return true;
            }
            y += 24f + CARD_GAP;
            var themes = ClickGuiRenderer.getThemes();
            for (int i = 0; i < themes.length; i++) {
                float tx = cx + (i % 2) * (CARD_W + CARD_GAP);
                float ty = y + (i / 2) * (46f + CARD_GAP);
                if (hit(mx, my, tx, ty, CARD_W, 46f)) {
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
                unfocusAll();
                ClickGuiRenderer.friendInputFocused = true;
                return true;
            }
            if (hit(mx, my, addX, y, BTN_W, INPUT_H)) {
                String name = ClickGuiRenderer.friendNameBuffer.toString().trim();
                if (!name.isEmpty()) {
                    dev.darkvisuals.client.managers.FriendsManager.addFriend(name);
                    ClickGuiRenderer.friendNameBuffer.setLength(0);
                }
                return true;
            }
            y += INPUT_H + ROWSET_GAP;
            for (String f : dev.darkvisuals.client.managers.FriendsManager.getFriends()) {
                if (hit(mx, my, cx + cw - 32f, y, 28f, LIST_ROW_H)) {
                    dev.darkvisuals.client.managers.FriendsManager.removeFriend(f);
                    return true;
                }
                y += LIST_ROW_H + LIST_ROW_GAP;
            }
            return true;
        }
        if (cat == Category.Markers) {
            y += INPUT_H + ROWSET_GAP;
            for (var w : dev.darkvisuals.client.managers.WaypointManager.list()) {
                if (hit(mx, my, cx + cw - 32f, y, 28f, LIST_ROW_H)) {
                    dev.darkvisuals.client.managers.WaypointManager.remove(w.name);
                    return true;
                }
                y += LIST_ROW_H + LIST_ROW_GAP;
            }
            return true;
        }
        if (cat == Category.Config) {
            float inputW = cw - BTN_W - 6f, saveX = cx + inputW + 6f;
            if (hit(mx, my, cx, y, inputW, INPUT_H)) {
                unfocusAll();
                ClickGuiRenderer.configInputFocused = true;
                return true;
            }
            if (hit(mx, my, saveX, y, BTN_W, INPUT_H)) {
                String name = ClickGuiRenderer.configNameBuffer.toString().trim();
                if (!name.isEmpty()) dev.darkvisuals.darkvisuals.getInstance().getConfigManager().saveConfig(name);
                return true;
            }
            y += INPUT_H + ROWSET_GAP;
            var cm = dev.darkvisuals.darkvisuals.getInstance().getConfigManager();
            for (String cfg : cm.getConfigList()) {
                if (hit(mx, my, cx + cw - 64f, y, 32f, LIST_ROW_H)) {
                    cm.loadConfig(cfg);
                    return true;
                }
                if (hit(mx, my, cx + cw - 28f, y, 28f, LIST_ROW_H)) {
                    cm.deleteConfig(cfg);
                    return true;
                }
                y += LIST_ROW_H + LIST_ROW_GAP;
            }
            return true;
        }
        return true;
    }

    public boolean isKeyOrInputFocused() {
        return ClickGuiRenderer.searchInputFocused
                || ClickGuiRenderer.friendInputFocused
                || ClickGuiRenderer.configInputFocused
                || ClickGuiRenderer.markerFocusedField != 0
                || ClickGuiRenderer.editingStringSetting != null
                || darkaInputFocused
                || darkaKeyFocused;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return handleKeyPressed(keyCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        return handleCharTyped(chr);
    }

    public boolean handleKeyPressed(int keyCode, int modifiers) {
        if (ClickGuiRenderer.searchInputFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (ClickGuiRenderer.searchBuffer.length() > 0) {
                    ClickGuiRenderer.searchBuffer.deleteCharAt(ClickGuiRenderer.searchBuffer.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                ClickGuiRenderer.searchInputFocused = false;
                return true;
            }
            // Поддержка Ctrl + V для вставки текста
            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clip = MinecraftClient.getInstance().keyboard.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    for (char c : clip.toCharArray()) {
                        if (ClickGuiRenderer.searchBuffer.length() < 32 && !Character.isISOControl(c)) {
                            ClickGuiRenderer.searchBuffer.append(c);
                        }
                    }
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                ClickGuiRenderer.searchBuffer.setLength(0);
                return true;
            }
            return true;
        }

        if (darkaInputFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (darkaInput.length() > 0) darkaInput.deleteCharAt(darkaInput.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendDarkaMessage();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                darkaInputFocused = false;
                return true;
            }
            return true;
        }

        if (darkaKeyFocused) {
            UI ui = darkvisuals.getInstance().getModuleManager().getModule(UI.class);
            if (ui != null) {
                String cur = ui.getDarkaApiKey();
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !cur.isEmpty()) {
                    ui.setDarkaApiKey(cur.substring(0, cur.length() - 1));
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    String clip = MinecraftClient.getInstance().keyboard.getClipboard();
                    if (clip != null && !clip.isEmpty()) {
                        ui.setDarkaApiKey(cur + clip.trim());
                    }
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    darkaKeyFocused = false;
                    return true;
                }
            }
            return true;
        }

        return false;
    }

    public boolean handleCharTyped(char chr) {
        if (Character.isISOControl(chr)) return isKeyOrInputFocused();

        if (ClickGuiRenderer.searchInputFocused) {
            welcomeScreen = false;
            if (ClickGuiRenderer.searchBuffer.length() < 32) {
                ClickGuiRenderer.searchBuffer.append(chr);
            }
            return true;
        }
        if (darkaInputFocused) {
            if (darkaInput.length() < 200) darkaInput.append(chr);
            return true;
        }
        if (darkaKeyFocused) {
            UI ui = darkvisuals.getInstance().getModuleManager().getModule(UI.class);
            if (ui != null) {
                String cur = ui.getDarkaApiKey();
                if (cur.length() < 256) ui.setDarkaApiKey(cur + chr);
            }
            return true;
        }
        return false;
    }

    private void sendDarkaMessage() {
        String msg = darkaInput.toString().trim();
        if (!msg.isEmpty()) {
            darkaInput.setLength(0);
            dev.darkvisuals.client.managers.DarkaManager.ask(msg);
        }
    }

    private void unfocusAll() {
        ClickGuiRenderer.searchInputFocused = false;
        ClickGuiRenderer.friendInputFocused = false;
        ClickGuiRenderer.markerFocusedField = 0;
        ClickGuiRenderer.configInputFocused = false;
        ClickGuiRenderer.editingStringSetting = null;
        darkaInputFocused = false;
        darkaKeyFocused = false;
    }

    private float aval(String key, float target, float speed) {
        SmoothAnimation a = anims.computeIfAbsent(key, k -> new SmoothAnimation(0f, speed));
        a.setTarget(target);
        return a.update();
    }

    private Identifier iconForCategory(Category cat) {
        return switch (cat) {
            case Render  -> ICON_RENDER;
            case Utility -> ICON_UTILITY;
            case Friends -> ICON_FRIENDS;
            case Config  -> ICON_CONFIG;
            case Theme   -> ICON_THEME;
            case Markers -> ICON_MARKERS;
            default      -> null;
        };
    }

    private String catLabel(Category cat) {
        return switch (cat) {
            case Render  -> "Visuals";
            case Utility -> "Utility";
            case Friends -> "Friends";
            case Config  -> "Configs";
            case Theme   -> "Themes";
            case Markers -> "Markers";
            default      -> cat.name();
        };
    }

    private String fmtNum(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.format("%.1f", v);
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
            default                          -> "KEY_" + key;
        };
    }

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

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float t) {
        t = clamp01(t);
        int a = (int) (((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
        int r = (int) (((from >> 16)  & 0xFF) + ((((to >> 16)  & 0xFF) - ((from >> 16)  & 0xFF)) * t));
        int g = (int) (((from >> 8)   & 0xFF) + ((((to >> 8)   & 0xFF) - ((from >> 8)   & 0xFF)) * t));
        int b = (int) ((from          & 0xFF) + (((to           & 0xFF) - (from          & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void rect(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        if (((color >>> 24) & 0xFF) == 0 || w <= 0 || h <= 0) return;
        Render2D.drawRoundedRect(ctx.getMatrices(), x, y, w, h, Math.min(r, Math.min(w, h) / 2f), Render2D.cachedColor(color));
    }

    private void fill(DrawContext ctx, float x, float y, float w, float h, int color) {
        if (((color >>> 24) & 0xFF) == 0 || w <= 0 || h <= 0) return;
        Render2D.drawRect(ctx.getMatrices(), x, y, w, h, Render2D.cachedColor(color));
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

    private void tex(DrawContext ctx, Identifier id, float x, float y, float w, float h, int tint) {
        RenderSystem.setShaderColor(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f,
                (tint & 0xFF) / 255f, ((tint >>> 24) & 0xFF) / 255f);
        ctx.drawTexture(RenderLayer::getGuiTextured, id, (int) x, (int) y, 0, 0,
                (int) w, (int) h, (int) w, (int) h);
        ctx.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}