package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.util.math.BlockPos;

/**
 * CoordsClipboard — копирование координат в буфер обмена по бинду.
 * Срабатывает один раз и выключается.
 */
public class CoordsClipboard extends Module {

    private final BooleanSetting tpCommand = new BooleanSetting("Формат /tp", false);
    private final BooleanSetting includeDimension = new BooleanSetting("Измерение", true);

    public CoordsClipboard() {
        super("CoordsClipboard", Category.Utility, "Копировать координаты в буфер");
    }

    @Override
    public void onEnable() {
        super.onEnable();

        try {
            if (mc.player == null || mc.world == null) return;

            BlockPos pos = mc.player.getBlockPos();
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();

            StringBuilder builder = new StringBuilder();
            if (tpCommand.getValue()) builder.append("/tp ");
            builder.append(x).append(" ").append(y).append(" ").append(z);

            if (includeDimension.getValue() && mc.world.getRegistryKey() != null) {
                String dim = mc.world.getRegistryKey().getValue().getPath();
                builder.append(" (").append(dim).append(")");
            }

            String result = builder.toString();
            mc.keyboard.setClipboard(result);
            darkvisuals.getInstance().getNotifyManager()
                    .add(new Notify(NotifyIcons.successIcon, "Скопировано: " + result, 1500));
        } catch (Throwable ignored) {
        } finally {
            setToggled(false);
        }
    }
}
