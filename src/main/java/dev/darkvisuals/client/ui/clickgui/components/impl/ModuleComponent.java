package dev.darkvisuals.client.ui.clickgui.components.impl;

import dev.darkvisuals.modules.settings.impl.*;
import dev.darkvisuals.client.ui.clickgui.components.Component;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.client.util.animations.Animation;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.math.MathUtils;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.client.util.renderer.Render2D;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import dev.darkvisuals.client.managers.ThemeManager;
import net.minecraft.client.resource.language.I18n;

public class ModuleComponent extends Component {

    private final Module module;
    private boolean open;
    private boolean binding;
    private boolean showBindModeMenu;
    private boolean renderExternally;

     
    private float bindTextX, bindTextY, bindTextW, bindTextH;

    @Getter private final List<Component> components = new ArrayList<>();

    private final Animation hoverAnim   = new Animation(200, 1f, false, Easing.BOTH_SINE);
    private final Animation toggleAnim  = new Animation(200, 1f, false, Easing.BOTH_SINE);
    @Getter private final Animation openAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private final Animation bindMenuAnimation = new Animation(200, 1f, false, Easing.BOTH_SINE);
    private boolean toggleAnimInitialized;
    private boolean lastToggleState;
    private boolean isToggleAnimating;
    private boolean hoverAnimInitialized;

    private static final float HEADER_HEIGHT = 24f;
    private static final float CHILD_HEIGHT  = 20f;

    public ModuleComponent(Module module) {
        super(module.getName());
        this.module = module;
        this.lastToggleState = module.isToggled();

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) components.add(new BooleanComponent((BooleanSetting) setting));
            else if (setting instanceof NumberSetting) components.add(new SliderComponent((NumberSetting) setting));
            else if (setting instanceof EnumSetting) components.add(new EnumComponent((EnumSetting<?>) setting));
            else if (setting instanceof StringSetting) components.add(new StringComponent((StringSetting) setting));
            else if (setting instanceof ListSetting) components.add(new ListComponent((ListSetting) setting));
            else if (setting instanceof BindSetting) components.add(new BindComponent((BindSetting) setting));
            else if (setting instanceof dev.darkvisuals.modules.settings.impl.ButtonSetting)
                components.add(new ButtonComponent((dev.darkvisuals.modules.settings.impl.ButtonSetting) setting));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = MathUtils.isHovered(x, y, width, HEADER_HEIGHT, mouseX, mouseY);
         
        if (hovered && mc.currentScreen instanceof dev.darkvisuals.client.ui.clickgui.ClickGui clickGui) {
            clickGui.setDescription(module.getDescription());
        }
         
        if (!hoverAnimInitialized) {
            hoverAnim.setDuration(0);
            hoverAnim.update(hovered);
            hoverAnim.setDuration(200);
            hoverAnimInitialized = true;
        } else {
            hoverAnim.update(hovered);
        }
         
        boolean currentState = module.isToggled();
        if (!toggleAnimInitialized) {
            toggleAnim.setDuration(0);
            toggleAnim.update(currentState);
            toggleAnim.setDuration(200);
            toggleAnimInitialized = true;
            lastToggleState = currentState;
            isToggleAnimating = false;
        } else if (currentState != lastToggleState) {
            toggleAnim.setDuration(200);
            toggleAnim.update(currentState);
            lastToggleState = currentState;
            isToggleAnimating = true;
        } else if (isToggleAnimating && (toggleAnim.finished(currentState))) {
            isToggleAnimating = false;
        }
        openAnimation.update(open);
        bindMenuAnimation.update(showBindModeMenu);

         
        Color base = new Color(67, 67, 67, (int) (120 * Math.max(0f, Math.min(1f, globalAlpha))));
        Color themeAccent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        Color active = new Color(themeAccent.getRed(), themeAccent.getGreen(), themeAccent.getBlue(), 180);
        Color bg = base;

        float totalHeight = getHeight();
        Render2D.drawRoundedRect(context.getMatrices(), x, y, width, totalHeight, 5f, bg);


        Color textColor = new Color(255, 255, 255, (int) ((200 + 55 * hoverAnim.getValue()) * Math.max(0f, Math.min(1f, globalAlpha))));
        float titleFontSize = 8f;
        float titleFontH = Fonts.REGULAR.getHeight(titleFontSize);
        float titleY = y + (HEADER_HEIGHT - titleFontH) / 2f;
        String localizedModuleName = I18n.translate(module.getName());
        Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(titleFontSize),
                localizedModuleName, x + 8f, titleY, textColor);

         
        String bindText = "";
        if (binding) bindText = "Press key...";
        else if (module.getBind() != null && module.getBind().getKey() != -1)
            bindText = module.getBind().toString().replace("_", " ");

         
        float bindFontSize = 6f;
        bindTextW = bindText.isEmpty() ? 0f : Fonts.REGULAR.getWidth(bindText, bindFontSize);
        bindTextH = Fonts.REGULAR.getHeight(bindFontSize);
         
         
        bindTextX = Float.NaN;
        bindTextY = Float.NaN;

         
         
        if (!toggleAnimInitialized) {
            toggleAnim.setDuration(0);
            toggleAnim.update(module.isToggled());
             
            toggleAnim.setDuration(200);
            toggleAnimInitialized = true;
        }

                 
        float switchW = 20f;
        float switchH = 10f;
        float reservedRight = 0f;  
        float switchX = x + width - reservedRight - 24f;  
        float switchY = y + (HEADER_HEIGHT - switchH) / 2f;
        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        float progress = isToggleAnimating ? (float) toggleAnim.getValue() : (currentState ? 1f : 0f);
        Render2D.drawRoundedRect(context.getMatrices(), switchX, switchY, switchW * progress, switchH, 4f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (255 * progress * Math.max(0f, Math.min(1f, globalAlpha)))));
        Render2D.drawRoundedRect(context.getMatrices(), switchX + (switchW * progress), switchY,
                switchW * (1f - progress), switchH, 3f, new Color(23, 23, 23, (int) (100 * Math.max(0f, Math.min(1f, globalAlpha)))));
        float thumbW = 8f;
        float thumbH = 8f;
         
        float padding = 1.5f;
        float thumbX = switchX + padding + (switchW - thumbW - 2f * padding) * progress;
        float thumbY = switchY + padding + (switchH - thumbH - 2f * padding) / 2f;
        Render2D.drawRoundedRect(context.getMatrices(), thumbX, thumbY, thumbW, thumbH, 3f, Color.WHITE);

         
        if (open && !renderExternally) {
            float childY = y + HEADER_HEIGHT;
            float visibleH = getChildrenFullHeight();

             
            context.enableScissor((int) x, (int) (y + HEADER_HEIGHT),
                    (int) (x + width), (int) (y + HEADER_HEIGHT + visibleH));

            for (Component component : components) {
                if (!component.getVisible().get()) continue;
                component.setX(x + 5f);
                component.setY(childY);
                component.setWidth(width - 10f);
                component.setHeight(CHILD_HEIGHT);
                component.setGlobalAlpha(globalAlpha);
                component.render(context, mouseX, mouseY, delta);
                childY += component.getHeight() + component.getAddHeight().get();
            }

            context.disableScissor();
        }

         
        if (!bindText.isEmpty()) {
            float totalHeightAfter = getHeight();
            bindTextX = x + 8f;
            bindTextY = y + totalHeightAfter - bindTextH - 1f;
            Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(bindFontSize),
                    bindText, bindTextX, bindTextY,
                    new Color(200, 200, 200, (int) (180 * Math.max(0f, Math.min(1f, globalAlpha)))));
        }

         
        if (showBindModeMenu) {
            float itemW = 80f;
            float itemH = 15f;  

             
            float desiredX = (Float.isNaN(bindTextX) ? (x + width - 90f) : (bindTextX + bindTextW + 6f));
            float menuX = Math.min(desiredX, x + width - itemW - 6f);
             
            if (!Float.isNaN(bindTextX) && menuX < bindTextX) {
                float leftCandidate = bindTextX - itemW - 6f;
                if (leftCandidate >= x + 6f) menuX = leftCandidate;
            }
            float menuYBase = (Float.isNaN(bindTextY) ? (y + HEADER_HEIGHT + 2f) : (bindTextY - (itemH * 2f + 7f) - 4f));

             
            float a = (float) Math.max(0f, Math.min(1f, bindMenuAnimation.getValue()));
            float slideOffset = (1f - a) * 6f;
            float menuY = menuYBase + slideOffset;

            Color menuBg = new Color(20, 20, 20, (int) (220 * a * Math.max(0f, Math.min(1f, globalAlpha))));
            Render2D.drawRoundedRect(context.getMatrices(), menuX, menuY, itemW, itemH * 2 + 7f, 3f, menuBg);

            String opt1 = "Toggle";
            String opt2 = "Hold";

            boolean isToggle = module.getBind() != null && module.getBind().getMode() == Bind.Mode.TOGGLE;
            boolean isHold = module.getBind() != null && module.getBind().getMode() == Bind.Mode.HOLD;

            int textAlpha = (int) (255 * a * Math.max(0f, Math.min(1f, globalAlpha)));
             
            Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(7f), opt1, menuX + 18f, menuY + 5f, new Color(255, 255, 255, textAlpha));
            Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(10f), "D", menuX + 6f, menuY + 5.5f,
                    new Color(0, 0, 0, (int) ((isToggle ? 255 : 0) * a * Math.max(0f, Math.min(1f, globalAlpha)))));

             
            Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(7f), opt2, menuX + 18f, menuY + 5f + itemH, new Color(255, 255, 255, textAlpha));
            Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(10f), "D", menuX + 6f, menuY + 5.5f + itemH,
                    new Color(0, 0, 0, (int) ((isHold ? 255 : 0) * a * Math.max(0f, Math.min(1f, globalAlpha)))));
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean onHeader = MathUtils.isHovered(x, y, width, HEADER_HEIGHT, (float) mouseX, (float) mouseY);
        boolean onBindText = (!Float.isNaN(bindTextX)) && MathUtils.isHovered(bindTextX, bindTextY - 1f, bindTextW, bindTextH + 2f, (float) mouseX, (float) mouseY);

         
        if (button == 0 && onBindText && !binding) {
            if (module.getBind() != null && !module.getBind().isEmpty()) {
                showBindModeMenu = !showBindModeMenu;
                return;
            }
        }

        if (onHeader) {
            if (button == 0 && !binding) {  
                module.toggle();
                return;
            } else if (button == 1 && !components.isEmpty() && !binding && !renderExternally) {  
                open = !open;
                return;
            } else if (button == 2 && !binding) {  
                binding = true;
                return;
            }
        }

         
        if (showBindModeMenu) {
            float itemW = 80f;
            float itemH = 15f;  

            float desiredX = (Float.isNaN(bindTextX) ? (x + width - 90f) : (bindTextX + bindTextW + 6f));
            float menuX = Math.min(desiredX, x + width - itemW - 6f);
            if (!Float.isNaN(bindTextX) && menuX < bindTextX) {
                float leftCandidate = bindTextX - itemW - 6f;
                if (leftCandidate >= x + 6f) menuX = leftCandidate;
            }
            float menuY = (Float.isNaN(bindTextY) ? (y + HEADER_HEIGHT + 2f) : (bindTextY - (itemH * 2f + 7f) - 4f));

            if (MathUtils.isHovered(menuX, menuY, itemW, itemH, (float) mouseX, (float) mouseY)) {
                module.getBind().setMode(Bind.Mode.TOGGLE);
                showBindModeMenu = false;
                return;
            }
            if (MathUtils.isHovered(menuX, menuY + itemH, itemW, itemH, (float) mouseX, (float) mouseY)) {
                module.getBind().setMode(Bind.Mode.HOLD);
                showBindModeMenu = false;
                return;
            }
             
            if (!MathUtils.isHovered(menuX, menuY, itemW, itemH * 2f + 7f, (float) mouseX, (float) mouseY)) {
                showBindModeMenu = false;
                return;
            }
             
            return;
        }

        if (open && !renderExternally) {
            for (Component component : components) component.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
         
        if (showBindModeMenu) return;
        if (open && !renderExternally) {
            for (Component component : components) component.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
                module.setBind(new Bind(-1, false));  
            } else {
                 
                Bind.Mode mode = module.getBind() != null ? module.getBind().getMode() : Bind.Mode.TOGGLE;
                module.setBind(new Bind(keyCode, false, mode));
            }
            binding = false;
            return;
        }

        if (open && !renderExternally) {
            for (Component component : components) component.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void keyReleased(int keyCode, int scanCode, int modifiers) {
        if (open && !renderExternally) {
            for (Component component : components) component.keyReleased(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (open && !renderExternally) {
            for (Component component : components) component.charTyped(chr, modifiers);
        }
    }

     
    @Override
    public float getHeight() {
        if (renderExternally) return HEADER_HEIGHT;
        return HEADER_HEIGHT + (open ? getChildrenFullHeight() : 0f);
    }

    public float getChildrenFullHeight() {
        float h = 0f;
        for (Component c : components) {
            if (!c.getVisible().get()) continue;
            h += CHILD_HEIGHT + c.getAddHeight().get();
        }
        return h;
    }

    private static Color lerpColor(Color c1, Color c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (c1.getRed()   + (c2.getRed()   - c1.getRed())   * t);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int) (c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * t);
        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * t);
        return new Color(r, g, b, a);
    }
    public Module getModule() {
        return module;
    }

    public void setRenderExternally(boolean value) {
        this.renderExternally = value;
    }

    public boolean isRenderExternally() {
        return renderExternally;
    }

    public boolean isBindModeMenuOpen() {
        return showBindModeMenu;
    }

    public float renderSettingsExternally(DrawContext context, float contentX, float contentY, float contentW,
                                          float clipX, float clipY, float clipW, float clipH, int mouseX, int mouseY, float delta, float scrollY) {
        float childY = contentY - scrollY;
        float totalHeight = 0f;
        context.enableScissor((int) clipX, (int) clipY, (int) (clipX + clipW), (int) (clipY + clipH));
        for (Component component : components) {
            if (!component.getVisible().get()) continue;
            component.setX(contentX + 5f);
            component.setY(childY);
            component.setWidth(contentW - 10f);
            component.setHeight(CHILD_HEIGHT);
            component.setGlobalAlpha(globalAlpha);
            component.render(context, mouseX, mouseY, delta);
            float h = component.getHeight() + component.getAddHeight().get();
            childY += h;
            totalHeight += h;
        }
        context.disableScissor();
        return totalHeight;
    }

    public void mouseClickedExternal(double mouseX, double mouseY, int button) {
        for (Component component : components) component.mouseClicked(mouseX, mouseY, button);
    }

    public void mouseReleasedExternal(double mouseX, double mouseY, int button) {
        for (Component component : components) component.mouseReleased(mouseX, mouseY, button);
    }

    public void keyPressedExternal(int keyCode, int scanCode, int modifiers) {
        for (Component component : components) component.keyPressed(keyCode, scanCode, modifiers);
    }

    public void keyReleasedExternal(int keyCode, int scanCode, int modifiers) {
        for (Component component : components) component.keyReleased(keyCode, scanCode, modifiers);
    }

    public void charTypedExternal(char chr, int modifiers) {
        for (Component component : components) component.charTyped(chr, modifiers);
    }
}