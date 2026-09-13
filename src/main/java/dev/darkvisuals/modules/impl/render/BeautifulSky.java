package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.Random;

 
public class BeautifulSky extends Module {

    private final BooleanSetting stars = new BooleanSetting("Stars (End)", true);
    private final NumberSetting starCount = new NumberSetting("Star Count", 800f, 100f, 2500f, 50f);
    private final NumberSetting domeRadius = new NumberSetting("Dome Radius", 180f, 60f, 400f, 10f);

    private static final int DOME_RINGS = 24;
    private static final int DOME_SEGMENTS = 48;

     
    private float[] starTheta;
    private float[] starPhi;
    private float[] starSeed;

    public BeautifulSky() {
        super("BeautifulSky", Category.Render, "Красивое процедурное небо в Энде и Аду без ресурс-пака");
        getSettings().add(stars);
        getSettings().add(starCount);
        getSettings().add(domeRadius);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        regenerateStars();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (!isToggled() || fullNullCheck()) return;

        RegistryKey<World> dim = mc.world.getRegistryKey();
        boolean isEnd = dim == World.END;
        boolean isNether = dim == World.NETHER;
        if (!isEnd && !isNether) return;  

        if (starTheta == null || starTheta.length != (int) (float) starCount.getValue()) {
            regenerateStars();
        }

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();

        float radius = domeRadius.getValue();

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
         
         
        matrices.translate(0, 0, 0);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (isEnd) {
            renderGradientDome(matrix, radius, 18, 8, 28, 70, 30, 90);
            if (stars.getValue()) renderStars(matrix, radius * 0.98f);
        } else {
            renderGradientDome(matrix, radius, 25, 6, 4, 140, 45, 20);
        }

        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private void regenerateStars() {
        int count = (int) (float) starCount.getValue();
        starTheta = new float[count];
        starPhi = new float[count];
        starSeed = new float[count];
        Random rnd = new Random(1337L);  
        for (int i = 0; i < count; i++) {
            starTheta[i] = rnd.nextFloat() * (float) (Math.PI * 2.0);
             
            starPhi[i] = (float) Math.acos(1.0f - rnd.nextFloat() * 1.0f) * 0.92f;
            starSeed[i] = rnd.nextFloat() * 1000f;
        }
    }

      


 
    private void renderGradientDome(Matrix4f matrix, float radius,
                                    int zr, int zg, int zb,
                                    int hr, int hg, int hb) {
        Tessellator tessellator = Tessellator.getInstance();

        for (int ring = 0; ring < DOME_RINGS; ring++) {
            float f0 = ring / (float) DOME_RINGS;
            float f1 = (ring + 1) / (float) DOME_RINGS;

            float phi0 = f0 * (float) (Math.PI / 2.0);
            float phi1 = f1 * (float) (Math.PI / 2.0);

            float y0 = MathHelper.sin(phi0) * radius;
            float y1 = MathHelper.sin(phi1) * radius;
            float rr0 = MathHelper.cos(phi0) * radius;
            float rr1 = MathHelper.cos(phi1) * radius;

            int r0 = lerpC(hr, zr, f0), g0 = lerpC(hg, zg, f0), b0 = lerpC(hb, zb, f0);
            int r1 = lerpC(hr, zr, f1), g1 = lerpC(hg, zg, f1), b1 = lerpC(hb, zb, f1);

            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int seg = 0; seg <= DOME_SEGMENTS; seg++) {
                float angle = (float) (seg * (Math.PI * 2.0) / DOME_SEGMENTS);
                float cos = MathHelper.cos(angle), sin = MathHelper.sin(angle);

                buffer.vertex(matrix, cos * rr0, y0, sin * rr0).color(r0, g0, b0, 255);
                buffer.vertex(matrix, cos * rr1, y1, sin * rr1).color(r1, g1, b1, 255);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private void renderStars(Matrix4f matrix, float radius) {
        long now = System.currentTimeMillis();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < starTheta.length; i++) {
            float theta = starTheta[i];
            float phi = starPhi[i];

            float y = MathHelper.cos(phi) * radius;
            float rr = MathHelper.sin(phi) * radius;
            float cx = MathHelper.cos(theta) * rr;
            float cz = MathHelper.sin(theta) * rr;

             
            float twinkle = 0.55f + 0.45f * (float) Math.sin(now / 450.0 + starSeed[i]);
            int a = (int) (255 * MathHelper.clamp(twinkle, 0f, 1f));
            if (a <= 3) continue;

            float size = radius * 0.006f;
             
            buffer.vertex(matrix, cx - size, y - size, cz).color(255, 255, 255, a);
            buffer.vertex(matrix, cx + size, y - size, cz).color(255, 255, 255, a);
            buffer.vertex(matrix, cx + size, y + size, cz).color(255, 255, 255, a);
            buffer.vertex(matrix, cx - size, y + size, cz).color(255, 255, 255, a);
        }

        BuiltBuffer built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private static int lerpC(int a, int b, float f) {
        return MathHelper.clamp((int) MathHelper.lerp(f, a, b), 0, 255);
    }
}