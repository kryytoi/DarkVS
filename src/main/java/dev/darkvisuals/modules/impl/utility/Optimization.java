package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.particle.ParticlesMode;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class Optimization extends Module {


    private ParticlesMode oldParticles = ParticlesMode.ALL;
    private double oldEntityDistance = 1.0;
    private int oldBiomeBlendRadius = 2;
    private boolean oldEntityShadows = true;
    private GraphicsMode oldGraphics = GraphicsMode.FANCY;
    private CloudRenderMode oldClouds = CloudRenderMode.FANCY;
    private boolean oldAo = true;
    private boolean oldBobView = true;
    private int oldMipmap = 4;
    private int oldMaxFps = 120;
    private boolean oldVsync = true;
    private int oldViewDistance = 12;
    private int oldSimulationDistance = 12;
    private double oldFovEffectScale = 1.0;
    private double oldScreenEffectScale = 1.0;
    // имя этой опции нестабильно между версиями yarn (screen/nausea) — ищем через рефлексию
    private net.minecraft.client.option.SimpleOption<Double> screenEffectOption;

    private long lastCleanup = 0L;






    public @NotNull BooleanSetting dynamicFps = new BooleanSetting("Динамический ФПС", false);
    public @NotNull NumberSetting backgroundFps = new NumberSetting("ФПС в фоне", 15f, 1f, 60f, 1f, dynamicFps::getValue);


    public @NotNull BooleanSetting hideEntities = new BooleanSetting("Скрывать сущности", false);

    public @NotNull BooleanSetting hidePlayers = new BooleanSetting("Скрывать игроков", false);

    public @NotNull BooleanSetting hideBlockEntities = new BooleanSetting("Скрывать блоки-сущности", false);

    public @NotNull BooleanSetting hideItems = new BooleanSetting("Скрывать выпавшие предметы", false);


    public @NotNull BooleanSetting limitParticles = new BooleanSetting("Лимит частиц", false);
    public @NotNull NumberSetting particleLimit = new NumberSetting("Максимум частиц", 200f, 0f, 2000f, 10f, limitParticles::getValue);


    public @NotNull BooleanSetting slowerLight = new BooleanSetting("Реже обновлять свет", false);


    public @NotNull BooleanSetting reduceViewDistance = new BooleanSetting("Уменьшить дальность прорисовки", false);
    public @NotNull NumberSetting viewDistanceLimit = new NumberSetting("Лимит дальности прорисовки", 8f, 2f, 32f, 1f, reduceViewDistance::getValue);


    public @NotNull BooleanSetting reduceSimulationDistance = new BooleanSetting("Уменьшить дальность симуляции", false);
    public @NotNull NumberSetting simulationDistanceLimit = new NumberSetting("Дальность симуляции", 5f, 2f, 32f, 1f, reduceSimulationDistance::getValue);


    public @NotNull BooleanSetting noDistortion = new BooleanSetting("Без эффектов искажения", false);


    public static final AtomicInteger renderedItemsThisFrame = new AtomicInteger(0);

    public static int lightFrameCounter = 0;






    public @NotNull BooleanSetting fpsProtector = new BooleanSetting("Защита ФПС", false);
    public @NotNull NumberSetting fpsProtectThreshold = new NumberSetting("Целевой ФПС", 60f, 20f, 240f, 5f, fpsProtector::getValue);


    public @NotNull BooleanSetting adaptiveRenderDistance = new BooleanSetting("Адаптивная дальность прорисовки", false);
    public @NotNull NumberSetting maxRenderDistance = new NumberSetting("Макс. дальность прорисовки", 8f, 4f, 32f, 1f, adaptiveRenderDistance::getValue);


    // ===== New optimization options =====
    private static Optimization instance;

    public @NotNull BooleanSetting hudOptimize = new BooleanSetting("Optimizaciya HUD", false);
    public @NotNull BooleanSetting hudFastBackground = new BooleanSetting("Ploskiy fon HUD", false, hudOptimize::getValue);
    public @NotNull BooleanSetting hudStaticAnimations = new BooleanSetting("Statichnye animacii HUD", false, hudOptimize::getValue);
    public @NotNull BooleanSetting hudSlowCounters = new BooleanSetting("Redkie obnovleniya schetchikov", false, hudOptimize::getValue);
    public @NotNull BooleanSetting noWeather = new BooleanSetting("Otklyuchit pogodu", false);
    public @NotNull BooleanSetting limitFps = new BooleanSetting("Ogranichit FPS", false);
    public @NotNull NumberSetting fpsLimit = new NumberSetting("Limit FPS", 120f, 30f, 360f, 5f, limitFps::getValue);
    private double baseEntityDistance = 1.0;
    private boolean adaptiveActive = false;

    private boolean lastFocused = true;
    private Field particlesMapField;

    public Optimization() {
        super("Оптимизация", Category.Utility, "Моментально повышает ФПС, убирая лишнюю нагрузку");

        getSettings().add(dynamicFps);
        getSettings().add(backgroundFps);
        getSettings().add(hideEntities);
        getSettings().add(hidePlayers);
        getSettings().add(hideBlockEntities);
        getSettings().add(hideItems);
        getSettings().add(limitParticles);
        getSettings().add(particleLimit);
        getSettings().add(slowerLight);
        getSettings().add(reduceViewDistance);
        getSettings().add(viewDistanceLimit);
        getSettings().add(reduceSimulationDistance);
        getSettings().add(simulationDistanceLimit);
        getSettings().add(noDistortion);
        getSettings().add(fpsProtector);
        getSettings().add(fpsProtectThreshold);
        getSettings().add(adaptiveRenderDistance);
        getSettings().add(maxRenderDistance);
        getSettings().add(hudOptimize);
        getSettings().add(hudFastBackground);
        getSettings().add(hudStaticAnimations);
        getSettings().add(hudSlowCounters);
        getSettings().add(noWeather);
        getSettings().add(limitFps);
        getSettings().add(fpsLimit);
        instance = this;
    }

    @Override
    public void onEnable() {

        super.onEnable();

        if (mc.options == null) return;

        try {

            oldParticles       = mc.options.getParticles().getValue();
            oldEntityDistance  = mc.options.getEntityDistanceScaling().getValue();
            baseEntityDistance = mc.options.getEntityDistanceScaling().getValue();
            oldBiomeBlendRadius= mc.options.getBiomeBlendRadius().getValue();
            oldEntityShadows   = mc.options.getEntityShadows().getValue();
            oldGraphics        = mc.options.getGraphicsMode().getValue();
            oldClouds          = mc.options.getCloudRenderMode().getValue();
            oldAo              = mc.options.getAo().getValue();
            oldBobView         = mc.options.getBobView().getValue();
            oldMipmap          = mc.options.getMipmapLevels().getValue();
            oldMaxFps          = mc.options.getMaxFps().getValue();
            oldVsync           = mc.options.getEnableVsync().getValue();
            oldViewDistance    = mc.options.getViewDistance().getValue();
            oldSimulationDistance = mc.options.getSimulationDistance().getValue();
            oldFovEffectScale  = mc.options.getFovEffectScale().getValue();
            SimpleOption<Double> screenOpt = findScreenEffectOption();
            oldScreenEffectScale = screenOpt != null ? screenOpt.getValue() : 1.0;


            mc.options.getParticles().setValue(ParticlesMode.MINIMAL);
            mc.options.getEntityDistanceScaling().setValue(0.5);
            mc.options.getBiomeBlendRadius().setValue(0);
            mc.options.getEntityShadows().setValue(false);
            mc.options.getGraphicsMode().setValue(GraphicsMode.FAST);
            mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            mc.options.getAo().setValue(false);
            mc.options.getBobView().setValue(false);
            mc.options.getMipmapLevels().setValue(0);
            mc.options.getMaxFps().setValue(getEffectiveMaxFps());
            mc.options.getEnableVsync().setValue(false);


            if (reduceViewDistance.getValue()) {
                int limit = Math.round(viewDistanceLimit.getValue());
                if (mc.options.getViewDistance().getValue() > limit) {
                    mc.options.getViewDistance().setValue(limit);
                }
            }

            if (reduceSimulationDistance.getValue()) {
                int limit = Math.round(simulationDistanceLimit.getValue());
                if (mc.options.getSimulationDistance().getValue() > limit) {
                    mc.options.getSimulationDistance().setValue(limit);
                }
            }

            if (noDistortion.getValue()) {
                // FOV-эффекты (скорость/замедление) — стабильный геттер
                mc.options.getFovEffectScale().setValue(0.0);
                // эффекты экрана (тошнота/портал) — через рефлексию
                if (screenOpt != null) screenOpt.setValue(0.0);
            }


            lastFocused = mc.isWindowFocused();


            clearRenderCaches();


            forceMemoryCleanup(true);
        } catch (Exception e) {
            System.out.println("[Optimization] Ошибка при включении: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {

        super.onDisable();

        if (mc.options == null) return;

        try {
            mc.options.getParticles().setValue(oldParticles);
            mc.options.getEntityDistanceScaling().setValue(oldEntityDistance);
            mc.options.getBiomeBlendRadius().setValue(oldBiomeBlendRadius);
            mc.options.getEntityShadows().setValue(oldEntityShadows);
            mc.options.getGraphicsMode().setValue(oldGraphics);
            mc.options.getCloudRenderMode().setValue(oldClouds);
            mc.options.getAo().setValue(oldAo);
            mc.options.getBobView().setValue(oldBobView);
            mc.options.getMipmapLevels().setValue(oldMipmap);
            mc.options.getMaxFps().setValue(oldMaxFps);
            mc.options.getEnableVsync().setValue(oldVsync);
            mc.options.getViewDistance().setValue(oldViewDistance);
            mc.options.getSimulationDistance().setValue(oldSimulationDistance);
            mc.options.getFovEffectScale().setValue(oldFovEffectScale);
            SimpleOption<Double> screenOpt = findScreenEffectOption();
            if (screenOpt != null) screenOpt.setValue(oldScreenEffectScale);
        } catch (Exception e) {
            System.out.println("[Optimization] Ошибка при выключении: " + e.getMessage());
        }
    }



    @EventHandler
    public void onTick(EventTick event) {
        handleDynamicFps();
        handleParticleLimit();
        handleFpsProtector();
        handleAdaptiveRenderDistance();

        long now = System.currentTimeMillis();
        if (now - lastCleanup < 10_000L) return;
        lastCleanup = now;
        forceMemoryCleanup(false);
    }


    /**
     * Минимальное допустимое значение entityDistanceScaling в ванилле — 0.5.
     * Всё, что меньше, отклоняется SimpleOption и спамит «Illegal option value» в лог.
     */
    private static final double MIN_ENTITY_DISTANCE = 0.5;

    /**
     * Безопасно задаёт масштаб дальности сущностей: клампит в допустимый диапазон
     * и не пишет значение, если оно не поменялось (чтобы не дёргать опцию каждый тик).
     */
    private void setEntityDistance(double value) {
        if (mc.options == null) return;
        double clamped = Math.max(MIN_ENTITY_DISTANCE, Math.min(4.0, value));
        try {
            double current = mc.options.getEntityDistanceScaling().getValue();
            if (Math.abs(current - clamped) < 0.0001) return;
            mc.options.getEntityDistanceScaling().setValue(clamped);
        } catch (Throwable ignored) {}
    }

    private void handleFpsProtector() {
        if (mc.options == null) return;
        if (!fpsProtector.getValue()) {

            if (mc.options.getEntityDistanceScaling().getValue() != baseEntityDistance && !adaptiveRenderDistance.getValue()) {
                setEntityDistance(baseEntityDistance);
            }
            return;
        }

        try {
            int fps = dev.darkvisuals.client.util.math.Counter.getCurrentFPS();
            float target = fpsProtectThreshold.getValue();

            if (fps < target) {
                // минимум ваниллы — 0.5, ниже игра отклоняет значение
                setEntityDistance(0.5);
            } else {
                setEntityDistance(baseEntityDistance);
            }
        } catch (Throwable ignored) {}
    }


    private void handleAdaptiveRenderDistance() {
        if (mc.options == null) return;
        if (!adaptiveRenderDistance.getValue()) return;

        try {
            int fps = dev.darkvisuals.client.util.math.Counter.getCurrentFPS();
            float target = fpsProtectThreshold.getValue();
            int max = Math.round(maxRenderDistance.getValue());

            if (fps < target * 0.7) {

                mc.options.getViewDistance().setValue(Math.max(2, max / 2));
                setEntityDistance(0.5);
            } else if (fps < target) {

                mc.options.getViewDistance().setValue(Math.max(2, (int)(max * 0.7)));
                setEntityDistance(0.6);
            } else {
                mc.options.getViewDistance().setValue(max);
                setEntityDistance(baseEntityDistance);
            }
        } catch (Throwable ignored) {}
    }


    public static Optimization getInstanceSafe() {
        return instance;
    }

    /** Effective FPS cap: user limit if enabled, otherwise 260. */
    public int getEffectiveMaxFps() {
        return limitFps.getValue() ? Math.max(30, Math.round(fpsLimit.getValue())) : 260;
    }

    // HUD optimization flags (queried from render hot paths, no module lookups).
    public static boolean isHudFastBackground() {
        Optimization o = instance;
        return o != null && o.isToggled() && o.hudOptimize.getValue() && o.hudFastBackground.getValue();
    }

    public static boolean isHudStaticAnimations() {
        Optimization o = instance;
        return o != null && o.isToggled() && o.hudOptimize.getValue() && o.hudStaticAnimations.getValue();
    }

    public static boolean isHudSlowCounters() {
        Optimization o = instance;
        return o != null && o.isToggled() && o.hudOptimize.getValue() && o.hudSlowCounters.getValue();
    }

    public static boolean isWeatherHidden() {
        Optimization o = instance;
        return o != null && o.isToggled() && o.noWeather.getValue();
    }

    public boolean isFpsProtectorActive() { return isToggled() && fpsProtector.getValue(); }

    /**
     * Опция «эффектов экрана» (тошнота/портал). Имя в mappings менялось между
     * версиями (screenEffectScale / nauseaEffectScale), поэтому ищем поле
     * рефлексией по шаблону «effect» + «screen|nausea» (fullscreen исключён).
     */
    @SuppressWarnings("unchecked")
    private SimpleOption<Double> findScreenEffectOption() {
        if (screenEffectOption != null) return screenEffectOption;
        try {
            for (Field f : net.minecraft.client.option.GameOptions.class.getDeclaredFields()) {
                if (!SimpleOption.class.isAssignableFrom(f.getType())) continue;
                String n = f.getName().toLowerCase(java.util.Locale.ROOT);
                if (!n.contains("effect")) continue;
                if (!n.contains("screen") && !n.contains("nausea")) continue;
                f.setAccessible(true);
                Object value = f.get(mc.options);
                if (value instanceof SimpleOption<?> option) {
                    screenEffectOption = (SimpleOption<Double>) option;
                    return screenEffectOption;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }


    private void handleDynamicFps() {
        if (mc.options == null) return;
        if (!dynamicFps.getValue()) return;

        boolean focused = mc.isWindowFocused();
        if (focused == lastFocused) return;
        lastFocused = focused;

        try {
            if (focused) {
                mc.options.getMaxFps().setValue(getEffectiveMaxFps());
            } else {
                int bg = Math.max(1, Math.round(backgroundFps.getValue()));
                mc.options.getMaxFps().setValue(bg);
            }
        } catch (Throwable ignored) {}
    }


    private void handleParticleLimit() {
        renderedItemsThisFrame.set(0);

        if (!limitParticles.getValue()) return;
        int limit = Math.round(particleLimit.getValue());

        if (mc.particleManager == null) return;

        try {
            if (particlesMapField == null) {
                for (Field f : mc.particleManager.getClass().getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        particlesMapField = f;
                        break;
                    }
                }
            }
            if (particlesMapField == null) return;

            Object mapObj = particlesMapField.get(mc.particleManager);
            if (!(mapObj instanceof Map<?, ?> map)) return;

            int total = 0;
            for (Object value : map.values()) {
                if (value instanceof Collection<?> collection) total += collection.size();
            }

            int toRemove = total - limit;
            if (toRemove <= 0) return;

            for (Object value : map.values()) {
                if (toRemove <= 0) break;

                if (value instanceof Queue<?> queue) {
                    while (toRemove > 0 && !queue.isEmpty()) {
                        queue.poll();
                        toRemove--;
                    }
                } else if (value instanceof Collection<?> collection) {
                    Iterator<?> it = collection.iterator();
                    while (toRemove > 0 && it.hasNext()) {
                        it.next();
                        it.remove();
                        toRemove--;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }


    public boolean tryConsumeItemBudget() {
        if (!limitParticles.getValue()) return true;
        int limit = Math.round(particleLimit.getValue());
        return renderedItemsThisFrame.getAndIncrement() < limit;
    }



    private void clearRenderCaches() {
        try {
            if (mc.particleManager != null && mc.world != null) {

                mc.particleManager.setWorld(mc.world);
            }
        } catch (Throwable ignored) {}
    }



    private void forceMemoryCleanup(boolean force) {
        try {
            Runtime rt = Runtime.getRuntime();
            long free = rt.freeMemory();
            long total = rt.totalMemory();
            double freeRatio = (double) free / (double) total;
            if (force || freeRatio < 0.20) {
                System.gc();
            }
        } catch (Throwable ignored) {}
    }
}
