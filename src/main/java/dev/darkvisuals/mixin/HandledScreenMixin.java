package dev.darkvisuals.mixin;

import dev.darkvisuals.client.events.impl.EventHandledScreen;
import dev.darkvisuals.darkvisuals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
	@Shadow protected int backgroundWidth;
	@Shadow protected int backgroundHeight;
	@Shadow protected int x;
	@Shadow protected int y;
	@Shadow @Nullable protected Slot focusedSlot;

	@Inject(method = "render", at = @At("RETURN"))
	private void darkvisuals$afterRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		darkvisuals.post(new EventHandledScreen(context, focusedSlot, backgroundWidth, backgroundHeight, x, y));
	}
}
