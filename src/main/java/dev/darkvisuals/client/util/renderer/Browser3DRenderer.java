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
 * 3D-плоскость с живым экраном реального браузера:
 *  - пока игрок движется, плоскость плавно следует за его спиной (Vec3d.lerp);
 *  - когда игрок стоит, плоскость плавно подходит ВПЕРЕДИ игрока
 *    и разворачивается передней стороной к нему;
 *  - на плоскость выводится кадр окна реального браузера
 *    ({@link BrowserCapture}), либо заглушка, если окно не найдено.
 *
 * Геометрия рисуется в "пиксельном" пространстве (1px = 0.01 блока),
 * поэтому размеры хрома окна задаются как в обычном GUI.
 */
public class Browser3DRenderer implements Wrapper {

    /** Порог скорости, ниже которого игрок считается остановившимся (0.03 блока/тик). */
    private static final double MOVING_THRESHOLD_SQ = 0.03 * 0.03;
    /** Если игрок телепортировался/сменил мир — мгновенно переставить экран. */
    private static final double SNAP_DISTANCE_SQ = 64.0 * 64.0;

    /** Пиксели за блок (масштаб матрицы = 0.01). */
    private static final float PX_PER_BLOCK = 100.0f;
    /** Аспект плоскости, пока нет кадра браузера. */
    private static final float FALLBACK_ASPECT = 0.625f;
    /** Высота строки заголовка (px). */
    private static final float TITLE_BAR = 26f;

    private static final int PANEL_COLOR = 0xF20F1116;
    private static final int TITLE_BAR_COLOR = 0xF21A1D24;
    private static final int BORDER_COLOR = 0x40FFFFFF;

    private static final int TRAFFIC_RED = 0xE6FF5F56;
    private static final int TRAFFIC_YELLOW = 0xE6FFBD2E;
    private static final int TRAFFIC_GREEN = 0xE627C93F;

    /** Текущая позиция экрана в мире (сглаженная). */
    private Vec3d targetPos;
    /** Позиция на предыдущем тике — для интерполяции в рендере. */
    private Vec3d prevPos;
    /** Угол поворота плоскости (градусы), +Z локальной оси смотрит на игрока. */
    private float yaw;
    private float prevYaw;
    /** true, когда игрок стоит (плоскость перед игроком). */
    private boolean standing;

    public boolean isStanding() {
        return standing;
    }

    public void reset() {
        targetPos = null;
        prevPos = null;
        yaw = 0f;
        prevYaw = 0f;
        standing = false;
    }

    /**
     * Актуализация координат (вызывается из tick() модуля).
     */
    public void update(ClientPlayerEntity player, float distance, float followSpeed) {
        boolean moving = player.getVelocity().lengthSquared() > MOVING_THRESHOLD_SQ;
        Vec3d desired = desiredScreenPos(player, distance, !moving);

        if (targetPos == null
                || targetPos.squaredDistanceTo(player.getX(), player.getY(), player.getZ()) > SNAP_DISTANCE_SQ) {
            // первое появление или телепорт/смена мира
            targetPos = desired;
            prevPos = desired;
            yaw = facingYaw(desired, player);
            prevYaw = yaw;
            standing = !moving;
            return;
        }

        prevPos = targetPos;
        prevYaw = yaw;
        standing = !moving;

        // в движении — следование за спиной; на месте — быстрый подлёт вперёд к игроку
        float speed = standing ? Math.min(0.8f, followSpeed * 2.0f) : followSpeed;
        targetPos = targetPos.lerp(desired, speed);

        // плавная ориентация передней стороной к игроку
        float targetYaw = facingYaw(targetPos, player);
        yaw += MathHelper.wrapDegrees(targetYaw - yaw) * 0.45f;
    }

    /**
     * Рендер плоскости (вызывается из события рендера мира).
     */
    public void render(MatrixStack matrices, float tickDelta, BrowserCapture capture, float widthBlocks) {
        if (targetPos == null || mc.player == null || mc.world == null) return;

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d renderPos = prevPos.lerp(targetPos, tickDelta);
        float renderYaw = MathHelper.lerp(tickDelta, prevYaw, yaw);

        float pw = widthBlocks * PX_PER_BLOCK;
        // аспект берём из реального кадра браузера
        float aspect = FALLBACK_ASPECT;
        if (capture != null && capture.isAvailable() && capture.getHeight() > 0) {
            aspect = (float) capture.getHeight() / capture.getWidth();
        }
        float ph = pw * aspect + TITLE_BAR;

        matrices.push();
        matrices.translate(renderPos.x - cam.x, renderPos.y - cam.y, renderPos.z - cam.z);
        // разворот передней стороной к игроку
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderYaw));
        // пиксельное пространство, ось Y направлена вниз
        matrices.scale(0.01f, -0.01f, 0.01f);

        Color accent = ThemeManager.getInstance().getThemeColor();
        drawChrome(matrices, pw, ph, accent);

        if (capture != null && capture.isAvailable()) {
            drawCapture(matrices, 1.5f, TITLE_BAR + 1.5f, pw - 3f, ph - TITLE_BAR - 3f,
                    capture.getTexture().getGlId());
            drawTitleText(matrices, pw, capture.getWindowTitle());
        } else {
            drawTitleText(matrices, pw, "Браузер не найден");
            drawPlaceholder(matrices, pw, ph, capture);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    /**
     * Точка на линии взгляда игрока:
     * сзади — пока игрок движется, спереди и ближе — когда он стоит.
     */
    private Vec3d desiredScreenPos(ClientPlayerEntity player, double distance, boolean front) {
        float yawRad = (float) Math.toRadians(player.headYaw);
        Vec3d look = new Vec3d(-MathHelper.sin(yawRad), 0.0, MathHelper.cos(yawRad));
        Vec3d anchor = new Vec3d(player.getX(), player.getEyeY() - 0.35, player.getZ());
        if (front) {
            // стоя — плоскость подлетает почти вплотную и висит перед лицом
            double frontDistance = Math.min(distance * 0.7, 1.75);
            return anchor.add(look.multiply(frontDistance));
        }
        return anchor.subtract(look.multiply(distance));
    }

    /**
     * Угол, при котором +Z локальной оси плоскости указывает на игрока.
     */
    private float facingYaw(Vec3d screenPos, ClientPlayerEntity player) {
        double dx = player.getX() - screenPos.x;
        double dz = player.getZ() - screenPos.z;
        return (float) Math.toDegrees(Math.atan2(dx, dz));
    }

    private void drawChrome(MatrixStack matrices, float pw, float ph, Color accent) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO
        );
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // подложка под весь экран
        quad(buffer, m, 0, 0, pw, ph, 0f, PANEL_COLOR);
        // строка заголовка
        quad(buffer, m, 0, 0, pw, TITLE_BAR, 0.3f, TITLE_BAR_COLOR);
        // акцентная линия под заголовком
        quad(buffer, m, 0, TITLE_BAR, pw, TITLE_BAR + 1.5f, 0.3f, withAlpha(accent, 150));
        // кнопки "светофор"
        quad(buffer, m, 11f, 9f, 19f, 17f, 0.4f, TRAFFIC_RED);
        quad(buffer, m, 24f, 9f, 32f, 17f, 0.4f, TRAFFIC_YELLOW);
        quad(buffer, m, 37f, 9f, 45f, 17f, 0.4f, TRAFFIC_GREEN);
        // рамка
        quad(buffer, m, 0, 0, pw, 1.5f, 0.4f, BORDER_COLOR);
        quad(buffer, m, 0, ph - 1.5f, pw, ph, 0.4f, BORDER_COLOR);
        quad(buffer, m, 0, 0, 1.5f, ph, 0.4f, BORDER_COLOR);
        quad(buffer, m, pw - 1.5f, 0, pw, ph, 0.4f, BORDER_COLOR);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    /**
     * Текстурированный прямоугольник с кадром браузера (в мировом пространстве).
     */
    private void drawCapture(MatrixStack matrices, float x, float y, float w, float h, int glId) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, glId);

        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(m, x, y, 0.2f).texture(0f, 0f);
        buffer.vertex(m, x + w, y, 0.2f).texture(1f, 0f);
        buffer.vertex(m, x + w, y + h, 0.2f).texture(1f, 1f);
        buffer.vertex(m, x, y + h, 0.2f).texture(0f, 1f);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.setShaderTexture(0, 0);
    }

    /**
     * Заголовок окна браузера в строке заголовка плоскости.
     */
    private void drawTitleText(MatrixStack matrices, float pw, String title) {
        matrices.push();
        matrices.translate(0f, 0f, 1f); // чуть перед плоскостью

        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        Matrix4f m = matrices.peek().getPositionMatrix();

        String text = tr.trimToWidth(title, (int) (pw - 70f));
        tr.draw(text, 54f, 9f, 0xFFC8CDD7,
                false, m, vcp, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        vcp.draw();
        matrices.pop();
    }

    /**
     * Заглушка, когда окно браузера ещё не найдено.
     */
    private void drawPlaceholder(MatrixStack matrices, float pw, float ph, BrowserCapture capture) {
        matrices.push();
        matrices.translate(0f, 0f, 1f);

        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        Matrix4f m = matrices.peek().getPositionMatrix();
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        TextRenderer.TextLayerType layer = TextRenderer.TextLayerType.NORMAL;

        String status = capture == null ? null : capture.getSearchStatus();
        String first = status == null || status.isBlank() ? "Браузер не найден" : status;
        first = tr.trimToWidth(first, (int) (pw - 20f));
        String[] lines = {
                first,
                "",
                "Откройте окно выбранного браузера —",
                "его экран появится здесь автоматически.",
                "",
                "Браузер выбирается настройкой \"Браузер\" в модуле."
        };

        float y = TITLE_BAR + (ph - TITLE_BAR) / 2f - lines.length * 6f;
        for (String line : lines) {
            float lw = tr.getWidth(line);
            tr.draw(line, pw / 2f - lw / 2f, y, 0xFFA9AFB9,
                    false, m, vcp, layer, 0, light);
            y += 12f;
        }

        vcp.draw();
        matrices.pop();
    }

    private static int withAlpha(Color color, int alpha) {
        return (color.getRGB() & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static void quad(BufferBuilder buffer, Matrix4f m,
                             float x1, float y1, float x2, float y2, float z, int rgba) {
        buffer.vertex(m, x1, y1, z).color(rgba);
        buffer.vertex(m, x2, y1, z).color(rgba);
        buffer.vertex(m, x2, y2, z).color(rgba);
        buffer.vertex(m, x1, y2, z).color(rgba);
    }
}
