package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventHandledScreen;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.screen.slot.Slot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

 
public class SeeCraft extends Module {

    private static final int SLOT = 18;       
    private static final int CELL = 16;       

    private final NumberSetting maxRecipes = new NumberSetting("setting.maxRecipes", 1f, 1f, 3f, 1f);

    public SeeCraft() {
        super("SeeCraft", Category.Render, "Показывает крафт предмета иконками при наведении");
        getSettings().add(maxRecipes);
    }

    @EventHandler
    public void onHandledScreen(EventHandledScreen event) {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) return;

        Slot hover = event.getSlotHover();
        if (hover == null || hover.getStack().isEmpty()) return;

        ItemStack target = hover.getStack();
        List<RecipeDisplayEntry> recipes = findRecipesProducing(target);
        if (recipes.isEmpty()) return;

        int max = maxRecipes.getValue().intValue();
        if (recipes.size() > max) recipes = recipes.subList(0, max);

        drawRecipePanel(event, recipes, target);
    }

    private List<RecipeDisplayEntry> findRecipesProducing(ItemStack target) {
        List<RecipeDisplayEntry> result = new ArrayList<>();
        try {
            if (mc.player == null) return result;
            ClientRecipeBook book = mc.player.getRecipeBook();
            if (book == null) return result;

             
            for (RecipeResultCollection col : book.getOrderedResults()) {
                for (RecipeDisplayEntry entry : col.getAllRecipes()) {
                    Item output = getRecipeOutputItem(entry);
                    if (output != null && output.equals(target.getItem())) {
                        result.add(entry);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    private Item getRecipeOutputItem(RecipeDisplayEntry entry) {
        try {
            SlotDisplay result = entry.display().result();
            if (result instanceof SlotDisplay.ItemSlotDisplay itemDisp) {
                return itemDisp.item().value();
            } else if (result instanceof SlotDisplay.StackSlotDisplay stackDisp) {
                return stackDisp.stack().getItem();
            }
        } catch (Throwable ignored) {}
        return null;
    }

      
    private List<ItemStack> getIngredientIcons(RecipeDisplayEntry entry) {
        List<ItemStack> icons = new ArrayList<>();
        try {
            boolean filled = false;
            if (entry.display() instanceof ShapedCraftingRecipeDisplay shaped) {
                for (SlotDisplay slotDisp : shaped.ingredients()) {
                    icons.add(slotToStack(slotDisp));
                }
                filled = icons.stream().anyMatch(s -> !s.isEmpty());
            } else if (entry.display() instanceof ShapelessCraftingRecipeDisplay shapeless) {
                for (SlotDisplay slotDisp : shapeless.ingredients()) {
                    icons.add(slotToStack(slotDisp));
                }
                filled = icons.stream().anyMatch(s -> !s.isEmpty());
            }
             
            if (!filled) {
                icons.clear();
                for (Ingredient ing : getIngredients(entry)) {
                    icons.add(ingredientToStack(ing));
                }
            }
        } catch (Throwable ignored) {}
        return icons;
    }

    private ItemStack slotToStack(SlotDisplay slotDisp) {
        try {
            if (slotDisp instanceof SlotDisplay.ItemSlotDisplay itemDisp) {
                return new ItemStack(itemDisp.item().value());
            } else if (slotDisp instanceof SlotDisplay.StackSlotDisplay stackDisp) {
                return stackDisp.stack();
            } else if (slotDisp instanceof SlotDisplay.TagSlotDisplay tagDisp) {
                 
                 
                var entryList = net.minecraft.registry.Registries.ITEM.getOptional(tagDisp.tag());
                if (entryList.isPresent()) {
                    var first = entryList.get().stream().findFirst();
                    if (first.isPresent()) {
                        return new ItemStack(first.get().value());
                    }
                }
            }
        } catch (Throwable ignored) {}
        return ItemStack.EMPTY;
    }

    private ItemStack ingredientToStack(Ingredient ing) {
        try {
            var item = ing.getMatchingItems().findFirst();
            if (item.isPresent()) {
                return new ItemStack(item.get().value());
            }
        } catch (Throwable ignored) {}
        return ItemStack.EMPTY;
    }

    private List<Ingredient> getIngredients(RecipeDisplayEntry entry) {
        try {
            return entry.craftingRequirements().orElse(List.of());
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private void drawRecipePanel(EventHandledScreen event, List<RecipeDisplayEntry> recipes, ItemStack target) {
        DrawContext ctx = event.getDrawContext();
        int guiX = event.getGuiX();
        int guiY = event.getGuiY();
        int bgW = event.getBackgroundWidth();

        int recipeCount = recipes.size();
         
        int gridW = 3 * SLOT;
        int panelW = gridW + 30 + SLOT + 12;
        int panelH = recipeCount > 1 ? 3 * SLOT + 8 * recipeCount : 3 * SLOT + 4;

        float panelX = guiX + bgW + 10;  
        float panelY = guiY + 10;

         
        Render2D.drawRoundedRect(ctx.getMatrices(), panelX, panelY, panelW, panelH, 8,
                new Color(20, 20, 26, 230));

        float y = panelY + 4;
        for (int r = 0; r < recipeCount; r++) {
            RecipeDisplayEntry entry = recipes.get(r);
            drawOneRecipe(ctx, entry, panelX, y);
            y += 3 * SLOT + 8;
        }
    }

    private void drawOneRecipe(DrawContext ctx, RecipeDisplayEntry entry, float panelX, float panelY) {
        List<ItemStack> ingredients = getIngredientIcons(entry);

         
        int gw = 3, gh = 3;
        if (entry.display() instanceof ShapedCraftingRecipeDisplay shaped) {
            gw = shaped.width();
            gh = shaped.height();
        } else if (entry.display() instanceof ShapelessCraftingRecipeDisplay shapeless) {
             
            int count = ingredients.size();
            if (count <= 3) { gw = count; gh = 1; }
            else if (count <= 6) { gw = 3; gh = 2; }
            else { gw = 3; gh = 3; }
        }

         
        for (int i = 0; i < gw * gh; i++) {
            int col = i % gw;
            int row = i / gw;
            float sx = panelX + 6 + col * SLOT;
            float sy = panelY + 6 + row * SLOT;

             
            Render2D.drawRoundedRect(ctx.getMatrices(), sx, sy, CELL, CELL, 3,
                    new Color(0, 0, 0, 90));

            if (i < ingredients.size()) {
                ItemStack stack = ingredients.get(i);
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, Math.round(sx + (CELL - 16) / 2f), Math.round(sy + (CELL - 16) / 2f));
                }
            }
        }

         
        float gridW = gw * SLOT;
        float arrowX = panelX + 6 + gridW + 6;
        float arrowY = panelY + 6 + (gh / 2f) * SLOT;
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(6f), "→", arrowX, arrowY, Color.WHITE);

         
        float resX = panelX + 6 + gridW + 26;
        float resY = panelY + 6 + (gh / 2f) * SLOT;
        Render2D.drawRoundedRect(ctx.getMatrices(), resX, resY, CELL, CELL, 3,
                new Color(0, 0, 0, 110));
        Item result = getRecipeOutputItem(entry);
        if (result != null) {
            ctx.drawItem(new ItemStack(result), Math.round(resX), Math.round(resY));
        }
    }
}
