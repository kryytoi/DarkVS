package dev.darkvisuals.client.render.renderers.impl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.render.builders.states.QuadColorState;
import dev.darkvisuals.client.render.builders.states.QuadRadiusState;
import dev.darkvisuals.client.render.builders.states.SizeState;
import dev.darkvisuals.client.render.providers.ResourceProvider;
import dev.darkvisuals.client.render.renderers.IRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

public record RockstarBuiltBlur(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        float blurRadius
    ) implements IRenderer {

	private static final ShaderProgramKey BLUR_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("rock_blur"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    private static final Supplier<SimpleFramebuffer> TEMP_FBO_SUPPLIER = Suppliers.memoize(() -> new SimpleFramebuffer(1920, 1024, false));
    // MAIN_FBO намеренно НЕ кешируется — Minecraft пересоздаёт его при запуске/ресайзе,
    // и закешированный указатель становится удалённым FBO → GL_INVALID_VALUE каждый кадр.

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        if (main == null) return;

        SimpleFramebuffer fbo = TEMP_FBO_SUPPLIER.get();
        if (fbo.textureWidth != main.textureWidth || fbo.textureHeight != main.textureHeight) fbo.resize(main.textureWidth, main.textureHeight);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        fbo.beginWrite(true);
        main.draw(fbo.textureWidth, fbo.textureHeight);
        main.beginWrite(true);
        RenderSystem.setShaderTexture(0, fbo.getColorAttachment());

        float width = this.size.width(), height = this.size.height();

		ShaderProgram shader = RenderSystem.setShader(BLUR_SHADER_KEY);
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4());
        shader.getUniform("Smoothness").set(this.smoothness);
        shader.getUniform("BlurRadius").set(this.blurRadius);

		BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, x, y, z).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).color(this.color.color4());
        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}