package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventThemeChanged;
import dev.darkvisuals.client.managers.FriendsManager;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.cosmetics.CosmeticsScreen;
import dev.darkvisuals.client.util.models.BlackWings;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.util.models.CapModel;
import dev.darkvisuals.client.util.models.KaguneModel;
import dev.darkvisuals.client.util.models.RobotTentaclesModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Set;

public final class Cosmetics extends Module implements ThemeManager.ThemeChangeListener {

    private static final float PI_STEP = (float) (Math.PI * 2f / 180f);
    private static final float WING_SCALE = 1.0f;
    private static final float FLAP_SPEED = 1.6f;
    private static final float FLAP_AMPLITUDE = 25f;
    private static final int NIMBUS_ARMS = 2;
    private static final int NIMBUS_SEGMENTS = 17;
    private static final float NIMBUS_RADIUS = 0.45f;
    private static final float NIMBUS_BASE_SIZE = 0.23f;
    private static final double NIMBUS_STEP_RADIANS = 0.11;
    private static final int NIMBUS_MAX_ALPHA = 255;
    private static final int NIMBUS_ALPHA_FALLOFF = 9;
    private static final float NIMBUS_SPEED = 170.0f;

    private static final ClassicWingPoint[] CLASSIC_WING_SHAPE = {
            new ClassicWingPoint(0.08f, 0.10f, 0.88f),
            new ClassicWingPoint(0.28f, 0.34f, 0.78f),
            new ClassicWingPoint(0.56f, 0.82f, 0.62f),
            new ClassicWingPoint(0.86f, 0.30f, 0.52f),
            new ClassicWingPoint(1.14f, 0.46f, 0.40f),
            new ClassicWingPoint(1.24f, 0.04f, 0.30f),
            new ClassicWingPoint(1.02f, -0.18f, 0.28f),
            new ClassicWingPoint(1.18f, -0.64f, 0.22f),
            new ClassicWingPoint(0.86f, -0.46f, 0.20f),
            new ClassicWingPoint(0.80f, -0.98f, 0.14f),
            new ClassicWingPoint(0.54f, -0.74f, 0.16f),
            new ClassicWingPoint(0.30f, -1.16f, 0.12f),
            new ClassicWingPoint(0.10f, -0.54f, 0.18f)
    };

    private final ButtonSetting openMenu =
            new ButtonSetting("Открыть меню", () -> mc.setScreen(new CosmeticsScreen(this)));

    private final BooleanSetting nimbus = new BooleanSetting("Нимб", true);
    private final BooleanSetting wings = new BooleanSetting("Крылья", true);
    private final BooleanSetting wings2 = new BooleanSetting("Крылья 2", false);
    private final BooleanSetting blackWings = new BooleanSetting("Чёрный крылья", false);

    private final BooleanSetting butterflyWingAnimation = new BooleanSetting("Анимация крыльев", true, () -> wings.getValue());
    private final NumberSetting butterflyWingSize = new NumberSetting("Размер", 1.0f, 0.65f, 1.8f, 0.05f);
    private final BooleanSetting classicWingAnimation = new BooleanSetting("Анимация крыльев 2", true, () -> wings2.getValue());
    private final NumberSetting classicWingSize = new NumberSetting("Размер 2", 1.0f, 0.65f, 1.8f, 0.05f);
    private final BooleanSetting blackWingAnimation = new BooleanSetting("Анимация чёрных крыльев", true, () -> blackWings.getValue());
    private final NumberSetting blackWingSize = new NumberSetting("Размер чёрных", 1.0f, 0.65f, 1.8f, 0.05f);
     
    private final BooleanSetting cap = new BooleanSetting("Кепка", false, () -> false);
    private final BooleanSetting chinaHat = new BooleanSetting("Шляпа", false, () -> false);
    private final BooleanSetting kagune = new BooleanSetting("Кагуне", false, () -> false);
    private final BooleanSetting mortyPatch = new BooleanSetting("Повязка Морти", false, () -> false);
    private final BooleanSetting nikeCap = new BooleanSetting("Кепка Nike", false, () -> false);
    private final BooleanSetting robotTentacles = new BooleanSetting("Робо-щупальца", false, () -> false);

    private final NumberSetting robotTentacleSize = new NumberSetting("Размер робо-щупалец", 1.0f, 0.5f, 2.0f, 0.05f);
    private final NumberSetting robotTentacleSpeed = new NumberSetting("Скорость робо-щупалец", 1.0f, 0.1f, 3.0f, 0.05f);
    private final BooleanSetting robotTentacleAnimation = new BooleanSetting("Анимация робо-щупалец", true, () -> robotTentacles.getValue());

     
    private final NumberSetting kaguneSize = new NumberSetting("Размер кагуне", 1.0f, 0.5f, 2.0f, 0.05f);
    private final NumberSetting kaguneSpeed = new NumberSetting("Скорость кагуне", 1.0f, 0.1f, 3.0f, 0.05f);
    private final BooleanSetting kaguneAnimation = new BooleanSetting("Анимация кагуне", true, () -> kagune.getValue());

     
    private final NumberSetting chinaBrimRadius = new NumberSetting("Радиус шляпы", 0.7f, 0.3f, 1.4f, 0.05f);
    private final NumberSetting chinaOpacity = new NumberSetting("Прозрачность шляпы", 0.65f, 0.0f, 1.0f, 0.01f);

     
    private final CapModel capModel = new CapModel();
    private float propellerRotation;
    private float propellerSpeed;
    private float kaguneAnimTime;
    private float robotTentacleAnimTime;

    private float selfClassicBodyYaw;
    private boolean selfClassicBodyYawInitialized;

     
     
     
     
    private volatile String previewOverrideId;

    public void setPreviewOverride(String id) { this.previewOverrideId = id; }
    public void clearPreviewOverride() { this.previewOverrideId = null; }
     
 
 
 
    private boolean guiPreview = false;
    private float guiPreviewYaw = 0f;

    public void renderCosmeticForGui(String id, PlayerEntity player, MatrixStack matrices, float bodyYaw) {
        guiPreview = true;
        guiPreviewYaw = bodyYaw;

        float prevBody = player.prevBodyYaw;
        float body = player.bodyYaw;
        player.prevBodyYaw = bodyYaw;
        player.bodyYaw = bodyYaw;

        RenderSystem.disableCull();  
        try {
            switch (id) {
                case "nimbus" -> renderNimbusPreview(matrices, player);
                case "w1" -> renderButterflyWings(player, 1.0f, matrices, player.getPos());
                case "w2" -> renderClassicWings(player, 1.0f, matrices, player.getPos());
                case "black" -> BlackWings.render(player, 1.0f, matrices, player.getPos(),
                        getBlackWingSize(), isBlackWingAnimated());
                case "cap" -> renderCap(player, 1.0f, matrices, player.getPos());
                case "china" -> renderChinaHat(player, 1.0f, matrices, player.getPos());
                case "kagune" -> KaguneModel.render(player, 1.0f, matrices, player.getPos(),
                        kaguneAnimTime, kaguneSize.getValue().floatValue(), kaguneAnimation.getValue());
                case "morty" -> renderMortyPatch(player, 1.0f, matrices, player.getPos());
                case "nike" -> renderNikeCap(player, 1.0f, matrices, player.getPos());
                case "robot_tentacles" -> RobotTentaclesModel.render(player, 1.0f, matrices, player.getPos(),
                        robotTentacleAnimTime, robotTentacleSize.getValue().floatValue(), robotTentacleAnimation.getValue());
            }
        } finally {
            RenderSystem.enableCull();
            player.prevBodyYaw = prevBody;
            player.bodyYaw = body;
            guiPreview = false;
        }
    }

    private final ThemeManager themeManager;
    private Color currentColor;

    public Cosmetics() {
        super("Cosmetics", Category.Render, "Визуальные украшения");
        getSettings().add(openMenu);
         
        getSettings().add(kaguneSize);
        getSettings().add(kaguneSpeed);
        getSettings().add(kaguneAnimation);
        getSettings().add(robotTentacleSize);
        getSettings().add(robotTentacleSpeed);
        getSettings().add(robotTentacleAnimation);
        getSettings().add(chinaBrimRadius);
        getSettings().add(chinaOpacity);

         
        nimbus.setVisible(() -> false);
        wings.setVisible(() -> false);
        wings2.setVisible(() -> false);
        blackWings.setVisible(() -> false);
        cap.setVisible(() -> false);
        chinaHat.setVisible(() -> false);
        mortyPatch.setVisible(() -> false);
        nikeCap.setVisible(() -> false);
        robotTentacles.setVisible(() -> false);
        kagune.setVisible(() -> false);
        robotTentacleSize.setVisible(() -> robotTentacles.getValue());
        robotTentacleSpeed.setVisible(() -> robotTentacles.getValue());
        butterflyWingSize.setVisible(() -> wings.getValue());
        classicWingSize.setVisible(() -> wings2.getValue());
        blackWingSize.setVisible(() -> blackWings.getValue());
        kaguneSize.setVisible(() -> kagune.getValue());
        kaguneSpeed.setVisible(() -> kagune.getValue());
        chinaBrimRadius.setVisible(() -> chinaHat.getValue());
        chinaOpacity.setVisible(() -> chinaHat.getValue());

        this.themeManager = ThemeManager.getInstance();
        this.currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);
    }

     
    public BooleanSetting getNimbusSetting()     { return nimbus; }
    public BooleanSetting getWingsSetting()      { return wings; }
    public BooleanSetting getWings2Setting()     { return wings2; }
    public BooleanSetting getBlackWingsSetting() { return blackWings; }
    public BooleanSetting getCapSetting()      { return cap; }
    public BooleanSetting getMortyPatchSetting() { return mortyPatch; }
    public BooleanSetting getNikeCapSetting() { return nikeCap; }
    public BooleanSetting getChinaHatSetting() { return chinaHat; }
    public BooleanSetting getKaguneSetting()   { return kagune; }
    public BooleanSetting getRobotTentaclesSetting() { return robotTentacles; }

    public Color getThemeColor() { return currentColor; }
    public float getBlackWingSize() { return blackWingSize.getValue().floatValue(); }
    public boolean isBlackWingAnimated() { return blackWingAnimation.getValue(); }

     
     
     
     
     
     
     
     
     
     
     
     
     

    public void renderButterflyWingsPreview(PlayerEntity player, MatrixStack matrices) {
        renderButterflyWings(player, 1.0f, matrices, player.getPos());
    }

    public void renderClassicWingsPreview(PlayerEntity player, MatrixStack matrices) {
        renderClassicWings(player, 1.0f, matrices, player.getPos());
    }

 
    public void renderNimbusPreview(MatrixStack matrices, PlayerEntity player) {
        int baseColor = currentColor.getRGB();
        long nowMs = System.currentTimeMillis();
        double radiansPerMillisecond = NIMBUS_SPEED * Math.PI / 180.0 / 1000.0;
        double headY = player.getHeight() + 0.1;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, darkvisuals.id("hud/glow.png"));

        for (int arm = 0; arm < NIMBUS_ARMS; arm++) {
            double baseAngle = radiansPerMillisecond * nowMs + arm * (Math.PI * 2.0 / NIMBUS_ARMS);
            for (int segment = 0; segment < NIMBUS_SEGMENTS; segment++) {
                double segmentAngle = baseAngle - segment * NIMBUS_STEP_RADIANS;
                double offsetX = Math.cos(segmentAngle) * NIMBUS_RADIUS;
                double offsetZ = Math.sin(segmentAngle) * NIMBUS_RADIUS;

                float progress = segment / (float) Math.max(1, NIMBUS_SEGMENTS - 1);
                float size = NIMBUS_BASE_SIZE * (1.0f - progress * 0.7f);
                int alpha = MathHelper.clamp(NIMBUS_MAX_ALPHA - segment * NIMBUS_ALPHA_FALLOFF, 0, NIMBUS_MAX_ALPHA);
                int segmentColor = multAlpha(baseColor, alpha / 255.0f);

                renderNimbusBillboard(matrices, 180.0f, 0.0f, offsetX, headY, offsetZ, size, segmentColor);
            }
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
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
        selfClassicBodyYawInitialized = false;
        previewOverrideId = null;
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }
    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        kaguneAnimTime += 0.18f * kaguneSpeed.getValue().floatValue();
        robotTentacleAnimTime += 0.18f * robotTentacleSpeed.getValue().floatValue();
        if (!mc.player.isOnGround() && mc.player.getVelocity().y < -0.08) {
            propellerSpeed = Math.min(propellerSpeed + 4.0f, 60.0f);
        } else {
            propellerSpeed = Math.max(propellerSpeed - 2.0f, 0.0f);
        }
        propellerRotation += propellerSpeed;
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        boolean showNimbus = nimbus.getValue();
        boolean renderButterfly = wings.getValue();
        boolean renderClassic = wings2.getValue();
        boolean renderBlack = blackWings.getValue();
        boolean renderCapH = cap.getValue();
        boolean renderChina = chinaHat.getValue();
        boolean renderKag = kagune.getValue();
        boolean renderMorty = mortyPatch.getValue();
        boolean renderNike = nikeCap.getValue();
        boolean renderRobotTentacles = robotTentacles.getValue();

        String preview = previewOverrideId;
        if (preview != null) {
            showNimbus = "nimbus".equals(preview);
            renderButterfly = "w1".equals(preview);
            renderClassic = "w2".equals(preview);
            renderBlack = "black".equals(preview);
            renderCapH = "cap".equals(preview);
            renderChina = "china".equals(preview);
            renderKag = "kagune".equals(preview);
            renderMorty = "morty".equals(preview);
            renderNike = "nike".equals(preview);
            renderRobotTentacles = "robot_tentacles".equals(preview);
        }

        if (showNimbus) {
            renderNimbus(event);
        }

        if (!renderButterfly && !renderClassic && !renderBlack && !renderCapH && !renderChina && !renderKag && !renderMorty && !renderNike && !renderRobotTentacles) return;

        float tickDelta = event.getTickDelta();
        MatrixStack matrices = event.getMatrices();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {
                 
                if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
                if (!renderButterfly && !renderClassic && !renderBlack && !renderCapH
                        && !renderChina && !renderKag && !renderMorty && !renderNike && !renderRobotTentacles) continue;
                if (renderButterfly) renderButterflyWings(player, tickDelta, matrices, cameraPos);
                if (renderClassic) renderClassicWings(player, tickDelta, matrices, cameraPos);
                if (renderBlack) {
                    BlackWings.render(player, tickDelta, matrices, cameraPos,
                            blackWingSize.getValue().floatValue(), blackWingAnimation.getValue());
                }
                if (renderCapH) renderCap(player, tickDelta, matrices, cameraPos);
                if (renderChina) renderChinaHat(player, tickDelta, matrices, cameraPos);
                if (renderKag) {
                    float animatedTime = kaguneAnimTime + 0.18f * kaguneSpeed.getValue().floatValue() * tickDelta;
                    KaguneModel.render(player, tickDelta, matrices, cameraPos,
                            animatedTime, kaguneSize.getValue().floatValue(), kaguneAnimation.getValue());
                }
                if (renderMorty) renderMortyPatch(player, tickDelta, matrices, cameraPos);
                if (renderNike) renderNikeCap(player, tickDelta, matrices, cameraPos);
                if (renderRobotTentacles) {
                    float animatedTime = robotTentacleAnimTime + 0.18f * robotTentacleSpeed.getValue().floatValue() * tickDelta;
                    RobotTentaclesModel.render(player, tickDelta, matrices, cameraPos,
                            animatedTime, robotTentacleSize.getValue().floatValue(), robotTentacleAnimation.getValue());
                }
                continue;
            }

             
            if (!shouldRenderCosmeticForPlayer(player)) continue;
            Set<String> remote = dev.darkvisuals.client.managers.CosmeticsSyncManager.getRemoteCosmetics(player.getUuid());
            if (remote.isEmpty()) continue;
            if (remote.contains("w1")) renderButterflyWings(player, tickDelta, matrices, cameraPos);
            if (remote.contains("w2")) renderClassicWings(player, tickDelta, matrices, cameraPos);
            if (remote.contains("bw")) {
                BlackWings.render(player, tickDelta, matrices, cameraPos,
                        blackWingSize.getValue().floatValue(), blackWingAnimation.getValue());
            }
            if (remote.contains("cap")) renderCap(player, tickDelta, matrices, cameraPos);
            if (remote.contains("china")) renderChinaHat(player, tickDelta, matrices, cameraPos);
            if (remote.contains("kagune")) {
                float animatedTime = kaguneAnimTime + 0.18f * kaguneSpeed.getValue().floatValue() * tickDelta;
                KaguneModel.render(player, tickDelta, matrices, cameraPos,
                        animatedTime, kaguneSize.getValue().floatValue(), kaguneAnimation.getValue());
            }
            if (remote.contains("morty")) renderMortyPatch(player, tickDelta, matrices, cameraPos);
            if (remote.contains("nike")) renderNikeCap(player, tickDelta, matrices, cameraPos);
            if (remote.contains("robot_tentacles")) {
                float animatedTime = robotTentacleAnimTime + 0.18f * robotTentacleSpeed.getValue().floatValue() * tickDelta;
                RobotTentaclesModel.render(player, tickDelta, matrices, cameraPos,
                        animatedTime, robotTentacleSize.getValue().floatValue(), robotTentacleAnimation.getValue());
            }
        }
    }

    private int multAlpha(int color, float alphaMult) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (((color >> 24) & 0xFF) * alphaMult);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void renderButterflyWings(PlayerEntity player, float tickDelta, MatrixStack matrices, Vec3d cameraPos) {
        if (player.isGliding() || player.getPose() == EntityPose.SWIMMING || player.isInSwimmingPose()) {
            return;
        }

        Vec3d velocity = player.getVelocity();
        float bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float yawRad = bodyYaw * 0.017453292F;
        Vec3d forward = new Vec3d(-MathHelper.sin(yawRad), 0.0, MathHelper.cos(yawRad));
        Vec3d sideways = new Vec3d(forward.z, 0.0, -forward.x);

        float forwardMove = (float) (velocity.x * forward.x + velocity.z * forward.z);
        float strafeMove = (float) (velocity.x * sideways.x + velocity.z * sideways.z);
        float verticalMove = (float) velocity.y;

        boolean animated = butterflyWingAnimation.getValue();
        float smoothLean = animated ? MathHelper.clamp(-forwardMove * 140.0f - verticalMove * 48.0f, -24.0f, 26.0f) : 0.0f;
        float smoothStrafe = animated ? MathHelper.clamp(strafeMove * 90.0f, -10.0f, 10.0f) : 0.0f;
        float wingSpring = animated ? MathHelper.clamp(
                Math.abs(forwardMove) * 0.95f + Math.abs(strafeMove) * 0.65f + Math.abs(verticalMove) * 0.75f,
                0.0f,
                1.7f
        ) : 0.0f;

        float anim = (player.age + tickDelta) * 0.22f * FLAP_SPEED + wingSpring * 0.40f;
        float sin = animated ? MathHelper.sin(anim) : 0.0f;
        float cos = animated ? MathHelper.cos(anim) : 0.0f;

        float spreadAngle = 18.0f + wingSpring * 5.0f;
        float pitchAngle = 13f + smoothLean * 0.30f + cos * 4.0f;
        float rollAngle = (sin * FLAP_AMPLITUDE) + smoothStrafe * 0.75f;
        EntityPose pose = player.getPose();
        boolean fallFlying = player.isGliding();
        boolean horizontalPose = pose == EntityPose.SWIMMING || fallFlying;
        if (horizontalPose) {
            spreadAngle -= 4.0f;
            pitchAngle -= 6.0f;
            rollAngle *= 0.72f;
        }

        if (player.isSneaking()) {
            spreadAngle -= 3.0f;
            pitchAngle += 8.0f;
        }

        double px = MathHelper.lerp(tickDelta, player.prevX, player.getX()) - cameraPos.x;
        double py = MathHelper.lerp(tickDelta, player.prevY, player.getY()) - cameraPos.y;
        double pz = MathHelper.lerp(tickDelta, player.prevZ, player.getZ()) - cameraPos.z;

        matrices.push();
        matrices.translate(px, py, pz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        applyBackPoseTransform(matrices, player, tickDelta, pose, fallFlying);

        int themeColor = currentColor.getRGB();
        int topColor = multAlpha(themeColor, 0.5f);
        int bottomColor = multAlpha(themeColor, 0.4f);
        int outlineColor = multAlpha(themeColor, 0.8f);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float butterflyScale = WING_SCALE * butterflyWingSize.getValue().floatValue();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        renderButterflyWing(buffer, matrices, 1.0f, spreadAngle, pitchAngle, rollAngle, butterflyScale, topColor, bottomColor);
        renderButterflyWing(buffer, matrices, -1.0f, spreadAngle, pitchAngle, rollAngle, butterflyScale, topColor, bottomColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth(1.9f);
        BufferBuilder outlineBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        renderButterflyWingOutline(outlineBuffer, matrices, 1.0f, spreadAngle, pitchAngle, rollAngle, butterflyScale, outlineColor);
        renderButterflyWingOutline(outlineBuffer, matrices, -1.0f, spreadAngle, pitchAngle, rollAngle, butterflyScale, outlineColor);
        BufferRenderer.drawWithGlobalProgram(outlineBuffer.end());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        matrices.pop();
    }

      
    private void renderCap(PlayerEntity player, float tickDelta, MatrixStack matrices, Vec3d cameraPos) {
        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        matrices.push();
        double headOffsetY = player.isSneaking() ? 1.25D : 1.5D;
        matrices.translate(x - cameraPos.x, y + headOffsetY - cameraPos.y, z - cameraPos.z);

         
        float yaw = guiPreview ? guiPreviewYaw + 180.0f
                : MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);
        float pitch = guiPreview ? 0.0f : MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

        int light;
        if (guiPreview) {
            light = LightmapTextureManager.MAX_LIGHT_COORDINATE;  
        } else {
            BlockPos lightPos = BlockPos.ofFloored(x, y + headOffsetY, z);
            light = LightmapTextureManager.pack(
                    mc.world.getLightLevel(LightType.BLOCK, lightPos),
                    mc.world.getLightLevel(LightType.SKY, lightPos));
        }

        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        capModel.render(matrices, vcp, light, propellerRotation);
        vcp.draw();
        matrices.pop();
    }

      
    private void renderChinaHat(PlayerEntity player, float tickDelta, MatrixStack matrices, Vec3d cameraPos) {
        Vec3d pos = guiPreview ? player.getPos() : player.getLerpedPos(tickDelta);

        matrices.push();
        float eyeHeight = player.getEyeHeight(player.getPose());
        matrices.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);

        float bodyYaw = guiPreview ? guiPreviewYaw : MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        matrices.translate(0.0, eyeHeight - 0.15f, 0.0);

        float netHeadYaw = guiPreview ? 0.0f
                : MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw) - bodyYaw;
        float headPitch = guiPreview ? 0.0f : MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-netHeadYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch));

        float baseToCrown = player.isInSneakingPose() ? 0.305f : 0.265f;
        float pitchRad = (float) Math.toRadians(MathHelper.clamp(headPitch, -90.0f, 90.0f));
        float cosPitch = MathHelper.clamp((float) Math.cos(pitchRad), 0.0f, 1.0f);
        float tiltFactor = 1.0f - cosPitch;
        float upMul = headPitch < 0.0f ? 1.6f : 1.0f;
        float clearance = 0.03f * cosPitch + (headPitch < 0.0f ? 0.008f * tiltFactor : 0.0f);
        float dynamicOffset = -0.05f + 0.04f + 0.08f * tiltFactor * upMul;
        float forwardOffset = -0.06f * tiltFactor * Math.signum(headPitch) * upMul;
        matrices.translate(0.0, (baseToCrown + clearance) + dynamicOffset, forwardOffset);

        renderConeHatHollow(matrices, chinaBrimRadius.getValue().floatValue(), 0.35f,
                ThemeManager.getInstance().getCurrentTheme().getBackgroundColor(), 96);
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
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        float tipAlpha = MathHelper.clamp(chinaOpacity.getValue().floatValue(), 0.0f, 1.0f);
        float baseAlpha = MathHelper.clamp(tipAlpha * 0.7f, 0.0f, 1.0f);
        BufferBuilder coneStrip = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segs; i++) {
            double a = (i / (double) segs) * Math.PI * 2.0;
            float px = (float) (Math.cos(a) * radius);
            float pz = (float) (Math.sin(a) * radius);
            coneStrip.vertex(m, px, 0f, pz).color(r, g, b, baseAlpha);
            coneStrip.vertex(m, 0f, height, 0f).color(r, g, b, tipAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(coneStrip.end());
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

 
    private void renderMortyPatch(PlayerEntity player, float tickDelta, MatrixStack matrices, Vec3d cameraPos) {
        if (!player.isAlive() || player.isInvisible()) return;

        Vec3d pos = guiPreview ? player.getPos() : player.getLerpedPos(tickDelta);
        float eyeHeight = player.getEyeHeight(player.getPose());

        matrices.push();
        matrices.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);

         
        float bodyYaw = guiPreview ? guiPreviewYaw : MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        matrices.translate(0.0, eyeHeight, 0.0);

        float netHeadYaw = guiPreview ? 0.0f
                : MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw) - bodyYaw;
        float headPitch = guiPreview ? 0.0f : MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-netHeadYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch));

         
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(18.0f));

         
        Color patchColor = new Color(0x14, 0x14, 0x16);
        Color strapColor = new Color(0x20, 0x20, 0x24);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        Matrix4f m = matrices.peek().getPositionMatrix();

         
        final float STRAP_R = 0.285f;    
        final float STRAP_H = 0.028f;     
        final int SEGS = 32;
        float sr = strapColor.getRed() / 255f;
        float sg = strapColor.getGreen() / 255f;
        float sb = strapColor.getBlue() / 255f;

        BufferBuilder strap = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= SEGS; i++) {
            double a = (i / (double) SEGS) * Math.PI * 2.0;
            float x = (float) (Math.cos(a) * STRAP_R);
            float z = (float) (Math.sin(a) * STRAP_R);
            strap.vertex(m, x, STRAP_H, z).color(sr, sg, sb, 1.0f);
            strap.vertex(m, x, -STRAP_H, z).color(sr, sg, sb, 1.0f);
        }
        BufferRenderer.drawWithGlobalProgram(strap.end());

         
        final float PATCH_X = -0.13f;    
        final float PATCH_Y = 0.02f;
        final float PATCH_Z = 0.285f;    
        final float PATCH_R = 0.10f;
        float pr = patchColor.getRed() / 255f;
        float pg = patchColor.getGreen() / 255f;
        float pb = patchColor.getBlue() / 255f;

        BufferBuilder patch = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        patch.vertex(m, PATCH_X, PATCH_Y, PATCH_Z).color(pr, pg, pb, 1.0f);  
        for (int i = 0; i <= SEGS; i++) {
            double a = (i / (double) SEGS) * Math.PI * 2.0;
            float vx = PATCH_X + (float) (Math.cos(a) * PATCH_R);
            float vy = PATCH_Y + (float) (Math.sin(a) * PATCH_R);
            patch.vertex(m, vx, vy, PATCH_Z).color(pr, pg, pb, 1.0f);
        }
        BufferRenderer.drawWithGlobalProgram(patch.end());

        GL11.glEnable(GL11.GL_CULL_FACE);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

 
    private void renderNikeCap(PlayerEntity player, float tickDelta, MatrixStack matrices, Vec3d cameraPos) {
        if (!player.isAlive() || player.isInvisible()) return;

        Vec3d pos = guiPreview ? player.getPos() : player.getLerpedPos(tickDelta);
        float eyeHeight = player.getEyeHeight(player.getPose());

        matrices.push();
        matrices.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);

        float bodyYaw = guiPreview ? guiPreviewYaw : MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        matrices.translate(0.0, eyeHeight, 0.0);

        float netHeadYaw = guiPreview ? 0.0f
                : MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw) - bodyYaw;
        float headPitch = guiPreview ? 0.0f : MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-netHeadYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch));

         
        final float HW    = 0.29f;    
        final float BOT   = 0.05f;    
        final float TOP   = 0.34f;    
        final float BRIM_HW  = 0.21f;   
        final float BRIM_LEN = 0.22f;   
        final float BRIM_TH  = 0.035f;  

        Color black = new Color(0x0C, 0x0C, 0x0E);
        Color white = new Color(0xF2, 0xF2, 0xF2);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

         
        emitBox(buf, m, -HW, BOT, -HW, HW, TOP, HW, black);
         
        emitBox(buf, m, -0.05f, TOP, -0.05f, 0.05f, TOP + 0.04f, 0.05f, black);
         
        emitBox(buf, m, -BRIM_HW, BOT - BRIM_TH, HW - 0.02f, BRIM_HW, BOT, HW + BRIM_LEN, black);

         
        final String[] SWOOSH = {
                "..........##",
                ".........##.",
                "##.....###..",
                ".###..###...",
                "...####....."
        };
        final float PX = 0.028f;                        
        final float SW_W = SWOOSH[0].length() * PX;     
        final float SW_X0 = -SW_W / 2.0f;               
        final float SW_Y0 = BOT + 0.16f;                
        final float SW_Z  = HW + 0.003f;                
        float wr = white.getRed() / 255f, wg = white.getGreen() / 255f, wb = white.getBlue() / 255f;

        for (int row = 0; row < SWOOSH.length; row++) {
            for (int col = 0; col < SWOOSH[row].length(); col++) {
                if (SWOOSH[row].charAt(col) != '#') continue;
                float x0 = SW_X0 + col * PX;
                float y0 = SW_Y0 - row * PX;
                buf.vertex(m, x0,      y0,      SW_Z).color(wr, wg, wb, 1.0f);
                buf.vertex(m, x0 + PX, y0,      SW_Z).color(wr, wg, wb, 1.0f);
                buf.vertex(m, x0 + PX, y0 - PX, SW_Z).color(wr, wg, wb, 1.0f);
                buf.vertex(m, x0,      y0 - PX, SW_Z).color(wr, wg, wb, 1.0f);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        GL11.glEnable(GL11.GL_CULL_FACE);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

      
    private void emitBox(BufferBuilder buf, Matrix4f m,
                         float x0, float y0, float z0, float x1, float y1, float z1, Color c) {
        float r = c.getRed() / 255f, g = c.getGreen() / 255f, b = c.getBlue() / 255f;
         
        buf.vertex(m, x0, y0, z0).color(r, g, b, 1f); buf.vertex(m, x1, y0, z0).color(r, g, b, 1f);
        buf.vertex(m, x1, y0, z1).color(r, g, b, 1f); buf.vertex(m, x0, y0, z1).color(r, g, b, 1f);
         
        buf.vertex(m, x0, y1, z0).color(r, g, b, 1f); buf.vertex(m, x1, y1, z0).color(r, g, b, 1f);
        buf.vertex(m, x1, y1, z1).color(r, g, b, 1f); buf.vertex(m, x0, y1, z1).color(r, g, b, 1f);
         
        buf.vertex(m, x0, y0, z0).color(r, g, b, 1f); buf.vertex(m, x1, y0, z0).color(r, g, b, 1f);
        buf.vertex(m, x1, y1, z0).color(r, g, b, 1f); buf.vertex(m, x0, y1, z0).color(r, g, b, 1f);
         
        buf.vertex(m, x0, y0, z1).color(r, g, b, 1f); buf.vertex(m, x1, y0, z1).color(r, g, b, 1f);
        buf.vertex(m, x1, y1, z1).color(r, g, b, 1f); buf.vertex(m, x0, y1, z1).color(r, g, b, 1f);
         
        buf.vertex(m, x0, y0, z0).color(r, g, b, 1f); buf.vertex(m, x0, y0, z1).color(r, g, b, 1f);
        buf.vertex(m, x0, y1, z1).color(r, g, b, 1f); buf.vertex(m, x0, y1, z0).color(r, g, b, 1f);
         
        buf.vertex(m, x1, y0, z0).color(r, g, b, 1f); buf.vertex(m, x1, y0, z1).color(r, g, b, 1f);
        buf.vertex(m, x1, y1, z1).color(r, g, b, 1f); buf.vertex(m, x1, y1, z0).color(r, g, b, 1f);
    }

    private void renderClassicWings(PlayerEntity player, float tickDelta, MatrixStack matrices, Vec3d cameraPos) {
        if (!player.isAlive() || player.isInvisible()) {
            return;
        }
        if (player.isGliding() || player.getPose() == EntityPose.SWIMMING || player.isInSwimmingPose()) {
            return;
        }

        double px = MathHelper.lerp(tickDelta, player.prevX, player.getX()) - cameraPos.x;
        double py = MathHelper.lerp(tickDelta, player.prevY, player.getY()) - cameraPos.y;
        double pz = MathHelper.lerp(tickDelta, player.prevZ, player.getZ()) - cameraPos.z;

        float bodyYaw = resolveClassicBodyYaw(player, tickDelta);
        Vec3d velocity = player.getVelocity();
        float yawRad = bodyYaw * 0.017453292F;
        Vec3d forward = new Vec3d(-MathHelper.sin(yawRad), 0.0, MathHelper.cos(yawRad));
        Vec3d sideways = new Vec3d(forward.z, 0.0, -forward.x);

        float forwardMove = (float) (velocity.x * forward.x + velocity.z * forward.z);
        float strafeMove = (float) (velocity.x * sideways.x + velocity.z * sideways.z);
        float verticalMove = (float) velocity.y;

        boolean animated = classicWingAnimation.getValue();
        float smoothLean = animated ? MathHelper.clamp(-forwardMove * 140.0f - verticalMove * 48.0f, -24.0f, 26.0f) : 0.0f;
        float smoothStrafe = animated ? MathHelper.clamp(strafeMove * 90.0f, -10.0f, 10.0f) : 0.0f;
        float wingSpring = animated ? MathHelper.clamp(
                Math.abs(forwardMove) * 0.95f + Math.abs(strafeMove) * 0.65f + Math.abs(verticalMove) * 0.75f,
                0.0f,
                1.7f
        ) : 0.0f;

        float anim = (player.age + tickDelta) * 0.22f * FLAP_SPEED + wingSpring * 0.40f;
        float sin = animated ? MathHelper.sin(anim) : 0.0f;
        float cos = animated ? MathHelper.cos(anim) : 0.0f;

        float spreadAngle = 18.0f + wingSpring * 5.0f;
        float pitchAngle = 13f + smoothLean * 0.30f + cos * 4.0f;
        float rollAngle = (sin * FLAP_AMPLITUDE) + smoothStrafe * 0.75f;
        EntityPose pose = player.getPose();
        boolean fallFlying = player.isGliding();
        boolean horizontalPose = pose == EntityPose.SWIMMING || fallFlying;
        if (horizontalPose) {
            spreadAngle -= 4.0f;
            pitchAngle -= 6.0f;
            rollAngle *= 0.72f;
        }

        if (player.isSneaking()) {
            spreadAngle -= 3.0f;
            pitchAngle += 8.0f;
        }

        ClassicWingPose wingPose = resolveClassicWingPose(player, tickDelta, pose);
        float open = spreadAngle * wingPose.openMultiplier;
        float scale = wingPose.scaleMultiplier * classicWingSize.getValue().floatValue();
        float animatedSidePitch = wingPose.sidePitch + pitchAngle * 0.18f;
        float animatedSideRoll = (wingPose.sideRoll + rollAngle * 0.20f);

        int themeColor = currentColor.getRGB();
        int baseColor = multAlpha(themeColor, 0.85f);
        int glowColor = multAlpha(themeColor, 0.22f);
        int coreColor = multAlpha(themeColor, 0.26f);
        int outlineColor = multAlpha(themeColor, 0.62f);
        int ribsColor = multAlpha(themeColor, 0.20f);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
        matrices.translate(px, py, pz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        if (wingPose.preTranslateY != 0.0f || wingPose.preTranslateZ != 0.0f) {
            matrices.translate(0.0f, wingPose.preTranslateY, wingPose.preTranslateZ);
        }
        if (wingPose.pitchRotation != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(wingPose.pitchRotation));
        }
        if (wingPose.rollRotation != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(wingPose.rollRotation));
        }
        matrices.translate(0.0f, wingPose.anchorY, wingPose.anchorZ);
        matrices.scale(scale, scale, scale);

        renderClassicWingSide(matrices, -1.0f, open, animatedSidePitch, animatedSideRoll, baseColor, glowColor, coreColor, outlineColor, ribsColor, wingPose);
        renderClassicWingSide(matrices, 1.0f, open, animatedSidePitch, animatedSideRoll, baseColor, glowColor, coreColor, outlineColor, ribsColor, wingPose);
        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    private void renderNimbus(EventRender3D.Game event) {
        if (mc.player == null || mc.world == null || mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        float tickDelta = event.getTickDelta();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        double x = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX());
        double y = MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY()) + mc.player.getHeight() + 0.1;
        double z = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ());

        int baseColor = currentColor.getRGB();
        long nowMs = System.currentTimeMillis();
        double radiansPerMillisecond = NIMBUS_SPEED * Math.PI / 180.0 / 1000.0;

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, darkvisuals.id("hud/glow.png"));

        MatrixStack matrices = event.getMatrices();
        for (int arm = 0; arm < NIMBUS_ARMS; arm++) {
            double baseAngle = radiansPerMillisecond * nowMs + arm * (Math.PI * 2.0 / NIMBUS_ARMS);
            for (int segment = 0; segment < NIMBUS_SEGMENTS; segment++) {
                double segmentAngle = baseAngle - segment * NIMBUS_STEP_RADIANS;
                double offsetX = Math.cos(segmentAngle) * NIMBUS_RADIUS;
                double offsetZ = Math.sin(segmentAngle) * NIMBUS_RADIUS;

                float progress = segment / (float) Math.max(1, NIMBUS_SEGMENTS - 1);
                float size = NIMBUS_BASE_SIZE * (1.0f - progress * 0.7f);
                int alpha = MathHelper.clamp(NIMBUS_MAX_ALPHA - segment * NIMBUS_ALPHA_FALLOFF, 0, NIMBUS_MAX_ALPHA);
                int segmentColor = multAlpha(baseColor, alpha / 255.0f);

                renderNimbusBillboard(
                        matrices,
                        mc.gameRenderer.getCamera().getYaw(),
                        mc.gameRenderer.getCamera().getPitch(),
                        x - camera.x + offsetX,
                        y - camera.y,
                        z - camera.z + offsetZ,
                        size,
                        segmentColor
                );
            }
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderNimbusBillboard(MatrixStack matrices, float cameraYaw, float cameraPitch,
                                       double x, double y, double z, float size, int color) {
        int a = (color >> 24) & 0xFF;
        if (a <= 0) {
            return;
        }

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        float half = size * 0.5f;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -half, -half, 0.0f).texture(0.0f, 1.0f).color(r, g, b, a);
        buffer.vertex(matrix, -half, half, 0.0f).texture(0.0f, 0.0f).color(r, g, b, a);
        buffer.vertex(matrix, half, half, 0.0f).texture(1.0f, 0.0f).color(r, g, b, a);
        buffer.vertex(matrix, half, -half, 0.0f).texture(1.0f, 1.0f).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();
    }

    private boolean shouldRenderCosmeticForPlayer(PlayerEntity player) {
        if (mc.player == null) return false;
        if (player == mc.player) return true;
         
         
        if (FriendsManager.checkFriend(player.getGameProfile().getName())) return true;
        return !dev.darkvisuals.client.managers.CosmeticsSyncManager
                .getRemoteCosmetics(player.getUuid()).isEmpty();
    }

    private void applyBackPoseTransform(MatrixStack matrices, PlayerEntity player, float tickDelta, EntityPose pose, boolean fallFlying) {
        if (fallFlying) {
            float pitch = player.getPitch(tickDelta);
            float clampedPitch = MathHelper.clamp(pitch, -65.0f, 65.0f);

            matrices.translate(0.0f, 0.3f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-(90.0f + clampedPitch)));
            matrices.translate(0.0f, -0.15f, 0.12f);
            return;
        }

        if (pose == EntityPose.SWIMMING) {
            float pitch = player.getPitch(tickDelta);
            float clampedPitch = MathHelper.clamp(pitch, -65.0f, 65.0f);

            matrices.translate(0.0f, 0.3f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-(90.0f + clampedPitch)));
            matrices.translate(0.0f, -0.15f, 0.12f);
            return;
        }

        if (player.isSneaking()) {
            matrices.translate(0.0f, 1.15f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(24.0f));
            matrices.translate(0.0f, 0.0f, 0.08f);
        } else {
            matrices.translate(0.0f, 1.30f, 0.08f);
        }
    }

    private float resolveClassicBodyYaw(PlayerEntity player, float tickDelta) {
        if (guiPreview) return guiPreviewYaw;  
        float targetBodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        if (player != mc.player) {
            return targetBodyYaw;
        }

        if (!selfClassicBodyYawInitialized) {
            selfClassicBodyYaw = targetBodyYaw;
            selfClassicBodyYawInitialized = true;
            return selfClassicBodyYaw;
        }

        float delta = MathHelper.wrapDegrees(targetBodyYaw - selfClassicBodyYaw);
        selfClassicBodyYaw += MathHelper.clamp(delta, -14.0f, 14.0f);
        return selfClassicBodyYaw;
    }

    private ClassicWingPose resolveClassicWingPose(PlayerEntity player, float tickDelta, EntityPose pose) {
        float pitch = player.getPitch(tickDelta);

        if (player.isGliding()) {
            float clampedPitch = MathHelper.clamp(pitch, -65.0f, 65.0f);
            return new ClassicWingPose(1.18f, 0.10f, 0.0f, 0.0f, -(90.0f + clampedPitch), 0.0f,
                    0.76f, 0.92f, 0.10f, 0.58f, 0.05f, 0.0f, 0.06f, -5.0f, -2.0f, 0.13f);
        }

        if (pose == EntityPose.SWIMMING || player.isInSwimmingPose()) {
            float clampedPitch = MathHelper.clamp(pitch, -65.0f, 65.0f);
            float bodyShiftY = player.isInSwimmingPose() ? 1.10f : 1.18f;
            float bodyShiftZ = player.isInSwimmingPose() ? 0.18f : 0.12f;
            return new ClassicWingPose(bodyShiftY, bodyShiftZ, 0.18f, 0.48f, -(90.0f + clampedPitch), 0.0f,
                    0.84f, 0.96f, 0.12f, 0.70f, 0.03f, 0.0f, 0.01f, -7.0f, -3.0f, 0.16f);
        }

        if (player.isSneaking()) {
            return new ClassicWingPose(0.0f, 0.0f, 0.96f, 0.10f, 18.0f, 0.0f,
                    1.0f, 1.0f, 0.18f, 4.5f, 0.06f, 0.0f, 0.02f, -11.0f, -4.0f, 0.12f);
        }

        return new ClassicWingPose(0.0f, 0.0f, 1.18f, 0.10f, 0.0f, 0.0f,
                1.0f, 1.0f, 0.18f, 4.5f, 0.06f, 0.0f, 0.02f, -11.0f, -4.0f, 0.12f);
    }

    private void renderClassicWingSide(MatrixStack matrices, float side, float open, float sidePitch, float sideRoll,
                                       int baseColor, int glowColor, int coreColor, int outlineColor, int ribsColor,
                                       ClassicWingPose pose) {
        matrices.push();
        matrices.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * open));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * sideRoll));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sidePitch));

        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        drawClassicWingLayer(matrices, side, 1.22f, glowColor, multAlpha(glowColor, 0));
        drawClassicWingLayer(matrices, side, 0.84f, coreColor, multAlpha(coreColor, 0));

        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawClassicWingLayer(matrices, side, 1.0f, baseColor, multAlpha(baseColor, 10 / 255.0f));

        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        drawClassicWingOutline(matrices, side, 1.0f, outlineColor);
        drawClassicWingRibs(matrices, side, 0.96f, ribsColor);
        matrices.pop();
    }

    private void drawClassicWingLayer(MatrixStack matrices, float side, float scale, int rootColor, int edgeColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < CLASSIC_WING_SHAPE.length; i++) {
            ClassicWingPoint current = CLASSIC_WING_SHAPE[i];
            ClassicWingPoint next = CLASSIC_WING_SHAPE[(i + 1) % CLASSIC_WING_SHAPE.length];
            vertex(buffer, matrix, 0.0f, 0.0f, 0.0f, rootColor);
            vertex(buffer, matrix, side * current.x * scale, current.y * scale, 0.0f, applyClassicWingPointAlpha(edgeColor, current.alphaMultiplier));
            vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0.0f, applyClassicWingPointAlpha(edgeColor, next.alphaMultiplier));
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawClassicWingOutline(MatrixStack matrices, float side, float scale, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        RenderSystem.lineWidth(1.35f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        for (int i = 0; i < CLASSIC_WING_SHAPE.length; i++) {
            ClassicWingPoint current = CLASSIC_WING_SHAPE[i];
            ClassicWingPoint next = CLASSIC_WING_SHAPE[(i + 1) % CLASSIC_WING_SHAPE.length];
            addLine(buffer, matrix,
                    side * current.x * scale, current.y * scale, 0.0f,
                    side * next.x * scale, next.y * scale, 0.0f,
                    color
            );
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private void drawClassicWingRibs(MatrixStack matrices, float side, float scale, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int[] ribIndices = {2, 4, 7, 9, 11};

        RenderSystem.lineWidth(0.9f);
        for (int ribIndex : ribIndices) {
            ClassicWingPoint point = CLASSIC_WING_SHAPE[ribIndex];
            vertex(buffer, matrix, 0.0f, 0.0f, 0.0f,
                    multAlpha(color, 0.75f));
            vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0f,
                    applyClassicWingPointAlpha(color, point.alphaMultiplier));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private int applyClassicWingPointAlpha(int color, float multiplier) {
        int alpha = (color >> 24) & 0xFF;
        return multAlpha(color, (alpha * multiplier) / 255.0f);
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        buffer.vertex(matrix, x, y, z).color(r, g, b, a);
    }

    private void renderButterflyWing(BufferBuilder buffer, MatrixStack matrices, float side, float spread, float pitch, float roll, float scale,
                                     int topColor, int bottomColor) {
        float root = 0.12f * scale;
        float topW = 1.52f * scale;
        float topH = 0.64f * scale;
        float lowW = 1.14f * scale;
        float lowH = 0.39f * scale;

        matrices.push();
        matrices.translate(0.15f * side, 0f, -0.17f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * spread));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * roll));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        addDoubleSidedGradientTriangle(buffer, matrix,
                side * root, 0.02f, -0.01f,
                side * (root + topW * 0.22f), topH * 0.98f, -0.06f,
                side * (root + topW * 0.88f), topH * 0.60f, -0.13f,
                topColor, bottomColor
        );
        addDoubleSidedGradientTriangle(buffer, matrix,
                side * root, 0.02f, -0.01f,
                side * (root + topW * 0.88f), topH * 0.60f, -0.13f,
                side * (root + topW), topH * 0.12f, -0.17f,
                topColor, bottomColor
        );
        addDoubleSidedGradientTriangle(buffer, matrix,
                side * root, -0.03f, -0.03f,
                side * (root + lowW * 0.26f), -lowH * 0.96f, -0.11f,
                side * (root + lowW * 0.84f), -lowH * 0.54f, -0.18f,
                bottomColor, topColor
        );
        addDoubleSidedGradientTriangle(buffer, matrix,
                side * root, -0.03f, -0.03f,
                side * (root + lowW * 0.84f), -lowH * 0.54f, -0.18f,
                side * (root + lowW), -lowH * 0.12f, -0.21f,
                bottomColor, topColor
        );

        matrices.pop();
    }

    private void renderButterflyWingOutline(BufferBuilder buffer, MatrixStack matrices, float side, float spread, float pitch, float roll,
                                            float scale, int outlineColor) {
        float root = 0.12f * scale;
        float topW = 1.52f * scale;
        float topH = 0.64f * scale;
        float lowW = 1.14f * scale;
        float lowH = 0.39f * scale;

        matrices.push();
        matrices.translate(0.15f * side, 0f, -0.17f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * spread));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * roll));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        addLine(buffer, matrix,
                side * root, 0.02f, -0.01f,
                side * (root + topW * 0.22f), topH * 0.98f, -0.06f,
                outlineColor
        );
        addLine(buffer, matrix,
                side * (root + topW * 0.22f), topH * 0.98f, -0.06f,
                side * (root + topW * 0.88f), topH * 0.60f, -0.13f,
                outlineColor
        );
        addLine(buffer, matrix,
                side * (root + topW * 0.88f), topH * 0.60f, -0.13f,
                side * (root + topW), topH * 0.12f, -0.17f,
                outlineColor
        );
        addLine(buffer, matrix,
                side * root, -0.03f, -0.03f,
                side * (root + lowW * 0.26f), -lowH * 0.96f, -0.11f,
                outlineColor
        );
        addLine(buffer, matrix,
                side * (root + lowW * 0.26f), -lowH * 0.96f, -0.11f,
                side * (root + lowW * 0.84f), -lowH * 0.54f, -0.18f,
                outlineColor
        );
        addLine(buffer, matrix,
                side * (root + lowW * 0.84f), -lowH * 0.54f, -0.18f,
                side * (root + lowW), -lowH * 0.12f, -0.21f,
                outlineColor
        );
        addLine(buffer, matrix,
                side * root, -0.01f, -0.02f,
                side * (root + topW * 0.60f), 0.08f, -0.08f,
                outlineColor
        );
        matrices.pop();
    }

    private void addDoubleSidedGradientTriangle(BufferBuilder buffer, Matrix4f matrix,
                                                float x1, float y1, float z1,
                                                float x2, float y2, float z2,
                                                float x3, float y3, float z3,
                                                int nearColor, int farColor) {
        int nr = (nearColor >> 16) & 0xFF;
        int ng = (nearColor >> 8) & 0xFF;
        int nb = nearColor & 0xFF;
        int na = (nearColor >> 24) & 0xFF;
        int fr = (farColor >> 16) & 0xFF;
        int fg = (farColor >> 8) & 0xFF;
        int fb = farColor & 0xFF;
        int fa = (farColor >> 24) & 0xFF;

        buffer.vertex(matrix, x1, y1, z1).color(nr, ng, nb, na);
        buffer.vertex(matrix, x2, y2, z2).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x3, y3, z3).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x3, y3, z3).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x2, y2, z2).color(fr, fg, fb, fa);
        buffer.vertex(matrix, x1, y1, z1).color(nr, ng, nb, na);
    }

    private void addLine(BufferBuilder buffer, Matrix4f matrix,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }

    private static final class ClassicWingPoint {
        private final float x;
        private final float y;
        private final float alphaMultiplier;

        private ClassicWingPoint(float x, float y, float alphaMultiplier) {
            this.x = x;
            this.y = y;
            this.alphaMultiplier = alphaMultiplier;
        }
    }

    private static final class ClassicWingPose {
        private final float preTranslateY;
        private final float preTranslateZ;
        private final float anchorY;
        private final float anchorZ;
        private final float pitchRotation;
        private final float rollRotation;
        private final float openMultiplier;
        private final float scaleMultiplier;
        private final float motionSpreadBoost;
        private final float flapAmplitude;
        private final float sideOffset;
        private final float sideYOffset;
        private final float sideZOffset;
        private final float sideRoll;
        private final float sidePitch;
        private final float flapSpeed;

        private ClassicWingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ, float pitchRotation,
                                float rollRotation, float openMultiplier, float scaleMultiplier, float motionSpreadBoost,
                                float flapAmplitude, float sideOffset, float sideYOffset, float sideZOffset, float sideRoll,
                                float sidePitch, float flapSpeed) {
            this.preTranslateY = preTranslateY;
            this.preTranslateZ = preTranslateZ;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.pitchRotation = pitchRotation;
            this.rollRotation = rollRotation;
            this.openMultiplier = openMultiplier;
            this.scaleMultiplier = scaleMultiplier;
            this.motionSpreadBoost = motionSpreadBoost;
            this.flapAmplitude = flapAmplitude;
            this.sideOffset = sideOffset;
            this.sideYOffset = sideYOffset;
            this.sideZOffset = sideZOffset;
            this.sideRoll = sideRoll;
            this.sidePitch = sidePitch;
            this.flapSpeed = flapSpeed;
        }
    }
}