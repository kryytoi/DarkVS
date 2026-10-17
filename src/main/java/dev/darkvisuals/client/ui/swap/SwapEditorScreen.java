package dev.darkvisuals.client.ui.swap;

import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.impl.utility.Swap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class SwapEditorScreen extends Screen implements Wrapper {

    private final Swap swapModule;

    private TextFieldWidget fromSearchField;
    private TextFieldWidget toSearchField;

    private List<Item> fromResults = new ArrayList<>();
    private List<Item> toResults = new ArrayList<>();

    private int fromScrollOffset = 0;
    private int toScrollOffset = 0;
    private static final int MAX_VISIBLE = 6;
    private static final int ITEM_H = 18;

    public SwapEditorScreen(Swap module) {
        super(Text.literal("Swap Editor"));
        this.swapModule = module;
    }

    @Override
    protected void init() {
        super.init();

        int panelW = 200;
        int panelH = 260;
        int panelX = (this.width - panelW * 2 - 20) / 2;
        int panelY = (this.height - panelH) / 2;

        // From search field
        fromSearchField = new TextFieldWidget(this.textRenderer,
                panelX + 5, panelY + 35, panelW - 10, 16,
                Text.literal(""));
        fromSearchField.setMaxLength(100);
        fromSearchField.setText(swapModule.getFromItem());
        fromSearchField.setChangedListener(text -> {
            fromScrollOffset = 0;
            updateFromResults(text);
        });
        addDrawableChild(fromSearchField);
        updateFromResults(swapModule.getFromItem());

        // To search field
        int toPanelX = panelX + panelW + 20;
        toSearchField = new TextFieldWidget(this.textRenderer,
                toPanelX + 5, panelY + 35, panelW - 10, 16,
                Text.literal(""));
        toSearchField.setMaxLength(100);
        toSearchField.setText(swapModule.getToItem());
        toSearchField.setChangedListener(text -> {
            toScrollOffset = 0;
            updateToResults(text);
        });
        addDrawableChild(toSearchField);
        updateToResults(swapModule.getToItem());
    }

    private void updateFromResults(String query) {
        fromResults = searchItems(query);
    }

    private void updateToResults(String query) {
        toResults = searchItems(query);
    }

    private List<Item> searchItems(String query) {
        List<Item> results = new ArrayList<>();
        String q = query == null ? "" : query.toLowerCase().trim();
        // Remove minecraft: prefix if present
        if (q.startsWith("minecraft:")) q = q.substring("minecraft:".length());
        for (Item item : Registries.ITEM) {
            String id = Registries.ITEM.getId(item).getPath();
            if (q.isEmpty() || id.contains(q)) {
                results.add(item);
                if (results.size() >= 200) break;
            }
        }
        return results;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background
        context.fill(0, 0, this.width, this.height, 0xAA000000);

        int panelW = 200;
        int panelH = 260;
        int panelX = (this.width - panelW * 2 - 20) / 2;
        int panelY = (this.height - panelH) / 2;
        int toPanelX = panelX + panelW + 20;

        // Draw FROM panel
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC1a1a2e);
        context.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF8B5CF6);
        context.drawText(this.textRenderer, "С какого предмета", panelX + 5, panelY + 10, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, "Поиск:", panelX + 5, panelY + 25, 0xFFAAAAAA, false);

        // Draw TO panel
        context.fill(toPanelX, panelY, toPanelX + panelW, panelY + panelH, 0xCC1a2e1a);
        context.fill(toPanelX, panelY, toPanelX + panelW, panelY + 1, 0xFF34D399);
        context.drawText(this.textRenderer, "На какой предмет", toPanelX + 5, panelY + 10, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, "Поиск:", toPanelX + 5, panelY + 25, 0xFFAAAAAA, false);

        // Draw FROM list
        int listY = panelY + 55;
        int visible = Math.min(MAX_VISIBLE, fromResults.size() - fromScrollOffset);
        for (int i = 0; i < visible; i++) {
            int idx = i + fromScrollOffset;
            if (idx >= fromResults.size()) break;
            Item item = fromResults.get(idx);
            String itemId = Registries.ITEM.getId(item).toString();
            int itemY = listY + i * ITEM_H;
            boolean hovered = mouseX >= panelX + 5 && mouseX <= panelX + panelW - 5
                    && mouseY >= itemY && mouseY < itemY + ITEM_H;
            String selected = swapModule.getFromItem();
            boolean isSelected = itemId.equalsIgnoreCase(selected);
            if (hovered) context.fill(panelX + 3, itemY, panelX + panelW - 3, itemY + ITEM_H, 0x55FFFFFF);
            if (isSelected) context.fill(panelX + 3, itemY, panelX + panelW - 3, itemY + ITEM_H, 0x558B5CF6);
            // Draw item icon
            context.drawItem(new ItemStack(item), panelX + 5, itemY + 1);
            context.drawText(this.textRenderer, Registries.ITEM.getId(item).getPath(),
                    panelX + 24, itemY + 5, 0xFFFFFFFF, false);
        }

        // Draw TO list
        visible = Math.min(MAX_VISIBLE, toResults.size() - toScrollOffset);
        for (int i = 0; i < visible; i++) {
            int idx = i + toScrollOffset;
            if (idx >= toResults.size()) break;
            Item item = toResults.get(idx);
            String itemId = Registries.ITEM.getId(item).toString();
            int itemY = listY + i * ITEM_H;
            boolean hovered = mouseX >= toPanelX + 5 && mouseX <= toPanelX + panelW - 5
                    && mouseY >= itemY && mouseY < itemY + ITEM_H;
            String selected = swapModule.getToItem();
            boolean isSelected = itemId.equalsIgnoreCase(selected);
            if (hovered) context.fill(toPanelX + 3, itemY, toPanelX + panelW - 3, itemY + ITEM_H, 0x55FFFFFF);
            if (isSelected) context.fill(toPanelX + 3, itemY, toPanelX + panelW - 3, itemY + ITEM_H, 0x5534D399);
            context.drawItem(new ItemStack(item), toPanelX + 5, itemY + 1);
            context.drawText(this.textRenderer, Registries.ITEM.getId(item).getPath(),
                    toPanelX + 24, itemY + 5, 0xFFFFFFFF, false);
        }

        // Draw selected item labels
        int bottomY = panelY + panelH - 30;
        context.drawText(this.textRenderer, "Выбрано: " + swapModule.getFromItem(),
                panelX + 5, bottomY, 0xFF8B5CF6, false);
        context.drawText(this.textRenderer, "Выбрано: " + swapModule.getToItem(),
                toPanelX + 5, bottomY, 0xFF34D399, false);

        // Close hint
        context.drawText(this.textRenderer, "ESC - закрыть",
                this.width / 2 - 30, panelY + panelH + 10, 0xFF888888, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelW = 200;
            int panelH = 260;
            int panelX = (this.width - panelW * 2 - 20) / 2;
            int panelY = (this.height - panelH) / 2;
            int toPanelX = panelX + panelW + 20;
            int listY = panelY + 55;

            // Check FROM list click
            for (int i = 0; i < Math.min(MAX_VISIBLE, fromResults.size() - fromScrollOffset); i++) {
                int idx = i + fromScrollOffset;
                if (idx >= fromResults.size()) break;
                int itemY = listY + i * ITEM_H;
                if (mouseX >= panelX + 3 && mouseX <= panelX + panelW - 3
                        && mouseY >= itemY && mouseY < itemY + ITEM_H) {
                    String itemId = Registries.ITEM.getId(fromResults.get(idx)).toString();
                    swapModule.setFromItem(itemId);
                    fromSearchField.setText(itemId);
                    return true;
                }
            }

            // Check TO list click
            for (int i = 0; i < Math.min(MAX_VISIBLE, toResults.size() - toScrollOffset); i++) {
                int idx = i + toScrollOffset;
                if (idx >= toResults.size()) break;
                int itemY = listY + i * ITEM_H;
                if (mouseX >= toPanelX + 3 && mouseX <= toPanelX + panelW - 3
                        && mouseY >= itemY && mouseY < itemY + ITEM_H) {
                    String itemId = Registries.ITEM.getId(toResults.get(idx)).toString();
                    swapModule.setToItem(itemId);
                    toSearchField.setText(itemId);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelW = 200;
        int panelH = 260;
        int panelX = (this.width - panelW * 2 - 20) / 2;
        int toPanelX = panelX + panelW + 20;

        if (mouseX >= panelX && mouseX <= panelX + panelW) {
            fromScrollOffset = Math.max(0, Math.min(fromScrollOffset - (int)verticalAmount, Math.max(0, fromResults.size() - MAX_VISIBLE)));
        } else if (mouseX >= toPanelX && mouseX <= toPanelX + panelW) {
            toScrollOffset = Math.max(0, Math.min(toScrollOffset - (int)verticalAmount, Math.max(0, toResults.size() - MAX_VISIBLE)));
        }
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}
