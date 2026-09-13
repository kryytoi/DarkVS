package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.awt.Color;
import net.minecraft.client.resource.language.I18n;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;
import net.minecraft.client.gl.ShaderProgramKeys;
import dev.darkvisuals.client.util.perf.Perf;
import net.minecraft.util.shape.VoxelShape;
import java.util.ArrayList;
import java.util.List;

public class BlockOverlay extends Module {

     
    private final NumberSetting lineWidth = new NumberSetting("setting.lineWidth", 2.0f, 1.0f, 5.0f, 0.1f);
    private final NumberSetting alpha = new NumberSetting("setting.alpha", 150, 0, 255, 1);

     
    private final BooleanSetting fill = new BooleanSetting("setting.fill", false, () -> true);
    private final NumberSetting fillAlpha = new NumberSetting("setting.fillAlpha", 50, 0, 255, 1, () -> fill.getValue());


    private final ThemeManager themeManager;


    public BlockOverlay() {
        super("BlockOverlay", Category.Render, I18n.translate("module.blockoverlay.description"));
        getSettings().add(lineWidth);
        getSettings().add(alpha);
        getSettings().add(fill);
        getSettings().add(fillAlpha);

        this.themeManager = ThemeManager.getInstance();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        try (var __ = Perf.scopeCpu("BlockOverlay.onRender3D")) {
             
            HitResult hitResult = mc.crosshairTarget;
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHitResult.getBlockPos();

             
            Vec3d playerPos = mc.player.getLerpedPos(event.getTickDelta());
            Vec3d blockCenter = Vec3d.ofCenter(blockPos);
            double distance = playerPos.distanceTo(blockCenter);

            if (distance > 100.0f) return;

             
            Color overlayColor = getOverlayColor();
            Color overlayColorSecondary = getOverlayColorSecondary();

             
            VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);

             
            if (shape.isEmpty()) {
                Box blockBox = new Box(blockPos).expand(0.001);
                renderBlockOverlayDirect(event.getMatrices(), blockBox, overlayColor, overlayColorSecondary, event.getTickDelta());
                return;
            }

            List<Box> boxes = new ArrayList<>();
            shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                boxes.add(new Box(minX, minY, minZ, maxX, maxY, maxZ).offset(blockPos).expand(0.001));
            });

            for (Box b : boxes) {
                renderBlockOverlayDirect(event.getMatrices(), b, overlayColor, overlayColorSecondary, event.getTickDelta());
            }
        }
    }



    private Color getOverlayColor() {
         
        Color themeColor = themeManager.getCurrentTheme().getBackgroundColor();
        return new Color(
                themeColor.getRed(),
                themeColor.getGreen(),
                themeColor.getBlue(),
                alpha.getValue().intValue()
        );
    }

    private Color getOverlayColorSecondary() {
         
        Color themeColorSecondary = themeManager.getCurrentTheme().getSecondaryBackgroundColor();
        return new Color(
                themeColorSecondary.getRed(),
                themeColorSecondary.getGreen(),
                themeColorSecondary.getBlue(),
                alpha.getValue().intValue()
        );
    }

    private void renderBlockOverlayDirect(MatrixStack matrices, Box box, Color overlayColor, Color overlayColorSecondary, float tickDelta) {
         
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
         
        if (fill.getValue()) {
            Color fillColor = new Color(
                    overlayColor.getRed(),
                    overlayColor.getGreen(),
                    overlayColor.getBlue(),
                    fillAlpha.getValue().intValue()
            );
            Color fillColorSecondary = new Color(
                    overlayColorSecondary.getRed(),
                    overlayColorSecondary.getGreen(),
                    overlayColorSecondary.getBlue(),
                    fillAlpha.getValue().intValue()
            );
            renderBoxFillGradient(matrices, box, fillColor, fillColorSecondary);
        }
        
         
        Color outlineColor = new Color(
                overlayColor.getRed(),
                overlayColor.getGreen(),
                overlayColor.getBlue(),
                alpha.getValue().intValue()
        );
        Color outlineColorSecondary = new Color(
                overlayColorSecondary.getRed(),
                overlayColorSecondary.getGreen(),
                overlayColorSecondary.getBlue(),
                alpha.getValue().intValue()
        );
        renderBoxOutlineGradient(matrices, box, outlineColor, outlineColorSecondary, lineWidth.getValue().floatValue());
        
         
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    
    private void renderBoxFill(MatrixStack matrices, Box box, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        
         
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);
        
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;
        
         
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        
         
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        
         
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        
         
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        
         
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        
         
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void renderBoxOutline(MatrixStack matrices, Box box, Color color, float lineWidth) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        
         
        Vec3d boxCenter = box.getCenter();
        double distance = camera.distanceTo(boxCenter);
        
         
         
        float adjustedLineWidth = Math.max(0.5f, lineWidth * (float)(10.0 / Math.max(distance, 1.0)));
        
         
        GL11.glLineWidth(adjustedLineWidth);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
         
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);
        
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;
        
         
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        
         
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        
         
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void renderBoxFillGradient(MatrixStack matrices, Box box, Color startColor, Color endColor) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        
         
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);
        
        float r1 = startColor.getRed() / 255.0f;
        float g1 = startColor.getGreen() / 255.0f;
        float b1 = startColor.getBlue() / 255.0f;
        float a1 = startColor.getAlpha() / 255.0f;
        
        float r2 = endColor.getRed() / 255.0f;
        float g2 = endColor.getGreen() / 255.0f;
        float b2 = endColor.getBlue() / 255.0f;
        float a2 = endColor.getAlpha() / 255.0f;
        
         
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        
         
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        
         
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        
         
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        
         
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        
         
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void renderBoxOutlineGradient(MatrixStack matrices, Box box, Color startColor, Color endColor, float lineWidth) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        
         
        Vec3d boxCenter = box.getCenter();
        double distance = camera.distanceTo(boxCenter);
        
         
        float adjustedLineWidth = Math.max(0.5f, lineWidth * (float)(10.0 / Math.max(distance, 1.0)));
        
         
        GL11.glLineWidth(adjustedLineWidth);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
         
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);
        
        float r1 = startColor.getRed() / 255.0f;
        float g1 = startColor.getGreen() / 255.0f;
        float b1 = startColor.getBlue() / 255.0f;
        float a1 = startColor.getAlpha() / 255.0f;
        
        float r2 = endColor.getRed() / 255.0f;
        float g2 = endColor.getGreen() / 255.0f;
        float b2 = endColor.getBlue() / 255.0f;
        float a2 = endColor.getAlpha() / 255.0f;
        
         
        buffer.vertex(matrix, minX, minY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r1, g1, b1, a1);
        
         
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        
         
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}