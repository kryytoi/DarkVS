package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.events.impl.EventPlayerTick;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.TotemPopEvent;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ActionParticles extends Module {

    private static final Identifier GLOW_TEXTURE    = darkvisuals.id("hud/glow.png");
    private static final Identifier BLOOM_TEXTURE   = darkvisuals.id("hud/bloom.png");
    private static final Identifier DOLLAR_TEXTURE  = darkvisuals.id("particles/dollar.png");
    private static final Identifier SNOW_TEXTURE    = darkvisuals.id("particles/snow.png");
    private static final Identifier STAR_TEXTURE    = darkvisuals.id("particles/star.png");
    private static final Identifier FIREFLY_TEXTURE = darkvisuals.id("particles/firefly.png");

    private final ListSetting type = new ListSetting("Тип частиц", true,
            new BooleanSetting("Свечение", true),
            new BooleanSetting("Блум", false),
            new BooleanSetting("Доллар", false),
            new BooleanSetting("Снег", false),
            new BooleanSetting("Звезда", false),
            new BooleanSetting("Светлячок", false)
    );

    private final ListSetting reason = new ListSetting("Добавлять при", false,
            new BooleanSetting("Бездействии", false),
            new BooleanSetting("Беге", false),
            new BooleanSetting("Ударе", true),
            new BooleanSetting("Падении перла", false),
            new BooleanSetting("Падении трезубца", false),
            new BooleanSetting("Сносе тотема", true)
    );

    private final NumberSetting count = new NumberSetting("Количество", 10f, 2f, 40f, 1f);

    private final BooleanSetting glow = new BooleanSetting("Свечение", true);

    private final ArrayList<ParticleData> particles = new ArrayList<>();
    private final Random rnd = new Random();

    public ActionParticles() {
        super("ActionParticles", Category.Render, "Красивые частицы при различных действиях");
        getSettings().add(type);
        getSettings().add(reason);
        getSettings().add(count);
        getSettings().add(glow);
    }

    @Override
    public void onDisable() {
        synchronized (particles) {
            particles.clear();
        }
        super.onDisable();
    }

    private boolean isEnable(String name) {
        BooleanSetting setting = reason.getName(name);
        return setting != null && setting.getValue();
    }

    private Identifier getTexture() {
        List<BooleanSetting> toggled = type.getToggled();
        String selected = toggled.isEmpty() ? "Свечение" : toggled.get(0).getName();
        return switch (selected) {
            case "Блум" -> BLOOM_TEXTURE;
            case "Доллар" -> DOLLAR_TEXTURE;
            case "Снег" -> SNOW_TEXTURE;
            case "Звезда" -> STAR_TEXTURE;
            case "Светлячок" -> FIREFLY_TEXTURE;
            default -> GLOW_TEXTURE;
        };
    }

    private boolean isPositionInBlock(Vec3d position) {
        if (mc.world == null || mc.player == null) return true;
        BlockPos blockPos = BlockPos.ofFloored(position);
        if (mc.world.getBlockState(blockPos).isSolidBlock(mc.world, blockPos)) {
            return true;
        }
        RaycastContext context = new RaycastContext(
                mc.player.getEyePos(),
                position,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        );
        BlockHitResult result = mc.world.raycast(context);
        return result.getType() == HitResult.Type.BLOCK;
    }

    private float random(float min, float max) {
        return min + rnd.nextFloat() * (max - min);
    }

    private boolean isMoving() {
        if (mc.player == null) return false;
        Vec3d velocity = mc.player.getVelocity();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004;
    }

    private int getThemeColor() {
        Color color = ThemeManager.getInstance().getThemeColor();
        return color.getRGB();
    }

    @EventHandler
    public void onAttack(EventAttackEntity event) {
        if (fullNullCheck()) return;
        if (!isEnable("Ударе")) return;
        if (!event.isEffectsAllowed()) return;
        if (!event.canProcess()) return;

        Entity target = event.getTarget();
        if (target == null || target == mc.player) return;

        for (int i = 0; i < 35; i++) {
            double targetX = target.getX() + random(-0.4f, 0.4f);
            double targetY = target.getY() + random(-0.4f, (float) target.getHeight() + 0.4f);
            double targetZ = target.getZ() + random(-0.4f, 0.4f);

            if (isPositionInBlock(new Vec3d(targetX, targetY, targetZ))) continue;

            float baseMx = random(-0.8f, 0.8f) * 2.0f;
            float baseMy = random(-0.25f, 1.4f);
            float baseMz = random(-0.8f, 0.8f) * 2.0f;

            Vec3d velocity = new Vec3d(baseMx * 0.075f, baseMy * 0.075f, baseMz * 0.075f);
            long life = (long) random(1000, 1200);

            addParticle(targetX, targetY, targetZ, velocity, getThemeColor(), 0.3f, life, 0.5f, 0.0007f);
        }
    }

    @EventHandler
    public void onTotemPop(TotemPopEvent event) {
        if (fullNullCheck()) return;
        if (!isEnable("Сносе тотема")) return;

        Entity entity = event.getEntity();
        if (entity == null) return;

        double centerX = entity.getX();
        double centerY = entity.getY() + entity.getHeight() / 2.0;
        double centerZ = entity.getZ();

        for (int i = 0; i < 50; i++) {
            double theta = rnd.nextDouble() * 2.0 * Math.PI;
            double phi = rnd.nextDouble() * Math.PI;
            double speed = (rnd.nextDouble() * 0.5 + 0.5) * 0.1;

            double vx = Math.sin(phi) * Math.cos(theta) * speed;
            double vy = Math.sin(phi) * Math.sin(theta) * speed;
            double vz = Math.cos(phi) * speed;

            double spawnX = centerX + random(-0.3f, 0.3f);
            double spawnY = centerY + random(-0.3f, 0.3f);
            double spawnZ = centerZ + random(-0.3f, 0.3f);

            if (isPositionInBlock(new Vec3d(spawnX, spawnY, spawnZ))) continue;

            int color = rnd.nextDouble() < 0.7 ? 0xFF00FF00 : 0xFFFFFF00;
            long life = (long) random(1500, 2000);

            addParticle(spawnX, spawnY, spawnZ, new Vec3d(vx, vy, vz), color, 0.3f, life, 2.0f, 0.00005f);
        }
    }

    @EventHandler
    public void onUpdate(EventPlayerTick e) {
        if (fullNullCheck()) return;

        int particleCount = Math.round(count.getValue());

        if (isEnable("Бездействии")) {
            Vec3d base = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getHeight() / 2.0, mc.player.getZ());

            for (int i = 0; i < particleCount; i++) {
                double distance = random(7, 35);
                double angle = Math.toRadians(random(0, 360));
                double height = random(-7, 25);

                double spawnX = base.x + Math.cos(angle) * distance;
                double spawnY = base.y + height;
                double spawnZ = base.z + Math.sin(angle) * distance;

                Vec3d spawnPos = new Vec3d(spawnX, spawnY, spawnZ);
                if (isPositionInBlock(spawnPos)) continue;

                long life = (long) random(1500, 2000);
                double speed = rnd.nextDouble() < 0.8 ? random(0.015f, 0.03f) : 0.125f;
                double phi = Math.toRadians(random(0, 360));

                Vec3d velocity = new Vec3d(
                        Math.cos(phi) * speed,
                        random((float) (-speed * 0.1f), (float) (speed * 0.1f)),
                        Math.sin(phi) * speed
                );

                addParticle(spawnX, spawnY, spawnZ, velocity, getThemeColor(), 0.3f, life, 3.0f, 0.00005f);
            }
        }

        if (isEnable("Беге") && isMoving()) {
            Vec3d motion = mc.player.getVelocity();
            double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

            Vec3d direction;
            if (speed < 0.01) {
                direction = mc.player.getRotationVector().multiply(-1);
            } else if (mc.player.isGliding()) {
                direction = motion.normalize().multiply(-1);
            } else {
                direction = new Vec3d(-motion.x / speed, 0, -motion.z / speed);
            }

            double distanceBehind = (mc.player.isGliding() ? 1.2 : 0.5) + (speed > 0.1 ? speed * 1.5 : 0);
            double offsetX = random(-0.35f, 0.35f);
            double offsetZ = random(-0.35f, 0.35f);

            double posX = mc.player.getX() + direction.x * distanceBehind + offsetX;
            double posY = mc.player.isGliding()
                    ? mc.player.getY() + mc.player.getHeight() / 2.0 + direction.y * distanceBehind + random(-0.35f, 0.35f)
                    : mc.player.getY() + random(0.2f, (float) mc.player.getHeight() + 0.1f);
            double posZ = mc.player.getZ() + direction.z * distanceBehind + offsetZ;

            if (!isPositionInBlock(new Vec3d(posX, posY, posZ))) {
                double baseSpeed = 0.075;
                Vec3d velocity = direction.multiply(baseSpeed).add(
                        random(-0.01f, 0.01f),
                        random(-0.05f, 0.01f),
                        random(-0.01f, 0.01f)
                ).multiply(0.1);

                long life = (long) random(1500, 2000);
                addParticle(posX, posY, posZ, velocity, getThemeColor(), 0.3f, life, 3.0f, 0.00005f);
            }
        }

        boolean trackPearls = isEnable("Падении перла");
        boolean trackTridents = isEnable("Падении трезубца");
        if (trackPearls || trackTridents) {
            Box searchBox = mc.player.getBoundingBox().expand(100);
            List<Entity> entities = mc.world.getOtherEntities(null, searchBox, e2 -> true);

            for (Entity entity : entities) {
                if (trackPearls && entity instanceof EnderPearlEntity pearl) {
                    if (!pearl.isOnGround()) {
                        createProjectileParticles(pearl.getPos(), 1);
                    }
                }

                if (trackTridents && entity instanceof TridentEntity trident) {
                    if (trident.getVelocity().lengthSquared() > 0.01) {
                        createProjectileParticles(trident.getPos(), 1);
                    }
                }
            }
        }
    }

    private void createProjectileParticles(Vec3d position, int cnt) {
        int particleColor = getThemeColor();

        for (int i = 0; i < cnt * 2.5; i++) {
            double dy = random(0.1f, 0.35f);
            Vec3d particlePos = new Vec3d(position.x, position.y + dy, position.z);

            if (isPositionInBlock(particlePos)) continue;

            float speedMin = random(0.015f, 0.0375f);
            float speedMax = random(0.05f, 0.075f);
            double speedFinal = random(speedMin, speedMax);
            double speedFinalY = speedFinal * 0.4;

            double angleVel = Math.toRadians(random(0, 360));

            Vec3d velocity = new Vec3d(
                    Math.cos(angleVel) * speedFinal,
                    random((float) -speedFinalY, (float) speedFinalY),
                    Math.sin(angleVel) * speedFinal
            );

            long life = (long) random(2400, 2800);
            addParticle(particlePos.x, particlePos.y, particlePos.z, velocity, particleColor, 0.25f, life, 2.0f, 0.00005f);
        }
    }

    private void addParticle(double x, double y, double z, Vec3d velocity, int color, float size, long lifeTime, float smooth, double gravity) {
        if (ParticleData.checkCollision(x, y, z, size, mc)) {
            synchronized (particles) {
                particles.add(new ParticleData(new Vec3d(x, y, z), velocity, color, size, lifeTime, smooth, gravity));
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        synchronized (particles) {
            particles.removeIf(ParticleData::isDead);
        }

        if (particles.isEmpty()) return;

        MatrixStack matrices = e.getMatrices();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        Identifier texture = getTexture();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        if (glow.getValue()) {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        ArrayList<ParticleData> renderList;
        synchronized (particles) {
            renderList = new ArrayList<>(particles);
        }

        for (ParticleData particle : renderList) {
            particle.update(mc);

            double x = particle.position.x - camera.x;
            double y = particle.position.y - camera.y;
            double z = particle.position.z - camera.z;

            matrices.push();
            matrices.translate((float) x, (float) y, (float) z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            float half = particle.size / 2f;
            int alpha = (int) (particle.alpha * 255);
            int r = (particle.color >> 16) & 0xFF;
            int g = (particle.color >> 8) & 0xFF;
            int b = particle.color & 0xFF;

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(r, g, b, alpha);
            buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(r, g, b, alpha);
            buffer.vertex(matrix, half, half, 0).texture(1, 0).color(r, g, b, alpha);
            buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(r, g, b, alpha);

            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    static class ParticleData {
        Vec3d position;
        Vec3d velocity;
        int color;
        float size;
        long lifeTime;
        long birthTime;
        float alpha = 1.0f;
        float smoothFactor;
        long lastUpdateNs;
        double gravity;

        ParticleData(Vec3d position, Vec3d velocity, int color, float size, long lifeTime, float smooth, double gravity) {
            this.position = position;
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.lifeTime = lifeTime;
            this.birthTime = System.currentTimeMillis();
            this.lastUpdateNs = System.nanoTime();
            this.smoothFactor = smooth;
            this.gravity = gravity;
        }

        boolean isDead() {
            return System.currentTimeMillis() - birthTime >= lifeTime;
        }

        void update(net.minecraft.client.MinecraftClient mc) {
            long nowNs = System.nanoTime();
            double deltaSec = (nowNs - lastUpdateNs) / 1_000_000_000.0;
            lastUpdateNs = nowNs;

            float progress = Math.min(1.0f, (float) (System.currentTimeMillis() - birthTime) / lifeTime);
            double factor = Math.pow(1.0 - progress, smoothFactor);

            double vx = velocity.x;
            double vy = velocity.y;
            double vz = velocity.z;

            double newX = position.x;
            double newY = position.y;
            double newZ = position.z;

            newX += vx * factor * (deltaSec * 60);
            if (!checkCollision(newX, position.y, position.z, size, mc)) {
                vx = -vx * 0.8;
                newX = position.x;
            }

            newY += vy * factor * (deltaSec * 60);
            if (!checkCollision(newX, newY, position.z, size, mc)) {
                vy = -vy * 1.5;
                newY = position.y;
            }

            newZ += vz * factor * (deltaSec * 60);
            if (!checkCollision(newX, newY, newZ, size, mc)) {
                vz = -vz * 0.8;
                newZ = position.z;
            }

            position = new Vec3d(newX, newY, newZ);
            velocity = new Vec3d(vx * 0.9999, vy * 0.9999 - gravity, vz * 0.9999);
            alpha = 1.0f - progress;
        }

        static boolean checkCollision(double x, double y, double z, float size, net.minecraft.client.MinecraftClient mc) {
            if (mc.world == null) return false;
            double half = size * 0.5;
            int minX = MathHelper.floor(x - half);
            int maxX = MathHelper.floor(x + half);
            int minY = MathHelper.floor(y - half);
            int maxY = MathHelper.floor(y + half);
            int minZ = MathHelper.floor(z - half);
            int maxZ = MathHelper.floor(z + half);

            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int bx = minX; bx <= maxX; bx++) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        pos.set(bx, by, bz);
                        BlockState state = mc.world.getBlockState(pos);
                        if (!state.isAir() && state.isSolidBlock(mc.world, pos)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }
}
