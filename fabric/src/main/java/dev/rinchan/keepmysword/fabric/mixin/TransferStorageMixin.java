package dev.rinchan.keepmysword.fabric.mixin;

import com.mojang.serialization.Codec;

import dev.rinchan.keepmysword.item.StorageBoundary;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.transfer.VariantCodecs", remap = false)
public abstract class TransferStorageMixin {
    @Shadow @Final @Mutable public static Codec<ItemVariant> ITEM_CODEC;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void kms$storageBoundary(CallbackInfo ci) {
        ITEM_CODEC = StorageBoundary.resources(ITEM_CODEC, ItemVariant::toStack, ItemVariant::of);
    }
}
