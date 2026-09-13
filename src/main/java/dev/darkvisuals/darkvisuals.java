package dev.darkvisuals;


import dev.darkvisuals.client.managers.*;
import dev.darkvisuals.client.ui.mainmenu.MainMenu;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.security.LicenseGuard;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import dev.darkvisuals.client.ui.clickgui.ClickGui;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.*;


import java.io.File;
import java.lang.invoke.MethodHandles;

@Getter
public class darkvisuals implements ModInitializer, Wrapper {

    @Getter private static darkvisuals instance;

    private IEventBus eventHandler;
    private long initTime;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private AutoSaveManager autoSaveManager;
    private NotifyManager notifyManager;
    private PerformanceManager performanceManager;
    private ClickGui clickGui;
    private HudManager hudManager;
    private dev.darkvisuals.client.managers.AltManager altManager;
    private MainMenu mainmenu;
    private dev.darkvisuals.client.ui.hud.impl.WaypointOverlay waypointOverlay;

    public static Logger LOGGER = LogManager.getLogger(darkvisuals.class);
    private final File globalsDir = new File(mc.runDirectory, "darkvisuals");
    private final File configsDir = new File(globalsDir, "configs");

    @Override
    public void onInitialize() {
        LOGGER.info("[DarkVisuals] Я НАТАЛЬЯ МОРСКАЯ ПЕХОТА СТАРТУЕМ.");
        initTime = System.currentTimeMillis();
        instance = this;


        if (!LicenseGuard.verifyOnStartup()) {
            LOGGER.warn("[DarkVisuals] Глупий Крякер пашл вон");
            return;
        }

        createDirs(globalsDir, configsDir);
        eventHandler = new EventBus();

        eventHandler.registerLambdaFactory("dev.darkvisuals",
                (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())
        );

        FriendsManager.init(globalsDir);
        FriendsManager.importFromLauncher(mc.runDirectory);
        AltManager.init(globalsDir);
        String lastAlt = AltManager.getLastUsedNickname();
        if (lastAlt != null && !lastAlt.isEmpty()) {
            AltManager.applyNickname(lastAlt);
        }

        notifyManager = new NotifyManager();
        performanceManager = new PerformanceManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        configManager = new ConfigManager();
        autoSaveManager = new AutoSaveManager();
        clickGui = new ClickGui();
        hudManager = new HudManager();
        mainmenu = new MainMenu();
        dev.darkvisuals.emotes.network.EmotesClientNetwork.register();
        dev.darkvisuals.client.managers.CosmeticsSyncManager.init();

         
        waypointOverlay = new dev.darkvisuals.client.ui.hud.impl.WaypointOverlay();
        eventHandler.subscribe(waypointOverlay);

         
        autoSaveManager.loadAutoSave();

         
 
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return darkvisuals.id("fonts_reload");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        Fonts.reload();  
                         
                         
                        dev.darkvisuals.client.util.IntroManager.init(manager);
                    }
                }
        );

         
         
         
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
             
             
            LicenseGuard.tick();
            if (!LicenseGuard.isValid()) return;  

            if (client.currentScreen instanceof TitleScreen
                    && !(client.currentScreen instanceof MainMenu)) {
                client.setScreen(mainmenu);
            }
        });

        LOGGER.info("[darkvisuals] Successfully initialized for {} ms.", System.currentTimeMillis() - initTime);
    }

    private void createDirs(File... file) {
        for (File f : file) f.mkdirs();
    }

      
    public static void post(Object event) {
        darkvisuals i = instance;
        if (i == null || i.eventHandler == null) return;
        i.eventHandler.post(event);
    }

    public static Identifier id(String texture) {
        return Identifier.of("darkvisuals", texture);
    }
}