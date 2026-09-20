package dev.darkvisuals.client.util.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.browser.BrowserCapture;
import dev.darkvisuals.client.util.Wrapper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * 3D-плоскость с живым экраном реального браузера.
 *
 * ИЗМЕНЕНИЕ: drawCapture теперь принимает capture.getGlId() вместо
 * capture.getTexture().getGlId() — безопаснее, т.к. getGlId() возвращает -1
 * если текстура ещё не создана, а не NPE.
 */
public class Browser3DRenderer implements Wrapper {

    private static final double MOVING_THRESHOLD_SQ = 0.03 * 0.03;
    private static final double SNAP_DISTANCE_SQ    = 64.0 * 64.0;

    private static final float PX_PER_BLOCK  = 100.0f;
    private static final float FALLBACK_ASPECT = 0.625f;
    private static final float TITLE_BAR     = 26f;

    private static final int PANEL_COLOR      = 0xF20F1116;
    private static final int TITLE_BAR_COLOR  = 0xF21A1D24;
    private static final int BORDER_COLOR     = 0x40FFFFFF;
    private static final int TRAFFIC_RED      = 0xE6FF5F56;
    private static final int TRAFFIC_YELLOW   = 0xE6FFBD2E;
    private static final int TRAFFIC_GREEN    = 0xE627C93F;

    private Vec3d targetPos;
    private Vec3d prevPos;
    private float yaw;
    private float prevYaw;
    private boolean standing;

    public boolean isStanding() { return standing; }

    public void reset() {
        targetPos = null;
        prevPos   = null;
        yaw = 0f; prevYaw = 0f;
        standing  = false;
    }

    public void update(ClientPlayerEntity player, float distance, float followSpeed) {
        boolean moving  = player.getVelocity().lengthSquared() > MOVING_THRESHOLD_SQ;
        Vec3d   desired = desiredScreenPos(player, distance, !moving);

        if (targetPos == null
                || targetPos.squaredDistanceTo(player.getX(), player.getY(), player.getZ()) > SNAP_DISTANCE_SQ) {
            targetPos = desired;
            prevPos   = desired;
            yaw       = facingYaw(desired, player);
            prevYaw   = yaw;
            standing  = !moving;
            return;
        }

        prevPos  = targetPos;
        prevYaw  = yaw;
        standing = !moving;

        float speed = standing ? Math.min(0.8f, followSpeed * 2.0f) : followSpeed;
        targetPos = targetPos.lerp(desired, speed);

        float targetYaw = facingYaw(targetPos, player);
        yaw += MathHelper.wrapDegrees(targetYaw - yaw) * 0.45f;
    }

    public void render(MatrixStack matrices, float tickDelta, BrowserCapture capture, float widthBlocks) {
        if (targetPos == null || mc.player == null || mc.world == null) return;

        Vec3d cam       = mc.gameRenderer.getCamera().getPos();
        Vec3d renderPos = prevPos.lerp(targetPos, tickDelta);
        float renderYaw = MathHelper.lerp(tickDelta, prevYaw, yaw);

        float pw     = widthBlocks * PX_PER_BLOCK;
        float aspect = FALLBACK_ASPECT;
        if (capture != null && capture.isAvailable() && capture.getHeight() > 0) {
            aspect = (float) capture.getHeight() / capture.getWidth();
        }
        float ph = pw * aspect + TITLE_BAR;

        matrices.push();
        matrices.translate(renderPos.x - cam.x, renderPos.y - cam.y, renderPos.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderYaw));
        matrices.scale(0.01f, -0.01f, 0.01f);

        Color accent = ThemeManager.getInstance().getThemeColor();
        drawChrome(matrices, pw, ph, accent);

        if (capture != null && capture.isAvailable()) {
            // ИЗМЕНЕНИЕ: capture.getGlId() вместо capture.getTexture().getGlId()
            int glId = capture.getGlId();
            if (glId > 0) {
                drawCapture(matrices, 1.5f, TITLE_BAR + 1.5f, pw - 3f, ph - TITLE_BAR - 3f, glId);
            }
            drawTitleText(matrices, pw, capture.getWindowTitle());
        } else {
            drawTitleText(matrices, pw, "Браузер не найден");
            drawPlaceholder(matrices, pw, ph, capture);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private Vec3d desiredScreenPos(ClientPlayerEntity player, double distance, boolean front) {
        float yawRad = (float) Math.toRadians(player.headYaw);
        Vec3d look   = new Vec3d(-MathHelper.sin(yawRad), 0.0, MathHelper.cos(yawRad));
        Vec3d anchor = new Vec3d(player.getX(), player.getEyeY() - 0.35, player.getZ());
        if (front) {
            double fd = Math.min(distance * 0.7, 1.75);
            return anchor.add(look.multiply(fd));
        }
        return anchor.subtract(look.multiply(distance));
    }

    private float facingYaw(Vec3d screenPos, ClientPlayerEntity player) {
        double dx = player.getX() - screenPos.x;
        double dz = player.getZ() - screenPos.z;
        return (float) Math.toDegrees(Math.atan2(dx, dz));
    }

    private void drawChrome(MatrixStack matrices, float pw, float ph, Color accent) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,       GlStateManager.DstFactor.ZERO);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f m      = matrices.peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        quad(bb, m, 0,     0,           pw, ph,            0f,  PANEL_COLOR);
        quad(bb, m, 0,     0,           pw, TITLE_BAR,     0.3f, TITLE_BAR_COLOR);
        quad(bb, m, 0,     TITLE_BAR,   pw, TITLE_BAR + 1.5f, 0.3f, withAlpha(accent, 150));
        quad(bb, m, 11f,   9f,          19f, 17f,           0.4f, TRAFFIC_RED);
        quad(bb, m, 24f,   9f,          32f, 17f,           0.4f, TRAFFIC_YELLOW);
        quad(bb, m, 37f,   9f,          45f, 17f,           0.4f, TRAFFIC_GREEN);
        quad(bb, m, 0,     0,           pw,  1.5f,          0.4f, BORDER_COLOR);
        quad(bb, m, 0,     ph - 1.5f,  pw,  ph,             0.4f, BORDER_COLOR);
        quad(bb, m, 0,     0,           1.5f, ph,            0.4f, BORDER_COLOR);
        quad(bb, m, pw - 1.5f, 0,      pw,  ph,             0.4f, BORDER_COLOR);

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private void drawCapture(MatrixStack matrices, float x, float y, float w, float h, int glId) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, glId);

        Matrix4f      m  = matrices.peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bb.vertex(m, x,     y,     0.2f).texture(0f, 0f);
        bb.vertex(m, x + w, y,     0.2f).texture(1f, 0f);
        bb.vertex(m, x + w, y + h, 0.2f).texture(1f, 1f);
        bb.vertex(m, x,     y + h, 0.2f).texture(0f, 1f);
        BufferRenderer.drawWithGlobalProgram(bb.end());

        RenderSystem.setShaderTexture(0, 0);
    }

    private void drawTitleText(MatrixStack matrices, float pw, String title) {
        matrices.push();
        matrices.translate(0f, 0f, 1f);
        TextRenderer                  tr  = mc.textRenderer;
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        Matrix4f m = matrices.peek().getPositionMatrix();
        String   t = tr.trimToWidth(title, (int)(pw - 70f));
        tr.draw(t, 54f, 9f, 0xFFC8CDD7, false, m, vcp,
                TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        vcp.draw();
        matrices.pop();
    }

    private void drawPlaceholder(MatrixStack matrices, float pw, float ph, BrowserCapture capture) {
        matrices.push();
        matrices.translate(0f, 0f, 1f);
        TextRenderer                  tr  = mc.textRenderer;
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        Matrix4f m     = matrices.peek().getPositionMatrix();
        int      light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        TextRenderer.TextLayerType layer = TextRenderer.TextLayerType.NORMAL;

        String status = capture == null ? null : capture.getSearchStatus();
        String first  = (status == null || status.isBlank()) ? "Браузер не найден" : status;
        first = tr.trimToWidth(first, (int)(pw - 20f));
        String[] lines = {first, "", "Откройте окно выбранного браузера —",
                "его экран появится здесь автоматически.", "",
                "Браузер выбирается настройкой \"Браузер\" в модуле."};

        float y = TITLE_BAR + (ph - TITLE_BAR) / 2f - lines.length * 6f;
        for (String line : lines) {
            float lw = tr.getWidth(line);
            tr.draw(line, pw / 2f - lw / 2f, y, 0xFFA9AFB9, false, m, vcp, layer, 0, light);
            y += 12f;
        }
        vcp.draw();
        matrices.pop();
    }

    private static int withAlpha(Color color, int alpha) {
        return (color.getRGB() & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static void quad(BufferBuilder bb, Matrix4f m,
                              float x1, float y1, float x2, float y2, float z, int rgba) {
        bb.vertex(m, x1, y1, z).color(rgba);
        bb.vertex(m, x2, y1, z).color(rgba);
        bb.vertex(m, x2, y2, z).color(rgba);
        bb.vertex(m, x1, y2, z).color(rgba);
    }
}