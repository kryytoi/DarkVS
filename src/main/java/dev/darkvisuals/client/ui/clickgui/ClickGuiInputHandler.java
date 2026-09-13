package dev.darkvisuals.client.ui.clickgui;

import dev.darkvisuals.client.managers.FriendsManager;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.modules.settings.impl.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static dev.darkvisuals.client.ui.clickgui.ClickGuiRenderer.*;

public class ClickGuiInputHandler {
    private final ClickGuiState state;
    public ClickGuiInputHandler(ClickGuiState state) { this.state = state; }

    private ClickGuiRenderer r() { return ClickGuiRenderer.getInstance(); }

     
     
     
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        Window window = MinecraftClient.getInstance().getWindow();
        float px = panelX(window), py = panelY(window);
        if (!hit(mouseX, mouseY, px, py, PANEL_W, PANEL_H)) return false;

        float max = getMaxScroll();
        float cur = state.getScroll(currentCategory);
        state.setScroll(currentCategory, Math.max(0, Math.min(max, cur - (float) amount * 20)));
        return true;
    }

    private float getMaxScroll() {
        float content;
        if (isModuleTab(currentCategory)) content = r().modulesContentHeight(visibleModules(state));
        else if (currentCategory == Category.Theme) content = themesContentHeight();
        else if (currentCategory == Category.Friends) content = friendsContentHeight();
        else if (currentCategory == Category.Markers) content = markersContentHeight();
        else content = configContentHeight();
        return Math.max(0, content - contentVisibleH());
    }

     
     
     
    public boolean mouseClicked(double mouseX, double mouseY, int button, Window window) {
        float px = panelX(window), py = panelY(window);

         
        if (hit(mouseX, mouseY, SEARCH_X, SEARCH_Y, SEARCH_W, SEARCH_H)) {
            searchInputFocused = true;
            unfocusAllExcept(1);
            return true;
        }
        searchInputFocused = false;

         
        for (int i = 0; i < SIDEBAR_CATS.length; i++) {
            if (hit(mouseX, mouseY, pillX(px), pillY(py, i), PILL_W, PILL_H)) {
                Category c = SIDEBAR_CATS[i];
                if (currentCategory != c) {
                    currentCategory = c;
                    state.setScroll(c, 0);
                    openedSettingsModule = null;
                    bindingSetting = null;
                }
                unfocusAllExcept(0);
                return true;
            }
        }

        float cx = contentX(px), cw = CONTENT_W;
        float top = contentTop(py), bottom = contentBottom(py);
        boolean inContent = hit(mouseX, mouseY, cx, top, cw, bottom - top);
        boolean inPanel = hit(mouseX, mouseY, px, py, PANEL_W, PANEL_H);
        float mY = top - state.getScroll(currentCategory);

        if (!inContent) {
            unfocusAllExcept(0);
            return inPanel;
        }

        if (isModuleTab(currentCategory)) return clickModules(mouseX, mouseY, button, cx, cw, mY);
        if (currentCategory == Category.Theme) return clickThemes(mouseX, mouseY, cx, cw, mY);
        if (currentCategory == Category.Friends) return clickFriends(mouseX, mouseY, cx, cw, mY);
        if (currentCategory == Category.Markers) return clickMarkers(mouseX, mouseY, cx, cw, mY);
        if (currentCategory == Category.Config) return clickConfig(mouseX, mouseY, cx, cw, mY);
        return true;
    }

      
    private void unfocusAllExcept(int keep) {
        if (keep != 1) searchInputFocused = false;
        if (keep != 2) friendInputFocused = false;
        if (keep != 3) markerFocusedField = 0;
        if (keep != 4) configInputFocused = false;
        if (keep != 5) editingStringSetting = null;
    }

    private boolean clickModules(double mx, double my, int button, float cx, float cw, float mY) {
        List<Module> mods = visibleModules(state);
        float rowY = mY;
        for (Module m : mods) {
            boolean expanded = m == openedSettingsModule;

            if (hit(mx, my, cx, rowY, cw, ROW_H)) {
                float tx = cx + cw - 8 - TOGGLE_W;
                float gx = tx - 10 - 6 - GEAR, gy = rowY + (ROW_H - GEAR) / 2f;
                boolean onGear = hit(mx, my, gx - 3, gy - 3, GEAR + 6, GEAR + 6);
                if (onGear || button == 1) {
                    openedSettingsModule = expanded ? null : m;
                    bindingSetting = null;
                    editingStringSetting = null;
                    return true;
                }
                if (button == 0) m.setToggled(!m.isToggled());
                return true;
            }
            rowY += ROW_H;

            if (expanded) {
                rowY += SETTINGS_GAP;
                float bh = r().settingsBlockHeight(m);
                if (hit(mx, my, cx, rowY, cw, bh)) {
                    clickSettings(m, mx, my, cx + SETTINGS_PAD, rowY + SETTINGS_PAD, cw - SETTINGS_PAD * 2);
                    return true;
                }
                rowY += bh;
            }
            rowY += ROW_GAP;
        }
        editingStringSetting = null;
        return true;
    }

    private void clickSettings(Module m, double mx, double my, float sx, float sy, float sw) {
        editingStringSetting = null;
        float currY = sy;
        for (Setting<?> s : m.getSettings()) {
            if (!s.isVisible()) continue;
            float h = r().settingHeight(s);

            if (hit(mx, my, sx, currY, sw, h)) {
                if (s instanceof ListSetting ls) {
                    List<float[]> layout = r().listPillLayout(ls, sw);
                    List<BooleanSetting> opts = ls.getValue();
                    for (int i = 0; i < opts.size(); i++) {
                        float[] p = layout.get(i);
                        if (hit(mx, my, sx + p[0], currY + p[1], p[2], LIST_PILL_H)) {
                            BooleanSetting opt = opts.get(i);
                            if (ls.isSingleSelect()) {
                                if (!opt.getValue()) {
                                    opts.forEach(o -> o.setValue(false));
                                    opt.setValue(true);
                                }
                            } else opt.setValue(!opt.getValue());
                            return;
                        }
                    }
                }
                else if (s instanceof BooleanSetting b) b.setValue(!b.getValue());
                else if (s instanceof NumberSetting n) {
                    draggingSlider = n;
                    sliderTrackX = sx;
                    sliderTrackW = sw;
                    applySlider(n, mx);
                }
                else if (s instanceof BindSetting bs) bindingSetting = bs;
                else if (s instanceof ButtonSetting btn) btn.click();
                else if (s instanceof EnumSetting<?> es) es.increaseEnum();
                else if (s instanceof StringSetting ss) {
                    editingStringSetting = ss;
                    unfocusAllExcept(5);
                }
                return;
            }
            currY += h;
        }
    }

    private boolean clickThemes(double mx, double my, float cx, float cw, float mY) {
        if (hit(mx, my, cx, mY, cw, 24)) {
            MinecraftClient.getInstance().setScreen(new dev.darkvisuals.client.ui.colorgui.CustomThemeScreen());
            return true;
        }
        float rowY = mY + 30;
        for (var t : getThemes()) {
            if (hit(mx, my, cx, rowY, cw, LIST_ROW_H)) {
                dev.darkvisuals.client.managers.ThemeManager.getInstance().setTheme(t);
                themeColor = liveThemeColor();
                return true;
            }
            rowY += 30;
        }
        return true;
    }

    private boolean clickFriends(double mx, double my, float cx, float cw, float mY) {
        float inputW = cw - BTN_W - 6, addX = cx + inputW + 6;

        if (hit(mx, my, cx, mY, inputW, INPUT_H)) { friendInputFocused = true; unfocusAllExcept(2); return true; }
        if (hit(mx, my, addX, mY, BTN_W, INPUT_H)) { addCurrentFriend(); return true; }

        List<FriendOption> options = getFriendOptions();
        float optY = friendsOptionsTop(mY);
        for (int i = 0; i < options.size(); i++) {
            if (hit(mx, my, cx, optY + i * OPTION_H, cw, OPTION_H)) {
                BooleanSetting s = options.get(i).setting;
                s.setValue(!s.getValue());
                return true;
            }
        }

        float rowY = friendsListTop(mY, options.size()) + 14;
        for (String f : FriendsManager.getFriends()) {
            float delW = 40, delX = cx + cw - delW - 4;
            if (hit(mx, my, delX, rowY, delW, LIST_ROW_H)) {
                FriendsManager.removeFriend(f);
                setFriendStatus("Удалён: " + f);
                return true;
            }
            rowY += LIST_ROW_H + LIST_ROW_GAP;
        }
        friendInputFocused = false;
        return true;
    }

    private boolean clickMarkers(double mx, double my, float cx, float cw, float mY) {
        float[] L = markersLayout(cx, cw);
        if (hit(mx, my, L[0], mY, L[1], INPUT_H)) { markerFocusedField = 1; unfocusAllExcept(3); return true; }
        if (hit(mx, my, L[2], mY, L[4], INPUT_H)) { markerFocusedField = 2; unfocusAllExcept(3); return true; }
        if (hit(mx, my, L[3], mY, L[4], INPUT_H)) { markerFocusedField = 3; unfocusAllExcept(3); return true; }
        if (hit(mx, my, L[5], mY, L[6], INPUT_H)) { addCurrentMarker(); return true; }
        if (hit(mx, my, L[7], mY, L[8], INPUT_H)) { addMarkerHere(); return true; }

        float rowY = markersListTop(mY) + 14;
        for (var w : dev.darkvisuals.client.managers.WaypointManager.list()) {
            float delW = 40, delX = cx + cw - delW - 4;
            if (hit(mx, my, delX, rowY, delW, LIST_ROW_H)) {
                dev.darkvisuals.client.managers.WaypointManager.remove(w.name);
                setMarkerStatus("Удалена: " + w.name);
                return true;
            }
            rowY += LIST_ROW_H + LIST_ROW_GAP;
        }
        markerFocusedField = 0;
        return true;
    }

    private boolean clickConfig(double mx, double my, float cx, float cw, float mY) {
        float inputW = cw - BTN_W - 6, saveX = cx + inputW + 6;
        if (hit(mx, my, cx, mY, inputW, INPUT_H)) { configInputFocused = true; unfocusAllExcept(4); return true; }
        if (hit(mx, my, saveX, mY, BTN_W, INPUT_H)) { saveCurrentConfig(); return true; }

        var cm = dev.darkvisuals.darkvisuals.getInstance().getConfigManager();
        float rowY = mY + INPUT_H + 10 + 14;
        for (String cfg : cm.getConfigList()) {
            float delW = 40, loadW = 44;
            float delX = cx + cw - delW - 4, loadX = delX - loadW - 4;
            if (hit(mx, my, loadX, rowY, loadW, LIST_ROW_H)) {
                cm.loadConfig(cfg).thenAccept(ok -> setConfigStatus(ok ? "Загружено: " + cfg : "Ошибка загрузки: " + cfg));
                return true;
            }
            if (hit(mx, my, delX, rowY, delW, LIST_ROW_H)) {
                boolean ok = cm.deleteConfig(cfg);
                setConfigStatus(ok ? "Удалено: " + cfg : "Ошибка удаления: " + cfg);
                return true;
            }
            rowY += LIST_ROW_H + LIST_ROW_GAP;
        }
        configInputFocused = false;
        return true;
    }

     
     
     
    public boolean mouseDragged(double mx, double my, int b) {
        if (draggingSlider != null) { applySlider(draggingSlider, mx); return true; }
        return false;
    }

    private void applySlider(NumberSetting s, double mx) {
        float p = Math.max(0, Math.min(1, (float) ((mx - sliderTrackX) / sliderTrackW)));
        float val = s.getMin() + p * (s.getMax() - s.getMin());
        if (s.getIncrement() > 0) val = Math.round(val / s.getIncrement()) * s.getIncrement();
        s.setValue(Math.max(s.getMin(), Math.min(s.getMax(), val)));
    }

    public boolean mouseReleased(int b) { draggingSlider = null; return false; }

     
     
     
    public boolean keyPressed(int k, int m) {
        boolean enter = k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER;

        if (editingStringSetting != null) {
            StringSetting ss = editingStringSetting;
            if (k == GLFW.GLFW_KEY_BACKSPACE) {
                String v = ss.getValue();
                if (v != null && !v.isEmpty()) ss.setValue(v.substring(0, v.length() - 1));
            } else if (enter || k == GLFW.GLFW_KEY_ESCAPE) editingStringSetting = null;
            return true;
        }

        if (searchInputFocused) {
            if (k == GLFW.GLFW_KEY_BACKSPACE) { if (searchBuffer.length() > 0) searchBuffer.deleteCharAt(searchBuffer.length() - 1); }
            else if (k == GLFW.GLFW_KEY_ESCAPE) { searchBuffer.setLength(0); searchInputFocused = false; }
            else if (enter) searchInputFocused = false;
            return true;
        }

        if (friendInputFocused) {
            if (k == GLFW.GLFW_KEY_BACKSPACE) { if (friendNameBuffer.length() > 0) friendNameBuffer.deleteCharAt(friendNameBuffer.length() - 1); }
            else if (enter) addCurrentFriend();
            else if (k == GLFW.GLFW_KEY_ESCAPE) friendInputFocused = false;
            return true;
        }

        if (markerFocusedField != 0) {
            StringBuilder sb = markerFocusedField == 1 ? markerNameBuffer : markerFocusedField == 2 ? markerXBuffer : markerZBuffer;
            if (k == GLFW.GLFW_KEY_BACKSPACE) { if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1); }
            else if (k == GLFW.GLFW_KEY_TAB) markerFocusedField = markerFocusedField % 3 + 1;
            else if (enter) addCurrentMarker();
            else if (k == GLFW.GLFW_KEY_ESCAPE) markerFocusedField = 0;
            return true;
        }

        if (configInputFocused) {
            if (k == GLFW.GLFW_KEY_BACKSPACE) { if (configNameBuffer.length() > 0) configNameBuffer.deleteCharAt(configNameBuffer.length() - 1); }
            else if (enter) saveCurrentConfig();
            else if (k == GLFW.GLFW_KEY_ESCAPE) configInputFocused = false;
            return true;
        }
        return false;
    }

    public boolean charTyped(char c, int m) {
        if (Character.isISOControl(c)) return editingStringSetting != null || searchInputFocused
                || friendInputFocused || markerFocusedField != 0 || configInputFocused;

        if (editingStringSetting != null) {
            StringSetting ss = editingStringSetting;
            if (ss.isOnlyDigit() && !Character.isDigit(c)) return true;
            String cur = ss.getValue() == null ? "" : ss.getValue();
            if (cur.length() < 32) ss.setValue(cur + c);
            return true;
        }
        if (searchInputFocused) {
            if (searchBuffer.length() < 24) searchBuffer.append(c);
            return true;
        }
        if (friendInputFocused) {
            if (friendNameBuffer.length() < 16 && (Character.isLetterOrDigit(c) || c == '_') && c < 128) friendNameBuffer.append(c);
            return true;
        }
        if (markerFocusedField != 0) {
            if (markerFocusedField == 1) { if (markerNameBuffer.length() < 24) markerNameBuffer.append(c); }
            else {
                StringBuilder sb = markerFocusedField == 2 ? markerXBuffer : markerZBuffer;
                if (sb.length() < 8 && (Character.isDigit(c) || (c == '-' && sb.length() == 0))) sb.append(c);
            }
            return true;
        }
        if (configInputFocused) {
            if (configNameBuffer.length() < 32 && (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_')) configNameBuffer.append(c);
            return true;
        }
        return false;
    }

     
     
     
    private void addCurrentFriend() {
        String name = friendNameBuffer.toString().trim();
        if (name.isEmpty()) { setFriendStatus("Введите ник игрока"); return; }
        var mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getGameProfile().getName().equalsIgnoreCase(name)) { setFriendStatus("Нельзя добавить себя"); return; }
        for (String f : FriendsManager.getFriends()) {
            if (f.equalsIgnoreCase(name)) { setFriendStatus("Уже в друзьях: " + f); return; }
        }
        FriendsManager.addFriend(name);
        friendNameBuffer.setLength(0);
        setFriendStatus("Добавлен: " + name);
    }

    private void addCurrentMarker() {
        String name = markerNameBuffer.toString().trim();
        String xs = markerXBuffer.toString().trim(), zs = markerZBuffer.toString().trim();
        if (xs.isEmpty() || zs.isEmpty()) { setMarkerStatus("Введите X и Z"); return; }
        double x, z;
        try { x = Double.parseDouble(xs); z = Double.parseDouble(zs); }
        catch (NumberFormatException e) { setMarkerStatus("Неверные координаты"); return; }
        if (name.isEmpty()) name = (int) x + " " + (int) z;
        dev.darkvisuals.client.managers.WaypointManager.add(name, new net.minecraft.util.math.Vec3d(x + 0.5, 60.0, z + 0.5));
        setMarkerStatus("Добавлена: " + name);
        markerNameBuffer.setLength(0); markerXBuffer.setLength(0); markerZBuffer.setLength(0);
        markerFocusedField = 0;
    }

    private void addMarkerHere() {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) { setMarkerStatus("Игрок не в мире"); return; }
        String name = markerNameBuffer.toString().trim();
        var pos = mc.player.getPos();
        if (name.isEmpty()) name = (int) pos.x + " " + (int) pos.z;
        dev.darkvisuals.client.managers.WaypointManager.add(name, pos);
        setMarkerStatus("Добавлена: " + name);
        markerNameBuffer.setLength(0);
        markerFocusedField = 0;
    }

    private void saveCurrentConfig() {
        String name = configNameBuffer.toString().trim();
        if (name.isEmpty()) { setConfigStatus("Введите имя конфига"); return; }
        dev.darkvisuals.darkvisuals.getInstance().getConfigManager().saveConfig(name)
                .thenAccept(ok -> setConfigStatus(ok ? "Сохранено: " + name : "Ошибка сохранения: " + name));
    }

    private void setMarkerStatus(String s) {
        markerStatus = s;
        markerStatusUntil = System.currentTimeMillis() + 2500L;
    }

    private void setFriendStatus(String text) {
        onMain(() -> { friendStatus = text; friendStatusUntil = System.currentTimeMillis() + 3000L; });
    }

    private void setConfigStatus(String text) {
        onMain(() -> { configStatus = text; configStatusUntil = System.currentTimeMillis() + 3000L; });
    }

    private void onMain(Runnable r) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) r.run(); else client.execute(r);
    }
}