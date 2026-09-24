package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.FriendsManager;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

/**
 * FriendGlow - свечение/аура для друзей отдельным цветом.
 *
 * Под каждым другом рисуется светящееся кольцо (аддитивный блендинг),
 * плюс опциональный контур по хитбоксу. Цвет настраивается отдельно от темы.
 */
public class FriendGlow extends Module {

    private static final int RING_SEGMENTS = 48;

    private final BooleanSetting showRing = new BooleanSetting("Кольцо", true);
    private final BooleanSetting showOutline = new BooleanSetting("Контур хитбокса", true);
    private final BooleanSetting spin = new BooleanSetting("Вращение", true);
    private final NumberSetting ringRadius = new NumberSetting("Радиус кольца", 0.55f, 0.3f, 1.2f, 0.05f);
    private final NumberSetting range = new NumberSetting("Дистанция", 64f, 8f, 128f, 1f);
    private final ColorSetting friendColor =
            new ColorSetting("Цвет друзей", new Color(90, 255, 150, 190).getRGB());
    private final BooleanSetting themeColor = new BooleanSetting("Цвет темы", false);

    public FriendGlow() {
        super("FriendGlow", Category.Render, "Свечение и аура для друзей отдельным цветом");
        getSettings().add(showRing);
        getSettings().add(showOutline);
        getSettings().add(spin);
        getSettings().add(ringRadius);
        getSettings().add(range);
        getSettings().add(themeColor);
        getSettings().add(friendColor);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        try (var __ = Perf.scopeCpu("FriendGlow.onRender3D")) {

            Color base = themeColor.getValue()
                    ? ThemeManager.getInstance().getCurrentTheme().getAccentColor()
                    : friendColor.getColor();

            Vec3d cam = mc.gameRenderer.getCamera().getPos();
            float tickDelta = event.getTickDelta();
            long now = System.currentTimeMillis();
            float time = now / 1000f;
            float maxRange = range.getValue();

            MatrixStack matrices = event.getMatrices();

            boolean any = false;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                if (!FriendsManager.checkFriend(p.getGameProfile().getName())) continue;

                Vec3d pos = p.getLerpedPos(tickDelta);
                double dist = pos.distanceTo(cam);
                if (dist > maxRange) continue;

                // Плавное появление с расстоянием
                float fade = 1f - MathHelper.clamp((float) (dist / maxRange), 0f, 1f) * 0.55f;
                Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                        Math.max(20, (int) (base.getAlpha() * fade)));

                if (showRing.getValue()) {
                    drawRing(matrices, cam, pos, c, time);
                    any = true;
                }
                if (showOutline.getValue()) {
                    drawOutline(matrices, cam, p.getBoundingBox(), c);
                    any = true;
                }
            }
        }
    }

    /** Светящееся кольцо на земле под игроком (аддитивный блендинг). */
    private void drawRing(MatrixStack matrices, Vec3d cam, Vec3d pos, Color color, float time) {
        float r = ringRadius.getValue();
        float rot = spin.getValue() ? time * 50f : 0f;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
        matrices.translate((float) (pos.x - cam.x), (float) (pos.y - cam.y + 0.02), (float) (pos.z - cam.z));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float inner = r * 0.72f;
        float outer = r;
        float a = color.getAlpha() / 255f;
        float cr = color.getRed() / 255f;
        float cg = color.getGreen() / 255f;
        float cb = color.getBlue() / 255f;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = rot * ((float) Math.PI / 180f) + (i / (float) RING_SEGMENTS) * (float) (Math.PI * 2);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            // Внешний край почти прозрачный, внутренний ярче - эффект свечения
            buffer.vertex(matrix, outer * cos, 0f, outer * sin).color(cr, cg, cb, a * 0.15f);
            buffer.vertex(matrix, inner * cos, 0f, inner * sin).color(cr, cg, cb, a * 0.85f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Тонкий контур по хитбоксу друга. */
    private void drawOutline(MatrixStack matrices, Vec3d cam, net.minecraft.util.math.Box box, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float minX = (float) (box.minX - cam.x);
        float minY = (float) (box.minY - cam.y);
        float minZ = (float) (box.minZ - cam.z);
        float maxX = (float) (box.maxX - cam.x);
        float maxY = (float) (box.maxY - cam.y);
        float maxZ = (float) (box.maxZ - cam.z);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
    }
}
