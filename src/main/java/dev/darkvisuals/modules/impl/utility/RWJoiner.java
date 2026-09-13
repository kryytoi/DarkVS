package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class RWJoiner extends Module {

     
    private final StringSetting grief = new StringSetting("Гриф", "", false);
     
    private final NumberSetting delay = new NumberSetting("Задержка (тики)", 3, 1, 20, 1);

    private static final String COMPASS_NAME = "выбор гриф (ПКМ)";

    private enum Step { FIND_COMPASS, USE_COMPASS, WAIT_MENU, CLICK_TABLE, CLICK_HEAD, LOOP }
    private Step step = Step.FIND_COMPASS;
    private int timer = 0;

    public RWJoiner() {
        super("RWJoiner", Category.Utility, "Авто-вход на гриф ReallyWorld");
    }

    @Override
    public void onEnable() {
        super.onEnable();  
        step = Step.FIND_COMPASS;
        timer = 0;
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (timer > 0) { timer--; return; }

        switch (step) {
            case FIND_COMPASS -> {
                int slot = findCompassSlot();
                if (slot == -1) { setToggled(false); return; }  
                mc.player.getInventory().selectedSlot = slot;    
                step = Step.USE_COMPASS;
                timer = delayTicks();
            }
            case USE_COMPASS -> {
                 
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                step = Step.WAIT_MENU;
                timer = delayTicks();
            }
            case WAIT_MENU -> {
                if (mc.currentScreen instanceof HandledScreen<?>) {
                    step = Step.CLICK_TABLE;
                    timer = delayTicks();
                } else {
                     
                    step = Step.USE_COMPASS;
                    timer = delayTicks();
                }
            }
            case CLICK_TABLE -> {
                 
                clickItemInContainer(Items.CRAFTING_TABLE, null);
                step = Step.CLICK_HEAD;
                timer = delayTicks();
            }
            case CLICK_HEAD -> {
                 
                clickItemInContainer(Items.PLAYER_HEAD, targetHeadName());
                step = Step.LOOP;
                timer = delayTicks();
            }
            case LOOP -> {
                if (mc.player != null) mc.player.closeHandledScreen();  
                 
                if (findCompassSlot() == -1) {
                    setToggled(false);
                } else {
                    step = Step.FIND_COMPASS;
                    timer = delayTicks();
                }
            }
        }
    }

    private int delayTicks() {
        return Math.max(1, Math.round(delay.getValue()));
    }

     
    private int findCompassSlot() {
        int fallback = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.isEmpty() || s.getItem() != Items.COMPASS) continue;
            String name = s.getName().getString();
            if (name != null && name.toLowerCase().contains(COMPASS_NAME.toLowerCase())) return i;
            if (fallback == -1) fallback = i;
        }
        return fallback;  
    }

    private String targetHeadName() {
        String g = grief.getValue() == null ? "" : grief.getValue().trim();
        return "ГРИФ #" + g + " (1.16.5+)";
    }

     
     
    private boolean clickItemInContainer(Item item, String requiredName) {
        if (!(mc.currentScreen instanceof HandledScreen<?> hs)) return false;
        ScreenHandler handler = hs.getScreenHandler();
        int containerSize = handler.slots.size() - 36;  
        for (Slot slot : handler.slots) {
            if (containerSize > 0 && slot.id >= containerSize) continue;  
            ItemStack st = slot.getStack();
            if (st.isEmpty() || st.getItem() != item) continue;
            if (requiredName != null) {
                String n = st.getName().getString();
                if (n == null || !n.toLowerCase().contains(requiredName.toLowerCase())) continue;
            }
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
            return true;
        }
        return false;
    }
}