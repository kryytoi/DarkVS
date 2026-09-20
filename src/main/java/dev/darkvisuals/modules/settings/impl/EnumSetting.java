package dev.darkvisuals.modules.settings.impl;

import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.modules.settings.api.EnumConverter;
import dev.darkvisuals.modules.settings.api.Nameable;

import java.util.function.Supplier;

public class EnumSetting<Value extends Enum<?>> extends Setting<Value> {

    public EnumSetting(String name, Value defaultValue) {
        super(name, defaultValue);
    }

    public EnumSetting(String name, Value defaultValue, Supplier<Boolean> visible) {
        super(name, defaultValue, visible);
    }

    public void increaseEnum() {
        setValue((Value) EnumConverter.increaseEnum(value));
    }

    public String currentEnumName() {
        return ((Nameable) value).getName();
    }

    public void setEnumValue(String value) {
        for (Value e : (Value[]) this.value.getClass().getEnumConstants()) {
            // Gson сохраняет enum по name() («LIQUID_GLASS»), а UI показывает
            // displayName («Liquid Glass») — принимаем оба варианта, иначе
            // стиль Liquid Glass не загрузится из конфига.
            if (((Nameable) e).getName().equalsIgnoreCase(value) || e.name().equalsIgnoreCase(value)) {
                setValue(e);
                break;
            }
        }
    }
}