package dev.darkvisuals.mixin;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.intel.IntelWorkarounds;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL32C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(value = MinecraftClient.class, priority = 900)
public abstract class SodiumRenderAheadMixin {
    @Unique private boolean darkvisuals$renderAheadChecked;
    @Unique private boolean darkvisuals$disableRenderAhead;
    @Unique private LongArrayFIFOQueue darkvisuals$sodiumFences;

    // Run after Sodium queues its fence, so its next preRender never waits on this driver.
    @Inject(method = "render", at = @At("RETURN"), order = 1100)
    private void darkvisuals$releaseLegacyIntelFences(boolean tick, CallbackInfo ci) {
        if (!this.darkvisuals$renderAheadChecked) {
            this.darkvisuals$renderAheadChecked = true;
            String vendor = GL11C.glGetString(GL11C.GL_VENDOR);
            this.darkvisuals$disableRenderAhead = vendor != null
                    && vendor.toLowerCase(Locale.ROOT).contains("intel")
                    && IntelWorkarounds.findIntelDriverMatchingBug899() != null;
            if (this.darkvisuals$disableRenderAhead) {
                try {
                    // Sodium adds this field during Mixin application, after shadow resolution.
                    var field = MinecraftClient.class.getDeclaredField("fences");
                    field.setAccessible(true);
                    this.darkvisuals$sodiumFences = (LongArrayFIFOQueue) field.get(this);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Could not access Sodium's render-ahead fences", e);
                }
                LogManager.getLogger("DarkVisuals-SodiumCompat").info(
                        "Disabled CPU render-ahead GPU waits for the legacy Intel graphics driver");
            }
        }

        if (this.darkvisuals$disableRenderAhead) {
            while (!this.darkvisuals$sodiumFences.isEmpty()) {
                GL32C.glDeleteSync(this.darkvisuals$sodiumFences.dequeueLong());
            }
        }
    }
}
