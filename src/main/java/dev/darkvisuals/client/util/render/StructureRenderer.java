package dev.darkvisuals.client.util.render;

import dev.darkvisuals.client.ui.structureeditor.StructureData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Общий рендер структур из StructureData реальными моделями блоков.
 * Используется голограммой Structure Visualer и режимом стройки BuildSpaceManager.
 */
public final class StructureRenderer {

    // атлас текстур блоков (путь vanilla)
    private static final Identifier BLOCK_ATLAS = Identifier.ofVanilla("textures/atlas/blocks.png");

    // кэш BlockState по id блока, чтобы не дёргать реестр каждый кадр
    private static final Map<String, BlockState> STATE_CACHE = new ConcurrentHashMap<>();
    private static final Random RANDOM = Random.create();

    private StructureRenderer() {}

    /** BlockState по id блока (null, если блок неизвестен или это воздух). */
    public static BlockState resolveState(String blockId) {
        if (blockId == null || blockId.isEmpty()) return null;
        BlockState cached = STATE_CACHE.get(blockId);
        if (cached != null) return cached;

        BlockState result = null;
        try {
            Identifier id = Identifier.tryParse(blockId);
            if (id != null) {
                Block block;
                try {
                    block = Registries.BLOCK.get(id);
                } catch (Throwable t) {
                    return null;
                }
                if (block != null && block != Blocks.AIR) {
                    result = block.getDefaultState();
                }
            }
        } catch (Throwable ignored) {
        }
        if (result != null) STATE_CACHE.put(blockId, result);
        return result;
    }

    /**
     * Нарисовать все блоки структуры реальными моделями с заданной прозрачностью.
     * origin — мировая позиция, к которой привязаны относительные координаты блоков.
     * Вызывать только внутри EventRender3D.Game.
     */
    public static void renderBlocks(MatrixStack ms, Iterable<StructureData.PlacedBlock> blocks,
                                    BlockPos origin, float alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        alpha = Math.max(0.15f, Math.min(1f, alpha));

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vc = new ForcedAlphaVertexConsumer(
                immediate.getBuffer(RenderLayer.getEntityTranslucent(BLOCK_ATLAS)),
                (int) (alpha * 255));

        BlockRenderManager brm = mc.getBlockRenderManager();
        for (StructureData.PlacedBlock pb : blocks) {
            BlockState state = resolveState(pb.blockId());
            if (state == null) continue;

            BlockPos worldPos = origin.add(pb.x(), pb.y(), pb.z());
            BakedModel model = brm.getModel(state);

            ms.push();
            ms.translate(worldPos.getX() - cam.x, worldPos.getY() - cam.y, worldPos.getZ() - cam.z);
            brm.getModelRenderer().render(mc.world, model, state, worldPos, ms, vc, false, RANDOM, 0L, OverlayTexture.DEFAULT_UV);
            ms.pop();
        }
        immediate.draw();
    }
}
