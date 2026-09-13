package dev.darkvisuals.client.events.impl;

import dev.darkvisuals.client.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class EventMouse extends Event {
    private int button, action;
}