package dev.darkvisuals.client.render.renderers.impl;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.darkvisuals.client.render.msdf.MsdfFont;
import dev.darkvisuals.client.render.providers.ColorProvider;
import dev.darkvisuals.client.render.providers.ResourceProvider;
import dev.darkvisuals.client.render.renderers.IRenderer;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;

public record BuiltText(
		MsdfFont font,
		String text,
		float size,
		float thickness,
		int color,
		float smoothness,
		float spacing,
		int outlineColor,
		float outlineThickness
) implements IRenderer {

	private static final ShaderProgramKey MSDF_FONT_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("msdf_font"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

	@Override
	public void render(Matrix4f matrix, float x, float y, float z) {
		if (this.font == null || this.font.getAtlas() == null) return;
		if (this.text == null || this.text.isEmpty() || this.font.getWidth(this.text, this.size) <= 0.0f) return;

		try {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.setShaderTexture(0, this.font.getTextureId());

			boolean outlineEnabled = (this.outlineThickness > 0.0f);

			ShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER_KEY);
			if (shader != null) {
				var rangeU = shader.getUniform("Range");
				if (rangeU != null) rangeU.set(this.font.getAtlas().range());
				var thickU = shader.getUniform("Thickness");
				if (thickU != null) thickU.set(this.thickness);
				var smoothU = shader.getUniform("Smoothness");
				if (smoothU != null) smoothU.set(this.smoothness);
				var outlU = shader.getUniform("Outline");
				if (outlU != null) outlU.set(outlineEnabled ? 1 : 0);

				if (outlineEnabled) {
					var outThickU = shader.getUniform("OutlineThickness");
					if (outThickU != null) outThickU.set(this.outlineThickness);
					float[] outlineComponents = ColorProvider.normalize(this.outlineColor);
					var outColU = shader.getUniform("OutlineColor");
					if (outColU != null) outColU.set(outlineComponents[0], outlineComponents[1], outlineComponents[2], outlineComponents[3]);
				}
			}

			BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
			this.font.applyGlyphs(matrix, builder, this.text, this.size, (this.thickness + this.outlineThickness * 0.5f) * 0.5f * this.size, this.spacing, x, y + this.font.getMetrics().baselineHeight() * this.size, z, this.color);
			var built = builder.endNullable();
			if (built != null) {
				BufferRenderer.drawWithGlobalProgram(built);
			}
		} catch (Throwable ignored) {
		} finally {
			try {
				RenderSystem.setShaderTexture(0, 0);
				RenderSystem.enableCull();
				RenderSystem.disableBlend();
			} catch (Throwable ignored) {}
		}
	}
}
