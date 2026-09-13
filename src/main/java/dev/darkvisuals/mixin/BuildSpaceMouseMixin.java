package dev.darkvisuals.mixin;

import dev.darkvisuals.client.managers.BuildSpaceManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * В режиме стройки ЛКМ/ПКМ ставят и убирают виртуальные блоки,
 * средняя кнопка (пик блока) блокируется, чтобы не рассинхронизировать инвентарь.
 */
@Mixin(Mouse.class)
public abstract class BuildSpaceMouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void darkvisuals$buildSpaceMouse(long window, int button, int action, int mods, CallbackInfo ci) {
        BuildSpaceManager buildSpace = BuildSpaceManager.getInstance();
        if (!buildSpace.isActive()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return;

        if (button == 0 || button == 1) {
            buildSpace.onMouseButton(button, action);
            ci.cancel();
        } else if (button == 2) {
            ci.cancel();
        }
    }
}
