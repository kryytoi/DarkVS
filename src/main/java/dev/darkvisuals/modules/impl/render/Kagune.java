package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.managers.FriendsManager;
import dev.darkvisuals.client.util.models.KaguneModel;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

 
public class Kagune extends Module {

    @Getter
    private final NumberSetting size = new NumberSetting("Размер", 1.0f, 0.5f, 2.0f, 0.05f);

    @Getter
    private final NumberSetting speed = new NumberSetting("Скорость анимации", 1.0f, 0.1f, 3.0f, 0.05f);

    @Getter
    private final BooleanSetting animated = new BooleanSetting("Анимация", true);

    @Getter
    private final BooleanSetting onlySelf = new BooleanSetting("Только у себя", false);

    private float animTime = 0.0f;

    public Kagune() {
        super("Kagune", Category.Render, "Красные живые щупальца за спиной, как в Tokyo Ghoul");
    }

    @EventHandler
    public void onTick(EventTick event) {
        animTime += 0.18f * speed.getValue();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (mc.player == null || mc.world == null) return;

        float tickDelta = event.getTickDelta();
        MatrixStack matrices = event.getMatrices();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         
        float animatedTime = animTime + 0.18f * speed.getValue() * tickDelta;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldRender(player)) continue;
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;

            KaguneModel.render(player, tickDelta, matrices, cameraPos,
                    animatedTime, size.getValue(), animated.getValue());
        }
    }

    private boolean shouldRender(PlayerEntity player) {
        if (mc.player == null) return false;
        if (player == mc.player) return true;
        if (onlySelf.getValue()) return false;
        return FriendsManager.checkFriend(player.getGameProfile().getName());
    }
}