package dev.darkvisuals.client.render.msdf;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import dev.darkvisuals.darkvisuals;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.darkvisuals.client.render.msdf.FontData.AtlasData;
import dev.darkvisuals.client.render.msdf.FontData.GlyphData;
import dev.darkvisuals.client.render.msdf.FontData.MetricsData;
import dev.darkvisuals.client.render.providers.ResourceProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

public final class MsdfFont {

	private final String name;
	private final AbstractTexture texture;
	private final AtlasData atlas;
	private final MetricsData metrics;
	private final Map<Integer, MsdfGlyph> glyphs;
	private final Map<Integer, Map<Integer, Float>> kernings;

	private MsdfFont(String name, AbstractTexture texture, AtlasData atlas, MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
		this.name = name;
		this.texture = texture;
		this.atlas = atlas;
		this.metrics = metrics;
		this.glyphs = glyphs;
		this.kernings = kernings;
	}

	public int getTextureId() {
		return this.texture.getGlId();
	}

	public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color) {
		int prevChar = -1;
		for (int i = 0; i < text.length(); i++) {
			int _char = text.charAt(i);
			MsdfGlyph glyph = this.glyphs.get(_char);

			if (glyph == null) continue;

			Map<Integer, Float> kerning = this.kernings.get(prevChar);
			if (kerning != null) {
				x += kerning.getOrDefault(_char, 0.0f) * size;
			}

			x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
			prevChar = _char;
		}
	}

	public float getWidth(String text, float size) {
		int prevChar = -1;
		float width = 0.0f;
		for (int i = 0; i < text.length(); i++) {
			int _char = text.charAt(i);
			MsdfGlyph glyph = this.glyphs.get(_char);
			if (glyph == null) continue;
			Map<Integer, Float> kerning = this.kernings.get(prevChar);
			if (kerning != null) width += kerning.getOrDefault(_char, 0.0f) * size;
			width += (glyph.getWidth(size) * 1.05f);
			prevChar = _char;
		}

		return width;
	}

	public float getHeight(float size) {
		return ((metrics.ascender() * size) + (Math.abs(metrics.descender() * size))) * 1.05f;
	}

	public String getName() {
		return this.name;
	}

	public AtlasData getAtlas() {
		return this.atlas;
	}

	public MetricsData getMetrics() {
		return this.metrics;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String name = "?";
		private Identifier dataIdentifer;
		private Identifier atlasIdentifier;

		private Builder() {}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder data(String dataFileName) {
			this.dataIdentifer = darkvisuals.id("fonts/" + dataFileName + ".json");
			return this;
		}

		public Builder atlas(String atlasFileName) {
			this.atlasIdentifier = darkvisuals.id("fonts/" + atlasFileName + ".png");
			return this;
		}

		public MsdfFont build() {
			dev.darkvisuals.client.render.msdf.FontData data =
					ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);

			if (data == null) {
				throw new RuntimeException("Failed to read font data file: " + this.dataIdentifer
						+ "; Are you sure this is a json file? Check its syntax.");
			}

			 
			 
			 
			 
			 
			 
			 
			if (data.atlas() == null || data.metrics() == null
					|| data.glyphs() == null || data.kernings() == null) {
				throw new RuntimeException("Font data not fully loaded yet: " + this.dataIdentifer
						+ " (atlas/metrics/glyphs/kernings is null). Resources are probably still reloading — will retry.");
			}

			 
			 
			net.minecraft.client.texture.TextureManager tm =
					MinecraftClient.getInstance().getTextureManager();

			AbstractTexture texture = tm.getTexture(this.atlasIdentifier);
			if (texture == null) {
				 
				tm.registerTexture(this.atlasIdentifier,
						new net.minecraft.client.texture.ResourceTexture(this.atlasIdentifier));
				texture = tm.getTexture(this.atlasIdentifier);
			}
			if (texture == null) {
				throw new RuntimeException("Font atlas texture not found/registered: " + this.atlasIdentifier);
			}

			final AbstractTexture tex = texture;
			RenderSystem.recordRenderCall(() -> tex.setFilter(true, false));

			float aWidth = data.atlas().width();
			float aHeight = data.atlas().height();
			Map<Integer, MsdfGlyph> glyphs = data.glyphs().stream()
					.collect(Collectors.toMap(
							GlyphData::unicode,
							(glyphData) -> new MsdfGlyph(glyphData, aWidth, aHeight),
							(a, b) -> a));  

			Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
			data.kernings().forEach((kerning) -> {
				Map<Integer, Float> map = kernings.computeIfAbsent(kerning.leftChar(), k -> new HashMap<>());
				map.put(kerning.rightChar(), kerning.advance());
			});

			return new MsdfFont(this.name, tex, data.atlas(), data.metrics(), glyphs, kernings);
		}
	}
}
