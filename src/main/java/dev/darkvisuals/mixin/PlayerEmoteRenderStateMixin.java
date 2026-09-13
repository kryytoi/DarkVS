package dev.darkvisuals.mixin;

import dev.darkvisuals.emotes.EmoteRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(PlayerEntityRenderState.class)
public class PlayerEmoteRenderStateMixin implements EmoteRenderState {

    @Unique
    private UUID darkvisuals$emoteOwner;

    @Override
    public UUID darkvisuals$getEmoteOwner() {
        return darkvisuals$emoteOwner;
    }

    @Override
    public void darkvisuals$setEmoteOwner(UUID uuid) {
        this.darkvisuals$emoteOwner = uuid;
    }
}