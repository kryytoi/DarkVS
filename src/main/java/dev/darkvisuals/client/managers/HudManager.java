package dev.darkvisuals.client.managers;

import dev.darkvisuals.client.ui.hud.impl.*;
import dev.darkvisuals.client.util.math.MathUtils;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.client.events.impl.EventMouse;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.ui.hud.HudEditorScreen;
import dev.darkvisuals.client.ui.hud.windows.Window;
import dev.darkvisuals.client.util.render.Wrapper;
import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import lombok.Getter;
import lombok.Setter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static dev.darkvisuals.client.util.Wrapper.mc;

@Getter
public class HudManager implements Wrapper {

    @Setter private HudElement currentDragging;
    private final List<HudElement> hudElements = new ArrayList<>();
    protected final ListSetting elements = new ListSetting("setting.layout",
            new BooleanSetting("Watermark", true),
            new BooleanSetting("TargetHud", true),
            new BooleanSetting("Potions", true),
            new BooleanSetting("ArmorHUD", true),
            new BooleanSetting("Hotbar", true),
            new BooleanSetting("BetterNearHUD", true),
            new BooleanSetting("KeyboardHUD", true),
            new BooleanSetting("KeybindsHUD", true),
            new BooleanSetting("Inventory", true)
    );
    @Setter private Window window;

    public HudManager() {
        darkvisuals.getInstance().getEventHandler().subscribe(this);

        addElements(
                new Watermark(),
                new TargetHud(),
                new Potions(),
                new ArmorHUD(),
                new HotbarHUD(),
                new MusicHUD(),
                new BetterNearHUD(),
                new KeyboardHUD(),
                new KeybindsHUD(),
                new InventoryHUD()
        );

        for (HudElement element : hudElements) {
            try {
                for (Field field : element.getClass().getDeclaredFields()) {
                    if (!Setting.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Setting<?> setting = (Setting<?>) field.get(element);
                    if (setting != null && !element.getSettings().contains(setting)) element.getSettings().add(setting);
                }
            } catch (Exception ignored) {}

             
            try {
                darkvisuals.getInstance().getEventHandler().subscribe(element);
            } catch (Throwable ignored) {}
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (Module.fullNullCheck()) return;

        if (window != null) {
            if (!HudElement.isEditContext()) window.reset();

            if (window.closed()) {
                window = null;
                return;
            }

            window.render(e.getContext(), mouseX(), mouseY());
        }
    }

    @EventHandler
    public void onMouse(EventMouse e) {
        if (!HudElement.isEditContext() || Module.fullNullCheck()) return;

        if (e.getAction() == 1) {
            if (window != null) {
                if (MathUtils.isHovered(window.getX(), window.getY(), window.getWidth(), window.getFinalHeight(), mouseX(), mouseY())) {
                    window.mouseClicked(mouseX(), mouseY(), e.getButton());
                    return;
                } else window.reset();
            }

            if (e.getButton() == 1) {
                 
                for (HudElement element : hudElements) {
                    if (MathUtils.isHovered(element.getX(), element.getY(), element.getWidth(), element.getHeight(), mouseX(), mouseY())) {
                        return;
                    }
                }

                 
                for (HudElement element : hudElements) {
                    if (element.getWindow() == null) continue;
                    if (element.getSettings().size() == 1) return;
                    element.getWindow().reset();
                }

                window = new Window(mouseX() + 3, mouseY() + 3, 100, 12.5f, List.of(elements));
            }
        }
    }

    public int mouseX() {
        return (int) (mc.mouse.getX() / mc.getWindow().getScaleFactor());
    }

    public int mouseY() {
        return (int) (mc.mouse.getY() / mc.getWindow().getScaleFactor());
    }

    private void addElements(HudElement... element) {
        this.hudElements.addAll(List.of(element));
    }
}
