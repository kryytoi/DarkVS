package dev.darkvisuals.client.util.models;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

 
@Environment(EnvType.CLIENT)
public class GibBrain {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Vec3d prevPos;
    private Vec3d pos;
    private Vec3d motion = Vec3d.ZERO;
    private Vec3d smoothVel = Vec3d.ZERO;
    private float smoothBody;
    private float smoothHeadYaw;
    private float smoothHeadPitch;
    private double leashVelY = 0.0;

    public float prevLimbSwingAmount;
    public float limbSwingAmount;
    public float limbSwing;

    private ClientPlayerEntity entity;

    public void setEntity(ClientPlayerEntity entity) {
        this.entity = entity;
    }

    public void onUpdate() {
        if (entity == null || mc.world == null) return;

        Vec3d playerPos = entity.getPos();

        if (pos == null) {
            pos = playerPos;
            prevPos = pos;
            motion = Vec3d.ZERO;
            smoothVel = Vec3d.ZERO;
            return;
        }

        prevPos = pos;

        double distToPlayer = pos.distanceTo(playerPos);

         
        if (distToPlayer > 20.0) {
            pos = getAnchor(playerPos);
            prevPos = pos;
            motion = Vec3d.ZERO;
            smoothVel = Vec3d.ZERO;
            return;
        }

        boolean onGround = isSolidBox(pos.x, pos.y - 0.1, pos.z, 0.2);
        boolean playerHighAbove = !entity.isOnGround() && playerPos.y - pos.y > 1.5;

        if (!onGround && !playerHighAbove) {
            motion = motion.add(0.0, -0.078, 0.0);
        } else if (onGround && motion.y < 0.0) {
            motion = new Vec3d(motion.x, 0.0, motion.z);
        }

        boolean playerStill = entity.getVelocity().horizontalLengthSquared() < 9.0E-6 && entity.isOnGround();

         
        Vec3d anchor = getAnchor(playerPos);

        double distToAnchor = Math.sqrt(
                (pos.x - anchor.x) * (pos.x - anchor.x) + (pos.z - anchor.z) * (pos.z - anchor.z)
        );

        if (playerHighAbove) {
            handleFollowFlying(playerPos);
        } else {
            handleFollowAnchor(anchor, distToAnchor);
        }

        if (!playerHighAbove) {
            motion = new Vec3d(motion.x * 0.88, motion.y, motion.z * 0.88);
            pos = moveWithCollision(pos, motion);
        }

        boolean settled = distToAnchor < 0.9 && motion.horizontalLengthSquared() < 2.0E-4;

        handleRotation(playerStill && settled);
        limbTick();
    }

      
    private Vec3d getAnchor(Vec3d playerPos) {
        float yawRad = (float) Math.toRadians(entity.getYaw());
         
         
        double rx = -Math.cos(yawRad);
        double rz = -Math.sin(yawRad);
        double bx =  Math.sin(yawRad);
        double bz = -Math.cos(yawRad);
        return new Vec3d(
                playerPos.x + rx * 1.3 + bx * 0.4,
                playerPos.y,
                playerPos.z + rz * 1.3 + bz * 0.4
        );
    }

    private void handleFollowAnchor(Vec3d anchor, double distToAnchor) {
        Vec3d wantedVel;

        if (distToAnchor > 0.35) {
            double speed = distToAnchor > 6.0 ? 0.46 : (distToAnchor > 2.0 ? 0.34 : 0.22);
            Vec3d dir = new Vec3d(anchor.x - pos.x, 0.0, anchor.z - pos.z).normalize();
            wantedVel = new Vec3d(dir.x * speed, 0.0, dir.z * speed);
        } else {
            wantedVel = Vec3d.ZERO;
        }

        smoothVel = lerpVec(smoothVel, wantedVel, 0.16);
        motion = new Vec3d(smoothVel.x, motion.y, smoothVel.z);
    }

    private void handleFollowFlying(Vec3d playerPos) {
        Vec3d playerMotion = entity.getVelocity();
        Vec3d behindOffset = playerMotion.horizontalLength() > 0.02
                ? playerMotion.normalize().multiply(-1.2)
                : Vec3d.ZERO;
        Vec3d anchor = new Vec3d(playerPos.x + behindOffset.x, playerPos.y - 1.6, playerPos.z + behindOffset.z);
        double dist = pos.distanceTo(anchor);

        if (dist > 6.0) {
            pos = lerpVec(pos, anchor, 0.6);
            leashVelY = 0.0;
            motion = Vec3d.ZERO;
            smoothVel = Vec3d.ZERO;
        } else {
            double hDist = Math.sqrt(
                    (pos.x - anchor.x) * (pos.x - anchor.x) + (pos.z - anchor.z) * (pos.z - anchor.z)
            );
            double tH = hDist < 1.5 ? 0.06 : MathHelper.clamp(hDist * 0.045, 0.06, 0.18);
            double nx = pos.x + (anchor.x - pos.x) * tH;
            double nz = pos.z + (anchor.z - pos.z) * tH;
            double dy = anchor.y - pos.y;
            leashVelY -= 0.018;
            leashVelY += dy * 0.055;
            leashVelY *= 0.8;
            double ny = pos.y + leashVelY;
            pos = new Vec3d(nx, ny, nz);
            motion = Vec3d.ZERO;
            smoothVel = Vec3d.ZERO;
        }
    }

    private static Vec3d lerpVec(Vec3d from, Vec3d to, double t) {
        return new Vec3d(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t
        );
    }

    private void handleRotation(boolean standingBesidePlayer) {
        if (standingBesidePlayer && entity != null) {
             
            float targetBody = entity.getYaw();
            smoothBody = smoothBody + wrapDegrees(targetBody - smoothBody) * 0.10F;
        } else if (motion.x != 0.0 || motion.z != 0.0) {
            float targetBody = (float) Math.toDegrees(Math.atan2(-motion.x, motion.z));
            smoothBody = smoothBody + wrapDegrees(targetBody - smoothBody) * 0.15F;
        }

        Vec3d lookAt;
        if (mc.options != null && mc.options.getPerspective().isFirstPerson()) {
            lookAt = mc.player != null ? mc.player.getEyePos() : mc.gameRenderer.getCamera().getPos();
        } else {
            lookAt = mc.gameRenderer.getCamera().getPos();
        }

        Vec3d headPos = pos.add(0.0, 0.4, 0.0);
        double dx = lookAt.x - headPos.x;
        double dy = lookAt.y - headPos.y;
        double dz = lookAt.z - headPos.z;

        float worldYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetHeadPitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        float targetHeadYaw = wrapDegrees(smoothBody - worldYaw);

        targetHeadYaw = MathHelper.clamp(targetHeadYaw, -75.0F, 75.0F);
        targetHeadPitch = MathHelper.clamp(targetHeadPitch, -30.0F, 30.0F);

        smoothHeadYaw = smoothHeadYaw + (targetHeadYaw - smoothHeadYaw) * 0.18F;
        smoothHeadPitch = smoothHeadPitch + (targetHeadPitch - smoothHeadPitch) * 0.18F;
    }

    private void limbTick() {
        prevLimbSwingAmount = limbSwingAmount;
        float f = (float) motion.horizontalLength() * 4.0F;
        if (f > 1.0F) f = 1.0F;
        limbSwingAmount = limbSwingAmount + (f - limbSwingAmount) * 0.3F;
        limbSwing = limbSwing + limbSwingAmount;
    }

    private Vec3d moveWithCollision(Vec3d current, Vec3d vel) {
        double nx = current.x;
        double ny = current.y;
        double nz = current.z;

        double newY = ny + vel.y;
        if (vel.y < 0.0) {
            double groundY = findGroundBelow(nx, ny, nz, 0.5);
            if (groundY != -999.0 && newY < groundY) {
                newY = groundY;
                motion = new Vec3d(motion.x, 0.0, motion.z);
            }
        }
        ny = newY;

        double newX = nx + vel.x;
        if (isSolidBox(newX, ny + 0.01, nz, 0.25)) {
            double stepY = ny + 1.01;
            if (!isSolidBox(newX, stepY + 0.01, nz, 0.25) && !isSolidBox(newX, stepY + 0.5 - 0.01, nz, 0.25)) {
                motion = new Vec3d(motion.x, Math.max(motion.y, 0.18), motion.z);
            } else {
                newX = nx;
                motion = new Vec3d(0.0, motion.y, motion.z);
            }
        }
        nx = newX;

        double newZ = nz + vel.z;
        if (isSolidBox(nx, ny + 0.01, newZ, 0.25)) {
            double stepY = ny + 1.01;
            if (!isSolidBox(nx, stepY + 0.01, newZ, 0.25) && !isSolidBox(nx, stepY + 0.5 - 0.01, newZ, 0.25)) {
                motion = new Vec3d(motion.x, Math.max(motion.y, 0.18), motion.z);
            } else {
                newZ = nz;
                motion = new Vec3d(motion.x, motion.y, 0.0);
            }
        }
        nz = newZ;

        double groundSnap = findGroundBelow(nx, ny + 0.3, nz, 0.5);
        if (groundSnap != -999.0 && ny < groundSnap) {
            ny += Math.min(groundSnap - ny, 0.15);
            if (motion.y < 0.0) {
                motion = new Vec3d(motion.x, 0.0, motion.z);
            }
        }

        return new Vec3d(nx, ny, nz);
    }

    private double findGroundBelow(double x, double y, double z, double height) {
        if (mc.world == null) return -999.0;
        for (double dy = 0.0; dy <= 4.0; dy += 0.5) {
            double checkY = y - dy;
            BlockPos bp = BlockPos.ofFloored(x, checkY - 0.1, z);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return bp.getY() + 1.0;
            }
        }
        return -999.0;
    }

    private boolean isSolidBox(double x, double y, double z, double w) {
        if (mc.world == null) return false;
        double[] offsets = {-w, w};
        for (double dx : offsets) {
            for (double dz : offsets) {
                BlockPos bp = BlockPos.ofFloored(x + dx, y, z + dz);
                if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static float wrapDegrees(float deg) {
        deg %= 360.0F;
        if (deg >= 180.0F) deg -= 360.0F;
        if (deg < -180.0F) deg += 360.0F;
        return deg;
    }

    public Vec3d getPos(float tickDelta) {
        if (prevPos != null && pos != null) {
            return new Vec3d(
                    MathHelper.lerp(tickDelta, prevPos.x, pos.x),
                    MathHelper.lerp(tickDelta, prevPos.y, pos.y),
                    MathHelper.lerp(tickDelta, prevPos.z, pos.z)
            );
        }
        return pos != null ? pos : Vec3d.ZERO;
    }

    public float getBody()  { return smoothBody; }
    public float getYaw()   { return smoothHeadYaw; }
    public float getPitch() { return smoothHeadPitch; }

    public double getVelocityY() { return motion.y; }
}
