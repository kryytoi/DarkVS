package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventKey;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.impl.BindSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.glfw.GLFW;

 
public class Macros extends Module {

    public static final int MAX_MACROS = 50;

    private final BindSetting[] binds = new BindSetting[MAX_MACROS];
    private final StringSetting[] messages = new StringSetting[MAX_MACROS];
    private final boolean[] latch = new boolean[MAX_MACROS];  

    public Macros() {
        super("Macros", Category.Utility, "Пишет команду по бинду");

        for (int i = 0; i < MAX_MACROS; i++) {
            int slot = i + 1;
            BindSetting bind = new BindSetting("Макрос #" + slot + " бинд", new Bind(-1, false));
            StringSetting message = new StringSetting("Макрос #" + slot + " Команда", "", false);

            binds[i] = bind;
            messages[i] = message;

            getSettings().add(bind);
            getSettings().add(message);
        }
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (!isToggled() || fullNullCheck()) return;
        if (mc.currentScreen != null) return;  

        for (int i = 0; i < MAX_MACROS; i++) {
            Bind bind = binds[i].getValue();
            if (bind == null || bind.isEmpty() || bind.isMouse()) continue;
            if (bind.getKey() != e.getKey()) continue;

            if (e.getAction() == GLFW.GLFW_PRESS) {
                if (!latch[i]) {
                    latch[i] = true;
                    execute(messages[i].getValue());
                }
            } else if (e.getAction() == GLFW.GLFW_RELEASE) {
                latch[i] = false;
            }
        }
    }

 
    private void execute(String raw) {
        if (raw == null) return;
        String text = raw.trim();
        if (text.isEmpty()) return;
        if (mc.player == null || mc.player.networkHandler == null) return;

        if (text.startsWith("/")) {
            mc.player.networkHandler.sendChatCommand(text.substring(1));
        } else {
            mc.player.networkHandler.sendChatMessage(text);
        }
    }
}