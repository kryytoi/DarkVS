package dev.darkvisuals.modules.impl.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darkvisuals.client.events.impl.EventHandledScreen;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Nameable;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

 
public class InventoryProfiles extends Module {

      
    public enum ProfileType implements Nameable {
        CRYSTAL("Crystal PvP"),
        SWORD("Sword PvP"),
        BUILD("Строительство"),
        MINE("Шахта"),
        EXPLORE("Исследование"),
        SURVIVAL("Выживание"),
        CUSTOM("Свой");

        private final String name;
        ProfileType(String name) { this.name = name; }
        @Override public String getName() { return name; }
    }

     
    private final EnumSetting<ProfileType> profile =
            new EnumSetting<>("setting.profile", ProfileType.CRYSTAL);

    private final BooleanSetting safeMode =
            new BooleanSetting("setting.safeMode", true);

    private final BooleanSetting separateOffhand =
            new BooleanSetting("setting.separateOffhand", true);

    private final BooleanSetting showProgress =
            new BooleanSetting("setting.showProgress", true);

    private final BooleanSetting warnMissing =
            new BooleanSetting("setting.warnMissing", true);

    private final NumberSetting minCount =
            new NumberSetting("setting.minCount", 1f, 1f, 64f, 1f);

    private final ButtonSetting saveProfile =
            new ButtonSetting("setting.saveProfile", this::saveCurrentAsProfile);

    private final ButtonSetting applyProfile =
            new ButtonSetting("setting.applyProfile", this::applyProfileToInventory);

     
    private static final String FILE_NAME = "inventory_profiles.json";
    private static final Map<ProfileType, Map<Integer, ItemId>> profiles = new HashMap<>();
    private static boolean profilesLoaded = false;

    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END = 8;
    private static final int INV_START = 9;
    private static final int INV_END = 35;
    private static final int OFFHAND = 45;

    public InventoryProfiles() {
        super("InventoryProfiles", Category.Utility, "Профили расположения предметов для разных ситуаций");
        getSettings().add(profile);
        getSettings().add(safeMode);
        getSettings().add(separateOffhand);
        getSettings().add(showProgress);
        getSettings().add(warnMissing);
        getSettings().add(minCount);
        getSettings().add(saveProfile);
        getSettings().add(applyProfile);
        loadProfiles();
    }

     
    @EventHandler
    public void onHandledScreen(EventHandledScreen event) {
        if (mc.player == null || mc.currentScreen == null) return;
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;

        ProfileType type = profile.getValue();
        Map<Integer, ItemId> template = profiles.get(type);
        if (template == null || template.isEmpty()) return;

        ScreenHandler handler = screen.getScreenHandler();

         
        Map<Integer, ItemId> target = buildTargetMap(handler, template);

         
        int matched = 0, total = target.size();
        for (Map.Entry<Integer, ItemId> e : target.entrySet()) {
            Slot slot = findSlot(handler, e.getKey());
            if (slot == null) continue;

            ItemStack stackInSlot = slot.getStack();
            boolean correct = stackInSlotMatches(stackInSlot, e.getValue());

            if (correct) matched++;

             
            if (safeMode.getValue() && !correct) {
                Color c = stackInSlot.isEmpty()
                        ? new Color(255, 200, 0, 90)    
                        : new Color(255, 60, 60, 90);   
                drawSlotHighlight(event, slot, c);
            }
        }

         
        if (safeMode.getValue() && warnMissing.getValue()) {
            for (Map.Entry<Integer, ItemId> e : target.entrySet()) {
                if (hasItemAnywhere(handler, e.getValue())) continue;
                Slot slot = findSlot(handler, e.getKey());
                if (slot != null && slot.getStack().isEmpty()) {
                    drawSlotHighlight(event, slot, new Color(255, 255, 0, 110));
                }
            }
        }

         
        if (showProgress.getValue()) {
            drawProgress(event, matched, total);
        }
    }

     
    private void drawSlotHighlight(EventHandledScreen event, Slot slot, Color color) {
         
         
        float sx = slot.x + event.getGuiX();
        float sy = slot.y + event.getGuiY();
        Render2D.drawRoundedRect(event.getDrawContext().getMatrices(), sx, sy, 16, 16, 3, color);
    }

     
    private void drawProgress(EventHandledScreen event, int matched, int total) {
        if (total == 0) return;
        float ratio = matched / (float) total;
        String text = String.format("Профиль: %d/%d (%d%%)", matched, total, (int) (ratio * 100));

        int bgW = event.getBackgroundWidth();
        int guiX = event.getGuiX();
        int guiY = event.getGuiY();
        float cx = bgW / 2f;
        float barW = 120f;
        float barH = 8f;
        float barX = guiX + cx - barW / 2f;
        float barY = guiY - 18f;  

         
        Render2D.drawRoundedRect(event.getDrawContext().getMatrices(), barX, barY, barW, barH, 4,
                new Color(0, 0, 0, 140));
         
        Render2D.drawRoundedRect(event.getDrawContext().getMatrices(), barX, barY,
                barW * ratio, barH, 4, new Color(138, 43, 226, 220));
         
        Render2D.drawFont(event.getDrawContext().getMatrices(), Fonts.SEMIBOLD.getFont(4f),
                text, barX, barY - 8, Color.WHITE);
    }

     
    private Map<Integer, ItemId> buildTargetMap(ScreenHandler handler, Map<Integer, ItemId> template) {
        Map<Integer, ItemId> target = new HashMap<>();
         
         
        var playerInv = mc.player.getInventory();
        for (Map.Entry<Integer, ItemId> e : template.entrySet()) {
            int playerSlot = e.getKey();
            if (playerSlot < 0 || playerSlot >= 36) continue;  
            for (Slot s : handler.slots) {
                if (s.inventory == playerInv && s.getIndex() == playerSlot) {
                    target.put(s.id, e.getValue());
                    break;
                }
            }
        }
        return target;
    }

     
    private Slot findSlot(ScreenHandler handler, int slotId) {
        for (Slot s : handler.slots) {
            if (s.id == slotId) return s;
        }
        return null;
    }

    private boolean stackInSlotMatches(ItemStack stack, ItemId want) {
        if (stack.isEmpty()) return want.count == 0;
        if (!stack.getItem().equals(want.item)) return false;
        return stack.getCount() >= want.count;
    }

    private boolean hasItemAnywhere(ScreenHandler handler, ItemId want) {
        for (Slot s : handler.slots) {
            ItemStack st = s.getStack();
            if (!st.isEmpty() && st.getItem().equals(want.item) && st.getCount() >= want.count) {
                return true;
            }
        }
        return false;
    }

     
    private void saveCurrentAsProfile() {
        if (mc.player == null) return;
        ProfileType type = profile.getValue();
        Map<Integer, ItemId> snapshot = new HashMap<>();

        var inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack st = inv.getStack(i);
            if (!st.isEmpty()) {
                snapshot.put(i, new ItemId(st.getItem(), st.getCount()));
            }
        }
         
        ItemStack off = mc.player.getOffHandStack();
        if (separateOffhand.getValue() && !off.isEmpty()) {
            snapshot.put(OFFHAND, new ItemId(off.getItem(), off.getCount()));
        }

        profiles.put(type, snapshot);
        saveProfiles();
        darkvisuals.LOGGER.info("[InventoryProfiles] Сохранён профиль {}", type.getName());
    }

     
    private void applyProfileToInventory() {
        if (mc.player == null || mc.currentScreen == null) return;
        if (safeMode.getValue()) return;  
         
         
         
    }

     
    private static File profilesFile() {
        return new File(darkvisuals.getInstance().getGlobalsDir(), FILE_NAME);
    }

    private static void loadProfiles() {
        if (profilesLoaded) return;
        profilesLoaded = true;
        File f = profilesFile();
        if (!f.exists()) return;
        try (FileReader reader = new FileReader(f)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (ProfileType type : ProfileType.values()) {
                if (!root.has(type.name())) continue;
                JsonArray arr = root.getAsJsonArray(type.name());
                Map<Integer, ItemId> map = new HashMap<>();
                for (var el : arr) {
                    JsonObject o = el.getAsJsonObject();
                    int slot = o.get("slot").getAsInt();
                    String id = o.get("item").getAsString();
                    int count = o.get("count").getAsInt();
                    Item item = Registries.ITEM.get(Identifier.tryParse(id));
                    if (item != null) map.put(slot, new ItemId(item, count));
                }
                profiles.put(type, map);
            }
        } catch (Exception ignored) {}
    }

    private static void saveProfiles() {
        File f = profilesFile();
        JsonObject root = new JsonObject();
        for (Map.Entry<ProfileType, Map<Integer, ItemId>> e : profiles.entrySet()) {
            JsonArray arr = new JsonArray();
            for (Map.Entry<Integer, ItemId> s : e.getValue().entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("slot", s.getKey());
                o.addProperty("item", Registries.ITEM.getId(s.getValue().item).toString());
                o.addProperty("count", s.getValue().count);
                arr.add(o);
            }
            root.add(e.getKey().name(), arr);
        }
        try (FileWriter writer = new FileWriter(f)) {
            writer.write(root.toString());
        } catch (Exception ignored) {}
    }

      
    private record ItemId(Item item, int count) {}

     
    public static void ensureProfilesLoaded() { loadProfiles(); }
}
