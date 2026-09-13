package dev.darkvisuals.mixin;

import dev.darkvisuals.client.events.impl.EventClickSlot;
import dev.darkvisuals.modules.impl.utility.FriendHelper;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

	@Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
	private void darkvisuals$preventFriendAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
		FriendHelper fh = darkvisuals.getInstance().getModuleManager().getModule(FriendHelper.class);
		if (fh == null || !fh.isToggled()) return;

		BooleanSetting noFriendDamage = fh.getNoFriendDamage();
		if (noFriendDamage == null || !noFriendDamage.getValue()) return;

		if (target instanceof PlayerEntity pe) {
			String namePrimary = pe.getGameProfile().getName();
			String nameAlt = pe.getName() != null ? pe.getName().getString() : namePrimary;
			if (dev.darkvisuals.client.managers.FriendsManager.checkFriend(namePrimary) || dev.darkvisuals.client.managers.FriendsManager.checkFriend(nameAlt)) {
				ci.cancel();
			}
		}
	}

	@Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
	private void darkvisuals$clickSlotHook(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
		EventClickSlot event = new EventClickSlot(actionType, slotId, button, syncId);
		darkvisuals.post(event);
		if (event.isCancelled()) ci.cancel();
	}
} 