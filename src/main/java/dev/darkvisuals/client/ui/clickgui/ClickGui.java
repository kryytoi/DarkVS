package dev.darkvisuals.client.ui.clickgui;

import dev.darkvisuals.client.util.animations.SmoothAnimation;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Screen {

    private final ClickGuiState state;
    private final ClickGuiRenderer renderer;
    private final ClickGuiInputHandler inputHandler;

      
    private final SmoothAnimation openAnimation = new SmoothAnimation(0f, 14f);
    private boolean closing = false;

    public ClickGui() {
        super(Text.of("darkvisuals-clickgui"));
        this.state = new ClickGuiState();
        this.renderer = new ClickGuiRenderer();
        this.inputHandler = new ClickGuiInputHandler(this.state);
    }

    @Override
    protected void init() {
        super.init();
         
        if (!closing) {
            openAnimation.setTarget(1f);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        float progress = openAnimation.update();
        renderer.render(context, mouseX, mouseY, delta, this.client.getWindow(), state, progress);

         
        if (closing && progress <= 0.02f) {
            closing = false;
            openAnimation.snapTo(0f);
            this.client.setScreen(null);
        }
    }

      
    private boolean inputLocked() {
        return closing || openAnimation.getValue() < 0.85f;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inputLocked()) return true;
        if (inputHandler.mouseClicked(mouseX, mouseY, button, this.client.getWindow())) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (inputHandler.mouseReleased(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (inputLocked()) return true;
        if (inputHandler.mouseDragged(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (inputLocked()) return true;
        if (inputHandler.mouseScrolled(mouseX, mouseY, vertical)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         
        if (ClickGuiRenderer.bindingSetting != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                ClickGuiRenderer.bindingSetting.setValue(new dev.darkvisuals.modules.settings.api.Bind(GLFW.GLFW_KEY_UNKNOWN, false));
            } else {
                ClickGuiRenderer.bindingSetting.setValue(new dev.darkvisuals.modules.settings.api.Bind(keyCode, false));
            }
            ClickGuiRenderer.bindingSetting = null;
            return true;
        }

         
        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                && ClickGuiRenderer.openedSettingsModule == null
                && ClickGuiRenderer.editingStringSetting == null) {
            this.close();
            return true;
        }
        if (inputHandler.keyPressed(keyCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (inputHandler.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }

      
    @Override
    public void close() {
        if (!closing) {
            closing = true;
            openAnimation.setTarget(0f);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public void setDescription(String text) {
         
    }
}
