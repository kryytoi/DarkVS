package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Blink extends Module {

    private final BooleanSetting onWalk = new BooleanSetting("При ходьбе", true);
    private final BooleanSetting onJump = new BooleanSetting("При прыжке", true);
    private final ListSetting trigger = new ListSetting("Триггер", false, onWalk, onJump);

    private final NumberSetting duration = new NumberSetting("Длительность призрака", 2.0f, 0.5f, 6.0f, 0.5f);
    private final NumberSetting ghostAlpha = new NumberSetting("Прозрачность заливки призрака", 0.5f, 0.05f, 1.0f, 0.05f);

    private final List<GhostSnapshot> snapshots = new CopyOnWriteArrayList<>();
    private Vec3d lastSpawnPos = null;
    private boolean wasInAir = false;

    public Blink() {
        super("Blink", Category.Render, "Показывает призрачную модель игрока при ходьбе или прыжке");
        getSettings().add(trigger);
        getSettings().add(duration);
        getSettings().add(ghostAlpha);
    }

    @Override
    public void onDisable() {
        snapshots.clear();
        lastSpawnPos = null;
        wasInAir = false;
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck() || mc.player == null) return;

        long now = System.currentTimeMillis();
        long maxLifeMs = (long) (duration.getValue() * 1000L);
        snapshots.removeIf(s -> now - s.timestamp > maxLifeMs);

        Vec3d currentPos = mc.player.getPos();
        boolean isMoving = mc.player.getVelocity().horizontalLengthSquared() > 0.002 ||
                (mc.player.input != null && (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0));

        boolean isJumping = !mc.player.isOnGround() && mc.player.getVelocity().y > 0.08;

        boolean shouldSpawn = false;

        if (onJump.getValue() && isJumping && !wasInAir) {
            shouldSpawn = true;
        }

        if (onWalk.getValue() && isMoving && mc.player.isOnGround()) {
            if (lastSpawnPos == null || currentPos.distanceTo(lastSpawnPos) >= 1.2) {
                shouldSpawn = true;
            }
        }

        wasInAir = !mc.player.isOnGround();

        if (shouldSpawn) {
            lastSpawnPos = currentPos;
            snapshots.add(new GhostSnapshot(
                    currentPos,
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.bodyYaw,
                    now
            ));

            if (snapshots.size() > 16) {
                snapshots.remove(0);
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck() || mc.player == null || mc.world == null || snapshots.isEmpty()) return;

        long now = System.currentTimeMillis();
        long maxLifeMs = (long) (duration.getValue() * 1000L);
        float baseAlpha = ghostAlpha.getValue().floatValue();

        ThemeManager tm = ThemeManager.getInstance();
        Color themeColor = tm != null && tm.getCurrentTheme() != null ? tm.getCurrentTheme().getAccentColor() : new Color(180, 100, 255);
        float r = themeColor.getRed() / 255f;
        float g = themeColor.getGreen() / 255f;
        float b = themeColor.getBlue() / 255f;

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        MatrixStack matrices = event.getMatrices();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        for (GhostSnapshot snapshot : snapshots) {
            long age = now - snapshot.timestamp;
            if (age >= maxLifeMs) continue;

            float lifeRatio = 1.0f - ((float) age / (float) maxLifeMs);
            float currentAlpha = Math.max(0.01f, Math.min(1.0f, baseAlpha * lifeRatio));

            matrices.push();
            matrices.translate(snapshot.pos.x - cameraPos.x, snapshot.pos.y - cameraPos.y, snapshot.pos.z - cameraPos.z);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(r, g, b, currentAlpha);

            BlockPos lightPos = BlockPos.ofFloored(snapshot.pos.x, snapshot.pos.y, snapshot.pos.z);
            int light = LightmapTextureManager.pack(
                    mc.world.getLightLevel(LightType.BLOCK, lightPos),
                    mc.world.getLightLevel(LightType.SKY, lightPos)
            );

            float prevYaw = mc.player.getYaw();
            float prevPitch = mc.player.getPitch();
            float prevBodyYaw = mc.player.bodyYaw;
            float prevPrevBodyYaw = mc.player.prevBodyYaw;
            float prevHeadYaw = mc.player.headYaw;
            float prevPrevHeadYaw = mc.player.prevHeadYaw;

            mc.player.setYaw(snapshot.yaw);
            mc.player.setPitch(snapshot.pitch);
            mc.player.bodyYaw = snapshot.bodyYaw;
            mc.player.prevBodyYaw = snapshot.bodyYaw;
            mc.player.headYaw = snapshot.yaw;
            mc.player.prevHeadYaw = snapshot.yaw;

            try {
                dispatcher.render(mc.player, 0.0, 0.0, 0.0, 1.0f, matrices, immediate, light);
                immediate.draw();
            } catch (Throwable ignored) {
            } finally {
                mc.player.setYaw(prevYaw);
                mc.player.setPitch(prevPitch);
                mc.player.bodyYaw = prevBodyYaw;
                mc.player.prevBodyYaw = prevPrevBodyYaw;
                mc.player.headYaw = prevHeadYaw;
                mc.player.prevHeadYaw = prevPrevHeadYaw;
            }

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            matrices.pop();
        }
    }

    private static final class GhostSnapshot {
        final Vec3d pos;
        final float yaw;
        final float pitch;
        final float bodyYaw;
        final long timestamp;

        GhostSnapshot(Vec3d pos, float yaw, float pitch, float bodyYaw, long timestamp) {
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
            this.bodyYaw = bodyYaw;
            this.timestamp = timestamp;
        }
    }
}
