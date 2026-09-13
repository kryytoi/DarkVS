package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.resource.language.I18n;

public class HitSound extends Module {

    private final @NotNull BooleanSetting bell = new BooleanSetting("mode.bell", true, () -> false);
    private final @NotNull BooleanSetting crime = new BooleanSetting("mode.crime", false, () -> false);
    private final @NotNull BooleanSetting nya = new BooleanSetting("mode.nya", false, () -> false);
    private final @NotNull BooleanSetting skeet = new BooleanSetting("mode.skeet", false, () -> false);
    private final @NotNull BooleanSetting uwu = new BooleanSetting("mode.uwu", false, () -> false);
    private final @NotNull BooleanSetting moan = new BooleanSetting("mode.moan", false, () -> false);
    private final @NotNull BooleanSetting hit = new BooleanSetting("mode.hit", false, () -> false);
    private final @NotNull BooleanSetting hit2 = new BooleanSetting("mode.hit2", false, () -> false);
    private final @NotNull BooleanSetting hit3 = new BooleanSetting("mode.hit3", false, () -> false);
    private final @NotNull NumberSetting Volume = new NumberSetting(
            "setting.volume",
            1.00f,
            0.1f,
            2.0f,
            0.01f
    );
    private final @NotNull ListSetting mode = new ListSetting(
            "setting.sound",
            true,
            bell, crime, nya, skeet, uwu, moan
    );

    public HitSound() {
        super("HitSound", Category.Utility, I18n.translate("module.hitsound.description"));
        getSettings().add(Volume);
        getSettings().add(mode);
    }

    public @NotNull String getSelectedSound() {
        if (crime.getValue()) return "darkvisuals:crime";
        if (nya.getValue()) return "darkvisuals:nya";
        if (skeet.getValue()) return "darkvisuals:skeet";
        if (uwu.getValue()) return "darkvisuals:uwu";
        if (moan.getValue()) return "darkvisuals:moan" + randomMoanSuffix();
        if (hit.getValue()) return "darkvisuals:hit";
        if (hit2.getValue()) return "darkvisuals:hit2";
        if (hit3.getValue()) return "darkvisuals:hit3";
        return "darkvisuals:bell";  
    }

      
    private @NotNull String randomMoanSuffix() {
        return String.valueOf((int) (Math.random() * 4) + 1);
    }
    public @NotNull NumberSetting getVolume() {
        return Volume;
    }
}
