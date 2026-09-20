package dev.darkvisuals.client.ui.cosmetics;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.util.animations.SmoothAnimation;
import dev.darkvisuals.client.util.models.BlackWings;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Font;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.impl.render.Cosmetics;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

 
public final class CosmeticsScreen extends Screen {

     
    private static final Color ACCENT       = new Color(0x6B, 0x11, 0x7B);
    private static final Color ACCENT_BRIGHT = new Color(0x9A, 0x24, 0xB0);
    private static final Color PANEL_BG      = new Color(0x0D, 0x0D, 0x10, 244);
    private static final Color PANEL_BORDER  = new Color(0xFF, 0xFF, 0xFF, 26);
    private static final Color ROW_BG        = new Color(0x1B, 0x1B, 0x1F, 235);
    private static final Color ROW_ENABLED   = new Color(0x6B, 0x11, 0x7B, 70);
    private static final Color ICON_BG       = new Color(0x26, 0x26, 0x2B, 255);
    private static final Color PREVIEW_BG     = new Color(0x121214);
    private static final Color REMOVE_BG      = new Color(0x24, 0x24, 0x29, 255);
    private static final Color TEXT_PRIMARY   = new Color(0xF2, 0xF2, 0xF6);
    private static final Color TEXT_SECONDARY = new Color(0x97, 0x97, 0xA0);
    private static final Color STAR_ON        = new Color(0xE7, 0xC4, 0x5A);
    private static final Color STAR_OFF       = new Color(0x55, 0x55, 0x5C);
    private static final Color DIVIDER        = new Color(0xFF, 0xFF, 0xFF, 30);

    private final Cosmetics module;

    private final SmoothAnimation open = new SmoothAnimation(0f, 14f);
    private boolean closing = false;

    private final List<Item> items = new ArrayList<>();
    private String selectedId;

    private float scroll = 0f, targetScroll = 0f;

    private final Preferences prefs = Preferences.userRoot().node("darkvisuals/cosmetics");

    private Perspective previousPerspective;
    private boolean perspectiveForced = false;

     
    private float pX, pY, pW, pH;
    private float listX, listY, listW, listBottom;
    private float rightX, rightW;
    private float applyX, applyY, applyW, applyH;
    private float removeX, removeY, removeW, removeH;
    private float previewX, previewY, previewW, previewH;
    private static final float ROW_H = 50f;
    private static final float ROW_GAP = 8f;

    public CosmeticsScreen(Cosmetics module) {
        super(Text.of("darkvisuals-cosmetics"));
        this.module = module;
        buildItems();
    }

    private void buildItems() {
        items.clear();
        items.add(new Item("nimbus", "Нимб", "Светящееся кольцо над головой", module.getNimbusSetting()));
        items.add(new Item("black", "Чёрные Крылья", "Обычные чёрные крылья", module.getBlackWingsSetting()));
        items.add(new Item("w1", "Крылья 1", "Крылья v1", module.getWingsSetting()));
        items.add(new Item("w2", "Крылья 2", "Крылья v2", module.getWings2Setting()));
        items.add(new Item("cap", "Кепка", "Кепка с пропеллером", module.getCapSetting()));
        items.add(new Item("china", "Шляпа", "Коническая шляпа в цвет темы", module.getChinaHatSetting()));
        items.add(new Item("kagune", "Кагуне", "Живые щупальца за спиной, как в Tokyo Ghoul", module.getKaguneSetting()));
        items.add(new Item("morty", "Повязка Морти", "Повязка Злого Морти.", module.getMortyPatchSetting()));
        items.add(new Item("nike", "Кепка Nike", "Чёрная бейсболка Nike с белым свушем в майнкрафт-стиле", module.getNikeCapSetting()));
        items.add(new Item("robot_tentacles", "Робо-щупальца", "Механические щупальца с сервоприводами и клешнями", module.getRobotTentaclesSetting()));
        loadFavorites();
        sortItems();
         
        selectedId = items.get(0).id;
        for (Item it : items) {
            if (it.setting.getValue()) { selectedId = it.id; break; }
        }
        module.setPreviewOverride(selectedId);
    }

    @Override
    protected void init() {
        super.init();
        if (!closing) open.setTarget(1f);
        module.setPreviewOverride(selectedId);

         
         
        if (!perspectiveForced && this.client != null) {
            previousPerspective = this.client.options.getPerspective();
            if (previousPerspective.isFirstPerson()) {
                this.client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
            perspectiveForced = true;
        }
    }

     

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        float p = open.update();
        if (closing && p <= 0.02f) {
            closing = false;
            open.snapTo(0f);
            module.clearPreviewOverride();
            this.client.setScreen(null);
            return;
        }
        if (p <= 0.02f) return;

        MatrixStack ms = context.getMatrices();

         
        Render2D.drawRect(ms, 0, 0, this.width, this.height, new Color(0, 0, 0, c255(150 * p)));

         
        pW = 460f;
        pH = 300f;
        pX = (this.width - pW) / 2f;
        pY = (this.height - pH) / 2f;

        float cx = pX + pW / 2f;
        float cy = pY + pH / 2f;
        float sc = 0.96f + 0.04f * p;

        ms.push();
        ms.translate(cx, cy, 0);
        ms.scale(sc, sc, 1f);
        ms.translate(-cx, -cy, 0);

         
        Render2D.drawRoundedRect(ms, pX, pY, pW, pH, 14f, fade(PANEL_BG, p));
        Render2D.drawBorder(ms, pX, pY, pW, pH, 14f, 0f, 1f, fade(PANEL_BORDER, p));

         
        Render2D.drawRoundedRect(ms, pX + 14f, pY + 54f, 2f, pH - 72f, 1f,
                fade(new Color(0xFF, 0xFF, 0xFF, 40), p));

         
        Render2D.drawFont(ms, Fonts.BOLD.getFont(13f), "Cosmetics", pX + 22f, pY + 16f, fade(TEXT_PRIMARY, p));
        Render2D.drawRect(ms, pX + 18f, pY + 42f, pW - 36f, 1f, fade(DIVIDER, p));

         
        rightW = 128f;
        rightX = pX + pW - rightW - 18f;

         
        listX = pX + 22f;
        listY = pY + 56f;
        listW = rightX - 14f - listX;
        listBottom = pY + pH - 18f;

        renderList(context, ms, mouseX, mouseY, p);

         
        String prev = "Предпросмотр";
        float pw = Fonts.SEMIBOLD.getWidth(prev, 8.5f);
        Render2D.drawFont(ms, Fonts.SEMIBOLD.getFont(8.5f), prev,
                rightX + (rightW - pw) / 2f, pY + 60f, fade(TEXT_SECONDARY, p));

         
        previewX = rightX;
        previewY = pY + 80f;
        previewW = rightW;
        previewH = 116f;
        Render2D.drawRoundedRect(ms, previewX, previewY, previewW, previewH, 10f, fade(PREVIEW_BG, p));
        Render2D.drawBorder(ms, previewX, previewY, previewW, previewH, 10f, 0f, 1f, fade(PANEL_BORDER, p));

         
        applyX = rightX; applyW = rightW; applyH = 26f; applyY = pY + pH - 60f;
        removeX = rightX; removeW = rightW; removeH = 22f; removeY = pY + pH - 28f;

        boolean applyHover = inside(mouseX, mouseY, applyX, applyY, applyW, applyH);
        boolean removeHover = inside(mouseX, mouseY, removeX, removeY, removeW, removeH);

        Render2D.drawRoundedRect(ms, applyX, applyY, applyW, applyH, 8f,
                fade(applyHover ? ACCENT_BRIGHT : ACCENT, p));
        drawCentered(ms, Fonts.SEMIBOLD, "Применить", 8.5f, applyX, applyY, applyW, applyH, fade(TEXT_PRIMARY, p));

        Render2D.drawRoundedRect(ms, removeX, removeY, removeW, removeH, 8f,
                fade(removeHover ? new Color(0x30, 0x30, 0x36) : REMOVE_BG, p));
        drawCentered(ms, Fonts.SEMIBOLD, "Снять", 8.5f, removeX, removeY, removeW, removeH, fade(TEXT_PRIMARY, p));

        ms.pop();

         
        renderPreviewModel(context, mouseX, mouseY, p);
    }

    private void renderList(DrawContext context, MatrixStack ms, int mouseX, int mouseY, float p) {
        float visible = listBottom - listY;
        float total = items.size() * (ROW_H + ROW_GAP) - ROW_GAP;
        float max = Math.max(0f, total - visible);
        if (targetScroll > max) targetScroll = max;
        if (targetScroll < 0) targetScroll = 0;
        scroll += (targetScroll - scroll) * 0.35f;

        Render2D.startScissor(context, listX, listY, listW, visible);
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            float ry = listY - scroll + i * (ROW_H + ROW_GAP);
            if (ry + ROW_H < listY || ry > listBottom) continue;

            boolean focused = it.id.equals(selectedId);
            boolean enabled = it.setting.getValue();
            boolean hover = inside(mouseX, mouseY, listX, ry, listW, ROW_H) && mouseY >= listY && mouseY <= listBottom;

            Color bg = focused ? ACCENT : (enabled ? ROW_ENABLED : ROW_BG);
            if (hover && !focused) bg = new Color(Math.min(255, bg.getRed() + 12),
                    Math.min(255, bg.getGreen() + 12), Math.min(255, bg.getBlue() + 12), bg.getAlpha());
            Render2D.drawRoundedRect(ms, listX, ry, listW, ROW_H, 12f, fade(bg, p));
            if (focused) Render2D.drawBorder(ms, listX, ry, listW, ROW_H, 12f, 0f, 1f,
                    fade(new Color(0xC9, 0x6C, 0xDC, 160), p));

             
             
             
            float iconSize = 34f;
            float iconX = listX + 8f;
            float iconY = ry + (ROW_H - iconSize) / 2f;
            Render2D.drawRoundedRect(ms, iconX, iconY, iconSize, iconSize, 7f, fade(ICON_BG, p));
            drawCosmeticIcon3D(context, iconX, iconY, iconX + iconSize, iconY + iconSize, it.id);

             
            float tx = iconX + iconSize + 10f;
            Render2D.drawFont(ms, Fonts.SEMIBOLD.getFont(8.5f), it.title, tx, ry + 12f, fade(TEXT_PRIMARY, p));
            Render2D.drawFont(ms, Fonts.REGULAR.getFont(7.5f), it.desc, tx, ry + 27f, fade(TEXT_SECONDARY, p));

             
            float starCX = listX + listW - 22f;
            float starCY = ry + ROW_H / 2f;
            if (it.favorite) starFilled(ms, starCX, starCY, 6.5f, 2.8f, fade(STAR_ON, p));
            else starOutline(ms, starCX, starCY, 6.5f, 2.8f, fade(STAR_OFF, p));
        }
        Render2D.stopScissor(context);
    }

    private void renderPreviewModel(DrawContext context, int mouseX, int mouseY, float p) {
        MatrixStack ms = context.getMatrices();

        if (this.client == null || this.client.player == null) {
            String s = "Нет игрока";
            float w = Fonts.SEMIBOLD.getWidth(s, 9f);
            Render2D.drawFont(ms, Fonts.SEMIBOLD.getFont(9f), s,
                    previewX + (previewW - w) / 2f, previewY + previewH / 2f - 5f, fade(TEXT_SECONDARY, p));
            return;
        }

        Item sel = selected();

         
        if (sel != null && sel.setting.getValue()) {
            Render2D.drawGlow(ms,
                    previewX + previewW / 2f - 20f, previewY + previewH / 2f - 26f, 40f, 52f, 11f,
                    new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), c255(120 * p)), 20f, 4);
        }

         
         
         
         
        float animX = (float) Math.sin((System.currentTimeMillis() % 6000L) / 6000.0 * Math.PI * 2.0) * (previewW * 0.22f);
        float exCx = previewX + previewW / 2f;
        float exFootY = previewY + previewH * 0.35f;

        drawEntityWithCosmetics(context,
                (int)(previewX + 10f), (int)(previewY + 12f), (int)(previewX + previewW - 10f), (int)(previewY + previewH - 12f),
                (int) (previewW * 0.42f), 0.12f,
                exCx + animX, exFootY,
                this.client.player, sel);

        if (this.client.options.getPerspective().isFirstPerson()) {
            String hint = "Крылья видны от 3-го лица (F5)";
            float hw = Fonts.REGULAR.getWidth(hint, 7f);
            Render2D.drawFont(ms, Fonts.REGULAR.getFont(7f), hint,
                    previewX + (previewW - hw) / 2f, previewY + previewH + 8f, fade(TEXT_SECONDARY, p));
        }
    }

 
    private void drawEntityWithCosmetics(DrawContext context, int x1, int y1, int x2, int y2, int size, float yOffset,
                                         double mouseX, double mouseY, PlayerEntity entity, Item cosmetic) {
        if (this.client == null) return;

        float f = (x1 + x2) / 2f;
        float g = (y1 + y2) / 2f;
        context.enableScissor(x1, y1, x2, y2);

        float h = (float) Math.atan((f - mouseX) / 40.0);
        float i = (float) Math.atan((g - mouseY) / 40.0);

        Quaternionf entityRotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraRotation = new Quaternionf().rotateX(i * 20.0F * 0.017453292F);
        entityRotation.mul(cameraRotation);

        float prevBodyYaw = entity.bodyYaw;
        float prevYaw = entity.getYaw();
        float prevPitch = entity.getPitch();
        float prevHeadYaw = entity.headYaw;
        float prevPrevHeadYaw = entity.prevHeadYaw;

        entity.setYaw(180.0F + h * 40.0F);
        entity.setPitch(-i * 20.0F);
        entity.bodyYaw = 180.0F + h * 20.0F;
        entity.headYaw = 180.0F + g * 40.0F;
        entity.prevBodyYaw = entity.bodyYaw;
        entity.prevHeadYaw = entity.headYaw;

        Vector3f offset = new Vector3f(0.0F, entity.getHeight() / 2.0F + yOffset, 0.0F);
        float scale = size / entity.getScale();

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(f, g, 50.0);
        matrices.scale(scale, scale, -scale);
        matrices.translate(offset.x, offset.y, offset.z);
        matrices.multiply(entityRotation);

        DiffuseLighting.disableGuiDepthLighting();

        EntityRenderDispatcher dispatcher = this.client.getEntityRenderDispatcher();
        dispatcher.setRotation(cameraRotation);
        dispatcher.setRenderShadows(false);

        VertexConsumerProvider.Immediate immediate = this.client.getBufferBuilders().getEntityVertexConsumers();
        dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, matrices, immediate, 15728880);
        immediate.draw();


         
        if (cosmetic != null && cosmetic.setting != null) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

             
             
             
             
             
            float guiYaw = entity.bodyYaw + 180.0f;
            this.module.renderCosmeticForGui(cosmetic.id, entity, matrices, guiYaw);
        }

        dispatcher.setRenderShadows(true);
        matrices.pop();
        DiffuseLighting.enableGuiDepthLighting();

        entity.bodyYaw = prevBodyYaw;
        entity.setYaw(prevYaw);
        entity.setPitch(prevPitch);
        entity.headYaw = prevHeadYaw;
        entity.prevHeadYaw = prevPrevHeadYaw;

        context.disableScissor();
    }

 
 
    private void drawCosmeticIcon3D(DrawContext context, float x1, float y1, float x2, float y2, String cosmeticId) {
        if (this.client == null || this.client.player == null) return;
        PlayerEntity entity = this.client.player;

        float f = (x1 + x2) / 2f;
        float g = (y1 + y2) / 2f;
        float tile = x2 - x1;
        context.enableScissor((int) x1, (int) y1, (int) x2, (int) y2);

         
        float span = switch (cosmeticId) {
            case "nimbus" -> 1.15f;
            case "black"  -> 2.6f;
            case "w1"     -> 3.5f;
            case "w2"     -> 3.2f;
            case "cap"    -> 1.4f;   
            case "china"  -> 2.0f;   
            case "kagune" -> 3.0f;    
            case "morty"  -> 1.2f;  
            case "nike"   -> 1.2f;
            case "robot_tentacles" -> 3.0f;
            default       -> 2.6f;
        };
        float anchorY = switch (cosmeticId) {
            case "nimbus" -> entity.getHeight() + 0.1f;
            case "black"  -> 1.45f;
            case "cap"    -> 1.85f;  
            case "china"  -> 1.80f;  
            case "kagune" -> 1.30f;
            case "morty"  -> entity.getEyeHeight(entity.getPose()); 
            case "nike"   -> entity.getEyeHeight(entity.getPose()) + 0.15f;
            case "robot_tentacles" -> 1.30f;
            default       -> 1.35f;
        };

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(f, g, 60.0);
        float iconScale = (tile * 0.92f) / span;
        matrices.scale(iconScale, iconScale, -iconScale);
         
        matrices.multiply(new Quaternionf().rotateZ((float) Math.PI));
        matrices.multiply(new Quaternionf().rotateY((float) Math.PI));
         
        float tiltX = "nimbus".equals(cosmeticId) ? -38f : -12f;
        float tiltY = "nimbus".equals(cosmeticId) ? 0f : 20f;
        matrices.multiply(new Quaternionf().rotateX(tiltX * 0.017453292F));
        matrices.multiply(new Quaternionf().rotateY(tiltY * 0.017453292F));
         
        matrices.translate(0.0, -anchorY, 0.0);

         
         
         
        this.module.renderCosmeticForGui(cosmeticId, entity, matrices, 360.0f);

        matrices.pop();
        context.disableScissor();
    }

     

    private boolean inputLocked() {
        return closing || open.getValue() < 0.85f;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inputLocked()) return true;
        if (button == 0) {
             
            if (inside(mouseX, mouseY, applyX, applyY, applyW, applyH)) {
                Item it = selected();
                if (it != null) it.setting.setValue(true);
                return true;
            }
            if (inside(mouseX, mouseY, removeX, removeY, removeW, removeH)) {
                Item it = selected();
                if (it != null) it.setting.setValue(false);
                return true;
            }
             
            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listBottom) {
                for (int i = 0; i < items.size(); i++) {
                    Item it = items.get(i);
                    float ry = listY - scroll + i * (ROW_H + ROW_GAP);
                    if (mouseY < ry || mouseY > ry + ROW_H) continue;

                    float starCX = listX + listW - 22f;
                    float starCY = ry + ROW_H / 2f;
                    if (dist(mouseX, mouseY, starCX, starCY) <= 9f) {
                        it.favorite = !it.favorite;
                        saveFavorites();
                        sortItems();
                        return true;
                    }
                    selectedId = it.id;
                    module.setPreviewOverride(selectedId);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (inputLocked()) return true;
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listBottom) {
            targetScroll -= (float) vertical * 28f;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            open.setTarget(0f);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        module.clearPreviewOverride();
        if (perspectiveForced && this.client != null && previousPerspective != null) {
            this.client.options.setPerspective(previousPerspective);
            perspectiveForced = false;
        }
        super.removed();
    }

     

    private Item selected() {
        for (Item it : items) if (it.id.equals(selectedId)) return it;
        return items.isEmpty() ? null : items.get(0);
    }

    private void sortItems() {
        items.sort((a, b) -> Boolean.compare(b.favorite, a.favorite));
    }

    private void loadFavorites() {
        String s = prefs.get("favorites", "");
        Set<String> set = new HashSet<>(Arrays.asList(s.split(",")));
        for (Item it : items) it.favorite = set.contains(it.id);
    }

    private void saveFavorites() {
        StringBuilder sb = new StringBuilder();
        for (Item it : items) {
            if (it.favorite) {
                if (sb.length() > 0) sb.append(',');
                sb.append(it.id);
            }
        }
        prefs.put("favorites", sb.toString());
    }

    private void drawCentered(MatrixStack ms, Font font, String text, float size,
                              float x, float y, float w, float h, Color color) {
        float tw = font.getWidth(text, size);
        Render2D.drawFont(ms, font.getFont(size), text, x + (w - tw) / 2f, y + (h - size) / 2f, color);
    }

    private static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static double dist(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static int c255(float v) {
        return Math.max(0, Math.min(255, (int) v));
    }

    private static Color fade(Color col, float p) {
        return new Color(col.getRed(), col.getGreen(), col.getBlue(), c255(col.getAlpha() * p));
    }

     
    private void starFilled(MatrixStack ms, float cx, float cy, float rOut, float rIn, Color color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f m = ms.peek().getPositionMatrix();
        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f, a = color.getAlpha() / 255f;
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        bb.vertex(m, cx, cy, 0).color(r, g, b, a);
        for (int i = 0; i <= 10; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / 5.0;
            float rad = (i % 2 == 0) ? rOut : rIn;
            bb.vertex(m, (float) (cx + Math.cos(ang) * rad), (float) (cy + Math.sin(ang) * rad), 0).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderSystem.disableBlend();
    }

     
    private void starOutline(MatrixStack ms, float cx, float cy, float rOut, float rIn, Color color) {
        float[] xs = new float[10];
        float[] ys = new float[10];
        for (int i = 0; i < 10; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / 5.0;
            float rad = (i % 2 == 0) ? rOut : rIn;
            xs[i] = (float) (cx + Math.cos(ang) * rad);
            ys[i] = (float) (cy + Math.sin(ang) * rad);
        }
        for (int i = 0; i < 10; i++) {
            int j = (i + 1) % 10;
            Render2D.drawLine(ms, xs[i], ys[i], xs[j], ys[j], 1f, color);
        }
    }

    private static final class Item {
        final String id, title, desc;
        final BooleanSetting setting;
        boolean favorite;

        Item(String id, String title, String desc, BooleanSetting setting) {
            this.id = id;
            this.title = title;
            this.desc = desc;
            this.setting = setting;
        }
    }
}