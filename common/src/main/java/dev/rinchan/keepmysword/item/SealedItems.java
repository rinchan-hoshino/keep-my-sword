package dev.rinchan.keepmysword.item;

import dev.rinchan.keepmysword.KeepMySword;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public final class SealedItems {
    public static boolean isSealed(ItemStack stack) {
        return SealedRegistry.INERT != null && stack.is(SealedRegistry.INERT);
    }

    public static ItemStack seal(ItemStack source) {
        if (source.isEmpty()
                || SealedRegistry.ORIGINAL == null
                || SealedRegistry.INERT == null
                || isSealed(source)
                || KeepMySword.isExcluded(source)) return source;
        if (!dev.rinchan.rinlib.item.DamageState.isBroken(source)) return source;
        var sealed = new ItemStack(SealedRegistry.INERT, source.getCount());
        sealed.set(SealedRegistry.ORIGINAL, new OriginalPayload(source));
        sealed.set(DataComponents.CUSTOM_NAME, source.getHoverName());
        return sealed;
    }

    /** Rendering integrations may enumerate the registered item without its payload. */
    public static ItemStack visualCopy(ItemStack stack) {
        if (!isSealed(stack)) return stack;
        if (!stack.has(SealedRegistry.ORIGINAL))
            return new ItemStack(net.minecraft.world.item.Items.BARRIER);
        return originalForSave(stack);
    }

    public static ItemStack originalForSave(ItemStack stack) {
        if (!isSealed(stack)) return stack;
        var payload = stack.get(SealedRegistry.ORIGINAL);
        if (payload == null)
            throw new IllegalStateException("Sealed item has no original: refusing to serialize");
        return SnapshotScope.call(() -> payload.copy().copyWithCount(stack.getCount()));
    }

    public static boolean sealInPlace(
            ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        var replacement =
                ShellPersistence.decode(
                        stack,
                        registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE));
        if (replacement == stack) return false;
        ((ReplaceableStack) (Object) stack).kms$replaceWith(replacement);
        return true;
    }

    public static void sealAfterMutation(ItemStack stack) {
        if (SnapshotScope.active() || isSealed(stack)) return;
        var replacement = seal(stack);
        if (replacement != stack) ((ReplaceableStack) (Object) stack).kms$replaceWith(replacement);
    }

    private SealedItems() {}
}
