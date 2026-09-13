package dev.darkvisuals.mixin;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.Crosshair;
import dev.darkvisuals.modules.impl.render.NoRender;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        EventRender2D event = new EventRender2D(context, tickCounter);
        darkvisuals.post(event);
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            var setting = darkvisuals.getInstance().getHudManager().getElements().getName("Hotbar");
            if (setting instanceof BooleanSetting bs && bs.getValue()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }

     
    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    public void cancelStatusBars(DrawContext context, CallbackInfo ci) {
        if (isHudHotbarEnabled()) ci.cancel();
    }

     
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void cancelXpBar(DrawContext context, int i, CallbackInfo ci) {
        if (isHudHotbarEnabled()) ci.cancel();
    }

     
    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    public void cancelXpLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (isHudHotbarEnabled()) ci.cancel();
    }

    @Unique
    private boolean isHudHotbarEnabled() {
        var setting = darkvisuals.getInstance().getHudManager().getElements().getName("Hotbar");
        return setting instanceof BooleanSetting bs && bs.getValue();
    }

     

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    public void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (darkvisuals.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && darkvisuals.getInstance().getModuleManager().getModule(NoRender.class).potions.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
    public void renderScoreboardSidebar(DrawContext drawContext, ScoreboardObjective objective, CallbackInfo ci) {
        if (darkvisuals.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && darkvisuals.getInstance().getModuleManager().getModule(NoRender.class).scoreboard.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    public void renderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (darkvisuals.getInstance().getModuleManager().getModule(Crosshair.class).isToggled()) {
            ci.cancel();  
        }
    }

    @Unique
    private boolean isPotion(ItemStack stack, StatusEffect status) {
        if (!(stack.getItem() instanceof PotionItem)) return false;
        PotionContentsComponent component = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (component == null) return false;
        if (component.potion().isEmpty()) return false;
        for (StatusEffectInstance effect : component.potion().get().value().getEffects()) {
            if (effect.getEffectType().value() == status) return true;
        }
        return false;
    }
}