package dev.darkvisuals.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.darkvisuals.client.ChatUtils;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.utility.RWHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.MinecraftClient;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

	@Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
	private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
		if (message != null) {
			 
			String currentPrefix = darkvisuals.getInstance().getCommandManager().getPrefix();
			System.out.println("[DarkVisuals] chat mixin fired. msg='" + message + "' prefix='" + currentPrefix + "'");

			 
			if (message.startsWith(".reset")) {
				CommandDispatcher<CommandSource> dispatcher = darkvisuals.getInstance().getCommandManager().getDispatcher();
				StringReader reader = new StringReader("reset");
				try {
					dispatcher.execute(reader, darkvisuals.getInstance().getCommandManager().getSource());
				} catch (CommandSyntaxException e) {
					ChatUtils.sendMessage(String.format(I18n.translate("chat.error"), e.getRawMessage().getString()));
				}
				if (addToHistory) MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(message);
				ci.cancel();
				return;
			}

			String prefix = darkvisuals.getInstance().getCommandManager().getPrefix();
			if (prefix != null && !prefix.isEmpty() && message.startsWith(prefix)) {
				CommandDispatcher<CommandSource> dispatcher = darkvisuals.getInstance().getCommandManager().getDispatcher();
				StringReader reader = new StringReader(message.substring(prefix.length()));
				try {
					dispatcher.execute(reader, darkvisuals.getInstance().getCommandManager().getSource());
				} catch (CommandSyntaxException e) {
					ChatUtils.sendMessage(String.format(I18n.translate("chat.error"), e.getRawMessage().getString()));
				}
				if (addToHistory) MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(message);
				ci.cancel();
				return;
			}

			 
			RWHelper rwHelper = RWHelper.getInstance();
			if (rwHelper != null && rwHelper.isToggled() && rwHelper.getBlockBadWords().getValue()) {
				String blocked = rwHelper.findBlockedWord(message);
				if (blocked != null) {
					rwHelper.notifyBlockedWord(blocked);
					if (addToHistory) MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(message);
					ci.cancel();
				}
			}
		}
	}
}