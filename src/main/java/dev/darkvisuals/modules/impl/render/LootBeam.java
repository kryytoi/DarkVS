package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.Set;

public class LootBeam extends Module {

    private static final int BEAM_SEGMENTS = 16;

    private final NumberSetting range      = new NumberSetting("Дальность",    32f, 4f, 96f, 2f);
    private final NumberSetting beamHeight = new NumberSetting("Высота луча",  12f, 3f, 40f, 1f);
    private final NumberSetting beamWidth  = new NumberSetting("Ширина",        0.12f, 0.03f, 0.5f, 0.01f);
    private final BooleanSetting glow      = new BooleanSetting("Ореол снизу", true);
    private final BooleanSetting onlyRare  = new BooleanSetting("Только редкие", false);

    private static final Set<net.minecraft.item.Item> LEGENDARY = Set.of(
            Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE,
            Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
            Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP,
            Items.TOTEM_OF_UNDYING, Items.ELYTRA,
            Items.TRIDENT, Items.MACE
    );

    private static final Set<net.minecraft.item.Item> RARE = Set.of(
            Items.DIAMOND, Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE,
            Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
            Items.ENCHANTED_GOLDEN_APPLE, Items.ENDER_CHEST,
            Items.BEACON, Items.SHULKER_BOX,
            Items.NETHER_STAR, Items.EMERALD
    );

    private static final Set<net.minecraft.item.Item> UNCOMMON = Set.of(
            Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE,
            Items.GOLDEN_APPLE, Items.AMETHYST_SHARD,
            Items.ECHO_SHARD, Items.HEART_OF_THE_SEA,
            Items.MUSIC_DISC_11, Items.MUSIC_DISC_13
    );

    public LootBeam() {
        super("LootBeam", Category.Render, "Лучи над ценным лутом как в Diablo / Borderlands");
        getSettings().add(range);
        getSettings().add(beamHeight);
        getSettings().add(beamWidth);
        getSettings().add(glow);
        getSettings().add(onlyRare);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam  = mc.gameRenderer.getCamera().getPos();
        float maxR = range.getValue();
        long  now  = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (net.minecraft.entity.Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof ItemEntity ie)) continue;
            ItemStack stack = ie.getStack();
            if (stack.isEmpty()) continue;

            double dist = ent.getPos().distanceTo(mc.player.getPos());
            if (dist > maxR) continue;

            Color beamColor = getBeamColor(stack);
            if (beamColor == null) {
                if (onlyRare.getValue()) continue;
                beamColor = new Color(200, 200, 200, 200);
            }

            float flicker = 0.85f + 0.15f * MathHelper.sin(now / 300f + ent.getId() * 2.3f);
            int alpha = (int)(beamColor.getAlpha() * flicker
                    * Math.min(1f, (float)((maxR - dist) / 5f))
                    * Math.min(1f, ent.age / 10f));
            if (alpha <= 4) continue;

            Vec3d pos = ie.getLerpedPos(event.getTickDelta());
            float height = beamHeight.getValue();
            float w = beamWidth.getValue();
            float spin = (now / 800f) % (float)(Math.PI * 2);

            matrices.push();
            matrices.translate(
                    (float)(pos.x - cam.x),
                    (float)(pos.y - cam.y),
                    (float)(pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)Math.toDegrees(spin)));

            drawBeamCylinder(matrices.peek().getPositionMatrix(),
                    beamColor, alpha, w, height);

            if (glow.getValue()) {
                drawBaseDisc(matrices.peek().getPositionMatrix(),
                        beamColor, (int)(alpha * 0.4f), w * 4f);
            }

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private Color getBeamColor(ItemStack stack) {

        if (stack.hasEnchantments()) {
            return new Color(120, 60, 255, 220);
        }
        if (LEGENDARY.contains(stack.getItem())) {
            return new Color(255, 200, 30, 220);
        }
        if (RARE.contains(stack.getItem())) {
            return new Color(60, 180, 255, 200);
        }
        if (UNCOMMON.contains(stack.getItem())) {
            return new Color(180, 60, 255, 190);
        }
        return null;
    }

    private void drawBeamCylinder(Matrix4f m, Color c, int alpha, float radius, float height) {
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue();
        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= BEAM_SEGMENTS; i++) {
            float ang = (float)(i * Math.PI * 2 / BEAM_SEGMENTS);
            float x = MathHelper.cos(ang) * radius;
            float z = MathHelper.sin(ang) * radius;
            buf.vertex(m, x, 0f,     z).color(r, g, b, alpha);
            buf.vertex(m, x, height, z).color(r, g, b, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawBaseDisc(Matrix4f m, Color c, int alpha, float radius) {
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue();
        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(m, 0f, 0.01f, 0f).color(r, g, b, alpha);
        for (int i = 0; i <= BEAM_SEGMENTS; i++) {
            float ang = (float)(i * Math.PI * 2 / BEAM_SEGMENTS);
            buf.vertex(m, MathHelper.cos(ang) * radius, 0.01f, MathHelper.sin(ang) * radius)
               .color(r, g, b, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}
