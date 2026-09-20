package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import java.awt.*;

/**
 * ConsumeTimer — прогресс использования предмета (еда, зелья) в стеклянной панели.
 */
public class ConsumeTimer extends Module {

    private final NumberSetting x = new NumberSetting("X", 20f, 0f, 3840f, 1f);
    private final NumberSetting y = new NumberSetting("Y", 160f, 0f, 2160f, 1f);
    private final NumberSetting width = new NumberSetting("Ширина", 160f, 80f, 400f, 1f);
    private final NumberSetting height = new NumberSetting("Высота", 22f, 12f, 60f, 1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 11f, 0f, 40f, 1f);
    private final ColorSetting tint = new ColorSetting("Оттенок", new Color(140, 230, 160, 65).getRGB());

    private int ticksLeft = 0;
    private int ticksTotal = 1;

    public ConsumeTimer() {
        super("ConsumeTimer", Category.Utility, "Прогресс использования предмета");
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        if (mc.player.isUsingItem()) {
            ItemStack stack = mc.player.getActiveItem();
            if (stack != null && !stack.isEmpty()) {
                ticksTotal = Math.max(1, stack.getMaxUseTime(mc.player));
                ticksLeft = mc.player.getItemUseTimeLeft();
                return;
            }
        }
        ticksLeft = 0;
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck() || !Fonts.isLoaded()) return;
        if (ticksLeft <= 0) return;

        MatrixStack matrices = event.getContext().getMatrices();

        float w = width.getValue();
        float h = height.getValue();
        float px = x.getValue();
        float py = y.getValue();

        Render2D.drawGlass(matrices, px, py, w, h,
                1f, radius.getValue(), tint.getColor().getRGB(),
                8f, 3f, 3f, 2f);

        ItemStack stack = mc.player != null ? mc.player.getActiveItem() : ItemStack.EMPTY;
        String name = stack != null && !stack.isEmpty() ? stack.getName().getString() : "Использование";
        float seconds = ticksLeft / 20f;

        float fs = Math.min(8f, h * 0.5f);
        float textY = py + (h - Fonts.MEDIUM.getHeight(fs)) / 2f - 2f;
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), name,
                px + 7f, textY, new Color(255, 255, 255, 235));

        float barY = py + h - 5f;
        float barW = w - 14f;
        float progress = 1f - ticksLeft / (float) ticksTotal;
        Render2D.drawRect(matrices, px + 7f, barY, barW, 2.5f, new Color(0, 0, 0, 110));
        Render2D.drawRect(matrices, px + 7f, barY, Math.max(0f, barW * progress), 2.5f,
                new Color(140, 230, 160, 235));

        String sec = String.format("%.1fs", seconds);
        float secW = Fonts.MEDIUM.getWidth(sec, fs);
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), sec,
                px + w - 7f - secW, textY, new Color(140, 230, 160, 235));
    }
}
