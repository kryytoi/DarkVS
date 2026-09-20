package dev.darkvisuals.client.managers;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.ui.structureeditor.StructureData;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import dev.darkvisuals.client.util.render.StructureRenderer;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.StructureVisualer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

/**
 * BuildSpaceManager — «пространство стройки» для Structure Visualer.
 *
 * Игрока переносит в небо над его текущей позицией (только на клиенте!) и
 * даёт креативный полёт. Все пакеты на сервер, кроме keep-alive, замораживаются —
 * на сервере игрок стоит на месте и вообще не двигается. Ставить блоки можно
 * ЛКМ/ПКМ (блоки виртуальные, на сервер не уходят), ESC — сохранить дом и выйти.
 */
public class BuildSpaceManager implements Wrapper {

    private static BuildSpaceManager instance;

    public static BuildSpaceManager getInstance() {
        if (instance == null) instance = new BuildSpaceManager();
        return instance;
    }

    private static final int PLATFORM_SIZE = 32;                  // площадка 32x32
    private static final double BUILD_Y = 260.0;                  // высота площадки
    private static final double REACH = 7.0;                      // дальность установки блоков
    private static final double STEP = 0.05;                      // шаг луча

    private boolean active;
    private boolean subscribed;

    private final StructureData data = new StructureData();
    private BlockPos origin = BlockPos.ORIGIN;                    // мировой угол площадки (блок [0,0,0] стоит на origin)
    private StructureVisualer module;

    // сохранённое состояние игрока — восстанавливается при выходе
    private double savedX, savedY, savedZ;
    private boolean savedAllowFlying, savedFlying, savedInvulnerable, savedCreative, savedNoClip;
    private int savedSlot;

    public boolean isActive() {
        return active;
    }

    /** Войти в пространство стройки. Вызывается кнопкой «Сделай дом». */
    public void enter() {
        if (active) return;
        if (mc.player == null || mc.world == null) {
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon,
                    "Нужно быть в игре, чтобы строить дом.", 2500));
            return;
        }

        module = darkvisuals.getInstance().getModuleManager().getModule(StructureVisualer.class);
        if (module == null) return;

        if (!subscribed) {
            darkvisuals.getInstance().getEventHandler().subscribe(this);
            subscribed = true;
        }

        // рабочая копия: прошлый дом можно продолжать достраивать
        module.getStructure().copyInto(data);

        // сохранить состояние игрока
        ClientPlayerEntity p = mc.player;
        savedX = p.getX();
        savedY = p.getY();
        savedZ = p.getZ();
        PlayerAbilities ab = p.getAbilities();
        savedAllowFlying = ab.allowFlying;
        savedFlying = ab.flying;
        savedInvulnerable = ab.invulnerable;
        savedCreative = ab.creativeMode;
        savedNoClip = p.noClip;
        savedSlot = p.getInventory().selectedSlot;

        active = true; // пакеты замораживаются с этого момента

        // креатив + полёт + проход сквозь блоки
        ab.allowFlying = true;
        ab.flying = true;
        ab.invulnerable = true;
        ab.creativeMode = true;
        p.noClip = true;
        p.fallDistance = 0f;

        // Дать игроку возможность открывать инвентарь творческого режима
        mc.setScreen(new CreativeInventoryScreen(p, mc.world.getEnabledFeatures(), true));

        // площадка по центру над игроком (та же колонка чанков — они уже загружены)
        origin = BlockPos.ofFloored(p.getX() - PLATFORM_SIZE / 2.0, BUILD_Y, p.getZ() - PLATFORM_SIZE / 2.0);
        teleportClient(p, origin.getX() + PLATFORM_SIZE / 2.0 + 0.5, BUILD_Y + 2.0,
                origin.getZ() + PLATFORM_SIZE / 2.0 + 0.5);

        darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon,
                "Режим стройки: возьмите блок в руку — ПКМ поставить, ЛКМ убрать, ESC — сохранить и выйти.", 5000));
    }

    /**
     * Выйти из пространства стройки.
     * Сначала (пока пакеты ещё заморожены) возвращаем игрока на место,
     * потом размораживаем пакеты — сервер не замечает телепорт.
     */
    public void exit(boolean save) {
        if (!active) return;
        ClientPlayerEntity p = mc.player;

        if (p != null) {
            restoreAbilities(p);
            teleportClient(p, savedX, savedY, savedZ);
        }

        active = false; // пакеты разморожены

        if (save && module != null) {
            saveData();
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon,
                    "Дом сохранён! Нажмите «Сохранить и перенести», чтобы поставить его к прицелу.", 4000));
            if (mc.player != null) {
                module.openEditor();
            }
        }
    }

    /** Сохранить построенное в структуру модуля и на диск (якорь сбрасывается — дом ещё не поставлен). */
    private void saveData() {
        if (module == null) return;
        module.getStructure().copyFrom(data);
        module.getStructure().clearAnchor();
        module.saveStructure();
    }

    /** Вернуть игроку сохранённые способности, слот и коллизию. */
    private void restoreAbilities(ClientPlayerEntity p) {
        PlayerAbilities ab = p.getAbilities();
        ab.allowFlying = savedAllowFlying;
        ab.flying = savedFlying;
        ab.invulnerable = savedInvulnerable;
        ab.creativeMode = savedCreative;
        p.noClip = savedNoClip;
        p.getInventory().selectedSlot = savedSlot;
    }

    /** Телепорт только на клиенте (сервер о нём не узнает — пакеты заморожены). */
    private void teleportClient(ClientPlayerEntity p, double x, double y, double z) {
        p.prevX = x;
        p.prevY = y;
        p.prevZ = z;
        p.lastRenderX = x;
        p.lastRenderY = y;
        p.lastRenderZ = z;
        p.setPosition(x, y, z);
        p.fallDistance = 0f;
    }

    // ------------------------------------------------------------------
    // ввод (вызывается из BuildSpaceMouseMixin)
    // ------------------------------------------------------------------

    /** ЛКМ/ПКМ в режиме стройки: 0 — убрать блок, 1 — поставить блок из руки. */
    public void onMouseButton(int button, int action) {
        if (!active || mc.player == null || mc.world == null) return;
        if (action != GLFW.GLFW_PRESS) return;

        RayHit hit = raycast();
        if (hit == null) return;

        if (button == 0) {
            // ЛКМ — убрать блок
            if (hit.breakPos() != null) {
                int rx = hit.breakPos().getX() - origin.getX();
                int ry = hit.breakPos().getY() - origin.getY();
                int rz = hit.breakPos().getZ() - origin.getZ();
                if (data.remove(rx, ry, rz)) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        } else if (button == 1) {
            // ПКМ — поставить блок из руки
            if (hit.placePos() == null) return;
            String blockId = heldBlockId();
            if (blockId == null) {
                darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon,
                        "Возьмите блок в руку, чтобы ставить его.", 1500));
                return;
            }
            int rx = hit.placePos().getX() - origin.getX();
            int ry = hit.placePos().getY() - origin.getY();
            int rz = hit.placePos().getZ() - origin.getZ();
            if (!inBounds(rx, ry, rz) || data.contains(rx, ry, rz)) return;
            data.set(rx, ry, rz, blockId);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    /** id блока в основной руке (null, если в руке не блок). */
    private String heldBlockId() {
        ItemStack stack = mc.player.getMainHandStack();
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block != Blocks.AIR) return Registries.BLOCK.getId(block).toString();
        }
        return null;
    }

    private boolean inBounds(int relX, int relY, int relZ) {
        return relX >= 0 && relX < PLATFORM_SIZE
                && relZ >= 0 && relZ < PLATFORM_SIZE
                && relY >= 0 && relY < StructureData.MAX_HEIGHT;
    }

    // ------------------------------------------------------------------
    // луч до виртуальных блоков / площадки
    // ------------------------------------------------------------------

    /** breakPos — виртуальный блок под прицелом (или null), placePos — клетка, куда встанет новый блок. */
    public record RayHit(BlockPos breakPos, BlockPos placePos) {}

    public RayHit raycast() {
        if (mc.player == null) return null;
        Vec3d eye = mc.player.getEyePos();
        Vec3d dir = mc.player.getRotationVec(1.0f);

        int steps = (int) (REACH / STEP);
        int pcx = MathHelper.floor(eye.x), pcy = MathHelper.floor(eye.y), pcz = MathHelper.floor(eye.z);
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();

        for (int i = 1; i <= steps; i++) {
            double t = i * STEP;
            int cx = MathHelper.floor(eye.x + dir.x * t);
            int cy = MathHelper.floor(eye.y + dir.y * t);
            int cz = MathHelper.floor(eye.z + dir.z * t);
            if (cx == pcx && cy == pcy && cz == pcz) continue;

            Direction side = sideFromDir(dir);

            // виртуальный блок?
            if (data.contains(cx - ox, cy - oy, cz - oz)) {
                BlockPos abs = new BlockPos(cx, cy, cz);
                return new RayHit(abs, abs.offset(side));
            }

            // площадка: её клетки пустые, ставим прямо на пол
            if (cy == oy && inBounds(cx - ox, 0, cz - oz) && !data.contains(cx - ox, 0, cz - oz)) {
                return new RayHit(null, new BlockPos(cx, cy, cz));
            }

            pcx = cx;
            pcy = cy;
            pcz = cz;
        }
        return null;
    }

    /** Грань, через которую луч входит в новую клетку (по доминирующей оси взгляда). */
    private Direction sideFromDir(Vec3d dir) {
        double ax = Math.abs(dir.x), ay = Math.abs(dir.y), az = Math.abs(dir.z);
        if (ax >= ay && ax >= az) return dir.x > 0 ? Direction.WEST : Direction.EAST;
        if (ay >= ax && ay >= az) return dir.y > 0 ? Direction.DOWN : Direction.UP;
        return dir.z > 0 ? Direction.NORTH : Direction.SOUTH;
    }

    // ------------------------------------------------------------------
    // события
    // ------------------------------------------------------------------

    @EventHandler
    public void onTick(EventTick event) {
        if (!active) return;

        if (mc.player == null || mc.world == null) {
            active = false;
            saveData();
            return;
        }

        // Принудительно включаем режим полноценного креатива в пространстве стройки
        PlayerAbilities ab = mc.player.getAbilities();
        ab.creativeMode = true;
        ab.allowFlying = true;
        ab.flying = true;
        ab.invulnerable = true;

        if (mc.player.isDead() || mc.player.getHealth() <= 0.0f) {
            restoreAbilities(mc.player);
            active = false;
            saveData();
            mc.player.requestRespawn();
            return;
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (!active || mc.player == null || mc.world == null) return;

        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();

        // --- площадка: полупрозрачный пол + рамка + сетка ---
        Box floor = new Box(ox, oy, oz,
                ox + PLATFORM_SIZE, oy + 0.08, oz + PLATFORM_SIZE);
        Render3D.renderBox(e.getMatrices(), floor, new Color(120, 170, 255, 55));
        Render3D.renderBoxOutline(e.getMatrices(), floor, new Color(120, 170, 255, 190));
        for (int i = 4; i < PLATFORM_SIZE; i += 4) {
            Render3D.drawLine(
                    new Vec3d(ox + i, oy + 0.02, oz),
                    new Vec3d(ox + i, oy + 0.02, oz + PLATFORM_SIZE),
                    new Color(255, 255, 255, 45).getRGB(), 1f);
            Render3D.drawLine(
                    new Vec3d(ox, oy + 0.02, oz + i),
                    new Vec3d(ox + PLATFORM_SIZE, oy + 0.02, oz + i),
                    new Color(255, 255, 255, 45).getRGB(), 1f);
        }

        // --- блоки: реальные, плотные ---
        StructureRenderer.renderBlocks(e.getMatrices(), data.getBlocks(), origin, 1.0f);

        // --- прицел ---
        RayHit hit = raycast();
        if (hit != null) {
            if (hit.breakPos() != null) {
                Render3D.renderBoxOutline(e.getMatrices(),
                        new Box(hit.breakPos()).contract(0.002), new Color(255, 90, 90, 220));
            } else if (hit.placePos() != null) {
                Render3D.renderBoxOutline(e.getMatrices(),
                        new Box(hit.placePos()).contract(0.002), new Color(255, 255, 255, 170));
            }
        }
    }
}
