package dev.darkvisuals.modules.impl.utility;

import com.mojang.authlib.GameProfile;
import dev.darkvisuals.client.ChatUtils;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Nameable;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

 
public class VirtualPlayer extends Module {

      
    private static final int MAX_FAKE_PLAYERS = 50;

    @Getter
    private static VirtualPlayer instance;

      
    public enum SkinModel implements Nameable {
        STEVE("Steve", SkinTextures.Model.WIDE,
                Identifier.ofVanilla("textures/entity/player/wide/steve.png")),
        ALEX("Alex", SkinTextures.Model.SLIM,
                Identifier.ofVanilla("textures/entity/player/slim/alex.png"));

        private final String name;
        private final SkinTextures.Model model;
        private final Identifier texture;

        SkinModel(String name, SkinTextures.Model model, Identifier texture) {
            this.name = name;
            this.model = model;
            this.texture = texture;
        }

        @Override
        public String getName() {
            return name;
        }

          
        public SkinTextures toSkinTextures() {
            return new SkinTextures(texture, null, null, null, model, true);
        }
    }

      
    public enum MainHandItem implements Nameable {
        NONE("Пусто", null),
        NETHERITE_SWORD("Незеритовый меч", Items.NETHERITE_SWORD),
        DIAMOND_SWORD("Алмазный меч", Items.DIAMOND_SWORD),
        NETHERITE_AXE("Незеритовый топор", Items.NETHERITE_AXE),
        MACE("Булава", Items.MACE);

        private final String name;
        private final Item item;

        MainHandItem(String name, Item item) {
            this.name = name;
            this.item = item;
        }

        @Override
        public String getName() {
            return name;
        }

        public ItemStack toStack() {
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
    }

      
    public enum OffHandItem implements Nameable {
        NONE("Пусто", null),
        TOTEM("Тотем", Items.TOTEM_OF_UNDYING),
        SHIELD("Щит", Items.SHIELD);

        private final String name;
        private final Item item;

        OffHandItem(String name, Item item) {
            this.name = name;
            this.item = item;
        }

        @Override
        public String getName() {
            return name;
        }

        public ItemStack toStack() {
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
    }

 
    private static final class FakePlayer extends OtherClientPlayerEntity {
        private volatile SkinTextures skinTextures;

        FakePlayer(ClientWorld world, GameProfile profile, SkinTextures initial) {
            super(world, profile);
            this.skinTextures = initial;
        }

        @Override
        public SkinTextures getSkinTextures() {
            return skinTextures;
        }

        void setSkin(SkinTextures textures) {
            this.skinTextures = textures;
        }
    }

     

    @Getter
    private final StringSetting nickname = new StringSetting("Никнейм", "VirtualPlayer", false);

    @Getter
    private final EnumSetting<SkinModel> skin = new EnumSetting<>("Модель скина", SkinModel.STEVE);

    @Getter
    private final BooleanSetting nickSkin = new BooleanSetting("Скин по нику", true);

    @Getter
    private final BooleanSetting wearArmor = new BooleanSetting("Надеть сет брони", true);

    @Getter
    private final EnumSetting<MainHandItem> mainHand = new EnumSetting<>("Главная рука", MainHandItem.NETHERITE_SWORD);

    @Getter
    private final EnumSetting<OffHandItem> offHand = new EnumSetting<>("Вторая рука", OffHandItem.TOTEM);

    private final ButtonSetting addButton = new ButtonSetting("Добавить игрока", this::spawnFakePlayer);
    private final ButtonSetting removeLastButton = new ButtonSetting("Удалить последнего", this::removeLastFakePlayer);
    private final ButtonSetting removeAllButton = new ButtonSetting("Удалить всех", this::removeAllFakePlayers);

      
    private static int fakeIdCounter = -1337;

      
    private final Deque<OtherClientPlayerEntity> fakePlayers = new ArrayDeque<>();

    public VirtualPlayer() {
        super("VirtualPlayer", Category.Utility,
                "Спавнит клиентских фейковых игроков для теста рендера моделей, скинов и экипировки");
        instance = this;
        getSettings().add(nickname);
        getSettings().add(skin);
        getSettings().add(nickSkin);
        getSettings().add(wearArmor);
        getSettings().add(mainHand);
        getSettings().add(offHand);
        getSettings().add(addButton);
        getSettings().add(removeLastButton);
        getSettings().add(removeAllButton);
    }

      
    private void spawnFakePlayer() {
        if (fullNullCheck() || !isToggled()) return;

        if (fakePlayers.size() >= MAX_FAKE_PLAYERS) {
            ChatUtils.sendMessage("Достигнут лимит виртуальных игроков (" + MAX_FAKE_PLAYERS + "). Удалите лишних перед спавном.");
            return;
        }

        String name = sanitizeNick(nickname.getValue());
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        SkinModel model = skin.getValue();

        FakePlayer entity = new FakePlayer(mc.world, profile, model.toSkinTextures());

         
         
        if (nickSkin.getValue()) {
            SkullBlockEntity.fetchProfileByName(name).thenAccept(opt ->
                    opt.ifPresent(realProfile ->
                            mc.getSkinProvider().fetchSkinTextures(realProfile).thenAccept(texOpt ->
                                    texOpt.ifPresent(tex ->
                                            mc.execute(() -> {
                                                if (!entity.isRemoved()) entity.setSkin(tex);
                                            })))));
        }

        entity.setId(fakeIdCounter--);

         
        Vec3d pos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        entity.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
        entity.setHeadYaw(yaw);
        entity.setBodyYaw(yaw);
         
        entity.lastRenderX = pos.x;
        entity.lastRenderY = pos.y;
        entity.lastRenderZ = pos.z;

        applyEquipment(entity);

         
        mc.world.addEntity(entity);
        fakePlayers.addLast(entity);
    }

      
    private void applyEquipment(OtherClientPlayerEntity entity) {
        if (wearArmor.getValue()) {
            entity.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
            entity.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            entity.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
            entity.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        }
        entity.equipStack(EquipmentSlot.MAINHAND, mainHand.getValue().toStack());
        entity.equipStack(EquipmentSlot.OFFHAND, offHand.getValue().toStack());
    }

      
    private void removeLastFakePlayer() {
        OtherClientPlayerEntity last = fakePlayers.pollLast();
        if (last == null) return;
        discardEntity(last);
    }

      
    private void removeAllFakePlayers() {
        while (!fakePlayers.isEmpty()) {
            discardEntity(fakePlayers.pollLast());
        }
    }

    private void discardEntity(OtherClientPlayerEntity entity) {
        if (mc.world != null) {
            mc.world.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        }
        entity.discard();
    }

      
    private static String sanitizeNick(String raw) {
        String cleaned = raw == null ? "" : raw.replaceAll("[^a-zA-Z0-9_]", "");
        if (cleaned.isEmpty()) cleaned = "VirtualPlayer";
        return cleaned.length() > 16 ? cleaned.substring(0, 16) : cleaned;
    }

 
    @EventHandler
    public void onTick(EventTick e) {
        if (fakePlayers.isEmpty()) return;
        if (mc.world == null) {
            fakePlayers.clear();  
            return;
        }
        fakePlayers.removeIf(p -> p.isRemoved() || p.getWorld() != mc.world);
    }

    @Override
    public void onDisable() {
        removeAllFakePlayers();
        super.onDisable();
    }
}