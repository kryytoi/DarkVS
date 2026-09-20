package dev.darkvisuals.client.render.renderers.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;

import org.joml.Matrix4f;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.darkvisuals.client.render.builders.states.QuadColorState;
import dev.darkvisuals.client.render.builders.states.QuadRadiusState;
import dev.darkvisuals.client.render.builders.states.SizeState;
import dev.darkvisuals.client.render.providers.ResourceProvider;
import dev.darkvisuals.client.render.renderers.IRenderer;

/**
 * Порт LiquidGlass с 1.21.11 (RenderPipeline/GpuBuffer) на 1.21.4 (JsonShaderProgram).
 * Вместо uniform-буфера используются классические uniform'ы, вместо ScreenBlur —
 * копирование главного фреймбуфера во временный FBO и сэмпл из него (как в BuiltBlur).
 */
public record BuiltLiquidGlass(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float[] cutSizes,
        float distortion,
        float waveSize,
        float edgeLight,
        float shine,
        float blurRadius,
        float alpha,
        float blob2X, float blob2Y, float blob2Width, float blob2Height,
        float mergeRadius,
        boolean merge
    ) implements IRenderer {

    private static final ShaderProgramKey GLASS_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("glass"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    private static final Supplier<SimpleFramebuffer> TEMP_FBO_SUPPLIER = Suppliers.memoize(() -> new SimpleFramebuffer(1920, 1024, false));
    // ВАЖНО: главный фреймбуфер НЕ кешируем статически — Minecraft пересоздаёт его
    // при запуске/изменении размера окна, и закешированный указатель становится
    // удалённым FBO. Привязка такого имени даёт GL_INVALID_VALUE каждый кадр
    // (тысячи ошибок в минуту → падение FPS и нестабильность драйвера).
    private static final float[] NO_CUTS = {0f, 0f, 0f, 0f};

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getFramebuffer() == null) return;

        float width = Math.max(this.size.width(), 1f);
        float height = Math.max(this.size.height(), 1f);

        // при слиянии квад должен покрывать оба блоба + радиус слияния
        float drawX = x, drawY = y, drawW = width, drawH = height;
        if (this.merge) {
            float pad = Math.max(0f, this.mergeRadius);
            drawX = Math.min(x, this.blob2X) - pad;
            drawY = Math.min(y, this.blob2Y) - pad;
            float right = Math.max(x + width, this.blob2X + this.blob2Width) + pad;
            float bottom = Math.max(y + height, this.blob2Y + this.blob2Height) + pad;
            drawW = right - drawX;
            drawH = bottom - drawY;
        }

        SimpleFramebuffer fbo = TEMP_FBO_SUPPLIER.get();
        Framebuffer main = client.getFramebuffer();
        if (main == null) return;
        if (fbo.textureWidth != main.textureWidth || fbo.textureHeight != main.textureHeight) {
            fbo.resize(main.textureWidth, main.textureHeight);
        }

        // захватываем содержимое экрана за стеклом
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        fbo.beginWrite(true);
        main.draw(fbo.textureWidth, fbo.textureHeight);
        main.beginWrite(true);
        RenderSystem.setShaderTexture(0, fbo.getColorAttachment());

        float guiScale = (float) client.getWindow().getScaleFactor();
        float screenWidth = client.getWindow().getScaledWidth();
        float screenHeight = client.getWindow().getScaledHeight();

        float[] cuts = this.cutSizes == null || this.cutSizes.length < 4 ? NO_CUTS : this.cutSizes;
        float[] radii = {
                this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4()
        };

        int tint = this.color.color1();
        float tintA = ((tint >> 24) & 0xFF) / 255.0f;
        float tintR = ((tint >> 16) & 0xFF) / 255.0f;
        float tintG = ((tint >> 8) & 0xFF) / 255.0f;
        float tintB = (tint & 0xFF) / 255.0f;

        ShaderProgram shader = RenderSystem.setShader(GLASS_SHADER_KEY);
        shader.getUniform("Rect").set(drawX, drawY, drawW, drawH);
        shader.getUniform("Screen").set(screenWidth, screenHeight, guiScale, Math.max(0f, Math.min(1f, this.alpha)));
        shader.getUniform("Radius").set(radii[0], radii[1], radii[2], radii[3]);
        shader.getUniform("Cuts").set(cuts[0], cuts[1], cuts[2], cuts[3]);
        shader.getUniform("Tint").set(tintR, tintG, tintB, tintA);
        shader.getUniform("Glass").set(
                Math.max(0f, this.distortion),
                Math.max(0f, this.waveSize),
                Math.max(0f, this.edgeLight),
                Math.max(0f, this.shine));
        shader.getUniform("Blob0").set(x, y, width, height);
        shader.getUniform("Blob1").set(this.blob2X, this.blob2Y, this.blob2Width, this.blob2Height);
        shader.getUniform("Blobs").set(this.merge ? 2f : 1f, Math.max(0f, this.mergeRadius));
        shader.getUniform("BlurRadius").set(Math.max(0f, this.blurRadius));

        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, drawX, drawY, z).color(-1);
        builder.vertex(matrix, drawX, drawY + drawH, z).color(-1);
        builder.vertex(matrix, drawX + drawW, drawY + drawH, z).color(-1);
        builder.vertex(matrix, drawX + drawW, drawY, z).color(-1);
        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
