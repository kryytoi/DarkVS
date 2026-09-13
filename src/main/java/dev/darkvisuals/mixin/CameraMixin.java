package dev.darkvisuals.mixin;

import dev.darkvisuals.modules.impl.render.BetterMinecraft;
import dev.darkvisuals.darkvisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract float clipToSpace(float f);

	@Unique
	private Boolean darkvisuals$lastThirdPerson;

	@Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"))
	private float darkvisuals$smoothThirdPersonZoom(Camera instance, float desiredDistance) {
		BetterMinecraft module = darkvisuals.getInstance().getModuleManager().getModule(BetterMinecraft.class);
		if (module == null || !module.isToggled() || !module.smoothThirdPersonZoom.getValue()) {
			return this.clipToSpace(desiredDistance);
		}

		Perspective perspective = MinecraftClient.getInstance().options.getPerspective();
		boolean isThirdPerson = perspective != Perspective.FIRST_PERSON;

		if (darkvisuals$lastThirdPerson == null || darkvisuals$lastThirdPerson != isThirdPerson) {
			module.getThirdPersonAnimation().reset();
			darkvisuals$lastThirdPerson = isThirdPerson;
		}

		module.getThirdPersonAnimation().update(isThirdPerson);
		float factor = module.getThirdPersonAnimation().getValue();

		 
		float baseOffset = isThirdPerson ? 0.35f : 0.0f;
		float animatedDistance = Math.max(baseOffset, desiredDistance * factor);
		return this.clipToSpace(animatedDistance);
	}
} 