package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventKey;
import dev.darkvisuals.modules.impl.render.BetterMinecraft;
import dev.darkvisuals.modules.impl.render.ViewModel;
import dev.darkvisuals.client.ui.hud.impl.PerfHUD;
import dev.darkvisuals.client.ui.hud.HudElement;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {

	@Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
	public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
		ViewModel viewModel = darkvisuals.getInstance().getModuleManager().getModule(ViewModel.class);
		if (viewModel != null && viewModel.isConfiguring()) {
			 
			if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
				viewModel.closeConfig();
				ci.cancel();
				return;
			}
			 
			if (key == GLFW.GLFW_KEY_TAB && action == GLFW.GLFW_PRESS) {
				viewModel.toggleEditedHand();
				ci.cancel();
				return;
			}
			 
			if (viewModel.isBlockedMovementKey(key, scancode)) {
				ci.cancel();
				return;
			}
		}

		EventKey event = new EventKey(key, action, modifiers);
		darkvisuals.post(event);

		 
		if (key == GLFW.GLFW_KEY_Q && action == GLFW.GLFW_PRESS
				&& (modifiers & GLFW.GLFW_MOD_CONTROL) != 0
				&& (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
			try {
				var hudManager = darkvisuals.getInstance().getHudManager();
				if (hudManager != null) {
					PerfHUD perfHud = null;
					for (HudElement he : hudManager.getHudElements()) {
						if (he instanceof PerfHUD p) { perfHud = p; break; }
					}
					if (perfHud == null) {
						perfHud = new PerfHUD();
						 
						perfHud.setBounds(10, 10, 180, 120);
						hudManager.getHudElements().add(perfHud);
						darkvisuals.getInstance().getEventHandler().subscribe(perfHud);
					}
					perfHud.setToggled(!perfHud.isToggled());
				}
			} catch (Throwable ignored) {}
		}

		BetterMinecraft module = darkvisuals.getInstance().getModuleManager().getModule(BetterMinecraft.class);
		if (module == null || !module.isToggled()) return;

		 
		if (key == GLFW.GLFW_KEY_F5 && action == GLFW.GLFW_PRESS && module.smoothThirdPersonZoom.getValue()) {
			module.getThirdPersonAnimation().reset();
		}

		 
		if (key == GLFW.GLFW_KEY_TAB) {
			if (action == GLFW.GLFW_PRESS && module.smoothTab.getValue()) {
				module.setTabPressed(true);
				module.getTabOpenAnimation().reset();
				module.getTabOpenAnimation().update(true);
			} else if (action == GLFW.GLFW_RELEASE && module.smoothTab.getValue()) {
				module.setTabPressed(false);
				module.getTabOpenAnimation().reset();
				module.getTabOpenAnimation().update(false);
			}
		}
	}
}