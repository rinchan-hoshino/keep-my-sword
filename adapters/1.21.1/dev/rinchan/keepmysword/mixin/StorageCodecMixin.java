package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;

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
                                    "Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;",
                            ordinal = 0,
                            remap = false))
    private static Codec<ItemStack> kms$full(Codec<ItemStack> codec) {
        return StorageBoundary.full(codec);
    }

    @ModifyExpressionValue(
            method = "<clinit>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;",
                            ordinal = 1,
                            remap = false))
    private static Codec<ItemStack> kms$single(Codec<ItemStack> codec) {
        return StorageBoundary.adapt(codec);
    }
}
