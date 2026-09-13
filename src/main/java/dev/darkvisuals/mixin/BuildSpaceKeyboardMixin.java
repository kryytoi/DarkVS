package dev.darkvisuals.mixin;

import dev.darkvisuals.client.managers.BuildSpaceManager;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * В режиме стройки блокируем выброс предметов (Q) — пакеты заморожены,
 * и клиентская попытка выброса рассинхронизировала бы инвентарь с сервером.
 */
@Mixin(Keyboard.class)
public abstract class BuildSpaceKeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void darkvisuals$buildSpaceKeys(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        BuildSpaceManager buildSpace = BuildSpaceManager.getInstance();
        if (!buildSpace.isActive()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return;

        if (key == GLFW.GLFW_KEY_Q && action == GLFW.GLFW_PRESS) {
            ci.cancel();
        }
    }
}
