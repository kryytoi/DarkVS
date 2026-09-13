package dev.darkvisuals.mixin;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.BeautifulSky;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 
@Mixin(WorldRenderer.class)
public abstract class WorldRendererSkyMixin {

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void darkvisuals$renderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo ci) {
        BeautifulSky module = darkvisuals.getInstance().getModuleManager().getModule(BeautifulSky.class);
        if (module == null || !module.isToggled()) return;
        if (module.mc.world == null) return;

        RegistryKey<World> dim = module.mc.world.getRegistryKey();
        if (dim != World.END && dim != World.NETHER) return;  

        ci.cancel();  
    }
}