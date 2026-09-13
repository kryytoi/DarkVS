package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import dev.darkvisuals.modules.settings.api.Nameable;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class WeatherFX extends Module {

    public enum Mode implements Nameable {
        RAIN("Дождь"),
        SNOW("Снег");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getName() {
            return displayName;
        }
    }

    private final EnumSetting<Mode> mode = new EnumSetting<>("Режим", Mode.RAIN);

    private final NumberSetting density =
            new NumberSetting("Плотность", 60f, 10f, 200f, 10f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 14f, 5f, 32f, 1f);
    private final NumberSetting fallSpeed =
            new NumberSetting("Скорость", 1.0f, 0.3f, 3.0f, 0.1f);
    private final BooleanSetting onlyRaining = new BooleanSetting("Только в дождь", true);
    private final BooleanSetting splashes = new BooleanSetting("Брызги на земле", true, () -> mode.getValue() == Mode.RAIN);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(150, 190, 255, 255).getRGB());

    private final Random rnd = new Random();
    private final List<Drop> drops = new ArrayList<>();
    private final List<Splash> splashRings = new ArrayList<>();

    private static final int MAX_SPLASHES = 60;
    private static final long SPLASH_TTL_MS = 500L;

    public WeatherFX() {
        super("WeatherFX", Category.Render, "Красивый дождь со струями и брызгами или мягкий снег");
        getSettings().add(mode);
        getSettings().add(density);
        getSettings().add(radius);
        getSettings().add(fallSpeed);
        getSettings().add(onlyRaining);
        getSettings().add(splashes);
        getSettings().add(color);
    }

    @Override
    public void onDisable() {
        drops.clear();
        splashRings.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        boolean activeWeather = !onlyRaining.getValue() || mc.world.isRaining();
        if (!activeWeather) {
            drops.clear();
            splashRings.clear();
            return;
        }

        boolean snow = mode.getValue() == Mode.SNOW;
        double speed = snow ? 0.12 : 0.9;
        speed *= fallSpeed.getValue();

        // поддерживаем количество капель/снежинок
        int target = (int) (density.getValue() * (onlyRaining.getValue() ? Math.max(0.4f, mc.world.getRainGradient(1.0f)) : 1f));
        while (drops.size() < target) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = radius.getValue() * Math.sqrt(rnd.nextDouble());
            drops.add(new Drop(mc.player.getPos().add(
                    Math.cos(ang) * dist,
                    6 + rnd.nextDouble() * 8,
                    Math.sin(ang) * dist), snow));
        }
        while (drops.size() > target) {
            drops.remove(drops.size() - 1);
        }

        Iterator<Drop> it = drops.iterator();
        while (it.hasNext()) {
            Drop drop = it.next();
            drop.prevPos = drop.pos;

            if (snow) {
                // снежинка медленно порхает вниз
                drop.pos = drop.pos.add(
                        Math.sin(drop.swayPhase + drop.age * 0.07) * 0.02,
                        -speed,
                        Math.cos(drop.swayPhase + drop.age * 0.05) * 0.02);
                drop.age++;
            } else {
                // капля летит быстро и строго вниз
                drop.pos = drop.pos.add(0, -speed, 0);
            }

            BlockPos below = BlockPos.ofFloored(drop.pos);
            boolean landed = !mc.world.getBlockState(below).isAir()
                    && drop.pos.y < below.up().getY() + (snow ? 0.02 : 0.4);
            if (landed || drop.pos.y < mc.player.getY() - 14) {
                if (landed && !snow && splashes.getValue() && splashRings.size() < MAX_SPLASHES) {
                    splashRings.add(new Splash(new Vec3d(drop.pos.x, below.up().getY(), drop.pos.z)));
                }
                // пересоздаём каплю сверху
                double ang = rnd.nextDouble() * Math.PI * 2;
                double dist = radius.getValue() * Math.sqrt(rnd.nextDouble());
                drop.pos = mc.player.getPos().add(Math.cos(ang) * dist, 8 + rnd.nextDouble() * 6, Math.sin(ang) * dist);
                drop.prevPos = drop.pos;
                drop.age = 0;
            }
        }

        long now = System.currentTimeMillis();
        splashRings.removeIf(s -> now - s.birth >= SPLASH_TTL_MS);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (drops.isEmpty() && splashRings.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color c = color.getColor();
        boolean snow = mode.getValue() == Mode.SNOW;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (Drop drop : drops) {
            matrices.push();
            matrices.translate((float) (drop.pos.x - cam.x), (float) (drop.pos.y - cam.y), (float) (drop.pos.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            if (snow) {
                float half = 0.045f;
                int alpha = 210;
                buffer.vertex(matrix, -half, -half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, -half, half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, half, half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, half, -half, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            } else {
                // вытянутая капля-струя
                float w = 0.012f;
                float len = 0.32f * fallSpeed.getValue();
                int alpha = 150;
                buffer.vertex(matrix, -w, -len, 0).color(c.getRed(), c.getGreen(), c.getBlue(), 20);
                buffer.vertex(matrix, -w, 0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, w, 0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                buffer.vertex(matrix, w, -len, 0).color(c.getRed(), c.getGreen(), c.getBlue(), 20);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        // расходящиеся кольца брызг
        for (Splash splash : splashRings) {
            float t = (now - splash.birth) / (float) SPLASH_TTL_MS;
            float radius = (float) Easing.OUT_QUAD.apply(t) * 0.35f;
            float fade = 1f - t;
            int alpha = (int) (120 * fade);
            if (alpha <= 3 || radius <= 0.01f) continue;

            matrices.push();
            matrices.translate((float) (splash.pos.x - cam.x), (float) (splash.pos.y - cam.y), (float) (splash.pos.z - cam.z));
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 16; i++) {
                float angle = (float) (i * (Math.PI * 2.0) / 16);
                float cos = MathHelper.cos(angle);
                float sin = MathHelper.sin(angle);
                buffer.vertex(matrix, cos * radius * 0.4f, 0, sin * radius * 0.4f).color(c.getRed(), c.getGreen(), c.getBlue(), 0);
                buffer.vertex(matrix, cos * radius, 0, sin * radius).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static final class Drop {
        Vec3d pos, prevPos;
        final float swayPhase;
        int age;

        Drop(Vec3d pos, boolean snow) {
            this.pos = pos;
            this.prevPos = pos;
            this.swayPhase = snow ? new Random().nextFloat() * (float) (Math.PI * 2) : 0f;
        }
    }

    private static final class Splash {
        final Vec3d pos;
        final long birth = System.currentTimeMillis();

        Splash(Vec3d pos) {
            this.pos = pos;
        }
    }
}
