package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.MapCodec;

import dev.rinchan.keepmysword.item.StorageBoundary;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public abstract class StorageCodecMixin {
    @ModifyExpressionValue(
            method = "<clinit>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/serialization/MapCodec;recursive(Ljava/lang/String;Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;",
                            remap = false))
    private static MapCodec<ItemStack> kms$map(MapCodec<ItemStack> raw) {
        return StorageBoundary.map(raw);
    }
}
