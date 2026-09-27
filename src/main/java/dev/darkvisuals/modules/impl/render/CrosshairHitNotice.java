package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.awt.Color;

public class CrosshairHitNotice extends Module {

    private static final long FADE_MS = 1200L;

    private final NumberSetting radius    = new NumberSetting("Радиус",     40f,  20f, 100f, 2f);
    private final NumberSetting arcWidth  = new NumberSetting("Ширина арки", 8f,   3f,  25f, 1f);
    private final NumberSetting thickness = new NumberSetting("Толщина",     3f,   1f,   8f, 0.5f);
    private final BooleanSetting arrow    = new BooleanSetting("Стрелка",   true);
    private final ColorSetting colorSetting = new ColorSetting("Цвет", new Color(255, 40, 40, 255).getRGB());

    private boolean  hadDamage    = false;
    private float    lastHealth   = -1f;
    private float    damageAngle  = 0f;
    private long     damageMsTime = -99_999L;

    public CrosshairHitNotice() {
        super("CrosshairHitNotice", Category.Render, "Направленный индикатор откуда прилетел урон");
        getSettings().add(radius);
        getSettings().add(arcWidth);
        getSettings().add(thickness);
        getSettings().add(arrow);
        getSettings().add(colorSetting);
    }

    @Override
    public void onDisable() {
        lastHealth = -1f;
        hadDamage  = false;
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;

        float hp = mc.player.getHealth();

        if (lastHealth > 0f && hp < lastHealth - 0.01f) {

            Entity attacker = findClosestAttacker();
            if (attacker != null) {
                Vec3d toAttacker = attacker.getPos().subtract(mc.player.getPos());

                float yawRad = (float) Math.toRadians(mc.player.getYaw());
                float dx = (float) toAttacker.x;
                float dz = (float) toAttacker.z;

                float worldAngle = (float) Math.toDegrees(Math.atan2(dx, -dz));

                damageAngle  = worldAngle - mc.player.getYaw();
            } else {

                damageAngle = 0f;
            }
            damageMsTime = System.currentTimeMillis();
            hadDamage    = true;
        }
        lastHealth = hp;
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (!hadDamage) return;
        long age = System.currentTimeMillis() - damageMsTime;
        if (age < 0 || age > FADE_MS) return;

        float t     = age / (float) FADE_MS;
        float fadeI = MathHelper.clamp(age / 100f, 0f, 1f);
        float fadeO = MathHelper.clamp((1f - t) / 0.3f, 0f, 1f);
        float alpha = fadeI * fadeO * fadeI;
        if (alpha <= 0.01f) return;

        MatrixStack ms = event.getContext().getMatrices();
        int cx  = event.getContext().getScaledWindowWidth()  / 2;
        int cy  = event.getContext().getScaledWindowHeight() / 2;
        float r = radius.getValue();
        float aw = arcWidth.getValue();
        float lw = thickness.getValue();

        Color base = colorSetting.getColor();

        Color col  = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                (int)(255 * alpha));

        drawDirectionalArc(ms, cx, cy, r, aw, lw, damageAngle, col, alpha);
    }

    private void drawDirectionalArc(MatrixStack ms, int cx, int cy,
                                    float radius, float arcDeg, float lineW,
                                    float centerAngleDeg, Color col, float alpha) {
        float startDeg = centerAngleDeg - arcDeg / 2f;
        float endDeg   = centerAngleDeg + arcDeg / 2f;
        int   segments = Math.max(6, (int)(arcDeg / 4));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        int r = col.getRed(), g = col.getGreen(), b = col.getBlue(), a = col.getAlpha();

        float innerR = radius;
        float outerR = radius + lineW;

        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float ang0 = (float) Math.toRadians(startDeg + (endDeg - startDeg) * t0);
            float ang1 = (float) Math.toRadians(startDeg + (endDeg - startDeg) * t1);

            int a0 = (int)(a * (t0 < 0.15f ? t0 / 0.15f : t0 > 0.85f ? (1f - t0) / 0.15f : 1f));
            int a1 = (int)(a * (t1 < 0.15f ? t1 / 0.15f : t1 > 0.85f ? (1f - t1) / 0.15f : 1f));

            float ix0 = cx + MathHelper.sin(ang0) * innerR;
            float iy0 = cy - MathHelper.cos(ang0) * innerR;
            float ox0 = cx + MathHelper.sin(ang0) * outerR;
            float oy0 = cy - MathHelper.cos(ang0) * outerR;
            float ix1 = cx + MathHelper.sin(ang1) * innerR;
            float iy1 = cy - MathHelper.cos(ang1) * innerR;
            float ox1 = cx + MathHelper.sin(ang1) * outerR;
            float oy1 = cy - MathHelper.cos(ang1) * outerR;

            ms.push();
            org.joml.Matrix4f m4 = ms.peek().getPositionMatrix();
            buf.vertex(m4, ix0, iy0, 0f).color(r, g, b, a0);
            buf.vertex(m4, ox0, oy0, 0f).color(r, g, b, a0);
            buf.vertex(m4, ox1, oy1, 0f).color(r, g, b, a1);
            buf.vertex(m4, ix1, iy1, 0f).color(r, g, b, a1);
            ms.pop();
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        if (arrow.getValue()) {
            float midAngle = (float) Math.toRadians(centerAngleDeg);
            float tipR   = outerR + lineW * 2.5f;
            float baseR  = innerR - lineW;
            float sideHalf = lineW * 1.5f;

            float tipX  = cx + MathHelper.sin(midAngle) * tipR;
            float tipY  = cy - MathHelper.cos(midAngle) * tipR;

            float perpX = MathHelper.cos(midAngle) * sideHalf;
            float perpY = MathHelper.sin(midAngle) * sideHalf;

            float baseX = cx + MathHelper.sin(midAngle) * baseR;
            float baseY = cy - MathHelper.cos(midAngle) * baseR;

            BufferBuilder tri = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            org.joml.Matrix4f m4 = ms.peek().getPositionMatrix();
            tri.vertex(m4, tipX,           tipY,           0f).color(r, g, b, a);
            tri.vertex(m4, baseX + perpX,  baseY + perpY,  0f).color(r, g, b, 0);
            tri.vertex(m4, baseX - perpX,  baseY - perpY,  0f).color(r, g, b, 0);
            BufferRenderer.drawWithGlobalProgram(tri.end());
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private Entity findClosestAttacker() {
        Entity closest = null;
        double minDist = 20.0;
        for (Entity ent : mc.world.getEntities()) {
            if (ent == mc.player) continue;
            if (!(ent instanceof LivingEntity)) continue;
            double d = ent.getPos().distanceTo(mc.player.getPos());
            if (d < minDist) { minDist = d; closest = ent; }
        }
        return closest;
    }
}
