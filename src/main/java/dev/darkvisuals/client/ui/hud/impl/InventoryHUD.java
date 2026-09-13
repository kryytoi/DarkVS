package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.awt.*;

public class InventoryHUD extends HudElement {

    private final ThemeManager themeManager;

     
    private static final int COLS = 9;
    private static final int ROWS = 3;
    private static final float SLOT = 18f;     
    private static final float PADDING = 3f;   
    private static final float RADIUS = 4f;    

     
    private static final Color SLOT_BG = new Color(255, 255, 255, 22);

    public InventoryHUD() {
        super("Inventory");
        this.themeManager = ThemeManager.getInstance();
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        var context = e.getContext();
        var matrices = context.getMatrices();

        float baseX = getX();
        float baseY = getY();

        float gridW = COLS * SLOT;
        float gridH = ROWS * SLOT;
        float panelW = gridW + PADDING * 2f;
        float panelH = gridH + PADDING * 2f;

        setBounds(baseX, baseY, panelW, panelH);

         
        Render2D.drawHudBackground(matrices, baseX, baseY, panelW, panelH, RADIUS, 1f);

        boolean previewMode = mc.player == null;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                float cellX = baseX + PADDING + col * SLOT;
                float cellY = baseY + PADDING + row * SLOT;

                 
                Render2D.drawRoundedRect(matrices, cellX, cellY, SLOT - 1f, SLOT - 1f, 2f, SLOT_BG);

                 
                int slotIndex = 9 + row * COLS + col;
                ItemStack stack = previewMode
                        ? previewStack(row, col)
                        : mc.player.getInventory().getStack(slotIndex);

                if (stack == null || stack.isEmpty()) continue;

                 
                int itemX = Math.round(cellX + (SLOT - 16f) / 2f);
                int itemY = Math.round(cellY + (SLOT - 16f) / 2f);
                context.drawItem(stack, itemX, itemY);

                 
                if (stack.getCount() > 1) {
                    String cnt = String.valueOf(stack.getCount());
                    float cntSize = 7.0f;
                    var cntFont = Fonts.REGULAR.getFont(cntSize);
                    float cntW = Fonts.REGULAR.getWidth(cnt, cntSize);
                    float cntH = Fonts.REGULAR.getHeight(cntSize);
                    float cntX = cellX + SLOT - 2f - cntW;
                    float cntY = cellY + SLOT - 2f - cntH + 0.5f;
                    Render2D.drawFont(matrices, cntFont, cnt, cntX, cntY, Color.WHITE);
                }
            }
        }

        super.onRender2D(e);
    }

     
    private ItemStack previewStack(int row, int col) {
        return switch (row * COLS + col) {
            case 0 -> new ItemStack(Items.OAK_LOG, 64);
            case 1 -> new ItemStack(Items.STONE, 64);
            case 2 -> new ItemStack(Items.COBBLESTONE, 16);
            case 3 -> new ItemStack(Items.DIAMOND, 64);
            case 4 -> new ItemStack(Items.DIAMOND, 64);
            case 5 -> new ItemStack(Items.DIAMOND, 20);
            case 7 -> new ItemStack(Items.GOLDEN_APPLE, 32);
            case 8 -> new ItemStack(Items.GOLDEN_APPLE, 16);
            case 9 -> new ItemStack(Items.DIRT, 64);
            case 10 -> new ItemStack(Items.DIRT, 64);
            case 11 -> new ItemStack(Items.GRAVEL, 18);
            case 12 -> new ItemStack(Items.OBSIDIAN, 64);
            case 17 -> new ItemStack(Items.ENDER_PEARL, 3);
            case 18 -> new ItemStack(Items.WATER_BUCKET);
            case 19 -> new ItemStack(Items.COOKED_BEEF, 64);
            case 20 -> new ItemStack(Items.COOKED_BEEF, 36);
            case 22 -> new ItemStack(Items.SPLASH_POTION);
            case 24 -> new ItemStack(Items.POTION);
            default -> ItemStack.EMPTY;
        };
    }
}
