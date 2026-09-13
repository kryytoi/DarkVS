package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

 public class ShiftTap extends Module {

	private final NumberSetting holdTicks = new NumberSetting("setting.holdTicks", 3, 1, 8, 1);
	private int sneakTicksLeft = 0;
	private boolean forcedSneak = false;

	public ShiftTap() {
		super("ShiftTap", Category.Utility, net.minecraft.client.resource.language.I18n.translate("module.shifttap.description"));
        getSettings().add(holdTicks);
	}

	@EventHandler
	public void onAttack(EventAttackEntity e) {
		if (!isToggled()) return;
		if (!(e.getTarget() instanceof LivingEntity)) return;
		PlayerEntity self = e.getPlayer();
		MinecraftClient client = MinecraftClient.getInstance();
		if (self == null || client.player != self) return;

		 
		if (canCrit(self)) {
			this.sneakTicksLeft = holdTicks.getValue().intValue();
		}
	}

	@EventHandler
	public void onTick(EventTick e) {
		if (!isToggled()) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return;

		if (sneakTicksLeft > 0) {
			client.options.sneakKey.setPressed(true);
			forcedSneak = true;
			sneakTicksLeft--;
		} else if (forcedSneak) {
			client.options.sneakKey.setPressed(false);
			forcedSneak = false;
		}
	}

	private boolean canCrit(PlayerEntity player) {
		if (player.isOnGround()) return false;
		if (player.isSubmergedInWater() || player.isTouchingWater()) return false;
		if (player.isClimbing()) return false;
		if (player.hasVehicle()) return false;
		if (player.isSprinting()) return false;
		return true;
	}
} 