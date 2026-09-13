package dev.darkvisuals.emotes;

import net.minecraft.client.render.entity.model.PlayerEntityModel;

public record Emote(String id, String translationKey, long durationMs, EmotePose pose) {

    @FunctionalInterface
    public interface EmotePose {
 
        void apply(PlayerEntityModel model, float blend, float seconds);
    }
}