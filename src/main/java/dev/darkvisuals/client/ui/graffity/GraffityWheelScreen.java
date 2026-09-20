package dev.darkvisuals.client.ui.graffity;

import dev.darkvisuals.client.managers.GraffityManager;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.animations.SmoothAnimation;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.impl.render.Graffity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.List;

/**
 * GraffityWheelScreen — круг выбора граффити в стиле GTA 5.
 *
 * Открывается по бинду (по умолчанию G). Иконки граффити расставлены по кругу,
 * каждая в своём секторе. Наведя мышку на сектор и кликнув, выбираешь граффити —
 * оно ставится в мире перед игроком, круг закрывается.
 */
public class GraffityWheelScreen extends Screen {

    private static final float WHEEL_RADIUS = 130f;
    private static final float ICON_SIZE = 64f;
    private static final float CENTER_RADIUS = 34f;
    private static final float FONT_SIZE = 8f;

    private final Graffity module;
    private final SmoothAnimation openAnimation = new SmoothAnimation(0f, 16f);
    private boolean closing = false;

    // пока бинд зажат после открытия, не считаем его повторным нажатием
    private boolean bindHeldSinceOpen = true;

    public GraffityWheelScreen(Graffity module) {
        super(Text.of("darkvisuals-graffity-wheel"));
        this.module = module;
    }

    @Override
    protected void init() {
        super.init();
        if (!closing) openAnimation.setTarget(1f);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        float progress = openAnimation.update();
        if (closing && progress <= 0.02f) {
            closing = false;
            openAnimation.snapTo(0f);
            if (this.client != null) this.client.setScreen(null);
            return;
        }

        MatrixStack stack = context.getMatrices();
        Color theme = ThemeManager.getInstance().getThemeColor();

        float cx = this.width / 2f;
        float cy = this.height / 2f;

        // лёгкое затемнение, чтобы круг читался на любом фоне
        Render2D.drawRoundedRect(stack, 0, 0, this.width, this.height, 0,
                new Color(0, 0, 0, (int) (90 * progress)));

        List<Identifier> textures = GraffityManager.getTextures();
        List<String> names = GraffityManager.getNames();

        // центральный круг
        boolean centerHovered = isCenterHovered(mouseX, mouseY, cx, cy);
        Color centerColor = centerHovered
                ? new Color(200, 60, 60, (int) (220 * progress))
                : new Color(18, 18, 22, (int) (200 * progress));
        Render2D.drawRoundedRect(stack,
                cx - CENTER_RADIUS, cy - CENTER_RADIUS,
                CENTER_RADIUS * 2, CENTER_RADIUS * 2,
                CENTER_RADIUS, centerColor);

        String centerText = "Graffity";
        float ctw = Fonts.MEDIUM.getWidth(centerText, FONT_SIZE - 1.5f);
        Render2D.drawFont(stack, Fonts.MEDIUM.getFont(FONT_SIZE - 1.5f), centerText,
                cx - ctw / 2f, cy - Fonts.MEDIUM.getHeight(FONT_SIZE - 1.5f) / 2f,
                new Color(255, 255, 255, (int) (255 * progress)));

        if (textures.isEmpty()) {
            String empty = "Нет граффити в assets/darkvisuals/grafi/";
            float ew = Fonts.MEDIUM.getWidth(empty, FONT_SIZE);
            Render2D.drawFont(stack, Fonts.MEDIUM.getFont(FONT_SIZE), empty,
                    cx - ew / 2f, cy + CENTER_RADIUS + 16f,
                    new Color(255, 255, 255, (int) (180 * progress)));
            return;
        }

        int count = textures.size();
        int hovered = getHoveredIndex(mouseX, mouseY, count, cx, cy);

        // рисуем сначала «спицы» — линии от центра к каждой иконке
        for (int i = 0; i < count; i++) {
            float angle = angleFor(i, count);
            float r = WHEEL_RADIUS * (0.75f + 0.25f * progress);
            float nx = cx + MathHelper.cos(angle) * r;
            float ny = cy + MathHelper.sin(angle) * r;

            int spokeAlpha = (int) (60 * progress);
            if (i == hovered) {
                spokeAlpha = (int) (140 * progress);
                Render2D.drawLine(stack, cx, cy, nx, ny, 2.5f,
                        new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), spokeAlpha));
            } else {
                Render2D.drawLine(stack, cx, cy, nx, ny, 1.2f,
                        new Color(255, 255, 255, spokeAlpha));
            }
        }

        for (int i = 0; i < count; i++) {
            float[] pos = nodePosition(i, count, cx, cy, progress);
            float nx = pos[0] - ICON_SIZE / 2f;
            float ny = pos[1] - ICON_SIZE / 2f;

            boolean isHovered = i == hovered;
            float iconScale = isHovered ? 1.15f : 1.0f;
            float drawSize = ICON_SIZE * iconScale;
            float drawX = pos[0] - drawSize / 2f;
            float drawY = pos[1] - drawSize / 2f;

            if (isHovered) {
                // свечение под выбранной иконкой
                Render2D.drawBlurredRect(stack, drawX, drawY, drawSize, drawSize, drawSize / 2f, 8f,
                        new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), (int) (110 * progress)));
            }

            // рамка-квадрат под иконку (скруглённая)
            Color frameColor = isHovered
                    ? new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), (int) (240 * progress))
                    : new Color(30, 30, 36, (int) (210 * progress));
            Render2D.drawRoundedRect(stack, drawX, drawY, drawSize, drawSize, 10f, frameColor);

            // сама картинка граффити
            Render2D.drawTexture(stack, drawX + 5f, drawY + 5f, drawSize - 10f, drawSize - 10f, 0f,
                    textures.get(i), new Color(255, 255, 255, (int) (255 * progress)));

            // подпись под иконкой
            String label = i < names.size() ? names.get(i) : "graffity " + (i + 1);
            float tw = Fonts.MEDIUM.getWidth(label, FONT_SIZE);
            float labelY = drawY + drawSize + 4f;
            Render2D.drawFont(stack, Fonts.MEDIUM.getFont(FONT_SIZE), label,
                    pos[0] - tw / 2f, labelY,
                    isHovered
                            ? new Color(255, 255, 255, (int) (255 * progress))
                            : new Color(210, 210, 215, (int) (200 * progress)));
        }
    }

    private float angleFor(int index, int count) {
        return (float) (-Math.PI / 2 + (Math.PI * 2 / count) * index);
    }

    private float[] nodePosition(int index, int count, float cx, float cy, float progress) {
        float angle = angleFor(index, count);
        float radius = WHEEL_RADIUS * (0.75f + 0.25f * progress);
        return new float[]{
                cx + MathHelper.cos(angle) * radius,
                cy + MathHelper.sin(angle) * radius
        };
    }

    /**
     * Секторный выбор как в GTA 5: мышка попадает в сектор круга,
     * а не в саму иконку. Так целиться проще и понятнее.
     */
    private int getHoveredIndex(double mouseX, double mouseY, int count, float cx, float cy) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        // слишком близко к центру — ничего не выбрано
        if (dist <= CENTER_RADIUS) return -1;

        // угол мыши: 0 справа, растёт по часовой. Переводим так, чтобы
        // первая иконка была сверху и порядок шёл по часовой.
        double mouseAngle = Math.atan2(dy, dx); // -PI..PI, вверх это -PI/2
        double sector = (Math.PI * 2) / count;

        // сдвигаем так, что верхний сектор — нулевой
        double normalized = mouseAngle + Math.PI / 2;
        if (normalized < 0) normalized += Math.PI * 2;

        int index = (int) (normalized / sector);
        if (index >= count) index = count - 1;
        if (index < 0) index = 0;

        return index;
    }

    private boolean isCenterHovered(double mouseX, double mouseY, float cx, float cy) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        return dx * dx + dy * dy <= CENTER_RADIUS * CENTER_RADIUS;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);

        float cx = this.width / 2f;
        float cy = this.height / 2f;

        if (isCenterHovered(mouseX, mouseY, cx, cy)) {
            // клик по центру — закрыть, ничего не ставя
            beginClose();
            return true;
        }

        int count = GraffityManager.count();
        int hovered = getHoveredIndex(mouseX, mouseY, count, cx, cy);
        if (hovered >= 0 && hovered < count) {
            module.placeSelected(hovered);
            // звук клика как в остальных UI
            if (this.client != null) {
                this.client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                        net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.7f));
            }
            beginClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            beginClose();
            return true;
        }

        var bind = module.getWheelBind().getValue();
        if (bind != null && !bind.isMouse() && keyCode == bind.getKey()) {
            // бинд ещё зажат с момента открытия — игнорируем
            if (bindHeldSinceOpen) return true;
            beginClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        var bind = module.getWheelBind().getValue();
        if (bind != null && !bind.isMouse() && keyCode == bind.getKey()) {
            bindHeldSinceOpen = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void beginClose() {
        closing = true;
        openAnimation.setTarget(0f);
    }
}
