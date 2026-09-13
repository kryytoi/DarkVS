package dev.darkvisuals.client.util;

import dev.darkvisuals.darkvisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

 public final class IntroManager {

    private static final Identifier GIF_ID = Identifier.of("darkvisuals", "gif/startgif.gif");

    private static final List<Identifier> frames = new ArrayList<>();
    private static final List<Integer> durations = new ArrayList<>();  

    private static long startTime = -1L;
    private static boolean loaded = false;
    private static boolean finished = false;

    private IntroManager() {}

      
    public static synchronized void init(net.minecraft.resource.ResourceManager resourceManager) {
        if (loaded) return;
        loaded = true;
        try {
            if (resourceManager == null) {
                darkvisuals.LOGGER.warn("[Intro] ResourceManager недоступен, GIF не загружен");
                return;
            }
            InputStream stream = resourceManager
                    .getResource(GIF_ID)
                    .map(r -> {
                        try { return r.getInputStream(); }
                        catch (Exception e) { return null; }
                    })
                    .orElse(null);
            if (stream == null) {
                darkvisuals.LOGGER.warn("[Intro] GIF не найден: {}", GIF_ID);
                return;
            }

            ImageInputStream iis = ImageIO.createImageInputStream(stream);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) { iis.close(); return; }
            ImageReader reader = readers.next();
             
             
             
            reader.setInput(iis, true, false);

             
             
            int i = 0;
            while (true) {
                BufferedImage frame;
                try {
                    frame = reader.read(i);
                } catch (IndexOutOfBoundsException e) {
                    break;  
                }
                Identifier id = registerFrame(frame, i);
                if (id != null) {
                    frames.add(id);
                    durations.add(parseDelay(reader, i));
                }
                i++;
            }
            reader.dispose();
            iis.close();
            startTime = System.currentTimeMillis();
            darkvisuals.LOGGER.info("[Intro] Загружено кадров: {}", frames.size());
        } catch (Exception e) {
            darkvisuals.LOGGER.error("[Intro] Ошибка загрузки GIF: {}", e.toString());
        }
    }

    private static Identifier registerFrame(BufferedImage image, int index) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            NativeImage ni = new NativeImage(width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    ni.setColorArgb(x, y, image.getRGB(x, y));
                }
            }
            Identifier id = Identifier.of("darkvisuals", "intro_frame_" + index);
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(ni));
            return id;
        } catch (Exception e) {
            darkvisuals.LOGGER.error("[Intro] Ошибка конверсии кадра {}: {}", index, e.toString());
            return null;
        }
    }

    private static int parseDelay(ImageReader reader, int index) {
        try {
            IIOMetadata meta = reader.getImageMetadata(index);
            if (meta == null) return 100;
            IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree("javax_imageio_gif_image_1.0");
            IIOMetadataNode gce = findChild(root, "GraphicControlExtension");
            if (gce != null) {
                String delay = gce.getAttribute("delayTime");
                int centis = Integer.parseInt(delay.trim());
                return Math.max(10, centis * 10);
            }
        } catch (Exception ignored) {}
        return 100;
    }

    private static IIOMetadataNode findChild(IIOMetadataNode node, String name) {
        for (int i = 0; i < node.getLength(); i++) {
            if (node.item(i) instanceof IIOMetadataNode n && name.equals(n.getNodeName())) {
                return n;
            }
        }
        return null;
    }

      
    public static boolean isActive() {
        return loaded && frames.size() > 0 && !finished && startTime >= 0;
    }

      
    public static Identifier currentFrame() {
        if (!isActive()) return null;
        long elapsed = System.currentTimeMillis() - startTime;
        long cursor = 0;
        int idx = 0;
        for (int i = 0; i < durations.size(); i++) {
            cursor += durations.get(i);
            idx = i;
            if (elapsed < cursor) break;
        }
        long total = 0;
        for (int d : durations) total += d;
        if (elapsed >= total) {
            finish();
            return null;
        }
        return idx < frames.size() ? frames.get(idx) : null;
    }

      
    public static float gifAspect() {
        return 1920f / 1012f;
    }

    private static void finish() {
        finished = true;
         
         
    }
}
