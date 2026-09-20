package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * ItemReminder — напоминание, когда расходники заканчиваются.
 */
public class ItemReminder extends Module {

    private final NumberSetting totems = new NumberSetting("Тотемы", 2f, 0f, 36f, 1f);
    private final NumberSetting pearls = new NumberSetting("Жемчуг", 4f, 0f, 64f, 1f);
    private final NumberSetting gaps = new NumberSetting("Яблоки", 4f, 0f, 64f, 1f);
    private final NumberSetting food = new NumberSetting("Еда", 8f, 0f, 64f, 1f);

    private long lastAlertTotem = -10_000L;
    private long lastAlertPearl = -10_000L;
    private long lastAlertGap = -10_000L;
    private long lastAlertFood = -10_000L;
    private static final long ALERT_COOLDOWN_MS = 30_000L;

    public ItemReminder() {
        super("ItemReminder", Category.Utility, "Напоминание о расходниках");
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();

        if (now - lastAlertTotem > ALERT_COOLDOWN_MS) {
            int count = count(Items.TOTEM_OF_UNDYING);
            if (count <= totems.getValue()) {
                alert("Тотемов: " + count + " (" + totems.getValue().intValue() + " минимум)");
                lastAlertTotem = now;
            }
        }

        if (now - lastAlertPearl > ALERT_COOLDOWN_MS) {
            int count = count(Items.ENDER_PEARL);
            if (count <= pearls.getValue()) {
                alert("Жемчуга Края: " + count);
                lastAlertPearl = now;
            }
        }

        if (now - lastAlertGap > ALERT_COOLDOWN_MS) {
            int count = count(Items.ENCHANTED_GOLDEN_APPLE) + count(Items.GOLDEN_APPLE);
            if (count <= gaps.getValue()) {
                alert("Золотых яблок: " + count);
                lastAlertGap = now;
            }
        }

        if (now - lastAlertFood > ALERT_COOLDOWN_MS) {
            int totalFood = 0;
            for (int i = 0; i < mc.player.getInventory().size(); i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                if (stack.get(net.minecraft.component.DataComponentTypes.FOOD) != null) totalFood += stack.getCount();
            }
            if (totalFood <= food.getValue()) {
                alert("Еды: " + totalFood);
                lastAlertFood = now;
            }
        }
    }

    private int count(Item item) {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    private void alert(String message) {
        try {
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.dangerIcon, message, 2000));
        } catch (Throwable ignored) {
        }
    }
}
