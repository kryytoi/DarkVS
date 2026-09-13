package dev.darkvisuals.client.util;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

 public final class UserTabBadge {

    public static final Identifier FONT = Identifier.of("darkvisuals", "user_tab");
    private static final String ICON_CHAR = "\uE100";
    private static final Style ICON_STYLE = Style.EMPTY.withFont(FONT);

    private UserTabBadge() {}

      
    public static Text prefix() {
        return Text.literal(ICON_CHAR + " ").setStyle(ICON_STYLE);
    }

      
    public static Text apply(Text original) {
        if (original == null) return null;
        return Text.empty().append(prefix()).append(original);
    }
}
