package dev.darkvisuals.client.events.impl;

import dev.darkvisuals.client.events.Event;
import lombok.Getter;
import net.minecraft.util.math.Vec3d;

 @Getter
public class EventExplosion extends Event {
    private final Vec3d center;

    public EventExplosion(Vec3d center) {
        this.center = center;
    }
}
