package dev.darkvisuals.client.render.builders.impl;

import dev.darkvisuals.client.render.builders.AbstractBuilder;
import dev.darkvisuals.client.render.builders.states.QuadColorState;
import dev.darkvisuals.client.render.builders.states.QuadRadiusState;
import dev.darkvisuals.client.render.builders.states.SizeState;
import dev.darkvisuals.client.render.renderers.impl.BuiltLiquidGlass;

public final class LiquidGlassBuilder extends AbstractBuilder<BuiltLiquidGlass> {

    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float[] cutSizes;
    private float distortion;
    private float waveSize;
    private float edgeLight;
    private float shine;
    private float blurRadius;
    private float alpha;
    private float blob2X, blob2Y, blob2Width, blob2Height;
    private float mergeRadius;
    private boolean merge;

    public LiquidGlassBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public LiquidGlassBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public LiquidGlassBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public LiquidGlassBuilder cutSizes(float[] cutSizes) {
        this.cutSizes = cutSizes;
        return this;
    }

    public LiquidGlassBuilder distortion(float distortion) {
        this.distortion = distortion;
        return this;
    }

    public LiquidGlassBuilder waveSize(float waveSize) {
        this.waveSize = waveSize;
        return this;
    }

    public LiquidGlassBuilder edgeLight(float edgeLight) {
        this.edgeLight = edgeLight;
        return this;
    }

    public LiquidGlassBuilder shine(float shine) {
        this.shine = shine;
        return this;
    }

    public LiquidGlassBuilder blurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
        return this;
    }

    public LiquidGlassBuilder alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public LiquidGlassBuilder secondBlob(float x, float y, float width, float height) {
        this.blob2X = x;
        this.blob2Y = y;
        this.blob2Width = width;
        this.blob2Height = height;
        return this;
    }

    public LiquidGlassBuilder mergeRadius(float mergeRadius) {
        this.mergeRadius = mergeRadius;
        return this;
    }

    public LiquidGlassBuilder merge(boolean merge) {
        this.merge = merge;
        return this;
    }

    @Override
    protected BuiltLiquidGlass _build() {
        return new BuiltLiquidGlass(
            this.size,
            this.radius,
            this.color,
            this.cutSizes,
            this.distortion,
            this.waveSize,
            this.edgeLight,
            this.shine,
            this.blurRadius,
            this.alpha,
            this.blob2X, this.blob2Y, this.blob2Width, this.blob2Height,
            this.mergeRadius,
            this.merge
        );
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.WHITE;
        this.cutSizes = null;
        this.distortion = 12.0f;
        this.waveSize = 3.0f;
        this.edgeLight = 4.0f;
        this.shine = 3.0f;
        this.blurRadius = 6.0f;
        this.alpha = 1.0f;
        this.blob2X = 0f;
        this.blob2Y = 0f;
        this.blob2Width = 1f;
        this.blob2Height = 1f;
        this.mergeRadius = 0f;
        this.merge = false;
    }
}
