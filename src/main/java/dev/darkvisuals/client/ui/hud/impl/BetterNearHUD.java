package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.ui.hud.HudStyle;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Font;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

 
public class BetterNearHUD extends HudElement implements ThemeManager.ThemeChangeListener {

     
    public static boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

     
    private static final double MAX_RANGE = 50.0;  
    private static final int MAX_PLAYERS = 5;      

     
    private static final float PADDING = 8f;       
    private static final float ROW_HEIGHT = 14f;   
    private static final float ROW_GAP = 3f;       
    private static final float HEADER_H = 14f;     
    private static final float AVATAR = 10f;       
    private static final float ICON = 11f;         
    private static final float RADIUS = 6f;        
    private static final float MIN_WIDTH = 108f;   

     
    private static final Color NAME_COLOR = Color.WHITE;                        
    private static final Color TITLE_COLOR = Color.WHITE;                       
    private Color accent = new Color(255, 140, 40);                             

     
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

     
    private static final Identifier ICON_ACCOUNTS = Identifier.of("darkvisuals", "hud/accounts.png");

    public BetterNearHUD() {
        super("BetterNearHUD");
        ThemeManager tm = ThemeManager.getInstance();
        if (tm != null) {
            accent = tm.getCurrentTheme().getAccentColor();
            tm.addThemeChangeListener(this);
        }
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.accent = theme.getAccentColor();
    }

      
    private record NearEntry(AbstractClientPlayerEntity player, double distance, float relativeYaw, double deltaY) {}

    @Override
    public void onRender2D(EventRender2D e) {
         
        if (!enabled) return;
        if (fullNullCheck() || closed()) return;

        ClientPlayerEntity self = mc.player;
        if (self == null || mc.world == null) return;

        List<NearEntry> nearby = collectNearbyPlayers(self);

        MatrixStack matrices = e.getContext().getMatrices();
        Font bold = Fonts.BOLD;
        Font medium = Fonts.MEDIUM;
        float nameSize = 6.5f;
        float distSize = 6f;
        float titleSize = 7f;

         
        float contentWidth = MIN_WIDTH;
        for (NearEntry entry : nearby) {
            float rowW = AVATAR + 5f
                    + medium.getWidth(entry.player().getGameProfile().getName(), nameSize)
                    + 8f + medium.getWidth(formatDistance(entry.distance()), distSize)
                    + 14f;  
            contentWidth = Math.max(contentWidth, rowW);
        }

        float rowsHeight = nearby.isEmpty()
                ? ROW_HEIGHT
                : nearby.size() * ROW_HEIGHT + (nearby.size() - 1) * ROW_GAP;

        float totalWidth = PADDING * 2 + contentWidth;
        float totalHeight = PADDING * 2 + HEADER_H + 4f + rowsHeight;

        setBounds(getX(), getY(), totalWidth, totalHeight);

        float x = getX(), y = getY();
        float fade = toggledAnimation.getValue();

         
        drawPanelBackground(matrices, x, y, totalWidth, totalHeight, fade);

         
        float iconX = x + PADDING;
        float iconY = y + PADDING + (HEADER_H - ICON) / 2f;

        Render2D.drawTexture(matrices, iconX, iconY, ICON, ICON, 0f,
                ICON_ACCOUNTS, withAlpha(accent, fade));

        float titleX = iconX + ICON + 5f;
        float titleY = y + PADDING + (HEADER_H - bold.getHeight(titleSize)) / 2f;
        Render2D.drawFont(matrices, bold.getFont(titleSize), "Better Near", titleX, titleY, withAlpha(TITLE_COLOR, fade));

         
        float rowY = y + PADDING + HEADER_H + 4f;

        if (nearby.isEmpty()) {
            Render2D.drawFont(matrices, medium.getFont(distSize), "No players nearby",
                    x + PADDING, rowY + (ROW_HEIGHT - medium.getHeight(distSize)) / 2f,
                    withAlpha(new Color(160, 160, 165), fade));
        } else {
            for (NearEntry entry : nearby) {
                drawPlayerRow(matrices, entry, x + PADDING, rowY, contentWidth, fade, medium, nameSize, distSize);
                rowY += ROW_HEIGHT + ROW_GAP;
            }
        }

         
        super.onRender2D(e);
    }

 
    private void drawPanelBackground(MatrixStack matrices, float x, float y,
                                     float w, float h, float fade) {
        Render2D.drawHudBackground(matrices, x, y, w, h, RADIUS, fade);
    }

      
    private List<NearEntry> collectNearbyPlayers(ClientPlayerEntity self) {
        List<NearEntry> result = new ArrayList<>();

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == self) continue;                     
            if (player.isInvisible()) continue;               
            if (player.isSpectator()) continue;               
            if (player.isRemoved() || !player.isAlive()) continue;

            double distance = self.distanceTo(player);
            if (distance > MAX_RANGE) continue;

             
            double dx = player.getX() - self.getX();
            double dz = player.getZ() - self.getZ();
            float angleTo = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);

             
            float relative = MathHelper.wrapDegrees(angleTo - self.getYaw());

            result.add(new NearEntry(player, distance, relative, player.getY() - self.getY()));
        }

        result.sort(Comparator.comparingDouble(NearEntry::distance));
        return result.size() > MAX_PLAYERS ? new ArrayList<>(result.subList(0, MAX_PLAYERS)) : result;
    }

      
    private void drawPlayerRow(MatrixStack matrices, NearEntry entry,
                               float rowX, float rowY, float contentWidth, float fade,
                               Font font, float nameSize, float distSize) {
        AbstractClientPlayerEntity player = entry.player();

         
        float avatarY = rowY + (ROW_HEIGHT - AVATAR) / 2f;
        Render2D.drawTexture(matrices, rowX, avatarY, AVATAR, AVATAR, 2f,
                0.125f, 0.125f, 0.125f, 0.125f,
                player.getSkinTextures().texture(),
                withAlpha(Color.WHITE, fade));

         
        float nameX = rowX + AVATAR + 5f;
        float nameY = rowY + (ROW_HEIGHT - font.getHeight(nameSize)) / 2f;
        Render2D.drawFont(matrices, font.getFont(nameSize),
                player.getGameProfile().getName(), nameX, nameY, withAlpha(NAME_COLOR, fade));

         
        Font arrowFont = Fonts.BOLD;
        float arrowSize = nameSize + 1.5f;
        String arrow = pickArrow(entry.relativeYaw(), entry.deltaY());
        float arrowW = arrowFont.getWidth(arrow, arrowSize);
        float arrowX = rowX + contentWidth - arrowW;
        Render2D.drawFont(matrices, arrowFont.getFont(arrowSize), arrow,
                arrowX, rowY + (ROW_HEIGHT - arrowFont.getHeight(arrowSize)) / 2f, withAlpha(accent, fade));

         
        Color distColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150);
        String dist = formatDistance(entry.distance());
        float distW = font.getWidth(dist, distSize);
        float distX = arrowX - 7f - distW;
        Render2D.drawFont(matrices, font.getFont(distSize), dist,
                distX, rowY + (ROW_HEIGHT - font.getHeight(distSize)) / 2f, withAlpha(distColor, fade));
    }

 
    private String pickArrow(float relativeYaw, double deltaY) {
        if (deltaY > 6.0) return "↑";   
        if (deltaY < -6.0) return "↓";  

        float normalized = MathHelper.wrapDegrees(relativeYaw) + 180f;  
        int index = Math.round(((normalized + 180f) % 360f) / 45f) % 8;
        return ARROWS[index];
    }

      
    private String formatDistance(double distance) {
        return Math.round(distance) + "m";
    }

      
    private static Color withAlpha(Color base, float fade) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(),
                MathHelper.clamp((int) (base.getAlpha() * fade), 0, 255));
    }
}