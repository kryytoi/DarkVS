package dev.darkvisuals.client.ui.structureeditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

 public class StructureData {

      
    public static final int MAX_HEIGHT = 32;

    private final Map<String, String> blocks = new LinkedHashMap<>();

    // закреплённая позиция дома в мире (сохраняется вместе с блоками)
    private boolean hasAnchor = false;
    private int anchorX, anchorY, anchorZ;

      
    public record PlacedBlock(int x, int y, int z, String blockId) {}

    private static String key(int x, int y, int z) {
        return x + ";" + y + ";" + z;
    }

    public void set(BlockPos pos, String blockId) {
        set(pos.getX(), pos.getY(), pos.getZ(), blockId);
    }

    public void set(int x, int y, int z, String blockId) {
        if (y < 0 || y >= MAX_HEIGHT) return;
        blocks.put(key(x, y, z), blockId);
    }

    public boolean remove(int x, int y, int z) {
        return blocks.remove(key(x, y, z)) != null;
    }

    public boolean remove(BlockPos pos) {
        return remove(pos.getX(), pos.getY(), pos.getZ());
    }

    public String get(int x, int y, int z) {
        return blocks.get(key(x, y, z));
    }

    public boolean contains(int x, int y, int z) {
        return blocks.containsKey(key(x, y, z));
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public int size() {
        return blocks.size();
    }

    public List<PlacedBlock> getBlocks() {
        List<PlacedBlock> list = new ArrayList<>(blocks.size());
        for (Map.Entry<String, String> e : blocks.entrySet()) {
            String[] p = e.getKey().split(";");
            try {
                list.add(new PlacedBlock(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), e.getValue()));
            } catch (NumberFormatException ignored) {
                 
            }
        }
        return list;
    }

    public void clear() {
        blocks.clear();
    }

    public void copyFrom(StructureData other) {
        blocks.clear();
        blocks.putAll(other.blocks);
        hasAnchor = other.hasAnchor;
        anchorX = other.anchorX;
        anchorY = other.anchorY;
        anchorZ = other.anchorZ;
    }


    public void copyInto(StructureData other) {
        other.blocks.clear();
        other.blocks.putAll(blocks);
        other.hasAnchor = hasAnchor;
        other.anchorX = anchorX;
        other.anchorY = anchorY;
        other.anchorZ = anchorZ;
    }

    /** Закрепить позицию дома в мире. */
    public void setAnchor(int x, int y, int z) {
        anchorX = x;
        anchorY = y;
        anchorZ = z;
        hasAnchor = true;
    }

    /** Убрать закреплённую позицию (дом ещё не поставлен). */
    public void clearAnchor() {
        hasAnchor = false;
    }

    public boolean hasAnchor() {
        return hasAnchor;
    }

    /** Закреплённая позиция или null, если дом ещё не поставлен. */
    public BlockPos getAnchor() {
        return hasAnchor ? new BlockPos(anchorX, anchorY, anchorZ) : null;
    }

      
    public int miny() {
        int m = MAX_HEIGHT;
        for (PlacedBlock b : getBlocks()) m = Math.min(m, b.y());
        return m == MAX_HEIGHT ? 0 : m;
    }

     
     
     

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject map = new JsonObject();
        blocks.forEach((k, v) -> map.add(k, new com.google.gson.JsonPrimitive(v)));
        root.add("blocks", map);
        if (hasAnchor) {
            root.addProperty("anchor", anchorX + ";" + anchorY + ";" + anchorZ);
        }
        return root;
    }

    public boolean loadJson(JsonObject root) {
        JsonObject map = root == null ? null : root.getAsJsonObject("blocks");
        if (map == null) return false;
        clear();
        hasAnchor = false;
        if (root.has("anchor") && root.get("anchor").isJsonPrimitive()) {
            try {
                String[] a = root.get("anchor").getAsString().split(";");
                setAnchor(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
            } catch (RuntimeException ignored) {

            }
        }
        for (Map.Entry<String, com.google.gson.JsonElement> e : map.entrySet()) {
            if (e.getValue() == null || e.getValue().isJsonNull() || !e.getValue().isJsonPrimitive()) continue;
            try {
                String[] p = e.getKey().split(";");
                int x = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);
                int z = Integer.parseInt(p[2]);
                if (y < 0 || y >= MAX_HEIGHT) continue;
                blocks.put(key(x, y, z), e.getValue().getAsString());
            } catch (RuntimeException ignored) {
                 
            }
        }
        return true;
    }

    public void saveToFile(File file) {
        try {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            Files.write(file.toPath(), GSON.toJson(toJson()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean loadFromFile(File file) {
        try {
            if (!file.exists()) return false;
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return loadJson(JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
