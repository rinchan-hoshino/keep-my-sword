package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.state.BrokenState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Engine root #6: reject broken items as ordinary crafting recipe ingredients. */
@Mixin(Ingredient.class)
public abstract class IngredientProtectionMixin {
    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void kms$rejectBrokenIngredient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (BrokenState.isBroken(stack)) {
            cir.setReturnValue(false);
        }
    }
}
