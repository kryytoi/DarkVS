package dev.darkvisuals.client.util.renderer;

import dev.darkvisuals.client.render.builders.*;
import dev.darkvisuals.client.render.builders.states.*;
import dev.darkvisuals.client.render.renderers.impl.*;
import dev.darkvisuals.client.util.Wrapper;

import dev.darkvisuals.client.util.renderer.fonts.Instance;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.Tessellator;


@UtilityClass
public class   Render2D implements Wrapper {

    public void drawRoundedRect(MatrixStack stack, float x, float y, float width, float height, float radius, Color color) {
        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

     public void drawRoundedRect(MatrixStack stack, float x, float y, float width, float height,
                                 float topLeft, float topRight, float bottomRight, float bottomLeft, Color color) {
        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(topLeft, topRight, bottomRight, bottomLeft))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawRockstarRoundedRect(MatrixStack stack, float x, float y, float width, float height, float radius, Color color) {
        RockstarBuiltRectangle built = Builder.rockstarRectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawBlurredRect(MatrixStack stack, float x, float y, float width, float height, float radius, float blurRadius, Color color) {
         
         
        int layers = 3;
        float baseAlpha = color.getAlpha();
        for (int i = 0; i < layers; i++) {
            float t = (i + 1) / (float) layers;
            float falloff = (1f - t);
            int a = Math.max(0, Math.min(255, (int) (baseAlpha * (0.6f * falloff + 0.2f))));
            float spread = Math.max(1f, Math.min(12f, blurRadius * 0.5f)) * (i + 1) * 0.5f;
            float rad = radius + i * 0.6f;
            drawRoundedRect(
                    stack,
                    x - spread,
                    y - spread,
                    width + spread * 2f,
                    height + spread * 2f,
                    rad,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), a)
            );
        }
    }

    public void drawRockstarBlurredRect(MatrixStack stack, float x, float y, float width, float height, float radius, float blurRadius, Color color) {
        RockstarBuiltBlur built = Builder.rockstarBlur()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
        drawRockstarRoundedRect(
                stack,
                x,
                y,
                width,
                height,
                radius,
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 60)
        );
    }

     
     
    public void drawShaderBlurRect(MatrixStack stack, float x, float y, float width, float height, float radius, float blurRadius, Color color) {
        BuiltBlur built = Builder.blur()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .blurRadius(blurRadius)
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

     public void drawLiquidGlass(MatrixStack stack, float x, float y, float width, float height, float radius, float fade) {
        Color tint = dev.darkvisuals.client.managers.ThemeManager.getInstance().getThemeColor();
        drawLiquidGlass(stack, x, y, width, height, radius, fade, tint);
    }

    public void drawLiquidGlass(MatrixStack stack, float x, float y, float width, float height, float radius, float fade, Color tint) {
        int tr = tint.getRed(), tg = tint.getGreen(), tb = tint.getBlue();

         
        drawBlurredRect(stack, x, y, width, height, radius, 14f, new Color(tr, tg, tb, clampAlpha(60 * fade)));

         
        drawRoundedRect(stack, x, y, width, height, radius, new Color(tr, tg, tb, clampAlpha(75 * fade)));

         
        drawGradientRect(stack, x, y + radius * 0.5f, width, height - radius,
                new Color(255, 255, 255, clampAlpha(28 * fade)),
                new Color(Math.max(0, tr - 25), Math.max(0, tg - 25), Math.max(0, tb - 25), clampAlpha(65 * fade)),
                false);

         
        drawBorder(stack, x + 0.5f, y + 0.5f, width - 1f, height - 1f, radius - 0.5f,
                2.5f, 0f, new Color(255, 255, 255, clampAlpha(36 * fade)));

         
        drawBorder(stack, x, y, width, height, radius, 0f, 1f, new Color(tr, tg, tb, clampAlpha(110 * fade)));

         
        drawGradientRect(stack, x + radius, y + 0.5f, width - radius * 2f, 1f,
                new Color(255, 255, 255, clampAlpha(150 * fade)),
                new Color(255, 255, 255, clampAlpha(20 * fade)),
                true);
    }

     public void drawHudBackground(MatrixStack stack, float x, float y,
                                  float width, float height, float radius, float fade) {
        if (dev.darkvisuals.client.ui.hud.HudStyle.isGlowing()) {
            Color accent = dev.darkvisuals.client.managers.ThemeManager
                    .getInstance().getCurrentTheme().getAccentColor();

             
            drawGlowOutline(stack, x, y, width, height, radius,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), clampAlpha(110 * fade)),
                    clampAlpha(95 * fade),
                    Math.min(9f, Math.max(5f, radius + 3f)));

             
            drawLiquidGlass(stack, x, y, width, height, radius, fade);
        } else {
            // Minimalistic: soft ambient shadow -> #0D0D0D @ ~85% panel -> hairline edge
            drawBlurredRect(stack, x, y, width, height, radius, 6f,
                    new Color(0, 0, 0, clampAlpha(120 * fade)));
            drawRoundedRect(stack, x, y, width, height, radius,
                    new Color(0x0D, 0x0D, 0x0D, clampAlpha(217 * fade)));
            drawBorder(stack, x, y, width, height, radius, 0f, 1f,
                    new Color(255, 255, 255, clampAlpha(16 * fade)));
        }
    }

     public void drawHudText(MatrixStack stack, Instance font, String text, float x, float y, Color color) {
        if (dev.darkvisuals.client.ui.hud.HudStyle.isGlowing()) {
            drawFont(stack, font, text, x + 0.5f, y + 0.5f,
                    new Color(0, 0, 0, Math.min(90, color.getAlpha())));
        }
        drawFont(stack, font, text, x, y, color);
    }

     public void drawHudPill(MatrixStack stack, float x, float y, float w, float h, float radius, float fade) {
        if (dev.darkvisuals.client.ui.hud.HudStyle.isGlowing()) {
            drawRoundedRect(stack, x, y, w, h, radius, new Color(255, 255, 255, clampAlpha(20 * fade)));
            drawBorder(stack, x, y, w, h, radius, 0f, 1f, new Color(255, 255, 255, clampAlpha(50 * fade)));
        } else {
            drawRoundedRect(stack, x, y, w, h, radius, new Color(0, 0, 0, clampAlpha(90 * fade)));
        }
    }

     public void drawHudBarTrack(MatrixStack stack, float x, float y, float w, float h, float radius, float fade) {
        if (dev.darkvisuals.client.ui.hud.HudStyle.isGlowing()) {
            drawRoundedRect(stack, x, y, w, h, radius, new Color(255, 255, 255, clampAlpha(40 * fade)));
        } else {
            drawRoundedRect(stack, x, y, w, h, radius, new Color(0, 0, 0, clampAlpha(110 * fade)));
        }
    }

    private int clampAlpha(float value) {
        return Math.max(0, Math.min(255, (int) value));
    }

    public void drawBorder(MatrixStack stack, float x, float y, float width, float height, float radius, float internalSmoothness, float externalSmoothness, Color color) {

        BuiltBorder built = Builder.border()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .smoothness(internalSmoothness, externalSmoothness)
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }
    public void drawRoundedRect2(MatrixStack stack, float x, float y, float width, float height, float radius1, float radius2, float radius3, float radius4, Color color) {
        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius1, radius2, radius3, radius4))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawStyledRect(MatrixStack stack, float x, float y, float width, float height, float radius, Color color, int blurAlpha) {
        drawBlurredRect(stack, x, y, width, height, radius, 10f, new Color(255, 255, 255, blurAlpha));
        drawRoundedRect(stack, x, y, width, height, radius, color);
    }
     
    public void drawRect(MatrixStack stack, float x, float y, float width, float height, Color color) {
        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(0))  
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

     
    public void drawGradientRect(MatrixStack stack, float x, float y, float width, float height, Color start, Color end, boolean horizontal) {
        QuadColorState quad;
        if (horizontal) {
             
            quad = new QuadColorState(start, end, end, start);
        } else {
             
            quad = new QuadColorState(start, start, end, end);
        }

        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(0))
                .color(quad)
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawFont(MatrixStack stack, Instance instance, String text, float x, float y, Color color) {
        BuiltText built = Builder.text()
                .size(instance.size())
                .font(instance.font())
                .text(text)
                .thickness(0.05f)
                .color(color)
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawTexture(MatrixStack stack, float x, float y, float width, float height, float radius, Identifier texture, Color color) {
        drawTexture(stack, x, y, width, height, radius, mc.getTextureManager().getTexture(texture), color);
    }

    public void drawTexture(MatrixStack stack, float x, float y, float width, float height, float radius, AbstractTexture texture, Color color) {
        BuiltTexture built = Builder.texture()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .texture(1f, 1f, 1f, 1f, texture)
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawTexture(MatrixStack stack, float x, float y, float width, float height, float radius, float u, float v, float textWidth, float texHeight, Identifier texture, Color color) {
        BuiltTexture built = Builder.texture()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .texture(u, v, textWidth, texHeight, mc.getTextureManager().getTexture(texture))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void startScissor(DrawContext context, float x, float y, float width, float height) {
        context.enableScissor((int) x, (int) y, (int) (x + width), (int) (y + height));
    }

    public void stopScissor(DrawContext context) {
        context.disableScissor();
    }

    public AbstractTexture convert(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        NativeImage img = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) img.setColorArgb(x, y, image.getRGB(x, y));

        return new NativeImageBackedTexture(img);
    }
    public void drawLine(MatrixStack stack,
                         float x1, float y1,
                         float x2, float y2,
                         float width,
                         Color color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length <= 0.5f) return;  

        float angle = (float) Math.atan2(dy, dx);

        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(length, width))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(color))
                .build();

        stack.push();
        stack.translate(x1, y1, 0);
        stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(angle));
        built.render(stack.peek().getPositionMatrix(), 0, -width / 2f);
        stack.pop();
    }

    public void drawGlow(MatrixStack stack, float x, float y, float width, float height, float radius, Color color, float intensity, int layers) {
        float step = intensity / layers;  
        float blurStep = radius / layers;  

        for (int i = 0; i < layers; i++) {
            float alpha = step * (i + 1);
            float layerRadius = radius + blurStep * i;
            float offset = i * 2;  

            drawBlurredRect(
                    stack,
                    x - offset,
                    y - offset,
                    width + offset * 2,
                    height + offset * 2,
                    layerRadius,
                    layerRadius,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) alpha)
            );
        }
    }

    public void drawTextGlow(MatrixStack stack, float x, float y, float width, float height, float radius, Color color, int glowAlpha, int layers) {
        for (int i = layers; i > 0; i--) {
            float factor = i / (float) layers;
            float offset = i * 1.5f;  
            int alpha = (int) (glowAlpha * factor);
            drawBlurredRect(
                    stack,
                    x - offset,
                    y - offset,
                    width + offset * 2,
                    height + offset * 2,
                    radius + offset / 2f,
                    radius + offset / 2f,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha)
            );
        }
    }

    public void drawGlowOutline(MatrixStack stack, float x, float y, float width, float height, float radius, Color glowColor, int intensity, float glowRadius) {
        drawBorder(
                stack,
                x - glowRadius,
                y - glowRadius,
                width + glowRadius * 2,
                height + glowRadius * 2,
                radius + glowRadius,
                1f,           
                glowRadius,   
                new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), intensity)
        );
    }

     public void drawCornerOutline(MatrixStack stack, float x, float y, float width, float height, float cornerSize, Color color) {
        float thickness = 2f;

         
        drawRect(stack, x, y, cornerSize, thickness, color);
        drawRect(stack, x, y, thickness, cornerSize, color);

         
        drawRect(stack, x + width - cornerSize, y, cornerSize, thickness, color);
        drawRect(stack, x + width - thickness, y, thickness, cornerSize, color);

         
        drawRect(stack, x, y + height - thickness, cornerSize, thickness, color);
        drawRect(stack, x, y + height - cornerSize, thickness, cornerSize, color);

         
        drawRect(stack, x + width - cornerSize, y + height - thickness, cornerSize, thickness, color);
        drawRect(stack, x + width - thickness, y + height - cornerSize, thickness, cornerSize, color);
    }

     public void drawRoundedCorner(MatrixStack stack, float x, float y, float width, float height,
                                  float cornerSize, Color cornerColor, float thickness) {

        if (cornerColor.getAlpha() <= 0) return;

         
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(cornerColor.getRed() / 255f, cornerColor.getGreen() / 255f,
                cornerColor.getBlue() / 255f, cornerColor.getAlpha() / 255f);

         
        org.joml.Matrix4f matrix = stack.peek().getPositionMatrix();

         
        drawCornerElement(matrix, x, y, cornerSize, thickness, true, true);  
        drawCornerElement(matrix, x + width - cornerSize, y, cornerSize, thickness, false, true);  
        drawCornerElement(matrix, x, y + height - cornerSize, cornerSize, thickness, true, false);  
        drawCornerElement(matrix, x + width - cornerSize, y + height - cornerSize, cornerSize, thickness, false, false);  

         
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

     private void drawCornerElement(org.joml.Matrix4f matrix, float x, float y, float size, float thickness,
                                   boolean left, boolean top) {

         
        float hX = left ? x : x + size - thickness;
        float hY = top ? y : y + size - thickness;
        float hWidth = left ? size : thickness;
        float hHeight = thickness;

        drawQuad(matrix, hX, hY, hWidth, hHeight);

         
        float vX = left ? x : x + size - thickness;
        float vY = top ? y : y + size - thickness;
        float vWidth = thickness;
        float vHeight = left ? thickness : size;

        drawQuad(matrix, vX, vY, vWidth, vHeight);
    }

     private void drawQuad(org.joml.Matrix4f matrix, float x, float y, float width, float height) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        bufferBuilder.vertex(matrix, x, y, 0).color(1f, 1f, 1f, 1f);
        bufferBuilder.vertex(matrix, x + width, y, 0).color(1f, 1f, 1f, 1f);
        bufferBuilder.vertex(matrix, x + width, y + height, 0).color(1f, 1f, 1f, 1f);
        bufferBuilder.vertex(matrix, x, y + height, 0).color(1f, 1f, 1f, 1f);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

     public void drawRoundedCorner(MatrixStack stack, float x, float y, float width, float height,
                                  float cornerSize, Color cornerColor) {
        drawRoundedCorner(stack, x, y, width, height, cornerSize, cornerColor, 2f);
    }
}