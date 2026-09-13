package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.resource.language.I18n;

@Getter
public class AspectRatio extends Module {

    private final @NotNull NumberSetting aspectRatio = new NumberSetting(
            "setting.aspectRatio",
            1.777f,    
            0.5f,      
            3.0f,      
            0.01f      
    );

    public AspectRatio() {
        super("Aspect Ratio", Category.Render, I18n.translate("module.aspectratio.description"));
        getSettings().add(aspectRatio);
    }
}
