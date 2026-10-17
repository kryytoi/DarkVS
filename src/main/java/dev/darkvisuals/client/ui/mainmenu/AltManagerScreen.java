package dev.darkvisuals.client.ui.mainmenu;

import dev.darkvisuals.client.managers.AltManager;
import dev.darkvisuals.client.render.builders.impl.RectangleBuilder;
import dev.darkvisuals.client.render.builders.impl.BlurBuilder;
import dev.darkvisuals.client.render.builders.impl.TextBuilder;
import dev.darkvisuals.client.render.builders.states.QuadColorState;
import dev.darkvisuals.client.render.builders.states.QuadRadiusState;
import dev.darkvisuals.client.render.builders.states.SizeState;
import dev.darkvisuals.client.util.animations.Animation;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.resource.language.I18n;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AltManagerScreen extends Screen {

    private final Screen parent;
    private final List<String> alts = new ArrayList<>();
    private final List<String> filteredAlts = new ArrayList<>();
    private final List<MainMenu.AnimatedButton> buttons = new ArrayList<>();
    private String inputBuffer = "";
    private String searchBuffer = "";
    private int selectedIndex = -1;
	private int caretIndex = 0;
	private int selectionStart = -1;
    private int searchCaretIndex = 0;
    private int searchSelectionStart = -1;

    private boolean inputFocused = false;
    private boolean searchFocused = false;

    private final Animation inputFocusAnimation = new Animation(220, 1.0, false, Easing.OUT_EXPO);
    private final Animation searchFocusAnimation = new Animation(220, 1.0, false, Easing.OUT_EXPO);
    private final Animation listHoverAnimation = new Animation(200, 1.0, false, Easing.OUT_EXPO);
    private int hoveredRowIndex = -1;

    private float listScroll = 0f;
    private float listScrollTarget = 0f;
    private float listMaxScroll = 0f;

    public AltManagerScreen(Screen parent) {
        super(Text.literal(I18n.translate("darkvisuals.alt.title")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        alts.clear();
        alts.addAll(AltManager.getNicknames());
        filterAlts();
        buttons.clear();
		if (caretIndex > inputBuffer.length()) caretIndex = inputBuffer.length();
        if (searchCaretIndex > searchBuffer.length()) searchCaretIndex = searchBuffer.length();

        listScroll = 0f;
        listScrollTarget = 0f;

        String active = AltManager.getLastUsedNickname();
        if (active != null) {
            for (int i = 0; i < filteredAlts.size(); i++) {
                if (filteredAlts.get(i).equalsIgnoreCase(active)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        float buttonWidth = 125f;
        float buttonHeight = 22f;
        float groupMargin = 8f;
        float centerY = this.height / 2f + 60f;

        float groupWidth = buttonWidth * 2f + groupMargin;
        float groupStartX = this.width / 2f - groupWidth / 2f;

        buttons.add(new MainMenu.AnimatedButton(groupStartX, centerY, buttonWidth, buttonHeight, I18n.translate("darkvisuals.alt.add"), b -> {
            if (!inputBuffer.isEmpty()) {
                if (AltManager.addNickname(inputBuffer)) {
                    inputBuffer = "";
                    caretIndex = 0;
                    selectionStart = -1;
                    init();
                }
            }
        }));

        buttons.add(new MainMenu.AnimatedButton(groupStartX + buttonWidth + groupMargin, centerY, buttonWidth, buttonHeight, I18n.translate("darkvisuals.alt.delete"), b -> {
            if (selectedIndex >= 0 && selectedIndex < filteredAlts.size()) {
                String target = filteredAlts.get(selectedIndex);
                if (AltManager.removeNickname(target)) {
                    selectedIndex = -1;
                    init();
                }
            }
        }));

        float backX = this.width / 2f - buttonWidth / 2f;
        buttons.add(new MainMenu.AnimatedButton(backX, centerY + buttonHeight + groupMargin * 2f, buttonWidth, buttonHeight, I18n.translate("darkvisuals.alt.back"), b -> {
            this.client.setScreen(parent);
        }));
    }

	@Override
	public boolean charTyped(char chr, int modifiers) {
		try {
			if (!inputFocused) return false;
			if (Character.isISOControl(chr)) return false;
			insertText(String.valueOf(chr));
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		try {
			if (keyCode == GLFW.GLFW_KEY_F11) {
				if (this.client != null && this.client.getWindow() != null) {
					this.client.getWindow().toggleFullscreen();
					this.client.options.getFullscreen().setValue(this.client.getWindow().isFullscreen());
					return true;
				}
			}

			if (inputFocused) {
				boolean ctrl = hasControlDown();
				boolean shift = hasShiftDown();

				if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
					String clip = this.client != null && this.client.keyboard != null ? this.client.keyboard.getClipboard() : null;
					if (clip != null && !clip.isEmpty()) insertText(clip);
					return true;
				}
				if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
					String sel = getSelectedText();
					if (sel != null && this.client != null && this.client.keyboard != null) this.client.keyboard.setClipboard(sel);
					return true;
				}
				if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
					String sel = getSelectedText();
					if (sel != null && this.client != null && this.client.keyboard != null) {
						this.client.keyboard.setClipboard(sel);
						deleteSelection();
					}
					return true;
				}
				if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
					selectionStart = 0;
					caretIndex = inputBuffer.length();
					return true;
				}

				if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
					if (!deleteSelection()) {
						caretIndex = Math.max(0, Math.min(caretIndex, inputBuffer.length()));
						if (caretIndex > 0 && !inputBuffer.isEmpty()) {
							inputBuffer = inputBuffer.substring(0, caretIndex - 1) + inputBuffer.substring(caretIndex);
							caretIndex--;
						}
					}
					return true;
				}
				if (keyCode == GLFW.GLFW_KEY_DELETE) {
					if (!deleteSelection()) {
						caretIndex = Math.max(0, Math.min(caretIndex, inputBuffer.length()));
						if (caretIndex < inputBuffer.length()) {
							inputBuffer = inputBuffer.substring(0, caretIndex) + inputBuffer.substring(caretIndex + 1);
						}
					}
					return true;
				}
				if (keyCode == GLFW.GLFW_KEY_LEFT) {
					moveCaret(-1, shift);
					return true;
				}
				if (keyCode == GLFW.GLFW_KEY_RIGHT) {
					moveCaret(1, shift);
					return true;
				}
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
					inputFocused = false;
					selectionStart = -1;
					return true;
				}
				if (keyCode == GLFW.GLFW_KEY_ENTER) {
					if (!inputBuffer.isEmpty()) {
						if (AltManager.addNickname(inputBuffer)) {
							inputBuffer = "";
							caretIndex = 0;
							selectionStart = -1;
							init();
						}
					}
					return true;
				}
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				if (this.client != null) {
					this.client.setScreen(parent);
				}
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_ENTER) {
				if (selectedIndex >= 0 && selectedIndex < filteredAlts.size()) {
					AltManager.applyNickname(filteredAlts.get(selectedIndex));
					init();
				}
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_DELETE && selectedIndex >= 0 && selectedIndex < filteredAlts.size()) {
				String target = filteredAlts.get(selectedIndex);
				if (AltManager.removeNickname(target)) {
					selectedIndex = -1;
					init();
				}
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_UP) {
				if (selectedIndex > 0) {
					selectedIndex--;
				} else if (filteredAlts.size() > 0) {
					selectedIndex = filteredAlts.size() - 1;
				}
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_DOWN) {
				if (selectedIndex < filteredAlts.size() - 1) {
					selectedIndex++;
				} else {
					selectedIndex = 0;
				}
				return true;
			}

			return false;
		} catch (Throwable ignored) {
			return false;
		}
	}

	@Override
	public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
		if (width <= 0 || height <= 0) return;
		try {
			super.resize(client, width, height);
		} catch (Throwable t) {
			this.width = width;
			this.height = height;
			this.init();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {

        float listWidth = 220f;
        float listHeight = 120f;
        float listX = this.width / 2f - listWidth / 2f;
        float listY = this.height / 2f - listHeight / 2f - 22f;
        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            int rowHeight = 16;
            int index = (int) (((mouseY - listY) + listScroll) / rowHeight);
			if (index >= 0 && index < filteredAlts.size()) {
				if (selectedIndex == index) {
					AltManager.applyNickname(filteredAlts.get(index));

					init();
				} else {
					selectedIndex = index;
				}
			}
        }

		float inputWidth = 220f;
		float inputHeight = 18f;
		float inputX = this.width / 2f - inputWidth / 2f;
		float inputY = this.height / 2f + 20f;
		if (mouseX >= inputX && mouseX <= inputX + inputWidth && mouseY >= inputY && mouseY <= inputY + inputHeight) {
			int newIndex = getCaretIndexByMouse((float) mouseX, inputX);
			if (button == 0) {
				if (hasShiftDown()) {
					if (selectionStart == -1) selectionStart = caretIndex;
					caretIndex = newIndex;
				} else {
					caretIndex = newIndex;
					selectionStart = -1;
				}
                inputFocused = true;
                inputFocusAnimation.update(true);
			}
			return true;
		}
        inputFocused = false;
        selectionStart = -1;
        inputFocusAnimation.update(false);

        for (MainMenu.AnimatedButton btn : buttons) {
            if (btn.isMouseOver((int) mouseX, (int) mouseY)) {
                btn.onPress();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        float listWidth = 220f;
        float listHeight = 120f;
        float listX = this.width / 2f - listWidth / 2f;
        float listY = this.height / 2f - listHeight / 2f - 22f;
        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            float step = (float) (-vertical * 12f);
            listScrollTarget = clamp(listScrollTarget + step, 0f, listMaxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        try {
            Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

        new RectangleBuilder()
                .size(new SizeState(this.width, this.height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(
                        new Color(10, 10, 15, 255),
                        new Color(10, 10, 15, 255),
                        new Color(40, 40, 50, 255),
                        new Color(40, 40, 50, 255)
                ))
                .build()
                .render(matrix, 0f, 0f, 0f);

        new BlurBuilder()
                .size(new SizeState(this.width, this.height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(new Color(15, 15, 20, 10)))
                .blurRadius(0f)
                .smoothness(30f)
                .build()
                .render(matrix, 0f, 0f, 0f);

        String title = I18n.translate("darkvisuals.alt.title");
        float titleX = this.width / 2f - Fonts.BOLD.getWidth(title, 9f) / 2f;
        float titleY = this.height / 2f - 120;
        new TextBuilder()
                .font(Fonts.BOLD.font())
                .text(title)
                .size(9f)
                .color(new Color(220, 220, 240))
                .smoothness(0.5f)
                .build()
                .render(matrix, titleX, titleY, 0f);

        String status = String.format("%d/%d alts", AltManager.getAltCount(), AltManager.getMaxAlts());
        float statusX = this.width / 2f - Fonts.REGULAR.getWidth(status, 6f) / 2f;
        float statusY = titleY + 27f;
        new TextBuilder()
                .font(Fonts.REGULAR.font())
                .text(status)
                .size(6f)
                .color(new Color(180, 180, 200))
                .smoothness(0.5f)
                .build()
                .render(matrix, statusX, statusY, 0f);

        float inputWidth = 220f;
        float inputHeight = 18f;
        float inputX = this.width / 2f - inputWidth / 2f;
        float inputY = this.height / 2f + 20f + 0.6f;
        new RectangleBuilder()
                .size(new SizeState(inputWidth, inputHeight))
                .radius(new QuadRadiusState(8))
                .color(new QuadColorState(new Color(24, 24, 30, 255)))
                .build()
                .render(matrix, inputX, inputY, 0f);

        if (inputBuffer.isEmpty() && !inputFocused) {
            String placeholder = I18n.translate("darkvisuals.alt.placeholder");
            try {
                Render2D.drawFont(drawContext.getMatrices(), Fonts.REGULAR.getFont(7f), placeholder, inputX + 6, inputY + 5, new Color(120, 120, 140));
            } catch (Throwable t) {
                drawContext.drawText(this.textRenderer, placeholder, (int)(inputX + 6), (int)(inputY + 5), 0xFF78788C, false);
            }
        } else if (!inputBuffer.isEmpty()) {
            try {
                Render2D.drawFont(drawContext.getMatrices(), Fonts.REGULAR.getFont(7f), inputBuffer, inputX + 6, inputY + 5, new Color(230, 230, 240));
            } catch (Throwable t) {
                drawContext.drawText(this.textRenderer, inputBuffer, (int)(inputX + 6), (int)(inputY + 5), 0xFFE6E6F0, false);
            }
        }

        if (selectionStart != -1 && selectionStart != caretIndex && !inputBuffer.isEmpty()) {
            int a = Math.max(0, Math.min(Math.min(selectionStart, caretIndex), inputBuffer.length()));
            int b = Math.max(0, Math.min(Math.max(selectionStart, caretIndex), inputBuffer.length()));
            if (a < b) {
                float ax = inputX + 6 + Fonts.REGULAR.getWidth(inputBuffer.substring(0, a), 7f);
                float bx = inputX + 6 + Fonts.REGULAR.getWidth(inputBuffer.substring(0, b), 7f);
                drawContext.fill((int) ax, (int) (inputY + 4), (int) bx, (int) (inputY + 4 + Fonts.REGULAR.getHeight(7f)), 0x775078C8);
            }
        }

        if (inputFocused) {
            int safeCaret = Math.max(0, Math.min(caretIndex, inputBuffer.length()));
            float caretX = inputX + 6 + Fonts.REGULAR.getWidth(inputBuffer.substring(0, safeCaret), 7f);
            int alpha = (int) (220 * (0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 180.0)));
            int color = (Math.max(40, alpha) << 24) | 0x00E6E6F0;
            drawContext.fill((int) caretX, (int) (inputY + 4), (int) caretX + 1, (int) (inputY + 4 + Fonts.REGULAR.getHeight(7f)), color);
        }

        float listWidth = 220f;
        float listHeight = 120f;
        float listX = this.width / 2f - listWidth / 2f;
        float listY = this.height / 2f - listHeight / 2f - 22f;
        new RectangleBuilder()
                .size(new SizeState(listWidth, listHeight))
                .radius(new QuadRadiusState(8))
                .color(new QuadColorState(new Color(30, 30, 38, 200)))
                .build()
                .render(matrix, listX, listY, 0f);

        int rowHeight = 16;

        float scrollSmooth = 0.18f;
        listScroll += (listScrollTarget - listScroll) * scrollSmooth;
        listScroll = clamp(listScroll, 0f, listMaxScroll);

        hoveredRowIndex = -1;
        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            hoveredRowIndex = (int) (((mouseY - listY) + listScroll) / rowHeight);
        }
        listHoverAnimation.update(hoveredRowIndex >= 0 && hoveredRowIndex < filteredAlts.size());
        float hoverT = listHoverAnimation.getValue();

        float contentHeight = filteredAlts.size() * rowHeight;
        listMaxScroll = Math.max(0f, contentHeight - listHeight);

        float visibleBottom = listY + listHeight;

        int sx1 = Math.round(listX) + 1;
        int sy1 = Math.round(listY) + 1;
        int sx2 = Math.round(listX + listWidth) - 1;
        int sy2 = Math.round(listY + listHeight) - 1;
        drawContext.enableScissor(sx1, sy1, sx2, sy2);
        for (int i = 0; i < filteredAlts.size(); i++) {
            float rowY = listY + i * rowHeight - listScroll;
            if (rowY + rowHeight < listY) continue;
            if (rowY > visibleBottom) break;
            boolean sel = i == selectedIndex;
            if (sel) {
                new RectangleBuilder()
                        .size(new SizeState(listWidth - 8, rowHeight))
                        .radius(new QuadRadiusState(6))
                        .color(new QuadColorState(new Color(60, 60, 80, 120)))
                        .build()
                        .render(matrix, listX + 4, rowY, 0f);
            }

            if (i == hoveredRowIndex && !sel) {
                int ha = (int) (80 + 70 * hoverT);
                new RectangleBuilder()
                        .size(new SizeState(listWidth - 8, rowHeight))
                        .radius(new QuadRadiusState(6))
                        .color(new QuadColorState(new Color(50, 60, 90, ha)))
                        .build()
                        .render(matrix, listX + 4, rowY, 0f);
            }
            boolean isActive = AltManager.getLastUsedNickname() != null && filteredAlts.get(i).equalsIgnoreCase(AltManager.getLastUsedNickname());
            new TextBuilder()
                    .font(Fonts.REGULAR.font())
                    .text(filteredAlts.get(i))
                    .size(7f)
                    .color(isActive ? new Color(90, 140, 255) : new Color(230, 230, 240))
                    .smoothness(0.5f)
                    .build()
                    .render(matrix, listX + 6, rowY + 4, 0f);
        }
        drawContext.disableScissor();

            for (MainMenu.AnimatedButton btn : buttons) {
                btn.render(drawContext, mouseX, mouseY, delta, 1.0f);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    private void moveCaret(int delta, boolean keepSelection) {
        caretIndex = Math.max(0, Math.min(caretIndex, inputBuffer.length()));
        int newIndex = Math.max(0, Math.min(inputBuffer.length(), caretIndex + delta));
        if (keepSelection) {
            if (selectionStart == -1) selectionStart = caretIndex;
            caretIndex = newIndex;
        } else {
            caretIndex = newIndex;
            selectionStart = -1;
        }
    }

    private void insertText(String text) {
        if (text == null || text.isEmpty()) return;

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'A' && c <= 'Z') ||
                (c >= 'a' && c <= 'z') ||
                (c >= '0' && c <= '9') ||
                (c >= 'А' && c <= 'Я') ||
                (c >= 'а' && c <= 'я') ||
                c == 'Ё' || c == 'ё' ||
                c == '_' || c == '.' || c == '-') {
                sb.append(c);
            }
        }
        String filtered = sb.toString();
        if (filtered.isEmpty()) return;
        deleteSelection();
        caretIndex = Math.max(0, Math.min(caretIndex, inputBuffer.length()));
        String left = inputBuffer.substring(0, caretIndex);
        String right = inputBuffer.substring(caretIndex);
        inputBuffer = left + filtered + right;
        caretIndex += filtered.length();
        selectionStart = -1;
    }

    private boolean deleteSelection() {
        if (selectionStart != -1 && selectionStart != caretIndex) {
            int a = Math.max(0, Math.min(Math.min(selectionStart, caretIndex), inputBuffer.length()));
            int b = Math.max(0, Math.min(Math.max(selectionStart, caretIndex), inputBuffer.length()));
            if (a < b) {
                inputBuffer = inputBuffer.substring(0, a) + inputBuffer.substring(b);
                caretIndex = a;
            }
            selectionStart = -1;
            return true;
        }
        selectionStart = -1;
        return false;
    }

    private String getSelectedText() {
        if (selectionStart != -1 && selectionStart != caretIndex) {
            int a = Math.max(0, Math.min(Math.min(selectionStart, caretIndex), inputBuffer.length()));
            int b = Math.max(0, Math.min(Math.max(selectionStart, caretIndex), inputBuffer.length()));
            if (a < b) {
                return inputBuffer.substring(a, b);
            }
        }
        return null;
    }

    private int getCaretIndexByMouse(float mouseX, float inputX) {

        int idx = 0;
        float target = mouseX - (inputX + 6);
        if (target <= 0) return 0;
        float lastWidth = 0f;
        for (int i = 1; i <= inputBuffer.length(); i++) {
            float w = Fonts.REGULAR.getWidth(inputBuffer.substring(0, i), 7f);
            if (w >= target) {

                return Math.max(0, (target - lastWidth) < (w - target) ? i - 1 : i);
            }
            lastWidth = w;
            idx = i;
        }
        return Math.max(0, Math.min(idx, inputBuffer.length()));
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private void filterAlts() {
        filteredAlts.clear();
        if (searchBuffer.isEmpty()) {
            filteredAlts.addAll(alts);
        } else {
            String searchLower = searchBuffer.toLowerCase();
            for (String alt : alts) {
                if (alt.toLowerCase().contains(searchLower)) {
                    filteredAlts.add(alt);
                }
            }
        }
    }
}

