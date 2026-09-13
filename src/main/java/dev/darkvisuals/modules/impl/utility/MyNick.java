package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;

 
public class MyNick extends Module {

    @Getter
    private static MyNick instance;

      
    @Getter
    private final BooleanSetting onlyThirdPerson = new BooleanSetting("Только от третьего лица", true);

    public MyNick() {
        super("MyNick", Category.Utility, "Показывает ваш ник над головой — как его видят другие игроки");
        instance = this;
        getSettings().add(onlyThirdPerson);
    }

 
    public boolean shouldForceOwnLabel(LivingEntity entity) {
        if (!isToggled()) return false;
        if (mc.player == null || entity != mc.player) return false;
        if (onlyThirdPerson.getValue() && mc.options.getPerspective().isFirstPerson()) return false;
        return true;
    }
}