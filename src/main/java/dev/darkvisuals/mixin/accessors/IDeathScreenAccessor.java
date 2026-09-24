package dev.darkvisuals.mixin.accessors;

import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DeathScreen.class)
public interface IDeathScreenAccessor {
    @Accessor("message")
    net.minecraft.text.Text darkvisuals$getMessage();

    @Accessor("isHardcore")
    boolean darkvisuals$isHardcore();
}