package dev.darkvisuals.client.ui.structureeditor;

import dev.darkvisuals.client.util.Wrapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.resource.Resource;

import java.awt.*;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

 public final class BlockColorCache implements Wrapper {

    private static final Map<String, Color> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> FAILED = new ConcurrentHashMap<>();

    private BlockColorCache() {}

     private static Color colorFromId(String blockId) {
        int hash = blockId.hashCode();
        float h = ((hash & 0xFFFF) / 65535f) * 360f;
        float s = 0.45f + ((hash >> 16 & 0xFF) / 255f) * 0.25f;
        return Color.getHSBColor(h, s, 0.75f);
    }

     public static Color getColor(String blockId, Color fallback) {
        if (blockId == null || blockId.isEmpty()) return fallback;
        Color cached = CACHE.get(blockId);
        if (cached != null) return cached;

         
        Color hashColor = colorFromId(blockId);

        Color result = hashColor;
        try {
            Identifier id = Identifier.tryParse(blockId);
            if (id == null) return hashColor;

            Block block;
            try {
                block = Registries.BLOCK.get(id);
            } catch (Throwable t) {
                return hashColor;
            }
             
            if (block == null || block == net.minecraft.block.Blocks.AIR) {
                return hashColor;
            }
            if (mc == null || mc.world == null || mc.getBlockRenderManager() == null) {
                 
                return hashColor;
            }

            BlockState state = block.getDefaultState();
            BakedModel model = mc.getBlockRenderManager().getModel(state);
            Random random = Random.create();
            List<BakedQuad> quads = model.getQuads(state, Direction.UP, random);
            if (quads.isEmpty()) quads = model.getQuads(state, null, random);
            if (quads.isEmpty()) {
                 
                CACHE.put(blockId, hashColor);
                return hashColor;
            }

            Identifier texId = quads.get(0).getSprite().getContents().getId();
            Resource resource = mc.getResourceManager().getResource(texId).orElse(null);
            if (resource == null) {
                CACHE.put(blockId, hashColor);
                return hashColor;
            }
            try (InputStream in = resource.getInputStream()) {
                    NativeImage img = NativeImage.read(in);
                    int w = Math.min(img.getWidth(), 64);
                    int h = Math.min(img.getHeight(), 64);
                    int step = Math.max(1, w / 16);
                    long r = 0, g = 0, b = 0, n = 0;
                    for (int y = 0; y < h; y += step) {
                        for (int x = 0; x < w; x += step) {
                            int argb = img.getColorArgb(x, y);
                            if ((argb >>> 24) < 128) continue;
                            r += (argb >> 16) & 0xFF;
                            g += (argb >> 8) & 0xFF;
                            b += argb & 0xFF;
                            n++;
                        }
                    }
                    if (n > 0) {
                        result = new Color((int) (r / n), (int) (g / n), (int) (b / n));
                        CACHE.put(blockId, result);
                    } else {
                        CACHE.put(blockId, hashColor);
                    }
            }
        } catch (Throwable t) {
            CACHE.put(blockId, hashColor);
        }
        return result;
    }
}
