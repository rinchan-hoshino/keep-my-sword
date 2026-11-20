package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.state.BrokenState;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.x engine root #6: block broken items from matching non-repair crafting recipes. */
@Mixin(RecipeManager.class)
public abstract class RecipeProtectionMixin {
    @Inject(
            method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void kms$rejectBrokenNonRepairRecipe(
            RecipeType<T> type,
            I input,
            Level level,
            ResourceKey<Recipe<?>> lastRecipe,
            CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        Optional<RecipeHolder<T>> matched = cir.getReturnValue();
        if (matched.isEmpty()) return;
        if (matched.get().value() instanceof RepairItemRecipe) return;
        for (int i = 0; i < input.size(); i++) {
            if (BrokenState.isBroken(input.getItem(i))) {
                cir.setReturnValue(Optional.empty());
                return;
            }
        }
    }
}
