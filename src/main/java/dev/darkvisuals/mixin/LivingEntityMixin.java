package dev.darkvisuals.mixin;

import dev.darkvisuals.client.util.Wrapper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Wrapper {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

}