package dev.rinchan.keepmysword.neoforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;

import dev.rinchan.keepmysword.item.StorageBoundary;

import net.neoforged.neoforge.transfer.item.ItemResource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ItemResource.class, remap = false)
public abstract class ItemResourceStorageMixin {
    @ModifyExpressionValue(
            method = "<clinit>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;"))
    private static Codec<ItemResource> kms$storage(Codec<ItemResource> raw) {
        return StorageBoundary.resources(raw, ItemResource::toStack, ItemResource::of);
    }
}
