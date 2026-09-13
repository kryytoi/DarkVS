package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.managers.UserTabManager;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

 public class UserTab extends Module {

    @Getter
    private static UserTab instance;

    @Getter
    private final BooleanSetting showInTab = new BooleanSetting("Значок в табе", true);

    @Getter
    private final BooleanSetting showAboveHead = new BooleanSetting("Значок над головой", true);

    @Getter
    private final BooleanSetting showSelf = new BooleanSetting("Показывать себя", true);

      
    @Getter
    private final BooleanSetting showSelfInList = new BooleanSetting("Я в табе", true);

      
    @Getter
    private final BooleanSetting cleanOwnName = new BooleanSetting("Убирать квадраты", true);

     
     
     
     

      
    private static final Pattern LEGACY_FORMATTING = Pattern.compile("§.");

     private static final Pattern PUA_GLYPHS =
            Pattern.compile("[\\uE000-\\uF8FF\\uFFFD\\x{F0000}-\\x{FFFFD}\\x{100000}-\\x{10FFFD}]");

     private static final Pattern NON_NAME_CHARS = Pattern.compile("[^\\p{L}\\p{N}_]+");

    private static final Pattern MULTI_SPACE = Pattern.compile(" {2,}");

    public UserTab() {
        super("UserTab", Category.Utility, "Показывает значок клиента рядом с ником игроков, использующих DarkVisuals");
        getSettings().add(showInTab);
        getSettings().add(showAboveHead);
        getSettings().add(showSelf);
        getSettings().add(showSelfInList);
        getSettings().add(cleanOwnName);
        instance = this;

        UserTabManager.init();
    }

      
    public boolean isDarkVisualsUser(UUID uuid) {
        if (!isToggled() || uuid == null) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (showSelf.getValue() && mc.player != null && uuid.equals(mc.player.getUuid())) {
            return true;
        }

        return UserTabManager.isKnownUser(uuid);
    }

     public static String sanitizeForNameSearch(String text) {
        if (text == null || text.isEmpty()) return "";
        String cleaned = LEGACY_FORMATTING.matcher(text).replaceAll("");
        cleaned = PUA_GLYPHS.matcher(cleaned).replaceAll(" ");
        cleaned = NON_NAME_CHARS.matcher(cleaned).replaceAll(" ");
        cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ");
        return cleaned.trim();
    }

     public boolean containsDarkVisualsUser(String text) {
        return findDarkVisualsUserName(text) != null;
    }

     public String findDarkVisualsUserName(String text) {
        if (!isToggled() || text == null || text.isEmpty()) return null;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return null;

        String sanitized = sanitizeForNameSearch(text);
        if (sanitized.isEmpty()) return null;

        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().getName();
            if (name == null || name.isEmpty()) continue;
            if (!containsWord(sanitized, name)) continue;
            if (isDarkVisualsUser(entry.getProfile().getId())) return name;
        }

        return null;
    }

     private boolean containsWord(String text, String name) {
        String lowerText = text.toLowerCase();
        String lowerName = name.toLowerCase();

        int idx = -1;
        while ((idx = lowerText.indexOf(lowerName, idx + 1)) != -1) {
            boolean leftOk = idx == 0 || isBoundary(text.charAt(idx - 1));
            int end = idx + name.length();
            boolean rightOk = end >= text.length() || isBoundary(text.charAt(end));
            if (leftOk && rightOk) return true;
        }
        return false;
    }

    private static boolean isBoundary(char c) {
        return !Character.isLetterOrDigit(c) && c != '_';
    }

     public boolean hasBadge(Text text) {
        if (text == null) return false;
        return text.visit((style, str) -> {
            Identifier font = style.getFont();
            if (font != null && "darkvisuals".equals(font.getNamespace())) {
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY).isPresent();
    }

     public Text stripServerGlyphs(Text text) {
        MutableText result = Text.empty();
        text.visit((style, str) -> {
            Identifier font = style.getFont();
             
            if (font != null && "darkvisuals".equals(font.getNamespace())) {
                result.append(Text.literal(str).setStyle(style));
                return Optional.empty();
            }
            String cleaned = PUA_GLYPHS.matcher(str).replaceAll("");
            cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ");
            if (!cleaned.isEmpty()) {
                result.append(Text.literal(cleaned).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
