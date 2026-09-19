package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.sugar.Local;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.world.item.crafting.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Covers recipes which match directly or through cached ingredients. Repair is explicit. */
@Mixin(RecipeManager.class)
public abstract class RecipeProtectionMixin {
    @Inject(method = "getRecipeFor", at = @At("RETURN"), cancellable = true)
    private void kms$protect(
            CallbackInfoReturnable<Optional<RecipeHolder<?>>> cir,
            @Local(argsOnly = true) RecipeInput input) {
        var recipe = cir.getReturnValue();
        if (recipe.isEmpty() || recipe.get().value() instanceof RepairItemRecipe) return;
        for (int i = 0; i < input.size(); i++) {
            if (SealedItems.isSealed(input.getItem(i))) {
                cir.setReturnValue(Optional.empty());
                return;
            }
        }
    }
}
