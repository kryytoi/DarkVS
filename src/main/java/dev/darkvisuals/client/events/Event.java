package dev.darkvisuals.client.events;

import lombok.Getter;

@Getter
public class Event {
    private boolean cancelled;

    public void cancel() {
        cancelled = true;
    }

    public void resume() {
        cancelled = false;
    }
    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}