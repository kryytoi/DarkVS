package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.managers.AutoSaveManager;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

 public class ViewModel extends Module implements Wrapper {

     
    public NumberSetting mainX = new NumberSetting("setting.mainX", 0f, -2f, 2f, 0.01f, () -> false);
    public NumberSetting mainY = new NumberSetting("setting.mainY", 0f, -2f, 2f, 0.01f, () -> false);
    public NumberSetting mainZ = new NumberSetting("setting.mainZ", 0f, -2f, 2f, 0.01f, () -> false);
    public NumberSetting offX = new NumberSetting("setting.offX", 0f, -2f, 2f, 0.01f, () -> false);
    public NumberSetting offY = new NumberSetting("setting.offY", 0f, -2f, 2f, 0.01f, () -> false);
    public NumberSetting offZ = new NumberSetting("setting.offZ", 0f, -2f, 2f, 0.01f, () -> false);

     
    private final ButtonSetting configureButton = new ButtonSetting("Настроить", this::openConfig);

     
    public enum EditedHand { MAIN, OFF }

    private boolean configuring;
    private EditedHand editedHand = EditedHand.MAIN;

    private boolean draggingLeft;
    private boolean draggingRight;
    private double lastCursorX, lastCursorY;
    private boolean cursorInitialized;

     
    private static final double DRAG_SENSITIVITY = 0.0035;
    private static final float SCROLL_SENSITIVITY = 0.03f;

    public ViewModel() {
        super("ViewModel", Category.Render, I18n.translate("module.viewmodel.description"));
        getSettings().add(mainX);
        getSettings().add(mainY);
        getSettings().add(mainZ);
        getSettings().add(offX);
        getSettings().add(offY);
        getSettings().add(offZ);
        getSettings().add(configureButton);

         
         
         
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (configuring) suppressMovementInput();
        });
    }

     
     
     

      
    public void openConfig() {
        if (configuring) return;
        configuring = true;
        editedHand = EditedHand.MAIN;  
        draggingLeft = false;
        draggingRight = false;
        cursorInitialized = false;

         
        if (mc.currentScreen != null) mc.setScreen(null);
        mc.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);

         
         
         
        mc.mouse.unlockCursor();

        darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon,
                "Настройка руки: ЛКМ по руке — выбрать и тащить, колесо — Z, ESC — выход", 2500));
    }

      
    public void closeConfig() {
        if (!configuring) return;
        configuring = false;
        draggingLeft = false;
        draggingRight = false;

         
        mc.mouse.lockCursor();

        saveToConfig();

        darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon,
                "Позиция руки сохранена", 1500));
    }

    private void saveToConfig() {
        try {
            AutoSaveManager asm = darkvisuals.getInstance().getAutoSaveManager();
            if (asm != null) asm.scheduleAutoSave();
        } catch (Throwable ignored) {}
    }

    public boolean isConfiguring() {
        return configuring;
    }

    public EditedHand getEditedHand() {
        return editedHand;
    }

    public void setEditedHand(EditedHand hand) {
        this.editedHand = hand;
    }

    public void toggleEditedHand() {
        editedHand = (editedHand == EditedHand.MAIN) ? EditedHand.OFF : EditedHand.MAIN;
        darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon,
                editedHand == EditedHand.MAIN ? "Редактируется основная рука" : "Редактируется дополнительная рука", 1200));
    }

     
     
     

    private void suppressMovementInput() {
        GameOptions o = mc.options;
        if (o == null) return;
        o.forwardKey.setPressed(false);
        o.backKey.setPressed(false);
        o.leftKey.setPressed(false);
        o.rightKey.setPressed(false);
        o.jumpKey.setPressed(false);
        o.sprintKey.setPressed(false);
        o.sneakKey.setPressed(false);
        o.attackKey.setPressed(false);
        o.useKey.setPressed(false);
    }

      
    public boolean isBlockedMovementKey(int key, int scancode) {
        GameOptions o = mc.options;
        if (o == null) return false;
        return o.forwardKey.matchesKey(key, scancode)
                || o.backKey.matchesKey(key, scancode)
                || o.leftKey.matchesKey(key, scancode)
                || o.rightKey.matchesKey(key, scancode)
                || o.jumpKey.matchesKey(key, scancode)
                || o.sprintKey.matchesKey(key, scancode)
                || o.attackKey.matchesKey(key, scancode)
                || o.useKey.matchesKey(key, scancode);
    }

     
     
     

      
    public void handleMouseButton(int button, int action) {
        if (!configuring) return;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
        boolean pressed = action == GLFW.GLFW_PRESS;

         
         
         
        if (pressed) {
            editedHand = handAtCursor();
            cursorInitialized = false;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) draggingLeft = pressed;
        else draggingRight = pressed;
    }

     private EditedHand handAtCursor() {
        if (mc.player == null) return editedHand;
        double screenHalf = mc.getWindow().getWidth() / 2.0;
        boolean cursorOnLeftHalf = lastCursorX < screenHalf;
        Arm mainArm = mc.player.getMainArm();
        boolean mainHandOnLeftHalf = (mainArm == Arm.LEFT);
        return (cursorOnLeftHalf == mainHandOnLeftHalf) ? EditedHand.MAIN : EditedHand.OFF;
    }

      
    public boolean handleCursorPos(double x, double y) {
        if (!configuring) return false;

        if (!cursorInitialized) {
            lastCursorX = x;
            lastCursorY = y;
            cursorInitialized = true;
            return true;
        }

        double dx = x - lastCursorX;
        double dy = y - lastCursorY;
        lastCursorX = x;
        lastCursorY = y;

        if (draggingLeft || draggingRight) {
            NumberSetting xSetting = editedHand == EditedHand.MAIN ? mainX : offX;
            NumberSetting ySetting = editedHand == EditedHand.MAIN ? mainY : offY;

             
             
             
            boolean isMainHand = editedHand == EditedHand.MAIN;
            Arm arm = (mc.player == null) ? Arm.RIGHT
                    : (isMainHand ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite());
            double signedDx = (arm == Arm.LEFT) ? -dx : dx;

            float newX = (float) (xSetting.getValue() + signedDx * DRAG_SENSITIVITY);
            float newY = (float) (ySetting.getValue() - dy * DRAG_SENSITIVITY);

            xSetting.setValue(MathHelper.clamp(newX, xSetting.getMin(), xSetting.getMax()));
            ySetting.setValue(MathHelper.clamp(newY, ySetting.getMin(), ySetting.getMax()));
        }

         
        return true;
    }

      
    public boolean handleScroll(double verticalAmount) {
        if (!configuring) return false;

        NumberSetting zSetting = editedHand == EditedHand.MAIN ? mainZ : offZ;
        float newZ = (float) (zSetting.getValue() + verticalAmount * SCROLL_SENSITIVITY);
        zSetting.setValue(MathHelper.clamp(newZ, zSetting.getMin(), zSetting.getMax()));
        return true;
    }
}