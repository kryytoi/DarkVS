package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import dev.darkvisuals.client.util.animations.Animation;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.world.WorldUtils;
import dev.darkvisuals.client.util.async.Async;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import meteordevelopment.orbit.EventHandler;
import java.awt.*;

import dev.darkvisuals.client.render.renderers.JelloRenderer;
import dev.darkvisuals.client.render.renderers.SoulRenderer;

public class TargetEsp extends Module implements ThemeManager.ThemeChangeListener {

    private static final float BASE_SIZE = 5.0f;
    private static final String markerType = "";

     
    private final BooleanSetting modeMarker = new BooleanSetting("mode.marker", true, () -> false);
    private final BooleanSetting modeGhosts = new BooleanSetting("mode.soul", false, () -> false);
    private final BooleanSetting modeJello = new BooleanSetting("mode.circle", false, () -> false);
    private final BooleanSetting modeCrystals = new BooleanSetting("mode.crystals", false, () -> false);
    private final BooleanSetting modeSvenya = new BooleanSetting("mode.svenya", false, () -> false);  

    private final BooleanSetting modeMarkerOld = new BooleanSetting("old", false, () -> false);
    private final BooleanSetting modeMarkerNew = new BooleanSetting("new", true, () -> false);

    private final ListSetting mode = new ListSetting(
            I18n.translate("setting.animationMode"),
            true,
            modeMarker, modeGhosts, modeJello, modeCrystals, modeSvenya  
    );

     
    private final NumberSetting markerScale = new NumberSetting("setting.markerSize", 3.0f, 0.5f, 8.0f, 0.1f);
    private final BooleanSetting markerGlow = new BooleanSetting("setting.markerGlow", true, () -> modeMarker.getValue());
    private final NumberSetting markerGlowIntensity = new NumberSetting("setting.glowIntensity", 0.8f, 0.1f, 2.0f, 0.1f);
    private final BooleanSetting markerHitFlash = new BooleanSetting("setting.hitFlash", true, () -> modeMarker.getValue());
    private ListSetting markerMode = new ListSetting("setting.markerMode", true, modeMarkerOld, modeMarkerNew);

     
    private final NumberSetting ghostsScale = new NumberSetting("setting.ghostsScale", 0.3f, 0.1f, 1.0f, 0.05f);
    private final NumberSetting ghostsParticleDensity = new NumberSetting("setting.particleDensity", 14, 5, 20, 1);
    private final BooleanSetting ghostsGlow = new BooleanSetting("setting.markerGlow", true, () -> modeGhosts.getValue());
    private final NumberSetting ghostsGlowIntensity = new NumberSetting("setting.glowIntensity", 0.5f, 0.1f, 1.5f, 0.1f);
    private final NumberSetting ghostsAlpha = new NumberSetting("setting.alpha", 1.0f, 0.1f, 1.0f, 0.1f);

     
    private final NumberSetting jelloHeight = new NumberSetting("setting.jelloHeight", 1.5f, 0.5f, 3.0f, 0.1f);
    private final NumberSetting jelloAnimationSpeed = new NumberSetting("setting.animationSpeed", 2500.0f, 1000.0f, 5000.0f, 100.0f);
    private final BooleanSetting jelloGlow = new BooleanSetting("setting.markerGlow", true, () -> modeJello.getValue());
    private final NumberSetting jelloGlowIntensity = new NumberSetting("setting.glowIntensity", 0.5f, 0.1f, 1.5f, 0.1f);
    private final NumberSetting jelloAlpha = new NumberSetting("setting.alpha", 1.0f, 0.1f, 1.0f, 0.1f);

     
    private final NumberSetting svenyaSpeed = new NumberSetting("setting.svenyaSpeed", 1.5f, 0.5f, 3.0f, 0.1f);

    private final ThemeManager themeManager;
    private Color currentColor;
    private final SoulRenderer soulRenderer;
    private final JelloRenderer jelloRenderer;

     
    private float crystalMoving = 0f;

     
    private PigEntity svenyaPig;

    public TargetEsp() {
        super("TargetEsp", Category.Render, I18n.translate("module.targetesp.description"));
        getSettings().add(mode);
        getSettings().add(markerScale);
        getSettings().add(markerGlow);
        getSettings().add(markerGlowIntensity);
        getSettings().add(markerHitFlash);
        getSettings().add(ghostsScale);
        getSettings().add(ghostsParticleDensity);
        getSettings().add(ghostsGlow);
        getSettings().add(ghostsGlowIntensity);
        getSettings().add(ghostsAlpha);
        getSettings().add(jelloHeight);
        getSettings().add(jelloAnimationSpeed);
        getSettings().add(jelloGlow);
        getSettings().add(jelloGlowIntensity);
        getSettings().add(jelloAlpha);
        getSettings().add(svenyaSpeed);  

         
        markerScale.setVisible(() -> modeMarker.getValue());
        markerGlow.setVisible(() -> modeMarker.getValue());
        markerGlowIntensity.setVisible(() -> modeMarker.getValue() && markerGlow.getValue());
        markerHitFlash.setVisible(() -> modeMarker.getValue());
        markerMode.setVisible(() -> modeMarker.getValue());

         
        ghostsScale.setVisible(() -> modeGhosts.getValue());
        ghostsParticleDensity.setVisible(() -> modeGhosts.getValue());
        ghostsGlow.setVisible(() -> modeGhosts.getValue());
        ghostsGlowIntensity.setVisible(() -> modeGhosts.getValue() && ghostsGlow.getValue());
        ghostsAlpha.setVisible(() -> modeGhosts.getValue());

         
        jelloHeight.setVisible(() -> modeJello.getValue());
        jelloAnimationSpeed.setVisible(() -> modeJello.getValue());
        jelloGlow.setVisible(() -> modeJello.getValue());
        jelloGlowIntensity.setVisible(() -> modeJello.getValue() && jelloGlow.getValue());
        jelloAlpha.setVisible(() -> modeJello.getValue());

         
        svenyaSpeed.setVisible(() -> modeSvenya.getValue());  

        this.themeManager = ThemeManager.getInstance();
        this.currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);
        this.soulRenderer = new SoulRenderer(themeManager);
        this.jelloRenderer = new JelloRenderer(themeManager);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor = theme.getBackgroundColor();
    }

    private final Animation animation = new Animation(300, 1f, true, Easing.BOTH_SINE);
    private final Animation soulAnimation = new Animation(500, 1f, true, Easing.BOTH_SINE);
    private final Animation jelloAnimation = new Animation(500, 1f, true, Easing.BOTH_SINE);
    private final Animation crystalsAnimation = new Animation(500, 1f, true, Easing.BOTH_SINE);
    private final Animation svenyaAnimation = new Animation(500, 1f, true, Easing.BOTH_SINE);  

    private LivingEntity lastTarget = null;
    private LivingEntity fadingTarget = null;
    private long lastSeenTime = 0L;
    private static final long ESP_DURATION = 2500;
    private long lastHitTime = 0L;
    private static final long HIT_FLASH_DURATION = 300;

    private Vec3d lastKnownCenter = null;
    private float lastKnownHeight = 1.8f;
    private float lastKnownWidth = 0.6f;

    private boolean forceFade = false;
    private Vec3d fadeOrigin = null;

    private long lastOcclusionAt = 0L;
    private boolean lastOcclusion = false;

    private volatile float cachedFinalSize = -1f;
    private volatile int cachedColorRGBA = 0xFFFFFFFF;
    private volatile float cachedRotation = 0f;

    private static int clamp255(double v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return (int) Math.round(v);
    }

    private Color getTargetColor(LivingEntity target) {
        if (target == null) return Color.WHITE;
        float healthPercent = target.getHealth() / target.getMaxHealth();
        if (healthPercent > 0.7f) return Color.GREEN;
        else if (healthPercent > 0.3f) return Color.YELLOW;
        else return Color.RED;
    }

    private Color getMarkerColor() {
        long now = System.currentTimeMillis();
        if (markerHitFlash.getValue() && now - lastHitTime < HIT_FLASH_DURATION) {
            return Color.RED;
        }
        return currentColor;
    }

    private Vec3d entityCenter(LivingEntity ent, EventRender2D e) {
        Vec3d lp = ent.getLerpedPos(e.getTickDelta());
        return lp.add(0, ent.getHeight() * 0.5, 0);
    }

    private static boolean isInvisibleAndUnrevealed(LivingEntity e) {
        return e.hasStatusEffect(StatusEffects.INVISIBILITY) || e.isInvisible();
    }

    private boolean isOccluded(Vec3d from, Vec3d to) {
        long now = System.currentTimeMillis();
        if (now - lastOcclusionAt < 50L) {
            return lastOcclusion;
        }
        HitResult hr = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        lastOcclusion = hr.getType() != HitResult.Type.MISS;
        lastOcclusionAt = now;
        return lastOcclusion;
    }

    @EventHandler
    public void onAttackEntity(EventAttackEntity e) {
        if (lastTarget != null && e.getTarget() == lastTarget) {
            lastHitTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) hit.getEntity();
                if (living.isAlive()) {
                    Vec3d center = entityCenter(living, e);

                    if (isInvisibleAndUnrevealed(living)) {
                        lastTarget = null;
                        fadeOrigin = center;
                        forceFade = true;
                        fadingTarget = null;
                    } else {
                        Vec3d eyes = mc.player.getCameraPosVec(e.getTickDelta());
                        if (isOccluded(eyes, center)) {
                            lastTarget = null;
                            fadeOrigin = center;
                            forceFade = true;
                            fadingTarget = null;
                        } else {
                            lastTarget = living;
                            lastSeenTime = now;
                            forceFade = false;
                            fadeOrigin = null;
                            fadingTarget = null;

                            lastKnownCenter = center;
                            lastKnownHeight = living.getHeight();
                            lastKnownWidth = living.getWidth();
                        }
                    }
                }
            }
        }

        if (lastTarget != null) {
            boolean killed = !lastTarget.isAlive();
            boolean expired = (now - lastSeenTime) > ESP_DURATION;

            if (killed || expired) {
                Vec3d centerForFade;
                try {
                    centerForFade = entityCenter(lastTarget, e);
                } catch (Throwable t) {
                    centerForFade = lastKnownCenter;
                }
                if (centerForFade != null) fadeOrigin = centerForFade;

                lastKnownHeight = lastTarget.getHeight();
                lastKnownWidth = lastTarget.getWidth();

                fadingTarget = lastTarget;
                lastTarget = null;
                forceFade = true;
            } else {
                if (isInvisibleAndUnrevealed(lastTarget)) {
                    fadeOrigin = entityCenter(lastTarget, e);
                    fadingTarget = lastTarget;
                    lastTarget = null;
                    forceFade = true;
                } else {
                    Vec3d eyes = mc.player.getCameraPosVec(e.getTickDelta());
                    Vec3d center = entityCenter(lastTarget, e);
                    if (isOccluded(eyes, center)) {
                        fadeOrigin = center;
                        fadingTarget = lastTarget;
                        lastTarget = null;
                        forceFade = true;
                    } else {
                        lastKnownCenter = center;
                        lastKnownHeight = lastTarget.getHeight();
                        lastKnownWidth = lastTarget.getWidth();
                    }
                }
            }
        }

        boolean grow = (lastTarget != null) && !forceFade;
        animation.update(grow);
        soulAnimation.update(grow);
        jelloAnimation.update(grow);
        crystalsAnimation.update(grow);
        svenyaAnimation.update(grow);  

         
        if (modeMarker.getValue() && (modeGhosts.getValue() || modeJello.getValue() || modeCrystals.getValue() || modeSvenya.getValue())) {
            modeGhosts.setValue(false);
            modeJello.setValue(false);
            modeCrystals.setValue(false);
            modeSvenya.setValue(false);
        }
        if (modeGhosts.getValue() && (modeMarker.getValue() || modeJello.getValue() || modeCrystals.getValue() || modeSvenya.getValue())) {
            modeMarker.setValue(false);
            modeJello.setValue(false);
            modeCrystals.setValue(false);
            modeSvenya.setValue(false);
        }
        if (modeJello.getValue() && (modeMarker.getValue() || modeGhosts.getValue() || modeCrystals.getValue() || modeSvenya.getValue())) {
            modeMarker.setValue(false);
            modeGhosts.setValue(false);
            modeCrystals.setValue(false);
            modeSvenya.setValue(false);
        }
        if (modeCrystals.getValue() && (modeMarker.getValue() || modeGhosts.getValue() || modeJello.getValue() || modeSvenya.getValue())) {
            modeMarker.setValue(false);
            modeGhosts.setValue(false);
            modeJello.setValue(false);
            modeSvenya.setValue(false);
        }
        if (modeSvenya.getValue() && (modeMarker.getValue() || modeGhosts.getValue() || modeJello.getValue() || modeCrystals.getValue())) {
            modeMarker.setValue(false);
            modeGhosts.setValue(false);
            modeJello.setValue(false);
            modeCrystals.setValue(false);
        }
        if (!modeMarker.getValue() && !modeGhosts.getValue() && !modeJello.getValue() && !modeCrystals.getValue() && !modeSvenya.getValue()) {
            modeMarker.setValue(true);
        }

        if (modeMarker.getValue()) {
            renderMarker(e);
        }

        if (animation.getValue() <= 0 && forceFade) {
            forceFade = false;
            fadeOrigin = null;
        }
        if (soulAnimation.getValue() <= 0 && forceFade && modeGhosts.getValue()) {
            fadeOrigin = null;
            fadingTarget = null;
        }
        if (jelloAnimation.getValue() <= 0 && forceFade && modeJello.getValue()) {
            fadeOrigin = null;
            fadingTarget = null;
        }
        if (crystalsAnimation.getValue() <= 0 && forceFade && modeCrystals.getValue()) {
            fadeOrigin = null;
            fadingTarget = null;
        }
        if (svenyaAnimation.getValue() <= 0 && forceFade && modeSvenya.getValue()) {  
            fadeOrigin = null;
            fadingTarget = null;
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        if (modeGhosts.getValue()) {
            if (lastTarget != null || (fadingTarget != null && soulAnimation.getValue() > 0)) {
                LivingEntity targetToRender = fadingTarget != null ? fadingTarget : lastTarget;
                soulRenderer.render(
                        e, targetToRender, ghostsParticleDensity.getValue().intValue(),
                        ghostsScale.getValue().floatValue(), ghostsGlow.getValue().booleanValue(),
                        ghostsGlowIntensity.getValue().doubleValue(), soulAnimation.getValue(),
                        fadeOrigin, lastKnownHeight, lastKnownWidth, ghostsAlpha.getValue().floatValue(),
                        getMarkerColor()
                );
            }
        } else if (modeJello.getValue()) {
            if (lastTarget != null || (fadingTarget != null && jelloAnimation.getValue() > 0)) {
                LivingEntity targetToRender = fadingTarget != null ? fadingTarget : lastTarget;
                jelloRenderer.render(
                        e, targetToRender, fadeOrigin, lastKnownWidth, lastKnownHeight,
                        jelloHeight.getValue().doubleValue(), jelloAnimationSpeed.getValue().doubleValue(),
                        jelloGlow.getValue().booleanValue(), jelloGlowIntensity.getValue().doubleValue(),
                        jelloAnimation.getValue(), getMarkerColor()
                );
            }
        } else if (modeCrystals.getValue()) {
            if (lastTarget != null || (fadingTarget != null && crystalsAnimation.getValue() > 0)) {
                renderCrystals(e);
            }
        } else if (modeSvenya.getValue()) {  
            if (lastTarget != null || (fadingTarget != null && svenyaAnimation.getValue() > 0)) {
                renderSvenya(e);
            }
        }
    }

     
    private void renderSvenya(EventRender3D.Game e) {
        float progress = svenyaAnimation.getValue();
        if (progress <= 0.0F) return;

        LivingEntity targetToRender = fadingTarget != null ? fadingTarget : lastTarget;
        if (targetToRender == null || mc.player == null || mc.world == null) return;

        MatrixStack matrices = e.getMatrices();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        float tickDelta = e.getTickDelta();

        double x, y, z;
        if (fadingTarget != null && fadeOrigin != null) {
            x = fadeOrigin.x;
            y = fadeOrigin.y - lastKnownHeight * 0.5;
            z = fadeOrigin.z;
        } else {
            Vec3d lp = targetToRender.getLerpedPos(tickDelta);
            x = lp.x;
            y = lp.y;
            z = lp.z;
        }

        if (svenyaPig == null || svenyaPig.getWorld() != mc.world) {
            svenyaPig = new PigEntity(EntityType.PIG, mc.world);
            svenyaPig.setNoGravity(true);
            svenyaPig.setSilent(true);
        }
        PigEntity pig = svenyaPig;
        pig.limbAnimator.setSpeed(0.0f);
        pig.limbAnimator.updateLimbs(0.0f, 1.0f, 1.0f);
        pig.age = 0;
        pig.setYaw(0);
        pig.setPitch(0);
        pig.bodyYaw = 0;
        pig.prevBodyYaw = 0;
        pig.headYaw = 0;
        pig.prevHeadYaw = 0;

        float radius = 0.7f;
        float ringHeight = 1f;
        float speedMul = 0.00025f * svenyaSpeed.getValue().floatValue();

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

         
        float time = -(System.currentTimeMillis() % 1000000) * speedMul;
        double[] px = new double[8];
        double[] py = new double[8];
        double[] pz = new double[8];
        float aoe = time * 360;
        for (int i = 0; i < 8; i++) {
            float angle = aoe + (i / 8.0f) * 360f;
            double rad = Math.toRadians(angle);
            float yOffset = (i % 2 == 0) ? 0.1f : -0.1f;
            px[i] = x + Math.cos(rad) * radius;
            py[i] = y + ringHeight + yOffset - 0.2f;
            pz[i] = z + Math.sin(rad) * radius;
        }

         
        double topX = x;
        double topY = y + 2.2f;
        double topZ = z;
        float time2 = (System.currentTimeMillis() % 1000000) * svenyaSpeed.getValue().floatValue() * 0.00100f;
        float topYaw = time2 * 180;
        float topPitch = (float) (Math.sin(time2 * 1.5) * 120);
        float topRoll = (float) (Math.cos(time2 * 1.2) * 90);

        for (int i = 0; i < 9; i++) {
            double wx, wy, wz;
            double nx, nz;
            if (i < 8) {
                wx = px[i];
                wy = py[i];
                wz = pz[i];
                int next = (i + 1) % 8;
                nx = px[next];
                nz = pz[next];
            } else {
                wx = topX;
                wy = topY;
                wz = topZ;
                nx = px[0];
                nz = pz[0];
            }

            matrices.push();
            matrices.translate(wx - camPos.x, wy - camPos.y, wz - camPos.z);

            if (i == 8) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(topYaw));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(topPitch));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(topRoll));
            } else {
                double lX = nx - wx;
                double lZ = nz - wz;
                float yaw = (float) Math.toDegrees(Math.atan2(-lZ, lX)) - 95;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
            }

            float scale = (i == 8) ? 0.4f * progress : 0.3f * progress;
            matrices.scale(scale, scale, scale);

            try {
                mc.getEntityRenderDispatcher().render(
                        pig, 0, 0, 0, tickDelta,
                        matrices, immediate, 0xF000F0
                );
            } catch (Exception ignored) {
            }

            matrices.pop();
        }

        immediate.draw();
    }

     
    private void renderCrystals(EventRender3D.Game e) {
        float alpha = crystalsAnimation.getValue();
        if (alpha <= 0.0F) return;

        LivingEntity targetToRender = fadingTarget != null ? fadingTarget : lastTarget;
        if (targetToRender == null || mc.player == null) return;

        net.minecraft.client.util.math.MatrixStack matrices = e.getMatrices();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        float tickDelta = e.getTickDelta();

        double tx, ty, tz;
        if (fadingTarget != null && fadeOrigin != null) {
            tx = fadeOrigin.x;
            ty = fadeOrigin.y - lastKnownHeight * 0.5;
            tz = fadeOrigin.z;
        } else {
            Vec3d lp = targetToRender.getLerpedPos(tickDelta);
            tx = lp.x;
            ty = lp.y;
            tz = lp.z;
        }

        crystalMoving += 1.0f;

        float entityHeight = targetToRender.getHeight();
        float entityWidth = targetToRender.getWidth();
        float width = entityWidth * 1.5f;

        Color baseColor = getMarkerColor();
        int cr = baseColor.getRed();
        int cg = baseColor.getGreen();
        int cb = baseColor.getBlue();

        matrices.push();
        matrices.translate(tx - camPos.x, ty - camPos.y, tz - camPos.z);

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.blendFunc(770, 1);
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);

        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        net.minecraft.client.render.BufferBuilder crystalBuffer = net.minecraft.client.render.Tessellator.getInstance().begin(
                net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLES, net.minecraft.client.render.VertexFormats.POSITION_COLOR);

        int crystalAlpha = Math.min(255, (int) (alpha * 255));
        int color = (crystalAlpha << 24) | (cr << 16) | (cg << 8) | cb;

        float cw = 0.085f;
        float ch = 0.22f;

        for (int i = 0; i < 360; i += 19) {
            float val = 1.2f - 0.5f * alpha;
            float angleDeg = i + crystalMoving * 0.3f;
            float angleRad = (float) Math.toRadians(angleDeg);
            float sin = (float) (Math.sin(angleRad) * width * val);
            float cos = (float) (Math.cos(angleRad) * width * val);

            float heightPrc = ((i / 20.0f) * 0.6180339f) % 1.0f;
            float crystalY = entityHeight * heightPrc;

            matrices.push();
            matrices.translate(sin, crystalY, cos);

            org.joml.Vector3f dir = new org.joml.Vector3f(-sin, 0, -cos).normalize();
            org.joml.Quaternionf rotation = new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0, 1, 0), dir);
            matrices.multiply(rotation);

            org.joml.Matrix4f matrix = matrices.peek().getPositionMatrix();

            float[] ex = {cw, 0, -cw, 0};
            float[] ez = {0, cw, 0, -cw};

            for (int j = 0; j < 4; j++) {
                int next = (j + 1) % 4;
                crystalBuffer.vertex(matrix, 0, ch, 0).color(color);
                crystalBuffer.vertex(matrix, ex[j], 0, ez[j]).color(color);
                crystalBuffer.vertex(matrix, ex[next], 0, ez[next]).color(color);
            }

            for (int j = 0; j < 4; j++) {
                int next = (j + 1) % 4;
                crystalBuffer.vertex(matrix, 0, -ch, 0).color(color);
                crystalBuffer.vertex(matrix, ex[next], 0, ez[next]).color(color);
                crystalBuffer.vertex(matrix, ex[j], 0, ez[j]).color(color);
            }

            matrices.pop();
        }

        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(crystalBuffer.end());
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, darkvisuals.id("hud/glow.png"));

        net.minecraft.client.render.BufferBuilder glowBuffer = net.minecraft.client.render.Tessellator.getInstance().begin(
                net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR);

        net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();

        for (int i = 0; i < 360; i += 19) {
            float val = 1.2f - 0.5f * alpha;
            float angleDeg = i + crystalMoving * 0.3f;
            float angleRad = (float) Math.toRadians(angleDeg);
            float sin = (float) (Math.sin(angleRad) * width * val);
            float cos = (float) (Math.cos(angleRad) * width * val);

            float heightPrc = ((i / 20.0f) * 0.6180339f) % 1.0f;
            float crystalY = entityHeight * heightPrc;

            matrices.push();
            matrices.translate(sin, crystalY, cos);
            matrices.multiply(camera.getRotation());

            org.joml.Matrix4f matrix = matrices.peek().getPositionMatrix();

            float glowSize = 0.3f * alpha;
            int glowAlpha = (int) Math.min(255, alpha * 200);
            int glowColor = (glowAlpha << 24) | (cr << 16) | (cg << 8) | cb;

            glowBuffer.vertex(matrix, -glowSize, glowSize, 0).texture(0f, 1f).color(glowColor);
            glowBuffer.vertex(matrix, glowSize, glowSize, 0).texture(1f, 1f).color(glowColor);
            glowBuffer.vertex(matrix, glowSize, -glowSize, 0).texture(1f, 0f).color(glowColor);
            glowBuffer.vertex(matrix, -glowSize, -glowSize, 0).texture(0f, 0f).color(glowColor);

            matrices.pop();
        }

        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(glowBuffer.end());

        matrices.pop();

        com.mojang.blaze3d.systems.RenderSystem.enableCull();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private void renderMarker(EventRender2D e) {
        Vec3d centerWorld = lastTarget != null ? entityCenter(lastTarget, e) : fadeOrigin;
        if (centerWorld == null) return;

        float animVal = animation.getValue();
        if (animVal <= 0 && !markerGlow.getValue()) return;

        Vec3d pos = WorldUtils.getPosition(centerWorld);
        if (!(pos.z > 0) || !(pos.z < 1)) return;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(pos.getX(), pos.getY(), 0);

        if (animVal > 0) {
            float maxSize = (float) WorldUtils.getScale(centerWorld, BASE_SIZE * markerScale.getValue() * animVal);

            double px = mc.player.getX();
            double pz = mc.player.getZ();
            double tx = (lastTarget != null) ? lastTarget.getX() : (lastKnownCenter != null ? lastKnownCenter.x : px);
            double tz = (lastTarget != null) ? lastTarget.getZ() : (lastKnownCenter != null ? lastKnownCenter.z : pz);
            float markerScaleVal = markerScale.getValue().floatValue();
            int baseR, baseG, baseB;
            {
                Color baseCol = getMarkerColor();
                baseR = baseCol.getRed();
                baseG = baseCol.getGreen();
                baseB = baseCol.getBlue();
            }
            long lastHitSnapshot = lastHitTime;
            boolean hitFlashEnabled = markerHitFlash.getValue();

            Async.run(() -> {
                double dx = tx - px;
                double dz = tz - pz;
                double distance = Math.sqrt(dx * dx + dz * dz);
                float finalSize = Math.max(maxSize - (float) distance, markerScaleVal);
                double sin = Math.sin(System.currentTimeMillis() / 1000.0);
                float rotationAngle = (float) (sin * 360);

                long now = System.currentTimeMillis();
                float hitPhase = (hitFlashEnabled && now - lastHitSnapshot < HIT_FLASH_DURATION) ? 1f : 0f;
                int fr = clamp255(baseR + (255 - baseR) * hitPhase);
                int fg = clamp255(baseG + (0 - baseG) * hitPhase);
                int fb = clamp255(baseB + (0 - baseB) * hitPhase);
                int fa = clamp255(255 * animVal);
                int rgba = (fa & 0xFF) << 24 | (fr & 0xFF) << 16 | (fg & 0xFF) << 8 | (fb & 0xFF);

                cachedFinalSize = finalSize;
                cachedRotation = rotationAngle;
                cachedColorRGBA = rgba;
            });
        }

        if (markerGlow.getValue()) {
            renderMarkerGlow(e, centerWorld);
        }

        renderMarkerMain(e, centerWorld, animVal, cachedColorRGBA);

        e.getContext().getMatrices().pop();
    }

    private void renderMarkerGlow(EventRender2D e, Vec3d centerWorld) {
        float animVal = animation.getValue();
        if (animVal <= 0) return;
        Color base = (lastTarget != null) ? getTargetColor(lastTarget) : currentColor;
        float glowSize = (float) WorldUtils.getScale(centerWorld, BASE_SIZE * markerScale.getValue() * (1.0f + markerGlowIntensity.getValue() * 0.5f) * animVal);
        int glowAlpha = clamp255(180 * animVal);

        Render2D.drawTexture(
                e.getContext().getMatrices(),
                -glowSize / 2f, -glowSize / 2f, glowSize, glowSize,
                0f,
                darkvisuals.id("hud/glow.png"),
                currentColor
        );
    }

    private void renderMarkerMain(EventRender2D e, Vec3d centerWorld, float animVal, int colorRGBA) {
        if (animVal <= 0) return;

        float finalSize = this.cachedFinalSize > 0 ? this.cachedFinalSize : (float) WorldUtils.getScale(centerWorld, BASE_SIZE * markerScale.getValue() * animVal);
        float rotationAngle = this.cachedRotation;

        Color finalColor = new Color(colorRGBA, true);

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationAngle));

        Render2D.drawTexture(
                e.getContext().getMatrices(),
                -finalSize / 2f, -finalSize / 2f, finalSize, finalSize,
                0f,
                darkvisuals.id(modeMarkerNew.getValue() ? "hud/alt_marker.png" : "hud/marker.png"),
                finalColor
        );

        e.getContext().getMatrices().pop();
    }
}