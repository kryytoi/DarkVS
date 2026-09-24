package dev.darkvisuals.client.ui.hud;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.darkvisuals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

public class HudEditorScreen extends ChatScreen {

    private static final int GRID = 16;
    private static boolean isOpen = false;

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

        for (int x = 0; x < w; x += GRID) ctx.fill(x, 0, x + 1, h, 0x12FFFFFF);
        for (int y = 0; y < h; y += GRID) ctx.fill(0, y, w, y + 1, 0x12FFFFFF);
        ctx.fill(w / 2, 0, w / 2 + 1, h, 0x33FFFFFF);
        ctx.fill(0, h / 2, w, h / 2 + 1, 0x33FFFFFF);

        for (HudElement el : darkvisuals.getInstance().getHudManager().getHudElements()) {
            if (el.getWidth() <= 0 || el.getHeight() <= 0) continue;
            int x = (int) el.getX() - 2, y = (int) el.getY() - 2;
            int ew = (int) el.getWidth() + 4, eh = (int) el.getHeight() + 4;
            boolean hover = mouseX >= x && mouseX <= x + ew && mouseY >= y && mouseY <= y + eh;

            ctx.drawBorder(x, y, ew, eh, hover || el.isDragging() ? accent : 0x40FFFFFF);
            if (hover || el.isDragging()) {
                String label = el.getName() + "  " + (int) el.getX() + ", " + (int) el.getY();
                ctx.drawTextWithShadow(textRenderer, label, x, y - 10, 0xFFFFFFFF);
            }
        }

        ctx.fill(w / 2 - 150, 4, w / 2 + 150, 30, 0x88000000);
        ctx.drawCenteredTextWithShadow(textRenderer, "Редактор HUD", w / 2, 8, accent);
        ctx.drawCenteredTextWithShadow(textRenderer,
                "ЛКМ тащить  |  ПКМ настройки  |  R сброс  |  ESC выход", w / 2, 19, 0xFFAAAAAA);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (keyCode == GLFW.GLFW_KEY_R) { resetLayout(); return true; }
        return true;
    }

    @Override public boolean charTyped(char chr, int modifiers) { return true; }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { return true; }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) { return true; }

    private void resetLayout() {
        float y = 0.02f;
        for (HudElement el : darkvisuals.getInstance().getHudManager().getHudElements()) {
            el.getPosition().getValue().setX(0.01f);
            el.getPosition().getValue().setY(y);
            y += Math.max(0.04f, (el.getHeight() + 4f) / height);
            if (y > 0.9f) y = 0.02f;
        }
        save();
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