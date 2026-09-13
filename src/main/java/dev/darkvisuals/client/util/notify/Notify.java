package dev.darkvisuals.client.util.notify;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.ui.hud.HudStyle;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.animations.Animation;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.math.TimerUtils;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.client.util.renderer.Render2D;
import lombok.Getter;

import java.awt.*;

@Getter
public class Notify implements Wrapper {

    private final NotifyIcon icon;
    private final String notify;
    private final long delay;
    private float y;
    private final Animation animation = new Animation(300, 1f, true, Easing.BOTH_SINE);
    private final TimerUtils timer = new TimerUtils();

    public Notify(NotifyIcon icon, String notify, long delay) {
        this.icon = icon;
        this.notify = notify;
        this.delay = delay;
        y = mc.getWindow().getScaledHeight() / 2f + 10;
        timer.reset();
    }

    public void render(EventRender2D e, float picunY) {
        y = animate(y, picunY);
        if (timer.passed(delay)) animation.update(false);
        float animA = (float) animation.getValue();
        if (animA <= 0.01f) return;

        if (HudStyle.isMinimalistic()) {
            renderMinimalistic(e, animA);
        } else {
            renderDefault(e, animA);
        }
    }

    private void renderMinimalistic(EventRender2D e, float animA) {
        String text = "/ " + notify;
        float fontSize = 7.5f;
        float textW = Fonts.MEDIUM.getWidth(text, fontSize);
        float switchW = 16f;
        float switchH = 9f;
        float pad = 6f;
        float gap = 6f;
        float h = 18f;
        float w = pad + switchW + gap + textW + pad;
        float x = mc.getWindow().getScaledWidth() / 2f - w / 2f;

        Render2D.drawHudBackground(e.getContext().getMatrices(), x, y - 2.5f, w, h, h / 2f, animA);

        Color purple = new Color(0xA0, 0x00, 0xFF, (int) (255 * animA));
        float sx = x + pad;
        float sy = y - 2.5f + (h - switchH) / 2f;
        Render2D.drawRoundedRect(e.getContext().getMatrices(), sx, sy, switchW, switchH, switchH / 2f, purple);
        float knob = 7f;
        float knobX = sx + switchW - knob - 1f;
        float knobY = sy + (switchH - knob) / 2f;
        Render2D.drawRoundedRect(e.getContext().getMatrices(), knobX, knobY, knob, knob, knob / 2f,
                new Color(255, 255, 255, (int) (255 * animA)));

        float textY = y - 2.5f + (h - Fonts.MEDIUM.getHeight(fontSize)) / 2f;
        Render2D.drawHudText(e.getContext().getMatrices(), Fonts.MEDIUM.getFont(fontSize), text,
                sx + switchW + gap, textY, new Color(255, 255, 255, (int) (255 * animA)));
    }

    private void renderDefault(EventRender2D e, float animA) {
        float width = Fonts.MEDIUM.getWidth(notify, 9f);
        float width2 = Fonts.ICONS.getWidth(icon.icon(), 8f);
        float width3 = width + width2 + 7f;
        float x = mc.getWindow().getScaledWidth() / 2f - (width3 / 2f);
        int bgA = (int) (175 * animA);
        int blurA = (int) (40 * animA);

        Render2D.drawShaderBlurRect(
                e.getContext().getMatrices(),
                x - 3.5f,
                y - 3.5f,
                width3 + 7f,
                17f,
                2f,
                8f,
                new Color(255, 255, 255, blurA)
        );
        Render2D.drawRoundedRect(
                e.getContext().getMatrices(),
                x - 2.5f,
                y - 2.5f,
                width3 + 5f,
                15f,
                1.5f,
                new Color(0, 0, 0, bgA)
        );
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.MEDIUM.getFont(9f), notify, x + width2 + 4f, y - 0.5f, new Color(255, 255, 255, (int) (255 * animation.getValue())));
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.ICONS.getFont(8f), icon.icon(), x + 1f, y + 1f, new Color(255, 255, 255, (int) (255 * animation.getValue())));
    }

    public float animate(float value, float target) {
        return value + (target - value) / 8f;
    }

    public boolean expired() {
        return timer.passed(delay) && animation.getValue() < 0.01f;
    }
}
