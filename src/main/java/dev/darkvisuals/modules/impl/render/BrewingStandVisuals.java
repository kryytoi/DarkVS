package dev.darkvisuals.modules.impl.render;

 

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrewingStandVisuals extends Module {

     
     
     
    private final NumberSetting uiScale       = new NumberSetting("Размер", 1.0f, 0.5f, 2.0f, 0.05f);
    private final NumberSetting uiHeight      = new NumberSetting("Высота", 1.25f, 0.6f, 2.5f, 0.05f);
    private final NumberSetting orbitSpeed    = new NumberSetting("Орбитальная скорость", 1.0f, 0.0f, 3.0f, 0.1f);
    private final NumberSetting levitation    = new NumberSetting("Левитация", 1.0f, 0.0f, 2.0f, 0.1f);
    private final BooleanSetting showText     = new BooleanSetting("Текст прогресса", true);
    private final BooleanSetting showOrbit    = new BooleanSetting("Орбитальные Партиклы", true);
    private final BooleanSetting showIdle     = new BooleanSetting("Показывать при бездействии", true);

     
     
     
    private static final int   MAX_BREW_TIME   = 400;     
    private static final float PIXEL           = 0.0125f;  

    private static final float RING_R_IN       = 24f;    
    private static final float RING_R_OUT      = 28f;
    private static final float SLOT_ORBIT_R    = 47f;    
    private static final float SLOT_BG_R       = 12.5f;  
    private static final float PARTICLE_R      = 36f;    
    private static final float HEART_HALF      = 8f;     
    private static final float TEXT_Y          = -82f;   

     
    private static final float ANGLE_INGREDIENT = 0f;
    private static final float[] ANGLE_POTIONS  = { 230f, 180f, 130f };  

    private static final Color COL_ACCENT   = new Color(255, 59, 75);    
    private static final Color COL_ACCENT_2 = new Color(255, 120, 130);  
    private static final Color COL_DISC     = new Color(14, 14, 20, 135);
    private static final Color COL_SLOT_BG  = new Color(14, 14, 20, 160);
    private static final Color COL_TRACK    = new Color(255, 255, 255, 34);
    private static final Color COL_SLOT_RIM = new Color(255, 255, 255, 48);
    private static final Color COL_PARTICLE = new Color(255, 255, 255, 165);

    private static final Identifier HEART_TEXTURE =
            Identifier.ofVanilla("textures/gui/sprites/hud/heart/full.png");


    private final Map<BlockPos, BrewSession> sessions = new ConcurrentHashMap<>();

    public BrewingStandVisuals() {
        super("BrewingStandVisuals", Category.Render, "Плавающий анимированный интерфейс варочной стойки");
        getSettings().add(uiScale);
        getSettings().add(uiHeight);
        getSettings().add(orbitSpeed);
        getSettings().add(levitation);
        getSettings().add(showText);
        getSettings().add(showOrbit);
        getSettings().add(showIdle);
    }

    @Override
    public void onDisable() {
        sessions.clear();
        super.onDisable();
    }

     
     
     

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) {
            sessions.clear();
            return;
        }

        if (mc.player.currentScreenHandler instanceof BrewingStandScreenHandler handler) {
             
            BlockPos pos = resolveStandPos();
            if (pos != null) {
                BrewSession s = sessions.computeIfAbsent(pos.toImmutable(), BrewSession::new);
                boolean wasBrewing = s.brewTime > 0;

                for (int i = 0; i < 3; i++) s.potions[i] = handler.getSlot(i).getStack().copy();
                s.ingredient = handler.getSlot(3).getStack().copy();
                s.brewTime   = handler.getBrewTime();
                s.fuel       = handler.getFuel();

                if (wasBrewing && s.brewTime <= 0) s.finishTime = System.currentTimeMillis();
            }
        } else {

            for (BrewSession s : sessions.values()) {
                if (s.brewTime > 0 && !s.ingredient.isEmpty()) {
                    s.brewTime--;
                    if (s.brewTime <= 0) {
                        s.ingredient.decrement(1);
                        s.finishTime = System.currentTimeMillis();
                    }
                }
            }
        }

         
        sessions.entrySet().removeIf(en ->
                !(mc.world.getBlockState(en.getKey()).getBlock() instanceof BrewingStandBlock)
                        || mc.player.squaredDistanceTo(Vec3d.ofCenter(en.getKey())) > 64.0 * 64.0);
    }

      
    private BlockPos resolveStandPos() {
        if (mc.crosshairTarget instanceof BlockHitResult bhr) {
            BlockPos p = bhr.getBlockPos();
            if (mc.world.getBlockState(p).getBlock() instanceof BrewingStandBlock) return p;
        }
        BlockPos best = null;
        double bestDist = 36.0;  
        for (BlockPos p : BlockPos.iterateOutwards(mc.player.getBlockPos(), 5, 4, 5)) {
            if (mc.world.getBlockState(p).getBlock() instanceof BrewingStandBlock) {
                double d = mc.player.squaredDistanceTo(Vec3d.ofCenter(p));
                if (d < bestDist) {
                    bestDist = d;
                    best = p.toImmutable();
                }
            }
        }
        return best;
    }

     
     
     

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck() || sessions.isEmpty()) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();
        long now = System.currentTimeMillis();
        float time = (now % 1_000_000L) / 1000f;  

        for (BrewSession s : sessions.values()) {
            boolean brewing = s.brewTime > 0;
            boolean hasItems = !s.ingredient.isEmpty() || !s.potions[0].isEmpty()
                    || !s.potions[1].isEmpty() || !s.potions[2].isEmpty();
            if (!brewing && !(showIdle.getValue() && hasItems)) continue;

            Vec3d base = Vec3d.ofCenter(s.pos);
            if (cam.squaredDistanceTo(base) > 48.0 * 48.0) continue;

            renderSession(e.getMatrices(), camera, cam, s, time, now);
        }
    }

    private void renderSession(MatrixStack matrices, Camera camera, Vec3d cam,
                               BrewSession s, float time, long now) {
        float sc = uiScale.getValue().floatValue();
        float bobAmp = levitation.getValue().floatValue();

         
        float appear = MathHelper.clamp((now - s.createdAt) / 300f, 0f, 1f);
        appear = 1f - (1f - appear) * (1f - appear);

         
        float target = s.brewTime > 0 ? (MAX_BREW_TIME - s.brewTime) / (float) MAX_BREW_TIME : 0f;
        float dt = MathHelper.clamp((now - s.lastFrame) / 1000f, 0f, 0.1f);
        s.lastFrame = now;
        s.displayProgress += (target - s.displayProgress) * Math.min(1f, dt * 12f);
        if (Math.abs(target - s.displayProgress) < 0.0005f) s.displayProgress = target;

         
        float phase = (s.pos.getX() * 31 + s.pos.getZ() * 17) % 7;
        float hover = MathHelper.sin(time * 1.15f + phase) * 0.035f * bobAmp;

        double x = s.pos.getX() + 0.5 - cam.x;
        double y = s.pos.getY() + uiHeight.getValue().floatValue() + hover - cam.y;
        double z = s.pos.getZ() + 0.5 - cam.z;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(camera.getRotation());           
        matrices.scale(PIXEL * sc, -PIXEL * sc, PIXEL * sc);  
        matrices.scale(appear, appear, appear);

        setup2D();

         
        drawDisc(matrices, 0f, 0f, RING_R_OUT + 3f, withAlpha(COL_DISC, appear));
        drawArc(matrices, 0f, 0f, RING_R_IN, RING_R_OUT, 0f, 360f, withAlpha(COL_TRACK, appear));

         
        float sweep = s.displayProgress * 360f;
        if (sweep > 0.5f) {
            drawArc(matrices, 0f, 0f, RING_R_IN, RING_R_OUT, 0f, sweep, withAlpha(COL_ACCENT, appear));
             
            float tipRad = (float) Math.toRadians(sweep);
            float midR = (RING_R_IN + RING_R_OUT) * 0.5f;
            float tx = MathHelper.sin(tipRad) * midR;
            float ty = -MathHelper.cos(tipRad) * midR;
            drawDisc(matrices, tx, ty, 6.5f, withAlpha(new Color(255, 59, 75, 70), appear));
            drawDisc(matrices, tx, ty, 3.4f, withAlpha(COL_ACCENT_2, appear));
        }

         
        if (s.finishTime > 0) {
            float ft = (now - s.finishTime) / 600f;
            if (ft < 1f) {
                float fr = RING_R_OUT + 26f * easeOutCirc(ft);
                int fa = (int) (170 * (1f - ft) * appear);
                drawArc(matrices, 0f, 0f, fr - 1.6f, fr + 1.6f, 0f, 360f,
                        new Color(255, 59, 75, Math.max(0, fa)));
            } else {
                s.finishTime = 0;
            }
        }

         
        if (showOrbit.getValue()) {
            float speed = orbitSpeed.getValue().floatValue();
            for (int i = 0; i < 3; i++) {  
                float a = (float) Math.toRadians(time * 80f * speed + i * 120f);
                drawDisc(matrices, MathHelper.sin(a) * PARTICLE_R, -MathHelper.cos(a) * PARTICLE_R,
                        2.1f, withAlpha(COL_PARTICLE, appear));
            }
            for (int i = 0; i < 3; i++) {  
                float a = (float) Math.toRadians(-time * 55f * speed + 60f + i * 120f);
                drawDisc(matrices, MathHelper.sin(a) * (PARTICLE_R - 4.5f), -MathHelper.cos(a) * (PARTICLE_R - 4.5f),
                        1.5f, withAlpha(new Color(255, 59, 75, 150), appear));
            }
        }

         
        float pulse = s.brewTime > 0 ? 1f + 0.09f * MathHelper.sin(time * 6.5f) : 1f;
        drawTexturedQuad(matrices, HEART_TEXTURE, 0f, 0f, HEART_HALF * pulse,
                withAlpha(new Color(255, 255, 255, 255), appear));

         
        float[] slotX = new float[4];
        float[] slotY = new float[4];
        float bobFreq = 2.35f;
         
        computeSlot(slotX, slotY, 0, ANGLE_INGREDIENT,
                MathHelper.sin(time * bobFreq) * 2.6f * bobAmp);
         
        for (int i = 0; i < 3; i++) {
            computeSlot(slotX, slotY, i + 1, ANGLE_POTIONS[i],
                    MathHelper.sin(time * bobFreq + 1.7f * (i + 1)) * 2.6f * bobAmp);
        }
        for (int i = 0; i < 4; i++) {
            drawDisc(matrices, slotX[i], slotY[i], SLOT_BG_R, withAlpha(COL_SLOT_BG, appear));
            boolean highlight = s.brewTime > 0 && (i == 0 ? !s.ingredient.isEmpty() : !s.potions[i - 1].isEmpty());
            Color rim = highlight ? new Color(255, 59, 75, 130) : COL_SLOT_RIM;
            drawArc(matrices, slotX[i], slotY[i], SLOT_BG_R - 1.1f, SLOT_BG_R, 0f, 360f, withAlpha(rim, appear));
        }

        cleanup2D();

         
        drawSlotItem(matrices, s.ingredient, slotX[0], slotY[0]);
        for (int i = 0; i < 3; i++) {
            drawSlotItem(matrices, s.potions[i], slotX[i + 1], slotY[i + 1]);
        }

         
        if (showText.getValue() && s.brewTime > 0) {
            int pct = Math.round(s.displayProgress * 100f);
            float secondsLeft = s.brewTime / 20f;
            String text = String.format("%d%% - %.1fс", pct, secondsLeft).replace('.', ',');
            drawCenteredText(matrices, text, TEXT_Y, appear);
        }

        matrices.pop();
    }

      
    private void computeSlot(float[] xs, float[] ys, int idx, float angleDeg, float bob) {
        float rad = (float) Math.toRadians(angleDeg);
        xs[idx] = MathHelper.sin(rad) * SLOT_ORBIT_R;
        ys[idx] = -MathHelper.cos(rad) * SLOT_ORBIT_R + bob;
    }

     
     
     

    private void setup2D() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
    }

    private void cleanup2D() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

      
    private void drawArc(MatrixStack matrices, float cx, float cy,
                         float rIn, float rOut, float startDeg, float sweepDeg, Color color) {
        if (sweepDeg <= 0f || color.getAlpha() <= 0) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f m = matrices.peek().getPositionMatrix();
        int segs = Math.max(6, (int) (sweepDeg / 4f));
        float r = color.getRed() / 255f, g = color.getGreen() / 255f,
                b = color.getBlue() / 255f, a = color.getAlpha() / 255f;

        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segs; i++) {
            float ang = (float) Math.toRadians(startDeg + sweepDeg * i / segs);
            float sx = MathHelper.sin(ang), sy = -MathHelper.cos(ang);
            buf.vertex(m, cx + sx * rIn, cy + sy * rIn, 0f).color(r, g, b, a);
            buf.vertex(m, cx + sx * rOut, cy + sy * rOut, 0f).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

      
    private void drawDisc(MatrixStack matrices, float cx, float cy, float radius, Color color) {
        if (color.getAlpha() <= 0) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f m = matrices.peek().getPositionMatrix();
        int segs = Math.max(10, (int) (radius * 2.2f));
        float r = color.getRed() / 255f, g = color.getGreen() / 255f,
                b = color.getBlue() / 255f, a = color.getAlpha() / 255f;

        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(m, cx, cy, 0f).color(r, g, b, a);
        for (int i = 0; i <= segs; i++) {
            float ang = (float) (Math.PI * 2.0 * i / segs);
            buf.vertex(m, cx + MathHelper.sin(ang) * radius, cy - MathHelper.cos(ang) * radius, 0f)
                    .color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

      
    private void drawTexturedQuad(MatrixStack matrices, Identifier texture,
                                  float cx, float cy, float half, Color color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        Matrix4f m = matrices.peek().getPositionMatrix();
        float r = color.getRed() / 255f, g = color.getGreen() / 255f,
                b = color.getBlue() / 255f, a = color.getAlpha() / 255f;

        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(m, cx - half, cy - half, 0.5f).texture(0f, 0f).color(r, g, b, a);
        buf.vertex(m, cx - half, cy + half, 0.5f).texture(0f, 1f).color(r, g, b, a);
        buf.vertex(m, cx + half, cy + half, 0.5f).texture(1f, 1f).color(r, g, b, a);
        buf.vertex(m, cx + half, cy - half, 0.5f).texture(1f, 0f).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

      
    private void drawSlotItem(MatrixStack matrices, ItemStack stack, float px, float py) {
        if (stack.isEmpty()) return;
        matrices.push();
        matrices.translate(px, py, 3f);        
        matrices.scale(16f, -16f, 16f);         

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        mc.getItemRenderer().renderItem(
                stack, ModelTransformationMode.GUI,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV,
                matrices, immediate, mc.world, 0);
        immediate.draw();

        matrices.pop();
    }

      
    private void drawCenteredText(MatrixStack matrices, String text, float yPx, float appear) {
        TextRenderer tr = mc.textRenderer;
        float width = tr.getWidth(text);

        matrices.push();
        matrices.translate(0f, yPx, 4f);
        Matrix4f m = matrices.peek().getPositionMatrix();

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        int alpha = (int) (255 * appear);
        int color = (Math.max(4, alpha) << 24) | 0xFFFFFF;
        int bg = ((int) (70 * appear)) << 24;
        tr.draw(text, -width / 2f, 0f, color, false, m, immediate,
                TextRenderer.TextLayerType.SEE_THROUGH, bg, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        immediate.draw();

        matrices.pop();
    }

     
     
     

    private static Color withAlpha(Color c, float mul) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                MathHelper.clamp((int) (c.getAlpha() * mul), 0, 255));
    }

    private static float easeOutCirc(float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        return (float) Math.sqrt(1.0 - (t - 1.0) * (t - 1.0));
    }

     
     
     

    private static final class BrewSession {
        final BlockPos pos;
        final long createdAt = System.currentTimeMillis();

        ItemStack ingredient = ItemStack.EMPTY;
        final ItemStack[] potions = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };

        int brewTime = 0;    
        int fuel = 0;

        float displayProgress = 0f;   
        long lastFrame = System.currentTimeMillis();
        long finishTime = 0;          

        BrewSession(BlockPos pos) {
            this.pos = pos;
        }
    }
}
