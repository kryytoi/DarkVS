package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.ui.browser.BrowserCapture;
import dev.darkvisuals.client.ui.browser.BrowserScreen;
import dev.darkvisuals.client.util.renderer.Browser3DRenderer;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.api.Nameable;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public class BrowserModule extends Module {

    public enum BrowserApp implements Nameable {
        Auto("Авто"),
        Chrome("Chrome"),
        Edge("Edge"),
        Firefox("Firefox"),
        Brave("Brave"),
        Opera("Opera"),
        Vivaldi("Vivaldi"),
        Yandex("Yandex");

        private final String name;

        BrowserApp(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        public String[] exeNames() {
            return switch (this) {
                case Chrome -> new String[]{"chrome.exe"};
                case Edge -> new String[]{"msedge.exe"};
                case Firefox -> new String[]{"firefox.exe"};
                case Brave -> new String[]{"brave.exe"};
                case Opera -> new String[]{"opera.exe"};
                case Vivaldi -> new String[]{"vivaldi.exe"};
                case Yandex -> new String[]{"browser.exe", "yandex.exe"};
                case Auto -> new String[]{
                        "chrome.exe", "msedge.exe", "firefox.exe", "brave.exe",
                        "opera.exe", "vivaldi.exe", "browser.exe", "yandex.exe", "chromium.exe"};
            };
        }
    }

    @Getter private final Browser3DRenderer renderer = new Browser3DRenderer();
    @Getter private final BrowserCapture capture = new BrowserCapture();

    private final NumberSetting distance = new NumberSetting(
            "Дистанция", 2.5f, 1.0f, 5.0f, 0.1f);
    private final NumberSetting followSpeed = new NumberSetting(
            "Скорость следования", 0.35f, 0.05f, 1.0f, 0.05f);
    private final NumberSetting screenWidth = new NumberSetting(
            "Ширина экрана", 4.0f, 1.5f, 8.0f, 0.25f);
    private final EnumSetting<BrowserApp> browser = new EnumSetting<>(
            "Браузер", BrowserApp.Auto);

    public BrowserModule() {
        super("Browser", Category.Utility, "Экран реального браузера на 3D-плоскости");
        setBind(new Bind(GLFW.GLFW_KEY_B, false));
        getSettings().add(distance);
        getSettings().add(followSpeed);
        getSettings().add(screenWidth);
        getSettings().add(browser);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (mc.player == null || mc.world == null) {
            setToggled(false);
            return;
        }

        renderer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.currentScreen instanceof BrowserScreen) {
            mc.setScreen(null);
        }
        capture.reset();
    }

    @Override
    public void toggle() {
        if (mc.currentScreen instanceof BrowserScreen) {

            mc.setScreen(null);
            return;
        }

        if (!isToggled()) {
            setToggled(true);
        }

        if (mc.currentScreen == null && !fullNullCheck()) {
            mc.setScreen(new BrowserScreen(this));
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        renderer.update(mc.player, distance.getValue(), followSpeed.getValue());
        capture.tick(Set.of(browser.getValue().exeNames()));
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        renderer.render(event.getMatrices(), event.getTickDelta(), capture, screenWidth.getValue());
    }
}
