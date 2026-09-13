package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

 
public class SkyShader extends Module {

    private final EnumSetting<SkyShaderMode> mode =
            new EnumSetting<>("Режим", SkyShaderMode.SKY);

    private final NumberSetting speed =
            new NumberSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);

    private final NumberSetting scale =
            new NumberSetting("Размер", 5.0f, 1.0f, 20.0f, 0.5f);

    private final NumberSetting intensity =
            new NumberSetting("Интенсивность", 0.01f, 0.001f, 0.05f, 0.001f);

    private final NumberSetting alpha =
            new NumberSetting("Прозрачность", 1.0f, 0.0f, 1.0f, 0.05f);

    private static final ShaderProgramKey SKY_SHADER = new ShaderProgramKey(
            Identifier.of("darkvisuals", "core/post/sky/sky"),
            VertexFormats.POSITION, Defines.EMPTY);

    private static final ShaderProgramKey WATER_SHADER = new ShaderProgramKey(
            Identifier.of("darkvisuals", "core/post/sky/water"),
            VertexFormats.POSITION, Defines.EMPTY);

    private static final ShaderProgramKey CAUSTIC_SHADER = new ShaderProgramKey(
            Identifier.of("darkvisuals", "core/post/sky/caustic"),
            VertexFormats.POSITION, Defines.EMPTY);

    private static final ShaderProgramKey AURORA_SHADER = new ShaderProgramKey(
            Identifier.of("darkvisuals", "core/post/sky/aurora"),
            VertexFormats.POSITION, Defines.EMPTY);

    private static final ShaderProgramKey NEBULA_SHADER = new ShaderProgramKey(
            Identifier.of("darkvisuals", "core/post/sky/nebula"),
            VertexFormats.POSITION, Defines.EMPTY);

    private static final ShaderProgramKey VORTEX_SHADER = new ShaderProgramKey(
            Identifier.of("darkvisuals", "core/post/sky/vortex"),
            VertexFormats.POSITION, Defines.EMPTY);

    private static final ShaderProgramKey RAINBOW_SHADER = new ShaderProgramKey(
            Identifier.of("darkvisuals", "core/post/sky/rainbow"),
            VertexFormats.POSITION, Defines.EMPTY);

    private static final ShaderProgramKey STARS_SHADER = new ShaderProgramKey(
            Identifier.of("darkvisuals", "core/post/sky/stars"),
            VertexFormats.POSITION, Defines.EMPTY);

    private long startMillis = -1;

    public SkyShader() {
        super("SkyShader", Category.Render, "Шейдерное небо с эффектами Sky, Water, Caustic, Aurora, Nebula, Vortex, Rainbow и Stars");
        getSettings().add(mode);
        getSettings().add(speed);
        getSettings().add(scale);
        getSettings().add(intensity);
        getSettings().add(alpha);
    }

    @Override
    public void onDisable() {
        startMillis = -1;
        super.onDisable();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (!isToggled() || fullNullCheck()) return;
        renderSkyShader();
    }

    private void renderSkyShader() {
        if (startMillis < 0) startMillis = System.currentTimeMillis();

        float time = (System.currentTimeMillis() - startMillis) / 1000.0f;
        float fw = mc.getWindow().getFramebufferWidth();
        float fh = mc.getWindow().getFramebufferHeight();

        Color themeColor = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        float cr = themeColor.getRed() / 255f;
        float cg = themeColor.getGreen() / 255f;
        float cb = themeColor.getBlue() / 255f;

        ShaderProgramKey key;
        SkyShaderMode m = mode.getValue();
        key = switch (m) {
            case SKY -> SKY_SHADER;
            case WATER -> WATER_SHADER;
            case CAUSTIC -> CAUSTIC_SHADER;
            case AURORA -> AURORA_SHADER;
            case NEBULA -> NEBULA_SHADER;
            case VORTEX -> VORTEX_SHADER;
            case RAINBOW -> RAINBOW_SHADER;
            case STARS -> STARS_SHADER;
        };

        ShaderProgram shader = RenderSystem.setShader(key);
        if (shader == null) return;

         
        shader.getUniform("uTime").set(time);
        shader.getUniform("uResolution").set(fw, fh);
        shader.getUniform("uColor").set(cr, cg, cb);
        shader.getUniform("uAlpha").set(alpha.getValue());
        shader.getUniform("uSpeed").set(speed.getValue());
        shader.getUniform("uScale").set(scale.getValue());
        shader.getUniform("uIntensity").set(intensity.getValue());

        Camera cam = mc.gameRenderer.getCamera();
        float yawRad = (float) Math.toRadians(-cam.getYaw());
        float pitchRad = (float) Math.toRadians(cam.getPitch());
        shader.getUniform("uCameraDir").set(yawRad, pitchRad);
        shader.getUniform("uFov").set((float) mc.options.getFov().getValue().intValue());

         
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        RenderSystem.setProjectionMatrix(new Matrix4f(), ProjectionType.ORTHOGRAPHIC);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

         
         
        Matrix4f identity = new Matrix4f();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buf.vertex(identity, -1f, -1f, 1f);
        buf.vertex(identity, 1f, -1f, 1f);
        buf.vertex(identity, 1f, 1f, 1f);
        buf.vertex(identity, -1f, 1f, 1f);
        BufferRenderer.drawWithGlobalProgram(buf.end());

         
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setProjectionMatrix(savedProj, ProjectionType.PERSPECTIVE);
    }
}
