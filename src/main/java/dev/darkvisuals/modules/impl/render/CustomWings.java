package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.FriendsManager;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Nameable;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

/**
 * CustomWings — анимированные 3D-крылья на спине игрока:
 * выбор стилей (Dragon, Angel, Cyber), реалистичная анимация взмахов,
 * адаптация под бег/полёт и полная кастомизация цвета.
 */
public class CustomWings extends Module {

    public enum WingStyle implements Nameable {
        Dragon("Dragon"),
        Angel("Angel"),
        Cyber("Cyber");

        private final String displayName;
        WingStyle(String displayName) { this.displayName = displayName; }
        @Override public String getName() { return displayName; }
    }

    public enum ColorMode implements Nameable {
        Theme("Theme"),
        Custom("Custom"),
        White("White"),
        Fire("Fire"),
        Rainbow("Rainbow");

        private final String displayName;
        ColorMode(String displayName) { this.displayName = displayName; }
        @Override public String getName() { return displayName; }
    }

    private final EnumSetting<WingStyle> style = new EnumSetting<>("Стиль крыльев", WingStyle.Dragon);
    private final NumberSetting scale = new NumberSetting("Масштаб", 1.0f, 0.4f, 2.2f, 0.05f);
    private final NumberSetting flapSpeed = new NumberSetting("Скорость взмахов", 1.0f, 0.2f, 3.0f, 0.1f);
    private final EnumSetting<ColorMode> colorMode = new EnumSetting<>("Режим цвета", ColorMode.Theme);
    private final ColorSetting customColor = new ColorSetting("Свой цвет", new Color(180, 50, 255, 255).getRGB());
    private final BooleanSetting glow = new BooleanSetting("Свечение", true);
    private final BooleanSetting showFriends = new BooleanSetting("Показывать друзьям", true);
    private final BooleanSetting onlyThirdPerson = new BooleanSetting("Только от 3-го лица", true);

    public CustomWings() {
        super("CustomWings", Category.Render, "3D анимированные крылья на спине персонажа");
        getSettings().add(style);
        getSettings().add(scale);
        getSettings().add(flapSpeed);
        getSettings().add(colorMode);
        getSettings().add(customColor);
        getSettings().add(glow);
        getSettings().add(showFriends);
        getSettings().add(onlyThirdPerson);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        float tickDelta = event.getTickDelta();
        MatrixStack matrices = event.getMatrices();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) {
                if (onlyThirdPerson.getValue() && mc.options.getPerspective().isFirstPerson()) continue;
                renderWingsForPlayer(matrices, p, tickDelta);
            } else if (showFriends.getValue() && FriendsManager.checkFriend(p.getGameProfile().getName())) {
                renderWingsForPlayer(matrices, p, tickDelta);
            }
        }
    }

    private void renderWingsForPlayer(MatrixStack matrices, PlayerEntity player, float tickDelta) {
        Vec3d pos = player.getLerpedPos(tickDelta);
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        float bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        boolean isFlying = player.isGliding();
        boolean isMoving = player.getVelocity().horizontalLengthSquared() > 0.002;
        boolean inAir = !player.isOnGround();

        // Расчёт фазы анимации взмахов
        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
        float speedMul = flapSpeed.getValue() * (isFlying ? 2.5f : (inAir ? 1.8f : (isMoving ? 1.2f : 0.7f)));
        float flap = (float) Math.sin(time * 4.5f * speedMul);

        // Углы раскрытия крыльев
        float baseSpread = isFlying ? 55f : (inAir ? 40f : (isMoving ? 25f : 16f));
        float spread = baseSpread + flap * (isFlying ? 32f : 18f);
        float pitch = (isFlying ? 10f : (player.isInSneakingPose() ? 25f : 8f)) + (float) Math.cos(time * 4.5f * speedMul) * 6f;

        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

        // Точка крепления — лопатки на спине
        float heightOffset = player.isInSneakingPose() ? 0.95f : 1.25f;
        float backOffset = player.isInSneakingPose() ? 0.08f : -0.16f;
        matrices.translate(0.0, heightOffset, backOffset);

        float sc = scale.getValue() * 0.9f;
        matrices.scale(sc, sc, sc);

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        if (glow.getValue()) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Color baseCol = resolveColor();
        WingStyle currentStyle = style.getValue();

        // Отрисовка левого и правого крыла
        renderWingSide(matrices, currentStyle, 1.0f, spread, pitch, baseCol);
        renderWingSide(matrices, currentStyle, -1.0f, spread, pitch, baseCol);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private void renderWingSide(MatrixStack ms, WingStyle st, float side, float spread, float pitch, Color col) {
        ms.push();

        // Смещение в сторону плеча и вращение крыла
        ms.translate(0.06f * side, 0f, 0f);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spread * side));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-12f * side));

        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float r = col.getRed() / 255f;
        float g = col.getGreen() / 255f;
        float b = col.getBlue() / 255f;
        float a = col.getAlpha() / 255f * 0.85f;

        switch (st) {
            case Dragon -> renderDragonWing(buf, m, side, r, g, b, a);
            case Angel -> renderAngelWing(buf, m, side, r, g, b, a);
            case Cyber -> renderCyberWing(buf, m, side, r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
        ms.pop();
    }

    private void renderDragonWing(BufferBuilder buf, Matrix4f m, float side, float r, float g, float b, float a) {
        float s = side;
        // Кость 1: основание -> локоть
        buf.vertex(m, 0f, 0f, 0f).color(r * 0.7f, g * 0.7f, b * 0.7f, a);
        buf.vertex(m, 0.4f * s, 0.45f, -0.05f).color(r * 0.8f, g * 0.8f, b * 0.8f, a);
        buf.vertex(m, 0.42f * s, 0.40f, 0.0f).color(r * 0.8f, g * 0.8f, b * 0.8f, a);
        buf.vertex(m, 0.02f * s, -0.05f, 0.0f).color(r * 0.7f, g * 0.7f, b * 0.7f, a);

        // Кость 2: локоть -> вершина
        buf.vertex(m, 0.4f * s, 0.45f, -0.05f).color(r, g, b, a);
        buf.vertex(m, 1.15f * s, 0.95f, -0.1f).color(r * 1.1f, g * 1.1f, b * 1.1f, a);
        buf.vertex(m, 1.12f * s, 0.90f, -0.05f).color(r * 1.1f, g * 1.1f, b * 1.1f, a);
        buf.vertex(m, 0.42f * s, 0.40f, 0.0f).color(r, g, b, a);

        // Перепонка 1 (внутренняя)
        buf.vertex(m, 0.05f * s, -0.05f, 0f).color(r * 0.5f, g * 0.5f, b * 0.5f, a * 0.7f);
        buf.vertex(m, 0.4f * s, 0.42f, -0.02f).color(r * 0.9f, g * 0.9f, b * 0.9f, a * 0.85f);
        buf.vertex(m, 0.65f * s, 0.05f, 0f).color(r * 0.75f, g * 0.75f, b * 0.75f, a * 0.75f);
        buf.vertex(m, 0.35f * s, -0.28f, 0.02f).color(r * 0.4f, g * 0.4f, b * 0.4f, a * 0.6f);

        // Перепонка 2 (внешняя)
        buf.vertex(m, 0.4f * s, 0.42f, -0.02f).color(r * 0.9f, g * 0.9f, b * 0.9f, a * 0.85f);
        buf.vertex(m, 1.12f * s, 0.92f, -0.08f).color(r * 1.2f, g * 1.2f, b * 1.2f, a * 0.9f);
        buf.vertex(m, 1.05f * s, 0.35f, -0.02f).color(r * 0.8f, g * 0.8f, b * 0.8f, a * 0.8f);
        buf.vertex(m, 0.65f * s, 0.05f, 0f).color(r * 0.75f, g * 0.75f, b * 0.75f, a * 0.75f);

        // Нижние когти/перья
        buf.vertex(m, 0.65f * s, 0.05f, 0f).color(r * 0.75f, g * 0.75f, b * 0.75f, a * 0.75f);
        buf.vertex(m, 1.05f * s, 0.35f, -0.02f).color(r * 0.8f, g * 0.8f, b * 0.8f, a * 0.8f);
        buf.vertex(m, 0.85f * s, -0.15f, 0.02f).color(r * 0.5f, g * 0.5f, b * 0.5f, a * 0.6f);
        buf.vertex(m, 0.55f * s, -0.22f, 0.02f).color(r * 0.4f, g * 0.4f, b * 0.4f, a * 0.5f);
    }

    private void renderAngelWing(BufferBuilder buf, Matrix4f m, float side, float r, float g, float b, float a) {
        float s = side;
        // Верхний ярус перьев
        buf.vertex(m, 0f, 0.1f, 0f).color(r, g, b, a * 0.9f);
        buf.vertex(m, 0.5f * s, 0.6f, -0.05f).color(r * 1.1f, g * 1.1f, b * 1.1f, a * 0.95f);
        buf.vertex(m, 1.05f * s, 0.85f, -0.08f).color(1f, 1f, 1f, a);
        buf.vertex(m, 0.65f * s, 0.45f, -0.02f).color(r, g, b, a * 0.85f);

        // Средний ярус перьев
        buf.vertex(m, 0.1f * s, -0.05f, 0f).color(r * 0.85f, g * 0.85f, b * 0.85f, a * 0.8f);
        buf.vertex(m, 0.65f * s, 0.45f, -0.02f).color(r, g, b, a * 0.85f);
        buf.vertex(m, 1.15f * s, 0.55f, -0.05f).color(r * 1.1f, g * 1.1f, b * 1.1f, a * 0.9f);
        buf.vertex(m, 0.75f * s, 0.15f, 0.02f).color(r * 0.85f, g * 0.85f, b * 0.85f, a * 0.75f);

        // Нижний ярус длинных перьев
        buf.vertex(m, 0.15f * s, -0.2f, 0.02f).color(r * 0.7f, g * 0.7f, b * 0.7f, a * 0.7f);
        buf.vertex(m, 0.75f * s, 0.15f, 0.02f).color(r * 0.85f, g * 0.85f, b * 0.85f, a * 0.75f);
        buf.vertex(m, 0.95f * s, -0.1f, 0.04f).color(r * 0.9f, g * 0.9f, b * 0.9f, a * 0.8f);
        buf.vertex(m, 0.45f * s, -0.45f, 0.05f).color(r * 0.6f, g * 0.6f, b * 0.6f, a * 0.6f);
    }

    private void renderCyberWing(BufferBuilder buf, Matrix4f m, float side, float r, float g, float b, float a) {
        float s = side;
        // Неоновые кибер-лезвия с градиентом
        // Клинок 1 (главный верхний)
        buf.vertex(m, 0.08f * s, 0.15f, 0f).color(r, g, b, a);
        buf.vertex(m, 0.55f * s, 0.85f, -0.04f).color(1f, 1f, 1f, a);
        buf.vertex(m, 1.25f * s, 1.05f, -0.08f).color(r * 1.3f, g * 1.3f, b * 1.3f, a);
        buf.vertex(m, 0.75f * s, 0.55f, -0.02f).color(r * 0.7f, g * 0.7f, b * 0.7f, a * 0.8f);

        // Клинок 2 (средний острый)
        buf.vertex(m, 0.15f * s, 0f, 0f).color(r * 0.8f, g * 0.8f, b * 0.8f, a * 0.9f);
        buf.vertex(m, 0.75f * s, 0.55f, -0.02f).color(r, g, b, a);
        buf.vertex(m, 1.15f * s, 0.40f, -0.05f).color(1f, 1f, 1f, a);
        buf.vertex(m, 0.65f * s, 0.05f, 0.02f).color(r * 0.6f, g * 0.6f, b * 0.6f, a * 0.75f);

        // Клинок 3 (нижний малый)
        buf.vertex(m, 0.18f * s, -0.15f, 0.02f).color(r * 0.6f, g * 0.6f, b * 0.6f, a * 0.75f);
        buf.vertex(m, 0.65f * s, 0.05f, 0.02f).color(r * 0.8f, g * 0.8f, b * 0.8f, a * 0.85f);
        buf.vertex(m, 0.92f * s, -0.25f, 0.04f).color(r * 1.1f, g * 1.1f, b * 1.1f, a * 0.9f);
        buf.vertex(m, 0.38f * s, -0.32f, 0.05f).color(r * 0.5f, g * 0.5f, b * 0.5f, a * 0.5f);
    }

    private Color resolveColor() {
        return switch (colorMode.getValue()) {
            case Theme -> ThemeManager.getInstance().getCurrentTheme().getAccentColor();
            case Custom -> customColor.getColor();
            case White -> new Color(245, 250, 255, 240);
            case Fire -> new Color(255, 105, 30, 240);
            case Rainbow -> {
                float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
                yield Color.getHSBColor(hue, 0.85f, 1.0f);
            }
        };
    }
}
