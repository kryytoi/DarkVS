package dev.darkvisuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;
    @Shadow @Final private boolean reloading;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;

    private static final Identifier LOGO_TEX = Identifier.of("darkvisuals", "textures/mainmenu/logo.png");

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        long now = Util.getMeasuringTimeMs();
        // The initial reload only preloads GUI shaders; fonts and text shaders are not ready yet.
        boolean resourcesReady = this.client.isFinishedLoading();

        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = now;
        }

        float f = this.reloadCompleteTime > -1L ? (float)(now - this.reloadCompleteTime) / 1000.0F : -1.0F;

        if (f >= 0.0F) {
            if (this.client.currentScreen != null) {
                this.client.currentScreen.render(context, 0, 0, delta);
            }
            float fade = 1.0F - MathHelper.clamp(f / 0.6F, 0.0F, 1.0F);
            int alpha = (int)(fade * 255.0F);
            if (alpha > 0) {
                context.fill(0, 0, width, height, (alpha << 24) | 0x0E0B16);
            }
            if (f >= 0.6F) {
                this.client.setOverlay(null);
                return;
            }
        }

        float rawProgress = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + rawProgress * 0.05F, 0.0F, 1.0F);

        if (f < 0.0F) {
            context.fill(0, 0, width, height, 0xFF0E0B16);
            context.fillGradient(0, 0, width, (int)(height * 0.45f), 0x7828143C, 0x000E0B16);
            context.fillGradient(0, (int)(height * 0.55f), width, height, 0x000E0B16, 0xB40A0810);

            int cx = width / 2;
            int cy = height / 2;

            int logoSize = 64;
            int logoX = cx - logoSize / 2;
            int logoY = cy - 64;
            try {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                context.drawTexture(RenderLayer::getGuiTextured, LOGO_TEX, logoX, logoY, 0.0F, 0.0F, logoSize, logoSize, logoSize, logoSize);
            } catch (Throwable ignored) {
            }

            if (resourcesReady && this.client.textRenderer != null) {
                String title = "DARK VISUALS";
                int tw = this.client.textRenderer.getWidth(title);
                context.drawTextWithShadow(this.client.textRenderer, title, cx - tw / 2, cy + 8, 0xFFA855F7);
            }

            int barW = 260;
            int barH = 16;
            int barX = cx - barW / 2;
            int barY = cy + 24;

            context.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF362A50);
            context.fill(barX, barY, barX + barW, barY + barH, 0xFF161224);

            int fillW = Math.max(0, (int)(barW * this.progress));
            if (fillW > 0) {
                context.fill(barX, barY, barX + fillW, barY + barH, 0xFFA855F7);
                context.fillGradient(barX, barY, barX + fillW, barY + barH / 2, 0x40FFFFFF, 0x00FFFFFF);
            }

            if (resourcesReady && this.client.textRenderer != null) {
                int pct = (int) Math.round(this.progress * 100.0);
                String barText = (this.reloading ? "Загрузка ресурсов... " : "Инициализация... ") + pct + "%";
                int tw = this.client.textRenderer.getWidth(barText);
                int th = this.client.textRenderer.fontHeight;
                context.drawTextWithShadow(this.client.textRenderer, barText, cx - tw / 2, barY + (barH - th) / 2 + 1, 0xFFFFFFFF);
            }
        }

        if (this.reloadCompleteTime == -1L && this.reload.isComplete()) {
            try {
                this.reload.throwException();
                this.exceptionHandler.accept(Optional.empty());
            } catch (Throwable t) {
                this.exceptionHandler.accept(Optional.of(t));
            }
            this.reloadCompleteTime = Util.getMeasuringTimeMs();
            if (this.client.currentScreen != null) {
                this.client.currentScreen.init(this.client, context.getScaledWindowWidth(), context.getScaledWindowHeight());
            }
        }
    }
}
