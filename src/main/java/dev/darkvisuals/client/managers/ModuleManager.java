package dev.darkvisuals.client.managers;

import dev.darkvisuals.client.events.impl.EventKey;
import dev.darkvisuals.client.events.impl.EventMouse;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.impl.utility.Record;
import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.impl.render.*;
import dev.darkvisuals.modules.impl.render.lyrics.KineticLyrics;
import dev.darkvisuals.modules.impl.utility.*;
import dev.darkvisuals.client.util.Wrapper;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ModuleManager implements Wrapper {

    private final List<Module> modules = new ArrayList<>();
    private final Map<Module, Bind> defaultBinds = new HashMap<>();

    public ModuleManager() {
        darkvisuals.getInstance().getEventHandler().subscribe(this);
        addModules(
                new HUD(),
                new NoRender(),
                new Fullbright(),
                new Crosshair(),
                new ViewModel(),
                new TargetEsp(),
                new AutoSprint(),
                new UI(),
                new AspectRatio(),
                new FTHelper(),
                new RWHelper(),
                new HitSound(),
                new CustomSounds(),
                new AutoRespawn(),
                new NameProtect(),
                new PvpHelper(),
                new JumpCircle(),
                new CubeTrails(),
                new ClientSound(),
                new TotemCounter(),
                new WorldParticles(),
                new DamageParticles(),
                new CustomFog(),
                new SwingAnimation(),
                new TimeChanger(),
                new ItemPhysic(),
                new FriendHelper(),
                new Predictions(),
                new BlockOverlay(),
                new BetterMinecraft(),
                new Zoom(),
                new ShiftTap(),
                new Trails(),
                new HitBubbles(),
                new HitStreak(),
                new HitColor(),
                new DiscordRPCModule(),
                new Cape(),
                new Dog(),
                new Cosmetics(),
                new Optimization(),
                new KillEffect(),
                new MyNick(),
                new VirtualPlayer(),
                new Cubes(),
                new LineGlyphes(),
                new ActionParticles(),
                new Owl(),
                new Emotes(),
                new Gib(),
                new Record(),
                new BeautifulSky(),
                new UserTab(),
                new QuickMarkers(),
                new BrewingStandVisuals(),
                new RWJoiner(),
                new CrystalColor(),
                new CrystalHelper(),
                new Macros(),
                new InventoryProfiles(),
                new HideACBot(),
                new SeeCraft(),
                new StructureVisualer(),
                new BrowserModule(),
                new CustomSwords(),
                new CustomInventory(),
                new HitRange(),
                new TnTEffect(),
                new SkyShader(),
                new Rain(),
                new ShieldBreakFX(),
                new CrystalBreakFX(),
                new CustomWings(),
                new SoundBasVisual(),
                new Particles(),
                new Fireflies(),
                new FallingLeaves(),
                new ShootingStars(),
                new LensFlare(),
                new ItemGlow(),
                new PlayerAura(),
                new WeatherFX(),
                new WaterSplash(),
                new RegenHearts(),
                new AmbientMotes(),
                new ArrowTrails(),
                new Footprints(),
                new FallImpact(),
                new SprintFX(),
                new UnderwaterBubbles(),
                new TorchGlow(),
                new EatingFX(),
                new ElytraTrails(),
                new TotemFX(),
                new DamageFX(),
                new KineticLyrics(),
                new Hitboxes(),
                new ChunkBorders(),
                new ItemESP(),
                new CrystalRadius(),
                new Snowfall(),
                new Aurora(),
                new Butterflies(),
                new ElytraWind(),
                new LowHealthFX(),
                new HitMarker(),
                new Souls(),
                // Эффекты красоты
                new Fireworks(),
                new Rainbow(),
                new CherryBlossom(),
                new SkyLanterns(),
                new Halo(),
                new FairyDust(),
                new FireAura(),
                new WaterRipples(),
                new OrbitingOrbs(),
                new MysticMist(),
                new GlassNotes(),
                new CombatTimer(),
                new SlotLock(),
                new ItemReminder(),
                new DurabilityAlert(),
                new CoordsClipboard(),
                new TotemPops(),
                new SessionStats(),
                new ConsumeTimer(),
                new PingDisplay(),
                new Graffity(),
                new BlockBreakProgress(),
                new CustomDeathScreen(),
                new FriendGlow(),
                new Portal(),
                new HitEffect()
        );

        for (Module module : modules) {
            try {
                for (Field field : module.getClass().getDeclaredFields()) {
                    if (!Setting.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Setting<?> setting = (Setting<?>) field.get(module);
                    if (setting != null && !module.getSettings().contains(setting)) module.getSettings().add(setting);
                }
            } catch (Exception ignored) {}
             
            defaultBinds.put(module, module.getBind());
        }
    }

    private void addModules(Module... module) {
        this.modules.addAll(List.of(module));
    }

    @EventHandler
    public void onKey(EventKey e) {
         
        if (mc.currentScreen != null) return;
         
        if (e.getKey() < 0) return;
        for (Module module : modules) {
            if (module.getBind().isMouse()) continue;
            if (module.getBind().getKey() < 0) continue;
            if (module.getBind().getKey() != e.getKey()) continue;
            switch (e.getAction()) {
                case GLFW.GLFW_PRESS -> {
                    if (module.getBind().getMode() == dev.darkvisuals.modules.settings.api.Bind.Mode.HOLD) module.setToggled(true);
                    else module.toggle();
                }
                case GLFW.GLFW_RELEASE -> {
                    if (module.getBind().getMode() == dev.darkvisuals.modules.settings.api.Bind.Mode.HOLD) module.setToggled(false);
                }
            }
        }
    }

    @EventHandler
    public void onMouse(EventMouse e) {
         
        if (mc.currentScreen != null) return;
        if (e.getButton() < 0) return;
        for (Module module : modules) {
            if (!module.getBind().isMouse()) continue;
            if (module.getBind().getKey() < 0) continue;
            if (module.getBind().getKey() != e.getButton()) continue;
            switch (e.getAction()) {
                case GLFW.GLFW_PRESS -> {
                    if (module.getBind().getMode() == dev.darkvisuals.modules.settings.api.Bind.Mode.HOLD) module.setToggled(true);
                    else module.toggle();
                }
                case GLFW.GLFW_RELEASE -> {
                    if (module.getBind().getMode() == dev.darkvisuals.modules.settings.api.Bind.Mode.HOLD) module.setToggled(false);
                }
            }
        }
    }

    public List<Module> getModules(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }

    public List<Category> getCategories() {
        return Arrays.asList(Category.values());
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (!clazz.isInstance(module)) continue;
            return (T) module;
        }
        return null;
    }

    public Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public void resetBindsToDefaults() {
        for (Module module : modules) {
            Bind def = defaultBinds.get(module);
            if (def == null) def = new Bind(-1, false);
            module.setBind(def);
        }
    }
}
