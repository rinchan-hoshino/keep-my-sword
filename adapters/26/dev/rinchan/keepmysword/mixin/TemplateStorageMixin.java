package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.*;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.world.item.ItemStackTemplate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Containers, bundles and other template-based storage need the same disk boundary. */
@Mixin(ItemStackTemplate.class)
public abstract class TemplateStorageMixin implements dev.rinchan.keepmysword.item.TemplateContext {
    @org.spongepowered.asm.mixin.Unique private DynamicOps<?> kms$ops;

    public void kms$context(DynamicOps<?> ops) {
        kms$ops = ops;
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "create")
    private net.minecraft.world.item.ItemStack kms$create(
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<
                            net.minecraft.world.item.ItemStack>
                    original) {
        var stack = SnapshotScope.call(() -> original.call());
        if (SnapshotScope.active()) return stack;
        return kms$ops == null ? SealedItems.seal(stack) : ShellPersistence.decode(stack, kms$ops);
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(
            method =
                    "apply(ILnet/minecraft/core/component/DataComponentPatch;)Lnet/minecraft/world/item/ItemStack;")
    private net.minecraft.world.item.ItemStack kms$apply(
            int count,
            net.minecraft.core.component.DataComponentPatch patch,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<
                            net.minecraft.world.item.ItemStack>
                    original) {
        var stack = SnapshotScope.call(() -> original.call(count, patch));
        if (SnapshotScope.active()) return stack;
        return kms$ops == null ? SealedItems.seal(stack) : ShellPersistence.decode(stack, kms$ops);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "withCount", at = @At("RETURN"))
    private void kms$copyContext(
            int count,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<ItemStackTemplate>
                    cir) {
        ((dev.rinchan.keepmysword.item.TemplateContext) (Object) cir.getReturnValue())
                .kms$context(kms$ops);
    }

    @ModifyExpressionValue(
            method = "<clinit>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/serialization/codecs/RecordCodecBuilder;mapCodec(Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;",
                            remap = false))
    private static MapCodec<ItemStackTemplate> kms$storage(MapCodec<ItemStackTemplate> raw) {
        return new MapCodec<>() {
            public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops) {
                return raw.keys(ops);
            }

            public <T> DataResult<ItemStackTemplate> decode(DynamicOps<T> ops, MapLike<T> input) {
                return raw.decode(ops, input)
                        .map(
                                value -> {
                                    ((dev.rinchan.keepmysword.item.TemplateContext) (Object) value)
                                            .kms$context(ops);
                                    return value;
                                });
            }

            public <T> RecordBuilder<T> encode(
                    ItemStackTemplate value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                if (SealedRegistry.INERT == null || value.item().value() != SealedRegistry.INERT)
                    return raw.encode(value, ops, prefix);
                var result = ShellPersistence.encode(SnapshotScope.call(value::create), ops);
                return result.map(
                                stack ->
                                        raw.encode(
                                                ItemStackTemplate.fromNonEmptyStack(stack),
                                                ops,
                                                prefix))
                        .result()
                        .orElseGet(() -> prefix.withErrorsFrom(result));
            }
        };
    }
}
