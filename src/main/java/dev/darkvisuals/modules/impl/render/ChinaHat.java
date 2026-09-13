package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.FriendsManager;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
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
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ChinaHat extends Module implements ThemeManager.ThemeChangeListener {

    private final NumberSetting brimRadius = new NumberSetting("setting.brimRadius", 0.7f, 0.3f, 1.4f, 0.05f);
    private final BooleanSetting showFriends = new BooleanSetting("setting.showFriends", true);
    private final NumberSetting opacity = new NumberSetting("setting.opacity", 0.65f, 0.0f, 1.0f, 0.01f);

    private static final int FIXED_SEGMENTS = 96;
    private final ThemeManager themeManager;
    private Color currentColor;

    public ChinaHat() {
        super("ChinaHat", Category.Render, net.minecraft.client.resource.language.I18n.translate("module.chinahat.description"));
        getSettings().add(brimRadius);
        getSettings().add(showFriends);
        getSettings().add(opacity);
        this.themeManager = ThemeManager.getInstance();
        this.currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);
    }

      
    public BooleanSetting getShowFriends() {
        return showFriends;
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor = theme.getBackgroundColor();
    }

    @EventHandler
    public void onThemeChanged(dev.darkvisuals.client.events.impl.EventThemeChanged event) {
        this.currentColor = event.getTheme().getBackgroundColor();
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        float tickDelta = e.getTickDelta();

        MatrixStack matrices = e.getMatrices();
        for (PlayerEntity p : mc.world.getPlayers()) {
             
            if (p == mc.player && mc.options.getPerspective().isFirstPerson()) continue;

             
            if (p == mc.player) {
                renderHatForPlayer(matrices, p, tickDelta);
                continue;
            }

             
            if (FriendsManager.checkFriend(p.getGameProfile().getName()) && showFriends.getValue()) {
                renderHatForPlayer(matrices, p, tickDelta);
            }
        }
    }

    private void renderHatForPlayer(MatrixStack matrices, PlayerEntity player, float tickDelta) {
        Vec3d pos = player.getLerpedPos(tickDelta);

        matrices.push();

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float eyeHeight = player.getEyeHeight(player.getPose());
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);

        float bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        float basePivotY = eyeHeight - 0.15f;
        matrices.translate(0.0, basePivotY, 0.0);
        float headYawAbs = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);
        float netHeadYaw = headYawAbs - bodyYaw;
        float headPitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
         
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-netHeadYaw));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(headPitch));
        float baseToCrown = player.isInSneakingPose() ? 0.305f : 0.265f;
        float pitchRad = (float) Math.toRadians(MathHelper.clamp(headPitch, -90.0f, 90.0f));
        float cosPitch = MathHelper.clamp((float) Math.cos(pitchRad), 0.0f, 1.0f);
        float tiltFactor = 1.0f - cosPitch;  
        float upMul = headPitch < 0.0f ? 1.6f : 1.0f;  

        float clearance = 0.03f * cosPitch + (headPitch < 0.0f ? 0.008f * tiltFactor : 0.0f);
        float dynamicOffset = -0.05f + 0.04f + 0.08f * tiltFactor * upMul;
        float forwardOffset = -0.06f * tiltFactor * Math.signum(headPitch) * upMul;
        matrices.translate(0.0, (baseToCrown + clearance) + dynamicOffset, forwardOffset);

        renderConeHatHollow(matrices,
                brimRadius.getValue().floatValue(),
                0.35f,
                ThemeManager.getInstance().getCurrentTheme().getBackgroundColor(),
                FIXED_SEGMENTS);

        matrices.pop();
    }

    private void renderConeHatHollow(MatrixStack matrices, float radius, float height, Color color, int segs) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDisable(GL11.GL_CULL_FACE);

        Matrix4f m = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        float tipY = height;
        BufferBuilder coneStrip = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        float tipAlpha = MathHelper.clamp(opacity.getValue().floatValue(), 0.0f, 1.0f);
        float baseAlpha = MathHelper.clamp(tipAlpha * 0.7f, 0.0f, 1.0f);
        for (int i = 0; i <= segs; i++) {
            double a = (i / (double) segs) * Math.PI * 2.0;
            float x = (float) (Math.cos(a) * radius);
            float z = (float) (Math.sin(a) * radius);
            coneStrip.vertex(m, x, 0f, z).color(r, g, b, baseAlpha);
            coneStrip.vertex(m, 0f, tipY, 0f).color(r, g, b, tipAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(coneStrip.end());

        GL11.glEnable(GL11.GL_CULL_FACE);
    }
}
