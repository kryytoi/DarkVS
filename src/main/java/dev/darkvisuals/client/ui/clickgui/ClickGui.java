package dev.darkvisuals.client.ui.clickgui;

import dev.darkvisuals.client.util.animations.SmoothAnimation;
import dev.darkvisuals.modules.impl.render.UI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Screen {

    private final ClickGuiState state;
    private final ClickGuiRenderer renderer;
    private final NewClickGuiRenderer newRenderer;
    private final NounClickGuiRenderer nounRenderer;
    private final ClickGuiInputHandler inputHandler;

    private final SmoothAnimation openAnimation = new SmoothAnimation(0f, 14f);
    private boolean closing = false;

    public ClickGui() {
        super(Text.of("darkvisuals-clickgui"));
        this.state = new ClickGuiState();
        this.renderer = new ClickGuiRenderer();
        this.newRenderer = NewClickGuiRenderer.getInstance();
        this.nounRenderer = NounClickGuiRenderer.getInstance();
        this.inputHandler = new ClickGuiInputHandler(this.state);
    }

    private UI.GuiStyle getGuiStyle() {
        var ui = dev.darkvisuals.darkvisuals.getInstance()
                .getModuleManager()
                .getModule(UI.class);
        return ui == null ? UI.GuiStyle.Old : ui.getGuiStyle();
    }

    private boolean isNewStyle() {
        return getGuiStyle() == UI.GuiStyle.New;
    }

    private boolean isNounStyle() {
        return getGuiStyle() == UI.GuiStyle.Noun;
    }

    private boolean isAltStyle() {
        return isNewStyle() || isNounStyle();
    }

    private boolean isAltStyleInputFocused() {
        if (isNewStyle()) return newRenderer.isKeyOrInputFocused();
        if (isNounStyle()) return nounRenderer.isKeyOrInputFocused();
        return false;
    }

    @Override
    protected void init() {
        super.init();

        if (!closing) {
            openAnimation.setTarget(1f);
            if (isAltStyle()) ClickGuiRenderer.openedSettingsModule = null;
            if (isNounStyle()) {
                NounClickGuiRenderer.playUiSound(true);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        Window window = this.client != null ? this.client.getWindow() : MinecraftClient.getInstance().getWindow();
        float progress = openAnimation.update();

        if (isNewStyle()) {
            newRenderer.render(context, mouseX, mouseY, delta, window, state, progress);
        } else if (isNounStyle()) {
            nounRenderer.render(context, mouseX, mouseY, delta, window, state, progress);
        } else {
            renderer.render(context, mouseX, mouseY, delta, window, state, progress);
        }

        if (closing && progress <= 0.02f) {
            closing = false;
            openAnimation.snapTo(0f);
            if (this.client != null) {
                this.client.setScreen(null);
            }
        }
    }

    private boolean inputLocked() {
        return closing || openAnimation.getValue() < 0.85f;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inputLocked()) return true;

        Window window = this.client != null ? this.client.getWindow() : MinecraftClient.getInstance().getWindow();

        if (isNounStyle()) {
            if (nounRenderer.mouseClicked(mouseX, mouseY, button, window, state)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (inputHandler.mouseClicked(mouseX, mouseY, button, window)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isNounStyle()) {
            if (nounRenderer.mouseReleased(mouseX, mouseY, button)) return true;
            return super.mouseReleased(mouseX, mouseY, button);
        }

        if (inputHandler.mouseReleased(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (inputLocked()) return true;

        if (isNounStyle()) {
            if (nounRenderer.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        if (inputHandler.mouseDragged(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (inputLocked()) return true;

        Window window = this.client != null ? this.client.getWindow() : MinecraftClient.getInstance().getWindow();

        if (isNounStyle()) {
            if (nounRenderer.scroll(mouseX, mouseY, vertical, window, state)) return true;
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }

        if (inputHandler.mouseScrolled(mouseX, mouseY, vertical)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClickGuiRenderer.bindingSetting != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                ClickGuiRenderer.bindingSetting.setValue(
                        new dev.darkvisuals.modules.settings.api.Bind(GLFW.GLFW_KEY_UNKNOWN, false)
                );
            } else {
                ClickGuiRenderer.bindingSetting.setValue(
                        new dev.darkvisuals.modules.settings.api.Bind(keyCode, false)
                );
            }
            ClickGuiRenderer.bindingSetting = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                && ClickGuiRenderer.openedSettingsModule == null
                && ClickGuiRenderer.editingStringSetting == null
                && !isAltStyleInputFocused()) {
            this.close();
            return true;
        }

        if (isNounStyle()) {
            if (nounRenderer.handleKeyPressed(keyCode, modifiers)) return true;
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (inputHandler.keyPressed(keyCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (isNounStyle()) {
            if (nounRenderer.handleCharTyped(chr)) return true;
            return super.charTyped(chr, modifiers);
        }

        if (inputHandler.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            openAnimation.setTarget(0f);
            if (isNounStyle()) {
                NounClickGuiRenderer.playUiSound(false);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public void setDescription(String text) {
    }
}