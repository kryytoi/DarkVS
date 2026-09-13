package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class CustomInventory extends Module {
    private static final Identifier DEFAULT_TEXTURE = Identifier.of("darkvisuals", "textures/background.png");
    private static final Identifier FILE_TEXTURE_ID = Identifier.of("darkvisuals", "custom_inventory_background");
    private static final String DEFAULT_SOURCE = "darkvisuals:textures/background.png";

    private final StringSetting texture = new StringSetting("setting.inventoryTexturePath", DEFAULT_SOURCE, () -> false, false);
    private final ButtonSetting selectTexture = new ButtonSetting("Выбрать картинку", this::openTexturePicker);
    public final NumberSetting alpha = new NumberSetting("setting.alpha", 1.0f, 0.05f, 1.0f, 0.05f);

    private String cachedSource = "";
    private Identifier cachedTexture = DEFAULT_TEXTURE;
    private int cachedWidth = 256;
    private int cachedHeight = 256;

    public CustomInventory() {
        super("CustomInventory", Category.Render, I18n.translate("module.custominventory.description"));
        getSettings().add(selectTexture);
        getSettings().add(alpha);
        getSettings().add(texture);
    }

    public void renderInventoryPanel(DrawContext context, int guiX, int guiY, int panelWidth, int panelHeight) {
        if (!isToggled() || context == null || panelWidth <= 0 || panelHeight <= 0) return;

        Identifier id = resolveTexture();
        if (id == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha.getValue());
        try {
            context.drawTexture(RenderLayer::getGuiTextured, id, guiX, guiY, 0, 0, panelWidth, panelHeight, panelWidth, panelHeight);
        } catch (Throwable throwable) {
            darkvisuals.LOGGER.warn("[CustomInventory] Не удалось отрисовать фон интерфейса инвентаря: {}", throwable.toString());
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }

    private void openTexturePicker() {
        try {
            File storageDir = getStorageDir();
            storageDir.mkdirs();

            String selected;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(4);
                filters.put(stack.UTF8("*.png"));
                filters.put(stack.UTF8("*.jpg"));
                filters.put(stack.UTF8("*.jpeg"));
                filters.put(stack.UTF8("*.bmp"));
                filters.flip();

                selected = TinyFileDialogs.tinyfd_openFileDialog(
                        "Выберите картинку для инвентаря",
                        storageDir.getAbsolutePath(),
                        filters,
                        "Изображения (*.png, *.jpg, *.jpeg, *.bmp)",
                        false
                );
            }

            if (selected == null || selected.isBlank()) return;

            File selectedFile = new File(selected);
            if (!selectedFile.isFile()) return;

            String extension = getExtension(selectedFile.getName());
            File savedFile = new File(storageDir, "inventory_background" + extension);
            Files.copy(selectedFile.toPath(), savedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            String relativePath = mc.runDirectory.toPath().relativize(savedFile.toPath()).toString().replace(File.separatorChar, '/');
            texture.setValue(relativePath);
            cachedSource = "";

            darkvisuals.LOGGER.info("[CustomInventory] Выбран и сохранён фон инвентаря: {}", savedFile.getAbsolutePath());
        } catch (Throwable throwable) {
            darkvisuals.LOGGER.warn("[CustomInventory] Не удалось выбрать фон инвентаря: {}", throwable.toString());
        }
    }

    private Identifier resolveTexture() {
        String source = texture.getValue() == null ? "" : texture.getValue().trim();
        if (source.isEmpty()) source = DEFAULT_SOURCE;

        if (source.equals(cachedSource) && cachedTexture != null) return cachedTexture;
        cachedSource = source;

        Identifier identifier = parseIdentifier(source);
        if (identifier != null) {
            cachedTexture = identifier;
            cachedWidth = 256;
            cachedHeight = 256;
            return cachedTexture;
        }

        File file = resolveFile(source);
        if (file == null || !file.isFile()) {
            darkvisuals.LOGGER.warn("[CustomInventory] Файл фона инвентаря не найден: {}", source);
            cachedTexture = DEFAULT_TEXTURE;
            cachedWidth = 256;
            cachedHeight = 256;
            return cachedTexture;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) throw new IllegalArgumentException("неподдерживаемый формат изображения");

            NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    nativeImage.setColorArgb(x, y, image.getRGB(x, y));
                }
            }

            mc.getTextureManager().registerTexture(FILE_TEXTURE_ID, new NativeImageBackedTexture(nativeImage));
            cachedTexture = FILE_TEXTURE_ID;
            cachedWidth = Math.max(1, image.getWidth());
            cachedHeight = Math.max(1, image.getHeight());
            return cachedTexture;
        } catch (Throwable throwable) {
            darkvisuals.LOGGER.warn("[CustomInventory] Не удалось загрузить фон инвентаря '{}': {}", source, throwable.toString());
            cachedTexture = DEFAULT_TEXTURE;
            cachedWidth = 256;
            cachedHeight = 256;
            return cachedTexture;
        }
    }

    private Identifier parseIdentifier(String source) {
        String lower = source.toLowerCase(Locale.ROOT);
        boolean looksLikeFile = lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".bmp") || lower.contains("/") || lower.contains("\\") || lower.startsWith(".");
        if (looksLikeFile && !source.contains(":")) return null;

        try {
            return Identifier.tryParse(source);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private File resolveFile(String source) {
        try {
            File direct = Path.of(source).toFile();
            if (direct.isAbsolute()) return direct;

            File fromRunDir = new File(mc.runDirectory, source);
            if (fromRunDir.isFile()) return fromRunDir;

            File fromDarkVisuals = new File(new File(mc.runDirectory, "darkvisuals"), source);
            if (fromDarkVisuals.isFile()) return fromDarkVisuals;

            File fromInventoryDir = new File(getStorageDir(), source);
            if (fromInventoryDir.isFile()) return fromInventoryDir;

            return fromRunDir;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private File getStorageDir() {
        return new File(new File(mc.runDirectory, "darkvisuals"), "custom_inventory");
    }

    private String getExtension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg")) return ".jpg";
        if (lower.endsWith(".jpeg")) return ".jpeg";
        if (lower.endsWith(".bmp")) return ".bmp";
        return ".png";
    }
}
