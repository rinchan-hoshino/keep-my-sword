package dev.rinchan.keepmysword;

import dev.rinchan.keepmysword.item.DamageState;
import dev.rinchan.keepmysword.item.EquipmentRefresh;
import dev.rinchan.keepmysword.item.SealedItems;
import dev.rinchan.rinlib.v2.Ids;
import dev.rinchan.rinlib.v2.ItemEvents;
import dev.rinchan.rinlib.v2.RepairEvents;
import dev.rinchan.rinlib.v2.Rin;
import dev.rinchan.rinlib.v2.RinMod;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public final class KeepMySword implements RinMod {
    public static final String MOD_ID = "keep_my_sword";
    private static final TagKey<Item> EXCLUDED_ITEMS = Ids.itemTag(MOD_ID, "excluded");
    private static KeepMySwordConfig config = new KeepMySwordConfig(Set.of());

    public static void initialize(Path configDirectory) throws IOException {
        config = KeepMySwordConfig.load(configDirectory);
    }

    public KeepMySword() {}

    @Override
    public void init(Rin env) {
        try {
            initialize(env.configDir());
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
        ItemEvents.BEFORE_BREAK.register((stack, level, owner) -> {
            if (SealedItems.sealInPlace(stack, level.registryAccess()) && owner != null)
                ((EquipmentRefresh) owner).kms$refreshEquipment();
            return isManagedBroken(stack);
        });
        RepairEvents.CRAFTING_VIEW.register((s, level) -> level != null
                ? SealedItems.originalForRepair(s, level)
                : SealedItems.originalForRepair(s));
        RepairEvents.ANVIL.register(new RepairEvents.AnvilHook() {
            @Override public boolean blockResult(ItemStack input, Level level) {
                return SealedItems.isSealed(input)
                        && SealedItems.originalForRepair(input, level).isEmpty();
            }
            @Override public ItemStack viewInput(ItemStack input, Level level) {
                return SealedItems.originalForRepair(input, level);
            }
            @Override public ItemStack finishResult(ItemStack left, ItemStack right, ItemStack result,
                    RegistryAccess access) {
                return SealedItems.isSealed(left)
                        || SealedItems.isSealed(right)
                        ? SealedItems.seal(result, access) : result;
            }
        });
    }

    public static boolean isRepairViewExcluded(Class<?> menuClass, String menuId) {
        return config.excludesRepairView(menuClass, menuId);
    }

    public static boolean isExcluded(ItemStack stack) {
        return stack.is(EXCLUDED_ITEMS) || config.matches(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getItem().builtInRegistryHolder().tags().map(tag -> tag.location().toString()));
    }

    public static boolean isManagedBroken(ItemStack stack) {
        return SealedItems.isSealed(stack)
                || (!isExcluded(stack) && DamageState.isBroken(stack));
    }
}
