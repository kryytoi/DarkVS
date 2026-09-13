package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.events.impl.EventPacket;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventThemeChanged;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Nameable;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.world.LightType;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

 
public class KillEffect extends Module implements ThemeManager.ThemeChangeListener {

     

    public enum Mode implements Nameable {
        PULSE("Пульс"),
        BEAM("Луч"),
        RICK("Рик C-137"),
        ASCEND("Призрак");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getName() {
            return displayName;
        }
    }

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.PULSE);

     
    private final NumberSetting pulseTime = new NumberSetting("Время пульса", 0.5f, 0.3f, 1.5f, 0.05f);
    private final NumberSetting pulseSize = new NumberSetting("Размер пульса", 2.8f, 1.0f, 6.0f, 0.1f);
    private final BooleanSetting blockOutline = new BooleanSetting("Обводка блоков В пульсе", true);
    private final NumberSetting outlineWidth = new NumberSetting("Размер обводки", 2.5f, 1.0f, 5.0f, 0.5f);

     
    private final NumberSetting lifeTime = new NumberSetting("Время жизни луча", 1.2f, 0.4f, 3.0f, 0.1f);
    private final NumberSetting beamHeight = new NumberSetting("Высота луча", 64f, 16f, 160f, 4f);
    private final NumberSetting beamRadius = new NumberSetting("Радиус луча", 0.45f, 0.15f, 1.5f, 0.05f);
    private final BooleanSetting explosionParticles = new BooleanSetting("Партиклы луча", true);

     
    private final NumberSetting rickDuration = new NumberSetting("Продолжительность рика", 2.5f, 1.8f, 4.0f, 0.1f);
    private final BooleanSetting rickParticles = new BooleanSetting("Партиклы Рика", true);
     
    private final NumberSetting ascendDuration = new NumberSetting("Время жизни Призрака", 4.0f, 2.0f, 10.0f, 0.1f);
    private final NumberSetting ascendHeight   = new NumberSetting("Высота призрака", 2.5f, 0.5f, 10f, 0.1f);

     

      
    private static final long KILL_ATTRIBUTION_WINDOW_MS = 4_000L;
      
    private static final long REMOVAL_ATTRIBUTION_WINDOW_MS = 1_200L;

    private static final int RING_SEGMENTS = 96;
    private static final int BEAM_SEGMENTS = 32;

      
    private static final float RING_BASE_Y = 0.05f;
      
    private static final float RING_BORDER_THICKNESS = 0.45f;
      
    private static final float RING_OUTER_FALLOFF = 0.14f;
      
    private static final float EXPANSION_PORTION = 0.85f;
      
    private static final float BLOCK_REVEAL_WIDTH = 0.55f;
      
    private static final float BLOCK_HIGHLIGHT_SIGMA = 0.5f;
      
    private static final int MAX_SURFACE_BLOCKS = 256;

     

      
    private static final float RICK_SPAWN_DISTANCE = 1.5f;
      
    private static final float RICK_PORTAL_RADIUS = 1.05f;
      
    private static final float RICK_FLOOR_PORTAL_RADIUS = 0.85f;
      
    private static final float RICK_PORTAL_CENTER_Y = 0.98f;
      
    private static final float RICK_EMERGE_DISTANCE = 0.9f;
      
    private static final float RICK_EXIT_DISTANCE = 1.05f;
      
    private static final float PORTAL_SPIN_DEG_PER_SEC = 260f;
      
    private static final int PORTAL_ARMS = 3;
      
    private static final int PORTAL_ARM_STEPS = 26;

     
     
     
    private static final float RK_PORTAL_A_IN_START  = 0.00f, RK_PORTAL_A_IN_END  = 0.12f;
    private static final float RK_EMERGE_START       = 0.10f, RK_EMERGE_END       = 0.28f;
    private static final float RK_PORTAL_A_OUT_START = 0.26f, RK_PORTAL_A_OUT_END = 0.36f;
    private static final float RK_ARM_RAISE_START    = 0.30f, RK_ARM_RAISE_END    = 0.40f;
    private static final float RK_PORTAL_B_IN_START  = 0.34f, RK_PORTAL_B_IN_END  = 0.44f;
    private static final float RK_SINK_START         = 0.42f, RK_SINK_END         = 0.72f;
    private static final float RK_ARM_LOWER_START    = 0.58f, RK_ARM_LOWER_END    = 0.66f;
    private static final float RK_TURN_START         = 0.64f, RK_TURN_END         = 0.74f;
    private static final float RK_PORTAL_C_IN_START  = 0.66f, RK_PORTAL_C_IN_END  = 0.76f;
    private static final float RK_PORTAL_B_OUT_START = 0.72f, RK_PORTAL_B_OUT_END = 0.82f;
    private static final float RK_EXIT_START         = 0.74f, RK_EXIT_END         = 0.90f;
    private static final float RK_RICK_FADE_IN_END   = 0.20f;
    private static final float RK_RICK_FADE_OUT_START = 0.80f, RK_RICK_FADE_OUT_END = 0.90f;
    private static final float RK_PORTAL_C_OUT_START = 0.88f, RK_PORTAL_C_OUT_END  = 1.00f;

     
    private static final int PORTAL_CORE_R = 200, PORTAL_CORE_G = 255, PORTAL_CORE_B = 185;
    private static final int PORTAL_MID_R  = 60,  PORTAL_MID_G  = 210, PORTAL_MID_B  = 70;
    private static final int PORTAL_EDGE_R = 12,  PORTAL_EDGE_G = 105, PORTAL_EDGE_B = 35;
    private static final int PORTAL_ARM_R  = 145, PORTAL_ARM_G  = 255, PORTAL_ARM_B  = 90;
    private static final int PORTAL_RIM_R  = 120, PORTAL_RIM_G  = 255, PORTAL_RIM_B  = 75;

     
    private static final int[] C_HAIR   = {166, 216, 231};  
    private static final int[] C_SKIN   = {228, 209, 168};  
    private static final int[] C_COAT   = {240, 241, 244};  
    private static final int[] C_SHIRT  = {99,  199, 196};  
    private static final int[] C_PANTS  = {139, 107, 62};   
    private static final int[] C_SHOES  = {28,  28,  32};   
    private static final int[] C_BELT   = {24,  24,  26};   
    private static final int[] C_BUCKLE = {232, 192, 46};   
    private static final int[] C_DARK   = {58,  50,  46};   
    private static final int[] C_EYES   = {248, 248, 246};
    private static final int[] C_GUN    = {122, 126, 133};
    private static final int[] C_GUN_GLOW = {110, 255, 110};

     

      
    private final Map<Integer, Long> recentTargets = new ConcurrentHashMap<>();
      
    private final Map<Integer, Vec3d> lastKnownPositions = new ConcurrentHashMap<>();

    private final List<Effect> effects = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    private final ThemeManager themeManager;
    private Color currentColor;

    public KillEffect() {
        super("KillEffect", Category.Render, "Визуальный эффект в точке гибели убитой сущности");
        getSettings().add(mode);
        getSettings().add(pulseTime);
        getSettings().add(pulseSize);
        getSettings().add(blockOutline);
        getSettings().add(outlineWidth);
        getSettings().add(lifeTime);
        getSettings().add(beamHeight);
        getSettings().add(beamRadius);
        getSettings().add(explosionParticles);
        getSettings().add(rickDuration);
        getSettings().add(rickParticles);
        getSettings().add(ascendDuration);
        getSettings().add(ascendHeight);

        this.themeManager = ThemeManager.getInstance();
        this.currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);
    }

     

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor = theme.getBackgroundColor();
    }

    @EventHandler
    public void onThemeChanged(EventThemeChanged event) {
        this.currentColor = event.getTheme().getBackgroundColor();
    }

    @Override
    public void onDisable() {
        recentTargets.clear();
        lastKnownPositions.clear();
        effects.clear();
        super.onDisable();
    }

     

    @EventHandler
    public void onAttackEntity(EventAttackEntity event) {
        if (fullNullCheck()) return;
        if (event.getPlayer() != mc.player) return;
        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity)) return;
        if (target == mc.player) return;
        if (!event.canProcess()) return;
        event.registerHit();

        recentTargets.put(target.getId(), System.currentTimeMillis());
        lastKnownPositions.put(target.getId(), target.getPos());
    }

 
    @EventHandler
    public void onPacketReceive(EventPacket.Receive event) {
        if (fullNullCheck()) return;
        if (!(event.getPacket() instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 3) return;  

        mc.execute(() -> {
            if (fullNullCheck()) return;
            Entity entity = packet.getEntity(mc.world);
            if (entity == null) return;
            Long hitTime = recentTargets.get(entity.getId());
            if (hitTime == null) return;
            if (System.currentTimeMillis() - hitTime > KILL_ATTRIBUTION_WINDOW_MS) {
                forget(entity.getId());
                return;
            }
            triggerKill(entity.getId(), entity.getPos(), entity);
        });
    }

 
    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        long now = System.currentTimeMillis();

        for (Map.Entry<Integer, Long> entry : recentTargets.entrySet()) {
            int entityId = entry.getKey();
            long hitTime = entry.getValue();

            if (now - hitTime > KILL_ATTRIBUTION_WINDOW_MS) {
                forget(entityId);
                continue;
            }

            Entity entity = mc.world.getEntityById(entityId);
            if (entity instanceof LivingEntity living) {
                lastKnownPositions.put(entityId, living.getPos());
                if (living.isDead() || living.getHealth() <= 0.0f) {
                    triggerKill(entityId, living.getPos(), living);
                }
            } else if (entity == null) {
                 
                if (now - hitTime <= REMOVAL_ATTRIBUTION_WINDOW_MS) {
                    Vec3d lastPos = lastKnownPositions.get(entityId);
                    if (lastPos != null) triggerKill(entityId, lastPos, null);
                    else forget(entityId);
                } else {
                    forget(entityId);
                }
            }
        }

         
        if (rickParticles.getValue()) {
            for (Effect effect : effects) {
                if (effect.mode == Mode.RICK && effect.rick != null) {
                    tickRickParticles(effect, now);
                }
            }
        }
    }

    private void forget(int entityId) {
        recentTargets.remove(entityId);
        lastKnownPositions.remove(entityId);
    }

    private void triggerKill(int entityId, Vec3d deathPos, Entity victim) {
        forget(entityId);
        Mode currentMode = mode.getValue();

        if (currentMode == Mode.PULSE) {
            float maxRadius = pulseSize.getValue();
            long ttl = (long) (pulseTime.getValue() * 1000L);
            List<SurfaceBlock> surface = blockOutline.getValue()
                    ? collectSurfaceBlocks(deathPos, maxRadius)
                    : List.of();
            effects.add(new Effect(deathPos, currentMode, ttl, surface));
        } else if (currentMode == Mode.BEAM) {
            long ttl = (long) (lifeTime.getValue() * 1000L);
            effects.add(new Effect(deathPos, currentMode, ttl, List.of()));
            if (explosionParticles.getValue()) {
                spawnDeathBurst(deathPos);
            }
        } else if (currentMode == Mode.ASCEND) {
            long ttl = (long) (ascendDuration.getValue() * 1000L);
            float w = victim != null ? victim.getWidth() : 0.6f;
            float h = victim != null ? victim.getHeight() : 1.8f;
            LivingEntity ghostEntity = victim instanceof LivingEntity le ? le : null;
            effects.add(new Effect(deathPos, currentMode, ttl, new AscendData(w, h, ghostEntity)));
        } else {  
            long ttl = (long) (rickDuration.getValue() * 1000L);
            effects.add(new Effect(deathPos, currentMode, ttl, buildRickData(deathPos, victim)));
        }
    }

 
    private List<SurfaceBlock> collectSurfaceBlocks(Vec3d origin, float maxRadius) {
        List<SurfaceBlock> result = new ArrayList<>();
        if (mc.world == null) return result;

        int range = MathHelper.ceil(maxRadius);
        BlockPos center = BlockPos.ofFloored(origin);
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        for (int dx = -range; dx <= range && result.size() < MAX_SURFACE_BLOCKS; dx++) {
            for (int dz = -range; dz <= range && result.size() < MAX_SURFACE_BLOCKS; dz++) {
                double columnX = center.getX() + dx + 0.5;
                double columnZ = center.getZ() + dz + 0.5;
                double horizontalDist = Math.hypot(columnX - origin.x, columnZ - origin.z);
                if (horizontalDist > maxRadius + 0.75) continue;

                 
                for (int dy = 2; dy >= -4; dy--) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = mc.world.getBlockState(cursor);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getOutlineShape(mc.world, cursor);
                    if (shape.isEmpty()) continue;

                     
                    BlockPos abovePos = cursor.up();
                    BlockState above = mc.world.getBlockState(abovePos);
                    if (!above.isAir() && !above.getOutlineShape(mc.world, abovePos).isEmpty()) continue;

                    BlockPos immutable = cursor.toImmutable();
                    List<Box> boxes = new ArrayList<>(1);
                    shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->
                            boxes.add(new Box(minX, minY, minZ, maxX, maxY, maxZ).offset(immutable).expand(0.0015)));
                    if (!boxes.isEmpty()) {
                        result.add(new SurfaceBlock(boxes, horizontalDist));
                    }
                    break;  
                }
            }
        }
        return result;
    }

      
    private void spawnDeathBurst(Vec3d pos) {
        if (mc.world == null) return;
        double cx = pos.x, cy = pos.y + 0.6, cz = pos.z;

        mc.world.addParticle(ParticleTypes.EXPLOSION, cx, cy, cz, 0.0, 0.0, 0.0);

        for (int i = 0; i < 28; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0;
            double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
            double speed = 0.15 + random.nextDouble() * 0.35;
            double vx = Math.sin(phi) * Math.cos(theta) * speed;
            double vy = Math.cos(phi) * speed + 0.1;
            double vz = Math.sin(phi) * Math.sin(theta) * speed;
            mc.world.addParticle(ParticleTypes.FIREWORK, cx, cy, cz, vx, vy, vz);
        }

        for (int i = 0; i < 12; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.8;
            double oy = random.nextDouble() * 1.2;
            double oz = (random.nextDouble() - 0.5) * 0.8;
            mc.world.addParticle(ParticleTypes.END_ROD, cx + ox, cy + oy, cz + oz,
                    (random.nextDouble() - 0.5) * 0.05, 0.05 + random.nextDouble() * 0.08, (random.nextDouble() - 0.5) * 0.05);
        }

        for (int i = 0; i < 10; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0;
            double r = 0.3 + random.nextDouble() * 0.5;
            mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    cx + Math.cos(theta) * r, cy + random.nextDouble() * 0.8, cz + Math.sin(theta) * r,
                    Math.cos(theta) * 0.1, 0.15, Math.sin(theta) * 0.1);
        }
    }

     

 
    private RickData buildRickData(Vec3d deathPos, Entity victim) {
         
        Vec3d toPlayer = mc.player != null
                ? new Vec3d(mc.player.getX() - deathPos.x, 0.0, mc.player.getZ() - deathPos.z)
                : Vec3d.ZERO;
        if (toPlayer.lengthSquared() < 1.0E-4) toPlayer = new Vec3d(1.0, 0.0, 0.0);
        toPlayer = toPlayer.normalize();

        Vec3d anchor = deathPos.add(toPlayer.multiply(RICK_SPAWN_DISTANCE));
        Vec3d facing = toPlayer.negate();                        
        Vec3d exitDir = new Vec3d(facing.z, 0.0, -facing.x);     

        float entityW = victim != null ? victim.getWidth() : 0.6f;
        float entityH = victim != null ? victim.getHeight() : 1.8f;

        return new RickData(anchor, facing, exitDir, entityW, entityH);
    }

     

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck() || effects.isEmpty()) return;

        long now = System.currentTimeMillis();
        effects.removeIf(effect -> now - effect.spawnTime > effect.ttl);
        if (effects.isEmpty()) return;

         
        Color themeColor = themeManager.getCurrentTheme().getBackgroundColor();
        this.currentColor = themeColor;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();

        setupRenderState();
        try {
            for (Effect effect : effects) {
                float t = MathHelper.clamp((now - effect.spawnTime) / (float) effect.ttl, 0f, 1f);

                if (effect.mode == Mode.PULSE) {
                    renderPulse(matrices, cam, effect, themeColor, t);
                } else if (effect.mode == Mode.BEAM) {
                    matrices.push();
                    matrices.translate(effect.origin.x - cam.x, effect.origin.y - cam.y, effect.origin.z - cam.z);
                    renderBeam(matrices.peek().getPositionMatrix(), themeColor, t);
                    matrices.pop();
                } else if (effect.mode == Mode.ASCEND && effect.ascend != null) {
                    renderAscend(matrices, cam, effect, t);
                } else if (effect.rick != null) {
                    renderRickEffect(matrices, cam, effect, now, t);
                }
            }
        } finally {
            restoreRenderState();
        }
    }

    private void setupRenderState() {
        RenderSystem.enableBlend();
         
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA.value, GlStateManager.DstFactor.ONE.value);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }

    private void restoreRenderState() {
        GL11.glLineWidth(1.0f);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

     

 
    private void renderPulse(MatrixStack matrices, Vec3d cam, Effect effect, Color color, float t) {
         
        float expansion = MathHelper.clamp(t / EXPANSION_PORTION, 0f, 1f);
        float eased = Easing.EASE_OUT_CUBIC.apply(expansion);
        float radius = pulseSize.getValue() * eased;

         
        float appear = MathHelper.clamp(t / 0.06f, 0f, 1f);
        float fade = appear * (float) Math.pow(1.0f - t, 1.15);
        int crestAlpha = (int) (235 * MathHelper.clamp(fade, 0f, 1f));
        if (crestAlpha <= 2 || radius <= 0.01f) return;

        int r = color.getRed(), g = color.getGreen(), b = color.getBlue();

         
        matrices.push();
        matrices.translate(effect.origin.x - cam.x, effect.origin.y - cam.y, effect.origin.z - cam.z);
        Matrix4f ringMatrix = matrices.peek().getPositionMatrix();
        renderShockwaveRing(ringMatrix, radius, r, g, b, crestAlpha);
        matrices.pop();

         
        if (!effect.surfaceBlocks.isEmpty()) {
            matrices.push();
            matrices.translate(-cam.x, -cam.y, -cam.z);
            Matrix4f worldMatrix = matrices.peek().getPositionMatrix();
            renderBlockWavefront(worldMatrix, cam, effect, radius, fade, r, g, b);
            matrices.pop();
        }
    }

 
    private void renderShockwaveRing(Matrix4f matrix, float radius, int r, int g, int b, int crestAlpha) {
        float border = Math.min(RING_BORDER_THICKNESS, radius * 0.65f);
        float borderInnerR = Math.max(0.0f, radius - border);
        float glowInnerR = borderInnerR * 0.35f;
        float outerR = radius + RING_OUTER_FALLOFF;

        int borderInnerAlpha = (int) (crestAlpha * 0.30f);

        Tessellator tessellator = Tessellator.getInstance();

         
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            buffer.vertex(matrix, cos * glowInnerR, RING_BASE_Y, sin * glowInnerR).color(r, g, b, 0);
            buffer.vertex(matrix, cos * borderInnerR, RING_BASE_Y, sin * borderInnerR).color(r, g, b, borderInnerAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

         
        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            buffer.vertex(matrix, cos * borderInnerR, RING_BASE_Y, sin * borderInnerR).color(r, g, b, borderInnerAlpha);
            buffer.vertex(matrix, cos * radius, RING_BASE_Y, sin * radius).color(r, g, b, crestAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

         
        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            buffer.vertex(matrix, cos * radius, RING_BASE_Y, sin * radius).color(r, g, b, crestAlpha);
            buffer.vertex(matrix, cos * outerR, RING_BASE_Y, sin * outerR).color(r, g, b, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

 
    private void renderBlockWavefront(Matrix4f matrix, Vec3d cam, Effect effect,
                                      float radius, float fade, int r, int g, int b) {
        Tessellator tessellator = Tessellator.getInstance();

         
        BufferBuilder fillBuffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (SurfaceBlock sb : effect.surfaceBlocks) {
            float alpha = computeBlockAlpha(sb, radius, fade);
            if (alpha <= 0.012f) continue;
            int fillAlpha = (int) (alpha * 90);
            if (fillAlpha <= 2) continue;

            for (Box box : sb.boxes) {
                float minX = (float) box.minX, minZ = (float) box.minZ;
                float maxX = (float) box.maxX, maxZ = (float) box.maxZ;
                float topY = (float) box.maxY + 0.002f;

                fillBuffer.vertex(matrix, minX, topY, minZ).color(r, g, b, fillAlpha);
                fillBuffer.vertex(matrix, maxX, topY, minZ).color(r, g, b, fillAlpha);
                fillBuffer.vertex(matrix, maxX, topY, maxZ).color(r, g, b, fillAlpha);
                fillBuffer.vertex(matrix, minX, topY, maxZ).color(r, g, b, fillAlpha);
            }
        }
        BuiltBuffer builtFill = fillBuffer.endNullable();
        if (builtFill != null) {
            BufferRenderer.drawWithGlobalProgram(builtFill);
        }

         
        GL11.glLineWidth(Math.max(1.0f, outlineWidth.getValue()));
        BufferBuilder lineBuffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (SurfaceBlock sb : effect.surfaceBlocks) {
            float alpha = computeBlockAlpha(sb, radius, fade);
            if (alpha <= 0.012f) continue;
            int lineAlpha = (int) (alpha * 255);
            if (lineAlpha <= 2) continue;

            for (Box box : sb.boxes) {
                emitBoxOutline(lineBuffer, matrix, box, r, g, b, lineAlpha);
            }
        }
        BuiltBuffer builtLines = lineBuffer.endNullable();
        if (builtLines != null) {
            BufferRenderer.drawWithGlobalProgram(builtLines);
        }
    }

 
    private float computeBlockAlpha(SurfaceBlock sb, float radius, float fade) {
        float d = (float) sb.distance;
        float reveal = MathHelper.clamp((radius - d + BLOCK_REVEAL_WIDTH) / BLOCK_REVEAL_WIDTH, 0f, 1f);
        if (reveal <= 0f) return 0f;

        float delta = radius - d;
        float highlight = (float) Math.exp(-(delta * delta) / (2.0f * BLOCK_HIGHLIGHT_SIGMA * BLOCK_HIGHLIGHT_SIGMA));
        return MathHelper.clamp(fade * reveal * (0.30f + 0.70f * highlight), 0f, 1f);
    }

      
    private void emitBoxOutline(BufferBuilder buffer, Matrix4f matrix, Box box, int r, int g, int b, int alpha) {
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;

         
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha);

         
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha);

         
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha);
    }

     

 
    private void renderBeam(Matrix4f matrix, Color color, float t) {
         
        float strike = MathHelper.clamp(t / 0.12f, 0f, 1f);
        float strikeEased = Easing.EASE_OUT_CUBIC.apply(strike);

        float height = beamHeight.getValue();
        float topY = height;
        float bottomY = topY - height * strikeEased;  

         
        float fade = t < 0.12f ? 1.0f : (float) Math.pow(1.0f - (t - 0.12f) / 0.88f, 1.6f);
        int coreAlpha = (int) (235 * MathHelper.clamp(fade, 0f, 1f));
        if (coreAlpha <= 2) return;
        int haloAlpha = (int) (coreAlpha * 0.45f);

         
        float widthK = 0.55f + 0.45f * fade;
        float coreR = beamRadius.getValue() * 0.45f * widthK;
        float haloR = beamRadius.getValue() * widthK;

         
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue();
        int coreRC = (r + 255 * 2) / 3, coreGC = (g + 255 * 2) / 3, coreBC = (b + 255 * 2) / 3;

        drawBeamCylinder(matrix, coreR, bottomY, topY, coreRC, coreGC, coreBC, coreAlpha);
        drawBeamCylinder(matrix, haloR, bottomY, topY, r, g, b, haloAlpha);

         
        if (strike >= 1.0f) {
            renderImpactDisc(matrix, r, g, b, haloAlpha, haloR * 3.0f);
        }
    }

    private void drawBeamCylinder(Matrix4f matrix, float radius, float bottomY, float topY,
                                  int r, int g, int b, int alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= BEAM_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / BEAM_SEGMENTS);
            float x = MathHelper.cos(angle) * radius;
            float z = MathHelper.sin(angle) * radius;
             
            buffer.vertex(matrix, x, bottomY, z).color(r, g, b, alpha);
            buffer.vertex(matrix, x, topY, z).color(r, g, b, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderImpactDisc(Matrix4f matrix, int r, int g, int b, int alpha, float radius) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, 0f, RING_BASE_Y, 0f).color(r, g, b, alpha);
        for (int i = 0; i <= BEAM_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / BEAM_SEGMENTS);
            buffer.vertex(matrix, MathHelper.cos(angle) * radius, RING_BASE_Y, MathHelper.sin(angle) * radius).color(r, g, b, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

     

 
    private void renderRickEffect(MatrixStack matrices, Vec3d cam, Effect effect, long now, float t) {
        RickData d = effect.rick;
        float spinDeg = ((now % 36000L) / 1000.0f) * PORTAL_SPIN_DEG_PER_SEC;

        float baseYaw = yawFromDir(d.facing);
        float exitYaw = yawFromDir(d.exitDir);

         
        float openA = openCurve(t, RK_PORTAL_A_IN_START, RK_PORTAL_A_IN_END, RK_PORTAL_A_OUT_START, RK_PORTAL_A_OUT_END);
        float openB = openCurve(t, RK_PORTAL_B_IN_START, RK_PORTAL_B_IN_END, RK_PORTAL_B_OUT_START, RK_PORTAL_B_OUT_END);
        float openC = openCurve(t, RK_PORTAL_C_IN_START, RK_PORTAL_C_IN_END, RK_PORTAL_C_OUT_START, RK_PORTAL_C_OUT_END);

         
        float emerge = Easing.EASE_OUT_CUBIC.apply(phase(t, RK_EMERGE_START, RK_EMERGE_END));
        float exit = Easing.FAST_IN_OUT.apply(phase(t, RK_EXIT_START, RK_EXIT_END));
        float turn = Easing.FAST_IN_OUT.apply(phase(t, RK_TURN_START, RK_TURN_END));

        Vec3d rickStart = d.anchor.subtract(d.facing.multiply(0.45));
        Vec3d rickPos = rickStart
                .add(d.facing.multiply(RICK_EMERGE_DISTANCE * emerge))
                .add(d.exitDir.multiply(RICK_EXIT_DISTANCE * exit));

        float rickYaw = MathHelper.lerp(turn, baseYaw, exitYaw);

        float fadeIn = phase(t, RK_EMERGE_START, RK_RICK_FADE_IN_END);
        float fadeOut = 1.0f - phase(t, RK_RICK_FADE_OUT_START, RK_RICK_FADE_OUT_END);
        float rickAlpha = MathHelper.clamp(Math.min(fadeIn, fadeOut), 0f, 1f);

         
        float armRaise = phase(t, RK_ARM_RAISE_START, RK_ARM_RAISE_END)
                * (1.0f - phase(t, RK_ARM_LOWER_START, RK_ARM_LOWER_END));
        armRaise = Easing.SMOOTH_STEP.apply(armRaise);

         
        float moveK = movementIntensity(t);
        float legSwing = moveK > 0.001f
                ? (float) Math.sin((now - effect.spawnTime) / 85.0) * 0.55f * moveK
                : 0f;

         
        float sinkRaw = phase(t, RK_SINK_START, RK_SINK_END);
        float sink = Easing.FAST_IN.apply(sinkRaw);
        float ghostAlpha = sinkRaw > 0f ? (float) Math.pow(1.0f - sinkRaw, 1.25f) : 1.0f;

         

         
        if (openA > 0.001f) {
            matrices.push();
            Vec3d pA = d.anchor.subtract(d.facing.multiply(0.55));
            matrices.translate(pA.x - cam.x, pA.y + RICK_PORTAL_CENTER_Y - cam.y, pA.z - cam.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(baseYaw));
            drawSpiralPortal(matrices, RICK_PORTAL_RADIUS, openA, spinDeg);
            matrices.pop();
        }

         
        if (openB > 0.001f) {
            matrices.push();
            matrices.translate(effect.origin.x - cam.x, effect.origin.y + 0.03 - cam.y, effect.origin.z - cam.z);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
            drawSpiralPortal(matrices, RICK_FLOOR_PORTAL_RADIUS, openB, -spinDeg);
            matrices.pop();
        }

         
        if (openC > 0.001f) {
            matrices.push();
            Vec3d pC = rickStart
                    .add(d.facing.multiply(RICK_EMERGE_DISTANCE))
                    .add(d.exitDir.multiply(RICK_EXIT_DISTANCE + 0.15));
            matrices.translate(pC.x - cam.x, pC.y + RICK_PORTAL_CENTER_Y - cam.y, pC.z - cam.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(exitYaw));
            drawSpiralPortal(matrices, RICK_PORTAL_RADIUS, openC, spinDeg * 1.2f);
            matrices.pop();
        }

         

        if (sinkRaw > 0f && ghostAlpha > 0.02f && openB > 0.05f) {
            RenderSystem.defaultBlendFunc();  
            matrices.push();
            double ghostY = effect.origin.y - sink * (d.entityHeight + 0.35);
            matrices.translate(effect.origin.x - cam.x, ghostY - cam.y, effect.origin.z - cam.z);
            drawVictimGhost(matrices, d.entityWidth, d.entityHeight, ghostAlpha);
            matrices.pop();
             
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA.value, GlStateManager.DstFactor.ONE.value);
        }

         

        if (rickAlpha > 0.02f) {
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(rickAlpha > 0.95f);  

            matrices.push();
            matrices.translate(rickPos.x - cam.x, rickPos.y - cam.y, rickPos.z - cam.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rickYaw));
            drawRickModel(matrices, rickAlpha, legSwing, armRaise);
            matrices.pop();

            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA.value, GlStateManager.DstFactor.ONE.value);
        }
    }

 
 
    private void renderAscend(MatrixStack matrices, Vec3d cam, Effect effect, float t) {
        AscendData d = effect.ascend;
        if (d == null) return;

        float emergeEnd = 0.22f;
        float fadeStart = 0.62f;

        float emerge = Easing.EASE_OUT_CUBIC.apply(MathHelper.clamp(t / emergeEnd, 0f, 1f));
        float dissolveRaw = MathHelper.clamp((t - fadeStart) / (1f - fadeStart), 0f, 1f);
        float dissolve = (float) Math.pow(1.0f - dissolveRaw, 1.3f);

        float alpha01 = 0.5f * emerge * dissolve;
        if (alpha01 <= 0.01f) return;

        float height = ascendHeight.getValue();
        float rise = Easing.EASE_OUT_CUBIC.apply(t);
        double y = effect.origin.y + height * rise;
        float sway = (float) Math.sin(t * Math.PI * 1.6) * 0.035f;

        matrices.push();
        matrices.translate(effect.origin.x - cam.x + sway, y - cam.y, effect.origin.z - cam.z);

        if (d.entity != null) {
            renderGhostEntity(matrices, d.entity, alpha01);
        } else {
            RenderSystem.defaultBlendFunc();
            drawVictimGhost(matrices, d.entityWidth, d.entityHeight, alpha01);
        }

        matrices.pop();
         
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA.value, GlStateManager.DstFactor.ONE.value);
    }

 
    private void renderGhostEntity(MatrixStack matrices, LivingEntity entity, float alpha01) {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

         
        RenderSystem.setShaderColor(0.55f, 1.0f, 0.62f, MathHelper.clamp(alpha01, 0f, 1f));

        BlockPos lightPos = BlockPos.ofFloored(entity.getX(), entity.getY(), entity.getZ());
        int light = mc.world != null
                ? LightmapTextureManager.pack(
                mc.world.getLightLevel(LightType.BLOCK, lightPos),
                mc.world.getLightLevel(LightType.SKY, lightPos))
                : LightmapTextureManager.MAX_LIGHT_COORDINATE;

        matrices.push();
        dispatcher.render(entity, 0.0, 0.0, 0.0, entity.getYaw(), matrices, immediate, light);
        matrices.pop();
        immediate.draw();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

      
    private float movementIntensity(float t) {
        float in = phase(t, RK_EMERGE_START, RK_EMERGE_END);
        float out = phase(t, RK_EXIT_START, RK_EXIT_END);
        float walkIn = in > 0f && in < 1f ? (float) Math.sin(Math.PI * in) : 0f;
        float walkOut = out > 0f && out < 1f ? (float) Math.sin(Math.PI * out) : 0f;
        return Math.max(walkIn, walkOut);
    }

      
    private static float phase(float t, float a, float b) {
        return MathHelper.clamp((t - a) / (b - a), 0f, 1f);
    }

 
    private static float openCurve(float t, float inStart, float inEnd, float outStart, float outEnd) {
        if (t <= inStart) return 0f;
        if (t < inEnd) return Easing.EASE_OUT_BACK.apply(phase(t, inStart, inEnd));
        if (t <= outStart) return 1f;
        if (t < outEnd) return 1f - Easing.FAST_IN.apply(phase(t, outStart, outEnd));
        return 0f;
    }

      
    private static float yawFromDir(Vec3d dir) {
        return (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
    }

     

 
    private void drawSpiralPortal(MatrixStack matrices, float maxRadius, float open, float spinDeg) {
        float radius = maxRadius * MathHelper.clamp(open, 0f, 1.12f);  
        if (radius <= 0.02f) return;
        int alpha = (int) (235 * MathHelper.clamp(open, 0f, 1f));
        if (alpha <= 2) return;

        Matrix4f m = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();

         
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(m, 0f, 0f, 0f).color(PORTAL_CORE_R, PORTAL_CORE_G, PORTAL_CORE_B, (int) (alpha * 0.85f));
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
            buffer.vertex(m, MathHelper.cos(angle) * radius, MathHelper.sin(angle) * radius, 0f)
                    .color(PORTAL_EDGE_R, PORTAL_EDGE_G, PORTAL_EDGE_B, (int) (alpha * 0.30f));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

         
        float spinRad = (float) Math.toRadians(spinDeg);
        for (int arm = 0; arm < PORTAL_ARMS; arm++) {
            float armOffset = (float) (arm * (Math.PI * 2.0) / PORTAL_ARMS);

            buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= PORTAL_ARM_STEPS; i++) {
                float ft = i / (float) PORTAL_ARM_STEPS;
                float rad = radius * (0.10f + 0.90f * ft);
                 
                float ang = spinRad + armOffset + ft * 9.6f;
                float halfW = radius * (0.085f * (1.0f - 0.60f * ft) + 0.012f);
                int armAlpha = (int) (alpha * 0.95f * Math.sin(Math.PI * ft));

                float cos = MathHelper.cos(ang), sin = MathHelper.sin(ang);
                float inR = rad - halfW, outR = rad + halfW;
                buffer.vertex(m, cos * inR, sin * inR, 0f).color(PORTAL_ARM_R, PORTAL_ARM_G, PORTAL_ARM_B, armAlpha);
                buffer.vertex(m, cos * outR, sin * outR, 0f).color(PORTAL_MID_R, PORTAL_MID_G, PORTAL_MID_B, (int) (armAlpha * 0.55f));
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

         
        float rimInner = radius * 0.90f;
        float rimOuter = radius * 1.07f;

        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
            float cos = MathHelper.cos(angle), sin = MathHelper.sin(angle);
            buffer.vertex(m, cos * rimInner, sin * rimInner, 0f).color(PORTAL_RIM_R, PORTAL_RIM_G, PORTAL_RIM_B, 0);
            buffer.vertex(m, cos * radius, sin * radius, 0f).color(PORTAL_RIM_R, PORTAL_RIM_G, PORTAL_RIM_B, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            float angle = (float) (i * (Math.PI * 2.0) / RING_SEGMENTS);
            float cos = MathHelper.cos(angle), sin = MathHelper.sin(angle);
            buffer.vertex(m, cos * radius, sin * radius, 0f).color(PORTAL_RIM_R, PORTAL_RIM_G, PORTAL_RIM_B, alpha);
            buffer.vertex(m, cos * rimOuter, sin * rimOuter, 0f).color(PORTAL_RIM_R, PORTAL_RIM_G, PORTAL_RIM_B, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

     

 
 
    private void drawRickModel(MatrixStack matrices, float alpha01, float legSwing, float armRaise) {
        int a = (int) (255 * MathHelper.clamp(alpha01, 0f, 1f));
        Tessellator tessellator = Tessellator.getInstance();

         
        for (int side = -1; side <= 1; side += 2) {
            matrices.push();
            matrices.translate(0f, 0.92f, 0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(legSwing * side));
            matrices.translate(0f, -0.92f, 0f);

            BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            Matrix4f lm = matrices.peek().getPositionMatrix();
            float x0 = side < 0 ? -0.15f : 0.04f;
            float x1 = side < 0 ? -0.04f : 0.15f;
            emitShadedBox(buf, lm, x0, 0.09f, -0.065f, x1, 0.95f, 0.065f, C_PANTS, a);           
            emitShadedBox(buf, lm, x0 - 0.015f, 0.00f, -0.09f, x1 + 0.015f, 0.09f, 0.14f, C_SHOES, a);  
            drawBuilt(buf);
            matrices.pop();
        }

         
        BufferBuilder body = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f bm = matrices.peek().getPositionMatrix();

         
        emitShadedBox(body, bm, -0.075f, 0.96f, -0.10f, 0.075f, 1.44f, 0.115f, C_SHIRT, a);
         
        emitShadedBox(body, bm, -0.17f, 0.88f, -0.095f, 0.17f, 0.96f, 0.10f, C_BELT, a);
        emitShadedBox(body, bm, -0.035f, 0.895f, 0.10f, 0.035f, 0.945f, 0.112f, C_BUCKLE, a);

         
        emitShadedBox(body, bm, -0.22f, 0.42f, -0.125f, -0.075f, 1.50f, 0.12f, C_COAT, a);    
        emitShadedBox(body, bm,  0.075f, 0.42f, -0.125f,  0.22f, 1.50f, 0.12f, C_COAT, a);    
        emitShadedBox(body, bm, -0.075f, 0.42f, -0.125f,  0.075f, 1.50f, -0.095f, C_COAT, a);  
        emitShadedBox(body, bm, -0.22f, 1.44f, -0.125f,  0.22f, 1.50f, 0.12f, C_COAT, a);     
         
        emitShadedBox(body, bm, -0.115f, 1.18f, 0.121f, -0.06f, 1.46f, 0.138f, C_COAT, a);
        emitShadedBox(body, bm,  0.06f,  1.18f, 0.121f,  0.115f, 1.46f, 0.138f, C_COAT, a);

         
        emitShadedBox(body, bm, -0.05f, 1.48f, -0.05f, 0.05f, 1.56f, 0.05f, C_SKIN, a);

         
        emitShadedBox(body, bm, -0.14f, 1.56f, -0.12f, 0.14f, 1.94f, 0.12f, C_SKIN, a);
         
        emitShadedBox(body, bm, -0.11f, 1.815f, 0.121f, 0.11f, 1.845f, 0.13f, C_HAIR, a);
         
        emitShadedBox(body, bm, -0.105f, 1.70f, 0.121f, -0.015f, 1.81f, 0.127f, C_EYES, a);
        emitShadedBox(body, bm,  0.015f, 1.70f, 0.121f,  0.105f, 1.81f, 0.127f, C_EYES, a);
        emitShadedBox(body, bm, -0.072f, 1.735f, 0.127f, -0.048f, 1.765f, 0.132f, C_DARK, a);
        emitShadedBox(body, bm,  0.048f, 1.735f, 0.127f,  0.072f, 1.765f, 0.132f, C_DARK, a);
         
        emitShadedBox(body, bm, -0.02f, 1.655f, 0.121f, 0.02f, 1.715f, 0.148f, C_SKIN, a);
         
        emitShadedBox(body, bm, -0.06f, 1.612f, 0.121f, 0.06f, 1.628f, 0.127f, C_DARK, a);
        emitShadedBox(body, bm, -0.03f, 1.578f, 0.121f, 0.03f, 1.588f, 0.126f, C_DARK, a);
        drawBuilt(body);

         
        BufferBuilder hairBuf = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        emitShadedBox(hairBuf, bm, -0.16f, 1.92f, -0.14f, 0.16f, 2.00f, 0.14f, C_HAIR, a);    
        emitShadedBox(hairBuf, bm, -0.155f, 1.62f, -0.165f, 0.155f, 1.94f, -0.11f, C_HAIR, a);  
        drawBuilt(hairBuf);

        float[][] spikes = {
                 
                { 75f,   0f, -0.15f,  0.00f, 0.26f},
                {-75f,   0f,  0.15f,  0.00f, 0.26f},
                { 50f,   0f, -0.12f,  0.00f, 0.32f},
                {-50f,   0f,  0.12f,  0.00f, 0.32f},
                { 25f, -15f, -0.08f, -0.05f, 0.35f},
                {-25f, -15f,  0.08f, -0.05f, 0.35f},
                { 12f,  16f, -0.04f,  0.06f, 0.30f},
                {-12f,  16f,  0.04f,  0.06f, 0.30f},
                {  0f,  -8f,  0.00f, -0.02f, 0.38f},
        };
        for (float[] s : spikes) {
            matrices.push();
            matrices.translate(s[2], 1.97f, s[3]);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s[0]));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(s[1]));
            BufferBuilder spike = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            Matrix4f sm = matrices.peek().getPositionMatrix();
            emitShadedBox(spike, sm, -0.042f, 0f, -0.042f, 0.042f, s[4], 0.042f, C_HAIR, a);
            drawBuilt(spike);
            matrices.pop();
        }

         
         
        for (int side = -1; side <= 1; side += 2) {
            boolean gunArm = side > 0;
            float raise = gunArm ? armRaise : 0f;

            matrices.push();
            matrices.translate(side * 0.235f, 1.44f, 0f);
             
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 52f * (1f - raise)));
            if (gunArm && raise > 0f) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotation(-1.52f * raise));  
            }

            BufferBuilder armBuf = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            Matrix4f am = matrices.peek().getPositionMatrix();
            emitShadedBox(armBuf, am, -0.055f, -0.44f, -0.055f, 0.055f, 0.03f, 0.055f, C_COAT, a);    
            emitShadedBox(armBuf, am, -0.045f, -0.52f, -0.045f, 0.045f, -0.44f, 0.045f, C_SKIN, a);   
            emitShadedBox(armBuf, am, -0.065f, -0.62f, -0.03f, 0.065f, -0.52f, 0.03f, C_SKIN, a);     

             
            if (gunArm && raise > 0.15f) {
                int ga = (int) (a * MathHelper.clamp((raise - 0.15f) / 0.25f, 0f, 1f));
                emitShadedBox(armBuf, am, -0.06f, -0.68f, -0.02f, 0.06f, -0.54f, 0.26f, C_GUN, ga);        
                emitShadedBox(armBuf, am, -0.035f, -0.645f, 0.26f, 0.035f, -0.575f, 0.34f, C_GUN, ga);     
                emitShadedBox(armBuf, am, -0.045f, -0.655f, 0.34f, 0.045f, -0.565f, 0.385f, C_GUN_GLOW, ga);  
                emitShadedBox(armBuf, am, -0.025f, -0.54f, 0.06f, 0.025f, -0.495f, 0.15f, C_GUN_GLOW, ga);    
            }
            drawBuilt(armBuf);

             
            if (!(gunArm && raise > 0.15f)) {
                for (int f = 0; f < 5; f++) {
                    matrices.push();
                    matrices.translate(-0.052f + f * 0.026f, -0.62f, 0f);
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((f - 2f) * 13f));
                    BufferBuilder finger = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                    Matrix4f fm = matrices.peek().getPositionMatrix();
                    float len = f == 2 ? 0.105f : (f == 0 || f == 4 ? 0.075f : 0.095f);
                    emitShadedBox(finger, fm, -0.011f, -len, -0.011f, 0.011f, 0f, 0.011f, C_SKIN, a);
                    drawBuilt(finger);
                    matrices.pop();
                }
            }
            matrices.pop();
        }
    }

 
    private void emitShadedBox(BufferBuilder buf, Matrix4f m,
                               float x0, float y0, float z0, float x1, float y1, float z1,
                               int[] rgb, int alpha) {
        if (alpha <= 1) return;
        int rT = shade(rgb[0], 1.00f), gT = shade(rgb[1], 1.00f), bT = shade(rgb[2], 1.00f);  
        int rB = shade(rgb[0], 0.55f), gB = shade(rgb[1], 0.55f), bB = shade(rgb[2], 0.55f);  
        int rN = shade(rgb[0], 0.90f), gN = shade(rgb[1], 0.90f), bN = shade(rgb[2], 0.90f);  
        int rE = shade(rgb[0], 0.75f), gE = shade(rgb[1], 0.75f), bE = shade(rgb[2], 0.75f);  

         
        buf.vertex(m, x0, y1, z0).color(rT, gT, bT, alpha);
        buf.vertex(m, x0, y1, z1).color(rT, gT, bT, alpha);
        buf.vertex(m, x1, y1, z1).color(rT, gT, bT, alpha);
        buf.vertex(m, x1, y1, z0).color(rT, gT, bT, alpha);
         
        buf.vertex(m, x0, y0, z0).color(rB, gB, bB, alpha);
        buf.vertex(m, x1, y0, z0).color(rB, gB, bB, alpha);
        buf.vertex(m, x1, y0, z1).color(rB, gB, bB, alpha);
        buf.vertex(m, x0, y0, z1).color(rB, gB, bB, alpha);
         
        buf.vertex(m, x0, y0, z1).color(rN, gN, bN, alpha);
        buf.vertex(m, x1, y0, z1).color(rN, gN, bN, alpha);
        buf.vertex(m, x1, y1, z1).color(rN, gN, bN, alpha);
        buf.vertex(m, x0, y1, z1).color(rN, gN, bN, alpha);
         
        buf.vertex(m, x0, y0, z0).color(rN, gN, bN, alpha);
        buf.vertex(m, x0, y1, z0).color(rN, gN, bN, alpha);
        buf.vertex(m, x1, y1, z0).color(rN, gN, bN, alpha);
        buf.vertex(m, x1, y0, z0).color(rN, gN, bN, alpha);
         
        buf.vertex(m, x1, y0, z0).color(rE, gE, bE, alpha);
        buf.vertex(m, x1, y1, z0).color(rE, gE, bE, alpha);
        buf.vertex(m, x1, y1, z1).color(rE, gE, bE, alpha);
        buf.vertex(m, x1, y0, z1).color(rE, gE, bE, alpha);
         
        buf.vertex(m, x0, y0, z0).color(rE, gE, bE, alpha);
        buf.vertex(m, x0, y0, z1).color(rE, gE, bE, alpha);
        buf.vertex(m, x0, y1, z1).color(rE, gE, bE, alpha);
        buf.vertex(m, x0, y1, z0).color(rE, gE, bE, alpha);
    }

    private static int shade(int channel, float k) {
        return MathHelper.clamp((int) (channel * k), 0, 255);
    }

    private static void drawBuilt(BufferBuilder buf) {
        BuiltBuffer built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

     

 
    private void drawVictimGhost(MatrixStack matrices, float width, float height, float alpha01) {
        Tessellator tessellator = Tessellator.getInstance();
        Matrix4f m = matrices.peek().getPositionMatrix();

        int fillA = (int) (85 * alpha01);
        int lineA = (int) (200 * alpha01);
        if (fillA <= 1 && lineA <= 1) return;

        float hw = width * 0.5f;
        boolean tall = height > 1.0f;
        float bodyH = tall ? height * 0.72f : height;

        int[] ghost = {130, 255, 140};

         
        BufferBuilder fill = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        emitShadedBox(fill, m, -hw, 0f, -hw, hw, bodyH, hw, ghost, fillA);
        if (tall) {
            float headHW = hw * 0.62f;
            emitShadedBox(fill, m, -headHW, bodyH, -headHW, headHW, height, headHW, ghost, fillA);
        }
        drawBuilt(fill);

         
        GL11.glLineWidth(2.0f);
        BufferBuilder lines = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        emitBoxOutline(lines, m, new Box(-hw, 0, -hw, hw, bodyH, hw), ghost[0], ghost[1], ghost[2], lineA);
        if (tall) {
            float headHW = hw * 0.62f;
            emitBoxOutline(lines, m, new Box(-headHW, bodyH, -headHW, headHW, height, headHW),
                    ghost[0], ghost[1], ghost[2], lineA);
        }
        BuiltBuffer builtLines = lines.endNullable();
        if (builtLines != null) BufferRenderer.drawWithGlobalProgram(builtLines);
        GL11.glLineWidth(1.0f);
    }

     

 
    private void tickRickParticles(Effect effect, long now) {
        if (mc.world == null) return;
        RickData d = effect.rick;
        float t = MathHelper.clamp((now - effect.spawnTime) / (float) effect.ttl, 0f, 1f);

         
        if (!d.burstA && t >= RK_PORTAL_A_IN_START) {
            d.burstA = true;
            Vec3d pA = d.anchor.subtract(d.facing.multiply(0.55)).add(0, RICK_PORTAL_CENTER_Y, 0);
            spawnPortalBurst(pA, 14);
        }
         
        if (!d.burstB && t >= RK_PORTAL_B_IN_START) {
            d.burstB = true;
            spawnPortalBurst(effect.origin.add(0, 0.15, 0), 12);
        }
         
        if (!d.burstC && t >= RK_PORTAL_C_IN_START) {
            d.burstC = true;
            Vec3d pC = d.anchor.subtract(d.facing.multiply(0.45))
                    .add(d.facing.multiply(RICK_EMERGE_DISTANCE))
                    .add(d.exitDir.multiply(RICK_EXIT_DISTANCE + 0.15))
                    .add(0, RICK_PORTAL_CENTER_Y, 0);
            spawnPortalBurst(pC, 14);
        }

         
        if (t >= RK_SINK_START && t <= RK_SINK_END) {
            float sinkRaw = phase(t, RK_SINK_START, RK_SINK_END);
            double topY = effect.origin.y + d.entityHeight * (1.0 - sinkRaw);
            for (int i = 0; i < 2; i++) {
                double ox = (random.nextDouble() - 0.5) * d.entityWidth;
                double oz = (random.nextDouble() - 0.5) * d.entityWidth;
                mc.world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                        effect.origin.x + ox, topY + random.nextDouble() * 0.3, effect.origin.z + oz,
                        0.0, 0.04 + random.nextDouble() * 0.05, 0.0);
            }
            if (random.nextInt(3) == 0) {
                mc.world.addParticle(ParticleTypes.END_ROD,
                        effect.origin.x, topY + 0.2, effect.origin.z,
                        (random.nextDouble() - 0.5) * 0.03, 0.06, (random.nextDouble() - 0.5) * 0.03);
            }
        }
    }

      
    private void spawnPortalBurst(Vec3d center, int count) {
        for (int i = 0; i < count; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0;
            double r = 0.6 + random.nextDouble() * 0.5;
            mc.world.addParticle(ParticleTypes.COMPOSTER,
                    center.x + Math.cos(theta) * r, center.y + (random.nextDouble() - 0.5) * 1.2, center.z + Math.sin(theta) * r,
                    Math.cos(theta) * 0.05, (random.nextDouble() - 0.5) * 0.04, Math.sin(theta) * 0.05);
        }
    }

     

      
    private static final class SurfaceBlock {
        final List<Box> boxes;
        final double distance;

        SurfaceBlock(List<Box> boxes, double distance) {
            this.boxes = boxes;
            this.distance = distance;
        }
    }

      
    private static final class RickData {
        final Vec3d anchor;       
        final Vec3d facing;       
        final Vec3d exitDir;      
        final float entityWidth;
        final float entityHeight;

         
        boolean burstA, burstB, burstC;

        RickData(Vec3d anchor, Vec3d facing, Vec3d exitDir, float entityWidth, float entityHeight) {
            this.anchor = anchor;
            this.facing = facing;
            this.exitDir = exitDir;
            this.entityWidth = entityWidth;
            this.entityHeight = entityHeight;
        }
    }
      
      
    private static final class AscendData {
        final float entityWidth;
        final float entityHeight;
        final LivingEntity entity;  

        AscendData(float entityWidth, float entityHeight, LivingEntity entity) {
            this.entityWidth = entityWidth;
            this.entityHeight = entityHeight;
            this.entity = entity;
        }
    }

    private static final class Effect {
        final Vec3d origin;
        final Mode mode;
        final long spawnTime;
        final long ttl;
        final List<SurfaceBlock> surfaceBlocks;
        final RickData rick;
        final AscendData ascend;

        Effect(Vec3d origin, Mode mode, long ttl, List<SurfaceBlock> surfaceBlocks) {
            this(origin, mode, ttl, surfaceBlocks, null, null);
        }

        Effect(Vec3d origin, Mode mode, long ttl, RickData rick) {
            this(origin, mode, ttl, List.of(), rick, null);
        }

        Effect(Vec3d origin, Mode mode, long ttl, AscendData ascend) {
            this(origin, mode, ttl, List.of(), null, ascend);
        }

        private Effect(Vec3d origin, Mode mode, long ttl, List<SurfaceBlock> surfaceBlocks, RickData rick, AscendData ascend) {
            this.origin = origin;
            this.mode = mode;
            this.spawnTime = System.currentTimeMillis();
            this.ttl = ttl;
            this.surfaceBlocks = surfaceBlocks;
            this.rick = rick;
            this.ascend = ascend;
        }
    }
}
