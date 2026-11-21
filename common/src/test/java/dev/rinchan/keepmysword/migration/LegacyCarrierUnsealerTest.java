package dev.rinchan.keepmysword.migration;

import static org.junit.jupiter.api.Assertions.*;

import dev.rinchan.keepmysword.config.KeepMySwordConfig;
import dev.rinchan.keepmysword.state.BrokenState;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.AddValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LegacyCarrierUnsealerTest {
    @BeforeAll
    static void bindComponentsIfPresent() throws Exception {
        try {
            Object initializers = BuiltInRegistries.class.getField("DATA_COMPONENT_INITIALIZERS").get(null);
            Method build = initializers.getClass().getMethod("build", HolderLookup.Provider.class);
            // NeoForge 26.x CommonHooks.validateComponent rejects anonymous HolderSet$1 when IS_RUNNING_IN_IDE is true.
            boolean wasInIde = SharedConstants.IS_RUNNING_IN_IDE;
            SharedConstants.IS_RUNNING_IN_IDE = false;
            try {
                HolderLookup.Provider lookup = createVanillaLookup();
                List<?> pending = (List<?>) build.invoke(initializers, lookup);
                Method apply = Class.forName(
                                "net.minecraft.core.component.DataComponentInitializers$PendingComponents")
                        .getMethod("apply");
                for (Object entry : pending) {
                    apply.invoke(entry);
                }
            } finally {
                SharedConstants.IS_RUNNING_IN_IDE = wasInIde;
            }
        } catch (NoSuchFieldException ignored) {
            // 1.21.1 binds item components during Bootstrap.bootStrap().
        }
    }

    private static HolderLookup.Provider createVanillaLookup() throws Exception {
        try {
            return (HolderLookup.Provider) VanillaRegistries.class.getMethod("createLookup").invoke(null);
        } catch (NoSuchMethodException ignored) {
            return (HolderLookup.Provider) VanillaRegistries.class.getMethod("createWorldLookup").invoke(null);
        }
    }

    private static RegistryAccess registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void unsealsNativeV12SealedRecoveryCompass() {
        RegistryAccess reg = registries();
        ItemStack original = new ItemStack(Items.DIAMOND_SWORD);
        original.set(DataComponents.CUSTOM_NAME, Component.literal("Excalibur"));
        original.setDamageValue(original.getMaxDamage());

        CompoundTag encoded = (CompoundTag) ItemStack.CODEC
                .encodeStart(reg.createSerializationContext(NbtOps.INSTANCE), original)
                .getOrThrow();
        encoded.putInt("DataVersion", LegacyCarrierUnsealer.currentDataVersion());

        CompoundTag custom = new CompoundTag();
        custom.put(LegacyCarrierUnsealer.SEALED_KEY, encoded);
        ItemStack carrier = new ItemStack(Items.RECOVERY_COMPASS);
        carrier.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));

        assertTrue(LegacyCarrierUnsealer.isLegacySealed(carrier));
        Optional<ItemStack> restored = LegacyCarrierUnsealer.unseal(carrier, reg);
        assertTrue(restored.isPresent());
        assertTrue(restored.get().is(Items.DIAMOND_SWORD));
        assertEquals("Excalibur", restored.get().getHoverName().getString());
        assertEquals(restored.get().getMaxDamage(), restored.get().getDamageValue());
        assertTrue(BrokenState.isBroken(restored.get()));
    }

    @Test
    void unsealsSchema1CompressedLegacyCarrier() throws Exception {
        RegistryAccess reg = registries();
        ItemStack original = new ItemStack(Items.IRON_PICKAXE);
        original.setDamageValue(original.getMaxDamage());

        CompoundTag encoded = (CompoundTag) ItemStack.CODEC
                .encodeStart(reg.createSerializationContext(NbtOps.INSTANCE), original)
                .getOrThrow();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(encoded, out);
        String b64 = Base64.getEncoder().encodeToString(out.toByteArray());

        ListTag chunks = new ListTag();
        chunks.add(StringTag.valueOf(b64.substring(0, b64.length() / 2)));
        chunks.add(StringTag.valueOf(b64.substring(b64.length() / 2)));

        CompoundTag payload = new CompoundTag();
        payload.putInt("schema", 1);
        payload.putInt("data_version", LegacyCarrierUnsealer.currentDataVersion());
        payload.put("chunks", chunks);

        CompoundTag custom = new CompoundTag();
        custom.put(LegacyCarrierUnsealer.SEALED_KEY, payload);
        ItemStack carrier = new ItemStack(Items.RECOVERY_COMPASS);
        carrier.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));

        Optional<ItemStack> restored = LegacyCarrierUnsealer.unseal(carrier, reg);
        assertTrue(restored.isPresent());
        assertTrue(restored.get().is(Items.IRON_PICKAXE));
        assertEquals(restored.get().getMaxDamage(), restored.get().getDamageValue());
    }

    @Test
    void ignoresOrdinaryRecoveryCompassAndMalformedPayload() {
        RegistryAccess reg = registries();
        ItemStack normalCompass = new ItemStack(Items.RECOVERY_COMPASS);
        assertFalse(LegacyCarrierUnsealer.isLegacySealed(normalCompass));
        assertTrue(LegacyCarrierUnsealer.unseal(normalCompass, reg).isEmpty());

        CompoundTag custom = new CompoundTag();
        custom.put(LegacyCarrierUnsealer.SEALED_KEY, new CompoundTag());
        ItemStack malformed = new ItemStack(Items.RECOVERY_COMPASS);
        malformed.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        assertTrue(LegacyCarrierUnsealer.isLegacySealed(malformed));
        assertTrue(LegacyCarrierUnsealer.unseal(malformed, reg).isEmpty());
    }

    @Test
    void gatesBrokenItemEnchantmentLevelExceptConfiguredMending() {
        Enchantment.EnchantmentDefinition def = Enchantment.definition(
                HolderSet.empty(),
                1,
                5,
                Enchantment.constantCost(1),
                Enchantment.constantCost(1),
                1,
                EquipmentSlotGroup.MAINHAND);
        Holder<Enchantment> combat = Holder.direct(new Enchantment(
                Component.literal("Sharpness"), def, HolderSet.empty(), DataComponentMap.EMPTY));
        Holder<Enchantment> mending = Holder.direct(new Enchantment(
                Component.literal("Mending"),
                def,
                HolderSet.empty(),
                DataComponentMap.builder()
                        .set(
                                EnchantmentEffectComponents.REPAIR_WITH_XP,
                                List.of(new ConditionalEffect<>(
                                        new AddValue(LevelBasedValue.constant(2.0F)), Optional.empty())))
                        .build()));

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(combat, 5);
        sword.enchant(mending, 1);

        assertEquals(5, EnchantmentHelper.getItemEnchantmentLevel(combat, sword));
        assertEquals(1, EnchantmentHelper.getItemEnchantmentLevel(mending, sword));

        sword.setDamageValue(sword.getMaxDamage());
        assertTrue(BrokenState.isBroken(sword));

        try {
            KeepMySwordConfig.setForTest(new KeepMySwordConfig(true, true, true, List.of()));
            assertEquals(0, EnchantmentHelper.getItemEnchantmentLevel(combat, sword));
            assertEquals(1, EnchantmentHelper.getItemEnchantmentLevel(mending, sword));

            KeepMySwordConfig.setForTest(new KeepMySwordConfig(false, true, true, List.of()));
            assertEquals(0, EnchantmentHelper.getItemEnchantmentLevel(combat, sword));
            assertEquals(0, EnchantmentHelper.getItemEnchantmentLevel(mending, sword));
        } finally {
            KeepMySwordConfig.setForTest(KeepMySwordConfig.defaults());
        }
    }
}
