package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.ChatUtils;
import dev.darkvisuals.client.events.impl.EventKey;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.impl.BindSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class Sigara extends Module {

    private final BindSetting smokeBind = new BindSetting("Вкурнуть", new Bind(-1, false));
    private final BindSetting dropBind = new BindSetting("Выкинуть", new Bind(-1, false));
    private final BindSetting mouthBind = new BindSetting("Положить в рот", new Bind(-1, false));

    public enum CigState {
        NONE,
        PUT_IN_MOUTH,
        IN_MOUTH,
        SMOKING,
        DROPPED
    }

    private CigState state = CigState.NONE;
    private long actionStartTime = 0L;
    private Vec3d dropStartPos = null;
    private Vec3d dropVelocity = null;
    private double groundY = 0.0;
    private boolean stomped = false;
    private final Random random = new Random();

    public Sigara() {
        super("Sigara", Category.Render, "3D Сигарета с интерактивными анимациями и дымом");
        getSettings().add(smokeBind);
        getSettings().add(dropBind);
        getSettings().add(mouthBind);
    }

    @Override
    public void onDisable() {
        state = CigState.NONE;
        dropStartPos = null;
        dropVelocity = null;
        stomped = false;
        super.onDisable();
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (!isToggled() || fullNullCheck() || mc.currentScreen != null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        int pressedKey = event.getKey();

        if (matches(smokeBind.getValue(), pressedKey)) {
            startSmoking();
        } else if (matches(mouthBind.getValue(), pressedKey)) {
            putInMouth();
        } else if (matches(dropBind.getValue(), pressedKey)) {
            dropAndStomp();
        }
    }

    private boolean matches(Bind bind, int key) {
        return bind != null && !bind.isEmpty() && !bind.isMouse() && bind.getKey() == key;
    }

    private void startSmoking() {
        state = CigState.SMOKING;
        actionStartTime = System.currentTimeMillis();
        if (mc.player != null && mc.world != null) {
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 0.9f, 1.2f);
            ChatUtils.sendMessage("§e[Sigara] §fВы достали зажигалку и закурили...");
        }
    }

    private void putInMouth() {
        state = CigState.PUT_IN_MOUTH;
        actionStartTime = System.currentTimeMillis();
        if (mc.player != null && mc.world != null) {
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.PLAYERS, 0.8f, 1.4f);
            ChatUtils.sendMessage("§e[Sigara] §fВы достали пачку и положили сигарету в рот.");
        }
    }

    private void dropAndStomp() {
        if (state == CigState.NONE) return;
        state = CigState.DROPPED;
        actionStartTime = System.currentTimeMillis();
        stomped = false;
        if (mc.player != null && mc.world != null) {
            mc.player.swingHand(Hand.MAIN_HAND);
            dropStartPos = mc.player.getEyePos().add(mc.player.getRotationVector().multiply(0.2));
            dropVelocity = mc.player.getRotationVector().multiply(2.2).add(0.0, 1.2, 0.0);
            groundY = mc.player.getY();
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.7f, 1.5f);
            ChatUtils.sendMessage("§e[Sigara] §fСигарета брошена и растоптана.");
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck() || mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();

        if (state == CigState.PUT_IN_MOUTH) {
            long elapsed = now - actionStartTime;
            if (elapsed >= 1400) {
                state = CigState.IN_MOUTH;
            }
        } else if (state == CigState.SMOKING) {
            long elapsed = now - actionStartTime;
            float p = MathHelper.clamp(elapsed / 3000f, 0f, 1f);

            if (p < 0.35f) {
                if (random.nextInt(3) == 0) {
                    Vec3d tip = getCigaretteTipPosition();
                    mc.world.addParticle(ParticleTypes.FLAME, tip.x, tip.y, tip.z,
                            (random.nextDouble() - 0.5) * 0.01, 0.02, (random.nextDouble() - 0.5) * 0.01);
                }
            } else if (p >= 0.55f && p < 0.95f) {
                Vec3d mouth = mc.player.getEyePos().add(0, -0.1, 0);
                Vec3d look = mc.player.getRotationVector();
                Vec3d spawn = mouth.add(look.multiply(0.25));
                for (int i = 0; i < 2; i++) {
                    mc.world.addParticle(
                            ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            spawn.x + (random.nextDouble() - 0.5) * 0.04,
                            spawn.y + (random.nextDouble() - 0.5) * 0.04,
                            spawn.z + (random.nextDouble() - 0.5) * 0.04,
                            look.x * 0.09 + (random.nextDouble() - 0.5) * 0.03,
                            look.y * 0.05 + 0.03 + random.nextDouble() * 0.02,
                            look.z * 0.09 + (random.nextDouble() - 0.5) * 0.03
                    );
                }
            } else if (p >= 1.0f) {
                state = CigState.IN_MOUTH;
            }
        } else if (state == CigState.IN_MOUTH) {
            if (random.nextInt(15) == 0) {
                Vec3d tip = getCigaretteTipPosition();
                mc.world.addParticle(ParticleTypes.SMOKE, tip.x, tip.y, tip.z, 0.0, 0.02, 0.0);
            }
        } else if (state == CigState.DROPPED) {
            long elapsed = now - actionStartTime;
            if (elapsed >= 900 && !stomped) {
                stomped = true;
                if (dropStartPos != null) {
                    mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.BLOCK_GRAVEL_STEP, SoundCategory.PLAYERS, 1.0f, 0.7f);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    for (int i = 0; i < 8; i++) {
                        mc.world.addParticle(ParticleTypes.SMOKE,
                                mc.player.getX() + (random.nextDouble() - 0.5) * 0.4,
                                groundY + 0.05,
                                mc.player.getZ() + (random.nextDouble() - 0.5) * 0.4,
                                (random.nextDouble() - 0.5) * 0.05, 0.02, (random.nextDouble() - 0.5) * 0.05);
                    }
                }
            }
            if (elapsed > 2400) {
                state = CigState.NONE;
                dropStartPos = null;
                dropVelocity = null;
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck() || mc.player == null) return;
        if (state == CigState.NONE) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();

        boolean firstPerson = mc.options.getPerspective().isFirstPerson();

        if (firstPerson && state != CigState.DROPPED) {
            float camYaw = mc.gameRenderer.getCamera().getYaw();
            float camPitch = mc.gameRenderer.getCamera().getPitch();

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

            if (state == CigState.PUT_IN_MOUTH) {
                float p = MathHelper.clamp((now - actionStartTime) / 1400f, 0f, 1f);
                if (p < 0.45f) {
                    float packProg = p / 0.45f;
                    float packY = MathHelper.lerp(packProg, -0.45f, -0.20f);

                    matrices.push();
                    matrices.translate(0.18f, packY, -0.38f);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25f));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15f));
                    render3DPack(matrices);

                    matrices.push();
                    matrices.translate(0.005f, 0.035f + packProg * 0.035f, 0f);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
                    render3DCigarette(matrices, false);
                    matrices.pop();

                    matrices.pop();
                } else {
                    float cigProg = (p - 0.45f) / 0.55f;
                    float cx = MathHelper.lerp(cigProg, 0.18f, 0.12f);
                    float cy = MathHelper.lerp(cigProg, -0.16f, -0.11f);
                    float cz = MathHelper.lerp(cigProg, -0.38f, -0.32f);

                    matrices.push();
                    matrices.translate(cx, cy, cz);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(cigProg, 70f, 82f)));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-15f));
                    render3DCigarette(matrices, false);
                    matrices.pop();
                }
            } else if (state == CigState.SMOKING) {
                float p = MathHelper.clamp((now - actionStartTime) / 3000f, 0f, 1f);
                boolean inhaling = (p >= 0.25f && p < 0.65f);
                float inhaleBob = inhaling ? (float) Math.sin((p - 0.25f) / 0.4f * Math.PI) * 0.008f : 0f;

                matrices.push();
                matrices.translate(0.12f, -0.11f + inhaleBob, -0.32f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(82f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-15f + (inhaling ? 6f : 0f)));
                render3DCigarette(matrices, inhaling);
                matrices.pop();
            } else if (state == CigState.IN_MOUTH) {
                float idle = (float) Math.sin(now / 350.0) * 0.003f;
                matrices.push();
                matrices.translate(0.12f, -0.11f + idle, -0.32f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(82f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-15f + idle * 80f));
                render3DCigarette(matrices, false);
                matrices.pop();
            }

            matrices.pop();
        } else if (state == CigState.DROPPED && dropStartPos != null && dropVelocity != null) {
            float t = (now - actionStartTime) / 1000f;
            double curX = dropStartPos.x + dropVelocity.x * t;
            double curY = dropStartPos.y + dropVelocity.y * t - 0.5 * 7.5 * t * t;
            double curZ = dropStartPos.z + dropVelocity.z * t;

            if (curY <= groundY + 0.02) {
                curY = groundY + 0.02;
            }

            matrices.push();
            matrices.translate(curX - camPos.x, curY - camPos.y, curZ - camPos.z);
            if (curY > groundY + 0.03) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 540f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(t * 720f));
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35f));
            }
            render3DCigarette(matrices, false);
            matrices.pop();
        } else {
            Vec3d mouth = getMouthPosition();
            matrices.push();
            matrices.translate(mouth.x - camPos.x, mouth.y - camPos.y, mouth.z - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.player.getYaw() - 90f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mc.player.getPitch() * 0.4f + 12f));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15f));
            render3DCigarette(matrices, state == CigState.SMOKING);
            matrices.pop();
        }
    }

    private Vec3d getMouthPosition() {
        Vec3d eye = mc.player.getEyePos();
        float yaw = mc.player.getYaw();
        Vec3d forward = mc.player.getRotationVector();
        float rad = (float) Math.toRadians(yaw + 90);
        Vec3d right = new Vec3d(-Math.sin(rad), 0, Math.cos(rad));

        return eye.add(0, -0.12, 0)
                .add(forward.multiply(0.22))
                .add(right.multiply(0.08));
    }

    private Vec3d getCigaretteTipPosition() {
        Vec3d mouth = getMouthPosition();
        Vec3d forward = mc.player.getRotationVector();
        return mouth.add(forward.multiply(0.25));
    }

    private void render3DPack(MatrixStack matrices) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        drawBox(buf, m, -0.025f, -0.04f, -0.012f, 0.025f, 0.015f, 0.012f, 220, 220, 230, 255);
        drawBox(buf, m, -0.025f, 0.015f, -0.012f, 0.025f, 0.04f, 0.012f, 200, 40, 45, 255);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawBox(BufferBuilder buf, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
        buf.vertex(m, x2, y1, z1).color(r, g, b, a);
        buf.vertex(m, x2, y2, z1).color(r, g, b, a);
        buf.vertex(m, x1, y2, z1).color(r, g, b, a);

        buf.vertex(m, x1, y1, z2).color(r, g, b, a);
        buf.vertex(m, x1, y2, z2).color(r, g, b, a);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a);
        buf.vertex(m, x2, y1, z2).color(r, g, b, a);

        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
        buf.vertex(m, x1, y1, z2).color(r, g, b, a);
        buf.vertex(m, x2, y1, z2).color(r, g, b, a);
        buf.vertex(m, x2, y1, z1).color(r, g, b, a);

        buf.vertex(m, x1, y2, z1).color(r, g, b, a);
        buf.vertex(m, x2, y2, z1).color(r, g, b, a);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a);
        buf.vertex(m, x1, y2, z2).color(r, g, b, a);

        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
        buf.vertex(m, x1, y2, z1).color(r, g, b, a);
        buf.vertex(m, x1, y2, z2).color(r, g, b, a);
        buf.vertex(m, x1, y1, z2).color(r, g, b, a);

        buf.vertex(m, x2, y1, z1).color(r, g, b, a);
        buf.vertex(m, x2, y1, z2).color(r, g, b, a);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a);
        buf.vertex(m, x2, y2, z1).color(r, g, b, a);
    }

    private void render3DCigarette(MatrixStack matrices, boolean litPuff) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float r = 0.012f;
        float fLen = 0.06f;
        float bLen = 0.18f;
        float total = fLen + bLen;

        drawCylinder(buf, m, 0f, fLen, r, 215, 140, 50, 255);
        drawCylinder(buf, m, fLen, total, r, 240, 240, 245, 255);

        int emberR = litPuff ? 255 : 220;
        int emberG = litPuff ? 130 : 50;
        int emberB = litPuff ? 30 : 15;
        drawCircle(buf, m, total, r, emberR, emberG, emberB, 255);
        drawCircle(buf, m, 0f, r, 210, 130, 45, 255);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawCylinder(BufferBuilder buf, Matrix4f m, float startX, float endX, float rad, int red, int green, int blue, int alpha) {
        int segs = 8;
        float step = (float) (Math.PI * 2.0 / segs);
        for (int i = 0; i < segs; i++) {
            float a1 = i * step;
            float a2 = (i + 1) * step;
            float y1 = MathHelper.cos(a1) * rad;
            float z1 = MathHelper.sin(a1) * rad;
            float y2 = MathHelper.cos(a2) * rad;
            float z2 = MathHelper.sin(a2) * rad;

            buf.vertex(m, startX, y1, z1).color(red, green, blue, alpha);
            buf.vertex(m, endX, y1, z1).color(red, green, blue, alpha);
            buf.vertex(m, endX, y2, z2).color(red, green, blue, alpha);
            buf.vertex(m, startX, y2, z2).color(red, green, blue, alpha);
        }
    }

    private void drawCircle(BufferBuilder buf, Matrix4f m, float x, float rad, int red, int green, int blue, int alpha) {
        int segs = 8;
        float step = (float) (Math.PI * 2.0 / segs);
        for (int i = 0; i < segs; i++) {
            float a1 = i * step;
            float a2 = (i + 1) * step;
            buf.vertex(m, x, 0f, 0f).color(red, green, blue, alpha);
            buf.vertex(m, x, MathHelper.cos(a1) * rad, MathHelper.sin(a1) * rad).color(red, green, blue, alpha);
            buf.vertex(m, x, MathHelper.cos(a2) * rad, MathHelper.sin(a2) * rad).color(red, green, blue, alpha);
            buf.vertex(m, x, 0f, 0f).color(red, green, blue, alpha);
        }
    }
}
