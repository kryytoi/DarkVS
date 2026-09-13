package dev.darkvisuals.client.ui.mainmenu;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.util.Wrapper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
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

 public class IntroScreen extends Screen implements Wrapper {

    private static final Identifier GIF_ID = Identifier.of("darkvisuals", "gif/startgif.gif");

    private final List<Identifier> frameIds = new ArrayList<>();
    private final List<Integer> frameDurations = new ArrayList<>();  

    private long startTimeMs = -1L;
    private boolean finished = false;
    private boolean loaded = false;

    public IntroScreen() {
        super(Text.of("intro"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        loadGif();
    }

    private void loadGif() {
        if (loaded) return;
        loaded = true;
        try {
            InputStream stream = mc.getResourceManager()
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
            reader.setInput(iis, true);

            int n = reader.getNumImages(true);
            for (int i = 0; i < n; i++) {
                BufferedImage frame = reader.read(i);
                Identifier id = registerFrame(frame, i);
                if (id != null) {
                    frameIds.add(id);
                    frameDurations.add(parseDelay(reader, i));
                }
            }
            reader.dispose();
            iis.close();
            darkvisuals.LOGGER.info("[Intro] Загружено кадров: {}", frameIds.size());
        } catch (Exception e) {
            darkvisuals.LOGGER.error("[Intro] Ошибка загрузки GIF: {}", e.toString());
        }
    }

      
    private Identifier registerFrame(BufferedImage image, int index) {
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
            NativeImageBackedTexture tex = new NativeImageBackedTexture(ni);
            mc.getTextureManager().registerTexture(id, tex);
            return id;
        } catch (Exception e) {
            darkvisuals.LOGGER.error("[Intro] Ошибка конверсии кадра {}: {}", index, e.toString());
            return null;
        }
    }

    private int parseDelay(ImageReader reader, int index) {
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

    private IIOMetadataNode findChild(IIOMetadataNode node, String name) {
        for (int i = 0; i < node.getLength(); i++) {
            if (node.item(i) instanceof IIOMetadataNode n && name.equals(n.getNodeName())) {
                return n;
            }
        }
        return null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF000000);

        if (frameIds.isEmpty()) {
            finishToMenu();
            return;
        }

        if (startTimeMs < 0) startTimeMs = System.currentTimeMillis();

        long elapsed = System.currentTimeMillis() - startTimeMs;
        long cursor = 0;
        int frameIndex = 0;
        for (int i = 0; i < frameDurations.size(); i++) {
            cursor += frameDurations.get(i);
            frameIndex = i;
            if (elapsed < cursor) break;
        }

        long totalMs = 0;
        for (int d : frameDurations) totalMs += d;
        if (elapsed >= totalMs) {
            finishToMenu();
            return;
        }

        if (frameIndex < frameIds.size()) {
            try {
                int W = this.width, H = this.height;
                 
                float scale = Math.max(W / 1920f, H / 1012f);
                float drawW = 1920f * scale;
                float drawH = 1012f * scale;
                float ox = (W - drawW) / 2f;
                float oy = (H - drawH) / 2f;
                context.drawTexture(RenderLayer::getGuiTextured, frameIds.get(frameIndex),
                        (int) ox, (int) oy, 0, 0, (int) drawW, (int) drawH, 1920, 1012);
            } catch (Throwable ignored) {}
        }
    }

    private void finishToMenu() {
        if (finished) return;
        finished = true;
        mc.setScreen(new MainMenu());
    }

    @Override
    public void close() {
        for (Identifier id : frameIds) {
            try {
                mc.getTextureManager().destroyTexture(id);
            } catch (Throwable ignored) {}
        }
        frameIds.clear();
        super.close();
    }
}
