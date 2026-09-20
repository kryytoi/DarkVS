package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventClickSlot;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * SlotLock — блокировка слотов хотбара от перемещения и выбрасывания.
 */
public class SlotLock extends Module {

    private final BooleanSetting slot1 = new BooleanSetting("Слот 1", false);
    private final BooleanSetting slot2 = new BooleanSetting("Слот 2", false);
    private final BooleanSetting slot3 = new BooleanSetting("Слот 3", false);
    private final BooleanSetting slot4 = new BooleanSetting("Слот 4", false);
    private final BooleanSetting slot5 = new BooleanSetting("Слот 5", false);
    private final BooleanSetting slot6 = new BooleanSetting("Слот 6", false);
    private final BooleanSetting slot7 = new BooleanSetting("Слот 7", false);
    private final BooleanSetting slot8 = new BooleanSetting("Слот 8", false);
    private final BooleanSetting slot9 = new BooleanSetting("Слот 9", false);

    private final BooleanSetting[] locked = {
            slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9
    };

    public SlotLock() {
        super("SlotLock", Category.Utility, "Блокировка слотов хотбара");
    }

    @EventHandler
    public void onClickSlot(EventClickSlot event) {
        if (mc.player == null) return;

        SlotActionType type = event.getSlotActionType();
        if (type != SlotActionType.PICKUP
                && type != SlotActionType.QUICK_MOVE
                && type != SlotActionType.SWAP
                && type != SlotActionType.THROW
                && type != SlotActionType.CLONE) {
            return;
        }

        int slot = event.getSlot();
        if (type == SlotActionType.SWAP) {
            // при обмене числовой клавишей button — целевой слот хотбара
            int button = event.getButton();
            if (button >= 0 && button <= 8 && locked[button].getValue()) event.setCancel(true);
            return;
        }

        if (slot < 0 || slot > 8) return; // 0..8 — хотбар в инвентаре игрока

        if (locked[slot].getValue()) {
            event.setCancel(true);
        }
    }
}
