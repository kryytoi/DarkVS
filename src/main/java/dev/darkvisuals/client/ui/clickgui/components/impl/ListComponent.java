package dev.darkvisuals.client.ui.clickgui.components.impl;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.clickgui.components.Component;
import dev.darkvisuals.client.util.animations.Animation;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.math.MathUtils;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListComponent extends Component {

    private final ListSetting setting;
    private final Map<BooleanSetting, Animation> pickAnimations = new HashMap<>();

    @Getter private final Animation openAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private boolean open;

    public ListComponent(ListSetting setting) {
        super(setting.getName());
        this.setting = setting;
        for (BooleanSetting setting1 : setting.getValue()) pickAnimations.put(setting1, new Animation(300, 1f, false, Easing.BOTH_SINE));
        this.visible = setting::isVisible;
        this.addHeight = () -> openAnimation.getValue() > 0 ? (setting.getValue().size() * 14f) * (float) openAnimation.getValue() : 0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnimation.update(open);

        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));

        Render2D.drawFont(context.getMatrices(),
                Fonts.BOLD.getFont(7.5f),
                I18n.translate(setting.getName()),
                x + 5f,
                y + 3.5f,
                new Color(
                        ThemeManager.getInstance().getCurrentTheme().getTextColor().getRed(),
                        ThemeManager.getInstance().getCurrentTheme().getTextColor().getGreen(),
                        ThemeManager.getInstance().getCurrentTheme().getTextColor().getBlue(),
                        (int) (ThemeManager.getInstance().getCurrentTheme().getTextColor().getAlpha() * ga)
                )
        );

        Render2D.drawFont(context.getMatrices(),
                Fonts.BOLD.getFont(7.5f),
                "(" + setting.getToggled().size() + "/" + setting.getValue().size() + ")",
                x + width - Fonts.REGULAR.getWidth("(" + setting.getToggled().size() + "/" + setting.getValue().size() + ")", 7.5f) - 5f,
                y + 3.5f,
                new Color(
                        ThemeManager.getInstance().getCurrentTheme().getTextColor().getRed(),
                        ThemeManager.getInstance().getCurrentTheme().getTextColor().getGreen(),
                        ThemeManager.getInstance().getCurrentTheme().getTextColor().getBlue(),
                        (int) (ThemeManager.getInstance().getCurrentTheme().getTextColor().getAlpha() * ga)
                )
        );

        if (openAnimation.getValue() > 0) {
            float yOffset = height;
            float a = (float) Math.max(0f, Math.min(1f, openAnimation.getValue()));
            for (BooleanSetting setting : setting.getValue()) {
                Animation anim = pickAnimations.get(setting);
                anim.update(setting.getValue());
                float textSlide = (1f - a) * 8f;
                int itemAlpha = (int) (ThemeManager.getInstance().getCurrentTheme().getTextColor().getAlpha() * ga * a);
                Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(10f), "D", x + width - 14f, y + yOffset + 3.5f,
                        new Color(0, 0, 0, (int) (255 * anim.getValue() * ga * a)));
                Render2D.drawFont(context.getMatrices(),
                        Fonts.BOLD.getFont(7.5f),
                        I18n.translate(setting.getName()),
                        x + 6f + textSlide,
                        y + yOffset + 3.5f,
                        new Color(
                                ThemeManager.getInstance().getCurrentTheme().getTextColor().getRed(),
                                ThemeManager.getInstance().getCurrentTheme().getTextColor().getGreen(),
                                ThemeManager.getInstance().getCurrentTheme().getTextColor().getBlue(),
                                itemAlpha
                        )
                );
                yOffset += 14f;
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
         
        if (MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY)) {
            if (button == 1) {  
                open = !open;
                return;
            }
            if (button == 0) {  
                if (setting.isSingleSelect()) {
                     
                    List<BooleanSetting> opts = setting.getValue();
                    int current = -1;
                    for (int i = 0; i < opts.size(); i++) if (opts.get(i).getValue()) { current = i; break; }
                    int next = (current + 1) % Math.max(1, opts.size());
                    for (BooleanSetting bs : opts) bs.setValue(false);
                    opts.get(next).setValue(true);
                } else {
                    open = !open;  
                }
                return;
            }
        }
         
        if (openAnimation.getValue() > 0 && button == 0) {
            float yOffset = height;
            float visibleH = (float) (setting.getValue().size() * 14f * Math.max(0f, Math.min(1f, openAnimation.getValue())));
            for (BooleanSetting s : setting.getValue()) {
                if (yOffset >= height + visibleH) break;  
                if (MathUtils.isHovered(x, y + yOffset, width, 14f, (float) mouseX, (float) mouseY)) {
                    if (setting.isSingleSelect()) {
                        for (BooleanSetting all : setting.getValue()) all.setValue(false);
                        s.setValue(true);
                    } else {
                        s.setValue(!s.getValue());
                    }
                    break;
                }
                yOffset += 14f;
            }
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