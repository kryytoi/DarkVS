package dev.darkvisuals.mixin;

import dev.darkvisuals.client.managers.BuildSpaceManager;
import dev.darkvisuals.client.ui.structureeditor.StructureEditorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * В режиме стройки:
 *  — ESC (меню игры) сохраняет дом и выходит из пространства стройки;
 *  — остальные экраны (инвентарь, чат и т.д.) не открываются, чтобы не слать пакеты.
 */
@Mixin(MinecraftClient.class)
public abstract class BuildSpaceScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void darkvisuals$buildSpaceScreen(Screen screen, CallbackInfo ci) {
        BuildSpaceManager buildSpace = BuildSpaceManager.getInstance();
        if (!buildSpace.isActive()) return;

        if (screen instanceof GameMenuScreen) {
            // ESC — сохранить дом и выйти
            ci.cancel();
            buildSpace.exit(true);
        } else if (screen != null && !(screen instanceof StructureEditorScreen)) {
            ci.cancel();
        }
    }
}
