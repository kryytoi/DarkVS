package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.BuildSpaceManager;
import dev.darkvisuals.client.ui.structureeditor.StructureData;
import dev.darkvisuals.client.ui.structureeditor.StructureEditorScreen;
import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import dev.darkvisuals.client.util.render.StructureRenderer;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.io.File;


public class StructureVisualer extends Module {

    private final ButtonSetting buildButton = new ButtonSetting("Сделай дом", this::startBuilding);
    private final BooleanSetting hologram = new BooleanSetting("Голограмма", true);
    private final BooleanSetting outline = new BooleanSetting("Обводка блоков", true);
    private final NumberSetting opacity = new NumberSetting("Прозрачность", 0.5f, 0.15f, 0.95f, 0.05f);

    private final StructureData structure = new StructureData();
    private final File configFile;

    public StructureVisualer() {
        super("Structure Visualer", Category.Render, I18n.translate("module.structurevisualer.description"));
        getSettings().add(buildButton);
        getSettings().add(hologram);
        getSettings().add(outline);
        getSettings().add(opacity);

        configFile = new File(darkvisuals.getInstance().getGlobalsDir(), "structure_visualer.json");
        structure.loadFromFile(configFile);
    }

    public StructureData getStructure() {
        return structure;
    }

    /** Сохранить текущую структуру в файл. */
    public void saveStructure() {
        structure.saveToFile(configFile);
    }

    /** Открыть экран дома (после выхода из режима стройки). */
    public void openEditor() {
        mc.setScreen(new StructureEditorScreen(this));
    }

    /** Кнопка «Сделай дом»: перенестись в пространство стройки и строить по-настоящему. */
    public void startBuilding() {
        if (mc.currentScreen != null) mc.setScreen(null);
        BuildSpaceManager.getInstance().enter();
    }

    /**
     * Поставить дом к прицелу и закрепить позицию.
     * Повторное нажатие «Сохранить и перенести» убирает прошлый дом
     * и ставит новый на текущую позицию прицела, закрепляя её.
     */
    public void applyStructure(StructureData data) {
        if (data.isEmpty()) {
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon,
                    "Дом пуст — постройте его в режиме стройки.", 2500));
            return;
        }
        structure.copyFrom(data);

        if (fullNullCheck()) {
            // вне мира позицию закрепить нельзя — дом встанет к прицелу при первом рендере
            structure.clearAnchor();
        } else {
            BlockPos pos = currentTargetPos();
            structure.setAnchor(pos.getX(), pos.getY(), pos.getZ());
        }

        structure.saveToFile(configFile);
        if (!isToggled()) setToggled(true);

        darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon,
                "Дом поставлен к прицелу и закреплён на месте!", 3000));
    }

    /**
     * Точка перед прицелом: блок, на который смотрим (ставим на его грань),
     * либо позиция в паре метров по взгляду.
     */
    private BlockPos currentTargetPos() {
        HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            return bhr.getBlockPos().offset(bhr.getSide());
        }
        float yaw = (float) Math.toRadians(mc.player.getYaw());
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);
        return BlockPos.ofFloored(
                mc.player.getX() + dx * 2.0,
                mc.player.getY() - 0.3,
                mc.player.getZ() + dz * 2.0
        );
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (!isToggled() || !hologram.getValue() || fullNullCheck()) return;
        if (structure.isEmpty()) return;

        // старый конфиг без закреплённой позиции — закрепляем один раз к прицелу
        if (!structure.hasAnchor()) {
            BlockPos pos = currentTargetPos();
            structure.setAnchor(pos.getX(), pos.getY(), pos.getZ());
            structure.saveToFile(configFile);
        }
        BlockPos anchor = structure.getAnchor();

        MatrixStack ms = e.getMatrices();
        float alpha = Math.max(0.15f, Math.min(1f, opacity.getValue()));

        // реальные полупрозрачные блоки
        StructureRenderer.renderBlocks(ms, structure.getBlocks(), anchor, alpha);

        // опциональная обводка блоков
        if (outline.getValue()) {
            Color edge = new Color(255, 255, 255, (int) (70 * alpha));
            for (StructureData.PlacedBlock pb : structure.getBlocks()) {
                if (StructureRenderer.resolveState(pb.blockId()) == null) continue;
                BlockPos wp = anchor.add(pb.x(), pb.y(), pb.z());
                Render3D.renderBoxOutline(ms, new Box(wp).contract(0.005), edge);
            }
        }
    }
}
