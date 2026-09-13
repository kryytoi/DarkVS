package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.utility.HitSound;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(PlayerEntity.class)
public abstract class HitSoundMixin {

 	@Unique
	private static final Map<String, SoundEvent> darkvisuals$soundCache = new ConcurrentHashMap<>();

	@Inject(method = "attack", at = @At("HEAD"))
	private void onAttack(Entity target, CallbackInfo ci) {
		MinecraftClient mc = MinecraftClient.getInstance();

		 
		if (mc.player == null || (Object) this != mc.player) return;

		HitSound module = darkvisuals.getInstance().getModuleManager().getModule(HitSound.class);
		if (module == null || !module.isToggled() || !(target instanceof LivingEntity)) return;

		float volume = module.getVolume().getValue();
		String soundId = module.getSelectedSound();

		SoundEvent sound = darkvisuals$soundCache.computeIfAbsent(
				soundId, id -> SoundEvent.of(Identifier.of(id)));

		try {
			 
			 
			 
			 
			mc.getSoundManager().play(
					PositionedSoundInstance.master(sound, 1.0f, volume));
		} catch (java.util.ConcurrentModificationException ignored) {
			 
		}
	}
}
