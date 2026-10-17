package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventKey;
import dev.darkvisuals.client.ui.swap.SwapEditorScreen;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.impl.BindSetting;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.registry.Registries;
import org.lwjgl.glfw.GLFW;

public class Swap extends Module {

    // The item identifier to swap FROM (in offhand) - stored as item registry name
    private final StringSetting fromItem = new StringSetting("Предмет из", "minecraft:totem_of_undying", false);
    // The item identifier to swap TO (from hotbar)
    private final StringSetting toItem = new StringSetting("Предмет в", "minecraft:shield", false);
    // The bind to trigger swap
    private final BindSetting swapBind = new BindSetting("Бинд", new Bind(-1, false));
    // Button to open editor
    private final ButtonSetting editor = new ButtonSetting("Редактор",
            () -> MinecraftClient.getInstance().send(
                    () -> MinecraftClient.getInstance().setScreen(new SwapEditorScreen(this))));

    private boolean latch = false;

    public Swap() {
        super("Swap", Category.Utility, "Меняет предмет в левой руке из хотбара");
    }

    public String getFromItem() { return fromItem.getValue(); }
    public String getToItem() { return toItem.getValue(); }
    public void setFromItem(String s) { fromItem.setValue(s); }
    public void setToItem(String s) { toItem.setValue(s); }

    @EventHandler
    public void onKey(EventKey e) {
        if (!isToggled() || fullNullCheck()) return;
        if (mc.currentScreen != null) return;

        Bind bind = swapBind.getValue();
        if (bind == null || bind.isEmpty() || bind.isMouse()) return;
        if (bind.getKey() != e.getKey()) return;

        if (e.getAction() == GLFW.GLFW_PRESS && !latch) {
            latch = true;
            doSwap();
        } else if (e.getAction() == GLFW.GLFW_RELEASE) {
            latch = false;
        }
    }

    private void doSwap() {
        if (mc.player == null || mc.interactionManager == null) return;

        String targetItemId = toItem.getValue();
        if (targetItemId == null || targetItemId.isBlank()) return;

        // Find the target item in hotbar (slots 0-8)
        int hotbarSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (id.equalsIgnoreCase(targetItemId)) {
                hotbarSlot = i;
                break;
            }
        }

        if (hotbarSlot < 0) return; // item not found in hotbar

        // Swap hotbar slot with offhand (slot 40 in player inventory = offhand)
        // Using SlotActionType.SWAP with button = hotbarSlot swaps that hotbar slot with offhand
        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                40, // offhand slot index in player screen handler
                hotbarSlot, // button = hotbar slot to swap with
                SlotActionType.SWAP,
                mc.player
        );
    }
}
