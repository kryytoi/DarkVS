package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import net.minecraft.client.resource.language.I18n;

public class Cape extends Module {

    public Cape() {
        super("Cape", Category.Utility, I18n.translate("module.cape.description"));
    }

}
