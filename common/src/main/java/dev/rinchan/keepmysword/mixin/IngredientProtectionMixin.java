package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A sealed item is never an ingredient made from the vanilla carrier. */
@Mixin(Ingredient.class)
public abstract class IngredientProtectionMixin {
    @Inject(
            method = "test(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void kms$protect(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack != null && SealedItems.isSealed(stack)) cir.setReturnValue(false);
    }
}
