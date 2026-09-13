package dev.darkvisuals.client.ui.browser;

import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.impl.utility.BrowserModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

/**
 * Полноэкранный вид реального браузера: кадр окна браузера растягивается
 * на весь экран, а весь ввод (клики, скролл, клавиатура) пересылается
 * в само окно через PostMessage — можно листать и делать всё как в обычном
 * браузере.
 *
 * Повторное нажатие бинда (или Esc) убирает полноэкранный вид и возвращает
 * управление игроку — при этом браузер продолжает рендериться на 3D-плоскости
 * в мире.
 */
public class BrowserScreen extends Screen {

    private static final float FONT_SMALL = 7.5f;
    private static final float BAR_HEIGHT = 16f;
    /**
     * Защита от мгновенного закрытия: нажатие бинда, открывшее экран,
     * доходит и самому экрану (EventKey постится до передачи клавиши GUI),
     * поэтому первые OPEN_GUARD_MS мс бинд экран не закрывает.
     */
    private static final long OPEN_GUARD_MS = 300L;

    private final BrowserModule module;
    private final long openedAt = System.currentTimeMillis();

    // раскладка кадра на экране (с учётом letterbox)
    private float drawX, drawY, drawW, drawH;
    private float scale = 1f;

    public BrowserScreen(BrowserModule module) {
        super(Text.of("darkvisuals-browser"));
        this.module = module;
    }

    @Override
    protected void init() {
        super.init();
        computeLayout();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        computeLayout();
    }

    private void computeLayout() {
        BrowserCapture capture = module.getCapture();
        if (!capture.isAvailable() || capture.getWidth() <= 0 || capture.getHeight() <= 0) {
            drawX = 0;
            drawY = 0;
            drawW = this.width;
            drawH = this.height;
            scale = 1f;
            return;
        }
        // вписать кадр в экран (letterbox)
        scale = Math.min(
                (float) this.width / capture.getWidth(),
                (float) (this.height - BAR_HEIGHT) / capture.getHeight());
        drawW = capture.getWidth() * scale;
        drawH = capture.getHeight() * scale;
        drawX = (this.width - drawW) / 2f;
        drawY = (this.height - BAR_HEIGHT - drawH) / 2f;
    }

    // ------------------------------------------------------------------
    // Рендер
    // ------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        computeLayout();
        MatrixStack stack = context.getMatrices();
        BrowserCapture capture = module.getCapture();

        // фон
        Render2D.drawRect(stack, 0, 0, this.width, this.height, new Color(8, 10, 13, 255));

        if (capture.isAvailable()) {
            // кадр реального браузера
            Render2D.drawTexture(stack, drawX, drawY, drawW, drawH, 0,
                    capture.getTexture(), new Color(255, 255, 255, 255));
            renderInfoBar(stack, capture);
        } else {
            renderPlaceholder(stack);
        }
    }

    private void renderInfoBar(MatrixStack stack, BrowserCapture capture) {
        // нижняя панель-подсказка
        Render2D.drawRect(stack, 0, this.height - BAR_HEIGHT, this.width, BAR_HEIGHT,
                new Color(12, 14, 18, 235));

        String state = module.getRenderer().isStanding()
                ? "3D-экран перед вами"
                : "3D-экран следует за вами";
        String hint = bindName() + " / Esc — вернуться в игру  ·  " + state;

        float hintW = Fonts.REGULAR.getWidth(hint, FONT_SMALL);
        Render2D.drawFont(stack, Fonts.REGULAR.getFont(FONT_SMALL), hint,
                10f, this.height - BAR_HEIGHT / 2f - Fonts.REGULAR.getHeight(FONT_SMALL) / 2f,
                new Color(150, 156, 166, 255));

        String title = trim(capture.getWindowTitle(), (int) (this.width - hintW - 40f));
        float titleW = Fonts.REGULAR.getWidth(title, FONT_SMALL);
        Render2D.drawFont(stack, Fonts.REGULAR.getFont(FONT_SMALL), title,
                this.width - titleW - 10f, this.height - BAR_HEIGHT / 2f - Fonts.REGULAR.getHeight(FONT_SMALL) / 2f,
                new Color(190, 196, 206, 255));
    }

    private void renderPlaceholder(MatrixStack stack) {
        String status = module.getCapture().getSearchStatus();
        if (status == null || status.isBlank()) status = "Окно браузера не найдено";

        String[] lines = {
                status,
                "",
                "Запустите выбранный браузер (настройка \"Браузер\" в модуле) —",
                "его экран появится здесь и на 3D-плоскости."
        };

        float y = this.height / 2f - lines.length * 6f;
        int i = 0;
        for (String line : lines) {
            float lw = Fonts.REGULAR.getWidth(line, i == 0 ? FONT_SMALL + 1f : FONT_SMALL);
            float x = this.width / 2f - lw / 2f;
            Color color = i == 0 ? new Color(235, 238, 244, 255) : new Color(160, 166, 176, 255);
            Render2D.drawFont(stack, Fonts.REGULAR.getFont(FONT_SMALL + (i == 0 ? 1f : 0f)), line, x, y, color);
            y += 12f;
            i++;
        }
    }

    // ------------------------------------------------------------------
    // Ввод -> пересылка в браузер
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // бинд-мышью тоже должен закрывать полноэкранный вид
        var bind = module.getBind();
        if (bind != null && bind.isMouse() && bind.getKey() >= 0 && button == bind.getKey()) {
            if (System.currentTimeMillis() - openedAt > OPEN_GUARD_MS) {
                closeScreen();
            }
            return true; // бинд не пересылается в браузер
        }

        BrowserCapture capture = module.getCapture();
        if (capture.isAvailable()) {
            int[] pos = toBrowser(mouseX, mouseY);
            if (pos != null) {
                capture.sendMouseDown(pos[0], pos[1], button);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        BrowserCapture capture = module.getCapture();
        if (capture.isAvailable()) {
            int[] pos = toBrowser(mouseX, mouseY);
            if (pos != null) {
                capture.sendMouseUp(pos[0], pos[1], button);
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        BrowserCapture capture = module.getCapture();
        if (capture.isAvailable()) {
            int[] pos = toBrowser(mouseX, mouseY);
            if (pos != null) {
                capture.sendMouseMove(pos[0], pos[1]);
            }
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        mouseMoved(mouseX, mouseY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        BrowserCapture capture = module.getCapture();
        if (capture.isAvailable()) {
            int[] pos = toBrowser(mouseX, mouseY);
            if (pos != null) {
                capture.sendWheel(pos[0], pos[1], verticalAmount, horizontalAmount);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // повторное нажатие бинда — убрать полноэкранный вид,
        // браузер продолжает рендериться на 3D-плоскости
        var bind = module.getBind();
        if (bind != null && !bind.isMouse() && bind.getKey() >= 0 && keyCode == bind.getKey()) {
            if (System.currentTimeMillis() - openedAt > OPEN_GUARD_MS) {
                closeScreen();
            }
            return true; // бинд не пересылается в браузер
        }

        // Esc закрывает через super.keyPressed (shouldCloseOnEsc)
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        BrowserCapture capture = module.getCapture();
        if (capture.isAvailable()) {
            int vk = BrowserCapture.glfwToVirtualKey(keyCode);
            if (vk > 0) {
                capture.sendKey(vk, true);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // отпускание бинда тоже не должно попадать в браузер
        var bind = module.getBind();
        if (bind != null && !bind.isMouse() && bind.getKey() >= 0 && keyCode == bind.getKey()) {
            return true;
        }

        BrowserCapture capture = module.getCapture();
        if (capture.isAvailable()) {
            int vk = BrowserCapture.glfwToVirtualKey(keyCode);
            if (vk > 0) {
                capture.sendKey(vk, false);
                return true;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        BrowserCapture capture = module.getCapture();
        if (capture.isAvailable()) {
            capture.sendChar(chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    // ------------------------------------------------------------------
    // Утилиты
    // ------------------------------------------------------------------

    /**
     * Переводит координаты экрана игры в координаты клиентской области
     * окна браузера. null — если точка вне кадра.
     */
    private int[] toBrowser(double mouseX, double mouseY) {
        if (drawW <= 0 || drawH <= 0 || scale <= 0f) return null;
        if (mouseX < drawX || mouseX > drawX + drawW) return null;
        if (mouseY < drawY || mouseY > drawY + drawH) return null;
        int bx = (int) Math.round((mouseX - drawX) / scale);
        int by = (int) Math.round((mouseY - drawY) / scale);
        return new int[]{bx, by};
    }

    private String bindName() {
        var bind = module.getBind();
        if (bind == null || bind.getKey() < 0) return "Бинд";
        if (bind.isMouse()) return "Кнопка " + (bind.getKey() + 1);
        String name = GLFW.glfwGetKeyName(bind.getKey(), 0);
        if (name == null || name.isBlank()) return "Бинд";
        return name.toUpperCase();
    }

    private String trim(String text, int maxWidth) {
        if (Fonts.REGULAR.getWidth(text, FONT_SMALL) <= maxWidth) return text;
        while (text.length() > 1 && Fonts.REGULAR.getWidth(text + "...", FONT_SMALL) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    private void closeScreen() {
        if (this.client != null) this.client.setScreen(null);
    }
}
