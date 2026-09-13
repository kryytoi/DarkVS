package dev.darkvisuals.client.util.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

 
public final class BoundsCapturingProvider implements VertexConsumerProvider {
    private final VertexConsumerProvider delegate;

    public float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
    public float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

    public BoundsCapturingProvider(VertexConsumerProvider delegate) {
        this.delegate = delegate;
    }

    public boolean hasBounds() {
        return minX <= maxX;
    }

    private void track(float x, float y, float z) {
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (z < minZ) minZ = z;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
        if (z > maxZ) maxZ = z;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        VertexConsumer real = delegate.getBuffer(layer);
        return new VertexConsumer() {
            @Override
            public VertexConsumer vertex(float x, float y, float z) {
                track(x, y, z);
                real.vertex(x, y, z);
                return this;
            }

            @Override
            public VertexConsumer color(int red, int green, int blue, int alpha) {
                real.color(red, green, blue, alpha);
                return this;
            }

            @Override
            public VertexConsumer texture(float u, float v) {
                real.texture(u, v);
                return this;
            }

            @Override
            public VertexConsumer overlay(int u, int v) {
                real.overlay(u, v);
                return this;
            }

            @Override
            public VertexConsumer light(int u, int v) {
                real.light(u, v);
                return this;
            }

            @Override
            public VertexConsumer normal(float x, float y, float z) {
                real.normal(x, y, z);
                return this;
            }
        };
    }
}
