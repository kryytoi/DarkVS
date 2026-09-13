package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.animations.infinity.InfinityAnimation;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.awt.*;

public class HotbarHUD extends HudElement {

    private final ThemeManager themeManager;

     
    private final float barWidth = 180f;
    private final float barHeight = 20f;
    private final float slotSize = 20f;
    private final float slotPadding = 2f;  
    private final float radius = 2f;
    private float xpBarHeight = 6f;  

     
    private static final Color BACKGROUND = new Color(30, 30, 30, 240);
    private static final Color XP_BG = new Color(0, 0, 0, 120);
    private static final Color XP_FG = new Color(80, 200, 120, 220);

     
    private static final Identifier HEART_CONTAINER       = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier HEART_FULL            = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF            = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier HEART_ABSORBING_FULL  = Identifier.ofVanilla("hud/heart/absorbing_full");
    private static final Identifier HEART_ABSORBING_HALF  = Identifier.ofVanilla("hud/heart/absorbing_half");
    private static final Identifier FOOD_EMPTY            = Identifier.ofVanilla("hud/food_empty");
    private static final Identifier FOOD_FULL             = Identifier.ofVanilla("hud/food_full");
    private static final Identifier FOOD_HALF             = Identifier.ofVanilla("hud/food_half");

     
    private final InfinityAnimation xpAnimPx = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation selAnimX = new InfinityAnimation(Easing.OUT_QUAD);
    private float lastSelX = -1f;

    public HotbarHUD() {
        super("Hotbar");
        this.themeManager = ThemeManager.getInstance();
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        if (mc.player != null && mc.player.isSpectator()) return;

        var context = e.getContext();
        var matrices = context.getMatrices();
        float baseX = getX();
        float baseY = getY();

         
        Color background = BACKGROUND;
         
        Color selected = themeManager.getCurrentTheme().getAccentColor();

         
        setBounds(baseX, baseY, barWidth, barHeight);

         
        Render2D.drawHudBackground(
                matrices,
                getX(), getY(),
                barWidth, barHeight,
                radius,
                1f
        );

         
        if (mc.player != null && !mc.player.getAbilities().creativeMode) {
            int iconSize = 9;    
            int iconStep = 8;    
            float rowH = 10f;
            this.xpBarHeight = 6f;

            float x = baseX;
            float w = barWidth;

             
            float barsY = baseY - (rowH + 6f + xpBarHeight);

             
            float health = mc.player.getHealth();
            float maxHealth = mc.player.getMaxHealth();
            float absorption = mc.player.getAbsorptionAmount();

            int healthInt = (int) Math.ceil(health);
            int maxHearts = (int) Math.ceil(maxHealth / 2f);
            int absorptionInt = (int) Math.ceil(absorption);

            for (int i = 0; i < maxHearts; i++) {
                int row = i / 10;
                int col = i % 10;
                int hx = (int) (x + col * iconStep);
                int hy = (int) (barsY - row * (iconSize + 1));

                context.drawGuiTexture(RenderLayer::getGuiTextured, HEART_CONTAINER, hx, hy, iconSize, iconSize);

                int heartValue = i * 2;
                if (healthInt > heartValue + 1) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, HEART_FULL, hx, hy, iconSize, iconSize);
                } else if (healthInt == heartValue + 1) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, HEART_HALF, hx, hy, iconSize, iconSize);
                }
            }

             
            if (absorptionInt > 0) {
                int absHearts = (int) Math.ceil(absorptionInt / 2f);
                int healthRows = (maxHearts + 9) / 10;
                for (int i = 0; i < absHearts; i++) {
                    int row = i / 10;
                    int col = i % 10;
                    int hx = (int) (x + col * iconStep);
                    int hy = (int) (barsY - (healthRows + row) * (iconSize + 1));

                    context.drawGuiTexture(RenderLayer::getGuiTextured, HEART_CONTAINER, hx, hy, iconSize, iconSize);

                    int absValue = i * 2;
                    if (absorptionInt > absValue + 1) {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, HEART_ABSORBING_FULL, hx, hy, iconSize, iconSize);
                    } else if (absorptionInt == absValue + 1) {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, HEART_ABSORBING_HALF, hx, hy, iconSize, iconSize);
                    }
                }
            }

             
            int hunger = mc.player.getHungerManager().getFoodLevel();  
            for (int i = 0; i < 10; i++) {
                int fx = (int) (x + w - (i + 1) * iconStep - 1);
                int fy = (int) barsY;

                context.drawGuiTexture(RenderLayer::getGuiTextured, FOOD_EMPTY, fx, fy, iconSize, iconSize);

                int foodValue = i * 2;
                if (hunger > foodValue + 1) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, FOOD_FULL, fx, fy, iconSize, iconSize);
                } else if (hunger == foodValue + 1) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, FOOD_HALF, fx, fy, iconSize, iconSize);
                }
            }

             
            float xp = mc.player.experienceProgress;  
            int level = mc.player.experienceLevel;
            float xpY = barsY + rowH + 4f;
            Render2D.drawRoundedRect(matrices, x, xpY, w, xpBarHeight, 2f, XP_BG);
            float xpTargetPx = w * Math.min(1f, Math.max(0f, xp));
            float xpFill = Math.max(0f, Math.min(w, xpAnimPx.animate(xpTargetPx, 110)));
            Render2D.drawRoundedRect(matrices, x, xpY, xpFill, xpBarHeight, 2f, XP_FG);
             
            String lvl = String.valueOf(level);
            float lvlSize = 8.5f;
            var fontLvl = Fonts.REGULAR.getFont(lvlSize);
            float lvlW = Fonts.REGULAR.getWidth(lvl, lvlSize);
            float lvlH = Fonts.REGULAR.getHeight(lvlSize);
            float lvlX = x + (w - lvlW) / 2f;
            float lvlY = xpY + (xpBarHeight - lvlH) / 2f + 0.3f;
            Render2D.drawFont(matrices, fontLvl, lvl, lvlX, lvlY, Color.WHITE);
        }

         
        if (mc.player != null) {
            var inv = mc.player.getInventory();
            int slot = inv.selectedSlot;  
             
            float sideGap = (barWidth - slotSize * 9f) / 2f;
             
            float slotXTarget = baseX + sideGap + slot * slotSize;
            float slotY = baseY;
            float slotW = slotSize;
            float slotH = barHeight;

             
            float selRadius = 2f;
             
            float selY = slotY;
            float selH = slotH;
             
             
            if (lastSelX < 0f) lastSelX = slotXTarget;
            float selX = selAnimX.animate(slotXTarget, 140);
            lastSelX = selX;
            float selW = (slot == 8) ? Math.max(slotW, getX() + barWidth - slotXTarget) : slotW;

             
            Render2D.drawRoundedRect(
                    matrices,
                    selX, selY,
                    selW, selH,
                    selRadius,
                    new Color(selected.getRed(), selected.getGreen(), selected.getBlue(), 160)
            );
             

            boolean isMainHandLeft = mc.player.getMainArm().equals(net.minecraft.util.Arm.LEFT);
            float offhandX = isMainHandLeft ? baseX - slotSize - 4f : baseX + barWidth + 4f;

            var offhandStack = inv.getStack(40);
            context.drawItem(offhandStack, (int) offhandX, (int) baseY);
            if (!offhandStack.isEmpty()) {
                String offhandCnt = String.valueOf(offhandStack.getCount());
                float offhandCntSize = 7.5f;
                var offhandCntFont = Fonts.REGULAR.getFont(offhandCntSize);
                float offhandCntW = Fonts.REGULAR.getWidth(offhandCnt, offhandCntSize);
                float offhandCntH = Fonts.REGULAR.getHeight(offhandCntSize);
                float offhandCntX = offhandX + slotSize - 2f - offhandCntW;
                float offhandCntY = baseY + barHeight - 2f - offhandCntH + 0.5f;

                Render2D.drawHudBackground(
                        matrices,
                        offhandX, baseY,
                        20, 20,
                        radius,
                        1f
                );

                context.drawItem(offhandStack, (int) offhandX + 2, (int) baseY + 2);

                if(offhandStack.getCount() > 1) Render2D.drawFont(matrices, offhandCntFont, offhandCnt, offhandCntX, offhandCntY, Color.WHITE);
            }

             
            for (int i = 0; i <= 8; i++) {
                float ix = baseX + sideGap + i * slotSize + 2f;     
                float iy = baseY + 2f;
                var stack = inv.getStack(i);
                context.drawItem(stack, (int) ix, (int) iy);
                 
                 
                String slotNum = String.valueOf(i + 1);
                float numSize = 6.0f;
                var numFont = Fonts.REGULAR.getFont(numSize);
                float numH = Fonts.REGULAR.getHeight(numSize);
                float slotLeftX = baseX + sideGap + i * slotSize + 2f;  
                float slotTopY = baseY + 2f;
                Render2D.drawFont(matrices, numFont, slotNum, slotLeftX, slotTopY - 0.5f, Color.WHITE);

                 
                if (!stack.isEmpty()) {
                    String cnt = String.valueOf(stack.getCount());
                    float cntSize = 7.5f;
                    var cntFont = Fonts.REGULAR.getFont(cntSize);
                    float cntW = Fonts.REGULAR.getWidth(cnt, cntSize);
                    float cntH = Fonts.REGULAR.getHeight(cntSize);
                    float slotRightX = baseX + sideGap + i * slotSize + slotSize - 2f;  
                    float slotBottomY = baseY + barHeight - 2f;  
                    float drawX = slotRightX - cntW;
                    float drawY = slotBottomY - cntH + 0.5f;
                    if(stack.getCount() > 1) Render2D.drawFont(matrices, cntFont, cnt, drawX, drawY, Color.WHITE);
                }
            }
        }

        super.onRender2D(e);
    }
}

