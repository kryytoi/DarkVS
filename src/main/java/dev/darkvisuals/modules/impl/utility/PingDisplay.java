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
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

/**
 * PingDisplay — задержка до сервера в стеклянной панели.
 */
public class PingDisplay extends Module {

    private final NumberSetting x = new NumberSetting("X", 20f, 0f, 3840f, 1f);
    private final NumberSetting y = new NumberSetting("Y", 40f, 0f, 2160f, 1f);
    private final NumberSetting width = new NumberSetting("Ширина", 92f, 50f, 300f, 1f);
    private final NumberSetting height = new NumberSetting("Высота", 22f, 12f, 60f, 1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 11f, 0f, 40f, 1f);
    private final ColorSetting tint = new ColorSetting("Оттенок", new Color(120, 200, 255, 55).getRGB());

    private int ping = -1;
    private int tickCounter = 0;

    public PingDisplay() {
        super("PingDisplay", Category.Utility, "Пинг в стеклянной панели");
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        if (++tickCounter < 20) return; // обновляем раз в секунду
        tickCounter = 0;

        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) {
            ping = -1;
            return;
        }

        PlayerListEntry entry = handler.getPlayerListEntry(mc.player.getUuid());
        ping = entry != null ? entry.getLatency() : -1;
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck() || !Fonts.isLoaded()) return;

        MatrixStack matrices = event.getContext().getMatrices();

        float w = width.getValue();
        float h = height.getValue();
        float px = x.getValue();
        float py = y.getValue();

        Render2D.drawGlass(matrices, px, py, w, h,
                1f, radius.getValue(), tint.getColor().getRGB(),
                8f, 3f, 3f, 2f);

        String value = ping < 0 ? "--" : ping + "ms";
        float fs = Math.min(9f, h * 0.5f);
        float valueW = Fonts.MEDIUM.getWidth(value, fs);
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), value,
                px + w - 8f - valueW, py + (h - Fonts.MEDIUM.getHeight(fs)) / 2f,
                pingColor());

        String label = "Ping";
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs * 0.85f), label,
                px + 8f, py + (h - Fonts.MEDIUM.getHeight(fs * 0.85f)) / 2f,
                new Color(200, 205, 210, 200));
    }

    private Color pingColor() {
        if (ping < 0) return new Color(160, 165, 170, 220);
        if (ping <= 80) return new Color(140, 230, 160, 245);
        if (ping <= 180) return new Color(255, 200, 110, 245);
        return new Color(255, 110, 110, 245);
    }
}
