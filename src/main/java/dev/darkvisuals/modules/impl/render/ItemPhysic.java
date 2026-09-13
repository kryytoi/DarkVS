package dev.darkvisuals.modules.impl.render;


import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import net.minecraft.client.resource.language.I18n;

 public class ItemPhysic extends Module {

    private static ItemPhysic instance;

    public ItemPhysic() {
        super("ItemPhysic", Category.Render, I18n.translate("module.itemphysic.description"));
        instance = this;
    }

      
    public static ItemPhysic get() {
        if (instance == null) {
            instance = darkvisuals.getInstance().getModuleManager().getModule(ItemPhysic.class);
        }
        return instance;
    }
}
