package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventKey;
import dev.darkvisuals.client.managers.WaypointManager;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.impl.BindSetting;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

 
public class QuickMarkers extends Module {

    private final BindSetting quickBind = new BindSetting("Бинд быстрой метки", new Bind(GLFW.GLFW_KEY_B, false));
    private final BooleanSetting toggleNearby = new BooleanSetting("Повтор удаляет ближнюю", true);

    private boolean latch = false;
    private int counter = 0;

    public QuickMarkers() {
        super("QuickMarkers", Category.Markers, "Быстрая метка по бинду");
        getSettings().add(quickBind);
        getSettings().add(toggleNearby);
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (!isToggled() || fullNullCheck()) return;
        if (mc.currentScreen != null) return;  

        Bind bind = quickBind.getValue();
        if (bind == null || bind.isEmpty() || bind.isMouse()) return;
        if (bind.getKey() != e.getKey()) return;

        if (e.getAction() == GLFW.GLFW_PRESS) {
            if (!latch) {
                latch = true;
                placeQuickMarker();
            }
        } else if (e.getAction() == GLFW.GLFW_RELEASE) {
            latch = false;
        }
    }

    private void placeQuickMarker() {
        var pos = mc.player.getPos();

         
        if (toggleNearby.getValue()) {
            for (WaypointManager.Waypoint w : WaypointManager.list()) {
                if (w.name.startsWith("Quick #") && w.pos.distanceTo(pos) < 8.0) {
                    WaypointManager.remove(w.name);
                    return;
                }
            }
        }

        counter++;
        WaypointManager.add("Quick #" + counter, pos);
    }
}