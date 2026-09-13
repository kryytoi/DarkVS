package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventExplosion;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.util.TntCameraShakeState;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.state.TntEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

 
public class TnTEffect extends Module {

    private static final Identifier GLOW_TEXTURE = darkvisuals.id("hud/glow.png");

     
    public final NumberSetting triggerRadius = new NumberSetting("setting.tnteffect.triggerRadius", 20f, 5f, 64f, 1f);

     
    public final BooleanSetting screenFlash = new BooleanSetting("setting.tnteffect.screenFlash", true);
    public final NumberSetting flashDuration = new NumberSetting("setting.tnteffect.flashDuration", 400f, 300f, 500f, 10f, screenFlash::getValue);
    public final NumberSetting flashIntensity = new NumberSetting("setting.tnteffect.flashIntensity", 1.0f, 0.2f, 1.5f, 0.1f, screenFlash::getValue);
    public final ColorSetting flashColor = new ColorSetting("setting.tnteffect.flashColor", new Color(255, 245, 220, 255).getRGB());

     
    public final BooleanSetting shockwaveParticles = new BooleanSetting("setting.tnteffect.shockwaveParticles", true);
    public final NumberSetting particleDensity = new NumberSetting("setting.tnteffect.particleDensity", 60f, 10f, 150f, 5f, shockwaveParticles::getValue);
    public final ColorSetting smokeColor = new ColorSetting("setting.tnteffect.smokeColor", new Color(90, 85, 80, 255).getRGB());
    public final ColorSetting emberColor = new ColorSetting("setting.tnteffect.emberColor", new Color(255, 170, 60, 255).getRGB());

     
    public final BooleanSetting worldShockwave = new BooleanSetting("setting.tnteffect.worldShockwave", true);
    public final ColorSetting worldShockwaveColor = new ColorSetting("setting.tnteffect.worldShockwaveColor", new Color(255, 140, 40, 255).getRGB());
    public final NumberSetting worldShockwaveSpeed = new NumberSetting("setting.tnteffect.worldShockwaveSpeed", 45f, 10f, 120f, 5f, worldShockwave::getValue);

     
    public final BooleanSetting tntDebris = new BooleanSetting("setting.tnteffect.tntDebris", true);
    public final NumberSetting debrisCount = new NumberSetting("setting.tnteffect.debrisCount", 24f, 4f, 60f, 2f, tntDebris::getValue);

     
    public final BooleanSetting cameraShake = new BooleanSetting("setting.tnteffect.cameraShake", true);
    public final NumberSetting shakeIntensity = new NumberSetting("setting.tnteffect.shakeIntensity", 6f, 1f, 15f, 0.5f, cameraShake::getValue);
    public final NumberSetting shakeDuration = new NumberSetting("setting.tnteffect.shakeDuration", 500f, 200f, 1200f, 50f, cameraShake::getValue);

     
    public final BooleanSetting fusePulse = new BooleanSetting("setting.tnteffect.fusePulse", true);
    public final NumberSetting fusePulseThreshold = new NumberSetting("setting.tnteffect.fusePulseThreshold", 30f, 10f, 60f, 5f, fusePulse::getValue);
    public final NumberSetting fusePulseAmplitude = new NumberSetting("setting.tnteffect.fusePulseAmplitude", 0.35f, 0.05f, 0.6f, 0.05f, fusePulse::getValue);
    public final BooleanSetting fuseGlow = new BooleanSetting("setting.tnteffect.fuseGlow", true);
    public final ColorSetting fuseGlowColor = new ColorSetting("setting.tnteffect.fuseGlowColor", new Color(255, 90, 40, 220).getRGB());

    private final Random rnd = new Random();

     
    private final List<FlashPulse> flashes = new ArrayList<>();
     
     
    private final List<ShockParticle> particles = new ArrayList<>();
     
     
    private final List<FuseGlowRequest> fuseGlowQueue = new ArrayList<>();
     
     
     
    private final List<WorldRing> worldRings = new ArrayList<>();

    private static final int RING_SEGMENTS = 96;
      
    private static final long WORLD_SHOCKWAVE_TTL_MS = 5_000L;
      
    private static final float WORLD_SHOCKWAVE_FADE_START = 0.7f;

    public TnTEffect() {
        super("TnTEffect", Category.Render, "module.tnteffect.description");
        getSettings().add(triggerRadius);
        getSettings().add(screenFlash);
        getSettings().add(flashDuration);
        getSettings().add(flashIntensity);
        getSettings().add(flashColor);
        getSettings().add(shockwaveParticles);
        getSettings().add(particleDensity);
        getSettings().add(smokeColor);
        getSettings().add(emberColor);
        getSettings().add(worldShockwave);
        getSettings().add(worldShockwaveColor);
        getSettings().add(worldShockwaveSpeed);
        getSettings().add(tntDebris);
        getSettings().add(debrisCount);
        getSettings().add(cameraShake);
        getSettings().add(shakeIntensity);
        getSettings().add(shakeDuration);
        getSettings().add(fusePulse);
        getSettings().add(fusePulseThreshold);
        getSettings().add(fusePulseAmplitude);
        getSettings().add(fuseGlow);
        getSettings().add(fuseGlowColor);
    }

    @Override
    public void onDisable() {
        synchronized (flashes) { flashes.clear(); }
        synchronized (particles) { particles.clear(); }
        synchronized (fuseGlowQueue) { fuseGlowQueue.clear(); }
        synchronized (worldRings) { worldRings.clear(); }
        super.onDisable();
    }

     
     
     

    @EventHandler
    private void onExplosion(EventExplosion event) {
        if (fullNullCheck()) return;
        ClientPlayerEntity player = mc.player;

        try (var __ = Perf.scopeCpu("TnTEffect.onExplosion")) {
            double distance = player.getPos().distanceTo(event.getCenter());
            double radius = triggerRadius.getValue();
            if (distance > radius) return;

             
            float proximity = (float) Math.max(0.0, Math.min(1.0, 1.0 - distance / radius));
            if (proximity <= 0f) return;

            if (screenFlash.getValue()) {
                synchronized (flashes) {
                    flashes.add(new FlashPulse(System.currentTimeMillis(), (long) (float) flashDuration.getValue(), proximity));
                }
            }

            if (cameraShake.getValue()) {
                TntCameraShakeState.trigger(shakeIntensity.getValue() * proximity, (long) (float) shakeDuration.getValue());
            }

            if (shockwaveParticles.getValue()) {
                spawnShockwave(event.getCenter(), proximity);
            }

            if (tntDebris.getValue()) {
                spawnDebris(event.getCenter(), proximity);
            }

             
             
             
             
            if (worldShockwave.getValue()) {
                synchronized (worldRings) {
                    worldRings.add(new WorldRing(event.getCenter()));
                }
            }
        }
    }

 
    private void spawnDebris(Vec3d center, float proximity) {
        if (mc.world == null) return;
        BlockStateParticleEffect effect = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.TNT.getDefaultState());

        int count = Math.max(4, Math.round(debrisCount.getValue() * (0.4f + 0.6f * proximity)));
        for (int i = 0; i < count; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            double speed = 0.15 + rnd.nextDouble() * 0.35;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = 0.25 + rnd.nextDouble() * 0.35;
            mc.world.addParticle(effect,
                    center.x + (rnd.nextDouble() - 0.5) * 0.4,
                    center.y + 0.1 + rnd.nextDouble() * 0.3,
                    center.z + (rnd.nextDouble() - 0.5) * 0.4,
                    vx, vy, vz);
        }
    }

    private void spawnShockwave(Vec3d center, float proximity) {
        int count = Math.max(4, Math.round(particleDensity.getValue() * (0.35f + 0.65f * proximity)));

        synchronized (particles) {
             
            int ringCount = count;
            for (int i = 0; i < ringCount; i++) {
                double angle = (Math.PI * 2 * i) / ringCount + rnd.nextDouble() * 0.15;
                double speed = 0.28 + rnd.nextDouble() * 0.22;
                Vec3d velocity = new Vec3d(Math.cos(angle) * speed, 0.02 + rnd.nextDouble() * 0.05, Math.sin(angle) * speed);
                Vec3d pos = center.add(0, 0.1, 0);
                long life = 350 + rnd.nextInt(200);
                particles.add(new ShockParticle(ShockParticle.Type.EMBER, pos, velocity, emberColor.getColor(), 0.35f, life));
            }

             
            int smokeCount = Math.max(3, count / 3);
            for (int i = 0; i < smokeCount; i++) {
                double angle = rnd.nextDouble() * Math.PI * 2;
                double horizontalSpeed = rnd.nextDouble() * 0.12;
                Vec3d velocity = new Vec3d(Math.cos(angle) * horizontalSpeed, 0.06 + rnd.nextDouble() * 0.08, Math.sin(angle) * horizontalSpeed);
                Vec3d pos = center.add(rnd.nextDouble() * 0.6 - 0.3, rnd.nextDouble() * 0.4, rnd.nextDouble() * 0.6 - 0.3);
                long life = 900 + rnd.nextInt(700);
                particles.add(new ShockParticle(ShockParticle.Type.SMOKE, pos, velocity, smokeColor.getColor(), 1.1f + rnd.nextFloat() * 0.6f, life));
            }

             
            int flashCount = 2;
            for (int i = 0; i < flashCount; i++) {
                particles.add(new ShockParticle(ShockParticle.Type.FLASH, center.add(0, 0.2, 0), Vec3d.ZERO, flashColor.getColor(), 2.2f, 180));
            }
        }
    }

     
     
     

 
    public float getFusePulseScale(float fuse) {
        float threshold = fusePulseThreshold.getValue();
        if (fuse <= 0f || fuse > threshold) return 1f;

        float intensity = 1f - (fuse / threshold);  
        float freq = 2.0f + intensity * 6.0f;
        float amp = fusePulseAmplitude.getValue() * intensity;
         
         
        return 1f + (float) Math.sin(fuse * freq) * amp;
    }

      
    public void queueFuseGlow(TntEntityRenderState state) {
        float threshold = fusePulseThreshold.getValue();
        if (state.fuse <= 0f || state.fuse > threshold) return;
        float intensity = 1f - (state.fuse / threshold);
        if (intensity <= 0.02f) return;
        fuseGlowQueue.add(new FuseGlowRequest(state.x, state.y + state.height * 0.5, state.z, intensity));
    }

     
     
     

    @EventHandler
    private void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        try (var __ = Perf.scopeCpu("TnTEffect.onRender3D")) {
            renderShockwaveParticles(e.getMatrices());
            renderFuseGlow(e.getMatrices());
            renderWorldShockwave(e.getMatrices());
        }
    }

     
     
     

 
    private void renderWorldShockwave(MatrixStack matrices) {
        synchronized (worldRings) {
            worldRings.removeIf(WorldRing::isDead);
            if (worldRings.isEmpty()) return;

            Vec3d cam = mc.gameRenderer.getCamera().getPos();
            Color color = worldShockwaveColor.getColor();
            float speed = worldShockwaveSpeed.getValue();

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            for (WorldRing ring : worldRings) {
                float t = ring.progress();
                float radius = t * speed * (WORLD_SHOCKWAVE_TTL_MS / 1000f);

                 
                float fade = t < WORLD_SHOCKWAVE_FADE_START
                        ? 1f
                        : Easing.OUT_QUAD.apply(1f - (t - WORLD_SHOCKWAVE_FADE_START) / (1f - WORLD_SHOCKWAVE_FADE_START));
                int crestAlpha = (int) (220 * fade);
                if (crestAlpha <= 2 || radius <= 0.05f) continue;

                matrices.push();
                matrices.translate(ring.origin.x - cam.x, ring.origin.y - cam.y, ring.origin.z - cam.z);
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                emitShockwaveRing(matrix, radius, color.getRed(), color.getGreen(), color.getBlue(), crestAlpha);
                matrices.pop();
            }

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

      
    private void emitShockwaveRing(Matrix4f matrix, float radius, int r, int g, int b, int crestAlpha) {
        float border = Math.min(1.2f, radius * 0.5f);
        float borderInnerR = Math.max(0f, radius - border);
        float glowInnerR = borderInnerR * 0.3f;
        float outerR = radius + 0.5f;
        int borderInnerAlpha = (int) (crestAlpha * 0.28f);

        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder inner = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            inner.vertex(matrix, cos * glowInnerR, 0.05f, sin * glowInnerR).color(r, g, b, 0);
            inner.vertex(matrix, cos * borderInnerR, 0.05f, sin * borderInnerR).color(r, g, b, borderInnerAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(inner.end());

        BufferBuilder crest = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            crest.vertex(matrix, cos * borderInnerR, 0.05f, sin * borderInnerR).color(r, g, b, borderInnerAlpha);
            crest.vertex(matrix, cos * radius, 0.05f, sin * radius).color(r, g, b, crestAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(crest.end());

        BufferBuilder outer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            outer.vertex(matrix, cos * radius, 0.05f, sin * radius).color(r, g, b, crestAlpha);
            outer.vertex(matrix, cos * outerR, 0.05f, sin * outerR).color(r, g, b, 0);
        }
        BufferRenderer.drawWithGlobalProgram(outer.end());
    }

    private void renderShockwaveParticles(MatrixStack matrices) {
        synchronized (particles) {
            particles.removeIf(ShockParticle::isDead);
            if (particles.isEmpty()) return;

            Vec3d camera = mc.gameRenderer.getCamera().getPos();

            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

            List<ShockParticle> snapshot = new ArrayList<>(particles);
            for (ShockParticle particle : snapshot) {
                particle.update();

                 
                if (particle.type == ShockParticle.Type.SMOKE) {
                    RenderSystem.defaultBlendFunc();
                } else {
                    RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                }

                double x = particle.position.x - camera.x;
                double y = particle.position.y - camera.y;
                double z = particle.position.z - camera.z;

                matrices.push();
                matrices.translate((float) x, (float) y, (float) z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                float half = particle.size / 2f;
                int alpha = (int) (particle.alpha * 255);
                Color c = particle.color;

                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, half, half, 0).texture(1, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                BufferRenderer.drawWithGlobalProgram(buffer.end());

                matrices.pop();
            }

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private void renderFuseGlow(MatrixStack matrices) {
        if (fuseGlowQueue.isEmpty()) return;

        List<FuseGlowRequest> snapshot;
        synchronized (fuseGlowQueue) {
            snapshot = new ArrayList<>(fuseGlowQueue);
            fuseGlowQueue.clear();
        }

        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        Color glow = fuseGlowColor.getColor();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        for (FuseGlowRequest req : snapshot) {
            double x = req.x - camera.x;
            double y = req.y - camera.y;
            double z = req.z - camera.z;

            float size = 0.9f + req.intensity * 0.9f;
            int alpha = (int) (180 * req.intensity);

            matrices.push();
            matrices.translate((float) x, (float) y, (float) z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = size / 2f;

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(glow.getRed(), glow.getGreen(), glow.getBlue(), alpha);
            buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(glow.getRed(), glow.getGreen(), glow.getBlue(), alpha);
            buffer.vertex(matrix, half, half, 0).texture(1, 0).color(glow.getRed(), glow.getGreen(), glow.getBlue(), alpha);
            buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(glow.getRed(), glow.getGreen(), glow.getBlue(), alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

     
     
     

    @EventHandler
    private void onRender2D(EventRender2D event) {
        if (!screenFlash.getValue()) return;

        synchronized (flashes) {
            flashes.removeIf(FlashPulse::isDead);
            if (flashes.isEmpty()) return;

            try (var __ = Perf.scopeCpu("TnTEffect.onRender2D")) {
                 
                 
                float peak = 0f;
                float progress = 1f;  
                for (FlashPulse pulse : flashes) {
                    float p = pulse.progress();
                    float strength = pulse.strength * (1f - p);
                    if (strength >= peak) {
                        peak = strength;
                        progress = p;
                    }
                }
                if (peak <= 0.01f) return;

                int width = event.getContext().getScaledWindowWidth();
                int height = event.getContext().getScaledWindowHeight();
                float intensity = Math.min(1f, peak * flashIntensity.getValue());

                 
                 
                 
                float invertPhase = Easing.OUT_QUAD.apply(Math.max(0f, 1f - progress / 0.25f));
                if (invertPhase > 0.02f) {
                    RenderSystem.enableColorLogicOp();
                    RenderSystem.logicOp(GlStateManager.LogicOp.INVERT);
                     
                     
                     
                    if (invertPhase > 0.5f) {
                        event.getContext().fill(0, 0, width, height, 0xFFFFFFFF);
                    }
                    RenderSystem.disableColorLogicOp();
                }

                 
                 
                float fade = Easing.EASE_OUT_CIRC.apply(1f - progress);
                Color base = flashColor.getColor();
                int overlayAlpha = (int) (110 * intensity * fade);
                if (overlayAlpha > 0) {
                    int argb = (overlayAlpha << 24) | (base.getRed() << 16) | (base.getGreen() << 8) | base.getBlue();
                    event.getContext().fill(0, 0, width, height, argb);
                }

                drawVignette(event, width, height, intensity * fade);
            }
        }
    }

    private void drawVignette(EventRender2D event, int width, int height, float strength) {
        if (strength <= 0.01f) return;
        int maxAlpha = (int) (170 * strength);
        int band = Math.max(24, Math.round(Math.min(width, height) * 0.18f));

         
         
         
        event.getContext().fillGradient(0, 0, width, band, band(maxAlpha), 0);
        event.getContext().fillGradient(0, height - band, width, height, 0, band(maxAlpha));
        event.getContext().fillGradient(0, 0, band, height, band(maxAlpha), 0);
        event.getContext().fillGradient(width - band, 0, width, height, 0, band(maxAlpha));
    }

    private int band(int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24);
    }

     
     
     

    private record FlashPulse(long startMs, long durationMs, float strength) {
        boolean isDead() {
            return System.currentTimeMillis() - startMs >= durationMs;
        }

          
        float progress() {
            long elapsed = System.currentTimeMillis() - startMs;
            return Math.max(0f, Math.min(1f, elapsed / (float) durationMs));
        }
    }

    private static final class ShockParticle {
        enum Type { EMBER, SMOKE, FLASH }

        final Type type;
        Vec3d position;
        Vec3d velocity;
        final Color color;
        float size;
        final long lifeTime;
        final long birthTime;
        float alpha = 1f;

        ShockParticle(Type type, Vec3d position, Vec3d velocity, Color color, float size, long lifeTime) {
            this.type = type;
            this.position = position;
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.lifeTime = lifeTime;
            this.birthTime = System.currentTimeMillis();
        }

        boolean isDead() {
            return System.currentTimeMillis() - birthTime >= lifeTime;
        }

        void update() {
            float progress = Math.min(1f, (System.currentTimeMillis() - birthTime) / (float) lifeTime);
            position = position.add(velocity.multiply(0.1));

            switch (type) {
                case EMBER -> {
                    velocity = velocity.multiply(0.92).add(0, -0.004, 0);
                    alpha = 1f - progress;
                }
                case SMOKE -> {
                    velocity = velocity.multiply(0.985);
                    size += 0.015f;
                    alpha = (1f - progress) * 0.55f;
                }
                case FLASH -> {
                    alpha = (1f - progress) * (1f - progress);
                    size *= 0.94f;
                }
            }
        }
    }

    private record FuseGlowRequest(double x, double y, double z, float intensity) {}

      
    private record WorldRing(Vec3d origin, long spawnTime) {
        WorldRing(Vec3d origin) {
            this(origin, System.currentTimeMillis());
        }

        boolean isDead() {
            return System.currentTimeMillis() - spawnTime >= WORLD_SHOCKWAVE_TTL_MS;

        }

          
        float progress() {
            long elapsed = System.currentTimeMillis() - spawnTime;
            return Math.max(0f, Math.min(1f, elapsed / (float) WORLD_SHOCKWAVE_TTL_MS));
        }
    }
}
