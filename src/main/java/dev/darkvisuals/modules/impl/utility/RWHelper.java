package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.ChatUtils;
import dev.darkvisuals.client.events.impl.EventPlayerTick;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

 
public class RWHelper extends Module {

    @Getter
    private static RWHelper instance;

    @Getter
    private final BooleanSetting antiRodPull = new BooleanSetting("Не притягивать меня удочкой", true);
    @Getter
    private final BooleanSetting blockBadWords = new BooleanSetting("Защищать от запрещённых слов", true);
    @Getter
    private final BooleanSetting itemRadius = new BooleanSetting("Радиус действия предметов", true);
    @Getter
    private final BooleanSetting mentionNotify = new BooleanSetting("Уведомление при упоминании вашего ника", true);

      
    private static final List<String> BLOCKED_WORDS = List.of(
            "Юпитер", "Метеор", "Дельта", "Флюгер", "Целестиал",
            "Delta", "Celestial", "Fluger", "FlugerNew", "Целка",
            "Meteor", "Фантайм", "Funtime", "Нурик", "Нурсултан",
            "Nursultan", "Венус"
    );

      
    private static final Map<String, DonateItem> DONATE_ITEMS = new LinkedHashMap<>();

    static {
         
        DONATE_ITEMS.put("ловушка", new DonateItem(Shape.SQUARE, 6.0f));
         
        DONATE_ITEMS.put("шар хаоса", new DonateItem(Shape.CIRCLE, 8.0f));
        DONATE_ITEMS.put("шар света", new DonateItem(Shape.CIRCLE, 16.0f));
         
        DONATE_ITEMS.put("шар огня", new DonateItem(Shape.NONE, 0f));
        DONATE_ITEMS.put("шар воздуха", new DonateItem(Shape.NONE, 0f));
        DONATE_ITEMS.put("шар порядка", new DonateItem(Shape.NONE, 0f));
        DONATE_ITEMS.put("шар воды", new DonateItem(Shape.NONE, 0f));
        DONATE_ITEMS.put("шар земли", new DonateItem(Shape.NONE, 0f));
    }

     
      
    private static final double HOOK_RANGE_SQ = 64.0D;   
      
    private static final int PROTECT_TICKS = 15;
    private int protectTicks = 0;
    private Vec3d safeVelocity = Vec3d.ZERO;

     
    private static final long MENTION_COOLDOWN = 1500L;
    private long lastMention = 0L;

     
    private static final int CIRCLE_SEGMENTS = 72;
    private static final int SQUARE_ALPHA = (int) (255 * 0.35f);  

    public RWHelper() {
        super("RWHelper", Category.Utility, "Помощник для сервера Really World: защита от удочки, фильтр слов, радиусы донат-предметов и упоминания ника");
        instance = this;
        getSettings().add(antiRodPull);
        getSettings().add(blockBadWords);
        getSettings().add(itemRadius);
        getSettings().add(mentionNotify);
    }

      
      
      

 
    public boolean shouldCancelVelocityPacket(int entityId) {
        if (!antiRodPull.getValue()) return false;
        if (mc.player == null || mc.world == null) return false;
        if (entityId != mc.player.getId()) return false;

         
        if (mc.player.hurtTime > 0) return false;

         
        if (!hasForeignBobberOnMe()) return false;

        protectTicks = PROTECT_TICKS;
        safeVelocity = mc.player.getVelocity();
        return true;
    }

    public boolean shouldCancelExplosionKnockback() {
        if (!antiRodPull.getValue()) return false;
        if (mc.player == null || mc.world == null) return false;
        if (mc.player.hurtTime > 0) return false;

        if (!hasForeignBobberOnMe()) return false;

        protectTicks = PROTECT_TICKS;
        safeVelocity = mc.player.getVelocity();
        return true;
    }

    public boolean shouldCancelPositionLookVelocity(net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket packet) {
        if (!antiRodPull.getValue()) return false;
        if (mc.player == null || mc.world == null) return false;
        if (mc.player.hurtTime > 0) return false;

        var relatives = packet.relatives();
        var change = packet.change();

        boolean posRelative = relatives.contains(net.minecraft.network.packet.s2c.play.PositionFlag.X)
                && relatives.contains(net.minecraft.network.packet.s2c.play.PositionFlag.Y)
                && relatives.contains(net.minecraft.network.packet.s2c.play.PositionFlag.Z);
        if (!posRelative) return false;

        Vec3d pos = change.position();
        if (Math.abs(pos.x) > 1.0E-4 || Math.abs(pos.y) > 1.0E-4 || Math.abs(pos.z) > 1.0E-4) {
            return false;
        }

        if (!hasForeignBobberOnMe()) return false;

        protectTicks = PROTECT_TICKS;
        safeVelocity = mc.player.getVelocity();
        return true;
    }

      
    public boolean hasForeignBobberOnMe() {
        if (mc.player == null || mc.world == null) return false;
        ClientPlayerEntity player = mc.player;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof FishingBobberEntity bobber)) continue;
            PlayerEntity owner = bobber.getPlayerOwner();
            if (owner == null || owner == player) continue;

            if (bobber.getHookedEntity() == player) return true;
            if (bobber.squaredDistanceTo(player) <= HOOK_RANGE_SQ) return true;
        }
        return false;
    }

 
    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;
        if (!antiRodPull.getValue()) {
            protectTicks = 0;
            return;
        }

        ClientPlayerEntity player = mc.player;

         
         
        if (player.hurtTime > 0) {
            protectTicks = 0;
            safeVelocity = player.getVelocity();
            return;
        }

        if (hasForeignBobberOnMe()) {
            protectTicks = PROTECT_TICKS;
            Vec3d velocity = player.getVelocity();
            double horizontal = Math.hypot(velocity.x, velocity.z);
            double safeHorizontal = Math.hypot(safeVelocity.x, safeVelocity.z);
            if (horizontal > safeHorizontal + 0.05D || velocity.y > safeVelocity.y + 0.05D) {
                player.setVelocity(safeVelocity.x, Math.min(velocity.y, safeVelocity.y), safeVelocity.z);
            } else {
                safeVelocity = velocity;
            }
            return;
        }

        if (protectTicks <= 0) {
            safeVelocity = player.getVelocity();
            return;
        }
        protectTicks--;

        Vec3d velocity = player.getVelocity();
        double horizontal = Math.hypot(velocity.x, velocity.z);
        double safeHorizontal = Math.hypot(safeVelocity.x, safeVelocity.z);

        if (horizontal > safeHorizontal + 0.05D || velocity.y > safeVelocity.y + 0.05D) {
            player.setVelocity(safeVelocity.x, Math.min(velocity.y, safeVelocity.y), safeVelocity.z);
        } else {
            safeVelocity = velocity;
        }
    }

      
      
      

 
    public String findBlockedWord(String message) {
        if (message == null || message.isBlank()) return null;
        String lower = message.toLowerCase(Locale.ROOT);
        for (String word : BLOCKED_WORDS) {
            if (lower.contains(word.toLowerCase(Locale.ROOT))) return word;
        }
        return null;
    }

      
    public void notifyBlockedWord(String word) {
        ChatUtils.sendMessage("Слово \"" + word + "\" запрещено на сервере Really World, сообщение не отправлен��");
        addNotify("Слово \"" + word + "\" запрещено на Really World");
    }

      
      
      

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        if (!itemRadius.getValue()) return;

        try (var __ = Perf.scopeCpu("RWHelper.onRender3D")) {
            DonateItem item = getHeldDonateItem();
            if (item == null || item.shape() == Shape.NONE) return;

            Color theme = ThemeManager.getInstance().getCurrentTheme().getBackgroundColor();
            ClientPlayerEntity player = mc.player;
            float tickDelta = Render3D.getTickDelta();
            double x = player.prevX + (player.getX() - player.prevX) * tickDelta;
            double y = player.prevY + (player.getY() - player.prevY) * tickDelta;
            double z = player.prevZ + (player.getZ() - player.prevZ) * tickDelta;

            if (item.shape() == Shape.SQUARE) {
                renderSquare(e, new Vec3d(x, y, z), item.size(), theme);
            } else {
                renderCircle(new Vec3d(x, y, z), item.size(), theme);
            }
        }
    }

      
    private void renderSquare(EventRender3D.Game e, Vec3d center, float size, Color theme) {
        double half = size / 2.0D;
        Box box = new Box(
                center.x - half, center.y + 0.01D, center.z - half,
                center.x + half, center.y + 0.03D, center.z + half
        );
        Color fill = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), SQUARE_ALPHA);
        Color outline = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 220);
        Render3D.renderBox(e.getMatrices(), box, fill);
        Render3D.renderBoxOutline(e.getMatrices(), box, outline);
    }

      
    private void renderCircle(Vec3d center, float radius, Color theme) {
        int argb = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 235).getRGB();
        Render3D.DEBUG_LINE_WIDTH = 2.0f;

        double y = center.y + 0.02D;
        Vec3d prev = null;
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double angle = (Math.PI * 2 * i) / CIRCLE_SEGMENTS;
            Vec3d point = new Vec3d(
                    center.x + Math.cos(angle) * radius,
                    y,
                    center.z + Math.sin(angle) * radius
            );
            if (prev != null) Render3D.drawLine(prev, point, argb, Render3D.DEBUG_LINE_WIDTH);
            prev = point;
        }
    }

      
    private DonateItem getHeldDonateItem() {
        DonateItem main = resolveDonateItem(mc.player.getMainHandStack());
        if (main != null) return main;
        return resolveDonateItem(mc.player.getOffHandStack());
    }

    private DonateItem resolveDonateItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String name = stripFormatting(stack.getName().getString()).toLowerCase(Locale.ROOT).trim();
        if (name.isEmpty()) return null;
        for (Map.Entry<String, DonateItem> entry : DONATE_ITEMS.entrySet()) {
            if (name.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

      
      
      

 
    public void handleIncomingMessage(String raw) {
        if (!mentionNotify.getValue()) return;
        if (raw == null || raw.isBlank()) return;
        if (mc.getSession() == null) return;

        String self = mc.getSession().getUsername();
        if (self == null || self.isBlank()) return;

        String message = stripFormatting(raw);
        String sender = parseSender(message);
         
        if (sender != null && sender.equalsIgnoreCase(self)) return;

         
        String body = sender != null ? message.substring(message.indexOf(sender) + sender.length()) : message;
        if (!containsWord(body, self)) return;

        long now = System.currentTimeMillis();
        if (now - lastMention < MENTION_COOLDOWN) return;
        lastMention = now;

        String who = sender != null ? sender : "Кто-то";
        addNotify("[" + who + "] Написал ваш ник!");
        ChatUtils.sendMessage("[" + who + "] Написал ваш ник!");
    }

      
    private String parseSender(String message) {
        int colon = message.indexOf(':');
        int bracketOpen = message.indexOf('<');
        int bracketClose = message.indexOf('>');

        if (bracketOpen >= 0 && bracketClose > bracketOpen) {
            String candidate = message.substring(bracketOpen + 1, bracketClose).trim();
            if (isNickLike(candidate)) return candidate;
        }
        if (colon > 0) {
            String head = message.substring(0, colon);
            String[] parts = head.split("[\\s\\[\\]|»→⇨]+");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (isNickLike(candidate)) return candidate;
            }
        }
        return null;
    }

    private boolean isNickLike(String value) {
        if (value.length() < 3 || value.length() > 16) return false;
        for (char c : value.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') return false;
        }
        return true;
    }

    private boolean containsWord(String text, String word) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerWord = word.toLowerCase(Locale.ROOT);
        int index = lowerText.indexOf(lowerWord);
        while (index >= 0) {
            int end = index + lowerWord.length();
            boolean leftOk = index == 0 || !isNickChar(lowerText.charAt(index - 1));
            boolean rightOk = end >= lowerText.length() || !isNickChar(lowerText.charAt(end));
            if (leftOk && rightOk) return true;
            index = lowerText.indexOf(lowerWord, index + 1);
        }
        return false;
    }

    private boolean isNickChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

      
      
      

    private void addNotify(String text) {
        try {
            darkvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.dangerIcon, text, 2500));
        } catch (Throwable ignored) {}
    }

    private static String stripFormatting(String input) {
        return input.replaceAll("§.", "");
    }

    private enum Shape {
        NONE, CIRCLE, SQUARE
    }

    private record DonateItem(Shape shape, float size) {}
}
