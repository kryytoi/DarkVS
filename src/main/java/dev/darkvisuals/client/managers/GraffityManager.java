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

/**
 * GraffityManager — подгружает все PNG-граффити из assets/darkvisuals/grafi.
 *
 * Каждая картинка — это отдельное граффити. Квадратные PNG складываются в папку
 * src/main/resources/assets/darkvisuals/grafi/*.png и подхватываются при
 * перезагрузке ресурсов (F3+T или запуск).
 *
 * Граффити рендерятся как биллборды в мире (см. Graffity), поэтому нам нужны
 * только идентификаторы текстур — Minecraft сам загрузит PNG по Identifier.
 */
public final class GraffityManager {

    /** Папка внутри assets/darkvisuals/, где лежат граффити. */
    private static final String GRAFI_DIR = "grafi";

    private static final List<Identifier> textures = new ArrayList<>();
    private static final List<String> names = new ArrayList<>();
    private static volatile boolean loaded = false;

    private GraffityManager() {}

    /** Перечитать граффити из ресурсов. Вызывается из reload-листенера. */
    public static synchronized void reload(ResourceManager manager) {
        textures.clear();
        names.clear();
        loaded = false;

        if (manager == null) return;

        try {
            // findResources возвращает все ресурсы, подходящие под предикат —
            // так мы берём каждый .png из папки grafi без жёсткого списка.
            Map<Identifier, Resource> found = manager.findResources(
                    GRAFI_DIR,
                    id -> id.getPath().endsWith(".png")
            );

            List<Identifier> sorted = new ArrayList<>(found.keySet());
            sorted.sort((a, b) -> a.getPath().compareTo(b.getPath()));

            for (Identifier id : sorted) {
                String path = id.getPath();
                // из пути "grafi/my_tag.png" берём имя файла без расширения
                String name = path.substring(GRAFI_DIR.length() + 1);
                int dot = name.lastIndexOf('.');
                if (dot > 0) name = name.substring(0, dot);

                textures.add(id);
                names.add(name);
            }

            loaded = true;
            darkvisuals.LOGGER.info("[Graffity] Загружено граффити: {}", textures.size());
        } catch (Exception e) {
            darkvisuals.LOGGER.error("[Graffity] Ошибка загрузки граффити: {}", e.toString());
        }
    }

    /** Все загруженные текстуры граффити (порядок стабильный — по имени файла). */
    public static List<Identifier> getTextures() {
        return Collections.unmodifiableList(textures);
    }

    /** Имена граффити, параллельные списку текстур. */
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

    /** Квадратная ли текстура (по метаданным PNG). Граффити должны быть квадратными —
     *  неквадратные всё равно рисуются, но рамка колеса режет их по квадрату. */
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

    /** Зарегистрировать граффити во время игры (для теста/аддонов) —
     *  принимает уже готовую NativeImage, регистрирует её в менеджере текстур. */
    public static Identifier registerDynamic(String name, NativeImage image) {
        Identifier id = Identifier.of("darkvisuals", GRAFI_DIR + "/" + name);
        MinecraftClient.getInstance().getTextureManager().registerTexture(
                id, new NativeImageBackedTexture(image));
        textures.add(id);
        names.add(name);
        return id;
    }
}
