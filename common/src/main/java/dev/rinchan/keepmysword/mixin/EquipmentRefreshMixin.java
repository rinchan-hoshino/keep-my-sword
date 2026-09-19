package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.item.EquipmentRefresh;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface EquipmentRefreshMixin extends EquipmentRefresh {
    @Invoker("detectEquipmentUpdates")
    void kms$refreshEquipment();
}
