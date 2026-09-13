package dev.darkvisuals.client.managers;

import dev.darkvisuals.client.events.impl.EventGameShutdown;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.darkvisuals;
import meteordevelopment.orbit.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import dev.darkvisuals.client.events.impl.EventSettingChange;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoSaveManager implements Wrapper {

    private static final Logger LOGGER = LogManager.getLogger(AutoSaveManager.class);
    private static final String AUTO_SAVE_CONFIG_NAME = "autocfg";

     
    private final java.util.concurrent.atomic.AtomicBoolean emergencySaving = new java.util.concurrent.atomic.AtomicBoolean(false);
    private Thread shutdownHook;

     
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "SV-AutoSaveScheduler");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> pendingSave;
    private final Object saveLock = new Object();

     
    private volatile boolean allowAutoSave = false;

    public AutoSaveManager() {
         
        darkvisuals.getInstance().getEventHandler().subscribe(this);

         
        registerShutdownHook();
        registerUncaughtExceptionHandler();
    }

    @EventHandler
    public void onGameShutdown(EventGameShutdown event) {
        LOGGER.info("Автоматическое сохранение конфигурации...");

        synchronized (saveLock) {
            if (pendingSave != null && !pendingSave.isDone()) {
                pendingSave.cancel(false);
            }
        }

        try {
            ConfigManager configManager = darkvisuals.getInstance().getConfigManager();
            boolean success = configManager.saveConfig(AUTO_SAVE_CONFIG_NAME).join();
            if (success) {
                LOGGER.info("Конфигурация автоматически сохранена как '{}'", AUTO_SAVE_CONFIG_NAME);
            } else {
                LOGGER.error("Ошибка при автоматическом сохранении конфигурации");
            }
        } catch (Throwable t) {
            LOGGER.error("Ошибка при автоматическом сохранении конфигурации", t);
        } finally {
            scheduler.shutdownNow();
        }
    }

     @EventHandler
    public void onSettingChanged(EventSettingChange event) {
        scheduleAutoSave();
    }

     public void scheduleAutoSave() {
        if (!allowAutoSave) return;  
        synchronized (saveLock) {
            if (pendingSave != null && !pendingSave.isDone()) {
                pendingSave.cancel(false);
            }
            pendingSave = scheduler.schedule(this::doAutoSave, 500, TimeUnit.MILLISECONDS);
        }
    }

    private void doAutoSave() {
        try {
            ConfigManager configManager = darkvisuals.getInstance().getConfigManager();
            configManager.saveConfig(AUTO_SAVE_CONFIG_NAME).thenAccept(success -> {
                if (!success) {
                    LOGGER.warn("Автосохранение не удалось");
                }
            });
        } catch (Throwable t) {
            LOGGER.warn("Ошибка при выполнении автосохранения", t);
        }
    }

     public void loadAutoSave() {
         
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private boolean attempted = false;
            @Override
            public void onEndTick(MinecraftClient client) {
                if (attempted) return;
                attempted = true;

                ConfigManager configManager = darkvisuals.getInstance().getConfigManager();
                if (configManager.configExists(AUTO_SAVE_CONFIG_NAME)) {
                    LOGGER.info("Загрузка автоматически сохраненной конфигурации '{}'...", AUTO_SAVE_CONFIG_NAME);
                    configManager.loadConfig(AUTO_SAVE_CONFIG_NAME).thenAccept(success -> {
                        if (success) {
                            LOGGER.info("Автоматически сохраненная конфигурация загружена");
                        } else {
                            LOGGER.error("Ошибка при загрузке автоматически сохраненной конфигурации");
                        }
                         
                        allowAutoSave = true;
                    });
                } else {
                     
                    allowAutoSave = true;
                }
            }
        });
    }

     public void clearAutoSave() {
        ConfigManager configManager = darkvisuals.getInstance().getConfigManager();
        if (configManager.configExists(AUTO_SAVE_CONFIG_NAME)) {
            configManager.deleteConfig(AUTO_SAVE_CONFIG_NAME);
            LOGGER.info("Автоматически сохраненная конфигурация удалена");
        }
    }

     public void forceSave() {
        LOGGER.info("Принудительное сохранение конфигурации...");
        ConfigManager configManager = darkvisuals.getInstance().getConfigManager();
        try {
            boolean success = configManager.saveConfig(AUTO_SAVE_CONFIG_NAME).join();
            if (success) LOGGER.info("Конфигурация принудительно сохранена");
            else LOGGER.error("Ошибка при принудительном сохранении конфигурации");
        } catch (Throwable t) {
            LOGGER.error("Исключение при принудительном сохранении конфигурации", t);
        }
    }

     
    private void registerShutdownHook() {
        if (shutdownHook != null) return;
        shutdownHook = new Thread(() -> {
            emergencySave("JVM shutdown hook");
        }, "SV-AutoCfg-ShutdownHook");
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (Throwable t) {
            LOGGER.warn("Не удалось зарегистрировать shutdown hook для автосохранения", t);
        }
    }

    private void registerUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                LOGGER.error("Необработанное исключение в потоке {}: {}", thread.getName(), throwable.toString());
                 
                emergencySave("Uncaught exception");
            });
        } catch (Throwable t) {
            LOGGER.warn("Не удалось установить обработчик необработанных исключений для автосохранения", t);
        }
    }

    private void emergencySave(String reason) {
        if (!emergencySaving.compareAndSet(false, true)) return;
        try {
            LOGGER.info("Экстренное сохранение конфигурации ({}).", reason);
            ConfigManager configManager = darkvisuals.getInstance().getConfigManager();
             
            configManager.saveConfig(AUTO_SAVE_CONFIG_NAME).join();
            LOGGER.info("Экстренное сохранение завершено");
        } catch (Throwable t) {
            LOGGER.error("Ошибка при экстренном сохранении конфигурации", t);
        }
    }
}