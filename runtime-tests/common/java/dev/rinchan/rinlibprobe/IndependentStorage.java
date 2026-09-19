package dev.rinchan.rinlibprobe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class IndependentStorage {
    public static final Codec<ItemStack> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            BuiltInRegistries.ITEM
                                                    .byNameCodec()
                                                    .fieldOf("type")
                                                    .forGetter(ItemStack::getItem),
                                            Codec.INT
                                                    .fieldOf("amount")
                                                    .forGetter(ItemStack::getCount),
                                            DataComponentPatch.CODEC
                                                    .fieldOf("patch")
                                                    .forGetter(ItemStack::getComponentsPatch))
                                    .apply(
                                            i,
                                            (item, count, patch) ->
                                                    new ItemStack(
                                                            item.builtInRegistryHolder(),
                                                            count,
                                                            patch)));
}
