package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BindSetting;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.client.events.impl.EventTick;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.resource.language.I18n;

public class FTHelper extends Module {
     
    private final BindSetting disorientBind = new BindSetting("setting.disorientBind", new Bind(GLFW.GLFW_KEY_H, false));
    private final BindSetting trapBind = new BindSetting("setting.trapBind", new Bind(GLFW.GLFW_KEY_T, false));

    private boolean disorientLatch = false;
    private boolean trapLatch = false;

    public FTHelper() {
        super("FTHelper", Category.Utility, I18n.translate("module.fthelper.description"));
        getSettings().add(disorientBind);
        getSettings().add(trapBind);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!isToggled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        long window = mc.getWindow().getHandle();

         
        boolean disorientDown = isBindDown(window, disorientBind.getValue());
        if (disorientDown && !disorientLatch) {
            switchToItem(mc, Items.ENDER_EYE);  
            disorientLatch = true;
        } else if (!disorientDown) disorientLatch = false;

         
        boolean trapDown = isBindDown(window, trapBind.getValue());
        if (trapDown && !trapLatch) {
            switchToItem(mc, Items.NETHERITE_SCRAP);  
            trapLatch = true;
        } else if (!trapDown) trapLatch = false;
    }

    private static boolean isBindDown(long window, Bind bind) {
        if (bind.isMouse()) return GLFW.glfwGetMouseButton(window, bind.getKey()) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(window, bind.getKey()) == GLFW.GLFW_PRESS;
    }

    private void switchToItem(MinecraftClient mc, Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                mc.player.getInventory().selectedSlot = i;
                break;
            }
        }
    }
}
