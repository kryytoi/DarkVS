package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Box;

import java.awt.Color;

public class CrystalColor extends Module {

    private final ColorSetting color = new ColorSetting("Цвет", new Color(255, 0, 0, 255).getRGB());

    private final BooleanSetting fillMode = new BooleanSetting("Заливка", true);
    private final NumberSetting fillOpacity = new NumberSetting("Прозрачность заливки", 0.35f, 0.0f, 1.0f, 0.05f);

    private final BooleanSetting outlineMode = new BooleanSetting("Обводка", true);
    private final NumberSetting outlineWidth = new NumberSetting("Толщина обводки", 2.0f, 1.0f, 5.0f, 0.5f);

    private final NumberSetting expand = new NumberSetting("Размер бокса", 0.1f, 0.0f, 0.5f, 0.05f);

    public CrystalColor() {
        super("CrystalColor", Category.Render, "Красит все кристаллы Эндера в свой цвет");
        getSettings().add(color);
        getSettings().add(fillMode);
        getSettings().add(fillOpacity);
        getSettings().add(outlineMode);
        getSettings().add(outlineWidth);
        getSettings().add(expand);

        fillOpacity.setVisible(fillMode::getValue);
        outlineWidth.setVisible(outlineMode::getValue);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        if (!fillMode.getValue() && !outlineMode.getValue()) return;

        Color base = color.getColor();

        Render3D.DEBUG_LINE_WIDTH = outlineWidth.getValue().floatValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!crystal.isAlive()) continue;

            Box box = crystal.getBoundingBox().expand(expand.getValue());

            if (fillMode.getValue()) {
                int alpha = (int) (255 * fillOpacity.getValue());
                Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
                Render3D.renderBox(e.getMatrices(), box, fill);
            }
            if (outlineMode.getValue()) {
                Color outline = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255);
                Render3D.renderBoxOutline(e.getMatrices(), box, outline);
            }
        }
    }
}