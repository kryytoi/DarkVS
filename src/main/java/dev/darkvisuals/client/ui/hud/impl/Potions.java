package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.ui.hud.HudStyle;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.animations.infinity.InfinityAnimation;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Potions extends HudElement implements ThemeManager.ThemeChangeListener {

	private static final Identifier ICON_POTION = Identifier.of("darkvisuals", "textures/hud/potion.png");

	private final InfinityAnimation hudFade = new InfinityAnimation(Easing.BOTH_SINE);
	private final InfinityAnimation heightAnim = new InfinityAnimation(Easing.OUT_QUAD);
	private final InfinityAnimation widthAnim = new InfinityAnimation(Easing.OUT_QUAD);

	 
	private final Map<String, InfinityAnimation> itemAlpha = new LinkedHashMap<>();
	 
	private final Map<String, String> lastText = new HashMap<>();
	private final Map<String, String> lastIconKey = new HashMap<>();

	private final ThemeManager themeManager;
	private Color bgColor;
	private Color textColor;
	private Color negativeColor;
	private Color highlightColor;

	public final BooleanSetting showNegative = new BooleanSetting("ShowNegative", true);
	public final BooleanSetting highlightLowDuration = new BooleanSetting("HighlightLowDuration", true);

	public Potions() {
		super("Potions");
		this.themeManager = ThemeManager.getInstance();
		applyTheme(themeManager.getCurrentTheme());
		themeManager.addThemeChangeListener(this);
		this.getSettings().add(showNegative);
		this.getSettings().add(highlightLowDuration);
	}

	@Override
	public void onDisable() {
		themeManager.removeThemeChangeListener(this);
		super.onDisable();
	}

	@Override
	public void onThemeChanged(ThemeManager.Theme theme) {
		applyTheme(theme);
	}

	private void applyTheme(ThemeManager.Theme theme) {
		this.bgColor = new Color(30, 30, 30, 240);
		this.textColor = Color.WHITE;
		this.negativeColor = new Color(200, 80, 80, 220);
		this.highlightColor = theme.getAccentColor();
	}

	@Override
	public void onRender2D(EventRender2D e) {
		if (fullNullCheck() || closed()) return;
		Perf.tryBeginFrame();
		try (var __ = Perf.scopeCpu("Potions.onRender2D")) {
			ClientPlayerEntity player = mc.player;
			if (player == null) return;

			Collection<StatusEffectInstance> raw = player.getStatusEffects();
			boolean hasAny = !raw.isEmpty();
			boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
			 
			int contentHudAlpha = 255;

			 
			List<StatusEffectInstance> effects = new ArrayList<>();
			for (StatusEffectInstance eff : raw) {
				StatusEffect type = eff.getEffectType().value();
				if (!type.isBeneficial() && !showNegative.getValue()) continue;
				effects.add(eff);
			}
			effects.sort(Comparator.comparing(a -> a.getEffectType().value().getName().getString()));

			 
			List<String> keys = new ArrayList<>();
			List<String> texts = new ArrayList<>();
			List<String> icons = new ArrayList<>();
			for (StatusEffectInstance eff : effects) {
				StatusEffect type = eff.getEffectType().value();
				String name = type.getName().getString();
				String level = eff.getAmplifier() > 0 ? " " + toRoman(eff.getAmplifier() + 1) : "";
				String display = name + level;
				Identifier rid = Registries.STATUS_EFFECT.getId(type);
				String effectKey = rid == null ? "" : rid.getPath();
				String key = effectKey;  
				keys.add(key);
				texts.add(display);
				icons.add(effectKey);
				lastText.put(key, display);
				lastIconKey.put(key, effectKey);
			}

			 
			boolean previewMode = chatOpen && keys.isEmpty();
			if (previewMode) {
				keys.add("speed");
				texts.add("Speed II");
				icons.add("speed");
			}

			 
			for (String k : itemAlpha.keySet()) {
				boolean active = keys.contains(k);
				 
				itemAlpha.get(k).animate(active ? 1f : 0f, (active ? 220 : (hasAny ? 160 : 0)));
			}
			for (String k : keys) {
				itemAlpha.computeIfAbsent(k, kk -> new InfinityAnimation(Easing.OUT_QUAD)).animate(1f, 220);
			}

			 
			float posX = getX();
			float posY = getY();
			float uiScale = 0.90f;
			float headerH = 18.8f * uiScale;
			float spacing = 4f * uiScale;
			float rowH = 16f * uiScale;
			float pad = 4f * uiScale;
			float icon = 14f * uiScale;
			float font = 8f * uiScale;
			float titleFont = 9f * uiScale;
			float yAdjust = -3f * uiScale;

			float targetWidth = 90f;
			for (int i = 0; i < keys.size(); i++) {
				float w = icon + 2 * pad + Fonts.MEDIUM.getWidth(texts.get(i), font) + 30f * uiScale;
				targetWidth = Math.max(targetWidth, w);
			}

			 
			float visibleRows = keys.size();
			if (previewMode) visibleRows = 1f;
			float targetHeight = headerH + spacing + Math.max(0, visibleRows) * rowH;

			 
			heightAnim.animate(targetHeight, (hasAny || previewMode) ? 140 : 100);
			widthAnim.animate(targetWidth, 220);
			float currentHeight = heightAnim.getValue();
			float currentWidth = widthAnim.getValue();

			 
			boolean nothingVisible = false;

			 
			setBounds(getX(), getY(), currentWidth, Math.max(headerH + spacing, currentHeight));
			if (nothingVisible) {
				super.onRender2D(e);
				return;
			}

			e.getContext().getMatrices().push();
			var matrices = e.getContext().getMatrices();
			float radius = 9f * uiScale;

			if (HudStyle.isMinimalistic()) {
				renderMinimalistic(matrices, posX, posY, currentWidth, currentHeight, headerH, spacing, rowH, pad, icon, font, titleFont,
						keys, texts, icons, effects, previewMode, contentHudAlpha, uiScale, yAdjust);
				e.getContext().getMatrices().pop();
				super.onRender2D(e);
				return;
			}

			 
			Render2D.drawHudBackground(matrices, posX, posY, currentWidth, currentHeight, radius, 1f);

			 
			String titleStr = net.minecraft.client.resource.language.I18n.translate("hud.potions.title");
			float titleX = posX + currentWidth / 2f - Fonts.MEDIUM.getWidth(titleStr, titleFont) / 2f;
			float titleY = posY + headerH / 2f - Fonts.MEDIUM.getHeight(titleFont) / 2f;
			drawGlassText(matrices, titleFont, titleStr, titleX, titleY, new Color(255, 255, 255, contentHudAlpha));

			float curY = posY + headerH + spacing;

			 
			for (int i = 0; i < keys.size(); i++) {
				String k = keys.get(i);
				float a = 1f;
				float xOffset = (previewMode ? 0f : 2f);
				int alpha = (int)(contentHudAlpha * Math.max(a, 0.85f));

				StatusEffectInstance eff = previewMode ? null : effects.get(i);
				StatusEffect type = eff == null ? null : eff.getEffectType().value();
				boolean negative = type != null && !type.isBeneficial();
				boolean low = eff != null && eff.getDuration() <= 200;
				Color liveAccent = themeManager.getCurrentTheme().getAccentColor();
				Color draw = previewMode ? textColor : (low && highlightLowDuration.getValue() ? liveAccent : (negative ? negativeColor.brighter() : textColor));

				String iconKeyActive = icons.get(i) == null ? "" : icons.get(i);
				 
				float iconX = posX + pad + 2f * uiScale + xOffset;
				float rowCenterY = curY + yAdjust + rowH / 2f;
				String nameText = texts.get(i) == null ? "" : texts.get(i);
				float nameTextX = iconX + icon + pad + 1f * uiScale;
				float nameTextY = rowCenterY - Fonts.MEDIUM.getHeight(font) / 2f;

				if (!iconKeyActive.isEmpty()) {
					Identifier tex = Identifier.of("minecraft", "textures/mob_effect/" + iconKeyActive + ".png");
					Render2D.drawTexture(matrices, iconX, curY + yAdjust + (rowH - icon)/2f,
							icon, icon, 2f, 0f, 0f, 1f, 1f, tex, new Color(255, 255, 255, alpha));
				}

				if (!nameText.isBlank()) {
					drawGlassText(matrices, font, nameText, nameTextX, nameTextY,
							new Color(draw.getRed(), draw.getGreen(), draw.getBlue(), alpha));
				}

				 
				String t = previewMode ? "0:30" : (eff == null ? "" : formatDuration(eff.getDuration()));
				if (!t.isBlank()) {
					float tw = Fonts.MEDIUM.getWidth(t, font);
					float pillW = tw + 8f * uiScale;
					float pillH = rowH - 4f * uiScale;
					float rightEdge = posX + currentWidth - pad - 2f * uiScale;
					float pillX = rightEdge - pillW;
					float pillY = curY + yAdjust + 2f * uiScale;
					float pillR = pillH / 2f;
					 
					Render2D.drawHudPill(matrices, pillX, pillY, pillW, pillH, pillR, 1f);
					drawGlassText(matrices, font, t,
							pillX + (pillW - tw)/2f, rowCenterY - Fonts.MEDIUM.getHeight(font)/2f,
							new Color(255, 255, 255, alpha));
				}

				curY += rowH;
			}

			e.getContext().getMatrices().pop();
			super.onRender2D(e);
		}
	}

	private void renderMinimalistic(net.minecraft.client.util.math.MatrixStack matrices,
	                                float posX, float posY, float currentWidth, float currentHeight,
	                                float headerH, float spacing, float rowH, float pad, float icon,
	                                float font, float titleFont,
	                                List<String> keys, List<String> texts, List<String> icons,
	                                List<StatusEffectInstance> effects, boolean previewMode,
	                                int contentHudAlpha, float uiScale, float yAdjust) {
		Render2D.drawHudBackground(matrices, posX, posY, currentWidth, currentHeight, 8f, 1f);

		String titleStr = net.minecraft.client.resource.language.I18n.translate("hud.potions.title");
		float titleY = posY + headerH / 2f - Fonts.MEDIUM.getHeight(titleFont) / 2f;
		float slashW = Fonts.MEDIUM.getWidth(" / ", titleFont);
		float potIconW = titleFont + 1f;
		float titleBlockW = Fonts.MEDIUM.getWidth(titleStr, titleFont) + slashW + potIconW;
		float titleX = posX + (currentWidth - titleBlockW) / 2f;

		drawGlassText(matrices, titleFont, titleStr, titleX, titleY, new Color(255, 255, 255, contentHudAlpha));
		drawGlassText(matrices, titleFont, " / ", titleX + Fonts.MEDIUM.getWidth(titleStr, titleFont), titleY,
				new Color(180, 180, 180, contentHudAlpha));
		Render2D.drawTexture(matrices,
				titleX + Fonts.MEDIUM.getWidth(titleStr, titleFont) + slashW,
				posY + headerH / 2f - potIconW / 2f, potIconW, potIconW, 0f,
				ICON_POTION, new Color(255, 255, 255, contentHudAlpha));

		float curY = posY + headerH + spacing;
		for (int i = 0; i < keys.size(); i++) {
			StatusEffectInstance eff = previewMode ? null : effects.get(i);
			StatusEffect type = eff == null ? null : eff.getEffectType().value();
			boolean negative = type != null && !type.isBeneficial();
			boolean low = eff != null && eff.getDuration() <= 200;
			Color liveAccent = themeManager.getCurrentTheme().getAccentColor();
			Color draw = previewMode ? textColor : (low && highlightLowDuration.getValue() ? liveAccent : (negative ? negativeColor.brighter() : textColor));
			int alpha = contentHudAlpha;

			float rowPad = 3f * uiScale;
			float innerX = posX + pad;
			float innerW = currentWidth - pad * 2f;
			float innerH = rowH - 2f * uiScale;
			Render2D.drawRoundedRect(matrices, innerX, curY + yAdjust, innerW, innerH, 5f,
					new Color(0, 0, 0, 90));

			Color accentBar = negative ? negativeColor : new Color(200, 60, 60, alpha);
			Render2D.drawRoundedRect(matrices, innerX, curY + yAdjust + 2f, 2f, innerH - 4f, 1f, accentBar);

			String iconKeyActive = icons.get(i) == null ? "" : icons.get(i);
			float iconX = innerX + rowPad + 4f;
			float rowCenterY = curY + yAdjust + innerH / 2f;
			if (!iconKeyActive.isEmpty()) {
				Identifier tex = Identifier.of("minecraft", "textures/mob_effect/" + iconKeyActive + ".png");
				Render2D.drawTexture(matrices, iconX, curY + yAdjust + (innerH - icon) / 2f,
						icon, icon, 2f, 0f, 0f, 1f, 1f, tex, new Color(255, 255, 255, alpha));
			}

			String nameText = texts.get(i) == null ? "" : texts.get(i);
			float nameTextX = iconX + icon + pad;
			if (!nameText.isBlank()) {
				drawGlassText(matrices, font, nameText, nameTextX, rowCenterY - Fonts.MEDIUM.getHeight(font) / 2f,
						new Color(draw.getRed(), draw.getGreen(), draw.getBlue(), alpha));
			}

			String t = previewMode ? "0:30" : (eff == null ? "" : formatDuration(eff.getDuration()));
			if (!t.isBlank()) {
				float tw = Fonts.MEDIUM.getWidth(t, font);
				drawGlassText(matrices, font, t, posX + currentWidth - pad - tw - 4f,
						rowCenterY - Fonts.MEDIUM.getHeight(font) / 2f,
						new Color(255, 255, 255, alpha));
			}
			curY += rowH;
		}
	}

	private String toRoman(int number) {
		switch(number) {
			case 1: return "I";
			case 2: return "II";
			case 3: return "III";
			case 4: return "IV";
			case 5: return "V";
			default: return String.valueOf(number);
		}
	}

	private String formatDuration(int ticks) {
		int seconds = ticks/20;
		int minutes = seconds/60;
		seconds %= 60;
		return String.format("%d:%02d", minutes, seconds);
	}

	  
	private void drawGlassText(net.minecraft.client.util.math.MatrixStack matrices,
	                           float fontSize, String text, float x, float y, Color color) {
		Render2D.drawHudText(matrices, Fonts.MEDIUM.getFont(fontSize), text, x, y, color);
	}

	private float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}
}