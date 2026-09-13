package dev.darkvisuals.client.managers;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.modules.settings.impl.BindSetting;
import dev.darkvisuals.client.util.Wrapper;
import com.google.gson.*;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.MinecraftClient;

@Getter
public class ConfigManager implements Wrapper {

    private static final Logger LOGGER = LogManager.getLogger(ConfigManager.class);
    private final Gson gson;
    private final File configsDir;
    private final Map<String, ConfigData> configCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Старые английские имена модулей -> текущие русские (для загрузки старых конфигов)
    private static final Map<String, String> LEGACY_MODULE_NAMES = Map.of(
            "Optimization", "Оптимизация"
    );

    private static String legacyNameToCurrent(String name) {
        return LEGACY_MODULE_NAMES.getOrDefault(name, name);
    }

    public ConfigManager() {
        this.configsDir = new File(darkvisuals.getInstance().getGlobalsDir(), "configs");
        if (!this.configsDir.exists()) {
            this.configsDir.mkdirs();
        }
        LOGGER.info("Путь к папке конфигураций: {}", this.configsDir.getAbsolutePath());

         
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setLenient()
                .create();
    }

     public CompletableFuture<Boolean> saveConfig(String configName) {
         
        CompletableFuture<ConfigData> snapshotFuture = new CompletableFuture<>();
        Runnable collect = () -> {
            try {
                snapshotFuture.complete(collectConfigData());
            } catch (Exception e) {
                LOGGER.error("Ошибка при сборе данных конфигурации '{}'", configName, e);
                snapshotFuture.complete(null);
            }
        };
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) {
            collect.run();
        } else {
            client.execute(collect);
        }

         
        return snapshotFuture.thenApplyAsync(configData -> {
            if (configData == null) return false;
            try {
                File configFile = new File(configsDir, configName + ".simple");
                String json = gson.toJson(configData);
                Files.write(configFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
                configCache.put(configName, configData);
                LOGGER.info("Конфигурация '{}' успешно сохранена", configName);
                return true;
            } catch (Exception e) {
                LOGGER.error("Ошибка при сохранении конфигурации '{}'", configName, e);
                return false;
            }
        });
    }

     private ConfigData collectConfigData() {
        ConfigData configData = new ConfigData();

         
        try {
            String pref = darkvisuals.getInstance().getCommandManager().getPrefix();
            configData.setCommandPrefix(pref);
        } catch (Exception ignored) {}

         
        for (Module module : darkvisuals.getInstance().getModuleManager().getModules()) {
            ModuleData moduleData = new ModuleData();
            moduleData.setToggled(module.isToggled());
            moduleData.setBind(module.getBind());

             
            Map<String, Object> settings = new HashMap<>();
            for (Setting<?> setting : module.getSettings()) {
                Object value = setting.getValue();

                 
                if (setting instanceof ColorSetting) {
                     
                    value = String.format("%06X", (Integer) value);
                } else if (setting instanceof BindSetting) {
                     
                    dev.darkvisuals.modules.settings.api.Bind bind = (dev.darkvisuals.modules.settings.api.Bind) value;
                    String modeName = bind.getMode() != null ? bind.getMode().name() : dev.darkvisuals.modules.settings.api.Bind.Mode.TOGGLE.name();
                    value = bind.getKey() + ":" + bind.isMouse() + ":" + modeName;
                } else if (setting instanceof ListSetting) {
                     
                    ListSetting listSetting = (ListSetting) setting;
                    Map<String, Boolean> listValues = new HashMap<>();
                    for (BooleanSetting boolSetting : listSetting.getValue()) {
                        listValues.put(boolSetting.getName(), boolSetting.getValue());
                    }
                    value = listValues;
                }

                settings.put(setting.getName(), value);
            }
            moduleData.setSettings(settings);

            configData.getModules().put(module.getName(), moduleData);
        }

         
        configData.setCurrentTheme(ThemeManager.getInstance().getCurrentTheme().getName());

         
        Map<String, HudPositionData> hudPositions = new HashMap<>();
        for (HudElement hudElement : darkvisuals.getInstance().getHudManager().getHudElements()) {
            HudPositionData hudData = new HudPositionData();
            hudData.setX(hudElement.getPosition().getValue().getX());
            hudData.setY(hudElement.getPosition().getValue().getY());
            try {
                 
                dev.darkvisuals.modules.settings.impl.ListSetting elementsList = darkvisuals.getInstance().getHudManager().getElements();
                dev.darkvisuals.modules.settings.impl.BooleanSetting bs = elementsList.getName(hudElement.getName());
                boolean enabled = bs != null ? bs.getValue() : hudElement.isToggled();
                hudData.setEnabled(enabled);
            } catch (Exception ignored) {
                hudData.setEnabled(hudElement.isToggled());
            }
            hudPositions.put(hudElement.getName(), hudData);
        }
        configData.setHudPositions(hudPositions);

         
        Map<String, Map<String, Object>> hudSettings = new HashMap<>();
        for (HudElement hudElement : darkvisuals.getInstance().getHudManager().getHudElements()) {
            Map<String, Object> settings = new HashMap<>();
            for (Setting<?> setting : hudElement.getSettings()) {
                Object value = setting.getValue();
                if (setting instanceof ColorSetting) {
                    value = String.format("%06X", (Integer) value);
                } else if (setting instanceof ListSetting) {
                    ListSetting listSetting = (ListSetting) setting;
                    Map<String, Boolean> listValues = new HashMap<>();
                    for (BooleanSetting boolSetting : listSetting.getValue()) {
                        listValues.put(boolSetting.getName(), boolSetting.getValue());
                    }
                    value = listValues;
                } else if (setting instanceof BindSetting) {
                    dev.darkvisuals.modules.settings.api.Bind bind = (dev.darkvisuals.modules.settings.api.Bind) value;
                    String modeName = bind.getMode() != null ? bind.getMode().name() : dev.darkvisuals.modules.settings.api.Bind.Mode.TOGGLE.name();
                    value = bind.getKey() + ":" + bind.isMouse() + ":" + modeName;
                }
                settings.put(setting.getName(), value);
            }
            hudSettings.put(hudElement.getName(), settings);
        }
        configData.setHudSettings(hudSettings);

        return configData;
    }

     public CompletableFuture<Boolean> loadConfig(String configName) {
         
        return CompletableFuture.supplyAsync(() -> {
            try {
                File configFile = new File(configsDir, configName + ".simple");
                if (!configFile.exists()) {
                    LOGGER.error("Конфигурация '{}' не найдена", configName);
                    return null;
                }
                String json = Files.readString(configFile.toPath());
                return gson.fromJson(json, ConfigData.class);
            } catch (Exception e) {
                LOGGER.error("Ошибка при чтении конфигурации '{}': {}", configName, e.getMessage());
                return null;
            }
        }).thenCompose(configData -> {
             
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            if (configData == null) {
                result.complete(false);
                return result;
            }
            MinecraftClient.getInstance().execute(() -> {
                try {
                     
                    try {
                        String pref = configData.getCommandPrefix();
                        if (pref != null && !pref.isEmpty()) {
                            darkvisuals.getInstance().getCommandManager().setPrefix(pref);
                        }
                    } catch (Exception ignored) {}
                     
                    for (Map.Entry<String, ModuleData> entry : configData.getModules().entrySet()) {
                        String moduleName = entry.getKey();
                        ModuleData moduleData = entry.getValue();

                        Module module = darkvisuals.getInstance().getModuleManager().getModuleByName(legacyNameToCurrent(moduleName));
                        if (module != null) {
                             
                            if (moduleData.isToggled() != module.isToggled()) {
                                module.setToggled(moduleData.isToggled());
                            }

                             
                            if (moduleData.getBind() != null) {
                                module.setBind(moduleData.getBind());
                            }

                             
                            for (Map.Entry<String, Object> settingEntry : moduleData.getSettings().entrySet()) {
                                String settingName = settingEntry.getKey();
                                Object value = settingEntry.getValue();

                                Setting<?> setting = module.getSettings().stream()
                                        .filter(s -> s.getName().equals(settingName))
                                        .findFirst()
                                        .orElse(null);

                                if (setting != null) {
                                    try {
                                         
                                        setSettingValue(setting, value);
                                    } catch (Exception e) {
                                        LOGGER.warn("Не удалось применить настройку {} для модуля {}: {}",
                                                settingName, moduleName, e.getMessage());
                                    }
                                }
                            }
                        }
                    }

                     
                    try {
                        ThemeManager themeManager = ThemeManager.getInstance();
                        ThemeManager.Theme[] availableThemes = themeManager.getAvailableThemes();
                        for (ThemeManager.Theme theme : availableThemes) {
                            if (theme.getName().equals(configData.getCurrentTheme())) {
                                themeManager.setTheme(theme);
                                darkvisuals.post(new dev.darkvisuals.client.events.impl.EventThemeChanged(theme));
                                break;
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Не удалось применить тему {}: {}", configData.getCurrentTheme(), e.getMessage());
                    }

                     
                    if (configData.getHudPositions() != null) {
                        try {
                            for (Map.Entry<String, HudPositionData> entry : configData.getHudPositions().entrySet()) {
                                String hudName = entry.getKey();
                                HudPositionData hudData = entry.getValue();

                                HudElement hudElement = darkvisuals.getInstance().getHudManager().getHudElements().stream()
                                        .filter(element -> element.getName().equals(hudName))
                                        .findFirst()
                                        .orElse(null);

                                if (hudElement != null) {
                                    hudElement.getPosition().getValue().setX(hudData.getX());
                                    hudElement.getPosition().getValue().setY(hudData.getY());
                                    if (hudData.isEnabled() != hudElement.isToggled()) {
                                        hudElement.setToggled(hudData.isEnabled());
                                    }
                                     
                                    try {
                                        dev.darkvisuals.modules.settings.impl.ListSetting elementsList = darkvisuals.getInstance().getHudManager().getElements();
                                        dev.darkvisuals.modules.settings.impl.BooleanSetting bs = elementsList.getName(hudName);
                                        if (bs != null && bs.getValue() != hudData.isEnabled()) {
                                            bs.setValue(hudData.isEnabled());
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Не удалось применить позиции HUD: {}", e.getMessage());
                        }
                    }

                     
                    if (configData.getHudSettings() != null) {
                        for (Map.Entry<String, Map<String, Object>> entry : configData.getHudSettings().entrySet()) {
                            String hudName = entry.getKey();
                            Map<String, Object> settings = entry.getValue();
                            HudElement hudElement = darkvisuals.getInstance().getHudManager().getHudElements().stream()
                                    .filter(element -> element.getName().equals(hudName))
                                    .findFirst()
                                    .orElse(null);
                            if (hudElement != null) {
                                for (Map.Entry<String, Object> settingEntry : settings.entrySet()) {
                                    String settingName = settingEntry.getKey();
                                    Object value = settingEntry.getValue();
                                    Setting<?> setting = hudElement.getSettings().stream()
                                            .filter(s -> s.getName().equals(settingName))
                                            .findFirst()
                                            .orElse(null);
                                    if (setting != null) {
                                        try {
                                            setSettingValue(setting, value);
                                        } catch (Exception e) {
                                            LOGGER.warn("Не удалось применить настройку {} для HUD {}: {}", settingName, hudName, e.getMessage());
                                        }
                                    }
                                }
                            }
                        }
                    }

                     
                    configCache.put(configName, configData);
                    LOGGER.info("Конфигурация '{}' успешно загружена", configName);
                    result.complete(true);
                } catch (Exception e) {
                    LOGGER.error("Ошибка при применении конфигурации '{}': {}", configName, e.getMessage());
                    result.complete(false);
                }
            });
            return result;
        });
    }

     public String[] getConfigList() {
        File[] files = configsDir.listFiles((dir, name) -> name.endsWith(".simple"));
        if (files == null) return new String[0];

        String[] configs = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            configs[i] = files[i].getName().replace(".simple", "");
        }
        return configs;
    }

     public String getConfigsDirectory() {
        return configsDir.getAbsolutePath();
    }

     public boolean deleteConfig(String configName) {
        File configFile = new File(configsDir, configName + ".simple");
        if (configFile.exists()) {
            boolean deleted = configFile.delete();
            if (deleted) {
                configCache.remove(configName);
                LOGGER.info("Конфигурация '{}' удалена", configName);
            }
            return deleted;
        }
        return false;
    }

     public boolean configExists(String configName) {
        return new File(configsDir, configName + ".simple").exists();
    }

     @SuppressWarnings("unchecked")
    private void setSettingValue(Setting<?> setting, Object value) {
        if (setting instanceof BooleanSetting) {
            if (value instanceof Boolean) {
                ((BooleanSetting) setting).setValue((Boolean) value);
            }
        } else if (setting instanceof NumberSetting) {
            if (value instanceof Number) {
                NumberSetting numberSetting = (NumberSetting) setting;
                float floatValue = ((Number) value).floatValue();
                if (floatValue >= numberSetting.getMin() && floatValue <= numberSetting.getMax()) {
                    numberSetting.setValue(floatValue);
                }
            }
        } else if (setting instanceof StringSetting) {
            if (value instanceof String) {
                ((StringSetting) setting).setValue((String) value);
            }
        } else if (setting instanceof EnumSetting) {
            if (value instanceof String) {
                EnumSetting<?> enumSetting = (EnumSetting<?>) setting;
                try {
                     
                    enumSetting.setEnumValue((String) value);
                } catch (Exception e) {
                    LOGGER.warn("Неверное значение enum: {}", value);
                }
            }
        } else if (setting instanceof ColorSetting) {
            if (value instanceof String) {
                try {
                    String hex = (String) value;
                    // принимаем и 6-значный RGB (как сохраняем сейчас), и 8-значный
                    // ARGB из старых конфигов (FF96BEFF) — иначе parseLong падает
                    // на значениях, не влезающих в int, и цвет теряется
                    long parsed = Long.parseLong(hex, 16);
                    int color;
                    if (hex.length() > 6) {
                        color = (int) parsed; // 8 hex-цифр ARGB
                    } else {
                        color = (int) (parsed | 0xFF000000L); // RGB -> непрозрачный ARGB
                    }
                    ((ColorSetting) setting).set(color);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Неверный формат цвета: {}", value);
                }
            } else if (value instanceof Number) {
                ((ColorSetting) setting).set(((Number) value).intValue());
            }
        } else if (setting instanceof ListSetting) {
            if (value instanceof Map) {
                ListSetting listSetting = (ListSetting) setting;
                @SuppressWarnings("unchecked")
                Map<String, Object> listValues = (Map<String, Object>) value;

                for (BooleanSetting boolSetting : listSetting.getValue()) {
                    Object savedValue = listValues.get(boolSetting.getName());
                    if (savedValue instanceof Boolean) {
                        boolSetting.setValue((Boolean) savedValue);
                    }
                }
            }
        } else if (setting instanceof BindSetting) {
            if (value instanceof String) {
                try {
                    String[] parts = ((String) value).split(":");
                    int key = Integer.parseInt(parts[0]);
                    boolean isMouse = parts.length > 1 && Boolean.parseBoolean(parts[1]);
                    dev.darkvisuals.modules.settings.api.Bind.Mode mode = dev.darkvisuals.modules.settings.api.Bind.Mode.TOGGLE;
                    if (parts.length > 2) {
                        try {
                            mode = dev.darkvisuals.modules.settings.api.Bind.Mode.valueOf(parts[2]);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    ((BindSetting) setting).setValue(new dev.darkvisuals.modules.settings.api.Bind(key, isMouse, mode));
                } catch (Exception e) {
                    LOGGER.warn("Неверный формат бинда: {}", value);
                }
            }
        }
    }

     public static class ConfigData {
        private Map<String, ModuleData> modules = new HashMap<>();
        private String currentTheme;
        private Map<String, HudPositionData> hudPositions = new HashMap<>();
        private Map<String, Map<String, Object>> hudSettings = new HashMap<>();
        private String commandPrefix;

        public Map<String, ModuleData> getModules() {
            return modules;
        }

        public void setModules(Map<String, ModuleData> modules) {
            this.modules = modules;
        }

        public String getCurrentTheme() {
            return currentTheme;
        }

        public void setCurrentTheme(String currentTheme) {
            this.currentTheme = currentTheme;
        }

        public Map<String, HudPositionData> getHudPositions() {
            return hudPositions;
        }

        public void setHudPositions(Map<String, HudPositionData> hudPositions) {
            this.hudPositions = hudPositions;
        }

        public Map<String, Map<String, Object>> getHudSettings() {
            return hudSettings;
        }

        public void setHudSettings(Map<String, Map<String, Object>> hudSettings) {
            this.hudSettings = hudSettings;
        }

        public String getCommandPrefix() {
            return commandPrefix;
        }

        public void setCommandPrefix(String commandPrefix) {
            this.commandPrefix = commandPrefix;
        }
    }

    public static class ModuleData {
        private boolean toggled;
        private dev.darkvisuals.modules.settings.api.Bind bind;
        private Map<String, Object> settings = new HashMap<>();

        public boolean isToggled() {
            return toggled;
        }

        public void setToggled(boolean toggled) {
            this.toggled = toggled;
        }

        public dev.darkvisuals.modules.settings.api.Bind getBind() {
            return bind;
        }

        public void setBind(dev.darkvisuals.modules.settings.api.Bind bind) {
            this.bind = bind;
        }

        public Map<String, Object> getSettings() {
            return settings;
        }

        public void setSettings(Map<String, Object> settings) {
            this.settings = settings;
        }
    }

    public static class HudPositionData {
        private float x;
        private float y;
        private boolean enabled;

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
