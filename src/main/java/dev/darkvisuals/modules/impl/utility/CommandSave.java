package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.ChatUtils;
import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;

public class CommandSave extends Module {

    private static CommandSave INSTANCE;

    private final BooleanSetting saveHub = new BooleanSetting("Спасать от /hub", true);
    private final BooleanSetting saveSpawn = new BooleanSetting("Спасать от /spawn", true);
    private final BooleanSetting saveSuicide = new BooleanSetting("Спасать от /suicide", true);
    private final ListSetting saveFrom = new ListSetting("Спасать от", false, saveHub, saveSpawn, saveSuicide);

    public CommandSave() {
        super("CommandSave", Category.Utility, "Спасает от случайной отправки опасных команд");
        INSTANCE = this;
        getSettings().add(saveFrom);
    }

    public static CommandSave getInstance() {
        return INSTANCE;
    }

    public boolean shouldBlockCommand(String message) {
        if (!isToggled() || message == null) return false;

        String raw = message.trim().toLowerCase();
        if (raw.startsWith("/")) {
            raw = raw.substring(1).trim();
        }

        String cmd = raw.split("\\s+")[0];

        if (saveHub.getValue() && (cmd.equals("hub") || cmd.equals("lobby"))) {
            notifyBlocked("/" + cmd);
            return true;
        }

        if (saveSpawn.getValue() && cmd.equals("spawn")) {
            notifyBlocked("/" + cmd);
            return true;
        }

        if (saveSuicide.getValue() && (cmd.equals("suicide") || cmd.equals("kill"))) {
            notifyBlocked("/" + cmd);
            return true;
        }

        return false;
    }

    private void notifyBlocked(String commandName) {
        ChatUtils.sendMessage("§c[CommandSave] §fКоманда §e" + commandName + "§f была заблокирована для вашей безопасности!");
        try {
            if (darkvisuals.getInstance() != null && darkvisuals.getInstance().getNotifyManager() != null) {
                darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.dangerIcon, "Команда " + commandName + " заблокирована!", 2000));
            }
        } catch (Throwable ignored) {}
    }
}
