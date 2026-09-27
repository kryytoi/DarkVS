package dev.darkvisuals.client.managers;

import dev.darkvisuals.darkvisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class GraffityManager {

    private static final String GRAFI_DIR = "grafi";

    private static final List<Identifier> textures = new ArrayList<>();
    private static final List<String> names = new ArrayList<>();
    private static volatile boolean loaded = false;

    private GraffityManager() {}

    public static synchronized void reload(ResourceManager manager) {
        textures.clear();
        names.clear();
        loaded = false;

        if (manager == null) return;

        try {

            Map<Identifier, Resource> found = manager.findResources(
                    GRAFI_DIR,
                    id -> id.getPath().endsWith(".png")
            );

            List<Identifier> sorted = new ArrayList<>(found.keySet());
            sorted.sort((a, b) -> a.getPath().compareTo(b.getPath()));

            for (Identifier id : sorted) {
                String path = id.getPath();

                String name = path.substring(GRAFI_DIR.length() + 1);
                int dot = name.lastIndexOf('.');
                if (dot > 0) name = name.substring(0, dot);

                textures.add(id);
                names.add(name);
            }

            loadCustomTextures();

            loadCustomTextures();

            loaded = true;
            darkvisuals.LOGGER.info("[Graffity] Загружено граффити: {}", textures.size());
        } catch (Exception e) {
            darkvisuals.LOGGER.error("[Graffity] Ошибка загрузки граффити: {}", e.toString());
        }
    }

    public static List<Identifier> getTextures() {
        return Collections.unmodifiableList(textures);
    }

    public static List<String> getNames() {
        return Collections.unmodifiableList(names);
    }

    public static Identifier getTexture(int index) {
        if (index < 0 || index >= textures.size()) return null;
        return textures.get(index);
    }

    public static String getName(int index) {
        if (index < 0 || index >= names.size()) return "—";
        return names.get(index);
    }

    public static int count() {
        return textures.size();
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean isSquare(Identifier id) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            NativeImage img = NativeImage.read(mc.getResourceManager().open(id));
            boolean square = img.getWidth() == img.getHeight();
            img.close();
            return square;
        } catch (Exception e) {
            return false;
        }
    }

    private static void loadCustomTextures() {
        try {
            java.nio.file.Path dir = dev.darkvisuals.darkvisuals.getInstance().getGlobalsDir().toPath().resolve(GRAFI_DIR);
            if (!java.nio.file.Files.isDirectory(dir)) return;
            try (var files = java.nio.file.Files.list(dir)) {
                files.filter(pf -> pf.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                     .sorted()
                     .forEach(pf -> {
                         try {
                             String name = pf.getFileName().toString();
                             name = name.substring(0, name.lastIndexOf('.'));
                             if (names.contains(name)) return;
                             net.minecraft.client.texture.NativeImage img = net.minecraft.client.texture.NativeImage.read(
                                     java.nio.file.Files.newInputStream(pf));
                             Identifier id = Identifier.of("darkvisuals", GRAFI_DIR + "/" + name);
                             MinecraftClient.getInstance().getTextureManager().registerTexture(id, new net.minecraft.client.texture.NativeImageBackedTexture(img));
                             textures.add(id);
                             names.add(name);
                         } catch (Exception ignored) {}
                     });
            }
        } catch (Exception e) {
            darkvisuals.LOGGER.error("[Graffity] custom textures load failed", e);
        }
    }
    public static Identifier registerDynamic(String name, NativeImage image) {
        Identifier id = Identifier.of("darkvisuals", GRAFI_DIR + "/" + name);
        MinecraftClient.getInstance().getTextureManager().registerTexture(
                id, new NativeImageBackedTexture(image));
        textures.add(id);
        names.add(name);
        return id;
    }
}
