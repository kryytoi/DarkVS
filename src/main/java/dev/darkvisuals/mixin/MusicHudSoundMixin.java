package dev.darkvisuals.mixin;

import dev.darkvisuals.client.ui.hud.impl.MusicHUD;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 
@Mixin(SoundManager.class)
public abstract class MusicHudSoundMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
    private void darkvisuals$onPlay(SoundInstance sound, CallbackInfo ci) {
        MusicHUD.onSoundPlayed(sound);
    }

    @Inject(method = "stop(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
    private void darkvisuals$onStop(SoundInstance sound, CallbackInfo ci) {
        MusicHUD.onSoundStopped(sound);
    }

    @Inject(method = "stopAll", at = @At("HEAD"))
    private void darkvisuals$onStopAll(CallbackInfo ci) {
        MusicHUD.onAllSoundsStopped();
    }
}
