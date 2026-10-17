package dev.darkvisuals.client.ui.hud;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.hud.impl.TargetHud;
import dev.darkvisuals.client.util.math.MathUtils;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.HUD;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.*;

public class HudEditorScreen extends ChatScreen {

    private static final int GRID = 20;
    private static boolean isOpen = false;

    private static boolean gridEnabled = true;
    private static boolean snapEnabled = true;

    private final Set<HudElement> selectedElements = new LinkedHashSet<>();
    private HudElement activeElement = null;

    // Режим рамки выделения мышью (Windows Marquee)
    private boolean isSelectingBox = false;
    private float boxStartX, boxStartY;
    private float boxCurX, boxCurY;

    // Режим перетаскивания элементов (Drag)
    private boolean isDraggingElements = false;
    private float dragStartMouseX, dragStartMouseY;
    private final Map<HudElement, Float> initialElementX = new HashMap<>();
    private final Map<HudElement, Float> initialElementY = new HashMap<>();

    // Режим изменения размера по 4 углам (Blender-style cubes)
    private HudElement resizingElement = null;
    private int resizingCorner = -1; // 0: Top-Left, 1: Top-Right, 2: Bottom-Right, 3: Bottom-Left
    private float initialScale = 1.0f;
    private float initialBaseW = 0f;
    private float initialBaseH = 0f;

    // Режим изменения внутреннего элемента (круг HP в TargetHUD)
    private boolean isResizingTargetHp = false;

    // Всплывающие уведомления (Toast)
    private String toastMessage = "";
    private long toastTime = 0L;

    public HudEditorScreen() {
        super("");
    }

    public static boolean isOpen() {
        return isOpen;
    }

    @Override
    protected void init() {
        super.init();
        isOpen = true;
        if (!Fonts.isLoaded()) {
            Fonts.load();
        }
        if (chatField != null) {
            chatField.setVisible(false);
            chatField.setEditable(false);
            chatField.setFocused(false);
        }
        setFocused(null);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int w = width, h = height;
        int accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor().getRGB() | 0xFF000000;

        // Мягкий темный фон редактора
        ctx.fill(0, 0, w, h, 0x65000000);

        // Минималистичная точечная сетка (Dot Grid)
        if (gridEnabled) {
            for (int x = GRID; x < w; x += GRID) {
                for (int y = GRID; y < h; y += GRID) {
                    ctx.fill(x, y, x + 1, y + 1, 0x22FFFFFF);
                }
            }
        }

        // Центральные направляющие
        ctx.fill(w / 2, 0, w / 2 + 1, h, 0x25FFFFFF);
        ctx.fill(0, h / 2, w, h / 2 + 1, 0x25FFFFFF);

        // Умные направляющие при перетаскивании (Smart Alignment Guides)
        if (isDraggingElements && activeElement != null) {
            float elCenterX = activeElement.getX() + activeElement.getWidth() / 2f;
            float elCenterY = activeElement.getY() + activeElement.getHeight() / 2f;
            if (Math.abs(elCenterX - w / 2f) < 4f) {
                ctx.fill(w / 2, 0, w / 2 + 1, h, accent);
            }
            if (Math.abs(elCenterY - h / 2f) < 4f) {
                ctx.fill(0, h / 2, w, h / 2 + 1, accent);
            }
        }

        // Отрисовка рамок и маркеров элементов HUD
        List<HudElement> elements = darkvisuals.getInstance().getHudManager().getHudElements();
        for (HudElement el : elements) {
            if (!isElementVisible(el)) continue;
            if (el.getWidth() <= 0 || el.getHeight() <= 0) continue;

            int ex = Math.round(el.getX());
            int ey = Math.round(el.getY());
            int ew = Math.round(el.getWidth());
            int eh = Math.round(el.getHeight());

            boolean isHovered = mouseX >= ex && mouseX <= ex + ew && mouseY >= ey && mouseY <= ey + eh;
            boolean isSelected = selectedElements.contains(el);
            boolean isActive = (el == activeElement);

            // Тонкая рамка вокруг элемента (1px)
            if (isSelected) {
                ctx.drawBorder(ex - 2, ey - 2, ew + 4, eh + 4, accent);
                ctx.fill(ex - 2, ey - 2, ex + ew + 2, ey + eh + 2, (accent & 0x00FFFFFF) | 0x15000000);
            } else if (isHovered) {
                ctx.drawBorder(ex - 2, ey - 2, ew + 4, eh + 4, 0x70FFFFFF);
            } else {
                ctx.drawBorder(ex - 2, ey - 2, ew + 4, eh + 4, 0x25FFFFFF);
            }

            // Название элемента сверху
            String localizedName = getLocalizedName(el.getName());
            if (isSelected || isHovered) {
                String dimText = localizedName + "  " + (int) el.getX() + ", " + (int) el.getY();
                if (Fonts.isLoaded() && Fonts.MEDIUM != null) {
                    float fontSz = 7.5f;
                    float th = Fonts.MEDIUM.getHeight(fontSz);
                    float ty = ey - th - 3f;
                    if (ty < 2f) ty = ey + eh + 4f;
                    Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(fontSz), dimText, ex, ty, new Color(isSelected ? accent : 0xFFDDDDDD, true));
                } else {
                    ctx.drawTextWithShadow(textRenderer, dimText, ex, ey - 10, isSelected ? accent : 0xFFDDDDDD);
                }
            }

            // Отрисовка 4 угловых кубиков масштабирования (Blender style)
            if (isSelected) {
                drawResizeHandles(ctx, el, ex, ey, ew, eh, mouseX, mouseY, accent);
            }

            // Отрисовка манипулятора внутреннего элемента: круг HP в TargetHUD
            if (isActive && el instanceof TargetHud th) {
                drawTargetHpRing(ctx, th, mouseX, mouseY, accent);
            }
        }

        // Отрисовка рамки множественного выделения (Windows Marquee Selection)
        if (isSelectingBox) {
            int sx = Math.round(Math.min(boxStartX, boxCurX));
            int sy = Math.round(Math.min(boxStartY, boxCurY));
            int sw = Math.round(Math.abs(boxCurX - boxStartX));
            int sh = Math.round(Math.abs(boxCurY - boxStartY));

            if (sw > 1 && sh > 1) {
                ctx.fill(sx, sy, sx + sw, sy + sh, (accent & 0x00FFFFFF) | 0x25000000);
                ctx.drawBorder(sx, sy, sw, sh, (accent & 0x00FFFFFF) | 0xCC000000);
            }
        }

        // Верхний минималистичный тулбар (Floating Pill Toolbar)
        renderToolbar(ctx, w, mouseX, mouseY, accent);

        // Нижняя строка подсказок на русском
        renderBottomHint(ctx, w, h);

        // Всплывающее сообщение (Toast)
        if (!toastMessage.isEmpty() && System.currentTimeMillis() - toastTime < 3000L) {
            if (Fonts.isLoaded() && Fonts.MEDIUM != null) {
                float toastFontSz = 8f;
                float textW = Fonts.MEDIUM.getWidth(toastMessage, toastFontSz);
                float textH = Fonts.MEDIUM.getHeight(toastFontSz);
                int toastW = Math.round(textW + 24f);
                int toastX = w / 2 - toastW / 2;
                int toastY = 44;
                int toastH = 20;
                ctx.fill(toastX, toastY, toastX + toastW, toastY + toastH, 0xE0181822);
                ctx.drawBorder(toastX, toastY, toastW, toastH, accent);
                float textX = w / 2f - textW / 2f;
                float textY = toastY + (toastH - textH) / 2f;
                Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(toastFontSz), toastMessage, textX, textY, Color.WHITE);
            } else {
                int toastW = textRenderer.getWidth(toastMessage) + 24;
                int toastX = w / 2 - toastW / 2;
                int toastY = 44;
                ctx.fill(toastX, toastY, toastX + toastW, toastY + 18, 0xE0181822);
                ctx.drawBorder(toastX, toastY, toastW, 18, accent);
                ctx.drawCenteredTextWithShadow(textRenderer, toastMessage, w / 2, toastY + 5, 0xFFFFFFFF);
            }
        }
        ThemeManager.getInstance().renderThemePulse(ctx);
    }

    /**
     * Отрисовка 4 кубиков по углам для изменения размера (Blender Style)
     */
    private void drawResizeHandles(DrawContext ctx, HudElement el, int x, int y, int w, int h, int mouseX, int mouseY, int accent) {
        int cubeSize = 6;
        int half = cubeSize / 2;

        int[][] corners = {
                {x - 2, y - 2},                     // 0: Top-Left
                {x + w + 2, y - 2},                 // 1: Top-Right
                {x + w + 2, y + h + 2},             // 2: Bottom-Right
                {x - 2, y + h + 2}                  // 3: Bottom-Left
        };

        for (int i = 0; i < 4; i++) {
            int cx = corners[i][0];
            int cy = corners[i][1];

            boolean hovered = Math.abs(mouseX - cx) <= half + 2 && Math.abs(mouseY - cy) <= half + 2;
            boolean active = (resizingElement == el && resizingCorner == i);

            int fillColor = (hovered || active) ? accent : 0xFFFFFFFF;
            int borderColor = 0xFF121218;

            ctx.fill(cx - half, cy - half, cx + half, cy + half, fillColor);
            ctx.drawBorder(cx - half, cy - half, cubeSize, cubeSize, borderColor);
        }

        // Отображение текущих размеров в пикселях и масштаба
        if (el == activeElement || resizingElement == el) {
            String sizeBadge = "Ш: " + w + "px  В: " + h + "px  (" + String.format(Locale.ROOT, "%.2f", el.getScale()) + "x)";
            if (Fonts.isLoaded() && Fonts.MEDIUM != null) {
                float badgeFontSz = 7f;
                float badgeTextW = Fonts.MEDIUM.getWidth(sizeBadge, badgeFontSz);
                float badgeTextH = Fonts.MEDIUM.getHeight(badgeFontSz);
                int badgeW = Math.round(badgeTextW + 12f);
                int badgeH = 14;
                int badgeX = x + w / 2 - badgeW / 2;
                int badgeY = y + h + 8;
                ctx.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xD0101018);
                ctx.drawBorder(badgeX, badgeY, badgeW, badgeH, 0x40FFFFFF);
                float textX = x + w / 2f - badgeTextW / 2f;
                float textY = badgeY + (badgeH - badgeTextH) / 2f;
                Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(badgeFontSz), sizeBadge, textX, textY, new Color(0xFFEEEEEE, true));
            } else {
                int badgeW = textRenderer.getWidth(sizeBadge) + 10;
                int badgeX = x + w / 2 - badgeW / 2;
                int badgeY = y + h + 8;
                ctx.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 13, 0xD0101018);
                ctx.drawBorder(badgeX, badgeY, badgeW, 13, 0x40FFFFFF);
                ctx.drawCenteredTextWithShadow(textRenderer, sizeBadge, x + w / 2, badgeY + 3, 0xFFEEEEEE);
            }
        }
    }

    /**
     * Манипулятор внутреннего элемента: круг HP в TargetHUD
     */
    private void drawTargetHpRing(DrawContext ctx, TargetHud th, int mouseX, int mouseY, int accent) {
        float circleSize = th.getHpCircleSize();
        float circleRelX = th.getHpCircleRelX();
        float circleRelY = th.getHpCircleRelY();

        int cx = Math.round(th.getX() + circleRelX + circleSize / 2f);
        int cy = Math.round(th.getY() + circleRelY + circleSize / 2f);
        int r = Math.round(circleSize / 2f);

        // Рисуем круговую направляющую рамку
        ctx.drawBorder(cx - r - 2, cy - r - 2, (r + 2) * 2, (r + 2) * 2, (accent & 0x00FFFFFF) | 0x90000000);

        // Маркер изменения размера круга на правой стороне окружности
        int handleX = cx + r + 2;
        int handleY = cy;
        int handleRadius = 3;

        boolean hovered = Math.hypot(mouseX - handleX, mouseY - handleY) <= 5.0;
        int color = (hovered || isResizingTargetHp) ? accent : 0xFF00E5FF;

        ctx.fill(handleX - handleRadius, handleY - handleRadius, handleX + handleRadius, handleY + handleRadius, color);
        ctx.drawBorder(handleX - handleRadius, handleY - handleRadius, handleRadius * 2, handleRadius * 2, 0xFF101018);

        if (hovered || isResizingTargetHp) {
            String hpTip = "Круг HP: " + (int) circleSize + "px";
            if (Fonts.isLoaded() && Fonts.MEDIUM != null) {
                float hpFontSz = 7.5f;
                float hpTextH = Fonts.MEDIUM.getHeight(hpFontSz);
                Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(hpFontSz), hpTip, handleX + 6, handleY - hpTextH / 2f, Color.WHITE);
            } else {
                ctx.drawTextWithShadow(textRenderer, hpTip, handleX + 6, handleY - 4, 0xFFFFFFFF);
            }
        }
    }

    /**
     * Верхняя минималистичная панель управления (Toolbar Pill)
     */
    private void renderToolbar(DrawContext ctx, int screenWidth, int mouseX, int mouseY, int accent) {
        int barW = 440;
        int barH = 24;
        int barX = screenWidth / 2 - barW / 2;
        int barY = 10;

        // Фон капсулы
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xD012121A);
        ctx.drawBorder(barX, barY, barW, barH, 0x30FFFFFF);

        boolean customFonts = Fonts.isLoaded() && Fonts.BOLD != null && Fonts.MEDIUM != null;

        // Логотип и заголовок
        if (customFonts) {
            float logoSz = 8.5f;
            float logoY = barY + (barH - Fonts.BOLD.getHeight(logoSz)) / 2f;
            Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(logoSz), "DARK VISUALS", barX + 12, logoY, new Color(accent, true));
        } else {
            ctx.drawTextWithShadow(textRenderer, "DARK VISUALS", barX + 12, barY + 7, accent);
        }

        // Разделитель
        ctx.fill(barX + 90, barY + 5, barX + 91, barY + barH - 5, 0x30FFFFFF);

        float btnFontSz = 7.5f;
        float btnTextY = customFonts ? barY + (barH - Fonts.MEDIUM.getHeight(btnFontSz)) / 2f : barY + 7;

        // Кнопка: Сетка
        int btnGridX = barX + 100;
        boolean hoverGrid = mouseX >= btnGridX && mouseX <= btnGridX + 68 && mouseY >= barY + 3 && mouseY <= barY + barH - 3;
        String gridText = "Сетка: " + (gridEnabled ? "ВКЛ" : "ВЫКЛ");
        if (hoverGrid) ctx.fill(btnGridX - 3, barY + 3, btnGridX + 68, barY + barH - 3, 0x25FFFFFF);
        int gridCol = gridEnabled ? 0xFF00FF7F : 0xFFAAAAAA;
        if (customFonts) {
            Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(btnFontSz), gridText, btnGridX, btnTextY, new Color(gridCol, true));
        } else {
            ctx.drawTextWithShadow(textRenderer, gridText, btnGridX, barY + 7, gridCol);
        }

        // Кнопка: Магнит (Привязка)
        int btnSnapX = btnGridX + 75;
        boolean hoverSnap = mouseX >= btnSnapX && mouseX <= btnSnapX + 75 && mouseY >= barY + 3 && mouseY <= barY + barH - 3;
        String snapText = "Магнит: " + (snapEnabled ? "ВКЛ" : "ВЫКЛ");
        if (hoverSnap) ctx.fill(btnSnapX - 3, barY + 3, btnSnapX + 75, barY + barH - 3, 0x25FFFFFF);
        int snapCol = snapEnabled ? 0xFF00E5FF : 0xFFAAAAAA;
        if (customFonts) {
            Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(btnFontSz), snapText, btnSnapX, btnTextY, new Color(snapCol, true));
        } else {
            ctx.drawTextWithShadow(textRenderer, snapText, btnSnapX, barY + 7, snapCol);
        }

        // Кнопка: Удалить (Скрыть)
        int btnDelX = btnSnapX + 82;
        boolean hasSelection = !selectedElements.isEmpty();
        boolean hoverDel = mouseX >= btnDelX && mouseX <= btnDelX + 56 && mouseY >= barY + 3 && mouseY <= barY + barH - 3;
        if (hoverDel && hasSelection) ctx.fill(btnDelX - 3, barY + 3, btnDelX + 56, barY + barH - 3, 0x40FF4444);
        int delCol = hasSelection ? (hoverDel ? 0xFFFF4444 : 0xFFFFFFFF) : 0x55AAAAAA;
        if (customFonts) {
            Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(btnFontSz), "Удалить", btnDelX, btnTextY, new Color(delCol, true));
        } else {
            ctx.drawTextWithShadow(textRenderer, "Удалить", btnDelX, barY + 7, delCol);
        }

        // Кнопка: Сброс
        int btnResetX = btnDelX + 62;
        boolean hoverReset = mouseX >= btnResetX && mouseX <= btnResetX + 44 && mouseY >= barY + 3 && mouseY <= barY + barH - 3;
        if (hoverReset) ctx.fill(btnResetX - 3, barY + 3, btnResetX + 44, barY + barH - 3, 0x25FFFFFF);
        int resetCol = hoverReset ? accent : 0xFFCCCCCC;
        if (customFonts) {
            Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(btnFontSz), "Сброс", btnResetX, btnTextY, new Color(resetCol, true));
        } else {
            ctx.drawTextWithShadow(textRenderer, "Сброс", btnResetX, barY + 7, resetCol);
        }

        // Кнопка: Закрыть (✕ / X)
        int btnCloseX = barX + barW - 20;
        boolean hoverClose = mouseX >= btnCloseX - 4 && mouseX <= btnCloseX + 12 && mouseY >= barY + 3 && mouseY <= barY + barH - 3;
        if (hoverClose) ctx.fill(btnCloseX - 4, barY + 3, btnCloseX + 12, barY + barH - 3, 0x40FF4444);
        int closeCol = hoverClose ? 0xFFFF4444 : 0xFF888888;
        if (customFonts) {
            float closeSz = 8.5f;
            float closeY = barY + (barH - Fonts.BOLD.getHeight(closeSz)) / 2f;
            Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(closeSz), "X", btnCloseX, closeY, new Color(closeCol, true));
        } else {
            ctx.drawTextWithShadow(textRenderer, "✕", btnCloseX, barY + 7, closeCol);
        }
    }

    /**
     * Нижняя подсказка по управлению на русском
     */
    private void renderBottomHint(DrawContext ctx, int screenWidth, int screenHeight) {
        String hint = "ЛКМ - Выбор / Рамка  |  Перетаскивание - Двигать  |  Кубики по углам - Размер  |  Del - Скрыть  |  ESC - Выход";
        int hintH = 16;
        int hintY = screenHeight - 22;

        if (Fonts.isLoaded() && Fonts.REGULAR != null) {
            float hintFontSz = 7.5f;
            float hintTextW = Fonts.REGULAR.getWidth(hint, hintFontSz);
            float hintTextH = Fonts.REGULAR.getHeight(hintFontSz);
            int hintW = Math.round(hintTextW + 20f);
            int hintX = screenWidth / 2 - hintW / 2;

            ctx.fill(hintX, hintY, hintX + hintW, hintY + hintH, 0x90101018);
            ctx.drawBorder(hintX, hintY, hintW, hintH, 0x25FFFFFF);

            float textX = screenWidth / 2f - hintTextW / 2f;
            float textY = hintY + (hintH - hintTextH) / 2f;
            Render2D.drawFont(ctx.getMatrices(), Fonts.REGULAR.getFont(hintFontSz), hint, textX, textY, new Color(0xFFAAAAAA, true));
        } else {
            String legacyHint = "ЛКМ — Выбор / Рамка  |  Перетаскивание — Двигать  |  Кубики по углам — Размер  |  Del — Скрыть  |  ESC — Выход";
            int hintW = textRenderer.getWidth(legacyHint) + 20;
            int hintX = screenWidth / 2 - hintW / 2;

            ctx.fill(hintX, hintY, hintX + hintW, hintY + 16, 0x90101018);
            ctx.drawBorder(hintX, hintY, hintW, 16, 0x25FFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer, legacyHint, screenWidth / 2, hintY + 4, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return true;

        int w = width;
        int barW = 440;
        int barX = w / 2 - barW / 2;
        int barY = 10;
        int barH = 24;

        // Клик по верхнему тулбару
        if (mouseY >= barY && mouseY <= barY + barH) {
            int btnGridX = barX + 100;
            if (mouseX >= btnGridX && mouseX <= btnGridX + 68) {
                gridEnabled = !gridEnabled;
                showToast(gridEnabled ? "Сетка включена" : "Сетка выключена");
                return true;
            }

            int btnSnapX = btnGridX + 75;
            if (mouseX >= btnSnapX && mouseX <= btnSnapX + 75) {
                snapEnabled = !snapEnabled;
                showToast(snapEnabled ? "Магнитная привязка включена" : "Магнитная привязка выключена");
                return true;
            }

            int btnDelX = btnSnapX + 82;
            if (mouseX >= btnDelX && mouseX <= btnDelX + 56) {
                if (!selectedElements.isEmpty()) deleteSelectedElements();
                return true;
            }

            int btnResetX = btnDelX + 62;
            if (mouseX >= btnResetX && mouseX <= btnResetX + 44) {
                resetLayout();
                return true;
            }

            int btnCloseX = barX + barW - 20;
            if (mouseX >= btnCloseX - 4 && mouseX <= btnCloseX + 12) {
                close();
                return true;
            }
        }

        // Проверка клика по маркеру круга HP в TargetHUD
        if (activeElement instanceof TargetHud th) {
            float circleSize = th.getHpCircleSize();
            float circleRelX = th.getHpCircleRelX();
            float circleRelY = th.getHpCircleRelY();
            int cx = Math.round(th.getX() + circleRelX + circleSize / 2f);
            int cy = Math.round(th.getY() + circleRelY + circleSize / 2f);
            int r = Math.round(circleSize / 2f);
            int handleX = cx + r + 2;
            int handleY = cy;

            if (Math.hypot(mouseX - handleX, mouseY - handleY) <= 6.0) {
                isResizingTargetHp = true;
                return true;
            }
        }

        // Проверка клика по 4 угловым кубикам (Blender-style Handles)
        for (HudElement el : selectedElements) {
            int ex = Math.round(el.getX());
            int ey = Math.round(el.getY());
            int ew = Math.round(el.getWidth());
            int eh = Math.round(el.getHeight());

            int[][] corners = {
                    {ex - 2, ey - 2},
                    {ex + ew + 2, ey - 2},
                    {ex + ew + 2, ey + eh + 2},
                    {ex - 2, ey + eh + 2}
            };

            for (int i = 0; i < 4; i++) {
                if (Math.abs(mouseX - corners[i][0]) <= 5 && Math.abs(mouseY - corners[i][1]) <= 5) {
                    resizingElement = el;
                    resizingCorner = i;
                    initialScale = el.getScale();
                    initialBaseW = el.getBaseWidth();
                    initialBaseH = el.getBaseHeight();
                    dragStartMouseX = (float) mouseX;
                    dragStartMouseY = (float) mouseY;
                    return true;
                }
            }
        }

        // Проверка клика по элементам HUD
        HudElement clickedElement = null;
        List<HudElement> elements = darkvisuals.getInstance().getHudManager().getHudElements();
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement el = elements.get(i);
            if (!isElementVisible(el)) continue;
            if (MathUtils.isHovered(el.getX() - 2, el.getY() - 2, el.getWidth() + 4, el.getHeight() + 4, (int) mouseX, (int) mouseY)) {
                clickedElement = el;
                break;
            }
        }

        boolean shiftDown = hasShiftDown();

        if (clickedElement != null) {
            if (shiftDown) {
                if (selectedElements.contains(clickedElement)) {
                    selectedElements.remove(clickedElement);
                    if (activeElement == clickedElement) {
                        activeElement = selectedElements.isEmpty() ? null : selectedElements.iterator().next();
                    }
                } else {
                    selectedElements.add(clickedElement);
                    activeElement = clickedElement;
                }
            } else {
                if (!selectedElements.contains(clickedElement)) {
                    selectedElements.clear();
                    selectedElements.add(clickedElement);
                }
                activeElement = clickedElement;
            }

            // Начинаем перетаскивание выбранных элементов
            isDraggingElements = true;
            dragStartMouseX = (float) mouseX;
            dragStartMouseY = (float) mouseY;
            initialElementX.clear();
            initialElementY.clear();
            for (HudElement sel : selectedElements) {
                initialElementX.put(sel, sel.getX());
                initialElementY.put(sel, sel.getY());
            }
            return true;
        }

        // Клик в пустое место: сброс выделения и начало прямоугольной рамки выделения
        if (!shiftDown) {
            selectedElements.clear();
            activeElement = null;
        }
        isSelectingBox = true;
        boxStartX = (float) mouseX;
        boxStartY = (float) mouseY;
        boxCurX = (float) mouseX;
        boxCurY = (float) mouseY;

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0) return true;

        // Изменение внутреннего размера круга HP в TargetHUD
        if (isResizingTargetHp && activeElement instanceof TargetHud th) {
            float circleSize = th.getHpCircleSize();
            float circleRelX = th.getHpCircleRelX();
            float circleRelY = th.getHpCircleRelY();
            int cx = Math.round(th.getX() + circleRelX + circleSize / 2f);
            int cy = Math.round(th.getY() + circleRelY + circleSize / 2f);

            double dist = Math.hypot(mouseX - cx, mouseY - cy);
            th.setHpCircleSize((float) (dist * 2.0));
            return true;
        }

        // Изменение размера через угловые кубики (Blender style)
        if (resizingElement != null) {
            float diffX = (float) mouseX - dragStartMouseX;
            float factor = (resizingCorner == 0 || resizingCorner == 3) ? -diffX : diffX;
            float newW = Math.max(20f, initialBaseW * initialScale + factor);
            float newScale = newW / Math.max(10f, initialBaseW);
            resizingElement.setScale(newScale);
            return true;
        }

        // Перетаскивание выбранных элементов (Drag)
        if (isDraggingElements) {
            float totalDeltaX = (float) mouseX - dragStartMouseX;
            float totalDeltaY = (float) mouseY - dragStartMouseY;

            float sw = width;
            float sh = height;

            for (HudElement el : selectedElements) {
                Float initX = initialElementX.get(el);
                Float initY = initialElementY.get(el);
                if (initX == null || initY == null) continue;

                float newX = initX + totalDeltaX;
                float newY = initY + totalDeltaY;

                // Магнитная привязка к центру экрана и краям (если включена)
                if (snapEnabled && el == activeElement) {
                    float threshold = 6f;
                    float centerX = sw / 2f - el.getWidth() / 2f;
                    if (Math.abs(newX - centerX) <= threshold) newX = centerX;
                    if (Math.abs(newX - 4f) <= threshold) newX = 4f;
                    if (Math.abs(newX - (sw - 4f - el.getWidth())) <= threshold) newX = sw - 4f - el.getWidth();

                    float centerY = sh / 2f - el.getHeight() / 2f;
                    if (Math.abs(newY - centerY) <= threshold) newY = centerY;
                    if (Math.abs(newY - 4f) <= threshold) newY = 4f;
                    if (Math.abs(newY - (sh - 4f - el.getHeight())) <= threshold) newY = sh - 4f - el.getHeight();
                }

                newX = Math.max(0, Math.min(newX, sw - el.getWidth()));
                newY = Math.max(0, Math.min(newY, sh - el.getHeight()));

                el.getPosition().getValue().setX(newX / sw);
                el.getPosition().getValue().setY(newY / sh);
            }
            return true;
        }

        // Обновление рамки выделения (Windows Marquee)
        if (isSelectingBox) {
            boxCurX = (float) mouseX;
            boxCurY = (float) mouseY;

            float minX = Math.min(boxStartX, boxCurX);
            float maxX = Math.max(boxStartX, boxCurX);
            float minY = Math.min(boxStartY, boxCurY);
            float maxY = Math.max(boxStartY, boxCurY);

            for (HudElement el : darkvisuals.getInstance().getHudManager().getHudElements()) {
                if (!isElementVisible(el)) continue;
                float ex = el.getX();
                float ey = el.getY();
                float ew = el.getWidth();
                float eh = el.getHeight();

                boolean intersects = ex < maxX && ex + ew > minX && ey < maxY && ey + eh > minY;
                if (intersects) {
                    selectedElements.add(el);
                    if (activeElement == null) activeElement = el;
                }
            }
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isSelectingBox = false;
            isDraggingElements = false;
            resizingElement = null;
            resizingCorner = -1;
            isResizingTargetHp = false;
            save();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_R) {
            resetLayout();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_G) {
            gridEnabled = !gridEnabled;
            showToast(gridEnabled ? "Сетка включена" : "Сетка выключена");
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_M || keyCode == GLFW.GLFW_KEY_S) {
            snapEnabled = !snapEnabled;
            showToast(snapEnabled ? "Магнитная привязка включена" : "Магнитная привязка выключена");
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            deleteSelectedElements();
            return true;
        }

        // Попиксельное перемещение стрелочками (Nudge)
        float step = hasShiftDown() ? 10f : 1f;
        float dx = 0, dy = 0;
        if (keyCode == GLFW.GLFW_KEY_LEFT) dx = -step;
        else if (keyCode == GLFW.GLFW_KEY_RIGHT) dx = step;
        else if (keyCode == GLFW.GLFW_KEY_UP) dy = -step;
        else if (keyCode == GLFW.GLFW_KEY_DOWN) dy = step;

        if (dx != 0 || dy != 0) {
            float sw = width, sh = height;
            for (HudElement el : selectedElements) {
                float nx = Math.max(0, Math.min(el.getX() + dx, sw - el.getWidth()));
                float ny = Math.max(0, Math.min(el.getY() + dy, sh - el.getHeight()));
                el.getPosition().getValue().setX(nx / sw);
                el.getPosition().getValue().setY(ny / sh);
            }
            save();
            return true;
        }

        return true;
    }

    private void deleteSelectedElements() {
        if (selectedElements.isEmpty()) return;
        int count = selectedElements.size();
        HUD hudModule = darkvisuals.getInstance().getModuleManager().getModule(HUD.class);

        for (HudElement el : selectedElements) {
            el.setToggled(false);
            if (hudModule != null && hudModule.hasElementSetting(el.getName())) {
                hudModule.setElementVisible(el.getName(), false);
            }
            var setting = darkvisuals.getInstance().getHudManager().getElements().getName(el.getName());
            if (setting instanceof BooleanSetting bs) {
                bs.setValue(false);
            }
        }

        selectedElements.clear();
        activeElement = null;
        showToast("Скрыто элементов: " + count + " (включить можно в настройках HUD)");
        save();
    }

    private void resetLayout() {
        float y = 0.02f;
        for (HudElement el : darkvisuals.getInstance().getHudManager().getHudElements()) {
            el.getPosition().getValue().setX(0.01f);
            el.getPosition().getValue().setY(y);
            el.setScale(1.0f);
            y += Math.max(0.04f, (el.getHeight() + 4f) / height);
            if (y > 0.9f) y = 0.02f;
        }
        showToast("Расположение элементов сброшено");
        save();
    }

    private void showToast(String msg) {
        this.toastMessage = msg;
        this.toastTime = System.currentTimeMillis();
    }

    private boolean isElementVisible(HudElement el) {
        HUD hudModule = darkvisuals.getInstance().getModuleManager().getModule(HUD.class);
        if (hudModule != null && hudModule.hasElementSetting(el.getName())) {
            return hudModule.isElementVisible(el.getName());
        }
        var setting = darkvisuals.getInstance().getHudManager().getElements().getName(el.getName());
        if (setting instanceof BooleanSetting bs) {
            return bs.getValue();
        }
        return el.isToggled();
    }

    private String getLocalizedName(String name) {
        return switch (name) {
            case "Watermark" -> "Водяной знак";
            case "TargetHud" -> "Цель (TargetHUD)";
            case "Potions" -> "Зелья";
            case "ArmorHUD" -> "Броня";
            case "Hotbar" -> "Хотбар";
            case "BetterNearHUD" -> "Игроки рядом";
            case "KeyboardHUD" -> "Клавиатура (WASD)";
            case "KeybindsHUD" -> "Бинды";
            case "Inventory" -> "Инвентарь";
            case "MusicHUD" -> "Музыка";
            default -> name;
        };
    }

    @Override
    public void removed() {
        isOpen = false;
        save();
        super.removed();
    }

    private void save() {
        try {
            var asm = darkvisuals.getInstance().getAutoSaveManager();
            if (asm != null) asm.scheduleAutoSave();
        } catch (Throwable ignored) {}
    }
}
