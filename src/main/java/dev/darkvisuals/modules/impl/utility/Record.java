package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventKey;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.RecordManager;
import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.impl.BindSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;


public class Record extends Module {

    private final RecordManager recordManager = new RecordManager();

    private final BindSetting startBind = new BindSetting("Начать запись", new Bind(-1, false));
    private final BindSetting pauseResumeBind = new BindSetting("Пауза / Продолжить", new Bind(-1, false));
    private final BindSetting stopBind = new BindSetting("Завершить запись", new Bind(-1, false));

     
    private boolean startLatch, pauseLatch, stopLatch;

    public Record() {
        super("Record", Category.Utility, "Запись экрана майнкрафта.");
        getSettings().add(startBind);
        getSettings().add(pauseResumeBind);
        getSettings().add(stopBind);
    }

    @Override
    public void onDisable() {
        super.onDisable();
         
        if (recordManager.isActive()) recordManager.stop();
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (!isToggled() || fullNullCheck()) return;
        if (mc.currentScreen != null) return;

        Bind start = startBind.getValue();
        Bind pauseResume = pauseResumeBind.getValue();
        Bind stop = stopBind.getValue();

        if (matches(start, e)) {
            if (e.getAction() == GLFW.GLFW_PRESS && !startLatch) {
                startLatch = true;
                handleStart();
            } else if (e.getAction() == GLFW.GLFW_RELEASE) {
                startLatch = false;
            }
        }

        if (matches(pauseResume, e)) {
            if (e.getAction() == GLFW.GLFW_PRESS && !pauseLatch) {
                pauseLatch = true;
                handlePauseResume();
            } else if (e.getAction() == GLFW.GLFW_RELEASE) {
                pauseLatch = false;
            }
        }

        if (matches(stop, e)) {
            if (e.getAction() == GLFW.GLFW_PRESS && !stopLatch) {
                stopLatch = true;
                handleStop();
            } else if (e.getAction() == GLFW.GLFW_RELEASE) {
                stopLatch = false;
            }
        }
    }

    private boolean matches(Bind bind, EventKey e) {
        return bind != null && !bind.isEmpty() && !bind.isMouse() && bind.getKey() == e.getKey();
    }

    private void handleStart() {
        if (recordManager.isActive()) return;  
        boolean ok = recordManager.start();
        String msg = ok ? "Запись начата" : "Не удалось начать запись (проверь, установлен ли FFmpeg)";
        darkvisuals.getInstance().getNotifyManager().add(new Notify(
                ok ? NotifyIcons.successIcon : NotifyIcons.failIcon, msg, 1500));
    }

    private void handlePauseResume() {
        if (recordManager.isRecording()) {
            recordManager.pause();
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, "Запись на паузе", 1200));
        } else if (recordManager.isPaused()) {
            recordManager.resume();
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, "Запись продолжена", 1200));
        }
    }

    private void handleStop() {
        if (!recordManager.isActive()) return;
        recordManager.stop();
        darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, "Запись сохранена", 1500));
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (!isToggled()) return;

         
        recordManager.captureFrame();

        if (!recordManager.isActive()) return;  

        DrawContext context = e.getContext();
        boolean recording = recordManager.isRecording();
        int color = recording ? 0xFF00FF00 : 0xFFFF3333;  
        String label = recording ? "● ЗАПИСЬ" : "● ПАУЗА";
        String time = formatTime(recordManager.getElapsedMs());
        String text = label + "  " + time;

        int x = context.getScaledWindowWidth() / 2 - mc.textRenderer.getWidth(text) / 2;
        int y = 8;

        context.drawTextWithShadow(mc.textRenderer, text, x, y, color);
    }

    private String formatTime(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        return h > 0
                ? String.format("%02d:%02d:%02d", h, m, s)
                : String.format("%02d:%02d", m, s);
    }
}