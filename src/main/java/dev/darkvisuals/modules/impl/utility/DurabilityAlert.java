package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * DurabilityAlert — предупреждение о низком запасе прочности.
 * Одно предупреждение на предмет, пока он не починен выше порога.
 */
public class DurabilityAlert extends Module {

    private final NumberSetting threshold = new NumberSetting("Порог %", 15f, 1f, 80f, 1f);

    private final Set<String> alerted = new HashSet<>();
    private long lastAlert = -10_000L;
    private static final long ALERT_COOLDOWN_MS = 5_000L;

    public DurabilityAlert() {
        super("DurabilityAlert", Category.Utility, "Предупреждение о прочности");
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            checkStack(mc.player.getInventory().getStack(i), "inv" + i);
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            checkStack(mc.player.getEquippedStack(slot), "eq" + slot.getName());
        }
    }

    private void checkStack(ItemStack stack, String key) {
        if (stack.isEmpty() || !stack.isDamageable()) return;

        int percent = (int) Math.round((1f - stack.getDamage() / (float) stack.getMaxDamage()) * 100f);

        if (percent > threshold.getValue() + 5) {
            alerted.remove(key);
            return;
        }

        if (percent > threshold.getValue()) return;
        if (alerted.contains(key)) return;

        long now = System.currentTimeMillis();
        if (now - lastAlert < ALERT_COOLDOWN_MS) return;

        alerted.add(key);
        lastAlert = now;
        alert(stack.getName().getString() + " " + percent + "%");
    }

    private void alert(String message) {
        try {
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.dangerIcon, message, 2000));
        } catch (Throwable ignored) {
        }
    }
}
