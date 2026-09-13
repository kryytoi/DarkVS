package dev.darkvisuals.client.util.render;

import net.minecraft.client.render.VertexConsumer;

/**
 * Обёртка VertexConsumer, принудительно подменяющая альфу цвета вершин.
 * Позволяет рисовать модели блоков полупрозрачными («призраками»).
 */
public final class ForcedAlphaVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final int alpha;

    public ForcedAlphaVertexConsumer(VertexConsumer delegate, int alpha) {
        this.delegate = delegate;
        this.alpha = Math.max(0, Math.min(255, alpha));
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, this.alpha);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        delegate.light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }
}
