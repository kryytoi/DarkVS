package dev.darkvisuals.client.ui.clickgui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.util.animations.SmoothAnimation;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Font;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.modules.settings.impl.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGuiRenderer {
     
     
     
    public static final float PANEL_W = 470f, PANEL_H = 280f, PANEL_R = 20f, BORDER = 3f;
     
     
    public static final float PANEL_R_TL = 26f, PANEL_R_TR = 10f, PANEL_R_BR = 26f, PANEL_R_BL = 10f;
     
    private static final float PILL_ALPHA = 0.45f;

    public static final float SIDE_PAD = 14f, PILL_W = 122f, PILL_H = 28f, PILL_STEP = 37f, PILL_TOP = 22f, PILL_R = 8f;
    public static final float PILL_ICON_CIRCLE = 20f, PILL_ICON = 12f;

    public static final float SEP_X = SIDE_PAD + PILL_W + 12f;
    public static final float CONTENT_X = SEP_X + 10f;
    public static final float CONTENT_W = PANEL_W - CONTENT_X - 16f;
    public static final float CONTENT_TOP = 22f, CONTENT_BOTTOM = 16f;

    public static final float ROW_H = 26f, ROW_GAP = 6f, ROW_R = 7f;
    public static final float SETTINGS_R = 11f, OPTION_R = 5f, INPUT_R = 6f, BUTTON_R = 5f;
    public static final float SETTINGS_PAD = 8f, SETTINGS_GAP = 4f;
    public static final float TOGGLE_W = 24f, TOGGLE_H = 12f, GEAR = 12f;
    public static final float LIST_PILL_H = 14f, LIST_PILL_GAP = 4f, LIST_ROW_STEP = 18f;

    public static final float SEARCH_X = 12f, SEARCH_Y = 12f, SEARCH_W = 172f, SEARCH_H = 30f, SEARCH_R = 9f, SEARCH_ICON = 14f;
    public static final int LOGO_SIZE = 30;

    public static final float INPUT_H = 22f, BTN_W = 56f, LIST_ROW_H = 24f, LIST_ROW_GAP = 6f, OPTION_H = 18f;

    public static final Category[] SIDEBAR_CATS = {Category.Render, Category.Utility,
            Category.Friends, Category.Markers, Category.Theme, Category.Config};

     
     
    private static final int C_PANEL = 0xFF0B0B0B;
    private static final int C_ROW = 0xFF3B3B3B, C_ROW_HOVER = 0xFF474747;
    private static final int C_BLOCK = 0xFF2C2C2C, C_INNER = 0xFF1A1A1A, C_INNER_HOVER = 0xFF262626;
    private static final int C_PILL = 0xFFF1F1F1, C_PILL_HOVER = 0xFFDDDDDD, C_ICON_CIRCLE = 0xFF2E2E2E;
    private static final int C_SEP = 0xFF3A3A3A;
    private static final int C_TEXT_DARK = 0xFF1A1A1A, C_TEXT_DIM = 0xFF9A9A9A, C_WHITE = 0xFFFFFFFF;

     
     
     
    public static Category currentCategory = Category.Render;
    public static Module openedSettingsModule = null;
    public static Color themeColor = new Color(140, 60, 255);
    public static NumberSetting draggingSlider = null;
    public static float sliderTrackX = 0f, sliderTrackW = 1f;
    public static BindSetting bindingSetting = null;
    public static StringSetting editingStringSetting = null;

    public static final StringBuilder searchBuffer = new StringBuilder();
    public static boolean searchInputFocused = false;

    public static final StringBuilder configNameBuffer = new StringBuilder();
    public static boolean configInputFocused = false;
    public static String configStatus = null;
    public static long configStatusUntil = 0L;

    public static final StringBuilder friendNameBuffer = new StringBuilder();
    public static boolean friendInputFocused = false;
    public static String friendStatus = null;
    public static long friendStatusUntil = 0L;

    public static final StringBuilder markerNameBuffer = new StringBuilder();
    public static final StringBuilder markerXBuffer = new StringBuilder();
    public static final StringBuilder markerZBuffer = new StringBuilder();
    public static int markerFocusedField = 0;
    public static String markerStatus = null;
    public static long markerStatusUntil = 0L;

    private static final Identifier LOGO = Identifier.of("darkvisuals", "textures/logo.png");
    private static final Identifier ICON_RENDER = Identifier.of("darkvisuals", "textures/clickgui/render.png");
    private static final Identifier ICON_UTILITY = Identifier.of("darkvisuals", "textures/clickgui/utilities.png");
    private static final Identifier ICON_THEME = Identifier.of("darkvisuals", "hud/theme.png");
    private static final Identifier ICON_CONFIG = Identifier.of("darkvisuals", "hud/cloud.png");
    private static final Identifier ICON_FRIENDS = Identifier.of("darkvisuals", "hud/friends.png");
    private static final Identifier ICON_MARKERS = Identifier.of("darkvisuals", "textures/tags.png");
    private static final Identifier ICON_OPTIONS = Identifier.of("darkvisuals", "textures/mainmenu/options.png");

    private static ClickGuiRenderer instance;
private float globalAlpha = 1f;
    private final SmoothAnimation panelScale = new SmoothAnimation(0.94f, 12f);
    private final SmoothAnimation panelOpacity = new SmoothAnimation(0f, 10f);
    private final SmoothAnimation descriptionOpacity = new SmoothAnimation(0f, 12f);
    private final SmoothAnimation pillHighlightY = new SmoothAnimation(0f, 16f);
    private boolean pillHighlightInit = false;
    private final Map<String, SmoothAnimation> anims = new HashMap<>();

    // анимация смены категории: контент выезжает и проявляется
    private Category lastCategory = null;
    private final SmoothAnimation contentSwitch = new SmoothAnimation(0f, 13f);

    public ClickGuiRenderer() { instance = this; }
    public static ClickGuiRenderer getInstance() { return instance == null ? new ClickGuiRenderer() : instance; }

    private SmoothAnimation anim(String key, float initial, float speed) {
        return anims.computeIfAbsent(key, k -> new SmoothAnimation(initial, speed));
    }
    private float animatedValue(String key, float target, float speed) {
        SmoothAnimation a = anim(key, target, speed);
        a.setTarget(target);
        return a.update();
    }

    public static dev.darkvisuals.client.managers.ThemeManager.Theme[] getThemes() {
        return dev.darkvisuals.client.managers.ThemeManager.getInstance().getAvailableThemes();
    }
    public static Color liveThemeColor() {
        Color c = dev.darkvisuals.client.managers.ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
    }
    private static int theme() { return themeColor.getRGB() | 0xFF000000; }
    private static int themeA(int alpha) {
        return (alpha << 24) | (themeColor.getRed() << 16) | (themeColor.getGreen() << 8) | themeColor.getBlue();
    }

     
     
     
    public static float panelX(Window w) { return (w.getScaledWidth() - PANEL_W) / 2f; }
    public static float panelY(Window w) { return (w.getScaledHeight() - PANEL_H) / 2f + 10f; }
    public static float pillX(float px) { return px + SIDE_PAD; }
    public static float pillY(float py, int i) { return py + PILL_TOP + i * PILL_STEP; }
    public static float contentX(float px) { return px + CONTENT_X; }
    public static float contentTop(float py) { return py + CONTENT_TOP; }
    public static float contentBottom(float py) { return py + PANEL_H - CONTENT_BOTTOM; }
    public static float contentVisibleH() { return PANEL_H - CONTENT_TOP - CONTENT_BOTTOM; }

    public static boolean hit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static boolean isSearching() { return searchBuffer.length() > 0; }

    public static List<Module> getSearchResults() {
        String q = searchBuffer.toString().trim().toLowerCase();
        List<Module> out = new ArrayList<>();
        if (q.isEmpty()) return out;
        for (Module m : darkvisuals.getInstance().getModuleManager().getModules()) {
            String raw = m.getName() == null ? "" : m.getName().toLowerCase();
            String tr = I18n.translate(m.getName()).toLowerCase();
            if (raw.contains(q) || tr.contains(q)) out.add(m);
        }
        return out;
    }

    public static List<Module> visibleModules(ClickGuiState state) {
        return isSearching() ? getSearchResults() : state.getModules(currentCategory);
    }

    public static boolean isModuleTab(Category c) {
        return isSearching() || (c != Category.Friends && c != Category.Markers && c != Category.Theme && c != Category.Config);
    }

    private static Identifier iconFor(Category c) {
        if (c == Category.Render) return ICON_RENDER;
        if (c == Category.Utility) return ICON_UTILITY;
        if (c == Category.Theme) return ICON_THEME;
        if (c == Category.Config) return ICON_CONFIG;
        if (c == Category.Markers) return ICON_MARKERS;
        if (c == Category.Friends) return ICON_FRIENDS;
        return null;
    }

    private static String labelFor(Category c) {
        if (c == Category.Render) return "Visuals";
        if (c == Category.Utility) return "Utility";
        if (c == Category.Friends) return "Friends";
        if (c == Category.Markers) return "Markers";
        if (c == Category.Theme) return "Themes";
        if (c == Category.Config) return "Configs";
        return c.name();
    }

     
     
     
    public List<float[]> listPillLayout(ListSetting ls, float maxW) {
        List<float[]> out = new ArrayList<>();
        float x = 0, y = 0;
        for (BooleanSetting opt : ls.getValue()) {
            float w = width(Fonts.MEDIUM, I18n.translate(opt.getName()), 7.5f) + 12f;
            if (x > 0 && x + w > maxW) { x = 0; y += LIST_ROW_STEP; }
            out.add(new float[]{x, y, w});
            x += w + LIST_PILL_GAP;
        }
        return out;
    }

    public int listRows(ListSetting ls) {
        List<float[]> l = listPillLayout(ls, CONTENT_W - SETTINGS_PAD * 2);
        if (l.isEmpty()) return 1;
        return (int) (l.get(l.size() - 1)[1] / LIST_ROW_STEP) + 1;
    }

    public float settingHeight(Setting<?> s) {
        if (s instanceof NumberSetting) return 26f;
        if (s instanceof ListSetting ls) return listRows(ls) * LIST_ROW_STEP;
        if (s instanceof StringSetting) return 30f;
        if (s instanceof ButtonSetting) return 20f;
        return 18f;
    }

    public float settingsInnerHeight(Module m) {
        float h = 0;
        for (Setting<?> s : m.getSettings()) if (s.isVisible()) h += settingHeight(s);
        return h == 0 ? 18f : h;
    }

    public float settingsBlockHeight(Module m) { return SETTINGS_PAD * 2 + settingsInnerHeight(m); }

    public float modulesContentHeight(List<Module> mods) {
        float h = 0;
        for (Module m : mods) {
            h += ROW_H + ROW_GAP;
            if (m == openedSettingsModule) h += SETTINGS_GAP + settingsBlockHeight(m);
        }
        return h;
    }

    public static float themesContentHeight() { return 30f + getThemes().length * 30f; }

    public static float configContentHeight() {
        int n = darkvisuals.getInstance().getConfigManager().getConfigList().length;
        return INPUT_H + 10f + 14f + Math.max(1, n) * (LIST_ROW_H + LIST_ROW_GAP);
    }

    public static float friendsOptionsTop(float mY) { return mY + INPUT_H + 12f; }
    public static float friendsListTop(float mY, int optionCount) { return friendsOptionsTop(mY) + optionCount * OPTION_H + 18f; }
    public static float friendsContentHeight() {
        int options = getFriendOptions().size();
        int friends = dev.darkvisuals.client.managers.FriendsManager.getFriends().size();
        return INPUT_H + 12f + options * OPTION_H + 18f + 14f + Math.max(1, friends) * (LIST_ROW_H + LIST_ROW_GAP);
    }

    public static float markersListTop(float mY) { return mY + INPUT_H + 12f; }
    public static float markersContentHeight() {
        int n = dev.darkvisuals.client.managers.WaypointManager.list().size();
        return INPUT_H + 12f + 14f + Math.max(1, n) * (LIST_ROW_H + LIST_ROW_GAP);
    }

      
    public static float[] markersLayout(float cx, float cw) {
        float addW = 40, hereW = 44, coordW = 44, gap = 5;
        float nameW = cw - coordW * 2 - addW - hereW - gap * 4;
        float xX = cx + nameW + gap, zX = xX + coordW + gap;
        float addX = zX + coordW + gap, hereX = addX + addW + gap;
        return new float[]{cx, nameW, xX, zX, coordW, addX, addW, hereX, hereW};
    }

    public static class FriendOption {
        public final String labelKey;
        public final BooleanSetting setting;
        public FriendOption(String labelKey, BooleanSetting setting) { this.labelKey = labelKey; this.setting = setting; }
    }

    public static List<FriendOption> getFriendOptions() {
        List<FriendOption> list = new ArrayList<>();
        var mm = darkvisuals.getInstance().getModuleManager();
        var fh = mm.getModule(dev.darkvisuals.modules.impl.utility.FriendHelper.class);
        var ch = mm.getModule(dev.darkvisuals.modules.impl.render.ChinaHat.class);
        var tr = mm.getModule(dev.darkvisuals.modules.impl.render.Trails.class);
        var jc = mm.getModule(dev.darkvisuals.modules.impl.render.JumpCircle.class);
        if (fh != null) list.add(new FriendOption("Не ударять друзей", fh.getNoFriendDamage()));
        if (ch != null) list.add(new FriendOption("ChinaHat на друзьях", ch.getShowFriends()));
        if (tr != null) list.add(new FriendOption("Trails на друзьях", tr.getShowFriends()));
        if (jc != null) list.add(new FriendOption("JumpCircles на друзьях", jc.getShowFriends()));
        return list;
    }

    public static boolean isFriendOnline(String name) {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return false;
        for (var e : mc.getNetworkHandler().getPlayerList()) {
            if (e.getProfile() != null && e.getProfile().getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

     
     
     
    public void render(DrawContext context, int mouseX, int mouseY, float delta, Window window, ClickGuiState state) {
        render(context, mouseX, mouseY, delta, window, state, 1f);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta, Window window, ClickGuiState state, float openProgress) {
        if (window == null) return;
        openProgress = Math.max(0f, Math.min(1f, openProgress));
        if (openProgress <= 0.01f) return;
themeColor = liveThemeColor();
        panelScale.setTarget(0.94f + 0.06f * openProgress);
        panelOpacity.setTarget(openProgress);
        descriptionOpacity.setTarget(openProgress);
        globalAlpha = panelOpacity.update();

        context.draw();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        float sw = window.getScaledWidth(), sh = window.getScaledHeight();
        float scale = panelScale.update();

        // мягкое затемнение фона за панелью — интерфейс «отделяется» от игры
        float dimT = Math.max(0f, Math.min(1f, openProgress));
        int dimAlpha = (int) (140f * dimT);
        if (dimAlpha > 2) rect(context, 0, 0, sw, sh, 0f, withAlpha(0xFF07070C, dimAlpha));

        // отслеживаем смену категории для анимации контента
        if (lastCategory != currentCategory) {
            lastCategory = currentCategory;
            contentSwitch.snapTo(0f);
        }
        contentSwitch.setTarget(1f);
        float contentT = contentSwitch.update();

        var ms = context.getMatrices();
        ms.push();
        ms.translate(sw / 2f, sh / 2f, 0);
        ms.scale(scale, scale, 1f);
        ms.translate(-sw / 2f, -sh / 2f, 0);

        float px = panelX(window), py = panelY(window);

        renderSearchBar(context);
        renderTitle(context, sw);
        renderPanel(context, px, py);
        renderSidebar(context, px, py, mouseX, mouseY, state);
        String desc = renderContent(context, px, py, mouseX, mouseY, state, contentT);

if (desc != null && !desc.isEmpty()) {
            String d = I18n.translate(desc);
            float descriptionT = descriptionOpacity.update();
            float w = width(Fonts.BOLD, d, 14f);
            float descriptionY = sh - 30f + (1f - descriptionT) * 8f;
            // плашка-подложка под описанием модуля
            float padX = 12f, padY = 5f;
            rect(context, sw / 2f - w / 2f - padX, descriptionY - padY, w + padX * 2f, 14f + padY * 2f, 9f,
                    withAlpha(C_PANEL, (int) (210f * descriptionT)));
            rect(context, sw / 2f - w / 2f - padX, descriptionY - padY, w + padX * 2f, 14f + padY * 2f, 9f,
                    withAlpha(theme(), (int) (70f * descriptionT)));
            int descriptionColor = withAlpha(C_WHITE, (int) (255f * descriptionT));
            textShadow(context, Fonts.BOLD, d, sw / 2f - w / 2f, descriptionY, descriptionColor, 14f);
        }

        ms.pop();
        RenderSystem.enableDepthTest();
        globalAlpha = 1f;
    }

    private void renderSearchBar(DrawContext ctx) {
        float x = SEARCH_X, y = SEARCH_Y;
        float focusT = animatedValue("search:focus", searchInputFocused ? 1f : 0f, 12f);
        if (focusT > 0.01f) {
             
            float breathe = (float) (0.75f + 0.25f * Math.sin(System.currentTimeMillis() / 260.0));
            int borderColor = withAlpha(theme(), (int) (255f * focusT * breathe));
            rect(ctx, x - 1.5f, y - 1.5f, SEARCH_W + 3, SEARCH_H + 3, INPUT_R + 1.5f, borderColor);
        }
        rect(ctx, x, y, SEARCH_W, SEARCH_H, SEARCH_R, C_PILL);

        String q = searchBuffer.toString();
        boolean placeholder = q.isEmpty() && !searchInputFocused;
        String shown = placeholder ? "Поиск..." : q;
        float textX = x + 14, textY = y + (SEARCH_H - 10) / 2f;
        text(ctx, Fonts.BOLD, shown, textX, textY, placeholder ? 0xFF444444 : C_TEXT_DARK, 10f);
        if (searchInputFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            rect(ctx, textX + width(Fonts.BOLD, q, 10f) + 1, y + 8, 1, SEARCH_H - 16, 0, C_TEXT_DARK);
        }
        drawSearchIcon(ctx, x + SEARCH_W - 14 - SEARCH_ICON, y + (SEARCH_H - SEARCH_ICON) / 2f, SEARCH_ICON, C_TEXT_DARK, C_PILL);
    }

    private void renderTitle(DrawContext ctx, float sw) {
        String title = "Dark Visuals";
        float size = 20f;
        float tw = width(Fonts.BOLD, title, size);
        float total = LOGO_SIZE + 8 + tw;
        float x = sw / 2f - total / 2f, y = 8f;
        tex(ctx, LOGO, x, y, LOGO_SIZE, theme());
        textShadow(ctx, Fonts.BOLD, title, x + LOGO_SIZE + 8, y + (LOGO_SIZE - size) / 2f + 1, theme(), size);
    }

    private void renderPanel(DrawContext ctx, float px, float py) {
        int t = theme();
        rect(ctx, px, py, PANEL_W, PANEL_H, PANEL_R_TL, PANEL_R_TR, PANEL_R_BR, PANEL_R_BL, t);
        float ix = px + BORDER, iy = py + BORDER, iw = PANEL_W - BORDER * 2, ih = PANEL_H - BORDER * 2;
        rect(ctx, ix, iy, iw, ih, PANEL_R_TL - BORDER, PANEL_R_TR - BORDER, PANEL_R_BR - BORDER, PANEL_R_BL - BORDER, C_PANEL);

        // бегущий блик по верхней акцентной кромке панели
        float sheenT = (System.currentTimeMillis() % 4600L) / 4600f;
        float sheenX = px + sheenT * (PANEL_W + 180f) - 90f;
        ctx.enableScissor((int) (px + BORDER), (int) (py - 1), (int) (px + PANEL_W - BORDER), (int) (py + BORDER + 1));
        rect(ctx, sheenX, py, 90f, BORDER, BORDER / 2f, withAlpha(C_WHITE, 235));
        ctx.disableScissor();

        // мягкое внутреннее свечение сверху — «стекло»
        Render2D.drawGradientRect(ctx.getMatrices(), ix + 2, iy + 2, iw - 4, ih * 0.28f,
                new Color(255, 255, 255, 14), new Color(255, 255, 255, 0), false);

        // акцентные пятна-блобы медленно дышат и плывут
        final float BLOB_INSET = 8f;
        ctx.enableScissor((int) (ix + BLOB_INSET), (int) (iy + BLOB_INSET),
                (int) (ix + iw - BLOB_INSET), (int) (iy + ih - BLOB_INSET));
        long now = System.currentTimeMillis();
        float w1 = (float) Math.sin(now / 5200.0) * 16f;
        float w2 = (float) Math.cos(now / 6400.0) * 14f;
        float w3 = (float) Math.sin(now / 7100.0 + 2.0) * 18f;
        float breathe = 0.86f + 0.14f * (float) Math.sin(now / 1900.0);
        circle(ctx, px + 40 + w1, py - 30 + w2 * 0.5f, 95 * breathe, t);
        circle(ctx, px + 150 + w2, py - 75 + w1 * 0.5f, 90 * breathe, t);
        circle(ctx, px - 45 + w3 * 0.6f, py + 70 + w1, 80 * breathe, t);
        circle(ctx, px + PANEL_W - 30 + w1, py + PANEL_H + 10 + w3 * 0.5f, 90 * breathe, t);
        circle(ctx, px + PANEL_W - 130 + w2, py + PANEL_H + 45 + w1 * 0.5f, 80 * breathe, t);
        circle(ctx, px + PANEL_W + 30 + w3, py + PANEL_H - 80 + w2, 75 * breathe, t);
        ctx.disableScissor();

        // разделитель сайдбара
        rect(ctx, px + SEP_X, py + 16, 1, PANEL_H - 32, 0, C_SEP);
    }

    private void renderSidebar(DrawContext ctx, float px, float py, int mouseX, int mouseY, ClickGuiState state) {
        float x = pillX(px);

        int selIdx = 0;
        for (int i = 0; i < SIDEBAR_CATS.length; i++) if (SIDEBAR_CATS[i] == currentCategory) selIdx = i;
        float selY = pillY(py, selIdx);
        if (!pillHighlightInit) { pillHighlightY.snapTo(selY); pillHighlightInit = true; }
        pillHighlightY.setTarget(selY);
        float hlY = pillHighlightY.update();

        for (int i = 0; i < SIDEBAR_CATS.length; i++) {
            Category c = SIDEBAR_CATS[i];
            float y = pillY(py, i);
            boolean sel = c == currentCategory;
            boolean hov = hit(mouseX, mouseY, x, y, PILL_W, PILL_H);
            float selT = animatedValue("pillsel:" + c.name(), sel ? 1f : 0f, 14f);
            float hovT = animatedValue("pillhov:" + c.name(), hov ? 1f : 0f, 12f);

            float lift = animatedValue("pilllift:" + c.name(), hov ? 1.5f : 0f, 16f);

            int bg = lerpColor(lerpColor(C_PILL, C_PILL_HOVER, hovT), C_PILL, selT);
            bg = withAlpha(bg, (int) (255f * PILL_ALPHA));
            rect(ctx, x, y - lift, PILL_W, PILL_H, PILL_R * 1.4f, PILL_R * 0.6f, PILL_R * 0.6f, PILL_R * 1.4f, bg);
        }

        // мягкое свечение под выделенной пилюлей
        rect(ctx, x - 2, hlY - 2, PILL_W + 4, PILL_H + 4, PILL_R * 1.5f, withAlpha(theme(), 45));

        rect(ctx, x, hlY, PILL_W, PILL_H, PILL_R * 1.4f, PILL_R * 0.6f, PILL_R * 0.6f, PILL_R * 1.4f,
                withAlpha(theme(), (int) (255f * PILL_ALPHA)));

        for (int i = 0; i < SIDEBAR_CATS.length; i++) {
            Category c = SIDEBAR_CATS[i];
            float y = pillY(py, i);
            float selT = anim("pillsel:" + c.name(), 0f, 14f).getValue();
            float lift = anim("pilllift:" + c.name(), 0f, 16f).getValue();
            y -= lift;


            float pop = animatedValue("pillpop:" + c.name(), selT, 9f);
            float overshoot = 1f + 0.12f * (float) Math.sin(Math.min(1f, pop) * Math.PI) * selT;
            float iconSize = PILL_ICON_CIRCLE * overshoot;

            float cx = x + 5 + (PILL_ICON_CIRCLE - iconSize) / 2f, cy = y + (PILL_H - iconSize) / 2f;
            rect(ctx, cx, cy, iconSize, iconSize, iconSize / 2f, lerpColor(C_ICON_CIRCLE, C_WHITE, selT));
            Identifier icon = iconFor(c);
            if (icon != null) {
                float iw = PILL_ICON * overshoot;
                tex(ctx, icon, cx + (iconSize - iw) / 2f, cy + (iconSize - iw) / 2f,
                        iw, lerpColor(C_WHITE, C_ICON_CIRCLE, selT));
            }
            text(ctx, Fonts.BOLD, labelFor(c), x + 32, y + (PILL_H - 10) / 2f, lerpColor(C_TEXT_DARK, C_WHITE, selT), 10f);

            // бейдж с количеством модулей в категории
            if (isModuleTab(c)) {
                int count = state.getModules(c).size();
                if (count > 0) {
                    String cnt = String.valueOf(count);
                    float cntW = width(Fonts.BOLD, cnt, 7f) + 9f;
                    float bx = x + PILL_W - cntW - 7f;
                    rect(ctx, bx, y + (PILL_H - 12f) / 2f, cntW, 12f, 6f,
                            withAlpha(lerpColor(C_ICON_CIRCLE, C_WHITE, selT * 0.8f), (int) (255f * (0.6f + 0.4f * selT))));
                    text(ctx, Fonts.BOLD, cnt, bx + cntW / 2f - width(Fonts.BOLD, cnt, 7f) / 2f,
                            y + (PILL_H - 7f) / 2f, lerpColor(C_TEXT_DIM, C_TEXT_DARK, selT), 7f);
                }
            }
        }
    }

      
    private String renderContent(DrawContext ctx, float px, float py, int mouseX, int mouseY, ClickGuiState state, float contentT) {
        float cx = contentX(px), cw = CONTENT_W;
        float top = contentTop(py), bottom = contentBottom(py);
        float mY = top - state.getScroll(currentCategory);
        String desc = null;

        ctx.enableScissor((int) (px + SEP_X + 2), (int) top, (int) (px + PANEL_W - BORDER), (int) bottom);
        boolean inContent = hit(mouseX, mouseY, cx, top, cw, bottom - top);

        // анимация смены категории: контент выезжает справа и проявляется
        float ct = Math.max(0f, Math.min(1f, contentT));
        float slide = (1f - ct) * 18f;
        var contentMatrices = ctx.getMatrices();
        contentMatrices.push();
        contentMatrices.translate(slide, 0f, 0f);
        float prevAlpha = globalAlpha;
        globalAlpha *= ct;

        if (isModuleTab(currentCategory)) {
            desc = renderModules(ctx, cx, cw, mY, mouseX, mouseY, state, inContent, ct);
        } else if (currentCategory == Category.Theme) {
            renderThemesTab(ctx, cx, cw, mY, mouseX, mouseY);
        } else if (currentCategory == Category.Friends) {
            renderFriendsTab(ctx, cx, cw, mY, mouseX, mouseY);
        } else if (currentCategory == Category.Markers) {
            renderMarkersTab(ctx, cx, cw, mY, mouseX, mouseY);
        } else if (currentCategory == Category.Config) {
            renderConfigTab(ctx, cx, cw, mY, mouseX, mouseY);
        }

        globalAlpha = prevAlpha;
        contentMatrices.pop();
        ctx.disableScissor();
        return desc;
    }

     
     
     
    private String renderModules(DrawContext ctx, float cx, float cw, float mY, int mouseX, int mouseY, ClickGuiState state, boolean inContent, float contentT) {
        List<Module> mods = visibleModules(state);
        String desc = null;
        if (mods.isEmpty()) {
            text(ctx, Fonts.MEDIUM, isSearching() ? "Ничего не найдено" : "Пусто", cx + 4, mY + 4, C_TEXT_DIM, 9f);
            return null;
        }

        float rowY = mY;
        int rowIdx = 0;
        for (Module m : mods) {
            boolean expanded = m == openedSettingsModule;
            boolean hov = inContent && hit(mouseX, mouseY, cx, rowY, cw, ROW_H);
            float hovT = animatedValue("modhov:" + m.getName(), hov ? 1f : 0f, 12f);
            float togT = animatedValue("modtog:" + m.getName(), m.isToggled() ? 1f : 0f, 12f);

            // каскадное появление строк при смене категории
            float entrance = (contentT * (mods.size() + 3f) - rowIdx) / 2.5f;
            if (entrance < 0f) entrance = 0f;
            if (entrance > 1f) entrance = 1f;
            float rowOffset = (1f - entrance) * 10f;
            float prevAlpha = globalAlpha;
            globalAlpha *= entrance;

            float drawY = rowY + rowOffset;

            rect(ctx, cx, drawY, cw, ROW_H, ROW_R, lerpColor(C_ROW, C_ROW_HOVER, hovT));
            // тонкая акцентная полоска слева у включённых модулей
            if (togT > 0.02f) {
                rect(ctx, cx, drawY + 3f, 2.5f, ROW_H - 6f, 1.25f, withAlpha(theme(), (int) (200f * togT)));
            }
            text(ctx, Fonts.BOLD, I18n.translate(m.getName()), cx + 10, drawY + (ROW_H - 9) / 2f,
                    lerpColor(0xFFCFCFCF, C_WHITE, Math.max(togT, hovT * 0.6f)), 9f);

            // тумблер с пружинящим кружком
            float tx = cx + cw - 8 - TOGGLE_W, ty = drawY + (ROW_H - TOGGLE_H) / 2f;
            rect(ctx, tx, ty, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2f, lerpColor(C_INNER, theme(), togT));
            float knobPop = animatedValue("knobpop:" + m.getName(), togT, 8f);
            float knobOvershoot = 1f + 0.25f * (float) Math.sin(Math.min(1f, Math.max(0f, knobPop)) * Math.PI) * togT;
            float knobSize = 10f * knobOvershoot;
            float knobCX = tx + 1f + (TOGGLE_W - 12f) * togT + 5f;
            float knobCY = ty + 1f + 5f;
            rect(ctx, knobCX - knobSize / 2f, knobCY - knobSize / 2f, knobSize, knobSize, knobSize / 2f, C_WHITE);

            // шестерёнка настроек
            text(ctx, Fonts.MEDIUM, "/", tx - 10, drawY + (ROW_H - 9) / 2f, C_TEXT_DIM, 9f);
            float gx = tx - 10 - 6 - GEAR, gy = drawY + (ROW_H - GEAR) / 2f;
            boolean gearHov = inContent && hit(mouseX, mouseY, gx - 2, gy - 2, GEAR + 4, GEAR + 4);
            float gearT = animatedValue("gear:" + m.getName(), (gearHov || expanded) ? 1f : 0f, 14f);
            float gearRot = animatedValue("gearrot:" + m.getName(), expanded ? 1f : 0f, 8f);
            var gms = ctx.getMatrices();
            gms.push();
            gms.translate(gx + GEAR / 2f, gy + GEAR / 2f, 0);
            gms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(gearRot * 90f));
            gms.translate(-(gx + GEAR / 2f), -(gy + GEAR / 2f), 0);
            tex(ctx, ICON_OPTIONS, gx, gy, GEAR, lerpColor(0xFFCFCFCF, C_WHITE, gearT));
            gms.pop();

            globalAlpha = prevAlpha;
            if (hov) desc = m.getDescription();
            rowY += ROW_H;
            rowIdx++;

            // раскрывающийся блок настроек
            float expandT = animatedValue("expand:" + m.getName(), expanded ? 1f : 0f, 13f);
            if (expanded && expandT > 0.01f) {
                rowY += SETTINGS_GAP;
                float bh = settingsBlockHeight(m);
                float slide = (1f - expandT) * 6f;
                prevAlpha = globalAlpha;
                globalAlpha *= expandT;
                rect(ctx, cx, rowY + slide, cw, bh, SETTINGS_R, C_BLOCK);
                // акцентная кромка у блока настроек
                rect(ctx, cx, rowY + slide, 2.5f, bh, 1.25f, withAlpha(theme(), (int) (140f * expandT)));
                renderSettings(ctx, m, cx + SETTINGS_PAD, rowY + slide + SETTINGS_PAD, cw - SETTINGS_PAD * 2, mouseX, mouseY, inContent);
                globalAlpha = prevAlpha;
                rowY += bh;
            }
            rowY += ROW_GAP;
        }
        return desc;
    }

    private void renderSettings(DrawContext ctx, Module m, float sx, float sy, float sw, int mx, int my, boolean inContent) {
        float currY = sy;
        boolean any = false;
        for (Setting<?> s : m.getSettings()) {
            if (!s.isVisible()) continue;
            any = true;
            String name = I18n.translate(s.getName());
            String key = "set:" + System.identityHashCode(s);
            float h = settingHeight(s);

            if (s instanceof ListSetting ls) {
                List<float[]> layout = listPillLayout(ls, sw);
                List<BooleanSetting> opts = ls.getValue();
                for (int i = 0; i < opts.size(); i++) {
                    BooleanSetting opt = opts.get(i);
                    float[] p = layout.get(i);
                    float ox = sx + p[0], oy = currY + p[1];
                    boolean hov = inContent && hit(mx, my, ox, oy, p[2], LIST_PILL_H);
                    float t = animatedValue("opt:" + System.identityHashCode(opt), opt.getValue() ? 1f : 0f, 14f);
                    float hovT = animatedValue("opthov:" + System.identityHashCode(opt), hov ? 1f : 0f, 12f);
                    int bg = lerpColor(lerpColor(C_INNER, C_INNER_HOVER, hovT), theme(), t);
                    rect(ctx, ox, oy, p[2], LIST_PILL_H, OPTION_R, bg);
                    text(ctx, Fonts.MEDIUM, I18n.translate(opt.getName()), ox + 6, oy + (LIST_PILL_H - 7.5f) / 2f,
                            lerpColor(0xFFDDDDDD, C_WHITE, t), 7.5f);
                }
            }
            else if (s instanceof BooleanSetting b) {
                float t = animatedValue(key, b.getValue() ? 1f : 0f, 14f);
                text(ctx, Fonts.MEDIUM, name, sx, currY + 4, lerpColor(0xFFCCCCCC, C_WHITE, t), 8f);
                float tw = 20, th = 10, tx = sx + sw - tw, ty = currY + 3;
                rect(ctx, tx, ty, tw, th, th / 2f, lerpColor(C_INNER, theme(), t));
                rect(ctx, tx + 1 + (tw - 10) * t, ty + 1, 8, 8, 4, C_WHITE);
            }
            else if (s instanceof NumberSetting n) {
                text(ctx, Fonts.MEDIUM, name + ": " + formatNumber(n.getValue()), sx, currY + 1, C_WHITE, 7.5f);
                float p = (n.getValue() - n.getMin()) / (n.getMax() - n.getMin());
                float smooth = animatedValue(key, Math.max(0f, Math.min(1f, p)), 18f);
                float trackY = currY + 15;
                rect(ctx, sx, trackY, sw, 4, 2, C_INNER);
                float fill = sw * smooth;
                if (fill > 0) rect(ctx, sx, trackY, fill, 4, 2, theme());
                rect(ctx, sx + fill - 4, trackY - 2, 8, 8, 4, C_WHITE);
            }
            else if (s instanceof BindSetting bs) {
                boolean binding = bindingSetting == bs;
                String bindText = binding ? "..." : keyName(bs.getValue().getKey());
                float t = animatedValue(key, binding ? 1f : 0f, 14f);
                float bw = Math.max(34, width(Fonts.MEDIUM, bindText, 7f) + 12);
                text(ctx, Fonts.MEDIUM, name, sx, currY + 4, C_WHITE, 8f);
                rect(ctx, sx + sw - bw, currY + 2, bw, 12, 4, lerpColor(C_INNER, theme(), t));
                text(ctx, Fonts.MEDIUM, bindText, sx + sw - bw + 6, currY + 4.5f, C_WHITE, 7f);
            }
            else if (s instanceof EnumSetting<?> es) {
                String val = I18n.translate(es.currentEnumName());
                float vw = width(Fonts.MEDIUM, val, 7f) + 12;
                boolean hov = inContent && hit(mx, my, sx, currY, sw, h);
                float t = animatedValue(key, hov ? 1f : 0f, 20f);
                text(ctx, Fonts.MEDIUM, name, sx, currY + 4, C_WHITE, 8f);
                rect(ctx, sx + sw - vw, currY + 2, vw, 12, 4, lerpColor(C_INNER, theme(), t));
                text(ctx, Fonts.MEDIUM, val, sx + sw - vw + 6, currY + 4.5f, C_WHITE, 7f);
            }
            else if (s instanceof ButtonSetting) {
                boolean hov = inContent && hit(mx, my, sx, currY, sw, 16);
                float t = animatedValue(key, hov ? 1f : 0f, 20f);
                rect(ctx, sx, currY, sw, 16, 4, lerpColor(C_INNER, theme(), t));
                float lw = width(Fonts.MEDIUM, name, 7.5f);
                text(ctx, Fonts.MEDIUM, name, sx + (sw - lw) / 2f, currY + 4, C_WHITE, 7.5f);
            }
            else if (s instanceof StringSetting ss) {
                boolean editing = editingStringSetting == ss;
                text(ctx, Fonts.MEDIUM, name, sx, currY, 0xFFCCCCCC, 7.5f);
                float boxY = currY + 11;
                if (editing) rect(ctx, sx - 1, boxY - 1, sw + 2, 16, 5, theme());
                rect(ctx, sx, boxY, sw, 14, 4, C_INNER);
                String val = ss.getValue() == null ? "" : ss.getValue();
                String shown = val;
                while (width(Fonts.MEDIUM, shown, 7.5f) > sw - 10 && shown.length() > 1) shown = shown.substring(1);
                if (val.isEmpty() && !editing) shown = "...";
                text(ctx, Fonts.MEDIUM, shown, sx + 5, boxY + 3.5f, val.isEmpty() && !editing ? C_TEXT_DIM : C_WHITE, 7.5f);
                if (editing && (System.currentTimeMillis() / 500) % 2 == 0) {
                    rect(ctx, sx + 5 + width(Fonts.MEDIUM, shown, 7.5f) + 1, boxY + 2, 1, 10, 0, C_WHITE);
                }
            }
            currY += h;
        }
        if (!any) text(ctx, Fonts.MEDIUM, "Нет настроек", sx, currY + 4, C_TEXT_DIM, 8f);
    }

    private static String keyName(int key) {
        if (key <= 0) return "NONE";
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        if (key == GLFW.GLFW_KEY_ESCAPE) return "ESC";
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT) return "LSHIFT";
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        if (key == GLFW.GLFW_KEY_LEFT_CONTROL) return "LCTRL";
        if (key == GLFW.GLFW_KEY_RIGHT_CONTROL) return "RCTRL";
        if (key == GLFW.GLFW_KEY_LEFT_ALT) return "LALT";
        return "KEY_" + key;
    }

     
     
     
    private void renderThemesTab(DrawContext ctx, float cx, float cw, float mY, int mouseX, int mouseY) {
        var themes = getThemes();
        String currentName = dev.darkvisuals.client.managers.ThemeManager.getInstance().getCurrentTheme().getName();

        boolean addHov = hit(mouseX, mouseY, cx, mY, cw, 24);
        float addT = animatedValue("theme:add", addHov ? 1f : 0f, 12f);
        rect(ctx, cx, mY, cw, 24, ROW_R, lerpColor(C_ROW, theme(), addT));
        text(ctx, Fonts.BOLD, "+ Своя тема", cx + 10, mY + 7.5f, C_WHITE, 9f);

        float rowY = mY + 30;
        for (var t : themes) {
            Color tc = t.getAccentColor();
            boolean sel = currentName.equals(t.getName());
            boolean hov = hit(mouseX, mouseY, cx, rowY, cw, LIST_ROW_H);
            float selT = animatedValue("themesel:" + t.getName(), sel ? 1f : 0f, 12f);
            float hovT = animatedValue("themehov:" + t.getName(), hov ? 1f : 0f, 12f);
            int bg = lerpColor(lerpColor(C_ROW, C_ROW_HOVER, hovT), new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 255).getRGB(), selT);
            rect(ctx, cx, rowY, cw, LIST_ROW_H, ROW_R, bg);
            rect(ctx, cx + cw - 18, rowY + 6, 12, 12, 6, new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 255).getRGB());
            if (selT > 0.02f) rect(ctx, cx + cw - 20, rowY + 4, 16, 16, 8, lerpColor(0x00FFFFFF, C_WHITE, selT));
            if (selT > 0.02f) rect(ctx, cx + cw - 18, rowY + 6, 12, 12, 6, new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 255).getRGB());
            text(ctx, Fonts.BOLD, t.getName(), cx + 10, rowY + 7.5f, C_WHITE, 9f);
            rowY += 30;
        }
    }

     
     
     
    private void renderFriendsTab(DrawContext ctx, float cx, float cw, float mY, int mouseX, int mouseY) {
        float inputW = cw - BTN_W - 6, addX = cx + inputW + 6;

        drawInput(ctx, cx, mY, inputW, friendInputFocused, friendNameBuffer.toString(), "Ник игрока...");
        drawButton(ctx, addX, mY, BTN_W, INPUT_H, "ADD", "friend:add", mouseX, mouseY);

        if (friendStatus != null && System.currentTimeMillis() < friendStatusUntil) {
            text(ctx, Fonts.MEDIUM, friendStatus, cx, mY + INPUT_H + 2, 0xFF9BE39B, 8f);
        }

        List<FriendOption> options = getFriendOptions();
        float optY = friendsOptionsTop(mY);
        for (int i = 0; i < options.size(); i++) {
            FriendOption o = options.get(i);
            float y = optY + i * OPTION_H;
            float t = animatedValue("friendopt:" + i, o.setting.getValue() ? 1f : 0f, 14f);
            text(ctx, Fonts.MEDIUM, o.labelKey, cx, y + 4, lerpColor(0xFFCCCCCC, C_WHITE, t), 8.5f);
            float tw = 20, th = 10, tx = cx + cw - tw, ty = y + 3;
            rect(ctx, tx, ty, tw, th, th / 2f, lerpColor(C_INNER, theme(), t));
            rect(ctx, tx + 1 + (tw - 10) * t, ty + 1, 8, 8, 4, C_WHITE);
        }

        List<String> friends = dev.darkvisuals.client.managers.FriendsManager.getFriends();
        float listTop = friendsListTop(mY, options.size());
        text(ctx, Fonts.MEDIUM, I18n.translate("friends.header") + " " + friends.size(), cx, listTop, C_TEXT_DIM, 8.5f);

        float rowY = listTop + 14;
        for (String f : friends) {
            boolean hov = hit(mouseX, mouseY, cx, rowY, cw, LIST_ROW_H);
            float hovT = animatedValue("friendrow:" + f, hov ? 1f : 0f, 12f);
            rect(ctx, cx, rowY, cw, LIST_ROW_H, ROW_R, lerpColor(C_ROW, C_ROW_HOVER, hovT));
            boolean online = isFriendOnline(f);
            rect(ctx, cx + 8, rowY + 9, 6, 6, 3, online ? 0xFF4CD964 : 0xFF666666);
            text(ctx, Fonts.BOLD, f, cx + 20, rowY + 7.5f, C_WHITE, 9f);
            String status = online ? I18n.translate("friends.online") : I18n.translate("friends.offline");
            float delW = 40, delX = cx + cw - delW - 4;
            text(ctx, Fonts.MEDIUM, status, delX - width(Fonts.MEDIUM, status, 8f) - 8, rowY + 8, online ? 0xFF4CD964 : C_TEXT_DIM, 8f);
            text(ctx, Fonts.BOLD, "DEL", delX + delW / 2f - width(Fonts.BOLD, "DEL", 8f) / 2f, rowY + 8, 0xFFFF8888, 8f);
            rowY += LIST_ROW_H + LIST_ROW_GAP;
        }
        if (friends.isEmpty()) text(ctx, Fonts.MEDIUM, I18n.translate("friends.empty"), cx, rowY + 6, C_TEXT_DIM, 9f);
    }

     
     
     
    private void renderMarkersTab(DrawContext ctx, float cx, float cw, float mY, int mouseX, int mouseY) {
        float[] L = markersLayout(cx, cw);
        drawInput(ctx, L[0], mY, L[1], markerFocusedField == 1, markerNameBuffer.toString(), "Имя...");
        drawInput(ctx, L[2], mY, L[4], markerFocusedField == 2, markerXBuffer.toString(), "X");
        drawInput(ctx, L[3], mY, L[4], markerFocusedField == 3, markerZBuffer.toString(), "Z");
        drawButton(ctx, L[5], mY, L[6], INPUT_H, "ADD", "marker:add", mouseX, mouseY);
        drawButton(ctx, L[7], mY, L[8], INPUT_H, "HERE", "marker:here", mouseX, mouseY);

        if (markerStatus != null && System.currentTimeMillis() < markerStatusUntil) {
            text(ctx, Fonts.MEDIUM, markerStatus, cx, mY + INPUT_H + 2, 0xFF9BE39B, 8f);
        }

        var markers = dev.darkvisuals.client.managers.WaypointManager.list();
        float listTop = markersListTop(mY);
        text(ctx, Fonts.MEDIUM, "Markers: " + markers.size(), cx, listTop, C_TEXT_DIM, 8.5f);

        var mc = net.minecraft.client.MinecraftClient.getInstance();
        float rowY = listTop + 14;
        for (var w : markers) {
            boolean hov = hit(mouseX, mouseY, cx, rowY, cw, LIST_ROW_H);
            float hovT = animatedValue("markerrow:" + w.name, hov ? 1f : 0f, 12f);
            rect(ctx, cx, rowY, cw, LIST_ROW_H, ROW_R, lerpColor(C_ROW, C_ROW_HOVER, hovT));
            rect(ctx, cx + 8, rowY + 9, 6, 6, 3, theme());
            text(ctx, Fonts.BOLD, w.name, cx + 20, rowY + 7.5f, C_WHITE, 9f);
            String coords = (int) w.pos.x + ", " + (int) w.pos.z;
            if (mc.player != null) coords += "  (" + (int) mc.player.getPos().distanceTo(w.pos) + "m)";
            float delW = 40, delX = cx + cw - delW - 4;
            text(ctx, Fonts.MEDIUM, coords, delX - width(Fonts.MEDIUM, coords, 8f) - 8, rowY + 8, C_TEXT_DIM, 8f);
            text(ctx, Fonts.BOLD, "DEL", delX + delW / 2f - width(Fonts.BOLD, "DEL", 8f) / 2f, rowY + 8, 0xFFFF8888, 8f);
            rowY += LIST_ROW_H + LIST_ROW_GAP;
        }
        if (markers.isEmpty()) text(ctx, Fonts.MEDIUM, "Нет меток", cx, rowY + 6, C_TEXT_DIM, 9f);
    }

     
     
     
    private void renderConfigTab(DrawContext ctx, float cx, float cw, float mY, int mouseX, int mouseY) {
        float inputW = cw - BTN_W - 6, saveX = cx + inputW + 6;
        drawInput(ctx, cx, mY, inputW, configInputFocused, configNameBuffer.toString(), "Имя конфига...");
        drawButton(ctx, saveX, mY, BTN_W, INPUT_H, "SAVE", "cfg:save", mouseX, mouseY);

        float listTop = mY + INPUT_H + 10;
        if (configStatus != null && System.currentTimeMillis() < configStatusUntil) {
            text(ctx, Fonts.MEDIUM, configStatus, cx, listTop - 2, 0xFF9BE39B, 8f);
        }

        String[] configs = darkvisuals.getInstance().getConfigManager().getConfigList();
        float rowY = listTop + 14;
        for (String cfg : configs) {
            boolean hov = hit(mouseX, mouseY, cx, rowY, cw, LIST_ROW_H);
            float hovT = animatedValue("cfgrow:" + cfg, hov ? 1f : 0f, 12f);
            rect(ctx, cx, rowY, cw, LIST_ROW_H, ROW_R, lerpColor(C_ROW, C_ROW_HOVER, hovT));
            text(ctx, Fonts.BOLD, cfg, cx + 10, rowY + 7.5f, C_WHITE, 9f);
            float delW = 40, loadW = 44;
            float delX = cx + cw - delW - 4, loadX = delX - loadW - 4;
            text(ctx, Fonts.BOLD, "LOAD", loadX + loadW / 2f - width(Fonts.BOLD, "LOAD", 8f) / 2f, rowY + 8, 0xFF9BC7FF, 8f);
            text(ctx, Fonts.BOLD, "DEL", delX + delW / 2f - width(Fonts.BOLD, "DEL", 8f) / 2f, rowY + 8, 0xFFFF8888, 8f);
            rowY += LIST_ROW_H + LIST_ROW_GAP;
        }
        if (configs.length == 0) text(ctx, Fonts.MEDIUM, "Нет сохранённых конфигов", cx, rowY + 6, C_TEXT_DIM, 9f);
    }

     
     
     
    private void drawInput(DrawContext ctx, float x, float y, float w, boolean focused, String value, String placeholder) {
        if (focused) rect(ctx, x - 1, y - 1, w + 2, INPUT_H + 2, ROW_R + 1, theme());
        rect(ctx, x, y, w, INPUT_H, INPUT_R, focused ? C_INNER_HOVER : C_INNER);
        boolean ph = value.isEmpty() && !focused;
        String shown = ph ? placeholder : value;
        while (width(Fonts.MEDIUM, shown, 9f) > w - 14 && shown.length() > 1) shown = shown.substring(1);
        text(ctx, Fonts.MEDIUM, shown, x + 7, y + 7, ph ? C_TEXT_DIM : C_WHITE, 9f);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            rect(ctx, x + 7 + width(Fonts.MEDIUM, shown, 9f) + 1, y + 5, 1, 12, 0, C_WHITE);
        }
    }

    private void drawButton(DrawContext ctx, float x, float y, float w, float h, String label, String animKey, int mx, int my) {
        boolean hov = hit(mx, my, x, y, w, h);
        float t = animatedValue(animKey, hov ? 1f : 0f, 12f);
        rect(ctx, x, y, w, h, BUTTON_R, lerpColor(themeA(190), theme(), t));
        text(ctx, Fonts.BOLD, label, x + w / 2f - width(Fonts.BOLD, label, 9f) / 2f, y + (h - 9) / 2f, C_WHITE, 9f);
    }

      
    private void drawSearchIcon(DrawContext ctx, float x, float y, float size, int color, int bg) {
        float ring = size * 0.68f;
        float thick = Math.max(1.4f, size * 0.17f);
        rect(ctx, x, y, ring, ring, ring / 2f, color);
        float in = ring - thick * 2f;
        rect(ctx, x + thick, y + thick, in, in, in / 2f, bg);
        int c = a(color);
        Render2D.drawLine(ctx.getMatrices(), x + ring * 0.82f, y + ring * 0.82f, x + size, y + size, thick, new Color(c, true));
    }

    private int a(int color) {
        if (globalAlpha >= 1f) return color;
        int al = (int) (((color >>> 24) & 0xFF) * Math.max(0f, globalAlpha));
        return (al << 24) | (color & 0x00FFFFFF);
    }

    public void rect(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        int c = a(color);
        if (((c >>> 24) & 0xFF) == 0 || w <= 0 || h <= 0) return;
        Render2D.drawRoundedRect(ctx.getMatrices(), x, y, w, h, Math.min(r, Math.min(w, h) / 2f), new Color(c, true));
    }

 
    public void rect(DrawContext ctx, float x, float y, float w, float h,
                      float rTL, float rTR, float rBR, float rBL, int color) {
        int c = a(color);
        if (((c >>> 24) & 0xFF) == 0 || w <= 0 || h <= 0) return;
        float cap = Math.min(w, h) / 2f;
        Render2D.drawRoundedRect(ctx.getMatrices(), x, y, w, h,
                Math.min(rTL, cap), Math.min(rTR, cap), Math.min(rBR, cap), Math.min(rBL, cap), new Color(c, true));
    }

    private void circle(DrawContext ctx, float cx, float cy, float r, int color) {
        rect(ctx, cx - r, cy - r, r * 2, r * 2, r, color);
    }

    private void tex(DrawContext ctx, Identifier id, float x, float y, float size, int tint) {
        int c = a(tint);
        RenderSystem.setShaderColor(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, ((c >>> 24) & 0xFF) / 255f);
        ctx.drawTexture(RenderLayer::getGuiTextured, id, (int) x, (int) y, 0, 0, (int) size, (int) size, (int) size, (int) size);
        ctx.draw();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public void text(DrawContext ctx, Font font, String s, float x, float y, int color, float size) {
        if (s == null || s.isBlank()) return;
        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(0, 0, 150);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            Render2D.drawFont(ms, font.getFont(size), s, x, y, new Color(a(color), true));
        } catch (IllegalStateException ignored) {
             
        }
        ms.pop();
    }

    public void textShadow(DrawContext ctx, Font font, String s, float x, float y, int color, float size) {
        text(ctx, font, s, x + 0.7f, y + 0.7f, 0xFF000000, size);
        text(ctx, font, s, x, y, color, size);
    }

    public float width(Font font, String s, float size) { return font.getWidth(s, size); }

    private String formatNumber(float v) { return v == (int) v ? String.valueOf((int) v) : String.format("%.1f", v); }

    private static int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
        int r = (int) (((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t));
        int g = (int) (((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t));
        int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
