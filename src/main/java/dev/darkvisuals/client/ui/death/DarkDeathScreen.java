package dev.darkvisuals.client.ui.death;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.animations.SmoothAnimation;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.Random;

/**
 * DarkDeathScreen - кастомный экран смерти в стиле клиента.
 *
 * Расширяет ванильный DeathScreen (сохраняя серверную логику), но полностью
 * перерисовывает визуал: тёмный градиент, пульсирующая виньетка, плавающие
 * частицы, крупный заголовок со scale-in анимацией, счёт и кнопки в стиле
 * клиента ("Возродиться", "В главное меню").
 */
public class DarkDeathScreen extends DeathScreen {

    private static final int PARTICLE_COUNT = 42;
    private final float[] particleX = new float[PARTICLE_COUNT];
    private final float[] particleY = new float[PARTICLE_COUNT];
    private final float[] particleSpeed = new float[PARTICLE_COUNT];
    private final float[] particleSize = new float[PARTICLE_COUNT];
    private final float[] particlePhase = new float[PARTICLE_COUNT];

    private final SmoothAnimation entrance = new SmoothAnimation(0f, 12f);
    private boolean initialized = false;

    private boolean respawnHovered = false;
    private boolean menuHovered = false;

    public DarkDeathScreen(Text message, boolean isHardcore) {
        super(message, isHardcore);
        Random rnd = new Random();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleX[i] = rnd.nextFloat();
            particleY[i] = rnd.nextFloat();
            particleSpeed[i] = 0.02f + rnd.nextFloat() * 0.06f;
            particleSize[i] = 1.2f + rnd.nextFloat() * 2.6f;
            particlePhase[i] = rnd.nextFloat() * 6.28f;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        entrance.setTarget(1f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float p = MathHelper.clamp(entrance.update(), 0f, 1f);
        long time = System.currentTimeMillis();
        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();

        MatrixStack stack = context.getMatrices();
        float w = this.width;
        float h = this.height;

        // Фон: тёмный вертикальный градиент
        context.fillGradient(0, 0, (int) w, (int) h,
                (int) (p * 255) << 24 | 0x0A0508,
                (int) (p * 255) << 24 | 0x1A0A12);

        // Пульсирующая красная виньетка (4 перекрывающихся градиента)
        float pulse = 0.5f + 0.5f * (float) Math.sin(time / 900.0);
        int vignetteAlpha = (int) (55 * p * (0.7f + 0.3f * pulse));
        context.fillGradient(0, 0, (int) w, (int) (h / 3), (vignetteAlpha / 2) << 24 | 0x5A0E14, 0);
        context.fillGradient(0, (int) (h - h / 3), (int) w, (int) h, 0, (vignetteAlpha) << 24 | 0x5A0E14);

        // Плавающие частицы
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float py = (particleY[i] - (time / 1000f) * particleSpeed[i]) % 1f;
            if (py < 0) py += 1f;
            float px = particleX[i] + 0.02f * (float) Math.sin(time / 1400.0 + particlePhase[i]);
            float drift = (float) Math.sin(time / 1000f * 1.5f + particlePhase[i]);
            int alpha = (int) (60 * p * (0.5f + 0.5f * drift * drift));
            float size = particleSize[i];
            Render2D.drawRoundedRect(stack, px * w, py * h, size, size, size / 2f,
                    new Color(255, 120, 130, Math.max(0, Math.min(255, alpha))));
        }

        // Заголовок с scale-in
        String title = "Вы погибли";
        float titleSize = 22f;
        float scale = 0.8f + 0.2f * p;
        float titleW = Fonts.SEMIBOLD.getWidth(title, titleSize);
        float titleH = Fonts.SEMIBOLD.getHeight(titleSize);
        float cx = w / 2f;
        float titleY = h * 0.28f;

        stack.push();
        stack.translate(cx, titleY + titleH / 2f, 0f);
        stack.scale(scale, scale, 1f);
        stack.translate(-cx, -(titleY + titleH / 2f), 0f);
        Render2D.drawFont(stack, Fonts.SEMIBOLD.getFont(titleSize), title, cx - titleW / 2f, titleY,
                new Color(255, 235, 240, (int) (245 * p)));
        stack.pop();

        // Подчёркивание в цвет темы
        float lineW = titleW * (0.55f + 0.45f * p);
        Render2D.drawRoundedRect(stack, cx - lineW / 2f, titleY + titleH + 6f, lineW, 1.6f, 0.8f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (220 * p)));

        // Счёт
        if (this.client != null && this.client.player != null) {
            String score = "Счёт: " + this.client.player.getScore();
            float ss = 8.5f;
            float sw = Fonts.MEDIUM.getWidth(score, ss);
            Render2D.drawFont(stack, Fonts.MEDIUM.getFont(ss), score, cx - sw / 2f, titleY + titleH + 18f,
                    new Color(235, 235, 245, (int) (200 * p)));
        }

        // Кнопки
        float btnW = 150f, btnH = 26f, gap = 14f;
        float btnY = h * 0.62f;
        float respawnX = cx - btnW - gap / 2f;
        float menuX = cx + gap / 2f;

        respawnHovered = mouseX >= respawnX && mouseX <= respawnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        menuHovered = mouseX >= menuX && mouseX <= menuX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        drawStyledButton(stack, "Возродиться", respawnX, btnY, btnW, btnH,
                respawnHovered ? accent : new Color(20, 20, 28, (int) (225 * p)), p);
        drawStyledButton(stack, "В главное меню", menuX, btnY, btnW, btnH,
                menuHovered ? new Color(170, 55, 55, (int) (235 * p)) : new Color(20, 20, 28, (int) (225 * p)), p);

        // Подсказка
        String hint = "Пробел - быстрый респавн";
        float hsz = 6.5f;
        float hw = Fonts.MEDIUM.getWidth(hint, hsz);
        Render2D.drawFont(stack, Fonts.MEDIUM.getFont(hsz), hint, cx - hw / 2f, btnY + btnH + 14f,
                new Color(255, 255, 255, (int) (110 * p)));
    }

    private void drawStyledButton(MatrixStack stack, String text, float x, float y, float w, float h, Color bg, float p) {
        Render2D.drawRoundedRect(stack, x, y, w, h, 8f, new Color(bg.getRed(), bg.getGreen(), bg.getBlue(),
                (int) (bg.getAlpha() * p)));
        float fs = 8f;
        float tw = Fonts.MEDIUM.getWidth(text, fs);
        Render2D.drawFont(stack, Fonts.MEDIUM.getFont(fs), text, x + (w - tw) / 2f, y + (h - Fonts.MEDIUM.getHeight(fs)) / 2f,
                new Color(255, 255, 255, (int) (245 * p)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float cx = this.width / 2f;
            float btnW = 150f, btnH = 26f, gap = 14f;
            float btnY = this.height * 0.62f;
            float respawnX = cx - btnW - gap / 2f;
            float menuX = cx + gap / 2f;

            if (mouseX >= respawnX && mouseX <= respawnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                respawn();
                return true;
            }
            if (mouseX >= menuX && mouseX <= menuX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                quitToMenu();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE && !respawnHovered) {
            respawn();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void respawn() {
        if (this.client == null || this.client.player == null) return;
        this.client.player.requestRespawn();
        this.client.setScreen(null);
    }

    private void quitToMenu() {
        if (this.client == null) return;
        if (this.client.world != null) this.client.world.disconnect();
        this.client.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
        this.client.setScreen(new TitleScreen());
    }
}
