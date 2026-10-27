package dev.rinchan.keepmysword;

import dev.rinchan.rinlib.item.DamageState;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public final class KeepMySword {
    public static final String MOD_ID = "keep_my_sword";
    private static KeepMySwordConfig config = new KeepMySwordConfig(Set.of());

    public static void initialize(Path configDirectory) throws IOException {
        config = KeepMySwordConfig.load(configDirectory);
    }

    private KeepMySword() {}

    public static boolean isExcluded(ItemStack stack) {
        return stack.is(VersionTags.EXCLUDED_ITEMS) || config.matches(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getItem().builtInRegistryHolder().tags().map(tag -> tag.location().toString()));
    }

    public static boolean isManagedBroken(ItemStack stack) {
        return dev.rinchan.keepmysword.item.SealedItems.isSealed(stack)
                || (!isExcluded(stack) && DamageState.isBroken(stack));
    }
}
