package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.state.BrokenState;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.20.1 engine root #6: block broken items from matching non-repair crafting recipes. */
@Mixin(RecipeManager.class)
public abstract class RecipeProtectionMixin {
    @Inject(
            method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true)
    private <C extends Container, T extends Recipe<C>> void kms$rejectBrokenNonRepairRecipe(
            RecipeType<T> type,
            C container,
            Level level,
            ResourceLocation lastRecipe,
            CallbackInfoReturnable<Optional<T>> cir) {
        Optional<T> matched = cir.getReturnValue();
        if (matched.isEmpty()) return;
        if (matched.get() instanceof RepairItemRecipe) return;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (BrokenState.isBroken(container.getItem(i))) {
                cir.setReturnValue(Optional.empty());
                return;
            }
        }
    }
}
