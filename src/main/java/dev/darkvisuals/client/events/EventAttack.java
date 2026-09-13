package dev.darkvisuals.client.events;


import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;

@AllArgsConstructor
@Getter
public class EventAttack extends Event {
    Entity entity;
}
