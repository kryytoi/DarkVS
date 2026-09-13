package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.resource.language.I18n;

public class ClientSound extends Module {
    private final @NotNull BooleanSetting pair1 = new BooleanSetting("mode.pair1", true, () -> false);
    private final @NotNull BooleanSetting pair2 = new BooleanSetting("mode.pair2", false, () -> false);
    private final @NotNull BooleanSetting legacy = new BooleanSetting("mode.legacy", false, () -> false);

    private final @NotNull NumberSetting volume = new NumberSetting(
            "setting.volume",
            1.0f,
            0.0f,
            2.0f,
            0.01f
    );

    private final @NotNull ListSetting mode = new ListSetting(
            "setting.sound",
            true,
            pair1, pair2, legacy
    );

    public ClientSound() {
        super("ClientSound", Category.Utility, I18n.translate("module.clientsound.description"));
        getSettings().add(volume);
        getSettings().add(mode);
    }

    public String getEnableSoundId() {
        if (pair2.getValue()) return "darkvisuals:enable2";
        if (legacy.getValue()) return "darkvisuals:enable";
        return "darkvisuals:enable1";  
    }

    public String getDisableSoundId() {
        if (pair2.getValue()) return "darkvisuals:disable2";
        if (legacy.getValue()) return "darkvisuals:disable";
        return "darkvisuals:disable1";  
    }

    public NumberSetting getVolume() {
        return volume;
    }
}
