package dev.darkvisuals.client.render.renderers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.darkvisuals;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

import static dev.darkvisuals.client.util.Wrapper.mc;

public class SoulRenderer {

    private final ThemeManager themeManager;

    public SoulRenderer(ThemeManager themeManager) {
        this.themeManager = themeManager;
    }

    public void render(EventRender3D.Game e,
                       LivingEntity target,
                       int particleDensity,
                       float ghostsScale,
                       boolean ghostsGlow,
                       double ghostsGlowIntensity,
                       float animationValue,
                       Vec3d fadeOrigin,
                       float lastKnownHeight,
                       float lastKnownWidth,
                       float alphaMultiplier,
                       Color overrideColor) {
        if (target == null && animationValue <= 0) return;

        var camera = mc.gameRenderer.getCamera();

         
        double entX, entY, entZ;
        float iAge;
        float height, radius;
        
        if (target != null) {
            entX = target.prevX + (target.getX() - target.prevX) * e.getTickDelta();
            entY = target.prevY + (target.getY() - target.prevY) * e.getTickDelta();
            entZ = target.prevZ + (target.getZ() - target.prevZ) * e.getTickDelta();
            iAge = (float) (target.age - 1 + (target.age - (target.age - 1)) * e.getTickDelta());
            height = target.getHeight();
            radius = target.getWidth();
        } else if (fadeOrigin != null) {
             
             
            entX = fadeOrigin.x;
            entY = fadeOrigin.y;
            entZ = fadeOrigin.z;
            iAge = 0;
            height = lastKnownHeight;
            radius = lastKnownWidth;
        } else {
            return;  
        }

        double tPosX = entX - camera.getPos().x;
        double tPosY = entY - camera.getPos().y;
        double tPosZ = entZ - camera.getPos().z;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, darkvisuals.id("hud/bloom.png"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        if (mc.player != null && target != null && mc.player.canSee(target)) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        float ageMultiplier = iAge * 2.5f;
        Color themeColor = overrideColor != null ? overrideColor : themeManager.getThemeColor();
        float scale = ghostsScale;

        MatrixStack base = e.getMatrices();
        base.push();
         
        if (target != null) {
            base.translate(tPosX, tPosY + height * 0.5, tPosZ);
        } else {
            base.translate(tPosX, tPosY, tPosZ);
        }

        if (ghostsGlow) {
            float glowScale = scale * (1.0f + (float) ghostsGlowIntensity);
            for (int j = 0; j < 3; j++) {
                float jOffset = j * 120;
                for (int i = 0; i <= particleDensity; i++) {
                    float iFloat = (float) i;
                    
                     
                    float particleProgress = iFloat / (float) particleDensity;
                    float fadeMultiplier = Math.min(1.0f, Math.max(0.0f, animationValue - particleProgress));
                    if (fadeMultiplier <= 0) continue;
                    
                    double radians = Math.toRadians(((iFloat / 1.5f + iAge) * 8 + jOffset) % 2880);
                    double sinQuad = Math.sin(Math.toRadians(ageMultiplier + i * (j + 1)) * 3f) / 1.8f;

                    int glowAlpha = clamp255(180 * ghostsGlowIntensity * fadeMultiplier * alphaMultiplier);
                    Color glowColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), glowAlpha);

                    double offsetX = Math.cos(radians) * radius;
                    double offsetY = sinQuad;
                    double offsetZ = Math.sin(radians) * radius;

                    base.push();
                    base.translate(offsetX, offsetY, offsetZ);
                    base.multiply(camera.getRotation());

                    Matrix4f matrix = base.peek().getPositionMatrix();
                    BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    buffer.vertex(matrix, -glowScale, glowScale, 0).texture(0f, 1f).color(glowColor.getRGB());
                    buffer.vertex(matrix, glowScale, glowScale, 0).texture(1f, 1f).color(glowColor.getRGB());
                    buffer.vertex(matrix, glowScale, -glowScale, 0).texture(1f, 0).color(glowColor.getRGB());
                    buffer.vertex(matrix, -glowScale, -glowScale, 0).texture(0, 0).color(glowColor.getRGB());
                    BufferRenderer.drawWithGlobalProgram(buffer.end());

                    base.pop();
                }
            }
        }

        for (int j = 0; j < 3; j++) {
            float jOffset = j * 120;
            float jMultiplier = j + 1;

            for (int i = 0; i <= particleDensity; i++) {
                float iFloat = (float) i;
                
                 
                float particleProgress = iFloat / (float) particleDensity;
                float fadeMultiplier = Math.min(1.0f, Math.max(0.0f, animationValue - particleProgress));
                if (fadeMultiplier <= 0) continue;
                
                double radians = Math.toRadians(((iFloat / 1.5f + iAge) * 8 + jOffset) % 2880);
                double sinQuad = Math.sin(Math.toRadians(ageMultiplier + i * jMultiplier) * 3f) / 1.8f;

                float baseAlpha = Math.min(1f, Math.max(0.3f, iFloat / (float) particleDensity));
                int color = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), clamp255(200 * baseAlpha * fadeMultiplier * alphaMultiplier)).getRGB();

                double offsetX = Math.cos(radians) * radius;
                double offsetY = sinQuad;
                double offsetZ = Math.sin(radians) * radius;

                base.push();
                base.translate(offsetX, offsetY, offsetZ);
                base.multiply(camera.getRotation());

                Matrix4f matrix = base.peek().getPositionMatrix();
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                buffer.vertex(matrix, -scale, scale, 0).texture(0f, 1f).color(color);
                buffer.vertex(matrix, scale, scale, 0).texture(1f, 1f).color(color);
                buffer.vertex(matrix, scale, -scale, 0).texture(1f, 0).color(color);
                buffer.vertex(matrix, -scale, -scale, 0).texture(0, 0).color(color);
                BufferRenderer.drawWithGlobalProgram(buffer.end());

                base.pop();
            }
        }

        base.pop();

        if (mc.player != null && target != null && mc.player.canSee(target)) {
            RenderSystem.depthMask(true);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static int clamp255(double v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return (int) Math.round(v);
    }
}


