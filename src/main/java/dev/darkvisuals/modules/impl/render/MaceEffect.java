package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class MaceEffect extends Module {

    private final ListSetting mode = new ListSetting("Режим", true,
            new BooleanSetting("Случайный", false),
            new BooleanSetting("Пульс", true),
            new BooleanSetting("Призрак", false),
            new BooleanSetting("Молния", false),
            new BooleanSetting("Разлом", false),
            new BooleanSetting("Гравитация", false),
            new BooleanSetting("Соник", false),
            new BooleanSetting("Взрыв", false));

    private final BooleanSetting onlyCrit = new BooleanSetting("Только в падении (Крит)", true);
    private final NumberSetting scale = new NumberSetting("Размер", 1.0f, 0.5f, 2.5f, 0.1f);
    private final NumberSetting duration = new NumberSetting("Длительность", 1.0f, 0.4f, 2.5f, 0.1f);

    private final ListSetting colorMode = new ListSetting("Цвет", true,
            new BooleanSetting("Тема", true),
            new BooleanSetting("Огонь", false),
            new BooleanSetting("Электро", false),
            new BooleanSetting("Космос", false),
            new BooleanSetting("Золотой", false));

    private final List<ActiveMaceHit> activeHits = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    public MaceEffect() {
        super("MaceEffect", Category.Render, "Эпичные визуальные эффекты при ударе булавой");
        getSettings().add(mode);
        getSettings().add(onlyCrit);
        getSettings().add(scale);
        getSettings().add(duration);
        getSettings().add(colorMode);
    }

    @Override
    public void onDisable() {
        activeHits.clear();
        super.onDisable();
    }

    @EventHandler
    public void onAttack(EventAttackEntity event) {
        if (fullNullCheck()) return;
        if (mc.player == null || event.getTarget() == null) return;

        if (!mc.player.getMainHandStack().isOf(Items.MACE)) return;

        boolean isAirOrCrit = mc.player.fallDistance > 0.0f
                || !mc.player.isOnGround()
                || mc.player.getVelocity().y < -0.05
                || (mc.player.isGliding());

        if (onlyCrit.getValue() && !isAirOrCrit) {
            return;
        }

        Entity target = event.getTarget();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.45, 0);

        String effectType = resolveEffectType();
        Color effectColor = resolveColor();
        long lifeTimeMs = (long) (duration.getValue() * 1000L);

        ActiveMaceHit hit = new ActiveMaceHit(
                effectType,
                targetPos,
                target instanceof LivingEntity ? (LivingEntity) target : null,
                target.getWidth(),
                target.getHeight(),
                System.currentTimeMillis(),
                lifeTimeMs,
                scale.getValue(),
                effectColor
        );

        activeHits.add(hit);
    }

    private String resolveEffectType() {
        if (isMode("Случайный")) {
            String[] pool = {"Пульс", "Призрак", "Молния", "Разлом", "Гравитация", "Соник", "Взрыв"};
            return pool[random.nextInt(pool.length)];
        }
        for (String m : new String[]{"Пульс", "Призрак", "Молния", "Разлом", "Гравитация", "Соник", "Взрыв"}) {
            if (isMode(m)) return m;
        }
        return "Пульс";
    }

    private boolean isMode(String name) {
        BooleanSetting s = mode.getName(name);
        return s != null && s.getValue();
    }

    private Color resolveColor() {
        if (isColor("Огонь")) return new Color(255, 95, 20);
        if (isColor("Электро")) return new Color(30, 200, 255);
        if (isColor("Космос")) return new Color(175, 45, 255);
        if (isColor("Золотой")) return new Color(255, 215, 40);

        ThemeManager tm = ThemeManager.getInstance();
        if (tm != null && tm.getCurrentTheme() != null) {
            return tm.getCurrentTheme().getAccentColor();
        }
        return new Color(180, 50, 255);
    }

    private boolean isColor(String name) {
        BooleanSetting s = colorMode.getName(name);
        return s != null && s.getValue();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck() || activeHits.isEmpty()) return;

        long now = System.currentTimeMillis();
        activeHits.removeIf(h -> (now - h.startTime) > h.durationMs);

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(false);

        for (ActiveMaceHit hit : activeHits) {
            float progress = MathHelper.clamp((now - hit.startTime) / (float) hit.durationMs, 0f, 1f);

            switch (hit.type) {
                case "Пульс" -> renderPulse(matrices, cam, hit, progress);
                case "Призрак" -> renderGhost(matrices, cam, hit, progress);
                case "Молния" -> renderLightning(matrices, cam, hit, progress);
                case "Разлом" -> renderFissure(matrices, cam, hit, progress);
                case "Гравитация" -> renderBlackHole(matrices, cam, hit, progress);
                case "Соник" -> renderSonicBoom(matrices, cam, hit, progress);
                case "Взрыв" -> renderMushroomBurst(matrices, cam, hit, progress);
                default -> renderPulse(matrices, cam, hit, progress);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderPulse(MatrixStack matrices, Vec3d cam, ActiveMaceHit hit, float t) {
        float eased = Easing.EASE_OUT_CUBIC.apply(t);
        float radius = 4.2f * hit.scale * eased;
        float alpha = (float) Math.pow(1.0f - t, 1.2) * 0.95f;
        if (alpha <= 0.01f || radius <= 0.05f) return;

        int r = hit.color.getRed(), g = hit.color.getGreen(), b = hit.color.getBlue();
        int a = (int) (240 * alpha);

        matrices.push();
        matrices.translate(hit.pos.x - cam.x, hit.pos.y - (hit.entityHeight * 0.4) - cam.y, hit.pos.z - cam.z);
        Matrix4f m = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        int segs = 36;
        float inner = Math.max(0f, radius - 0.5f * hit.scale);
        for (int i = 0; i <= segs; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segs);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            buffer.vertex(m, cos * inner, 0.04f, sin * inner).color(r, g, b, 0);
            buffer.vertex(m, cos * radius, 0.04f, sin * radius).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        float coreRadius = radius * 0.45f;
        for (int i = 0; i <= segs; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segs);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            buffer.vertex(m, 0f, 0.06f, 0f).color(255, 255, 255, (int) (140 * alpha));
            buffer.vertex(m, cos * coreRadius, 0.06f, sin * coreRadius).color(r, g, b, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    private void renderGhost(MatrixStack matrices, Vec3d cam, ActiveMaceHit hit, float t) {
        float rise = Easing.EASE_OUT_CUBIC.apply(t) * 2.4f * hit.scale;
        float fade = (float) Math.pow(1.0f - t, 1.4);
        float alpha = fade * 0.65f;
        if (alpha <= 0.01f) return;

        matrices.push();
        matrices.translate(hit.pos.x - cam.x, hit.pos.y + rise - cam.y, hit.pos.z - cam.z);

        if (hit.victim != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(
                    hit.color.getRed() / 255f,
                    hit.color.getGreen() / 255f,
                    hit.color.getBlue() / 255f,
                    alpha
            );

            BlockPos lightPos = BlockPos.ofFloored(hit.victim.getX(), hit.victim.getY(), hit.victim.getZ());
            int light = mc.world != null
                    ? LightmapTextureManager.pack(mc.world.getLightLevel(LightType.BLOCK, lightPos), mc.world.getLightLevel(LightType.SKY, lightPos))
                    : LightmapTextureManager.MAX_LIGHT_COORDINATE;

            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
            dispatcher.render(hit.victim, 0.0, 0.0, 0.0, 1.0f, matrices, immediate, light);
            immediate.draw();

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        } else {

            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            float w = hit.entityWidth * 0.7f * hit.scale;
            float h = hit.entityHeight * 0.9f * hit.scale;
            Matrix4f m = matrices.peek().getPositionMatrix();

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int r = hit.color.getRed(), g = hit.color.getGreen(), b = hit.color.getBlue();
            int a = (int) (180 * alpha);

            buf.vertex(m, -w, -h * 0.5f, 0).color(r, g, b, 0);
            buf.vertex(m, -w, h * 0.5f, 0).color(255, 255, 255, a);
            buf.vertex(m, w, h * 0.5f, 0).color(255, 255, 255, a);
            buf.vertex(m, w, -h * 0.5f, 0).color(r, g, b, 0);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        matrices.pop();
    }

    private void renderLightning(MatrixStack matrices, Vec3d cam, ActiveMaceHit hit, float t) {
        float fade = (float) Math.pow(1.0f - t, 2.0);
        float alpha = fade * 0.95f;
        if (alpha <= 0.01f) return;

        matrices.push();
        matrices.translate(hit.pos.x - cam.x, hit.pos.y - cam.y, hit.pos.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

        Matrix4f m = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int r = hit.color.getRed(), g = hit.color.getGreen(), b = hit.color.getBlue();
        int a = (int) (255 * alpha);

        float curY = 0f;
        float curX = 0f;
        float topY = 16.0f * hit.scale;
        int steps = 14;
        float stepH = topY / steps;

        Random boltRnd = new Random(hit.startTime);
        for (int i = 0; i < steps; i++) {
            float nextY = curY + stepH;
            float nextX = curX + (boltRnd.nextFloat() - 0.5f) * 0.75f * hit.scale;
            float thick = 0.08f * (1.0f - i / (float) steps * 0.5f) * hit.scale;

            buf.vertex(m, curX - thick, curY, 0).color(r, g, b, a);
            buf.vertex(m, nextX - thick, nextY, 0).color(255, 255, 255, a);
            buf.vertex(m, nextX + thick, nextY, 0).color(255, 255, 255, a);
            buf.vertex(m, curX + thick, curY, 0).color(r, g, b, a);

            if (i % 3 == 0 && i < steps - 2) {
                float bX = nextX + (boltRnd.nextBoolean() ? 0.8f : -0.8f) * hit.scale;
                float bY = nextY + stepH * 0.7f;
                float bThick = thick * 0.6f;
                buf.vertex(m, nextX - bThick, nextY, 0).color(r, g, b, a / 2);
                buf.vertex(m, bX - bThick, bY, 0).color(255, 255, 255, 0);
                buf.vertex(m, bX + bThick, bY, 0).color(255, 255, 255, 0);
                buf.vertex(m, nextX + bThick, nextY, 0).color(r, g, b, a / 2);
            }

            curX = nextX;
            curY = nextY;
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
        matrices.pop();
    }

    private void renderFissure(MatrixStack matrices, Vec3d cam, ActiveMaceHit hit, float t) {
        float expand = Easing.EASE_OUT_CUBIC.apply(Math.min(1.0f, t * 2.5f));
        float fade = (float) Math.pow(1.0f - t, 1.2);
        float alpha = fade * 0.9f;
        if (alpha <= 0.01f) return;

        matrices.push();
        matrices.translate(hit.pos.x - cam.x, hit.pos.y - (hit.entityHeight * 0.4) - cam.y, hit.pos.z - cam.z);
        Matrix4f m = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int r = hit.color.getRed(), g = hit.color.getGreen(), b = hit.color.getBlue();
        int a = (int) (230 * alpha);

        int rays = 8;
        Random fissRnd = new Random(hit.startTime);
        for (int i = 0; i < rays; i++) {
            float baseAngle = (float) (i * Math.PI * 2.0 / rays);
            float length = (2.5f + fissRnd.nextFloat() * 1.5f) * hit.scale * expand;
            float segLen = length / 4f;

            float px = 0f, pz = 0f;
            for (int s = 0; s < 4; s++) {
                float segAngle = baseAngle + (fissRnd.nextFloat() - 0.5f) * 0.4f;
                float nx = px + MathHelper.cos(segAngle) * segLen;
                float nz = pz + MathHelper.sin(segAngle) * segLen;
                float thick = 0.09f * (1.0f - s / 4.0f) * hit.scale;

                float perpX = -MathHelper.sin(segAngle) * thick;
                float perpZ = MathHelper.cos(segAngle) * thick;

                buf.vertex(m, px - perpX, 0.03f, pz - perpZ).color(r, g, b, a);
                buf.vertex(m, nx - perpX, 0.03f, nz - perpZ).color(255, 255, 255, (int) (a * 0.7f));
                buf.vertex(m, nx + perpX, 0.03f, nz + perpZ).color(255, 255, 255, (int) (a * 0.7f));
                buf.vertex(m, px + perpX, 0.03f, pz + perpZ).color(r, g, b, a);

                px = nx;
                pz = nz;
            }
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        matrices.pop();
    }

    private void renderBlackHole(MatrixStack matrices, Vec3d cam, ActiveMaceHit hit, float t) {
        float sizeMod = t < 0.35f
                ? 1.0f - (t / 0.35f) * 0.65f
                : 0.35f + ((t - 0.35f) / 0.65f) * 2.2f;
        float fade = (float) Math.pow(1.0f - t, 1.3);
        float alpha = fade * 0.95f;
        if (alpha <= 0.01f) return;

        matrices.push();
        matrices.translate(hit.pos.x - cam.x, hit.pos.y - cam.y, hit.pos.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(t * 360f * 2.0f));

        Matrix4f m = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        int r = hit.color.getRed(), g = hit.color.getGreen(), b = hit.color.getBlue();
        int a = (int) (240 * alpha);
        float outerR = 1.6f * hit.scale * sizeMod;
        float innerR = outerR * 0.3f;

        int segs = 32;
        for (int i = 0; i <= segs; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segs);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            buf.vertex(m, cos * innerR, sin * innerR, 0).color(10, 5, 20, (int) (255 * alpha));
            buf.vertex(m, cos * outerR, sin * outerR, 0).color(r, g, b, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        matrices.pop();
    }

    private void renderSonicBoom(MatrixStack matrices, Vec3d cam, ActiveMaceHit hit, float t) {
        float eased = Easing.EASE_OUT_CUBIC.apply(t);
        float radius = 3.8f * hit.scale * eased;
        float fade = (float) Math.pow(1.0f - t, 1.4);
        float alpha = fade * 0.95f;
        if (alpha <= 0.01f) return;

        matrices.push();
        matrices.translate(hit.pos.x - cam.x, hit.pos.y - cam.y, hit.pos.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

        Matrix4f m = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        int r = Math.min(255, hit.color.getRed() + 50);
        int g = Math.min(255, hit.color.getGreen() + 80);
        int b = Math.min(255, hit.color.getBlue() + 100);
        int a = (int) (220 * alpha);

        int rings = 3;
        for (int ring = 0; ring < rings; ring++) {
            float ringRad = radius * (1.0f - ring * 0.22f);
            if (ringRad <= 0.05f) continue;
            float border = 0.22f * hit.scale;
            float inner = Math.max(0f, ringRad - border);

            buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            int segs = 32;
            for (int i = 0; i <= segs; i++) {
                float angle = (float) (i * Math.PI * 2.0 / segs);
                float cos = MathHelper.cos(angle);
                float sin = MathHelper.sin(angle);
                buf.vertex(m, cos * inner, sin * inner, 0).color(r, g, b, 0);
                buf.vertex(m, cos * ringRad, sin * ringRad, 0).color(r, g, b, (int) (a * (1.0f - ring * 0.25f)));
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }
        matrices.pop();
    }

    private void renderMushroomBurst(MatrixStack matrices, Vec3d cam, ActiveMaceHit hit, float t) {
        float eased = Easing.EASE_OUT_CUBIC.apply(t);
        float stemHeight = 2.8f * hit.scale * eased;
        float capRadius = 2.2f * hit.scale * eased;
        float fade = (float) Math.pow(1.0f - t, 1.2);
        float alpha = fade * 0.9f;
        if (alpha <= 0.01f) return;

        matrices.push();
        matrices.translate(hit.pos.x - cam.x, hit.pos.y - (hit.entityHeight * 0.35) - cam.y, hit.pos.z - cam.z);

        Matrix4f m = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        int r = hit.color.getRed(), g = hit.color.getGreen(), b = hit.color.getBlue();
        int a = (int) (210 * alpha);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int segs = 20;
        for (int i = 0; i <= segs; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segs);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            float stemRad = 0.35f * hit.scale * (1.0f - t * 0.3f);
            buf.vertex(m, cos * stemRad, 0, sin * stemRad).color(r, g, b, a);
            buf.vertex(m, cos * (stemRad * 1.5f), stemHeight, sin * (stemRad * 1.5f)).color(255, 200, 80, a);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segs; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segs);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            buf.vertex(m, 0, stemHeight + 0.3f * hit.scale, 0).color(255, 230, 150, (int) (240 * alpha));
            buf.vertex(m, cos * capRadius, stemHeight - 0.2f * hit.scale, sin * capRadius).color(r, g, b, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        matrices.pop();
    }

    private static class ActiveMaceHit {
        final String type;
        final Vec3d pos;
        final LivingEntity victim;
        final float entityWidth;
        final float entityHeight;
        final long startTime;
        final long durationMs;
        final float scale;
        final Color color;

        ActiveMaceHit(String type, Vec3d pos, LivingEntity victim, float entityWidth, float entityHeight,
                      long startTime, long durationMs, float scale, Color color) {
            this.type = type;
            this.pos = pos;
            this.victim = victim;
            this.entityWidth = entityWidth;
            this.entityHeight = entityHeight;
            this.startTime = startTime;
            this.durationMs = durationMs;
            this.scale = scale;
            this.color = color;
        }
    }
}
