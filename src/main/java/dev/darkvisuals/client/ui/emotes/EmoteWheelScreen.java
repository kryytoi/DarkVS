package dev.darkvisuals.client.ui.emotes;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.animations.SmoothAnimation;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.emotes.Emote;
import dev.darkvisuals.emotes.EmoteManager;
import dev.darkvisuals.modules.impl.render.Emotes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.List;

public class EmoteWheelScreen extends Screen {

    private static final float WHEEL_RADIUS = 92f;
    private static final float NODE_WIDTH = 96f;
    private static final float NODE_HEIGHT = 26f;
    private static final float CENTER_RADIUS = 26f;
    private static final float FONT_SIZE = 8.5f;

    private final Emotes module;
    private final SmoothAnimation openAnimation = new SmoothAnimation(0f, 16f);
    private boolean closing = false;

      
    private boolean bindHeldSinceOpen = true;

    public EmoteWheelScreen(Emotes module) {
        super(Text.of("darkvisuals-emote-wheel"));
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
        int bgAlpha = (int) (110 * progress);

         
        Render2D.drawRoundedRect(stack, 0, 0, this.width, this.height, 0, new Color(0, 0, 0, bgAlpha));

        List<Emote> emotes = module.getWheelEmotes();

         
        boolean centerHovered = isCenterHovered(mouseX, mouseY, cx, cy);
        boolean playing = EmoteManager.isPlayingLocal();
        Color centerColor = centerHovered && playing
                ? new Color(200, 60, 60, (int) (220 * progress))
                : new Color(18, 18, 22, (int) (200 * progress));
        Render2D.drawRoundedRect(stack,
                cx - CENTER_RADIUS, cy - CENTER_RADIUS,
                CENTER_RADIUS * 2, CENTER_RADIUS * 2,
                CENTER_RADIUS, centerColor);

        String centerText = playing
                ? I18n.translate("emote.darkvisuals.stop")
                : I18n.translate("emote.darkvisuals.title");
        float ctw = Fonts.MEDIUM.getWidth(centerText, FONT_SIZE - 1.5f);
        Render2D.drawFont(stack, Fonts.MEDIUM.getFont(FONT_SIZE - 1.5f), centerText,
                cx - ctw / 2f, cy - Fonts.MEDIUM.getHeight(FONT_SIZE - 1.5f) / 2f,
                new Color(255, 255, 255, (int) (255 * progress)));

        if (emotes.isEmpty()) {
            String empty = I18n.translate("emote.darkvisuals.empty");
            float ew = Fonts.MEDIUM.getWidth(empty, FONT_SIZE);
            Render2D.drawFont(stack, Fonts.MEDIUM.getFont(FONT_SIZE), empty,
                    cx - ew / 2f, cy + CENTER_RADIUS + 14f,
                    new Color(255, 255, 255, (int) (180 * progress)));
            return;
        }

        int hovered = getHoveredIndex(mouseX, mouseY, emotes.size(), cx, cy);

        for (int i = 0; i < emotes.size(); i++) {
            Emote emote = emotes.get(i);
            float[] pos = nodePosition(i, emotes.size(), cx, cy, progress);
            float nx = pos[0] - NODE_WIDTH / 2f;
            float ny = pos[1] - NODE_HEIGHT / 2f;

            boolean isHovered = i == hovered;
            Color nodeColor = isHovered
                    ? new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), (int) (230 * progress))
                    : new Color(18, 18, 22, (int) (200 * progress));

            if (isHovered) {
                Render2D.drawBlurredRect(stack, nx, ny, NODE_WIDTH, NODE_HEIGHT, NODE_HEIGHT / 2f, 6f,
                        new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), (int) (90 * progress)));
            }
            Render2D.drawRoundedRect(stack, nx, ny, NODE_WIDTH, NODE_HEIGHT, NODE_HEIGHT / 2f, nodeColor);

            String label = I18n.translate(emote.translationKey());
            float tw = Fonts.MEDIUM.getWidth(label, FONT_SIZE);
            Color textColor = isHovered
                    ? new Color(255, 255, 255, (int) (255 * progress))
                    : new Color(220, 220, 225, (int) (230 * progress));
            Render2D.drawFont(stack, Fonts.MEDIUM.getFont(FONT_SIZE), label,
                    pos[0] - tw / 2f,
                    pos[1] - Fonts.MEDIUM.getHeight(FONT_SIZE) / 2f,
                    textColor);
        }
    }

      
    private float[] nodePosition(int index, int count, float cx, float cy, float progress) {
        float angle = (float) (-Math.PI / 2 + (Math.PI * 2 / count) * index);
        float radius = WHEEL_RADIUS * (0.75f + 0.25f * progress);
        return new float[]{
                cx + MathHelper.cos(angle) * radius,
                cy + MathHelper.sin(angle) * radius
        };
    }

    private int getHoveredIndex(double mouseX, double mouseY, int count, float cx, float cy) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            float[] pos = nodePosition(i, count, cx, cy, 1f);
            double dx = mouseX - pos[0];
            double dy = mouseY - pos[1];
            if (Math.abs(dx) <= NODE_WIDTH / 2f + 6f && Math.abs(dy) <= NODE_HEIGHT / 2f + 6f) {
                double dist = dx * dx + dy * dy;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = i;
                }
            }
        }
        return best;
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
            if (EmoteManager.isPlayingLocal()) EmoteManager.stopLocal();
            beginClose();
            return true;
        }

        List<Emote> emotes = module.getWheelEmotes();
        int hovered = getHoveredIndex(mouseX, mouseY, emotes.size(), cx, cy);
        if (hovered >= 0) {
            EmoteManager.playLocal(emotes.get(hovered));
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
             
             
            if (bindHeldSinceOpen) {
                return true;
            }
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
