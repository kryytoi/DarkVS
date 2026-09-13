package dev.darkvisuals.mixin;

import dev.darkvisuals.client.util.UserTabBadge;
import dev.darkvisuals.modules.impl.utility.UserTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

 @Mixin(PlayerListHud.class)
public class UserTabPlayerListMixin {

    @Inject(method = "collectPlayerEntries", at = @At("RETURN"), cancellable = true)
    private void darkvisuals$showSelfInTab(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
        try {
            UserTab userTab = UserTab.getInstance();
            if (userTab == null || !userTab.isToggled() || !userTab.getShowSelfInList().getValue()) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.player.networkHandler == null) return;

            UUID selfUuid = mc.player.getUuid();
            List<PlayerListEntry> entries = cir.getReturnValue();
            if (entries == null) return;

             
            for (PlayerListEntry entry : entries) {
                if (entry.getProfile().getId().equals(selfUuid)) return;
            }

            PlayerListEntry selfEntry = mc.player.networkHandler.getPlayerListEntry(selfUuid);
            if (selfEntry == null) return;

             
            List<PlayerListEntry> mutable = new ArrayList<>(entries);
            mutable.add(selfEntry);
            cir.setReturnValue(mutable);
        } catch (Throwable ignored) {}
    }

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void darkvisuals$modifyTabName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        try {
            UserTab userTab = UserTab.getInstance();
            if (userTab == null || !userTab.isToggled() || entry == null) return;

            Text result = cir.getReturnValue();
            if (result == null) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            boolean isSelf = mc.player != null && entry.getProfile().getId().equals(mc.player.getUuid());

             
            if (isSelf && userTab.getCleanOwnName().getValue()) {
                result = userTab.stripServerGlyphs(result);
            }

             
            if (userTab.getShowInTab().getValue()
                    && entry.getProfile().getName() != null
                    && userTab.isDarkVisualsUser(entry.getProfile().getId())) {
                result = UserTabBadge.apply(result);
            }

            cir.setReturnValue(result);
        } catch (Throwable ignored) {}
    }
}