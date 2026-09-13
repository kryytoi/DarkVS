package dev.darkvisuals.client.ui.clickgui.components.impl;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.clickgui.components.Component;
import dev.darkvisuals.client.util.math.MathUtils;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class ButtonComponent extends Component {

    private final ButtonSetting setting;
    private boolean hovered;

    public ButtonComponent(ButtonSetting setting) {
        super(setting.getName());
        this.setting = setting;
        this.visible = setting::isVisible;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));
        hovered = MathUtils.isHovered(x, y, width, height, mouseX, mouseY);

        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        Color bg = hovered
                ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (200 * ga))
                : new Color(60, 60, 60, (int) (160 * ga));

        Render2D.drawRoundedRect(context.getMatrices(), x, y, width, height - 2f, 3f, bg);

        String text = setting.getName();
        float fontSize = 7.5f;
        float textW = Fonts.BOLD.getWidth(text, fontSize);
        float textH = Fonts.BOLD.getHeight(fontSize);
        Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(fontSize), text,
                x + (width - textW) / 2f, y + (height - 2f - textH) / 2f,
                new Color(255, 255, 255, (int) (255 * ga)));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY)) {
            setting.click();
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
    }

    @Override
    public void keyReleased(int keyCode, int scanCode, int modifiers) {
    }

    @Override
    public void charTyped(char chr, int modifiers) {
    }
}
