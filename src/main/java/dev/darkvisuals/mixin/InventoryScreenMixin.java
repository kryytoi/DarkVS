package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.CustomInventory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {
    private InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "drawBackground", at = @At("RETURN"))
    private void darkvisuals$drawCustomInventoryPanel(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (darkvisuals.getInstance() == null || darkvisuals.getInstance().getModuleManager() == null) return;

        CustomInventory module = darkvisuals.getInstance().getModuleManager().getModule(CustomInventory.class);
        if (module == null || !module.isToggled()) return;

        module.renderInventoryPanel(context, this.x, this.y, this.backgroundWidth, this.backgroundHeight);
    }
}
