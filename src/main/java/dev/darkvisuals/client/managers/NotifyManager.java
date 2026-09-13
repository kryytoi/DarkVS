package dev.darkvisuals.client.managers;

import com.google.common.collect.Lists;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.notify.Notify;
import meteordevelopment.orbit.EventHandler;

import java.util.*;

public class NotifyManager implements Wrapper {

    public NotifyManager() {
        darkvisuals.getInstance().getEventHandler().subscribe(this);
    }

    private final List<Notify> notifies = new ArrayList<>();

    public void add(Notify notify) {
        notifies.add(notify);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (notifies.isEmpty()) return;
        float startY = mc.getWindow().getScaledHeight() / 2f + 26;
        if (notifies.size() > 10) notifies.removeFirst();
        notifies.removeIf(Notify::expired);

        for (Notify notify : Lists.newArrayList(notifies)) {
            startY = (startY - (dev.darkvisuals.client.ui.hud.HudStyle.isMinimalistic() ? 20f : 16f));
            notify.render(e, startY + (notifies.size() * (dev.darkvisuals.client.ui.hud.HudStyle.isMinimalistic() ? 20f : 16f)));
        }
    }
}