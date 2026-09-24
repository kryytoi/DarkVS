package dev.darkvisuals.modules.api;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.resource.language.I18n;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class Module implements Wrapper {
    private final String name, description;
    private final Category category;
    protected boolean toggled;
    @Setter private Bind bind = new Bind(-1, false);
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

     
    public Module(String name, Category category) {
        this(name, category, name);
    }

    public void onEnable() {
        toggled = true;
        darkvisuals.getInstance().getEventHandler().subscribe(this);
        if (!fullNullCheck() && !name.equals("UI")) {
            String translatedName = I18n.translate(name);
            String msg = I18n.translate("notify.featureEnabled", translatedName);
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, msg, 1000));
        }
    }

    public void onDisable() {
        toggled = false;
        darkvisuals.getInstance().getEventHandler().unsubscribe(this);
        if (!fullNullCheck() && !name.equals("UI")) {
            String translatedName = I18n.translate(name);
            String msg = I18n.translate("notify.featureDisabled", translatedName);
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, msg, 1000));
        }
    }

    public void setToggled(boolean toggled) {
    if (toggled && dev.darkvisuals.liteapi.LiteApi.isBlocked(name)) {
        if (!fullNullCheck()) {
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, "Функция заблокирована сервером", 1500));
        }
        return;
    }
    if (toggled) onEnable();
    else onDisable();
         
        try {
            dev.darkvisuals.client.managers.AutoSaveManager asm = darkvisuals.getInstance().getAutoSaveManager();
            if (asm != null) asm.scheduleAutoSave();
        } catch (Throwable ignored) {}
    }

    public void toggle() {
        setToggled(!toggled);
    }

    public static boolean fullNullCheck() {
        return mc.player == null || mc.world == null;
    }
}