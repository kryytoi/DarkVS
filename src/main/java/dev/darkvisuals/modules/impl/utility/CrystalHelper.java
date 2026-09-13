package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.Color;

public class CrystalHelper extends Module {

     
    private static final float EXPLOSION_POWER = 6.0f;
    private static final float EXPLOSION_RADIUS = EXPLOSION_POWER * 2.0f;

    private final BooleanSetting fillMode = new BooleanSetting("Заливка", true);
    private final NumberSetting fillOpacity = new NumberSetting("Прозрачность заливки", 0.35f, 0.0f, 1.0f, 0.05f);
    private final BooleanSetting outlineMode = new BooleanSetting("Обводка", true);
    private final NumberSetting outlineWidth = new NumberSetting("Толщина обводки", 2.0f, 1.0f, 5.0f, 0.5f);
    private final NumberSetting expand = new NumberSetting("Размер бокса", 0.1f, 0.0f, 0.5f, 0.05f);

    private static final Color SAFE_COLOR = new Color(40, 220, 60);
    private static final Color DAMAGE_COLOR = new Color(230, 40, 40);
    private static final Color LETHAL_COLOR = new Color(15, 15, 15);

    public CrystalHelper() {
        super("CrystalHelper", Category.Utility, "Красит кристаллы по опасности: зелёный — не заденет, красный — снимет ХП, чёрный — смертельно");
        getSettings().add(fillMode);
        getSettings().add(fillOpacity);
        getSettings().add(outlineMode);
        getSettings().add(outlineWidth);
        getSettings().add(expand);

        fillOpacity.setVisible(fillMode::getValue);
        outlineWidth.setVisible(outlineMode::getValue);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        if (!fillMode.getValue() && !outlineMode.getValue()) return;

        Render3D.DEBUG_LINE_WIDTH = outlineWidth.getValue().floatValue();

        Vec3d playerPos = mc.player.getPos();
        double health = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!crystal.isAlive()) continue;

            Vec3d crystalPos = crystal.getPos();
            double distance = crystalPos.distanceTo(playerPos);

            double damage = 0;
            if (distance <= EXPLOSION_RADIUS) {
                double exposure = getExposure(crystalPos, mc.player.getEyePos());
                if (exposure > 0) {
                    double impact = (1.0 - distance / EXPLOSION_RADIUS) * exposure;
                    damage = (impact * impact + impact) / 2.0 * 7.0 * EXPLOSION_RADIUS + 1.0;

                     
                    int armor = mc.player.getArmor();
                    double reduction = Math.min(0.8, armor * 0.04);
                    damage *= (1.0 - reduction);
                }
            }

            Color color;
            if (damage <= 0) {
                color = SAFE_COLOR;
            } else if (damage >= health) {
                color = LETHAL_COLOR;
            } else {
                color = DAMAGE_COLOR;
            }

            Box box = crystal.getBoundingBox().expand(expand.getValue());

            if (fillMode.getValue()) {
                int alpha = (int) (255 * fillOpacity.getValue());
                Render3D.renderBox(e.getMatrices(), box, new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            }
            if (outlineMode.getValue()) {
                Render3D.renderBoxOutline(e.getMatrices(), box, color);
            }
        }
    }

 
    private double getExposure(Vec3d from, Vec3d to) {
        HitResult hit = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.MISS ? 1.0 : 0.0;
    }
}