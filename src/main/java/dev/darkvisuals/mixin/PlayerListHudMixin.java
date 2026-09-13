package dev.darkvisuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.BetterMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void darkvisuals$setupTabAnimation(DrawContext context, int scaledWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
		BetterMinecraft module = darkvisuals.getInstance().getModuleManager().getModule(BetterMinecraft.class);
		if (module == null || !module.isToggled() || !module.smoothTab.getValue()) return;

		float value = module.getTabOpenAnimation().getValue();
		 
		if (!module.isTabPressed() && value <= 0.01f) {
			ci.cancel();  
			return;
		}

		MatrixStack matrices = context.getMatrices();
		matrices.push();

		 
		float slideOffset = 22.0f;
		float slide = slideOffset * (1.0f - value);
		matrices.translate(0, -slide, 0);

		 
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1f, 1f, 1f, value);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void darkvisuals$cleanupTabAnimation(DrawContext context, int scaledWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
		BetterMinecraft module = darkvisuals.getInstance().getModuleManager().getModule(BetterMinecraft.class);
		if (module == null || !module.isToggled() || !module.smoothTab.getValue()) return;

		 
		context.getMatrices().pop();
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.disableBlend();
	}
}