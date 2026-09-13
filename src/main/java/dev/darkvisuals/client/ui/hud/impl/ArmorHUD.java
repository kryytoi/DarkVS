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
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;

public class ArmorHUD extends HudElement implements ThemeManager.ThemeChangeListener {

    private final InfinityAnimation fadeAnimation = new InfinityAnimation(Easing.BOTH_SINE);

     
    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color lowDurabilityColor;
    private Color headerTextColor;

     
    private final Map<Integer, InfinityAnimation> slotAlpha = new HashMap<>();
     
    private final Map<Integer, ItemStack> lastSlotItem = new HashMap<>();

     
    private final ListSetting layoutModes = new ListSetting(
            "setting.layout",
            new BooleanSetting("setting.horizontal", true),
            new BooleanSetting("setting.vertical", false)
    );

    public ArmorHUD() {
        super("ArmorHUD");

        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);

         
        for (int i = 0; i < 4; i++) {
            slotAlpha.put(i, new InfinityAnimation(Easing.OUT_QUAD));
            lastSlotItem.put(i, ItemStack.EMPTY);
        }

        layoutModes.setSingleSelect(true);
        getSettings().add(layoutModes);
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

         
        int brightness = (int) (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue());
        if (brightness > 200 && bgColor.getAlpha() <= 150) {
            this.textColor = new Color(0, 0, 0, 255);
        } else {
            this.textColor = Color.WHITE;
        }

        this.headerTextColor = this.textColor;  
        this.lowDurabilityColor = new Color(200, 80, 80, 220);
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        Perf.tryBeginFrame();
        try (var __ = Perf.scopeCpu("ArmorHUD.onRender2D")) {

            ClientPlayerEntity player = mc.player;
            if (player == null) return;

            List<ItemStack> armorItems = new ArrayList<>();
            List<ItemStack> armorSlots = player.getInventory().armor;
            for (int i = armorSlots.size() - 1; i >= 0; i--) {
                ItemStack stack = armorSlots.get(i);
                armorItems.add(stack.isEmpty() ? ItemStack.EMPTY : stack);
            }

            boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
            boolean allEmpty = armorItems.stream().allMatch(ItemStack::isEmpty);
            boolean previewMode = chatOpen && allEmpty;

             
            if (chatOpen) {
                fadeAnimation.animate(1f, 160);
                previewMode = true;
            } else if (allEmpty && !previewMode) {
                fadeAnimation.animate(0f, 160);
                if (fadeAnimation.getValue() <= 0) return;
            } else {
                fadeAnimation.animate(1f, 160);
            }

             
            for (int i = 0; i < 4; i++) {
                ItemStack presentStack = (i < armorItems.size() ? armorItems.get(i) : ItemStack.EMPTY);
                boolean present = previewMode || !presentStack.isEmpty();
                if (!presentStack.isEmpty()) lastSlotItem.put(i, presentStack);
                slotAlpha.get(i).animate(present ? 1f : 0f, 150);
            }

            float posX = getX();
            float posY = getY();
            float iconSize = 18f;
            float padding = 3f;
            float fontSize = 6f;
            int alpha = (int) (200 * fadeAnimation.getValue());

            float totalWidth = 0f;
            float totalHeight = 0f;

            e.getContext().getMatrices().push();

            float cursorY = posY;
            float cursorX = posX;

            if (HudStyle.isMinimalistic()) {
                renderMinimalistic(e, armorItems, previewMode, posX, posY, fadeAnimation.getValue());
                e.getContext().getMatrices().pop();
                super.onRender2D(e);
                return;
            }

            boolean drawHorizontal = layoutModes.getName("setting.horizontal") != null && layoutModes.getName("setting.horizontal").getValue();
            boolean drawVertical = layoutModes.getName("setting.vertical") != null && layoutModes.getName("setting.vertical").getValue();

             
            if (!drawHorizontal && !drawVertical) drawHorizontal = true;

            boolean rightAnchored = getX() > mc.getWindow().getScaledWidth() / 2f;
            float sideGap = 3f;

            if (drawHorizontal) {
                float widthH = 0f;
                float textHeight = Fonts.MEDIUM.getHeight(fontSize);
                float heightH = iconSize + textHeight + sideGap;
                for (int i = 0; i < 4; i++) {
                    boolean presentNow = previewMode || (i < armorItems.size() && !armorItems.get(i).isEmpty());
                    ItemStack renderStack = (previewMode ? new ItemStack(
                            i == 0 ? net.minecraft.item.Items.DIAMOND_HELMET :
                            i == 1 ? net.minecraft.item.Items.DIAMOND_CHESTPLATE :
                            i == 2 ? net.minecraft.item.Items.DIAMOND_LEGGINGS :
                            net.minecraft.item.Items.DIAMOND_BOOTS
                    ) : (i < armorItems.size() ? armorItems.get(i) : ItemStack.EMPTY));
                    if (renderStack.isEmpty()) renderStack = lastSlotItem.getOrDefault(i, ItemStack.EMPTY);
                    float xOffset = cursorX + widthH;

                    float a = slotAlpha.get(i).getValue();
                    if (a <= 0.01f) { if (!presentNow) lastSlotItem.put(i, ItemStack.EMPTY); widthH += iconSize + padding; continue; }
                    int itemAlpha = (int) (255 * a);
                    float appearYOffset = (1f - a) * 4f;

                    if (!renderStack.isEmpty()) {
                        Render2D.drawHudBackground(
                                e.getContext().getMatrices(),
                                xOffset - 1f,
                                cursorY - 1f + appearYOffset,
                                iconSize + 2f,
                                iconSize + 2f,
                                4f,
                                a
                        );

                        String durabilityText = renderStack.isEmpty() ? "0%" :
                                (int)(((float)(renderStack.getMaxDamage() - renderStack.getDamage()) / renderStack.getMaxDamage()) * 100) + "%";
                        float textW = Fonts.MEDIUM.getWidth(durabilityText, fontSize);
                        float textX = xOffset + iconSize / 2f - textW / 2f;
                        float textY = cursorY - textHeight - 1f + appearYOffset;
                        Render2D.drawFont(
                                e.getContext().getMatrices(),
                                Fonts.MEDIUM.getFont(fontSize),
                                durabilityText,
                                textX,
                                textY,
                                new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), itemAlpha)
                        );

                        String itemId = Registries.ITEM.getId(renderStack.getItem()).getPath();
                        Identifier texture = Identifier.of("minecraft", "textures/item/" + itemId + ".png");
                        try {
                            Render2D.drawTexture(
                                    e.getContext().getMatrices(),
                                    xOffset,
                                    cursorY + appearYOffset,
                                    iconSize,
                                    iconSize,
                                    2f, 0f, 0f, 1f, 1f,
                                    texture,
                                    new Color(255, 255, 255, itemAlpha)
                            );
                        } catch (Exception ex) {
                            Render2D.drawFont(
                                    e.getContext().getMatrices(),
                                    Fonts.MEDIUM.getFont(fontSize),
                                    "[No Icon]",
                                    xOffset,
                                    cursorY + appearYOffset,
                                    new Color(255, 50, 50, itemAlpha)
                            );
                        }
                    }
                    if (!presentNow && a <= 0.02f) lastSlotItem.put(i, ItemStack.EMPTY);
                    widthH += iconSize + padding;
                }
                totalWidth = Math.max(totalWidth, widthH - padding);
                totalHeight += heightH + padding;  
                cursorY += heightH + padding;
            }

            if (drawVertical) {
                float widthV = iconSize;
                float heightV = 0f;
                for (int i = 0; i < 4; i++) {
                    boolean presentNow = previewMode || (i < armorItems.size() && !armorItems.get(i).isEmpty());
                    ItemStack renderStack = (previewMode ? new ItemStack(
                            i == 0 ? net.minecraft.item.Items.DIAMOND_HELMET :
                            i == 1 ? net.minecraft.item.Items.DIAMOND_CHESTPLATE :
                            i == 2 ? net.minecraft.item.Items.DIAMOND_LEGGINGS :
                            net.minecraft.item.Items.DIAMOND_BOOTS
                    ) : (i < armorItems.size() ? armorItems.get(i) : ItemStack.EMPTY));
                    if (renderStack.isEmpty()) renderStack = lastSlotItem.getOrDefault(i, ItemStack.EMPTY);
                    float yOffset = cursorY + heightV;

                    float a = slotAlpha.get(i).getValue();
                    if (a <= 0.01f) { if (!presentNow) lastSlotItem.put(i, ItemStack.EMPTY); heightV += iconSize + padding; continue; }
                    int itemAlpha = (int) (255 * a);
                    float appearXOffset = (1f - a) * (rightAnchored ? -4f : 4f);

                    if (!renderStack.isEmpty()) {
                        Render2D.drawHudBackground(
                                e.getContext().getMatrices(),
                                cursorX - 1f + appearXOffset,
                                yOffset - 1f,
                                iconSize + 2f,
                                iconSize + 2f,
                                4f,
                                a
                        );

                        String durabilityText = renderStack.isEmpty() ? "0%" :
                                (int)(((float)(renderStack.getMaxDamage() - renderStack.getDamage()) / renderStack.getMaxDamage()) * 100) + "%";
                        float textW = Fonts.MEDIUM.getWidth(durabilityText, fontSize);
                        float textX = rightAnchored ? (cursorX - sideGap - textW) : (cursorX + iconSize + sideGap);
                        float textY = yOffset + iconSize / 2f - Fonts.MEDIUM.getHeight(fontSize) / 2f;
                        Render2D.drawFont(
                                e.getContext().getMatrices(),
                                Fonts.MEDIUM.getFont(fontSize),
                                durabilityText,
                                textX + appearXOffset,
                                textY,
                                new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), itemAlpha)
                        );

                        String itemId = Registries.ITEM.getId(renderStack.getItem()).getPath();
                        Identifier texture = Identifier.of("minecraft", "textures/item/" + itemId + ".png");
                        try {
                            Render2D.drawTexture(
                                    e.getContext().getMatrices(),
                                    cursorX + appearXOffset,
                                    yOffset,
                                    iconSize,
                                    iconSize,
                                    2f, 0f, 0f, 1f, 1f,
                                    texture,
                                    new Color(255, 255, 255, itemAlpha)
                            );
                        } catch (Exception ex) {
                            Render2D.drawFont(
                                    e.getContext().getMatrices(),
                                    Fonts.MEDIUM.getFont(fontSize),
                                    "[No Icon]",
                                    cursorX + appearXOffset,
                                    yOffset,
                                    new Color(255, 50, 50, itemAlpha)
                            );
                        }
                    }
                    if (!presentNow && a <= 0.02f) lastSlotItem.put(i, ItemStack.EMPTY);
                    heightV += iconSize + padding;
                }
                totalWidth = Math.max(totalWidth, widthV);
                totalHeight += heightV - padding;
            }

            e.getContext().getMatrices().pop();

            if (totalWidth <= 0f) totalWidth = iconSize * 4 + padding * 3;
            if (totalHeight <= 0f) totalHeight = iconSize;

            setBounds(posX, posY, totalWidth, totalHeight);
            super.onRender2D(e);
        }
    }

    @Override
    public void onMouse(dev.darkvisuals.client.events.impl.EventMouse e) {
         
        if (!(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) || fullNullCheck()) return;

         
        super.onMouse(e);
    }

    private String formatDurability(ItemStack stack) {
        if (!stack.isDamageable()) return "∞";
        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamage();
        return (maxDamage - currentDamage) + "/" + maxDamage;
    }

    private void renderMinimalistic(EventRender2D e, List<ItemStack> armorItems, boolean previewMode,
                                    float posX, float posY, float fade) {
        float iconSize = 16f;
        float pad = 6f;
        float gap = 6f;
        float fontSize = 6f;
        float textH = Fonts.MEDIUM.getHeight(fontSize);
        float textGap = 3f;

        float contentW = iconSize * 4f + gap * 3f;
        float totalW = pad * 2f + contentW;
        float totalH = pad + iconSize + textGap + textH + pad;

        Render2D.drawHudBackground(e.getContext().getMatrices(), posX, posY, totalW, totalH, 8f, fade);

        float cursorX = posX + pad;
        float iconY = posY + pad;

        for (int i = 0; i < 4; i++) {
            ItemStack renderStack = previewMode ? new ItemStack(
                    i == 0 ? net.minecraft.item.Items.DIAMOND_HELMET :
                    i == 1 ? net.minecraft.item.Items.DIAMOND_CHESTPLATE :
                    i == 2 ? net.minecraft.item.Items.DIAMOND_LEGGINGS :
                    net.minecraft.item.Items.DIAMOND_BOOTS
            ) : (i < armorItems.size() ? armorItems.get(i) : ItemStack.EMPTY);
            if (renderStack.isEmpty()) renderStack = lastSlotItem.getOrDefault(i, ItemStack.EMPTY);

            float a = slotAlpha.get(i).getValue();
            if (a > 0.01f && !renderStack.isEmpty()) {
                int itemAlpha = (int) (255 * a * fade);
                e.getContext().drawItem(renderStack, Math.round(cursorX), Math.round(iconY));

                String durabilityText = !renderStack.isDamageable() ? "100%" :
                        (int) (((float) (renderStack.getMaxDamage() - renderStack.getDamage()) / renderStack.getMaxDamage()) * 100) + "%";
                float textW = Fonts.MEDIUM.getWidth(durabilityText, fontSize);
                float textX = cursorX + iconSize / 2f - textW / 2f;
                float textY = iconY + iconSize + textGap;
                Render2D.drawFont(
                        e.getContext().getMatrices(),
                        Fonts.MEDIUM.getFont(fontSize),
                        durabilityText,
                        textX,
                        textY,
                        new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), itemAlpha)
                );
            }
            cursorX += iconSize + gap;
        }

        setBounds(posX, posY, totalW, totalH);
    }
}
