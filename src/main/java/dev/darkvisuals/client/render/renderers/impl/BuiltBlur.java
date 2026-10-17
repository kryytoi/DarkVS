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

public record BuiltBlur(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        float blurRadius
    ) implements IRenderer {

	private static final ShaderProgramKey BLUR_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("blur"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    private static final Supplier<SimpleFramebuffer> TEMP_FBO_SUPPLIER = Suppliers.memoize(() -> new SimpleFramebuffer(1920, 1024, false));

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        Framebuffer main = client.getFramebuffer();
        if (main == null) return;
        if (main.textureWidth <= 0 || main.textureHeight <= 0) return;

        float width = this.size.width(), height = this.size.height();
        if (width <= 0 || height <= 0) return;

        try {
            SimpleFramebuffer fbo = TEMP_FBO_SUPPLIER.get();
            if (fbo == null) return;
            if (fbo.textureWidth != main.textureWidth || fbo.textureHeight != main.textureHeight) {
                fbo.resize(main.textureWidth, main.textureHeight);
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            fbo.beginWrite(true);
            main.draw(fbo.textureWidth, fbo.textureHeight);
            main.beginWrite(true);
            RenderSystem.setShaderTexture(0, fbo.getColorAttachment());

            ShaderProgram shader = RenderSystem.setShader(BLUR_SHADER_KEY);
            if (shader != null) {
                if (shader.getUniform("Size") != null) shader.getUniform("Size").set(width, height);
                if (shader.getUniform("Radius") != null) shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4());
                if (shader.getUniform("Smoothness") != null) shader.getUniform("Smoothness").set(this.smoothness);
                if (shader.getUniform("BlurRadius") != null) shader.getUniform("BlurRadius").set(this.blurRadius);

                BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                builder.vertex(matrix, x, y, z).color(this.color.color1());
                builder.vertex(matrix, x, y + height, z).color(this.color.color2());
                builder.vertex(matrix, x + width, y + height, z).color(this.color.color3());
                builder.vertex(matrix, x + width, y, z).color(this.color.color4());
                BufferRenderer.drawWithGlobalProgram(builder.end());
            }
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        } catch (Throwable ignored) {
        }
    }
}
