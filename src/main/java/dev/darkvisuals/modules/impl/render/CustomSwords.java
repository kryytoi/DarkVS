package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Nameable;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.List;

 public class CustomSwords extends Module {

      
    public enum Weapon implements Nameable {
        ABOMINABLE_BLADE("Abominable Blade"),
        ABOMINABLE_GREAT_SABER("Abominable Great Saber"),
        ABOMINABLE_SCYTHE("Abominable Scythe"),
        ACIDIC_CLEAVER("Acidic Cleaver"),
        AMETHYST_SHURIKEN("Amethyst Shuriken"),
        ANCIENT_ROYAL_GREAT_SWORD("Ancient Royal Great Sword"),
        AQUATIC_SACRED_BLADE("Aquatic Sacred Blade"),
        ARCANETHYST("Arcanethyst"),
        ASHURAS_BLADE("Ashura's Blade"),
        AWAKENED_LICHBLADE("Awakened Lichblade"),
        BLOOD_EDGE("Blood Edge"),
        BLOODY_DEATH("Bloody Death"),
        BRAMBLETHORN("Bramblethorn"),
        BRIMSTONE_CLAYMORE("Brimstone Claymore"),
        CARIAN_KNIGHTS_SWORD("Carian Knight's Sword"),
        CHRONO_BLADE("Chrono Blade"),
        CORRUPTED_MYTHIC_BLADE("Corrupted Mythic Blade"),
        CREATION_SPLITTER("Creation Splitter"),
        CRESCENT_ROSE("Crescent Rose"),
        CYBER_KATANA("Cyber Katana"),
        CYBER_MANTIS_BLADE("Cyber Mantis Blade"),
        CYBER_SWORD("Cyber Sword"),
        CYBERNETIC_CHAINSAW_BLADE("Cybernetic Chainsaw Blade"),
        CYBERNETIC_KATANA("Cybernetic Katana"),
        CYBERNETIC_KNIFE("Cybernetic Knife"),
        DAINSLEIF("Dainsleif"),
        DARK_BLADE("Dark Blade"),
        DARK_CLEAVER("Dark Cleaver"),
        DEATH_KNIGHTS_DAGGER("Death Knight's Dagger"),
        DEATH_KNIGHTS_SWORD("Death Knight's Sword"),
        DEMIGODS_UNHOLY_BLADE("Demigod's Unholy Blade"),
        DEMIGODS_UNHOLY_HALBERD("Demigod's Unholy Halberd"),
        DEMON_LORDS_GREAT_AXE("Demon Lord's Great Axe"),
        DEMON_LORDS_SWORD("Demon Lord's Sword"),
        DEMONIC_BLADE("Demonic Blade"),
        DEMONIC_CLEAVER("Demonic Cleaver"),
        DIVINE_AXE_RHITTA("Divine Axe Rhitta"),
        DIVINE_JUSTICE("Divine Justice"),
        DIVINE_PUNISHER("Divine Punisher"),
        DIVINE_REAPER("Divine Reaper"),
        DRAGON_SLAYING_BLADE("Dragon Slaying blade"),
        EDGE_OF_THE_ASTRAL_PLANE("Edge Of The Astral Plane"),
        EMBERBLADE("Emberblade"),
        ENIGMA("Enigma"),
        EPIC_SWORD("Epic Sword"),
        ESTOC("Estoc"),
        FALLEN_GODS_SPEAR("Fallen God's Spear"),
        FALLEN_GODS_SWORD("Fallen God's Sword"),
        FLORAL_LONGSWORD("Floral Longsword"),
        FLORAL_SABRE("Floral Sabre"),
        FOREST_GUARDIANS_GLAIVE("Forest Guardian's Glaive"),
        FROST_AXE("Frost Axe"),
        FROST_BLADE("Frost Blade"),
        FROST_SCYTHE("Frost Scythe"),
        HEARTHFLAME("Hearthflame"),
        HERO_SWORD("Hero Sword"),
        HOLY_MOONLIGHT_SWORD("Holy Moonlight Sword"),
        HORNETS_NEEDLE("Hornet's Needle"),
        ICEWHISPER("Icewhisper"),
        JADE_HALBERD("Jade Halberd"),
        KATANA("Katana"),
        LEGENDARY_SWORD("Legendary Sword"),
        LONGSWORD("Longsword"),
        MAGI_SCYTHE("Magi Scythe"),
        MASAMUNE("Masamune"),
        MJOLNIR("Mjolnir"),
        MOLTEN_BLADE("Molten Blade"),
        MOLTEN_SWORD("Molten Sword"),
        MURAMASA("Muramasa"),
        MYSTICAL_SPELLBLADE("Mystical Spellblade"),
        MYTHIC_BLADE("Mythic Blade"),
        OCEANS_RAGE("Ocean's Rage"),
        PARTISAN("Partisan"),
        PHARAOHS_TREASURE("Pharaoh's Treasure"),
        PHEONIX_GRACE("Pheonix Grace"),
        PLAGUE_LONGSWORD("Plague Longsword"),
        POWER_FUSE_HAMMER("Power Fuse Hammer"),
        POWER_FUSE_SWORD("Power Fuse Sword"),
        REQUIEM_OF_THE_NINTH_ABYSS("Requiem of the Ninth Abyss"),
        RIBBON_CLEAVER("Ribbon Cleaver"),
        RIGHTEOUS_RELIC("Righteous Relic"),
        RIVERS_OF_BLOOD("Rivers Of Blood"),
        ROYAL_CHAKRAM("Royal Chakram"),
        ROYAL_RAPIER("Royal Rapier"),
        SABRE("Sabre"),
        SCISSOR_BLADE("Scissor Blade"),
        SCULK_CLEAVER("Sculk Cleaver"),
        SCULK_SCYTHE("Sculk Scythe"),
        SCULK_SWORD("Sculk Sword"),
        SENTINELS_WILL("Sentinel's Will"),
        SILVERINE_BLADE("Silverine Blade"),
        SOUL_CLAWS("Soul Claws"),
        SOUL_COLLECTOR("Soul Collector"),
        SOUL_DEVOURER("Soul Devourer"),
        SOUL_EDGE("Soul Edge"),
        SOUL_HARVESTER("Soul Harvester"),
        SOUL_STEALER("Soul Stealer"),
        SOULRENDER("Soulrender"),
        STARS_EDGE("Star's Edge"),
        STEEL_SWORD("Steel Sword"),
        STOP_SIGN("Stop Sign"),
        STORM_BRINGER("Storm Bringer"),
        STORMS_EDGE("Storm's Edge"),
        SUNBREAK("Sunbreak"),
        TENGENS_BLADE("Tengen's Blade"),
        TERRA_BLADE("Terra Blade"),
        THOUSAND_DEMON_DAGGERS("Thousand Demon Daggers"),
        THUNDER_BRINGER("Thunder Bringer"),
        THUNDERBRAND("Thunderbrand"),
        TRUE_EXCALIBUR("True Excalibur"),
        VAMPIRIC_NEEDLE("Vampiric Needle"),
        WAKIZASHI("Wakizashi"),
        WATCHER_CLAYMORE("Watcher Claymore"),
        WATCHING_WARGLAIVE("Watching Warglaive"),
        WAXWEAVER("Waxweaver"),
        WHISPERWIND("Whisperwind"),
        WICKPIERCER("Wickpiercer"),
        WRAITH_SCYTHE("Wraith Scythe"),
        YORU("Yoru");

        private final String displayName;
        Weapon(String displayName) { this.displayName = displayName; }
        @Override public String getName() { return displayName; }
    }

    private final EnumSetting<Weapon> weapon =
            new EnumSetting<>("setting.weapon", Weapon.CRESCENT_ROSE);

    private final BooleanSetting selfOnly =
            new BooleanSetting("setting.selfOnly", true);

    private static final Identifier PACK_ID = Identifier.of("darkvisuals", "kimiko_swords");
    private static boolean packRegistered = false;

    public CustomSwords() {
        super("CustomSwords", Category.Render, "Заменяет ванильные мечи на кастомные модели");
        getSettings().add(weapon);
        getSettings().add(selfOnly);
        registerPackOnce();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        applyToHand();
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            ItemStack stack = mc.player.getMainHandStack();
            if (isSword(stack)) {
                stack.remove(DataComponentTypes.CUSTOM_MODEL_DATA);
            }
        }
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick e) {
        applyToHand();
    }

    private void applyToHand() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (!isSword(stack)) return;

         
        String key = weapon.getValue().getName();
        CustomModelDataComponent data = new CustomModelDataComponent(
                List.of(), List.of(), List.of(key), List.of());
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, data);
    }

    private boolean isSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof SwordItem || stack.isIn(ItemTags.SWORDS);
    }

    private static synchronized void registerPackOnce() {
        if (packRegistered) return;
        packRegistered = true;

        var container = FabricLoader.getInstance()
                .getModContainer("darkvisuals")
                .orElseThrow(() -> new IllegalStateException("[CustomSwords] Не найден mod container 'darkvisuals'"));

        ResourceManagerHelper.registerBuiltinResourcePack(PACK_ID, container, ResourcePackActivationType.ALWAYS_ENABLED);
    }
}
