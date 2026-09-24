package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventKey;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.GraffityManager;
import dev.darkvisuals.client.ui.graffity.GraffityEditorScreen;
import dev.darkvisuals.client.ui.graffity.GraffityWheelScreen;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.impl.BindSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Graffity — граффити из папки assets/darkvisuals/grafi.
 *
 * По бинду открывается круг выбора (как в GTA 5). Выбранное граффити лепится
 * на блок, по которому смотрит игрок, и остаётся там 20 минут, после чего
 * само исчезает.
 *
 * Граффити лежит на грани блока как картина — не поворачивается за камерой,
 * а зафиксировано в мире.
 *
 * PNG-картинки кладутся в src/main/resources/assets/darkvisuals/grafi/. Они
 * должны быть квадратными — так рамка круга не режет рисунок.
 */
public class Graffity extends Module {

    /** Одно размещённое граффити. */
    public static final class Decal {
        public final Vec3d pos;
        public final Direction side;
        public final Identifier texture;
        public final long placedAt;

        public Decal(Vec3d pos, Direction side, Identifier texture) {
            this.pos = pos;
            this.side = side;
            this.texture = texture;
            this.placedAt = System.currentTimeMillis();
        }

        public boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - placedAt > ttlMillis;
        }
    }

    private final List<Decal> decals = new ArrayList<>();

    /** Время жизни граффити — 20 минут по умолчанию. */
    private static final long TTL_MILLIS = TimeUnit.MINUTES.toMillis(20);

    // бинд, по которому открывается круг выбора граффити
    // Key that opens the graffiti editor (paint your own decal).
    private final BindSetting editorBind = new BindSetting("Graffity editor bind", new Bind(GLFW.GLFW_KEY_H, false));
    private final BindSetting wheelBind = new BindSetting("Бинды круга", new Bind(GLFW.GLFW_KEY_G, false));

    private final NumberSetting size = new NumberSetting("Размер", 2.0f, 0.5f, 8.0f, 0.25f);

    public Graffity() {
        super("Graffity", Category.Render, I18n.translate("module.graffity.description"));
        getSettings().add(wheelBind);
        getSettings().add(editorBind);
        getSettings().add(size);
    }

    public BindSetting getWheelBind() {
        return wheelBind;
    }

    public BindSetting getEditorBind() {
        return editorBind;
    }

    public List<Decal> getDecals() {
        return decals;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // при выключении модуля старые граффити нечистим — они живут свой срок
        // и исчезают сами; так включение/выключение не стирает рисунки
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (fullNullCheck()) return;
        if (mc.currentScreen != null) return;

        Bind bind = wheelBind.getValue();
        if (bind == null || bind.isEmpty() || bind.isMouse()) return;
        if (e.getKey() != bind.getKey()) return;
        if (e.getAction() != GLFW.GLFW_PRESS) return;

        // граффити нет вообще — круг открывать не из чего
        if (GraffityManager.count() == 0) {
            if (mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.of(
                        "Нет граффити. Положи квадратные PNG в assets/darkvisuals/grafi/"), false);
            }
            return;
        }

        mc.setScreen(new GraffityWheelScreen(this));
    }

    @EventHandler
    public void onEditorKey(EventKey e) {
        if (fullNullCheck()) return;
        if (mc.currentScreen != null) return;

        Bind eBind = editorBind.getValue();
        if (eBind == null || eBind.isEmpty() || eBind.isMouse()) return;
        if (e.getKey() != eBind.getKey()) return;
        if (e.getAction() != GLFW.GLFW_PRESS) return;

        mc.setScreen(new GraffityEditorScreen());
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        Perf.scopeCpu("graffity");

        // выкидываем истёкшие граффити (20 минут)
        synchronized (decals) {
            if (!decals.isEmpty()) {
                Iterator<Decal> it = decals.iterator();
                while (it.hasNext()) {
                    if (it.next().isExpired(TTL_MILLIS)) it.remove();
                }
            }

            if (decals.isEmpty()) return;

            float drawSize = size.getValue();
            Color white = new Color(255, 255, 255, 255);

            for (Decal d : decals) {
                Render3D.drawDecalTexture(e.getMatrices(), d.pos, d.side, drawSize, d.texture, white);
            }
        }
    }

    /** Поставить выбранное граффити на блок, по которому смотрит игрок. */
    public void placeSelected(int index) {
        if (fullNullCheck()) return;

        Identifier tex = GraffityManager.getTexture(index);
        if (tex == null) return;

        // нужно целиться в блок — граффити лепится на грань, как картина
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            mc.player.sendMessage(Text.of("Наведись на блок, чтобы поставить граффити"), false);
            return;
        }

        Direction side = hit.getSide();
        // точка попадания луча в грань — центр граффити
        Vec3d pos = hit.getPos();

        // чуть-чуть отодвигаем от самой грани, иначе будет z-fighting с блоком
        Vec3d offset = Vec3d.of(side.getVector()).multiply(0.02);
        pos = pos.add(offset);

        synchronized (decals) {
            decals.add(new Decal(pos, side, tex));
        }
    }

    /** Очистить все граффити вручную. */
    public void clearAll() {
        synchronized (decals) {
            decals.clear();
        }
    }
}
