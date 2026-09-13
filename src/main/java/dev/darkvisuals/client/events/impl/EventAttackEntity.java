package dev.darkvisuals.client.events.impl;

import dev.darkvisuals.client.events.Event;
import dev.darkvisuals.client.managers.HitDetectionManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

@AllArgsConstructor @Getter
public class EventAttackEntity extends Event {
    private final PlayerEntity player;
    private final Entity target;
    private boolean effectsAllowed = true;

    public EventAttackEntity(PlayerEntity player, Entity target) {
        this.player = player;
        this.target = target;
        this.effectsAllowed = true;
    }
    
     public boolean canProcess() {
        HitDetectionManager manager = HitDetectionManager.getInstance();
        return manager.canProcessHit(player, target);
    }

    public boolean isEffectsAllowed() {
        return effectsAllowed;
    }

    public void setEffectsAllowed(boolean allowed) {
        this.effectsAllowed = allowed;
    }
    
     public void registerHit() {
        HitDetectionManager.getInstance().registerHit(player, target);
    }
}