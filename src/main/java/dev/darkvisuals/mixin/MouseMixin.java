package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventMouse;
import dev.darkvisuals.modules.impl.render.ViewModel;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    public void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
         
        if (darkvisuals.getInstance() == null
                || darkvisuals.getInstance().getModuleManager() == null) {
            return;
        }
        EventMouse event = new EventMouse(button, action);
        darkvisuals.post(event);

        ViewModel viewModel = darkvisuals.getInstance().getModuleManager().getModule(ViewModel.class);
        if (viewModel != null && viewModel.isConfiguring()) {
            viewModel.handleMouseButton(button, action);
             
            ci.cancel();
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    public void onCursorPos(long window, double x, double y, CallbackInfo ci) {
         
        if (darkvisuals.getInstance() == null
                || darkvisuals.getInstance().getModuleManager() == null) {
            return;
        }
        ViewModel viewModel = darkvisuals.getInstance().getModuleManager().getModule(ViewModel.class);
        if (viewModel != null && viewModel.isConfiguring()) {
            viewModel.handleCursorPos(x, y);
             
            ci.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    public void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
         
        if (darkvisuals.getInstance() == null
                || darkvisuals.getInstance().getModuleManager() == null) {
            return;
        }
        ViewModel viewModel = darkvisuals.getInstance().getModuleManager().getModule(ViewModel.class);
        if (viewModel != null && viewModel.isConfiguring()) {
            viewModel.handleScroll(vertical);
            ci.cancel();
        }
    }
}
