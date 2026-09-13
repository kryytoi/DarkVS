package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.events.impl.EventThemeChanged;
import dev.darkvisuals.client.managers.FriendsManager;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.managers.ThemeManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.resource.language.I18n;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class JumpCircle extends Module implements ThemeManager.ThemeChangeListener {

     
    private final NumberSetting size = new NumberSetting("Circle Size", 0.5f, 0.2f, 2.0f, 0.05f);
    private final NumberSetting Life_Time = new NumberSetting("Life Time", 0.5f, 0.2f, 2.0f, 0.05f);
    private final NumberSetting speed = new NumberSetting("Speed", 1.0f, 0.5f, 3.0f, 0.1f);
    private final BooleanSetting showFriends = new BooleanSetting("setting.showFriends", true);

     
    private final EnumSetting<JumpCircleMode> mode =
            new EnumSetting<>("Режим", JumpCircleMode.JAGGED_RING);

     
    private final BooleanSetting animGrow = new BooleanSetting("Grow", true, () -> false);
    private final BooleanSetting animPulse = new BooleanSetting("Pulse", false, () -> false);
    private final BooleanSetting animRipple = new BooleanSetting("Ripple", false, () -> false);
    private final ListSetting animationMode = new ListSetting("Animation", true, animGrow, animPulse, animRipple);

     
    private final BooleanSetting novaRotate = new BooleanSetting("Nova Rotate", true, () -> mode.getValue() == JumpCircleMode.NOVA);

     
    private final Identifier texJaggedRing = darkvisuals.id("textures/circle_jagged_ring.png");
    private final Identifier texThickRing  = darkvisuals.id("textures/circle_thick_ring.png");
    private final Identifier texOctagon    = darkvisuals.id("textures/circle_octagon.png");
    private final Identifier texLattice    = darkvisuals.id("textures/circle_lattice.png");
    private final Identifier texNova       = darkvisuals.id("textures/circle_nova.png");

    private final List<Circle> circles = new CopyOnWriteArrayList<>();
    private final ThemeManager themeManager;
    private Color currentColor;

    private boolean wasOnGround = true;
    private long lastJumpTime = 0;

    private final Map<UUID, Boolean> friendWasOnGround = new HashMap<>();
    private final Map<UUID, Long> friendLastJumpTime = new HashMap<>();

    public JumpCircle() {
        super("JumpCircle", Category.Render, I18n.translate("module.jumpcircle.description"));
        getSettings().add(mode);
        getSettings().add(size);
        getSettings().add(Life_Time);
        getSettings().add(speed);
        getSettings().add(showFriends);
        getSettings().add(animationMode);
        getSettings().add(novaRotate);
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
    public void onThemeChanged(EventThemeChanged event) {
        this.currentColor = event.getTheme().getBackgroundColor();
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        circles.clear();
        friendWasOnGround.clear();
        friendLastJumpTime.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        boolean onGround = p.isOnGround();
        long currentTime = System.currentTimeMillis();
        double velocityY = p.getVelocity().y;

        if (wasOnGround && !onGround && velocityY > 0.01 && (currentTime - lastJumpTime) > 50) {
            spawnCircle(p);
            lastJumpTime = currentTime;
        }
        wasOnGround = onGround;

        if (showFriends.getValue()) {
            for (PlayerEntity other : mc.world.getPlayers()) {
                if (other == p) continue;
                if (!FriendsManager.checkFriend(other.getGameProfile().getName())) continue;

                UUID id = other.getUuid();
                boolean otherOnGround = other.isOnGround();
                boolean otherWasOnGround = friendWasOnGround.getOrDefault(id, true);
                long otherLastJump = friendLastJumpTime.getOrDefault(id, 0L);
                double otherVelY = other.getY() - other.prevY;

                if (otherWasOnGround && !otherOnGround && otherVelY > 0.01 && (currentTime - otherLastJump) > 50) {
                    spawnCircle(other);
                    friendLastJumpTime.put(id, currentTime);
                }
                friendWasOnGround.put(id, otherOnGround);
            }
        }
    }

    private void spawnCircle(PlayerEntity player) {
        BlockPos blockPos = player.getBlockPos();
        double y = blockPos.getY() + 0.1;
        Vec3d origin = new Vec3d(player.getX(), y, player.getZ());
        circles.add(new Circle(origin, (long) (Life_Time.getValue() * 1000L)));
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        try (var __ = Perf.scopeCpu("JumpCircle.onRender3D")) {
            long now = System.currentTimeMillis();
            circles.removeIf(c -> (now - c.spawnTime) > c.ttl);

            Identifier texture = getTextureForMode();
            JumpCircleMode currentMode = mode.getValue();

            for (Circle c : circles) {
                long age = now - c.spawnTime;
                float t = Math.max(0f, Math.min(1f, (age / (float) c.ttl) * speed.getValue()));

                 
                float radiusMul;
                float alphaK;
                if (animPulse.getValue()) {
                    float pulses = 2.0f;
                    float s = (float) Math.sin(Math.PI * pulses * t);
                    radiusMul = 0.7f + 0.5f * Math.max(0f, s);
                    alphaK = Math.max(0f, s) * (1.0f - t);
                } else if (animRipple.getValue()) {
                    float te = Easing.EASE_OUT_CIRC.apply(Math.max(0f, Math.min(1f, t)));
                    radiusMul = 0.6f + 0.8f * te;
                    alphaK = (float) Math.pow(Math.sin(Math.max(0f, Math.min(1f, t)) * (float) Math.PI), 1.2f);
                } else {
                     
                    float te = Easing.BOTH_SINE.apply(t);
                    radiusMul = 0.6f + 0.4f * te;
                    alphaK = (float) Math.pow(1.0f - t, 0.8f) * te;
                }

                alphaK = Math.max(0f, Math.min(1f, alphaK));
                int alpha = (int) (255 * alphaK);
                if (alpha <= 2) continue;

                float r = size.getValue() * radiusMul;
                // у пика анимации круг вспыхивает акцентным цветом темы
                Color bg = themeManager.getCurrentTheme().getBackgroundColor();
                Color ac = themeManager.getCurrentTheme().getAccentColor();
                float blend = alphaK * 0.85f;
                Color drawColor = new Color(
                        (int) (bg.getRed() + (ac.getRed() - bg.getRed()) * blend),
                        (int) (bg.getGreen() + (ac.getGreen() - bg.getGreen()) * blend),
                        (int) (bg.getBlue() + (ac.getBlue() - bg.getBlue()) * blend),
                        alpha);

                MatrixStack matrices = e.getMatrices();
                matrices.push();
                matrices.translate(
                        c.origin.x - mc.getEntityRenderDispatcher().camera.getPos().x,
                        c.origin.y - mc.getEntityRenderDispatcher().camera.getPos().y,
                        c.origin.z - mc.getEntityRenderDispatcher().camera.getPos().z
                );

                 
                if (currentMode == JumpCircleMode.NOVA && novaRotate.getValue()) {
                    float rotSpeed = 90f;  
                    float angle = (age / 1000f) * rotSpeed;
                    matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                }

                Render3D.drawTextureVivid(
                        matrices,
                        -r, 0, -r,
                        r, 0, r,
                        texture,
                        drawColor,
                        1.0f
                );

                matrices.pop();
            }
        }
    }

    private Identifier getTextureForMode() {
        return switch (mode.getValue()) {
            case JAGGED_RING -> texJaggedRing;
            case THICK_RING  -> texThickRing;
            case OCTAGON     -> texOctagon;
            case LATTICE     -> texLattice;
            case NOVA        -> texNova;
        };
    }

    private static class Circle {
        final Vec3d origin;
        final long spawnTime;
        final long ttl;

        Circle(Vec3d origin, long ttl) {
            this.origin = origin;
            this.spawnTime = System.currentTimeMillis();
            this.ttl = ttl;
        }
    }
}
