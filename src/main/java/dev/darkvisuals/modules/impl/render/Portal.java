package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

public class Portal extends Module {

    private final NumberSetting distance = new NumberSetting("Дистанция", 1f, 1f, 6f, 0.5f);
    private final StringSetting command = new StringSetting("Команда", "spawn", false);

    private BlockPos portalPos = null;
    private float portalYaw = 0f;
    private long lastUse = 0L;

    public Portal() {
        super("Portal", Category.Render, I18n.translate("module.portal.description"));
        getSettings().add(distance);
        getSettings().add(command);
    }

    @Override
    public void onDisable() {
        portalPos = null;
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (mc.player == null) return;

        // Портал фиксируется на момент активации и не следует за игроком,
        // иначе до него невозможно дойти.
        if (portalPos == null) {
            Vec3d look = mc.player.getRotationVector();
            double dx = look.x * distance.getValue();
            double dz = look.z * distance.getValue();

            // Направление вдоль горизонтали без потери шага на дистанциях > 1.
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > 1e-6) {
                dx = dx / horizontal * distance.getValue();
                dz = dz / horizontal * distance.getValue();
            }

            Vec3d spawn = mc.player.getPos().add(dx, 0, dz);
            portalPos = BlockPos.ofFloored(spawn.getX(), mc.player.getY(), spawn.getZ());
            portalYaw = mc.player.getYaw();
        }

        if (mc.player.getBlockPos().equals(portalPos)) {
            if (System.currentTimeMillis() - lastUse > 1500L) {
                lastUse = System.currentTimeMillis();
                executeCommand();
            }
        }
    }

    private void executeCommand() {
        String cmd = command.getValue();
        if (cmd == null || cmd.isEmpty()) return;
        if (mc.player.networkHandler == null) return;
        String text = cmd.trim();
        if (text.startsWith("/")) text = text.substring(1);
        if (text.isEmpty()) return;
        mc.player.networkHandler.sendChatCommand(text);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (portalPos == null || fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        double x = portalPos.getX() + 0.5 - cam.x;
        double y = portalPos.getY() - cam.y;
        double z = portalPos.getZ() + 0.5 - cam.z;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
        matrices.translate((float) x, (float) y, (float) z);
        // Разворот портала к игроку в момент его создания.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-portalYaw));

        float t = System.currentTimeMillis() / 1000f;
        Color c = new Color(138, 43, 226);
        drawPortal(matrices, t, c, 1f);
        drawPortal(matrices, t * 1.3f + 1.7f, c, 0.4f);

        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawPortal(MatrixStack matrices, float time, Color c, float intensity) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int segments = 24;
        float height = 2f;
        float halfW = 0.45f + 0.05f * (float) Math.sin(time * 2f);

        int alpha = (int) (140 * intensity);
        for (int i = 0; i < segments; i++) {
            float y0 = height * i / segments;
            float y1 = height * (i + 1) / segments;
            float w0 = halfW * (1f - y0 / height * 0.35f);
            float w1 = halfW * (1f - y1 / height * 0.35f);
            float wave = 0.06f * (float) Math.sin(time * 3f + i * 0.9f);

            buffer.vertex(matrix, -w0 + wave, y0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix,  w0 + wave, y0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix,  w1 - wave, y1, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, -w1 - wave, y1, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    @SuppressWarnings("unused")
    private Direction facing() {
        return mc.player.getHorizontalFacing();
    }
}
