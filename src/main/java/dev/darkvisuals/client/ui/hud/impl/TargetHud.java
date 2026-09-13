package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.ui.hud.HudStyle;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.util.Network.Server;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.animations.infinity.InfinityAnimation;
import dev.darkvisuals.client.util.math.MathUtils;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.renderer.fonts.Instance;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.modules.impl.utility.NameProtect;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.Identifier;
import dev.darkvisuals.darkvisuals;
import java.util.List;
import java.util.ArrayList;

import java.awt.*;

public class TargetHud extends HudElement implements ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color headerTextColor;
    private Color lowDurabilityColor;
    private Color absorbColor;

     
    private final BooleanSetting displayAbsorption = new BooleanSetting("displayAbsorption", true);
    private final BooleanSetting displayHudParticles = new BooleanSetting("hudParticles", true);
    private final ListSetting style;

    public TargetHud() {
        super("TargetHud");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);

         
        BooleanSetting optDefault = new BooleanSetting("targethud.style.default", true, () -> false);
        BooleanSetting optCard = new BooleanSetting("targethud.style.card", false, () -> false);
        this.style = new ListSetting("targethud.style", () -> true, true, optDefault, optCard).setSingleSelect(true);

        getSettings().add(displayAbsorption);
        getSettings().add(displayHudParticles);
        getSettings().add(style);
    }

    private void applyTheme(ThemeManager.Theme theme) {
         
        this.bgColor = new Color(30, 30, 30, 255);

        int brightness = (int) (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue());
        if (brightness > 200 && bgColor.getAlpha() <= 150) {
            this.textColor = new Color(0, 0, 0, 255);
        } else {
            this.textColor = theme.getTextColor();
        }

        this.headerTextColor = this.textColor;
        this.lowDurabilityColor = new Color(200, 80, 80, 220);
        this.absorbColor = new Color(255, 190, 0, 255);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

     
    private final InfinityAnimation fadeAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation scaleAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation slideAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation hpAnimPx = new InfinityAnimation(Easing.BOTH_SINE);
    private final InfinityAnimation absAnimPx = new InfinityAnimation(Easing.BOTH_SINE);

    private LivingEntity lastTarget = null;
    private long lastSeenTime = 0L;
    private static final long HUD_DURATION = 2000;
    private boolean forceFade = false;
    private Vec3d lastKnownCenter = null;

    private float animationDirectionX = 1f;
    private float animationDirectionY = 1f;
    private int lastHurtTicks = 0;
    private int prevHurtTime = 0;

     
    private static final float ROUNDING = 6f;
    private static final float SPACING = 4f;

     
    private static final float WIDTH = 112f;
    private static final float HEIGHT = 56f;

     
    private static final float ICON_SIZE = 12f;
    private static final float ICON_TILE = 14f;
    private static final float ICON_GAP = 2f;

     
    private static final float HEAD = 19f;

     
    private static final Identifier STAR_TEX = darkvisuals.id("hud/star.png");
    private final List<HudParticle> hudParticles = new ArrayList<>();
    private long lastFrameTimeMs = System.currentTimeMillis();
    private float previousHp01 = -1f;

    private Vec3d entityCenter(LivingEntity ent, EventRender2D e) {
        Vec3d lp = ent.getLerpedPos(e.getTickDelta());
        return lp.add(0, ent.getHeight() * 0.5, 0);
    }

    private static boolean isInvisibleAndUnrevealed(LivingEntity e) {
        return e.hasStatusEffect(StatusEffects.INVISIBILITY) || e.isInvisible();
    }

    private boolean isOccluded(Vec3d from, Vec3d to) {
        HitResult hr = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hr.getType() != HitResult.Type.MISS;
    }

    private Color getHitColor(LivingEntity entity, int alpha) {
        return new Color(255, 255, 255, alpha);  
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTimeMs) / 1000f;
        lastFrameTimeMs = now;

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity living && living.isAlive()) {
                Vec3d center = entityCenter(living, e);

                if (isInvisibleAndUnrevealed(living)) {
                } else if (isOccluded(mc.player.getCameraPosVec(e.getTickDelta()), center)) {
                    lastTarget = null;
                    forceFade = true;
                    lastKnownCenter = center;
                } else {
                    if (lastTarget == null) {
                        animationDirectionX = (float) (Math.random() * 2 - 1);
                        animationDirectionY = (float) (Math.random() * 2 - 1);
                    }
                    if (lastTarget != living) {
                        prevHurtTime = 0;
                    }
                    lastTarget = living;
                    lastSeenTime = now;
                    forceFade = false;
                    lastKnownCenter = center;
                }
            }
        }

        if (lastTarget != null && (!lastTarget.isAlive() || now - lastSeenTime > HUD_DURATION)) {
            lastTarget = null;
            forceFade = true;
        }

        boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        boolean previewMode = chatOpen && lastTarget == null;
        LivingEntity previewEntity = previewMode ? mc.player : null;

        boolean shouldShow = (lastTarget != null && !forceFade) || previewMode;
        fadeAnimation.animate(shouldShow ? 1f : 0f, 200);
        scaleAnimation.animate(shouldShow ? 1f : 0.9f, 200);
        slideAnimation.animate(shouldShow ? 0f : 1f, 220);

        if (fadeAnimation.getValue() <= 0 && forceFade && !previewMode) {
            forceFade = false;
            lastKnownCenter = null;
            previousHp01 = -1f;
            hudParticles.clear();
            prevHurtTime = 0;
            return;
        }

        float posX = getX();
        float posY = getY();
        setBounds(posX, posY, WIDTH, HEIGHT);

        int alpha = (int) (230 * fadeAnimation.getValue());
        if (alpha <= 0) return;

        LivingEntity target = lastTarget;
        if (!previewMode && target == null && lastKnownCenter == null) return;

        float rawHp = target != null ? MathUtils.round(Server.getHealth(target, false)) : (previewEntity != null ? MathUtils.round(Server.getHealth(previewEntity, false)) : 0f);
        float maxHp = target != null ? Math.max(1f, MathUtils.round(target.getMaxHealth())) : (previewEntity != null ? Math.max(1f, MathUtils.round(previewEntity.getMaxHealth())) : 20f);
        float absorb = target != null ? Math.max(0f, MathUtils.round(target.getAbsorptionAmount())) : (previewEntity != null ? Math.max(0f, MathUtils.round(previewEntity.getAbsorptionAmount())) : 0f);

        float hp01 = MathHelper.clamp(rawHp / maxHp, 0f, 1f);
        float abs01 = absorb > 0f ? MathHelper.clamp(absorb / maxHp, 0f, 1f) : 0f;

        float scale = scaleAnimation.getValue() * toggledAnimation.getValue() * fadeAnimation.getValue();
        float slideX = 16f * slideAnimation.getValue() * animationDirectionX;
        float slideY = 16f * slideAnimation.getValue() * animationDirectionY;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(posX + WIDTH / 2f + slideX, posY + HEIGHT / 2f + slideY, 0f);
        e.getContext().getMatrices().scale(scale, scale, 1f);
        e.getContext().getMatrices().translate(-(posX + WIDTH / 2f), -(posY + HEIGHT / 2f), 0f);

        if (HudStyle.isMinimalistic()) {
            renderMinimalistic(e, posX, posY, target, previewEntity, previewMode, rawHp, alpha, fadeAnimation.getValue());
            e.getContext().getMatrices().pop();
            super.onRender2D(e);
            return;
        }

         
        if (style.getName("targethud.style.card").getValue()) {
             
            float cardW = WIDTH - 2f;
            float cardH = HEIGHT + 2f;
            setBounds(posX, posY, cardW, cardH);

             
            Render2D.drawHudBackground(
                    e.getContext().getMatrices(),
                    posX, posY, cardW, cardH, ROUNDING,
                    fadeAnimation.getValue()
            );

             
            float avatarSize = HEAD + 12f;
            float avatarX = posX + SPACING;
            float avatarY = posY + (cardH / 2f - avatarSize / 2f) - 0.5f;
            Render2D.drawRoundedRect(e.getContext().getMatrices(), avatarX, avatarY, avatarSize, avatarSize, 6f,
                    new Color(80, 80, 80, Math.min(200, alpha)));

             
            if (previewMode && previewEntity instanceof PlayerEntity) {
                Color headColor = getHitColor(previewEntity, alpha);
                Render2D.drawTexture(
                        e.getContext().getMatrices(),
                        avatarX, avatarY,
                        avatarSize, avatarSize,
                        3f,
                        0.125f, 0.125f, 0.125f, 0.125f,
                        ((AbstractClientPlayerEntity) previewEntity).getSkinTextures().texture(),
                        headColor
                );
            } else if (target instanceof PlayerEntity) {
                Color headColor = getHitColor(target, alpha);
                Render2D.drawTexture(
                        e.getContext().getMatrices(),
                        avatarX, avatarY,
                        avatarSize, avatarSize,
                        3f,
                        0.125f, 0.125f, 0.125f, 0.125f,
                        ((AbstractClientPlayerEntity) target).getSkinTextures().texture(),
                        headColor
                );
            } else {
                 
                Render2D.drawFont(
                        e.getContext().getMatrices(),
                        Fonts.BOLD.getFont(8.5f),
                        "?",
                        avatarX + (avatarSize / 2f) - Fonts.BOLD.getWidth("?", 8.5f) / 2f,
                        avatarY + (avatarSize / 2f) - Fonts.BOLD.getHeight(8.5f) / 2f,
                        new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha)
                );
            }

             
            String dispName = (previewMode && previewEntity != null) ? previewEntity.getName().getString()
                    : ((target != null && !target.getName().getString().isEmpty()) ? target.getName().getString() : "Unknown");
             
            if (previewMode) {
                NameProtect np = NameProtect.getInstance();
                if (np != null && np.isToggled()) {
                    String replacement = np.getCustomName().getValue();
                    dispName = replacement != null && !replacement.isEmpty() ? replacement : "Protected";
                }
            }
            float nameX = avatarX + avatarSize + SPACING * 2 - 4f;
            float maxNameWCard = (posX + cardW - SPACING) - nameX;
            String drawName = ellipsize(dispName, 9f, maxNameWCard);
            Render2D.drawFont(
                    e.getContext().getMatrices(),
                    Fonts.BOLD.getFont(9f),
                    drawName,
                    nameX,
                    posY + SPACING + 1f,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha)
            );

             
            float barW2 = cardW - SPACING - 42f;
            float barH2 = 12f;
            float barX2 = posX + SPACING + 38f;
            float barY2 = posY + cardH - SPACING - barH2 - 1.5f;

             
            Render2D.drawHudBarTrack(e.getContext().getMatrices(), barX2, barY2, barW2, barH2, 2f,
                    fadeAnimation.getValue());
             
            float hpTargetPx2 = barW2 * hp01;
            float fillW = Math.max(0f, Math.min(barW2, hpAnimPx.animate(hpTargetPx2, 100)));
            Render2D.drawRoundedRect(e.getContext().getMatrices(), barX2, barY2, fillW, barH2, 2f,
                    themeManager.getCurrentTheme().getAccentColor());

             
            int percent = (int) Math.round(hp01 * 100.0);
            String pct = percent + "%";
            float pctX = barX2 + (barW2 - Fonts.BOLD.getWidth(pct, 8.5f)) / 2f;
            float pctY = barY2 + (barH2 - Fonts.BOLD.getHeight(8.5f)) / 2f + 0.2f;
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(7.5f), pct, pctX, pctY,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha));

            e.getContext().getMatrices().pop();
            super.onRender2D(e);
            return;
        }

         
         
         
        Render2D.drawHudBackground(
                e.getContext().getMatrices(),
                posX, posY, WIDTH, HEIGHT, 9f,
                fadeAnimation.getValue()
        );

        float headX = posX + SPACING + 2f;
        float headY = posY + SPACING + 2f;
        float barX = headX + HEAD + SPACING * 2;
         

         
        Render2D.drawRoundedRect(e.getContext().getMatrices(), headX, headY, HEAD, HEAD, 6f,
                new Color(80, 80, 80, Math.min(200, alpha)));

         
        if (previewMode && previewEntity instanceof PlayerEntity) {
            Color headColor = getHitColor(previewEntity, alpha);
            Render2D.drawTexture(
                    e.getContext().getMatrices(),
                    headX, headY,
                    HEAD, HEAD,
                    3f,
                    0.125f, 0.125f, 0.125f, 0.125f,
                    ((AbstractClientPlayerEntity) previewEntity).getSkinTextures().texture(),
                    headColor
            );
        } else if (target instanceof PlayerEntity) {
            Color headColor = getHitColor(target, alpha);
            Render2D.drawTexture(
                    e.getContext().getMatrices(),
                    headX, headY,
                    HEAD, HEAD,
                    3f,
                    0.125f, 0.125f, 0.125f, 0.125f,
                    ((AbstractClientPlayerEntity) target).getSkinTextures().texture(),
                    headColor
            );
        } else {
             
            Render2D.drawFont(
                    e.getContext().getMatrices(),
                    Fonts.BOLD.getFont(8.5f),
                    "?",
                    headX + (HEAD / 2f) - Fonts.BOLD.getWidth("?", 8.5f) / 2f,
                    headY + (HEAD / 2f) - Fonts.BOLD.getHeight(8.5f) / 2f,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha)
            );
        }

         
        float textX = headX + HEAD + SPACING * 2;
        float textY = posY + SPACING + 1f;
        String name = previewMode && previewEntity != null ? previewEntity.getName().getString() : ((target != null && !target.getName().getString().isEmpty())
                                                                                                    ? target.getName().getString() : "Unknown");
         
        if (previewMode) {
            NameProtect np = NameProtect.getInstance();
            if (np != null && np.isToggled()) {
                String replacement = np.getCustomName().getValue();
                name = replacement != null && !replacement.isEmpty() ? replacement : "Protected";
            }
        }
        float maxNameW = (posX + WIDTH - SPACING) - textX;
        String nameDraw = ellipsize(name, 9f, maxNameW);
        drawGlassText(e.getContext().getMatrices(),
                Fonts.BOLD,
                9f,
                nameDraw,
                textX,
                textY,
                new Color(headerTextColor.getRed(), headerTextColor.getGreen(), headerTextColor.getBlue(), alpha));

         
        int hpDisplay = Math.round(rawHp);
        String hpText = "HP: " + hpDisplay;
        float hpTextY = textY + Fonts.BOLD.getHeight(9f) + 1f;
        drawGlassText(e.getContext().getMatrices(),
                Fonts.BOLD,
                8f,
                hpText,
                textX,
                hpTextY,
                new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), Math.min(alpha, 200)));

         
        float barY = hpTextY + Fonts.BOLD.getHeight(8f) + 2f;
        float barW = (posX + WIDTH - SPACING) - barX;
        float barH = 5f;

        Render2D.drawHudBarTrack(e.getContext().getMatrices(), barX, barY, barW, barH, 2.5f,
                fadeAnimation.getValue());

        float hpTargetPx = barW * hp01;
        float absTargetPx = barW * abs01;
        float hpPx = Math.min(barW, hpAnimPx.animate(hpTargetPx, 100));
        float absPx = absorb > 0f ? Math.min(barW, absAnimPx.animate(absTargetPx, 120)) : 0f;

        Color liveAccent = themeManager.getCurrentTheme().getAccentColor();
        Render2D.drawRoundedRect(e.getContext().getMatrices(), barX, barY, hpPx, barH,
                Math.min(2f, hpPx / 2f), liveAccent);

        if (displayAbsorption.getValue() && absorb > 0f && absPx > 0f) {
            Render2D.drawRoundedRect(e.getContext().getMatrices(), barX, barY, absPx, barH,
                    Math.min(2f, absPx / 2f), absorbColor);
        }

         
        if (!previewMode && target != null && displayHudParticles.getValue()) {
             
            if (target.hurtTime > prevHurtTime) {
                 
                int spawn = 24;

                 
                float centerX = headX + HEAD / 2f;
                float centerY = headY + HEAD / 2f;

                 
                int perSideBase = Math.max(1, spawn / 4);
                int remainder = spawn - perSideBase * 4;
                for (int edge = 0; edge < 4; edge++) {
                    int count = perSideBase + (edge < remainder ? 1 : 0);
                    for (int i = 0; i < count; i++) {
                        float spawnX;
                        float spawnY;
                        float t = (float) Math.random();
                        if (edge == 0) {  
                            spawnX = headX + t * HEAD;
                            spawnY = headY;
                        } else if (edge == 1) {  
                            spawnX = headX + HEAD;
                            spawnY = headY + t * HEAD;
                        } else if (edge == 2) {  
                            spawnX = headX + t * HEAD;
                            spawnY = headY + HEAD;
                        } else {  
                            spawnX = headX;
                            spawnY = headY + t * HEAD;
                        }

                         
                        float baseAngle = (float) Math.atan2(spawnY - centerY, spawnX - centerX);
                         
                        float jitter = (float) ((Math.random() - 0.5) * Math.toRadians(70));
                        float angle = baseAngle + jitter;

                         
                        float speed = 7f + (float) (Math.random() * 9f);  
                        float vx = (float) (Math.cos(angle) * speed);
                        float vy = (float) (Math.sin(angle) * speed);

                        float size = 5.5f + (float) (Math.random() * 3.5f);
                        long lifeMs = 1200 + (int) (Math.random() * 1200);  
                        hudParticles.add(new HudParticle(spawnX, spawnY, vx, vy, size, lifeMs));
                    }
                }
                prevHurtTime = target.hurtTime;
            }
            prevHurtTime = target.hurtTime;
            previousHp01 = hp01;
        } else {
            previousHp01 = -1f;
            prevHurtTime = 0;
        }

         
        LivingEntity iconSource = previewMode ? previewEntity : target;
        if (iconSource != null) {
            ItemStack[] slotItems = new ItemStack[]{
                    iconSource.getEquippedStack(EquipmentSlot.FEET),
                    iconSource.getEquippedStack(EquipmentSlot.LEGS),
                    iconSource.getEquippedStack(EquipmentSlot.CHEST),
                    iconSource.getEquippedStack(EquipmentSlot.HEAD),
                    iconSource.getMainHandStack(),
                    iconSource.getOffHandStack()
            };

            float totalIconsW = ICON_TILE * slotItems.length + ICON_GAP * (slotItems.length - 1);
            float iconsX = posX + (WIDTH - totalIconsW) / 2f;
            float iconsY = posY + HEIGHT - SPACING - ICON_TILE;

            for (int i = 0; i < slotItems.length; i++) {
                float tileX = iconsX + i * (ICON_TILE + ICON_GAP);

                ItemStack stack = slotItems[i];
                if (stack != null && !stack.isEmpty()) {
                    float itemX = tileX + (ICON_TILE - ICON_SIZE) / 2f;
                    float itemY = iconsY + (ICON_TILE - ICON_SIZE) / 2f;
                    e.getContext().drawItem(stack, Math.round(itemX), Math.round(itemY));
                }
            }
        }

         
        if (!displayHudParticles.getValue()) {
            hudParticles.clear();
        }
        if (displayHudParticles.getValue() && !hudParticles.isEmpty()) {
            Color base = themeManager.getCurrentTheme().getAccentColor();
            hudParticles.removeIf(p -> p.updateAndIsDead(dt));
            for (HudParticle p : hudParticles) {
                int a = (int) (Math.min(1f, Math.max(0f, p.alpha)) * 255);
                if (a <= 0) continue;
                float size = p.size;
                Render2D.drawTexture(
                        e.getContext().getMatrices(),
                        p.x - size / 2f,
                        p.y - size / 2f,
                        size,
                        size,
                        0f,
                        STAR_TEX,
                        new Color(base.getRed(), base.getGreen(), base.getBlue(), a)
                );
            }
        }

        e.getContext().getMatrices().pop();
        super.onRender2D(e);
    }

     
    private String ellipsize(String text, float fontSize, float maxWidth) {
        if (text == null) return "";
        if (Fonts.BOLD.getWidth(text, fontSize) <= maxWidth) return text;
        String ellipsis = "...";
        float ellipsisW = Fonts.BOLD.getWidth(ellipsis, fontSize);
        if (ellipsisW > maxWidth) return "";  
        int lo = 0, hi = text.length();
        String best = "";
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            String candidate = text.substring(0, mid) + ellipsis;
            float w = Fonts.BOLD.getWidth(candidate, fontSize);
            if (w <= maxWidth) {
                best = candidate;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best;
    }

    private static class HudParticle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        long lifeMs;
        long ageMs;
        float alpha = 1f;

        HudParticle(float x, float y, float vx, float vy, float size, long lifeMs) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.lifeMs = lifeMs;
            this.ageMs = 0L;
        }

        boolean updateAndIsDead(float dtSeconds) {
            long dtMs = (long) (dtSeconds * 1000f);
            this.ageMs += dtMs;
            float t = Math.max(0f, Math.min(1f, ageMs / (float) lifeMs));
             
            float dampingPerSecond = 0.88f;  
            float factor = (float) Math.pow(dampingPerSecond, dtSeconds);
            this.vx *= factor;
            this.vy *= factor;
            this.x += vx * dtSeconds;
            this.y += vy * dtSeconds;
             
            this.alpha = 1f - t;
            return ageMs >= lifeMs || alpha <= 0f;
        }
    }
    private void renderMinimalistic(EventRender2D e, float posX, float posY,
                                    LivingEntity target, LivingEntity previewEntity, boolean previewMode,
                                    float rawHp, int alpha, float fade) {
        float cardW = 118f;
        float cardH = 36f;
        float pad = 5f;
        float avatar = 26f;
        float badge = 18f;
        float itemSize = 12f;

        setBounds(posX, posY, cardW, cardH + 18f);

        Render2D.drawHudBackground(e.getContext().getMatrices(), posX, posY, cardW, cardH, 8f, fade);

        float avatarX = posX + pad;
        float avatarY = posY + (cardH - avatar) / 2f;
        Render2D.drawRoundedRect(e.getContext().getMatrices(), avatarX, avatarY, avatar, avatar, 5f,
                new Color(60, 60, 60, Math.min(200, alpha)));

        LivingEntity headSrc = previewMode ? previewEntity : target;
        if (headSrc instanceof PlayerEntity) {
            Render2D.drawTexture(
                    e.getContext().getMatrices(),
                    avatarX, avatarY, avatar, avatar, 4f,
                    0.125f, 0.125f, 0.125f, 0.125f,
                    ((AbstractClientPlayerEntity) headSrc).getSkinTextures().texture(),
                    new Color(255, 255, 255, alpha)
            );
        }

        String dispName = (previewMode && previewEntity != null) ? previewEntity.getName().getString()
                : ((target != null && !target.getName().getString().isEmpty()) ? target.getName().getString() : "Unknown");
        if (previewMode) {
            NameProtect np = NameProtect.getInstance();
            if (np != null && np.isToggled()) {
                String replacement = np.getCustomName().getValue();
                dispName = replacement != null && !replacement.isEmpty() ? replacement : "Protected";
            }
        }

        float nameX = avatarX + avatar + pad;
        float nameY = posY + pad + 1f;
        float maxNameW = cardW - pad - badge - 6f - (nameX - posX);
        String drawName = ellipsize(dispName, 8f, maxNameW);
        Render2D.drawHudText(e.getContext().getMatrices(), Fonts.MEDIUM.getFont(8f), drawName, nameX, nameY,
                new Color(255, 255, 255, alpha));

        LivingEntity itemSrc = previewMode ? previewEntity : target;
        float heldY = nameY + Fonts.MEDIUM.getHeight(8f) + 3f;
        float heldX = nameX;
        if (itemSrc != null) {
            ItemStack main = itemSrc.getMainHandStack();
            ItemStack off = itemSrc.getOffHandStack();
            if (main != null && !main.isEmpty()) {
                e.getContext().drawItem(main, Math.round(heldX), Math.round(heldY));
                heldX += itemSize + 2f;
            }
            if (off != null && !off.isEmpty()) {
                e.getContext().drawItem(off, Math.round(heldX), Math.round(heldY));
            }
        }

        Color purple = themeManager.getCurrentTheme().getAccentColor();
        if (HudStyle.isMinimalistic()) {
            purple = new Color(0xA0, 0x00, 0xFF, alpha);
        } else {
            purple = new Color(purple.getRed(), purple.getGreen(), purple.getBlue(), alpha);
        }
        float badgeX = posX + cardW - pad - badge;
        float badgeY = posY + (cardH - badge) / 2f;
        Render2D.drawRoundedRect(e.getContext().getMatrices(), badgeX, badgeY, badge, badge, badge / 2f, purple);
        String hpStr = String.valueOf(Math.round(rawHp));
        float hpW = Fonts.BOLD.getWidth(hpStr, 7.5f);
        float hpH = Fonts.BOLD.getHeight(7.5f);
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(7.5f), hpStr,
                badgeX + (badge - hpW) / 2f, badgeY + (badge - hpH) / 2f,
                new Color(255, 255, 255, alpha));

        if (itemSrc != null) {
            ItemStack[] armor = new ItemStack[]{
                    itemSrc.getEquippedStack(EquipmentSlot.HEAD),
                    itemSrc.getEquippedStack(EquipmentSlot.CHEST),
                    itemSrc.getEquippedStack(EquipmentSlot.LEGS),
                    itemSrc.getEquippedStack(EquipmentSlot.FEET)
            };
            float armorY = posY + cardH + 3f;
            float armorTotalW = itemSize * 4f + 3f * 3f;
            float armorX = posX + (cardW - armorTotalW) / 2f;
            for (ItemStack stack : armor) {
                if (stack != null && !stack.isEmpty()) {
                    e.getContext().drawItem(stack, Math.round(armorX), Math.round(armorY));
                }
                armorX += itemSize + 3f;
            }
        }
    }

      
    private void drawLiquidGlass(net.minecraft.client.util.math.MatrixStack matrices,
                                 float x, float y, float w, float h, float r, float fade) {
        Render2D.drawLiquidGlass(matrices, x, y, w, h, r, fade);
    }

      
    private void drawGlassText(net.minecraft.client.util.math.MatrixStack matrices,
                               dev.darkvisuals.client.util.renderer.fonts.Font fontFamily, float fontSize,
                               String text, float x, float y, Color color) {
        Render2D.drawHudText(matrices, (Instance) fontFamily.getFont(fontSize), text, x, y, color);
    }
}